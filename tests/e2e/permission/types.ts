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

export interface TestCase {
  id: string;
  name: string;
  role: 'admin' | 'normal';
  action: string;
  expected: string;
  url?: string;
  menuText?: string;
  buttonText?: string;
  credentials?: { username: string; password: string };
}
