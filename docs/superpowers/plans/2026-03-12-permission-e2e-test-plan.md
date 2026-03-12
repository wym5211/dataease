# DataEase 权限自动化测试实现计划

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 使用 Playwright MCP 工具实现 DataEase 权限功能的端到端自动化测试

**Architecture:** 基于 MCP Playwright 工具操作本地 Chrome 浏览器，通过 TypeScript 编写测试脚本，支持多角色对比测试，失败继续执行，最后生成汇总报告。

**Tech Stack:** TypeScript, MCP Playwright, Node.js

---

## 文件结构

```
tests/e2e/permission/
├── config.ts              # 测试配置（URL、用户凭证、超时）
├── types.ts               # 类型定义（TestResult, Report等）
├── helpers.ts             # 通用辅助函数（登录、截图、等待）
├── login.spec.ts          # 登录流程测试
├── menu.spec.ts           # 菜单权限测试
├── dashboard.spec.ts      # 仪表板访问+编辑测试
├── screen.spec.ts         # 大屏访问+编辑测试
├── dataset.spec.ts        # 数据集访问+编辑测试
├── datasource.spec.ts     # 数据源访问+编辑测试
├── report.ts              # 结果汇总报告生成
└── run-all.ts             # 一键执行入口
```

---

## Chunk 1: 基础设施搭建

### Task 1: 创建测试目录和配置文件

**Files:**
- Create: `tests/e2e/permission/config.ts`
- Create: `tests/e2e/permission/types.ts`

- [ ] **Step 1: 创建测试目录**

```bash
mkdir -p tests/e2e/permission
```

- [ ] **Step 2: 创建配置文件 config.ts**

```typescript
// tests/e2e/permission/config.ts

export const config = {
  // 测试环境配置
  baseUrl: 'http://localhost:8090',

  // 测试用户
  users: {
    admin: {
      username: 'admin',
      password: 'DataEase@123456',
      displayName: '管理员'
    },
    normal: {
      username: 'testuser',
      password: 'Test@123456',
      displayName: '普通用户'
    }
  },

  // 超时配置（毫秒）
  timeout: {
    pageLoad: 10000,
    element: 5000,
    action: 3000
  },

  // 报告输出路径
  reportPath: 'tests/e2e/permission/reports',

  // 路由配置
  routes: {
    login: '/login',
    home: '/',
    dashboard: '/dashboard',
    screen: '/screen',
    dataset: '/dataset',
    datasource: '/datasource',
    systemUser: '/system/user'
  }
};

export type UserRole = 'admin' | 'normal';
export type TestUser = typeof config.users.admin;
```

- [ ] **Step 3: 创建类型定义 types.ts**

```typescript
// tests/e2e/permission/types.ts

export type TestStatus = 'pass' | 'fail' | 'skip';

export interface TestResult {
  id: string;
  name: string;
  status: TestStatus;
  duration: number;
  error?: string;
  expected?: string;
  actual?: string;
}

export interface TestSuiteResult {
  suiteName: string;
  results: TestResult[];
  passed: number;
  failed: number;
  skipped: number;
  duration: number;
}

export interface TestReport {
  timestamp: string;
  environment: string;
  totalSuites: number;
  totalTests: number;
  totalPassed: number;
  totalFailed: number;
  totalSkipped: number;
  totalDuration: number;
  suites: TestSuiteResult[];
}

export interface TestContext {
  userRole: string;
  isLoggedIn: boolean;
  currentUrl: string;
}
```

- [ ] **Step 4: 验证文件创建成功**

```bash
ls -la tests/e2e/permission/
```

Expected: 显示 config.ts 和 types.ts 文件

- [ ] **Step 5: 提交**

```bash
git add tests/e2e/permission/config.ts tests/e2e/permission/types.ts
git commit -m "test(e2e): 添加权限测试配置和类型定义"
```

---

### Task 2: 创建辅助函数模块

**Files:**
- Create: `tests/e2e/permission/helpers.ts`

- [ ] **Step 1: 创建辅助函数文件**

```typescript
// tests/e2e/permission/helpers.ts

import { config, UserRole, TestUser } from './config';
import { TestResult, TestStatus } from './types';

/**
 * 生成唯一测试ID
 */
export function generateTestId(suite: string, index: number): string {
  return `${suite}-${index.toString().padStart(2, '0')}`;
}

/**
 * 记录测试结果
 */
export function recordResult(
  id: string,
  name: string,
  status: TestStatus,
  startTime: number,
  error?: string,
  expected?: string,
  actual?: string
): TestResult {
  return {
    id,
    name,
    status,
    duration: Date.now() - startTime,
    error,
    expected,
    actual
  };
}

/**
 * 获取测试用户配置
 */
export function getUser(role: UserRole): TestUser {
  return config.users[role];
}

/**
 * 构建完整URL
 */
export function buildUrl(path: string): string {
  return `${config.baseUrl}${path}`;
}

/**
 * 格式化持续时间
 */
export function formatDuration(ms: number): string {
  if (ms < 1000) return `${ms}ms`;
  if (ms < 60000) return `${(ms / 1000).toFixed(1)}s`;
  return `${(ms / 60000).toFixed(1)}m`;
}

/**
 * 延迟函数
 */
export function delay(ms: number): Promise<void> {
  return new Promise(resolve => setTimeout(resolve, ms));
}

/**
 * 安全执行测试，捕获异常
 */
export async function safeTest<T>(
  testFn: () => Promise<T>
): Promise<{ success: boolean; data?: T; error?: string }> {
  try {
    const data = await testFn();
    return { success: true, data };
  } catch (error) {
    return { success: false, error: String(error) };
  }
}
```

