import { createRouter, createWebHashHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import type { App } from 'vue'

export const routes: AppRouteRecordRaw[] = [
  {
    path: '/',
    name: 'index',
    redirect: '/workbranch/index',
    component: () => import('@/layout/index.vue'),
    hidden: true,
    meta: {},
    children: [
      {
        path: 'workbranch',
        name: 'workbranch',
        hidden: true,
        component: () => import('@/views/workbranch/index.vue'),
        meta: { hidden: true }
      }
    ]
  },
  {
    path: '/sqlbot',
    name: 'sqlbot',
    component: () => import('@/layout/index.vue'),
    hidden: true,
    meta: {},
    children: [
      {
        path: 'index',
        name: 'clt',
        hidden: true,
        component: () => import('@/views/sqlbot/index.vue'),
        meta: { hidden: true }
      }
    ]
  },
  {
    path: '/login',
    name: 'login',
    hidden: true,
    meta: {},
    component: () => import('@/views/login/index.vue')
  },
  {
    path: '/admin-login',
    name: 'admin-login',
    hidden: true,
    meta: {},
    component: () => import('@/views/login/index.vue')
  },
  {
    path: '/401',
    name: '401',
    hidden: true,
    meta: {},
    component: () => import('@/views/401/index.vue')
  },
  {
    path: '/dvCanvas',
    name: 'dvCanvas',
    hidden: true,
    meta: {},
    component: () => import('@/views/data-visualization/index.vue')
  },
  {
    path: '/dashboard',
    name: 'dashboard',
    hidden: true,
    meta: {},
    component: () => import('@/views/dashboard/index.vue')
  },
  {
    path: '/dashboardPreview',
    name: 'dashboardPreview',
    hidden: true,
    meta: {},
    component: () => import('@/views/dashboard/DashboardPreviewShow.vue')
  },
  {
    path: '/chart',
    name: 'chart',
    hidden: true,
    meta: {},
    component: () => import('@/views/chart/index.vue')
  },
  {
    path: '/previewShow',
    name: 'previewShow',
    hidden: true,
    meta: {},
    component: () => import('@/views/data-visualization/PreviewShow.vue')
  },
  {
    path: '/DeResourceTree',
    name: 'DeResourceTree',
    hidden: true,
    meta: {},
    component: () => import('@/views/common/DeResourceTree.vue')
  },
  {
    path: '/dataset-embedded',
    name: 'dataset-embedded',
    hidden: true,
    meta: {},
    component: () => import('@/views/visualized/data/dataset/index.vue')
  },
  {
    path: '/dataset-embedded-form',
    name: 'dataset-embedded-form',
    hidden: true,
    meta: {},
    component: () => import('@/views/visualized/data/dataset/form/index.vue')
  },
  {
    path: '/preview',
    name: 'preview',
    hidden: true,
    meta: {},
    component: () => import('@/views/data-visualization/PreviewCanvas.vue')
  },
  {
    path: '/de-link/:uuid',
    name: 'link',
    hidden: true,
    meta: {},
    component: () => import('@/views/data-visualization/LinkContainer.vue')
  },
  {
    path: '/rich-text',
    name: 'rich-text',
    hidden: true,
    meta: {},
    component: () => import('@/custom-component/rich-text/DeRichTextView.vue')
  },
  {
    path: '/modify-pwd',
    name: 'modify-pwd',
    hidden: true,
    meta: {},
    component: () => import('@/layout/index.vue'),
    children: [
      {
        path: 'index',
        name: 'mpi',
        hidden: true,
        component: () => import('@/views/system/modify-pwd/index.vue'),
        meta: { hidden: true }
      }
    ]
  },
  {
    path: '/chart-view',
    name: 'chart-view',
    hidden: true,
    meta: {},
    component: () => import('@/views/chart/ChartView.vue')
  },
  {
    path: '/template-manage',
    name: 'template-manage',
    hidden: true,
    meta: {},
    component: () => import('@/views/template/indexInject.vue')
  },
  {
    path: '/404',
    name: '404',
    hidden: true,
    meta: {},
    component: () => import('@/views/404/index.vue')
  },
  {
    path: '/auto-login',
    name: 'auto-login',
    hidden: true,
    meta: {},
    component: () => import('@/views/tools/auto-login/index.vue')
  },
  {
    path: '/permissions/',
    hidden: true,
    meta: {},
    redirect: { name: 'permissions-menu' }
  },
  {
    path: '/permissions',
    name: 'permissions',
    hidden: true,
    meta: {},
    component: () => import('@/layout/index.vue'),
    redirect: { name: 'permissions-menu' },
    children: [
      {
        path: 'menu',
        name: 'permissions-menu',
        hidden: true,
        component: () => import('@/views/permissions/menu/index.vue'),
        meta: { title: '权限中心' }
      },
      {
        path: 'menu-auth',
        name: 'permissions-menu-auth',
        hidden: true,
        component: () => import('@/views/permissions/menu/index.vue'),
        meta: { title: '菜单授权' }
      },
      {
        path: 'resource',
        name: 'permissions-resource',
        hidden: true,
        component: () => import('@/views/permissions/resource/index.vue'),
        meta: { title: '资源授权' }
      },
      {
        path: 'templates',
        name: 'permissions-templates',
        hidden: true,
        component: () => import('@/views/permissions/template/index.vue'),
        meta: { title: '权限模板' }
      },
      {
        path: 'audit',
        name: 'permissions-audit',
        hidden: true,
        component: () => import('@/views/permissions/audit/index.vue'),
        meta: { title: '权限审计' }
      },
      {
        path: 'user',
        name: 'permissions-user',
        hidden: true,
        component: () => import('@/views/permissions/user/index.vue'),
        meta: {
          title: '用户管理',
          roles: ['admin']
        }
      },
      {
        path: 'role',
        name: 'permissions-role',
        hidden: true,
        component: () => import('@/views/permissions/role/index.vue'),
        meta: {
          title: '角色管理',
          roles: ['admin']
        }
      }
    ]
  },
  {
    path: '/user-setting',
    name: 'user-setting',
    component: () => import('@/layout/index.vue'),
    hidden: true,
    meta: {},
    children: [
      {
        path: 'index',
        name: 'us-index',
        component: () => import('@/views/system/user-setting/index.vue'),
        meta: {
          title: '用户设置',
          hidden: true
        }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes: routes as RouteRecordRaw[]
})

export const resetRouter = (): void => {
  const resetWhiteNameList = ['Login']
  router.getRoutes().forEach(route => {
    const { name } = route
    if (name && !resetWhiteNameList.includes(name as string)) {
      router.hasRoute(name) && router.removeRoute(name)
    }
  })
}

export const setupRouter = (app: App<Element>) => {
  app.use(router)
}

export default router
