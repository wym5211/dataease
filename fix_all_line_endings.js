const fs = require('fs');
const path = require('path');
const { execSync } = require('child_process');

// 获取所有修改过的前端文件
const output = execSync('cd E:\\\\cursor\\\\dataease && git status --short', { encoding: 'utf8' });
const lines = output.trim().split('\n');

const fixedFiles = [];

lines.forEach(line => {
  const status = line.substring(0, 2);
  const filePath = line.substring(3);

  // 只处理修改过的前端文件（.ts, .vue, .js）
  if (status.includes('M') && filePath.includes('core/core-frontend/src')) {
    if (filePath.endsWith('.ts') || filePath.endsWith('.vue') || filePath.endsWith('.js')) {
      const fullPath = path.join('E:\\cursor\\dataease', filePath);

      try {
        let content = fs.readFileSync(fullPath, 'utf8');
        const originalLength = content.length;

        // 将 CRLF 转换为 LF
        content = content.replace(/\r\n/g, '\n');

        if (content.length !== originalLength) {
          fs.writeFileSync(fullPath, content, 'utf8');
          fixedFiles.push(filePath);
        }
      } catch (err) {
        console.log('⚠️  无法处理:', filePath, err.message);
      }
    }
  }
});

if (fixedFiles.length > 0) {
  console.log('✅ 已修复以下文件的换行符格式:');
  fixedFiles.forEach(file => console.log(`   - ${file}`));
  console.log(`\n总计: ${fixedFiles.length} 个文件`);
} else {
  console.log('ℹ️  没有需要修复的文件');
}
