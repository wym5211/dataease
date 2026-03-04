#!/usr/bin/env node

const http = require('http');

// 测试多种密码登录
const passwords = ['DataEase@123456', '123456', 'admin', 'Admin@123'];

function tryLogin(password, index) {
  return new Promise((resolve) => {
    const options = {
      hostname: 'localhost',
      port: 8100,
      path: '/de2api/login/localLogin',
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      }
    };

    const req = http.request(options, (res) => {
      let data = '';
      res.on('data', (chunk) => { data += chunk; });
      res.on('end', () => {
        try {
          const result = JSON.parse(data);
          console.log(`密码 ${index + 1}. "${password}": ${result.msg}`);
          if (result.code === 0 && result.data) {
            resolve({ success: true, password, data: result });
          } else {
            resolve({ success: false, password });
          }
        } catch (e) {
          resolve({ success: false, password, error: e.message });
        }
      });
    });

    req.on('error', (error) => {
      resolve({ success: false, password, error: error.message });
    });

    req.write(JSON.stringify({ username: 'admin', password: password }));
    req.end();
  });
}

async function testPasswords() {
  console.log('测试admin登录...\n');

  for (let i = 0; i < passwords.length; i++) {
    const result = await tryLogin(passwords[i], i);
    if (result.success) {
      console.log(`\n成功！密码是: ${result.password}`);
      console.log('Token:', result.data.token);

      // 测试菜单API
      const token = result.data.token;
      testMenuAPI(token);
      return;
    }
    await new Promise(r => setTimeout(r, 1000));
  }

  console.log('\n所有密码都失败了');
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
      console.log('\n菜单API响应:');
      console.log(data);

      try {
        const result = JSON.parse(data);
        if (result.code === 0 && result.data) {
          console.log(`\n菜单数量: ${result.data.length}`);
          const menuNames = result.data.map(m => `ID ${m.id}: ${m.name} (${m.path})`);
          console.log('菜单列表:');
          menuNames.forEach(name => console.log('  ' + name));

          const hasBasicMenus = result.data.some(m => m.id >= 1 && m.id <= 6);
          if (hasBasicMenus) {
            console.log('\n✅ 成功！包含基础菜单（ID 1-6）');
          } else {
            console.log('\n❌ 失败！仍然缺少基础菜单（ID 1-6）');
          }
        }
      } catch (e) {
        console.error('解析菜单响应失败:', e);
      }
    });
  });

  req.on('error', (error) => {
    console.error('菜单API请求失败:', error);
  });

  req.end();
}

testPasswords();
