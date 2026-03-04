// 测试admin登录 - 使用可能的密码
const http = require('http');

const passwords = ['admin123', 'DataEase@123456', '123456', 'admin', 'Admin@123', 'DataEase'];

function tryLogin(password, index) {
  return new Promise((resolve) => {
    const postData = JSON.stringify({ name: 'admin', pwd: password });

    const options = {
      hostname: 'localhost',
      port: 8100,
      path: '/de2api/login/localLogin',
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Content-Length': Buffer.byteLength(postData)
      }
    };

    const req = http.request(options, (res) => {
      let data = '';
      res.on('data', (chunk) => { data += chunk; });
      res.on('end', () => {
        try {
          const result = JSON.parse(data);
          console.log(`[${index + 1}/${passwords.length}] 密码 "${password}": ${result.msg || result.code}`);

          if (result.code === 0 && result.data && result.data.token) {
            console.log(`\n✅ 登录成功！密码是: ${password}`);
            console.log(`Token: ${result.data.token.substring(0, 50)}...`);

            // 测试菜单API
            testMenuAPI(result.data.token);
            resolve({ success: true, password });
          } else if (result.msg && result.msg.includes('validator')) {
            resolve({ success: false, password, reason: 'validation_error' });
          } else {
            resolve({ success: false, password, reason: result.msg });
          }
        } catch (e) {
          resolve({ success: false, password, error: e.message });
        }
      });
    });

    req.on('error', (error) => {
      resolve({ success: false, password, error: error.message });
    });

    req.write(postData);
    req.end();
  });
}

function testMenuAPI(token) {
  const options = {
    hostname: 'localhost',
    port: 8100,
    path: '/de2api/menu/query',
    method: 'GET',
    headers: {
      'X-DE-TOKEN': token
    }
  };

  const req = http.request(options, (res) => {
    let data = '';
    res.on('data', (chunk) => { data += chunk; });
    res.on('end', () => {
      try {
        const result = JSON.parse(data);
        if (result.code === 0 && result.data) {
          console.log(`\n📋 菜单API响应：`);
          console.log(`菜单总数: ${result.data.length}`);

          const basicMenus = result.data.filter(m => m.id >= 1 && m.id <= 6);
          console.log(`基础菜单（ID 1-6）: ${basicMenus.length}/6`);

          console.log(`\n菜单详细列表：`);
          result.data.forEach(m => {
            const isBasic = m.id >= 1 && m.id <= 6;
            const flag = isBasic ? '🔹' : '  ';
            console.log(`  ${flag} [${m.id}] ${m.name.padEnd(20)} ${m.path}`);
          });

          console.log(`\n基础菜单检查（ID 1-6）：`);
          for (let i = 1; i <= 6; i++) {
            const menu = result.data.find(m => m.id === i);
            if (menu) {
              console.log(`  ✅ [${i}] ${menu.name} (${menu.path})`);
            } else {
              console.log(`  ❌ [${i}] 缺失`);
            }
          }

          if (basicMenus.length === 6) {
            console.log(`\n✅ 成功！admin用户拥有完整的基础菜单`);
          } else {
            console.log(`\n⚠️  警告：缺少基础菜单（需要6个，当前${basicMenus.length}个）`);
          }
        }
      } catch (e) {
        console.error('解析菜单响应失败:', e.message);
      }
    });
  });

  req.on('error', (error) => {
    console.error('菜单API请求失败:', error.message);
  });

  req.end();
}

async function main() {
  console.log('====================================');
  console.log('  Admin用户登录测试');
  console.log('====================================\n');

  for (let i = 0; i < passwords.length; i++) {
    const result = await tryLogin(passwords[i], i);
    if (result.success) {
      return; // 成功后退出
    }
    await new Promise(r => setTimeout(r, 500)); // 延迟500ms
  }

  console.log('\n❌ 所有密码都失败了');
  console.log('\n可能需要重置admin密码...');
}

main();
