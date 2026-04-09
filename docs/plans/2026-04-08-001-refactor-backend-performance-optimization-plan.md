---
title: "refactor: DataEase 后端性能与内存优化"
type: refactor
status: active
date: 2026-04-08
origin: docs/brainstorms/backend-performance-optimization-requirements.md
---

# DataEase 后端性能与内存优化计划

## Overview

对 DataEase Spring Boot 3.3 + Java 21 后端进行性能调优，通过启用虚拟线程、切换 ZGC、调优 JVM/Ehcache/连接池参数，在不更换语言和架构的前提下显著降低内存占用和请求延迟。

## Problem Frame

DataEase 后端在生产环境中内存占用偏高，且未利用 Java 21 和 Spring Boot 3.3 提供的性能特性（虚拟线程、ZGC）。当前配置几乎全部使用默认值，Docker/启动脚本中无任何 JVM 调优。BI 工具是 I/O 密集型应用，主要瓶颈在于数据库查询和并发处理，而非 CPU 计算。

## Requirements Trace

- R1. 启用 Java 21 虚拟线程，提升并发承载能力 (origin: P0)
- R2. 切换到 ZGC 垃圾收集器，GC 停顿 < 5ms (origin: P1)
- R3. 调优 JVM 内存参数，总内存降低 30% (origin: P2)
- R4. 优化 Ehcache 配置，减少不必要的内存分配 (origin: P3)
- R5. 配置数据库连接池，减少空闲连接 (origin: P4)
- R6. 后端内存占用降低 30%+，P95 延迟降低 20%+，GC 停顿 < 5ms (origin: 成功指标)

## Scope Boundaries

- 不更换编程语言、不拆分微服务、不升级 Spring Boot 主版本
- SQL 查询优化 (origin: P5) 为持续改进项，不纳入本次计划范围
- 不涉及前端性能优化
- 不涉及 Calcite 数据源连接池的默认值调整（这影响用户配置行为，需单独评估）

## Context & Research

### Relevant Code and Patterns

- **Spring Boot 3.3.0 parent + 3.3.13 dependency version** — 完整支持虚拟线程和分代 ZGC
- **Dockerfile** — 通过 `run-java.sh` 启动，读取 `JAVA_OPTIONS` 环境变量
- **start-backend.bat** — 开发环境通过 `mvn spring-boot:run` 启动
- **CommonThreadPool** (`sdk/common/src/main/java/io/dataease/utils/CommonThreadPool.java`) — 核心线程池，被 Bean 覆盖为 core=50/max=100
- **DBCP2** — 主数据库连接池，无显式配置，使用 Spring Boot 默认值
- **Ehcache** — 17 个命名缓存 + 1 个 token 缓存，多数永不过期，统一 10MB offheap

### Key Findings

1. `CommonThreadPool.addTask(task, timeout, unit)` 方法为每个带超时的任务创建 `newSingleThreadExecutor()`，在虚拟线程模式下可改用虚拟线程简化
2. Calcite 数据源默认连接池参数 (initial=50, min=50, max=100) 过大，但调整会影响用户行为，不在本次范围
3. 权限相关缓存（7个）全部永不过期，完全依赖手动清除 — 这是设计选择，不做修改
4. `ChartViewManege` 每次处理多图表时新建 `newFixedThreadPool(10)` — 可优化但非核心

## Key Technical Decisions

- **虚拟线程启用方式**: 通过 `spring.threads.virtual.enabled=true` 配置，Spring Boot 3.3 会自动将 Tomcat 请求处理和 @Async 任务切换到虚拟线程
- **GC 选择**: 分代 ZGC (`-XX:+UseZGC -XX:+ZGenerational`)，Java 21 生产就绪，比 G1 停顿更低
- **JVM 容器内存策略**: 使用 `-XX:MaxRAMPercentage=75.0` 替代硬编码 `-Xmx`，适配不同部署环境
- **Ehcache 优化策略**: 只调整资源分配（offheap 大小和磁盘缓存），不改变缓存过期策略（永不过期是有意设计）
- **连接池实现**: 继续使用 DBCP2（项目已依赖），仅添加显式参数配置

## Open Questions

### Deferred to Implementation

