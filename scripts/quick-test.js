<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>快速密码测试</title>
    <script src="https://cdn.jsdelivr.net/npm/jsencrypt@3.3.2/bin/jsencrypt.min.js"></script>
</head>
<body>
    <h2>密码功能快速测试</h2>
    <div id="log" style="background:#f0f0f0;padding:10px;white-space:pre-wrap;font-family:monospace;font-size:12px;max-height:400px;overflow-y:auto;"></div>

    <script>
        const log = document.getElementById('log');
        const API_BASE = 'http://localhost:8100/de2api';
        let adminToken = '';

        function addLog(msg) {
            log.textContent += msg + '\n';
            console.log(msg);
        }

        // 获取公钥
        async function getPublicKey() {
            addLog('1. 获取RSA公钥...');
            const res = await fetch(`${API_BASE}/dekey`);
            const data = await res.json();
            if (data.code === 0) {
                addLog('   ✓ 公钥获取成功');
                return data.data;
            }
            throw new Error('获取公钥失败');
        }

        // RSA加密
        function encrypt(text, key) {
            const rsa = new JSEncrypt();
            rsa.setPublicKey(key);
            return rsa.encrypt(text);
        }

        // Admin登录
        async function adminLogin() {
            addLog('\n2. Admin登录...');
            try {
                const publicKey = await getPublicKey();
                const encryptedName = encrypt('admin', publicKey);
                const encryptedPwd = encrypt('DataEase@123456', publicKey);

                const res = await fetch(`${API_BASE}/login/localLogin`, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({
                        name: encryptedName,
                        pwd: encryptedPwd
                    })
                });

                const data = await res.json();
                if (data.code === 0) {
                    adminToken = data.data;
                    addLog('   ✓ Admin登录成功');
                    addLog('   Token: ' + adminToken.substring(0, 50) + '...');
                    return true;
                }
                addLog('   ✗ 登录失败: ' + JSON.stringify(data));
                return false;
            } catch (error) {
                addLog('   ✗ 错误: ' + error.message);
                return false;
            }
        }

        // 创建用户（带自定义密码）
        async function createUser() {
            addLog('\n3. 创建测试用户（带自定义密码）...');
            try {
                const userData = {
                    account: 'testpwd2026v2',
                    name: '密码测试用户V2',
                    email: 'testpwd2026v2@test.com',
                    phone: '13900139999',
                    password: 'MyCustom@Pass789', // 自定义密码
                    roleIds: [2], // 普通用户角色
                    enable: true
                };

                addLog('   用户数据: ' + JSON.stringify(userData, null, 2));

                const res = await fetch(`${API_BASE}/user/create`, {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                        'X-DE-TOKEN': adminToken
                    },
                    body: JSON.stringify(userData)
                });

                const data = await res.json();
                if (data.code === 0 || !data.code) {
                    addLog('   ✓ 用户创建成功!');
                    addLog('   用户ID: ' + (data.data || '未知'));
                    return true;
                }
                addLog('   ✗ 创建失败: ' + JSON.stringify(data));
                return false;
            } catch (error) {
                addLog('   ✗ 错误: ' + error.message);
                return false;
            }
        }

        // 测试新用户登录（使用自定义密码）
        async function testUserLogin() {
            addLog('\n4. 测试新用户登录（使用自定义密码）...');
            try {
                const publicKey = await getPublicKey();
                const encryptedName = encrypt('testpwd2026v2', publicKey);
                const encryptedPwd = encrypt('MyCustom@Pass789', publicKey);

                const res = await fetch(`${API_BASE}/login/localLogin`, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({
                        name: encryptedName,
                        pwd: encryptedPwd
                    })
                });

                const data = await res.json();
                if (data.code === 0 && data.data) {
                    addLog('   ✓ 新用户登录成功!');
                    addLog('   Token: ' + data.data.substring(0, 50) + '...');
                    addLog('\n   ======================');
                    addLog('   ✅ 密码功能正常!');
                    addLog('   ======================');
                    return true;
                }
                addLog('   ✗ 登录失败: ' + JSON.stringify(data));
                return false;
            } catch (error) {
                addLog('   ✗ 错误: ' + error.message);
                return false;
            }
        }

        // 测试错误密码
        async function testWrongPassword() {
            addLog('\n5. 测试错误密码登录（应该失败）...');
            try {
                const publicKey = await getPublicKey();
                const encryptedName = encrypt('testpwd2026v2', publicKey);
                const encryptedPwd = encrypt('WrongPassword123', publicKey);

                const res = await fetch(`${API_BASE}/login/localLogin`, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({
                        name: encryptedName,
                        pwd: encryptedPwd
                    })
                });

                const data = await res.json();
                if (data.code !== 0) {
                    addLog('   ✓ 错误密码被正确拒绝');
                    addLog('   错误信息: ' + (data.msg || '密码错误'));
                    return true;
                }
                addLog('   ✗ 错误密码不应该登录成功!');
                return false;
            } catch (error) {
                addLog('   ✗ 错误: ' + error.message);
                return false;
            }
        }

        // 执行所有测试
        async function runAllTests() {
            addLog('========================================');
            addLog('   密码功能完整测试');
            addLog('========================================');

            const loginSuccess = await adminLogin();
            if (!loginSuccess) {
                addLog('\nAdmin登录失败，无法继续测试');
                return;
            }

            await new Promise(r => setTimeout(r, 1000));

            const userCreated = await createUser();
            if (!userCreated) {
                addLog('\n用户创建失败，无法继续测试');
                return;
            }

            await new Promise(r => setTimeout(r, 2000));

            await testUserLogin();
            await testWrongPassword();

            addLog('\n========================================');
            addLog('   测试完成');
            addLog('========================================');
        }

        // 自动执行测试
        setTimeout(runAllTests, 500);
    </script>
</body>
</html>
