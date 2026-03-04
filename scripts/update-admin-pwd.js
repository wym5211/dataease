const http = require('http');

// 生成BCrypt密码（使用Spring Security BCrypt格式）
// 我们知道明文密码 DataEase@123456
// BCrypt hash would be: $2a$10$...

// 实际上，让我们直接让后端处理
// 先不加密直接登录，让后端自动升级

async function testPlainLogin() {
  console.log('尝试明文密码登录（让后端自动升级为BCrypt）...');

  const options = {
    hostname: 'localhost',
    port: 8100,
    path: '/de2api/login/localLogin',
    method: 'POST',
    headers: { 'Content-Type': 'application/json' }
  };

  return new Promise((resolve, reject) => {
    const req = http.request(options, (res) => {
      let body = '';
      res.on('data', (chunk) => { body += chunk; });
      res.on('end', () => {
        const data = JSON.parse(body);
        console.log('响应:', JSON.stringify(data, null, 2));
        resolve(data);
      });
    });

    req.on('error', reject);

    // 发送明文密码
    req.write(JSON.stringify({
      name: 'admin',
      pwd: 'DataEase@123456'
    }));

    req.end();
  });
}

testPlainLogin().then(result => {
  if (result.code === 0) {
    console.log('\n✅ 登录成功! Token:', result.data.substring(0, 50) + '...');
  } else {
    console.log('\n❌ 登录失败');
  }
}).catch(err => {
  console.error('错误:', err.message);
});
