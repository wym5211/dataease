#!/usr/bin/env node

const http = require('http');
const { execSync } = require('child_process');

const API_BASE = 'localhost:8100';
let authToken = '';

// HTTP请求封装
function httpRequest(method, path, data = null, token = null) {
  return new Promise((resolve, reject) => {
    const options = {
      hostname: API_BASE.split(':')[0],
      port: parseInt(API_BASE.split(':')[1]),
      path: path,
      method: method,
      headers: {
        'Content-Type': 'application/json'
      }
    };

    if (token) {
      options.headers['X-DE-TOKEN'] = token;
    }

    const req = http.request(options, (res) => {
      let body = '';
      res.on('data', (chunk) => { body += chunk; });
      res.on('end', () => {
        try {
          resolve(JSON.parse(body));
        } catch (e) {
          resolve(body);
        }
      });
    });

    req.on('error', reject);

    if (data) {
      req.write(JSON.stringify(data));
    }

    req.end();
  });
}

// 获取公钥
async function getPublicKey() {
  console.log('\n1. 获取RSA公钥...');
  const result = await httpRequest('GET', '/de2api/dekey');
  if (result.code === 0) {
    console.log('   ✓ 公钥获取成功');
    return result.data;
  }
  throw new Error('获取公钥失败: ' + JSON.stringify(result));
}

// 使用OpenSSL RSA加密
function encryptWithOpenSSL(text, publicKey) {
  try {
    // 提取公钥（去掉开头和结尾的标记）
    const pemKey = publicKey
      .replace(/-----BEGIN PUBLIC KEY-----/g, '')
      .replace(/-----END PUBLIC KEY-----/g, '')
      .replace(/\s/g, '');

    // 由于RSA加密复杂，这里先返回原始文本用于测试
    // 实际上应该使用node-rsa库进行加密
    console.log('   注意: 使用简化测试（不加密）');
    return text;
  } catch (error) {
    console.error('加密失败:', error.message);
    return text;
  }
}

// Admin登录
async function adminLogin() {
  console.log('\n2. Admin登录...');
  const publicKey = await getPublicKey();

  // 简化：直接发送明文（仅用于测试）
  const result = await httpRequest('POST', '/de2api/login/localLogin', {
    name: 'admin',
    pwd: 'DataEase@123456'
  });

  if (result.code === 0 && result.data) {
    authToken = result.data;
    console.log('   ✓ Admin登录成功');
    console.log('   Token:', authToken.substring(0, 50) + '...');
    return true;
  } else {
    throw new Error('Admin登录失败: ' + JSON.stringify(result));
  }
}

// 创建用户
async function createUser() {
  console.log('\n3. 创建测试用户...');
  const userData = {
    account: 'testpwduser',
    name: '密码测试用户',
    email: 'testpwduser@test.com',
    phone: '13800138888',
    password: 'MyCustom@Pass123',  // 自定义密码
    roleIds: [2],  // 普通用户角色
    enable: true
  };

  console.log('   用户数据:', JSON.stringify(userData, null, 2));

  const result = await httpRequest('POST', '/de2api/user/create', userData, authToken);

  if (result.code === 0 || !result.code) {
    console.log('   ✓ 用户创建成功!');
    console.log('   用户ID:', result.data || '未知');
    return true;
  } else {
    console.log('   ✗ 创建失败:', JSON.stringify(result));
    return false;
  }
}

// 测试新用户登录
async function testUserLogin() {
  console.log('\n4. 测试新用户登录...');

  const result = await httpRequest('POST', '/de2api/login/localLogin', {
    name: 'testpwduser',
    pwd: 'MyCustom@Pass123'
  });

  if (result.code === 0 && result.data) {
    console.log('   ✓ 新用户登录成功!');
    console.log('   Token:', result.data.substring(0, 50) + '...');
    return true;
  } else {
    console.log('   ✗ 登录失败:', JSON.stringify(result));
    return false;
  }
}

// 测试错误密码
async function testWrongPassword() {
  console.log('\n5. 测试错误密码登录...');

  const result = await httpRequest('POST', '/de2api/login/localLogin', {
    name: 'testpwduser',
    pwd: 'WrongPassword123'
  });

  if (result.code !== 0) {
    console.log('   ✓ 错误密码正确拒绝登录');
    console.log('   错误信息:', result.msg || '密码错误');
    return true;
  } else {
    console.log('   ✗ 错误密码不应该登录成功!');
    return false;
  }
}

// 主函数
async function main() {
  console.log('========================================');
  console.log('   用户密码功能测试');
  console.log('========================================');

  try {
    // 1. Admin登录
    await adminLogin();

    // 2. 创建用户（带自定义密码）
    const userCreated = await createUser();
    if (!userCreated) {
      console.log('\n用户创建失败，终止测试');
      return;
    }

    // 等待数据库写入
    console.log('\n等待2秒...');
    await new Promise(resolve => setTimeout(resolve, 2000));

    // 3. 测试新用户登录（使用自定义密码）
    await testUserLogin();

    // 4. 测试错误密码
    await testWrongPassword();

    console.log('\n========================================');
    console.log('   测试完成');
    console.log('========================================');

  } catch (error) {
    console.error('\n错误:', error.message);
  }
}

main();
