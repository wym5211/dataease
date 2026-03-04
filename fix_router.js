const fs = require('fs');

const filePath = 'E:\\cursor\\dataease\\core\\core-frontend\\src\\router\\index.ts';

let content = fs.readFileSync(filePath, 'utf8');
const originalLength = content.length;

// 将 CRLF 转换为 LF
content = content.replace(/\r\n/g, '\n');

if (content.length !== originalLength) {
  fs.writeFileSync(filePath, content, 'utf8');
  console.log('✅ 已修复 router/index.ts 的换行符格式');
  console.log(`   删除了 ${originalLength - content.length} 个字符 (\\r)`);
} else {
  console.log('ℹ️  router/index.ts 已经是正确的换行符格式');
}