- [ ] **Step 2: 验证文件创建成功**

```bash
cat tests/e2e/permission/helpers.ts | head -20
```

Expected: 显示 helpers.ts 文件内容

- [ ] **Step 3: 提交**

```bash
git add tests/e2e/permission/helpers.ts
git commit -m "test(e2e): 添加权限测试辅助函数"
```

---

## Chunk 2: 核心测试模块

### Task 3: 创建登录测试模块

**Files:**
- Create: `tests/e2e/permission/login.spec.ts`

- [ ] **Step 1: 创建登录测试文件**

```typescript
// tests/e2e/permission/login.spec.ts

import { config, UserRole } from './config';
import { TestResult, TestSuiteResult } from './types';
import { generateTestId, recordResult, getUser, buildUrl, delay } from './helpers';

// MCP Playwright 工具将在运行时通过 Claude 调用
// 此文件定义测试逻辑，实际执行由 run-all.ts 协调

export const loginTests = {
  suiteName: 'login.spec.ts',

  /**
   * 测试用例定义
   */
  cases: [
    {
      id: 'login-01',
      name: '访问登录页面',
      role: 'admin' as UserRole,
      action: 'navigate',
      url: '/login',
      expected: '显示登录表单'
    },
    {
      id: 'login-02',
      name: 'admin 用户登录',
      role: 'admin' as UserRole,
      action: 'login',
      credentials: { username: 'admin', password: 'DataEase@123456' },
      expected: '登录成功并跳转到首页'
    },
    {
      id: 'login-03',
      name: '检查 admin token 存在',
      role: 'admin' as UserRole,
      action: 'checkToken',
      expected: 'localStorage 包含有效 token'
    },
    {
      id: 'login-04',
      name: '普通用户登录',
      role: 'normal' as UserRole,
      action: 'login',
      credentials: { username: 'testuser', password: 'Test@123456' },
      expected: '登录成功并跳转到首页'
    }
  ],

  /**
   * 生成测试执行指令（供 Claude 执行）
   */
  generateInstructions(): string[] {
    return this.cases.map(tc => {
      switch (tc.action) {
        case 'navigate':
          return `使用 browser_navigate 访问 ${buildUrl(tc.url!)}`;
        case 'login':
          return `使用 browser_snapshot 获取页面快照，然后填写登录表单：用户名=${tc.credentials!.username}，密码=${tc.credentials!.password}`;
        case 'checkToken':
          return `使用 browser_evaluate 执行 localStorage.getItem('token') 检查 token 是否存在`;
        default:
          return `执行测试: ${tc.name}`;
      }
    });
  },

  /**
   * 创建空结果对象
   */
  createEmptyResult(): TestSuiteResult {
    return {
      suiteName: this.suiteName,
      results: [],
      passed: 0,
      failed: 0,
      skipped: 0,
      duration: 0
    };
  }
};

/**
 * 登录测试执行器
 * 返回测试指令列表，由 run-all.ts 调用 Claude MCP 工具执行
 */
export function getLoginTestInstructions(): { suiteName: string; instructions: string[] } {
  return {
    suiteName: loginTests.suiteName,
    instructions: loginTests.generateInstructions()
  };
}
```

- [ ] **Step 2: 验证语法正确**

```bash
cd tests/e2e/permission && npx tsc --noEmit login.spec.ts --skipLibCheck 2>&1 || echo "语法检查完成"
```

Expected: 无致命错误

- [ ] **Step 3: 提交**

```bash
git add tests/e2e/permission/login.spec.ts
git commit -m "test(e2e): 添加登录流程测试用例"
```

---

### Task 4: 创建菜单权限测试模块

**Files:**
- Create: `tests/e2e/permission/menu.spec.ts`

- [ ] **Step 1: 创建菜单权限测试文件**