- **JVM 堆大小具体值**: 需要根据实际部署环境的可用内存确定 `-XX:MaxRAMPercentage` 的最佳值（建议初始 75%，后续可调）
- **Ehcache 各缓存实际使用量**: 需要在测试环境监控缓存命中率后，才能精确调整 offheap 分配
- **DBCP2 最优连接数**: 取决于实际并发量，初始建议 maxTotal=30，需根据监控调整

## Implementation Units

- [ ] **Unit 1: 启用虚拟线程**

**Goal:** 通过一行配置启用 Java 21 虚拟线程，大幅提升并发能力

**Requirements:** R1, R6

**Dependencies:** None

**Files:**
- Modify: `core/core-backend/src/main/resources/application.yml`

**Approach:**
在 `application.yml` 的 spring 配置下添加 `threads.virtual.enabled: true`。Spring Boot 3.3 会自动将 Tomcat 线程、@Async 任务、ScheduledTask 切换到虚拟线程。这是零风险变更，因为虚拟线程是 Thread 的子类，所有现有代码透明兼容。

**Test scenarios:**
- Happy path: 应用正常启动，API 端点响应正常
- Integration: 高并发场景下（模拟 100+ 并发请求）内存占用显著低于启用前
- Edge case: CommonThreadPool 的带超时任务仍能正确超时和清理

**Verification:**
- 应用正常启动，日志中可见虚拟线程相关输出
- 通过 `jcmd <pid> Thread.dump_to_file` 确认请求处理线程为虚拟线程

---

- [ ] **Unit 2: 配置 JVM 参数（ZGC + 内存调优）**

**Goal:** 在 Dockerfile 和启动脚本中添加 JVM 调优参数，切换 ZGC 并合理分配内存

**Requirements:** R2, R3, R6

**Dependencies:** None (可与 Unit 1 并行)

**Files:**
- Modify: `Dockerfile`
- Modify: `start-backend.bat`

**Approach:**
Dockerfile 中修改 `JAVA_OPTIONS` 环境变量，添加以下参数：
- `-XX:+UseZGC` — 使用 ZGC 垃圾收集器
- `-XX:+ZGenerational` — 启用分代模式（Java 21 新特性，性能更优）
- `-XX:MaxRAMPercentage=75.0` — 容器内存的 75% 用于 JVM 堆
- `-XX:InitialRAMPercentage=75.0` — 初始堆与最大堆一致，避免动态扩容
- `-XX:+UseStringDeduplication` — 字符串去重，降低内存占用
- `-XX:+AlwaysPreTouch` — ~~已移除：在 ZGC + 大内存容器下会显著增加启动时间，收益不足以抵消~~

对于 `start-backend.bat`，通过 Maven 的 `spring-boot.run.jvmArguments` 传递等价参数。

**Test scenarios:**
- Happy path: 应用正常启动，GC 日志确认使用 ZGC
- Edge case: 在低内存环境（如 1GB 容器）下应用仍能启动
- Integration: 压测场景下 GC 停顿 < 5ms

**Verification:**
- 启动后通过 `jcmd <pid> GC.heap_info` 确认 GC 算法为 ZGC
- GC 日志中停顿时间 < 5ms

---

- [ ] **Unit 3: 优化 Ehcache 配置**

**Goal:** 按缓存实际用途差异化分配资源，减少内存浪费

**Requirements:** R4, R6

**Dependencies:** 建议在 Unit 1、Unit 2 完成并运行一段时间后执行，以便监控实际缓存使用量

**Files:**
- Modify: `core/core-backend/src/main/resources/ehcache/ehcache.xml`

**Approach:**
根据缓存的实际数据量特征调整资源配置：

| 缓存 | 当前 offheap | 建议调整 | 原因 |
|------|-------------|---------|------|
| `de_v2_rsa` | 10MB | 1MB | RSA 密钥对数据量极小 |
| `de_v2_user_count` | 10MB | 1MB | 仅存 Integer |
| `de_v2_user_roles` | 10MB | 5MB | 用户角色列表，中等数据量 |
| `de_v2_user_busi_pers` | 10MB | 5MB | 业务权限列表 |
| `de_v2_role_busi_pers` | 10MB | 5MB | 角色权限列表 |
| `de_v2_world_map` | 10MB | 10MB | 世界地图数据量较大，保持不变 |
| token 缓存 disk | 200MB | 50MB | 实际 token 数量远小于 200MB |

