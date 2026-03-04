const { chromium } = require('playwright');

(async () => {
  const browser = await chromium.launch({ headless: false, slowMo: 100 });
  const context = await browser.newContext();
  const page = await context.newPage();

  console.log('=== 开始测试用户999登录 ===');

  try {
    // 访问登录页面
    await page.goto('http://localhost:8081');
    console.log('1. 打开登录页面');
    await page.waitForTimeout(2000);

    // 等待页面加载完成
    await page.waitForSelector('input[placeholder*="账号"]', { timeout: 10000 });
    console.log('2. 找到账号输入框');

    // 填写登录表单
    await page.fill('input[placeholder*="账号"]', '999');
    console.log('3. 输入账号: 999');

    await page.fill('input[placeholder*="密码"]', '123456');
    console.log('4. 输入密码: 123456');

    // 点击登录按钮
    await page.click('button:has-text("登录")');
    console.log('5. 点击登录按钮');

    // 等待登录结果
    await page.waitForTimeout(3000);

    // 检查是否登录成功（通过检查URL或页面元素）
    const url = page.url();
    console.log('6. 当前URL:', url);

    // 截图保存
    await page.screenshot({ path: 'E:/cursor/dataease/test-login-999-result.png', fullPage: true });
    console.log('7. 已保存截图: test-login-999-result.png');

    // 检查localStorage中的token
    const token = await page.evaluate(() => localStorage.getItem('user.token'));
    console.log('8. Token存在:', token ? '是' : '否');
    if (token) {
      console.log('   Token前50字符:', token.substring(0, 50));
    }

    // 检查页面是否显示工作台或仪表盘
    const pageContent = await page.content();
    const successIndicators = ['工作台', '仪表盘', '数据大屏', '数据源', '退出'];
    const foundIndicator = successIndicators.find(indicator => pageContent.includes(indicator));

    if (foundIndicator) {
      console.log('✅ 登录成功! 页面包含:', foundIndicator);
    } else if (url.includes('login')) {
      console.log('❌ 登录失败，仍在登录页面');

      // 查找错误信息
      const errorMsg = await page.$eval('.el-message--error, .error-message, .login-error', el => el.textContent).catch(() => null);
      if (errorMsg) {
        console.log('   错误信息:', errorMsg);
      }
    } else {
      console.log('⚠️ 无法确定登录状态，请查看截图');
    }

  } catch (error) {
    console.error('测试出错:', error.message);
    await page.screenshot({ path: 'E:/cursor/dataease/test-login-999-error.png', fullPage: true });
  } finally {
    await browser.close();
    console.log('\n=== 测试完成 ===');
  }
})();