```typescript
// tests/e2e/permission/menu.spec.ts

import { UserRole } from './config';
import { TestSuiteResult } from './types';
import { buildUrl } from './helpers';

export const menuTests = {
  suiteName: 'menu.spec.ts',

  cases: [
    {
      id: 'menu-01',
      name: 'admin 可见系统管理菜单',
      role: 'admin' as UserRole,
      action: 'checkMenuVisible',
      menuText: '系统管理',
      expected: '可见'
    },
    {
      id: 'menu-02',
      name: 'admin 点击用户管理菜单',
      role: 'admin' as UserRole,
      action: 'clickMenu',
      menuText: '用户管理',
      expected: '页面正常加载，URL 包含 /system/user'
    },
    {
      id: 'menu-03',
      name: '普通用户不可见系统管理菜单',
      role: 'normal' as UserRole,
      action: 'checkMenuHidden',
      menuText: '系统管理',
      expected: '不可见'
    },
    {
      id: 'menu-04',
      name: '普通用户直接访问 /system/user',
      role: 'normal' as UserRole,
      action: 'navigate',
      url: '/system/user',
      expected: '跳转到 401 页面或首页'
    }
  ],

  generateInstructions(): string[] {
    return this.cases.map(tc => {
      switch (tc.action) {
        case 'checkMenuVisible':
          return `使用 browser_snapshot 获取页面快照，检查菜单"${tc.menuText}"是否存在，预期：${tc.expected}`;
        case 'clickMenu':
          return `使用 browser_snapshot 找到菜单"${tc.menuText}"，然后用 browser_click 点击它，检查 URL 是否变化`;
        case 'checkMenuHidden':
          return `使用 browser_snapshot 获取页面快照，检查菜单"${tc.menuText}"是否不存在，预期：${tc.expected}`;
        case 'navigate':
          return `使用 browser_navigate 访问 ${buildUrl(tc.url!)}，检查是否被重定向到 401 或首页`;
        default:
          return `执行测试: ${tc.name}`;
      }
    });
  },

  createEmptyResult(): TestSuiteResult {
    return {
      suiteName: this.suiteName,
      results: [],
      passed: 0,
      failed: 0,
      skipped: 0,
      duration: 0
    };
  }
};

export function getMenuTestInstructions(): { suiteName: string; instructions: string[] } {
  return {
    suiteName: menuTests.suiteName,
    instructions: menuTests.generateInstructions()
  };
}
```

- [ ] **Step 2: 验证语法正确**

```bash
cd tests/e2e/permission && npx tsc --noEmit menu.spec.ts --skipLibCheck 2>&1 || echo "语法检查完成"
```

- [ ] **Step 3: 提交**

```bash
git add tests/e2e/permission/menu.spec.ts
git commit -m "test(e2e): 添加菜单权限测试用例"
```

---

### Task 5: 创建仪表板权限测试模块

**Files:**
- Create: `tests/e2e/permission/dashboard.spec.ts`

- [ ] **Step 1: 创建仪表板测试文件**

```typescript
// tests/e2e/permission/dashboard.spec.ts

import { UserRole } from './config';
import { TestSuiteResult } from './types';
import { buildUrl } from './helpers';

export const dashboardTests = {
  suiteName: 'dashboard.spec.ts',

  cases: [
    // 访问权限测试
    {
      id: 'dashboard-01',
      name: 'admin 访问仪表板列表',
      role: 'admin' as UserRole,
      action: 'navigate',
      url: '/dashboard',
      expected: '可见所有仪表板'
    },
    {
      id: 'dashboard-02',
      name: '普通用户访问仪表板列表',
      role: 'normal' as UserRole,
      action: 'navigate',
      url: '/dashboard',
      expected: '仅可见被授权的仪表板'
    },
    {
      id: 'dashboard-03',
      name: '普通用户直接访问无权限仪表板',
      role: 'normal' as UserRole,
      action: 'navigateUnauthorized',
      url: '/dashboard/unauthorized-id',
      expected: '显示无权限提示'
    },
    // 编辑权限测试
    {
      id: 'dashboard-04',
      name: 'admin 新建仪表板',
      role: 'admin' as UserRole,
      action: 'clickButton',
      buttonText: '新建',
      expected: '进入编辑器页面'
    },
    {
      id: 'dashboard-05',
      name: 'admin 编辑已有仪表板',
      role: 'admin' as UserRole,
      action: 'editResource',
      expected: '可正常编辑保存'
    },
    {
      id: 'dashboard-06',
      name: '普通用户编辑有权限仪表板',
      role: 'normal' as UserRole,
      action: 'editAuthorizedResource',
      expected: '可正常编辑保存'
    },
    {
      id: 'dashboard-07',
      name: '普通用户只读仪表板无编辑按钮',
      role: 'normal' as UserRole,
      action: 'checkButtonHidden',
      buttonText: '编辑',
      expected: '编辑按钮不可见或禁用'
    }
  ],

  generateInstructions(): string[] {
    return this.cases.map(tc => {
      switch (tc.action) {
        case 'navigate':
          return `使用 browser_navigate 访问 ${buildUrl(tc.url!)}，使用 browser_snapshot 检查仪表板列表`;
        case 'navigateUnauthorized':
          return `使用 browser_navigate 访问 ${buildUrl(tc.url!)}，检查是否显示无权限提示`;
        case 'clickButton':
          return `使用 browser_snapshot 找到"${tc.buttonText}"按钮，使用 browser_click 点击`;
        case 'editResource':
          return `使用 browser_snapshot 找到仪表板列表，点击编辑按钮，检查是否进入编辑器`;
        case 'editAuthorizedResource':
          return `使用 browser_snapshot 找到有权限的仪表板，点击编辑按钮，验证可正常操作`;
        case 'checkButtonHidden':
          return `使用 browser_snapshot 检查只读仪表板的"${tc.buttonText}"按钮是否不可见或禁用`;
        default:
          return `执行测试: ${tc.name}`;
      }
    });
  },

  createEmptyResult(): TestSuiteResult {
    return {
      suiteName: this.suiteName,
      results: [],
      passed: 0,
      failed: 0,
      skipped: 0,
      duration: 0
    };
  }
};

export function getDashboardTestInstructions(): { suiteName: string; instructions: string[] } {
  return {
    suiteName: dashboardTests.suiteName,
    instructions: dashboardTests.generateInstructions()
  };
}
```

