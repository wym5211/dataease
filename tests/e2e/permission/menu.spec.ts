// tests/e2e/permission/menu.spec.ts

import { UserRole } from './config';
import { TestCase } from './types';
import { buildUrl, createEmptyResult } from './helpers';

export const menuTests = {
  suiteName: 'menu.spec.ts',

  cases: [
    {
      id: 'menu-01',
      name: 'admin 可见系统管理菜单',
      role: 'admin' as UserRole,
      action: 'checkMenuVisible',
      menuText: '系统管理',
      expected: '菜单项可见'
    },
    {
      id: 'menu-02',
      name: 'admin 点击用户管理菜单',
      role: 'admin' as UserRole,
      action: 'clickMenu',
      menuText: '用户管理',
      expected: '页面跳转到 /system/user，正常加载'
    },
    {
      id: 'menu-03',
      name: '普通用户不可见系统管理菜单',
      role: 'normal' as UserRole,
      action: 'checkMenuHidden',
      menuText: '系统管理',
      expected: '菜单项不可见'
    },
    {
      id: 'menu-04',
      name: '普通用户直接访问 /system/user',
      role: 'normal' as UserRole,
      action: 'navigate',
      url: '/system/user',
      expected: '跳转到 401 页面或首页'
    }
  ] as TestCase[],

  generateInstructions(): string[] {
    return this.cases.map(tc => {
      switch (tc.action) {
        case 'checkMenuVisible':
          return `使用 browser_snapshot 获取页面快照，检查菜单"${tc.menuText}"是否存在，预期结果：${tc.expected}`;
        case 'clickMenu':
          return `使用 browser_snapshot 找到菜单"${tc.menuText}"，然后用 browser_click 点击，检查 URL 是否变为 /system/user`;
        case 'checkMenuHidden':
          return `使用 browser_snapshot 获取页面快照，检查菜单"${tc.menuText}"是否不存在，预期结果：${tc.expected}`;
        case 'navigate':
          return `使用 browser_navigate 访问 ${buildUrl(tc.url!)}，检查是否被重定向到 401 或首页`;
        default:
          return `执行测试: ${tc.name}`;
      }
    });
  },

  createEmptyResult() {
    return createEmptyResult(this.suiteName);
  }
};

export function getMenuTestInstructions(): { suiteName: string; cases: TestCase[]; instructions: string[] } {
  return {
    suiteName: menuTests.suiteName,
    cases: menuTests.cases,
    instructions: menuTests.generateInstructions()
  };
}
