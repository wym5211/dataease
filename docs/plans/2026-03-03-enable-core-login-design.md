# 启用 CoreLoginServer 支持普通用户登录设计方案

日期: 2026-03-03

## 1. 问题分析

### 1.1 当前行为
DataEase 社区版在 `standalone` 模式下使用 `SubstituleLoginServer` 作为登录实现：
- 仅支持 `admin` 用户登录
- 硬编码用户名和密码验证
- 不支持数据库用户验证

### 1.2 问题根源
Spring Bean 加载顺序：
1. `SubstituleLoginServer` 使用 `@ConditionalOnMissingBean(name = "loginServer")`
2. `CoreLoginServer` 使用 `@Service("loginServer")` 和 `@Primary`
3. 当前 `standalone` 模式下，`CoreLoginServer` 未被正确扫描加载

## 2. 解决方案

### 2.1 目标
修改 `CoreLoginServer` 的配置，确保在 `standalone` 模式下优先加载，替代社区版登录实现。

### 2.2 具体改动

**文件:** `core/core-backend/src/main/java/io/dataease/core/permissions/login/CoreLoginServer.java`

**修改内容:**
1. 添加 `@ConditionalOnProperty` 注解，强制在 standalone 模式下启用
2. 或者调整类扫描路径，确保被 Spring 扫描

**推荐方案:**
```java
@Service("loginServer")
@Primary
@ConditionalOnProperty(name = "spring.profiles.active",
                       havingValue = "standalone",
                       matchIfMissing = true)
@RestController
public class CoreLoginServer implements LoginApi {
    // ... 现有代码保持不变
}
```

### 2.3 架构图

```
┌─────────────────────────────────────────────────────────────┐
│                     standalone 模式                          │
└─────────────────────────────────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────┐
│  修改前:                                                    │
│  ┌─────────────────────┐                                    │
│  │ SubstituleLoginServer│ ← @ConditionalOnMissingBean       │
│  │ (仅支持 admin 登录)  │   被加载                          │
│  └─────────────────────┘                                    │
│                           ↑                                  │
│  ┌─────────────────────┐  │                                  │
│  │   CoreLoginServer   │  │ 未被扫描                        │
│  │ (支持数据库用户)     │  │                                  │
│  └─────────────────────┘                                    │
└─────────────────────────────────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────┐
│  修改后:                                                    │
│  ┌─────────────────────┐                                    │
│  │   CoreLoginServer   │ ← @Primary + @ConditionalOnProperty │
│  │ (支持数据库用户)     │   优先加载                         │
│  └─────────────────────┘                                    │
│                           ↓                                  │
│  ┌─────────────────────┐                                    │
│  │ SubstituleLoginServer│ 被 CoreLoginServer 替代           │
│  └─────────────────────┘                                    │
└─────────────────────────────────────────────────────────────┘
```

## 3. 技术细节

### 3.1 登录流程
```
用户输入用户名/密码
        ↓
前端加密 (RSA)
        ↓
POST /login/localLogin
        ↓
CoreLoginServer.localLogin()
        ↓
RSA 解密用户名/密码
        ↓
查询数据库 sys_user 表
        ↓
BCryptPasswordEncoder 验证密码
        ↓
生成 JWT Token
        ↓
返回 TokenVO
```

### 3.2 密码验证逻辑
- 如果密码以 `$2a$` 或 `$2b$` 开头：使用 BCrypt 验证
- 否则：明文比较，并自动升级为 BCrypt 加密

### 3.3 用户状态检查
- 检查 `user.status` 字段
- `status = 0` 表示禁用，禁止登录

## 4. 测试计划

### 4.1 测试场景
1. **admin 用户登录** - 验证管理员仍可正常登录
2. **新建用户登录** - 创建测试用户，验证能否登录
3. **密码重置后登录** - 重置密码后验证登录
4. **禁用用户登录** - 验证禁用用户无法登录

### 4.2 验证点
- [ ] 前端登录页面正常显示
- [ ] 用户创建成功
- [ ] 密码加密存储正确
- [ ] 新建用户能正常登录
- [ ] JWT Token 正确生成
- [ ] 登录后页面跳转正确

## 5. 风险与回滚

### 5.1 风险
- 修改可能影响现有 admin 登录
- 可能影响其他依赖登录状态的功能

### 5.2 回滚方案
- 还原 `CoreLoginServer.java` 的修改
- 重新启动后端服务即可恢复

## 6. 相关文件

| 文件 | 说明 |
|------|------|
| `CoreLoginServer.java` | 需要修改的主要文件 |
| `SubstituleLoginServer.java` | 社区版登录实现（无需修改）|
| `SysUserMapper.java` | 用户数据访问层 |
| `application-standalone.yml` | standalone 配置文件 |

---

**设计批准:** 待批准
