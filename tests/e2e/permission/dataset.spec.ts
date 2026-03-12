// tests/e2e/permission/dataset.spec.ts

import { UserRole } from './config';
import { TestCase } from './types';
import { buildUrl, createEmptyResult } from './helpers';

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
      name: '普通用户尝试操作无权限数据集',
      role: 'normal' as UserRole,
      action: 'checkButtonHidden',
      buttonText: '编辑',
      expected: '操作按钮不可见或禁用'
    },
    // 编辑权限测试
    {
      id: 'dataset-04',
      name: 'admin 点击新建数据集',
      role: 'admin' as UserRole,
      action: 'clickButton',
      buttonText: '新建',
      expected: '进入数据集创建流程'
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
  ] as TestCase[],

  generateInstructions(): string[] {
    return this.cases.map(tc => {
      switch (tc.action) {
        case 'navigate':
          return `使用 browser_navigate 访问 ${buildUrl(tc.url!)}，使用 browser_snapshot 检查数据集列表是否显示`;
        case 'clickButton':
          return `使用 browser_snapshot 找到"${tc.buttonText}"按钮，使用 browser_click 点击，检查是否进入创建流程`;
        case 'editResource':
          return `使用 browser_snapshot 找到数据集列表中第一个数据集的编辑按钮，使用 browser_click 点击，检查编辑页面是否正常加载`;
        case 'editAuthorizedResource':
          return `使用 browser_snapshot 找到有权限的数据集，点击编辑按钮，验证可正常操作`;
        case 'checkButtonHidden':
          return `使用 browser_snapshot 检查无权限数据集的"${tc.buttonText}"按钮是否不可见或被禁用`;
        default:
          return `执行测试: ${tc.name}`;
      }
    });
  },

  createEmptyResult() {
    return createEmptyResult(this.suiteName);
  }
};

export function getDatasetTestInstructions(): { suiteName: string; cases: TestCase[]; instructions: string[] } {
  return {
    suiteName: datasetTests.suiteName,
    cases: datasetTests.cases,
    instructions: datasetTests.generateInstructions()
  };
}
