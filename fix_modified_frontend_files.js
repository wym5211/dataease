const fs = require('fs');
const path = require('path');
const { execSync } = require('child_process');

console.log('检查已修改的前端文件...\n');

// 获取所有修改过的文件
const output = execSync('git diff --name-only', { encoding: 'utf8', cwd: 'E:\\cursor\\dataease' });
const modifiedFiles = output.trim().split('\n').filter(f => f.trim() !== '');

const fixedFiles = [];
const extensions = ['.ts', '.vue', '.js', '.tsx', '.jsx'];

modifiedFiles.forEach(filePath => {
  // 只处理前端文件
  if (filePath.includes('core/core-frontend/src')) {
    const ext = path.extname(filePath);

    if (extensions.includes(ext)) {
      const fullPath = path.join('E:\\cursor\\dataease', filePath);

      try {
        let content = fs.readFileSync(fullPath, 'utf8');
        const crCount = (content.match(/\r\n/g) || []).length;

        if (crCount > 0) {
          // 将 CRLF 转换为 LF
          content = content.replace(/\r\n/g, '\n');
          fs.writeFileSync(fullPath, content, 'utf8');
          fixedFiles.push({
            file: filePath,
            crCount: crCount
          });
        }
      } catch (err) {
        console.log('⚠️  无法处理:', filePath, err.message);
      }
    }
  }
});

if (fixedFiles.length > 0) {
  console.log('✅ 已修复以下文件的换行符格式:');
  fixedFiles.forEach(({ file, crCount }) => {
    console.log(`   - ${file} (${crCount} 个 CRLF)`);
  });
  console.log(`\n总计: ${fixedFiles.length} 个文件`);
  console.log('\n✅ 换行符问题已全部修复！前端应该可以正常编译了。');
} else {
  console.log('ℹ️  没有发现需要修复的文件');
}
