// tests/e2e/permission/login.spec.ts

import { UserRole } from './config';
import { TestCase } from './types';
import { buildUrl, createEmptyResult } from './helpers';

export const loginTests = {
  suiteName: 'login.spec.ts',

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
      name: '登出 admin 用户',
      role: 'admin' as UserRole,
      action: 'logout',
      expected: '成功登出并跳转到登录页'
    },
    {
      id: 'login-05',
      name: '普通用户登录',
      role: 'normal' as UserRole,
      action: 'login',
      credentials: { username: 'testuser', password: 'Test@123456' },
      expected: '登录成功并跳转到首页'
    },
    {
      id: 'login-06',
      name: '检查普通用户 token 存在',
      role: 'normal' as UserRole,
      action: 'checkToken',
      expected: 'localStorage 包含有效 token'
    }
  ] as TestCase[],

  generateInstructions(): string[] {
    return this.cases.map(tc => {
      switch (tc.action) {
        case 'navigate':
          return `使用 browser_navigate 访问 ${buildUrl(tc.url!)}`;
        case 'login':
          return `使用 browser_snapshot 获取页面快照，找到用户名和密码输入框，填写：用户名=${tc.credentials!.username}，密码=${tc.credentials!.password}，然后点击登录按钮`;
        case 'checkToken':
          return `使用 browser_evaluate 执行 'localStorage.getItem(\"DE-TOKEN\")' 检查 token 是否存在`;
        case 'logout':
          return `使用 browser_snapshot 找到用户头像或退出按钮，使用 browser_click 点击退出`;
        default:
          return `执行测试: ${tc.name}`;
      }
    });
  },

  createEmptyResult() {
    return createEmptyResult(this.suiteName);
  }
};

export function getLoginTestInstructions(): { suiteName: string; cases: TestCase[]; instructions: string[] } {
  return {
    suiteName: loginTests.suiteName,
    cases: loginTests.cases,
    instructions: loginTests.generateInstructions()
  };
}
