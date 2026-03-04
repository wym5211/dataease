const fs = require('fs');
const path = require('path');

function findFiles(dir, extensions) {
  let files = [];

  try {
    const items = fs.readdirSync(dir);

    for (const item of items) {
      const fullPath = path.join(dir, item);
      const stat = fs.statSync(fullPath);

      if (stat.isDirectory()) {
        // 跳过 node_modules 和隐藏目录
        if (!item.startsWith('.') && item !== 'node_modules' && item !== 'dist') {
          files = files.concat(findFiles(fullPath, extensions));
        }
      } else if (stat.isFile()) {
        const ext = path.extname(item);
        if (extensions.includes(ext)) {
          files.push(fullPath);
        }
      }
    }
  } catch (err) {
    // 忽略无法访问的目录
  }

  return files;
}

const frontendDir = 'E:\\\\cursor\\\\dataease\\\\core\\\\core-frontend\\\\src';
const extensions = ['.ts', '.vue', '.js', '.tsx', '.jsx'];

console.log('扫描前端文件...\n');

const files = findFiles(frontendDir, extensions);
console.log(`找到 ${files.length} 个文件\n`);

let fixedCount = 0;
let totalReplaced = 0;

files.forEach(filePath => {
  try {
    let content = fs.readFileSync(filePath, 'utf8');
    const crCount = (content.match(/\r\n/g) || []).length;

    if (crCount > 0) {
      content = content.replace(/\r\n/g, '\n');
      fs.writeFileSync(filePath, content, 'utf8');
      fixedCount++;
      totalReplaced += crCount;

      if (fixedCount <= 10) {
        const relativePath = path.relative('E:\\\\cursor\\\\dataease', filePath);
        console.log(`✅ ${relativePath}: ${crCount} 个 CRLF`);
      }
    }
  } catch (err) {
    // 忽略无法读取的文件
  }
});

console.log(`\n${fixedCount > 10 ? '...' : ''}`);
console.log(`\n📊 统计:`);
console.log(`   修复文件数: ${fixedCount}`);
console.log(`   替换CRLF数: ${totalReplaced}`);
console.log(`\n✅ 完成！所有前端文件换行符已修复为 LF 格式`);
