# 备份导入性能优化设计

## 背景

导入 MySQL 数据集后，点击数据集报"请检查数据源的有效性"。手动校验数据源后恢复正常。

### 根因

1. 数据源导入时，`calciteProvider.update()` 异步构建 schema，但 `waitForSchemaReady()` 串行等待每个数据源（最多5秒），不更新数据源状态
2. 数据集导入时，每张表都单独查询数据源名称（N+1 问题）

### 目标

- 功能正确性优先：导入完成后数据源状态正确，点击数据集不报错
- 导入速度提升 3-5 倍（中等规模：10-50 个数据集，5-20 个数据源）

## 方案

### 改动1：数据源导入并行化

**文件**：`BackupDatasourceServiceImpl.java`

**现状**：`importDatasources()` 中每个数据源串行执行 `calciteProvider.update()` + `waitForSchemaReady()`。

**改动**：
- 并行触发所有数据源的 `calciteProvider.update()`
- 使用 `CountDownLatch` 等待所有 schema 构建完成
- 统一检查每个数据源的 schema 就绪状态并更新数据库状态

**预期效果**：10 个数据源从串行 ~50 秒降到并行 ~5 秒。

### 改动2：数据源名称批量查询

**文件**：`BackupDatasetServiceImpl.java`

**现状**：`importDatasetTable()` 中每张表执行 `SELECT * FROM core_datasource WHERE name = ?`。

**改动**：
- `importDatasets()` 入口处，收集所有数据集中所有表引用的数据源名称（去重）
- 一次查询 `WHERE name IN (...)` 获取所有数据源
- 构建 `Map<String, Long>`（name → id），传递给 `importDatasetTable()`
- `importDatasetTable()` 从 Map 取值，不再查数据库

**预期效果**：150 次查询降到 1 次。

## 不改动

- CalciteProvider（schema 构建机制不变）
- 前端交互流程
- 数据库 schema
- 其他模块

## 风险

低。改动集中在 backup 模块内部，不影响其他功能。
