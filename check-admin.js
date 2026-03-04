const mysql = require('mysql2/promise');

async function checkAdmin() {
  try {
    const connection = await mysql.createConnection({
      host: '127.0.0.1',
      user: 'root',
      password: '123456',
      database: 'dataease10'
    });

    const [rows] = await connection.execute(
      'SELECT id, username, password, status FROM sys_user WHERE username = ?',
      ['admin']
    );

    if (rows.length > 0) {
      console.log('Admin用户信息:');
      console.log('ID:', rows[0].id);
      console.log('用户名:', rows[0].username);
      console.log('密码哈希:', rows[0].password.substring(0, 60) + '...');
      console.log('状态:', rows[0].status);
    }

    await connection.end();
  } catch (error) {
    console.error('错误:', error.message);
  }
}

checkAdmin();