总体 offheap 从 ~180MB（17×10MB + 10MB token）降至 ~70MB，磁盘从 200MB 降至 50MB。上表仅列出有明确调整建议的缓存，其余 11 个缓存建议统一将 offheap 从 10MB 降至 3MB。

**Test scenarios:**
- Happy path: 应用正常启动，所有缓存功能正常
- Edge case: 高并发登录场景下 token 缓存容量足够
- Integration: 权限缓存清除后能正确重新加载

**Verification:**
- 通过 Ehcache MBean 或 JMX 确认各缓存容量和使用率
- 内存占用较优化前降低

---

- [ ] **Unit 4: 配置数据库连接池参数**

**Goal:** 为 DBCP2 添加显式连接池配置，避免使用默认值

**Requirements:** R5

**Dependencies:** None

**Files:**
- Modify: `core/core-backend/src/main/resources/application-standalone.yml`
- Modify: `core/core-backend/src/main/resources/application-desktop.yml`

**Approach:**
在两个 profile 配置文件中添加 `spring.datasource.dbcp2.*` 参数：

Standalone (MySQL):
- `max-total`: 30（默认 8 过小，30 适合中等并发）
- `max-idle`: 15
- `min-idle`: 5
- `max-wait-millis`: 5000
- `time-between-eviction-runs-millis`: 60000
- `min-evictable-idle-time-millis`: 300000
- `test-while-idle`: true
- `validation-query`: SELECT 1

Desktop (H2):
- `max-total`: 10（单用户场景）
- `max-idle`: 5
- `min-idle`: 2

**Test scenarios:**
- Happy path: 应用正常启动，数据库操作正常
- Edge case: 连接池耗尽时请求返回超时错误而非无限等待
- Integration: 长时间运行后空闲连接被正确回收

**Verification:**
- 应用启动后通过 DBCP2 MBean 确认连接池参数生效
- 数据库端确认活跃连接数符合配置

## System-Wide Impact

- **Interaction graph:** 所有 HTTP 请求处理线程将从平台线程变为虚拟线程，对业务代码透明
- **Error propagation:** ZGC 几乎不产生 stop-the-world，不会影响错误处理超时
- **State lifecycle risks:** Ehcache 资源缩减后，如果缓存数据量超出新配置，会触发 eviction。权限缓存使用 LRU 策略驱逐，不影响正确性
- **API surface parity:** 无 API 变更，所有接口行为不变
- **Unchanged invariants:** 权限缓存的永不过期策略不变，token 缓存的 TTL 机制不变，Calcite 数据源连接池参数不变

## Risks & Dependencies

| Risk | Mitigation |
|------|------------|
| 虚拟线程与 synchronized 代码冲突（pinning） | Java 21 已解决大部分 pinning 问题；如遇问题可回退到平台线程 |
| ZGC 内存开销比 G1 略高 | 通过 MaxRAMPercentage 控制总内存，ZGC 额外开销在可接受范围 |
| Ehcache 资源缩减导致缓存命中率下降 | 先监控实际使用量再调整；可动态修改 ehcache.xml 无需代码变更 |
| 连接池 maxTotal=30 在高并发场景不够 | 可通过配置参数调整，无需代码修改 |
| `run-java.sh` 脚本可能覆盖 JAVA_OPTIONS | 检查 run-java.sh 行为，确认 JAVA_OPTIONS 被正确合并而非覆盖 |

## Documentation / Operational Notes

- Dockerfile 中 `JAVA_OPTIONS` 为累积式环境变量，用户可通过 `-e JAVA_OPTIONS=...` 覆盖或追加
- 建议在生产环境开启 GC 日志：`-Xlog:gc*:file=/opt/dataease2.0/logs/gc.log`
- 优化后建议监控：JVM 堆使用率、GC 停顿时间、数据库活跃连接数、Ehcache 命中率

## Sources & References

- **Origin document:** [backend-performance-optimization-requirements.md](../brainstorms/backend-performance-optimization-requirements.md)
- Related code: `Dockerfile`, `start-backend.bat`, `sdk/common/src/main/java/io/dataease/utils/CommonThreadPool.java`
- Spring Boot 3.3 Virtual Threads: https://docs.spring.io/spring-boot/docs/3.3.x/reference/html/features.html#features.spring-application.virtual-threads
- ZGC in Java 21: https://openjdk.org/jeps/439
