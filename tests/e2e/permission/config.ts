// tests/e2e/permission/config.ts

export const config = {
  // 测试环境配置
  baseUrl: 'http://localhost:8080',

  // 测试用户
  users: {
    admin: {
      username: 'admin',
      password: 'DataEase@123456',
      displayName: '管理员'
    },
    normal: {
      username: 'testuser',
      password: 'Test@123456',
      displayName: '普通用户'
    }
  },

  // 超时配置（毫秒）
  timeout: {
    pageLoad: 10000,
    element: 5000,
    action: 3000
  },

  // 报告输出路径
  reportPath: 'tests/e2e/permission/reports',

  // 路由配置
  routes: {
    login: '/login',
    home: '/',
    dashboard: '/dashboard',
    screen: '/screen',
    dataset: '/dataset',
    datasource: '/datasource',
    systemUser: '/system/user'
  }
};

export type UserRole = 'admin' | 'normal';
export type TestUser = typeof config.users.admin;
