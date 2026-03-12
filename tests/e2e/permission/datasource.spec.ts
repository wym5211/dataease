// tests/e2e/permission/datasource.spec.ts

import { UserRole } from './config';
import { TestCase } from './types';
import { buildUrl, createEmptyResult } from './helpers';

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
      name: 'admin 点击新建数据源',
      role: 'admin' as UserRole,
      action: 'clickButton',
      buttonText: '新建',
      expected: '进入数据源创建流程'
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
  ] as TestCase[],

  generateInstructions(): string[] {
    return this.cases.map(tc => {
      switch (tc.action) {
        case 'navigate':
          return `使用 browser_navigate 访问 ${buildUrl(tc.url!)}，使用 browser_snapshot 检查数据源列表是否显示`;
        case 'clickButton':
          return `使用 browser_snapshot 找到"${tc.buttonText}"按钮，使用 browser_click 点击，检查是否进入创建流程`;
        case 'editResource':
          return `使用 browser_snapshot 找到数据源列表中第一个数据源的编辑按钮，使用 browser_click 点击，检查编辑页面是否正常加载`;
        case 'editAuthorizedResource':
          return `使用 browser_snapshot 找到有权限的数据源，点击编辑按钮，验证可正常操作`;
        case 'checkButtonHidden':
          return `使用 browser_snapshot 检查只读数据源的"${tc.buttonText}"按钮是否不可见或被禁用`;
        default:
          return `执行测试: ${tc.name}`;
      }
    });
  },

  createEmptyResult() {
    return createEmptyResult(this.suiteName);
  }
};

export function getDatasourceTestInstructions(): { suiteName: string; cases: TestCase[]; instructions: string[] } {
  return {
    suiteName: datasourceTests.suiteName,
    cases: datasourceTests.cases,
    instructions: datasourceTests.generateInstructions()
  };
}
