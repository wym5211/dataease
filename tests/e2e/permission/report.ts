// tests/e2e/permission/report.ts

import * as fs from 'fs';
import * as path from 'path';
import { TestReport, TestSuiteResult } from './types';
import { formatDuration } from './helpers';

/**
 * 生成控制台报告
 */
export function printConsoleReport(report: TestReport): void {
  const border = '═'.repeat(58);
  const line = '─'.repeat(58);

  console.log('\n');
  console.log(`╔${border}╗`);
  console.log(`║${center('DataEase 权限测试报告', 56)}║`);
  console.log(`║${center(report.timestamp, 56)}║`);
  console.log(`╠${border}╣`);
  console.log(`║ 测试环境: ${report.environment.padEnd(45)}║`);
  console.log(`║ 总用例数: ${report.totalTests.toString().padEnd(45)}║`);

  const statusLine = `通过: ${report.totalPassed}    失败: ${report.totalFailed}    跳过: ${report.totalSkipped}`;
  console.log(`║ ${statusLine.padEnd(54)}║`);

  console.log(`╠${border}╣`);

  for (const suite of report.suites) {
    const status = suite.failed > 0 ? '✗' : suite.skipped > 0 ? '~' : '✓';
    const total = suite.results.length;
    const suiteLine = `[${status}] ${suite.suiteName.padEnd(22)} ${suite.passed}/${total} ${suite.failed > 0 ? '失败:' + suite.failed : '通过'}`;
    console.log(`║ ${suiteLine.padEnd(54)}║`);

    // 显示失败的测试详情
    for (const result of suite.results) {
      if (result.status === 'fail') {
        console.log(`║     └─ ${result.id} ${truncate(result.name, 28).padEnd(28)}║`);
        if (result.expected) {
          console.log(`║        预期: ${truncate(result.expected, 40).padEnd(40)}║`);
        }
        if (result.actual) {
          console.log(`║        实际: ${truncate(result.actual, 40).padEnd(40)}║`);
        }
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
 * 截断文本
 */
function truncate(text: string, maxLen: number): string {
  if (text.length <= maxLen) return text;
  return text.substring(0, maxLen - 3) + '...';
}

/**
 * 生成 HTML 报告
 */
export function generateHtmlReport(report: TestReport): string {
  return `<!DOCTYPE html>
<html lang="zh-CN">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>DataEase 权限测试报告</title>
  <style>
    * { box-sizing: border-box; }
    body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; margin: 0; padding: 20px; background: #f5f7fa; }
    .container { max-width: 1000px; margin: 0 auto; background: white; padding: 30px; border-radius: 8px; box-shadow: 0 2px 12px rgba(0,0,0,0.1); }
    h1 { color: #333; margin: 0 0 10px 0; border-bottom: 3px solid #409eff; padding-bottom: 15px; }
    .meta { color: #666; margin-bottom: 20px; }
    .summary { display: grid; grid-template-columns: repeat(4, 1fr); gap: 15px; margin: 25px 0; }
    .summary-item { text-align: center; padding: 20px; background: #f8f9fa; border-radius: 8px; }
    .summary-item .number { font-size: 32px; font-weight: bold; margin-bottom: 5px; }
    .summary-item .label { color: #666; font-size: 14px; }
    .pass { color: #67c23a; }
    .fail { color: #f56c6c; }
    .skip { color: #909399; }
    .suite { margin: 20px 0; border: 1px solid #e4e7ed; border-radius: 8px; overflow: hidden; }
    .suite-header { padding: 15px 20px; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; display: flex; justify-content: space-between; align-items: center; }
    .suite-header.suite-fail { background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%); }
    .suite-header.suite-skip { background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%); }
    .suite-content { padding: 0; }
    .test-result { padding: 12px 20px; border-bottom: 1px solid #f0f0f0; display: flex; justify-content: space-between; align-items: center; }
    .test-result:last-child { border-bottom: none; }
    .test-result:hover { background: #fafafa; }
    .test-id { color: #909399; font-size: 12px; margin-right: 10px; }
    .test-name { flex: 1; }
    .status-icon { font-size: 18px; font-weight: bold; width: 30px; text-align: center; }
    .error-detail { margin-top: 10px; padding: 12px; background: #fef0f0; border-radius: 4px; font-size: 13px; color: #f56c6c; }
    .error-detail div { margin: 5px 0; }
  </style>
</head>
<body>
  <div class="container">
    <h1>🛡️ DataEase 权限测试报告</h1>
    <div class="meta">
      <div>📅 测试时间: ${report.timestamp}</div>
      <div>🌐 测试环境: ${report.environment}</div>
      <div>⏱️ 总耗时: ${formatDuration(report.totalDuration)}</div>
    </div>

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

    ${report.suites.map(suite => {
      const statusClass = suite.failed > 0 ? 'suite-fail' : suite.skipped > 0 ? 'suite-skip' : '';
      return `
      <div class="suite">
        <div class="suite-header ${statusClass}">
          <strong>${suite.suiteName}</strong>
          <span>${suite.passed}/${suite.results.length} 通过</span>
        </div>
        <div class="suite-content">
          ${suite.results.map(r => `
          <div class="test-result">
            <div>
              <span class="test-id">${r.id}</span>
              <span class="test-name">${r.name}</span>
              ${r.status === 'fail' && r.error ? `
              <div class="error-detail">
                ${r.expected ? `<div>预期: ${r.expected}</div>` : ''}
                ${r.actual ? `<div>实际: ${r.actual}</div>` : ''}
                <div>错误: ${r.error}</div>
              </div>` : ''}
            </div>
            <span class="status-icon ${r.status}">${getStatusIcon(r.status)}</span>
          </div>`).join('')}
        </div>
      </div>`;
    }).join('')}
  </div>
</body>
</html>`;
}

function getStatusIcon(status: string): string {
  switch (status) {
    case 'pass': return '✓';
    case 'fail': return '✗';
    case 'skip': return '○';
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

  const timestamp = new Date().toISOString().replace(/[:.]/g, '-').substring(0, 19);

  // 保存 JSON 报告
  const jsonPath = path.join(outputDir, `report-${timestamp}.json`);
  fs.writeFileSync(jsonPath, JSON.stringify(report, null, 2));

  // 保存 HTML 报告
  const htmlPath = path.join(outputDir, `report-${timestamp}.html`);
  fs.writeFileSync(htmlPath, generateHtmlReport(report));

  console.log(`📄 报告已保存:`);
  console.log(`   JSON: ${jsonPath}`);
  console.log(`   HTML: ${htmlPath}`);
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
