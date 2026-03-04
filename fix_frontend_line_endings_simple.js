const fs = require('fs');
const path = require('path');

const filesToFix = [
  'E:\\cursor\\dataease\\core\\core-frontend\\src\\permission.ts',
  'E:\\cursor\\dataease\\core\\core-frontend\\src\\router\\index.ts',
  'E:\\cursor\\dataease\\core\\core-frontend\\src\\views\\login\\index.vue',
  'E:\\cursor\\dataease\\core\\core-frontend\\src\\layout\\components\\Header.vue',
  'E:\\cursor\\dataease\\core\\core-frontend\\src\\views\\permissions\\index.vue',
  'E:\\cursor\\dataease\\core\\core-frontend\\src\\views\\permissions\\menu\\index.vue',
  'E:\\cursor\\dataease\\core\\core-frontend\\src\\views\\permissions\\resource\\index.vue',
  'E:\\cursor\\dataease\\core\\core-frontend\\src\\views\\permissions\\template\\index.vue',
  'E:\\cursor\\dataease\\core\\core-frontend\\src\\views\\permissions\\audit\\index.vue',
  'E:\\cursor\\dataease\\core\\core-frontend\\src\\views\\permissions\\user\\api\\adapter.ts'
];

let fixedCount = 0;

filesToFix.forEach(filePath => {
  try {
    if (fs.existsSync(filePath)) {
      let content = fs.readFileSync(filePath, 'utf8');
      const crCount = (content.match(/\r\n/g) || []).length;

      if (crCount > 0) {
        content = content.replace(/\r\n/g, '\n');
        fs.writeFileSync(filePath, content, 'utf8');
        console.log(`✅ ${path.basename(filePath)}: 删除 ${crCount} 个 CRLF`);
        fixedCount++;
      } else {
        console.log(`ℹ️  ${path.basename(filePath)}: 已经是 LF 格式`);
      }
    }
  } catch (err) {
    console.log(`⚠️  ${path.basename(filePath)}: ${err.message}`);
  }
});

console.log(`\n总计修复: ${fixedCount} 个文件`);
