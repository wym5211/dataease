// tests/e2e/permission/dashboard.spec.ts

import { UserRole } from './config';
import { TestCase } from './types';
import { buildUrl, createEmptyResult } from './helpers';

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
      expected: '可见仪表板列表'
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
      url: '/dashboard/preview/999999',
      expected: '显示无权限提示或跳转'
    },
    // 编辑权限测试
    {
      id: 'dashboard-04',
      name: 'admin 点击新建仪表板',
      role: 'admin' as UserRole,
      action: 'clickButton',
      buttonText: '新建',
      expected: '进入仪表板创建/编辑器页面'
    },
    {
      id: 'dashboard-05',
      name: 'admin 编辑已有仪表板',
      role: 'admin' as UserRole,
      action: 'editResource',
      expected: '可正常编辑，有保存按钮'
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
  ] as TestCase[],

  generateInstructions(): string[] {
    return this.cases.map(tc => {
      switch (tc.action) {
        case 'navigate':
          return `使用 browser_navigate 访问 ${buildUrl(tc.url!)}，使用 browser_snapshot 检查仪表板列表是否显示`;
        case 'navigateUnauthorized':
          return `使用 browser_navigate 访问 ${buildUrl(tc.url!)}，使用 browser_snapshot 检查是否显示无权限提示或被重定向`;
        case 'clickButton':
          return `使用 browser_snapshot 找到"${tc.buttonText}"按钮，使用 browser_click 点击，检查是否进入编辑器`;
        case 'editResource':
          return `使用 browser_snapshot 找到仪表板列表中第一个仪表板的编辑按钮，使用 browser_click 点击，检查编辑器是否正常加载`;
        case 'editAuthorizedResource':
          return `使用 browser_snapshot 找到有权限的仪表板，点击编辑按钮，验证编辑器可正常操作`;
        case 'checkButtonHidden':
          return `使用 browser_snapshot 检查只读仪表板的"${tc.buttonText}"按钮是否不可见或被禁用`;
        default:
          return `执行测试: ${tc.name}`;
      }
    });
  },

  createEmptyResult() {
    return createEmptyResult(this.suiteName);
  }
};

export function getDashboardTestInstructions(): { suiteName: string; cases: TestCase[]; instructions: string[] } {
  return {
    suiteName: dashboardTests.suiteName,
    cases: dashboardTests.cases,
    instructions: dashboardTests.generateInstructions()
  };
}