- [ ] **Step 2: 提交**

```bash
git add tests/e2e/permission/dashboard.spec.ts
git commit -m "test(e2e): 添加仪表板权限测试用例"
```

---

### Task 6: 创建大屏权限测试模块

**Files:**
- Create: `tests/e2e/permission/screen.spec.ts`

- [ ] **Step 1: 创建大屏测试文件**

```typescript
// tests/e2e/permission/screen.spec.ts

import { UserRole } from './config';
import { TestSuiteResult } from './types';
import { buildUrl } from './helpers';

export const screenTests = {
  suiteName: 'screen.spec.ts',

  cases: [
    // 访问权限测试
    {
      id: 'screen-01',
      name: 'admin 访问大屏列表',
      role: 'admin' as UserRole,
      action: 'navigate',
      url: '/screen',
      expected: '可见所有大屏'
    },
    {
      id: 'screen-02',
      name: '普通用户访问大屏列表',
      role: 'normal' as UserRole,
      action: 'navigate',
      url: '/screen',
      expected: '仅可见被授权的大屏'
    },
    {
      id: 'screen-03',
      name: '普通用户直接访问无权限大屏',
      role: 'normal' as UserRole,
      action: 'navigateUnauthorized',
      url: '/screen/unauthorized-id',
      expected: '显示无权限提示'
    },
    // 编辑权限测试
    {
      id: 'screen-04',
      name: 'admin 新建大屏',
      role: 'admin' as UserRole,
      action: 'clickButton',
      buttonText: '新建',
      expected: '进入大屏编辑器'
    },
    {
      id: 'screen-05',
      name: 'admin 编辑已有大屏',
      role: 'admin' as UserRole,
      action: 'editResource',
      expected: '可正常编辑保存'
    },
    {
      id: 'screen-06',
      name: '普通用户编辑有权限大屏',
      role: 'normal' as UserRole,
      action: 'editAuthorizedResource',
      expected: '可正常编辑保存'
    },
    {
      id: 'screen-07',
      name: '普通用户只读大屏无编辑按钮',
      role: 'normal' as UserRole,
      action: 'checkButtonHidden',
      buttonText: '编辑',
      expected: '编辑按钮不可见或禁用'
    }
  ],

  generateInstructions(): string[] {
    return this.cases.map(tc => {
      switch (tc.action) {
        case 'navigate':
          return `使用 browser_navigate 访问 ${buildUrl(tc.url!)}，使用 browser_snapshot 检查大屏列表`;
        case 'navigateUnauthorized':
          return `使用 browser_navigate 访问 ${buildUrl(tc.url!)}，检查是否显示无权限提示`;
        case 'clickButton':
          return `使用 browser_snapshot 找到"${tc.buttonText}"按钮，使用 browser_click 点击`;
        case 'editResource':
          return `使用 browser_snapshot 找到大屏列表，点击编辑按钮，检查是否进入编辑器`;
        case 'editAuthorizedResource':
          return `使用 browser_snapshot 找到有权限的大屏，点击编辑按钮，验证可正常操作`;
        case 'checkButtonHidden':
          return `使用 browser_snapshot 检查只读大屏的"${tc.buttonText}"按钮是否不可见或禁用`;
        default:
          return `执行测试: ${tc.name}`;
      }
    });
  },

  createEmptyResult(): TestSuiteResult {
    return {
      suiteName: this.suiteName,
      results: [],
      passed: 0,
      failed: 0,
      skipped: 0,
      duration: 0
    };
  }
};

export function getScreenTestInstructions(): { suiteName: string; instructions: string[] } {
  return {
    suiteName: screenTests.suiteName,
    instructions: screenTests.generateInstructions()
  };
}
```

- [ ] **Step 2: 提交**

```bash
git add tests/e2e/permission/screen.spec.ts
git commit -m "test(e2e): 添加大屏权限测试用例"
```

---

### Task 7: 创建数据集权限测试模块

**Files:**
- Create: `tests/e2e/permission/dataset.spec.ts`

