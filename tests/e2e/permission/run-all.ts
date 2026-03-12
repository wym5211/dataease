// tests/e2e/permission/run-all.ts

import { config } from './config';
import {
  getLoginTestInstructions,
  getMenuTestInstructions,
  getDashboardTestInstructions,
  getScreenTestInstructions,
  getDatasetTestInstructions,
  getDatasourceTestInstructions
} from './specs-index';
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
      console.log(`${(index + 1).toString().padStart(2)}. ${instruction}`);
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
