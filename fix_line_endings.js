const fs = require('fs');
const path = require('path');

const filePath = 'E:\\cursor\\dataease\\core\\core-frontend\\src\\permission.ts';

// 读取文件
let content = fs.readFileSync(filePath, 'utf8');

// 将 CRLF (\r\n) 转换为 LF (\n)
content = content.replace(/\r\n/g, '\n');

// 写回文件
fs.writeFileSync(filePath, content, 'utf8');

console.log('✅ 已修复 permission.ts 的换行符格式');
console.log('   CRLF (\\r\\n) → LF (\\n)');