- [ ] **Step 1: 创建数据集测试文件**

```typescript
// tests/e2e/permission/dataset.spec.ts

import { UserRole } from './config';
import { TestSuiteResult } from './types';
import { buildUrl } from './helpers';

export const datasetTests = {
  suiteName: 'dataset.spec.ts',

  cases: [
    // 访问权限测试
    {
      id: 'dataset-01',
      name: 'admin 访问数据集列表',
      role: 'admin' as UserRole,
      action: 'navigate',
      url: '/dataset',
      expected: '可见所有数据集'
    },
    {
      id: 'dataset-02',
      name: '普通用户访问数据集列表',
      role: 'normal' as UserRole,
      action: 'navigate',
      url: '/dataset',
      expected: '仅可见被授权的数据集'
    },
    {
      id: 'dataset-03',
      name: '普通用户尝试编辑无权限数据集',
      role: 'normal' as UserRole,
      action: 'checkButtonHidden',
      buttonText: '编辑',
      expected: '操作按钮不可见或禁用'
    },
    // 编辑权限测试
    {
      id: 'dataset-04',
      name: 'admin 新建数据集',
      role: 'admin' as UserRole,
      action: 'clickButton',
      buttonText: '新建',
      expected: '进入创建流程'
    },
    {
      id: 'dataset-05',
      name: 'admin 编辑已有数据集',
      role: 'admin' as UserRole,
      action: 'editResource',
      expected: '可修改并保存'
    },
    {
      id: 'dataset-06',
      name: '普通用户编辑有权限数据集',
      role: 'normal' as UserRole,
      action: 'editAuthorizedResource',
      expected: '可正常编辑保存'
    },
    {
      id: 'dataset-07',
      name: '普通用户尝试删除无权限数据集',
      role: 'normal' as UserRole,
      action: 'checkButtonHidden',
      buttonText: '删除',
      expected: '删除按钮不可见或禁用'
    }
  ],

  generateInstructions(): string[] {
    return this.cases.map(tc => {
      switch (tc.action) {
        case 'navigate':
          return `使用 browser_navigate 访问 ${buildUrl(tc.url!)}，使用 browser_snapshot 检查数据集列表`;
        case 'clickButton':
          return `使用 browser_snapshot 找到"${tc.buttonText}"按钮，使用 browser_click 点击`;
        case 'editResource':
          return `使用 browser_snapshot 找到数据集列表，点击编辑按钮，检查是否可编辑`;
        case 'editAuthorizedResource':
          return `使用 browser_snapshot 找到有权限的数据集，点击编辑按钮，验证可正常操作`;
        case 'checkButtonHidden':
          return `使用 browser_snapshot 检查无权限数据集的"${tc.buttonText}"按钮是否不可见或禁用`;
        default:
          return `执行测试: ${tc.name}`;
      }
    });
  },

  createEmptyResult(): TestSuiteResult {
    return {
      suiteName: this.suiteName,
      results: [],
      passed: 0,
      failed: 0,
      skipped: 0,
      duration: 0
    };
  }
};

export function getDatasetTestInstructions(): { suiteName: string; instructions: string[] } {
  return {
    suiteName: datasetTests.suiteName,
    instructions: datasetTests.generateInstructions()
  };
}
```

- [ ] **Step 2: 提交**

```bash
git add tests/e2e/permission/dataset.spec.ts
git commit -m "test(e2e): 添加数据集权限测试用例"
```

---

### Task 8: 创建数据源权限测试模块

**Files:**
- Create: `tests/e2e/permission/datasource.spec.ts`

- [ ] **Step 1: 创建数据源测试文件**

