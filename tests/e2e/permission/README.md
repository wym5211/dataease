# DataEase 权限自动化测试

## 概述

使用 Playwright MCP 工具实现 DataEase 权限功能的端到端自动化测试。

## 前置条件

1. **服务运行**
   - 前端: `localhost:8090`
   - 后端: `localhost:8100`

2. **测试用户**
   - admin / DataEase@123456
   - testuser / Test@123456 (需要预先创建)

3. **测试数据**
   - 每类资源至少2个（仪表板、大屏、数据集、数据源）
   - 部分资源需要配置为普通用户只读

## 执行测试

### 方式一：生成测试指令

```bash
npx ts-node tests/e2e/permission/run-all.ts
```

然后将输出的指令复制给 Claude，让其通过 MCP Playwright 工具执行。

### 方式二：直接让 Claude 执行

直接告诉 Claude：

> "请执行 DataEase 权限测试，测试文件在 tests/e2e/permission/"

Claude 会读取测试配置并依次执行 MCP 操作。

## 测试覆盖

| 模块 | 测试内容 | 用例数 |
|------|----------|--------|
| login | 登录流程 | 6 |
| menu | 菜单权限 | 4 |
| dashboard | 仪表板访问+编辑 | 7 |
| screen | 大屏访问+编辑 | 7 |
| dataset | 数据集访问+编辑 | 7 |
| datasource | 数据源访问+编辑 | 7 |
| **总计** | | **38** |

## 文件结构

```
tests/e2e/permission/
├── config.ts              # 测试配置（URL、用户凭证）
├── types.ts               # 类型定义
├── helpers.ts             # 通用辅助函数
├── login.spec.ts          # 登录流程测试
├── menu.spec.ts           # 菜单权限测试
├── dashboard.spec.ts      # 仪表板访问+编辑测试
├── screen.spec.ts         # 大屏访问+编辑测试
├── dataset.spec.ts        # 数据集访问+编辑测试
├── datasource.spec.ts     # 数据源访问+编辑测试
├── specs-index.ts         # 测试套件索引
├── report.ts              # 结果汇总报告
├── run-all.ts             # 一键执行入口
├── README.md              # 本文档
└── reports/               # 测试报告输出目录
```

## 报告

测试完成后生成：
- 控制台汇总报告
- JSON 格式报告: `reports/report-<timestamp>.json`
- HTML 格式报告: `reports/report-<timestamp>.html`

## 配置

修改 `config.ts` 可调整：
- 测试环境 URL
- 测试用户凭证
- 超时时间
- 路由配置

## 测试用例说明

### 登录测试 (login.spec.ts)
- 访问登录页面
- admin 用户登录
- 检查 token 存在
- 登出用户
- 普通用户登录

### 菜单权限测试 (menu.spec.ts)
- admin 可见系统管理菜单
- admin 点击用户管理菜单
- 普通用户不可见系统管理菜单
- 普通用户直接访问无权限路由

### 资源权限测试 (dashboard/screen/dataset/datasource)
- admin 访问资源列表
- 普通用户访问资源列表（仅可见授权资源）
- 普通用户直接访问无权限资源
- admin 新建资源
- admin 编辑资源
- 普通用户编辑有权限资源
- 普通用户只读资源无编辑按钮
