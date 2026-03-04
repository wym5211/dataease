// 测试admin登录和菜单API
const http = require('http');

const loginOptions = {
  hostname: 'localhost',
  port: 8100,
  path: '/de2api/login/localLogin',
  method: 'POST',
  headers: {
    'Content-Type': 'application/json'
  }
};

const req = http.request(loginOptions, (res) => {
  let data = '';
  res.on('data', (chunk) => { data += chunk; });
  res.on('end', () => {
    console.log('登录响应:', data);

    try {
      const result = JSON.parse(data);
      if (result.code === 0 && result.data && result.data.token) {
        const token = result.data.token;

        // 使用token获取菜单
        const menuOptions = {
          hostname: 'localhost',
          port: 8100,
          path: '/de2api/menu/query',
          method: 'GET',
          headers: {
            'X-DE-TOKEN': token
          }
        };

        const menuReq = http.request(menuOptions, (menuRes) => {
          let menuData = '';
          menuRes.on('data', (chunk) => { menuData += chunk; });
          menuRes.on('end', () => {
            console.log('\n菜单响应:', menuData);
          });
        });
        menuReq.end();
      }
    } catch (e) {
      console.error('解析错误:', e);
    }
  });
});

req.write(JSON.stringify({username: 'admin', password: 'DataEase@123456'}));
req.end();