```typescript
// tests/e2e/permission/datasource.spec.ts

import { UserRole } from './config';
import { TestSuiteResult } from './types';
import { buildUrl } from './helpers';

export const datasourceTests = {
  suiteName: 'datasource.spec.ts',

  cases: [
    // 访问权限测试
    {
      id: 'datasource-01',
      name: 'admin 访问数据源列表',
      role: 'admin' as UserRole,
      action: 'navigate',
      url: '/datasource',
      expected: '可见所有数据源'
    },
    {
      id: 'datasource-02',
      name: '普通用户访问数据源列表',
      role: 'normal' as UserRole,
      action: 'navigate',
      url: '/datasource',
      expected: '仅可见被授权的数据源'
    },
    {
      id: 'datasource-03',
      name: '普通用户尝试删除无权限数据源',
      role: 'normal' as UserRole,
      action: 'checkButtonHidden',
      buttonText: '删除',
      expected: '删除按钮不可见或禁用'
    },
    // 编辑权限测试
    {
      id: 'datasource-04',
      name: 'admin 新建数据源',
      role: 'admin' as UserRole,
      action: 'clickButton',
      buttonText: '新建',
      expected: '进入创建流程'
    },
    {
      id: 'datasource-05',
      name: 'admin 编辑已有数据源',
      role: 'admin' as UserRole,
      action: 'editResource',
      expected: '可修改并保存'
    },
    {
      id: 'datasource-06',
      name: '普通用户编辑有权限数据源',
      role: 'normal' as UserRole,
      action: 'editAuthorizedResource',
      expected: '可正常编辑保存'
    },
    {
      id: 'datasource-07',
      name: '普通用户尝试编辑只读数据源',
      role: 'normal' as UserRole,
      action: 'checkButtonHidden',
      buttonText: '编辑',
      expected: '编辑按钮不可见或禁用'
    }
  ],

  generateInstructions(): string[] {
    return this.cases.map(tc => {
      switch (tc.action) {
        case 'navigate':
          return `使用 browser_navigate 访问 ${buildUrl(tc.url!)}，使用 browser_snapshot 检查数据源列表`;
        case 'clickButton':
          return `使用 browser_snapshot 找到"${tc.buttonText}"按钮，使用 browser_click 点击`;
        case 'editResource':
          return `使用 browser_snapshot 找到数据源列表，点击编辑按钮，检查是否可编辑`;
        case 'editAuthorizedResource':
          return `使用 browser_snapshot 找到有权限的数据源，点击编辑按钮，验证可正常操作`;
        case 'checkButtonHidden':
          return `使用 browser_snapshot 检查只读数据源的"${tc.buttonText}"按钮是否不可见或禁用`;
        default:
          return `执行测试: ${tc.name}`;
      }
    });
  },

  createEmptyResult(): TestSuiteResult {
    return {
      suiteName: this.suiteName,
      results: [],
      passed: 0,
      failed: 0,
      skipped: 0,
      duration: 0
    };
  }
};

export function getDatasourceTestInstructions(): { suiteName: string; instructions: string[] } {
  return {
    suiteName: datasourceTests.suiteName,
    instructions: datasourceTests.generateInstructions()
  };
}
```

- [ ] **Step 2: 提交**

```bash
git add tests/e2e/permission/datasource.spec.ts
git commit -m "test(e2e): 添加数据源权限测试用例"
```

---

## Chunk 3: 报告与执行器

### Task 9: 创建报告生成模块

**Files:**
- Create: `tests/e2e/permission/report.ts`

- [ ] **Step 1: 创建报告生成文件**

```typescript
// tests/e2e/permission/report.ts

import * as fs from 'fs';
import * as path from 'path';
import { TestReport, TestSuiteResult, TestResult } from './types';
import { formatDuration } from './helpers';

/**
 * 生成控制台报告
 */
export function printConsoleReport(report: TestReport): void {
  const border = '═'.repeat(60);
  const line = '─'.repeat(60);

  console.log('\n');
  console.log(`╔${border}╗`);
  console.log(`║${center('DataEase 权限测试报告', 58)}║`);
  console.log(`║${center(report.timestamp, 58)}║`);
  console.log(`╠${border}╣`);
  console.log(`║ 测试环境: ${report.environment.padEnd(46)}║`);
  console.log(`║ 总用例数: ${report.totalTests.toString().padEnd(46)}║`);

  const statusLine = `通过: ${report.totalPassed}    失败: ${report.totalFailed}    跳过: ${report.totalSkipped}`;
  console.log(`║ ${statusLine.padEnd(56)}║`);

  console.log(`╠${border}╣`);

  for (const suite of report.suites) {
    const status = suite.failed > 0 ? '✗' : suite.skipped > 0 ? '~' : '✓';
    const suiteLine = `[${status}] ${suite.suiteName.padEnd(20)} ${suite.passed}/${suite.results.length} ${suite.failed > 0 ? '失败:' + suite.failed : '通过'}`;
    console.log(`║ ${suiteLine.padEnd(56)}║`);

    // 显示失败的测试详情
    for (const result of suite.results) {
      if (result.status === 'fail') {
        console.log(`║     └─ ${result.id} ${result.name.padEnd(30)}║`);
        console.log(`║        预期: ${result.expected?.substring(0, 40).padEnd(40)}║`);
        console.log(`║        实际: ${result.actual?.substring(0, 40).padEnd(40)}║`);
      }
    }
  }

  console.log(`╚${border}╝`);
  console.log('\n');
}

/**
 * 文本居中
 */
function center(text: string, width: number): string {
  const padding = Math.max(0, width - text.length);
  const left = Math.floor(padding / 2);
  const right = padding - left;
  return ' '.repeat(left) + text + ' '.repeat(right);
}

/**
 * 生成 HTML 报告
 */
export function generateHtmlReport(report: TestReport): string {
  return `<!DOCTYPE html>
