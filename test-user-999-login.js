const { chromium } = require('playwright');

(async () => {
  const browser = await chromium.launch({ headless: false });
  const page = await browser.newPage();

  try {
    console.log('导航到登录页面...');
    await page.goto('http://localhost:8084/#/login?redirect=/permissions/menu');

    // 等待页面加载完成
    await page.waitForLoadState('networkidle');
    console.log('页面加载完成');

    // 等待登录表单出现
    await page.waitForSelector('input[placeholder*="账号"]', { timeout: 5000 });
    console.log('登录表单已加载');

    // 填写用户名
    await page.fill('input[placeholder*="账号"]', '999');
    console.log('用户名已填写: 999');

    // 填写密码
    await page.fill('input[type="password"]', '123456');
    console.log('密码已填写: 123456');

    // 等待一小会儿
    await page.waitForTimeout(500);

    // 点击登录按钮
    await page.click('button[type="submit"], .submit');
    console.log('已点击登录按钮');

    // 等待跳转
    await page.waitForTimeout(3000);

    // 获取当前URL
    const currentUrl = page.url();
    console.log('登录后当前URL:', currentUrl);

    // 检查是否成功跳转到工作台而不是权限页面
    if (currentUrl.includes('/workbranch/index')) {
      console.log('✅ 成功：登录后跳转到工作台页面');
    } else if (currentUrl.includes('/permissions')) {
      console.log('❌ 失败：登录后仍然跳转到权限页面');
    } else {
      console.log('⚠️  跳转到其他页面:', currentUrl);
    }

    // 截图
    await page.screenshot({ path: 'user-999-login-result.png' });
    console.log('截图已保存到: user-999-login-result.png');

    // 等待一段时间以便观察
    await page.waitForTimeout(5000);

  } catch (error) {
    console.error('登录测试出错:', error.message);
  } finally {
    await browser.close();
  }
})();
