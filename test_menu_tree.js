// 测试菜单树结构
const http = require('http');

function login(password) {
  return new Promise((resolve, reject) => {
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
          if (result.code === 0 && result.data && result.data.token) {
            resolve(result.data.token);
          } else {
            reject(new Error(result.msg || '登录失败'));
          }
        } catch (e) {
          reject(e);
        }
      });
    });

    req.on('error', reject);
    req.write(postData);
    req.end();
  });
}

function printMenuTree(menus, indent = 0) {
  const prefix = '  '.repeat(indent);
  menus.forEach(menu => {
    const hasChildren = menu.children && menu.children.length > 0;
    console.log(`${prefix}[${menu.id}] ${menu.name}${hasChildren ? ' (+'.concat(menu.children.length, ' children)') : ''}`);

    if (hasChildren) {
      printMenuTree(menu.children, indent + 1);
    }
  });
}

function countAllMenus(menus) {
  let count = 0;
  menus.forEach(menu => {
    count++;
    if (menu.children && menu.children.length > 0) {
      count += countAllMenus(menu.children);
    }
  });
  return count;
}

function findMenu(menus, menuId) {
  for (const menu of menus) {
    if (menu.id === menuId) return menu;
    if (menu.children && menu.children.length > 0) {
      const found = findMenu(menu.children, menuId);
      if (found) return found;
    }
  }
  return null;
}

async function main() {
  try {
    console.log('登录admin用户...\n');
    const token = await login('123456');
    console.log('✅ 登录成功！\n');

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
            console.log('========================================');
            console.log('  菜单树结构');
            console.log('========================================\n');

            printMenuTree(result.data);

            const totalCount = countAllMenus(result.data);
            console.log(`\n========================================`);
            console.log(`  根菜单数量: ${result.data.length}`);
            console.log(`  总菜单数量（含子菜单）: ${totalCount}`);
            console.log('========================================\n');

            // 检查菜单4、5、6
            console.log('检查基础菜单（ID 4-6）：');
            const menu4 = findMenu(result.data, 4);
            if (menu4) {
              console.log(`  ✅ 菜单4 (data): ${menu4.children ? '有 ' + menu4.children.length + ' 个子菜单' : '没有子菜单'}`);
              if (menu4.children && menu4.children.length > 0) {
                console.log('      子菜单列表:');
                menu4.children.forEach(child => {
                  console.log(`        [${child.id}] ${child.name}`);
                });
              }
            } else {
              console.log('  ❌ 菜单4 (data) 不存在');
            }

            const menu5 = findMenu(result.data, 5);
            console.log(`  ${menu5 ? '✅' : '❌'} 菜单5 (dataset): ${menu5 ? '存在' : '不存在'}`);

            const menu6 = findMenu(result.data, 6);
            console.log(`  ${menu6 ? '✅' : '❌'} 菜单6 (datasource): ${menu6 ? '存在' : '不存在'}`);

            if (menu4 && menu4.children && menu4.children.length >= 2) {
              console.log('\n✅ 成功！admin用户拥有完整的数据准备菜单（包含数据集和数据源）');
            } else {
              console.log('\n❌ 失败！数据准备菜单不完整');
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
  } catch (error) {
    console.error('测试失败:', error.message);
  }
}

main();