<html lang="zh-CN">
<head>
  <meta charset="UTF-8">
  <title>DataEase 权限测试报告</title>
  <style>
    body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; margin: 40px; background: #f5f5f5; }
    .container { max-width: 900px; margin: 0 auto; background: white; padding: 30px; border-radius: 8px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }
    h1 { color: #333; border-bottom: 2px solid #409eff; padding-bottom: 10px; }
    .summary { display: grid; grid-template-columns: repeat(4, 1fr); gap: 15px; margin: 20px 0; }
    .summary-item { text-align: center; padding: 15px; background: #f8f9fa; border-radius: 6px; }
    .summary-item .number { font-size: 28px; font-weight: bold; }
    .summary-item .label { color: #666; font-size: 14px; }
    .pass { color: #67c23a; }
    .fail { color: #f56c6c; }
    .skip { color: #909399; }
    .suite { margin: 20px 0; border: 1px solid #eee; border-radius: 6px; }
    .suite-header { padding: 15px; background: #f8f9fa; border-bottom: 1px solid #eee; display: flex; justify-content: space-between; }
    .suite-content { padding: 15px; }
    .test-result { padding: 10px 0; border-bottom: 1px solid #f0f0f0; display: flex; justify-content: space-between; }
    .test-result:last-child { border-bottom: none; }
    .status-icon { font-weight: bold; }
    .error-detail { margin-top: 10px; padding: 10px; background: #fef0f0; border-radius: 4px; font-size: 13px; }
  </style>
</head>
<body>
  <div class="container">
    <h1>DataEase 权限测试报告</h1>
    <p>测试时间: ${report.timestamp}</p>
    <p>测试环境: ${report.environment}</p>

    <div class="summary">
      <div class="summary-item">
        <div class="number">${report.totalTests}</div>
        <div class="label">总用例数</div>
      </div>
      <div class="summary-item">
        <div class="number pass">${report.totalPassed}</div>
        <div class="label">通过</div>
      </div>
      <div class="summary-item">
        <div class="number fail">${report.totalFailed}</div>
        <div class="label">失败</div>
      </div>
      <div class="summary-item">
        <div class="number skip">${report.totalSkipped}</div>
        <div class="label">跳过</div>
      </div>
    </div>

    ${report.suites.map(suite => `
    <div class="suite">
      <div class="suite-header">
        <strong>${suite.suiteName}</strong>
        <span class="${suite.failed > 0 ? 'fail' : 'pass'}">${suite.passed}/${suite.results.length} 通过</span>
      </div>
      <div class="suite-content">
        ${suite.results.map(r => `
        <div class="test-result">
          <span>${r.id} ${r.name}</span>
          <span class="status-icon ${r.status}">${getStatusIcon(r.status)}</span>
          ${r.status === 'fail' ? `
          <div class="error-detail">
            <div>预期: ${r.expected}</div>
            <div>实际: ${r.actual}</div>
          </div>` : ''}
        </div>`).join('')}
      </div>
    </div>`).join('')}
  </div>
</body>
</html>`;
}

function getStatusIcon(status: string): string {
  switch (status) {
    case 'pass': return '✓';
    case 'fail': return '✗';
    case 'skip': return '~';
    default: return '?';
  }
}

/**
 * 保存报告到文件
 */
export function saveReport(report: TestReport, outputDir: string): void {
  // 确保目录存在
  if (!fs.existsSync(outputDir)) {
    fs.mkdirSync(outputDir, { recursive: true });
  }

  // 保存 JSON 报告
  const jsonPath = path.join(outputDir, `report-${Date.now()}.json`);
  fs.writeFileSync(jsonPath, JSON.stringify(report, null, 2));

  // 保存 HTML 报告
  const htmlPath = path.join(outputDir, `report-${Date.now()}.html`);
  fs.writeFileSync(htmlPath, generateHtmlReport(report));

  console.log(`报告已保存到: ${outputDir}`);
}

/**
 * 创建汇总报告
 */
export function createReport(suiteResults: TestSuiteResult[]): TestReport {
  const totalTests = suiteResults.reduce((sum, s) => sum + s.results.length, 0);
  const totalPassed = suiteResults.reduce((sum, s) => sum + s.passed, 0);
  const totalFailed = suiteResults.reduce((sum, s) => sum + s.failed, 0);
  const totalSkipped = suiteResults.reduce((sum, s) => sum + s.skipped, 0);
  const totalDuration = suiteResults.reduce((sum, s) => sum + s.duration, 0);

  return {
    timestamp: new Date().toLocaleString('zh-CN'),
    environment: 'http://localhost:8090',
    totalSuites: suiteResults.length,
    totalTests,
    totalPassed,
    totalFailed,
    totalSkipped,
    totalDuration,
    suites: suiteResults
  };
}
```

- [ ] **Step 2: 提交**

```bash
git add tests/e2e/permission/report.ts
git commit -m "test(e2e): 添加测试报告生成模块"
```

---

### Task 10: 创建主执行器

**Files:**
- Create: `tests/e2e/permission/run-all.ts`

- [ ] **Step 1: 创建执行器文件**

```typescript
// tests/e2e/permission/run-all.ts

import { config } from './config';
import { TestReport, TestSuiteResult, TestResult } from './types';
import {
  getLoginTestInstructions,
  getMenuTestInstructions,
  getDashboardTestInstructions,
  getScreenTestInstructions,
  getDatasetTestInstructions,
  getDatasourceTestInstructions
} from './specs';
import { printConsoleReport, saveReport, createReport } from './report';

/**
 * DataEase 权限测试执行器
 *
 * 使用说明:
 * 1. 确保前端服务运行在 localhost:8090
 * 2. 确保后端服务运行在 localhost:8100
 * 3. 确保测试用户存在（admin 和 testuser）
 * 4. 运行此脚本，按照提示执行 MCP 操作
 */

async function main() {
  console.log('╔════════════════════════════════════════════════════════╗');
  console.log('║           DataEase 权限自动化测试                       ║');
  console.log('╚════════════════════════════════════════════════════════╝');
  console.log('\n');

  // 检查服务状态
  console.log('📋 前置检查:');
  console.log(`   - 前端服务: ${config.baseUrl}`);
  console.log(`   - 后端服务: http://localhost:8100`);
  console.log(`   - 测试用户: admin, testuser`);
  console.log('\n');

  // 收集所有测试指令
  const allTestSuites = [
    getLoginTestInstructions(),
    getMenuTestInstructions(),
    getDashboardTestInstructions(),
    getScreenTestInstructions(),
    getDatasetTestInstructions(),
    getDatasourceTestInstructions()
  ];

  // 输出测试计划
  console.log('📝 测试计划:');
  let totalTests = 0;
  for (const suite of allTestSuites) {
    console.log(`   - ${suite.suiteName}: ${suite.instructions.length} 个测试`);
    totalTests += suite.instructions.length;
  }
  console.log(`\n   共计: ${totalTests} 个测试用例\n`);

  // 输出执行指令
  console.log('─'.repeat(60));
  console.log('执行以下测试指令（复制给 Claude 执行 MCP 操作）:');
  console.log('─'.repeat(60));
  console.log('\n');

  for (const suite of allTestSuites) {
    console.log(`\n### ${suite.suiteName}`);
    console.log('```');
    suite.instructions.forEach((instruction, index) => {
      console.log(`${index + 1}. ${instruction}`);
    });
    console.log('```\n');
  }

  console.log('─'.repeat(60));
  console.log('测试指令输出完成');
  console.log('请将上述指令复制给 Claude，让其通过 MCP Playwright 工具执行');
  console.log('─'.repeat(60));
}

// 导出测试套件供外部调用
export {
  getLoginTestInstructions,
  getMenuTestInstructions,
  getDashboardTestInstructions,
  getScreenTestInstructions,
  getDatasetTestInstructions,
  getDatasourceTestInstructions
};

// 运行主函数
main().catch(console.error);
```

- [ ] **Step 2: 创建 specs 索引文件**

```typescript
// tests/e2e/permission/specs/index.ts

export { getLoginTestInstructions } from '../login.spec';
export { getMenuTestInstructions } from '../menu.spec';
export { getDashboardTestInstructions } from '../dashboard.spec';
export { getScreenTestInstructions } from '../screen.spec';
export { getDatasetTestInstructions } from '../dataset.spec';
export { getDatasourceTestInstructions } from '../datasource.spec';
```

- [ ] **Step 3: 修正 run-all.ts 的导入路径**

```typescript
// 修改 run-all.ts 中的导入
import {
  getLoginTestInstructions,
  getMenuTestInstructions,
  getDashboardTestInstructions,
  getScreenTestInstructions,
  getDatasetTestInstructions,
  getDatasourceTestInstructions
} from './login.spec'; // 临时从各文件导入
```

实际上需要分别导入：

```typescript
import { getLoginTestInstructions } from './login.spec';
import { getMenuTestInstructions } from './menu.spec';
import { getDashboardTestInstructions } from './dashboard.spec';
import { getScreenTestInstructions } from './screen.spec';
import { getDatasetTestInstructions } from './dataset.spec';
import { getDatasourceTestInstructions } from './datasource.spec';
```

- [ ] **Step 4: 提交**

```bash
git add tests/e2e/permission/run-all.ts
git commit -m "test(e2e): 添加测试执行器入口"
```

---

### Task 11: 创建 README 文档

**Files:**
- Create: `tests/e2e/permission/README.md`

- [ ] **Step 1: 创建 README**

```markdown
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
| login | 登录流程 | 4 |
| menu | 菜单权限 | 4 |
| dashboard | 仪表板访问+编辑 | 7 |
| screen | 大屏访问+编辑 | 7 |
| dataset | 数据集访问+编辑 | 7 |
| datasource | 数据源访问+编辑 | 7 |
| **总计** | | **36** |

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
```

- [ ] **Step 2: 提交**

```bash
git add tests/e2e/permission/README.md
git commit -m "test(e2e): 添加权限测试 README 文档"
```

---

## 完成检查清单

- [ ] 所有测试文件创建完成
- [ ] 配置文件正确
- [ ] 报告模块可正常生成报告
- [ ] README 文档完整
- [ ] 所有文件已提交到 git

---

**计划完成。准备执行？**
