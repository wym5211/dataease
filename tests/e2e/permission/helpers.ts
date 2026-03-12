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

/**
 * 创建空测试结果
 */
export function createEmptyResult(suiteName: string) {
  return {
    suiteName,
    results: [] as TestResult[],
    passed: 0,
    failed: 0,
    skipped: 0,
    duration: 0
  };
}
