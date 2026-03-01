<template>
  <div class="permissions-container">
    <p class="router-title">{{ t('commons.permissions_manage') }}</p>
    <el-tabs v-model="activeName">
      <el-tab-pane label="菜单授权" name="menu">
        <menu-authorization />
      </el-tab-pane>
      <el-tab-pane label="资源授权" name="resource">
        <resource-authorization />
      </el-tab-pane>
      <el-tab-pane label="权限模板" name="templates">
        <permission-templates />
      </el-tab-pane>
      <el-tab-pane label="权限审计" name="audit">
        <permission-audit />
      </el-tab-pane>
      <el-tab-pane label="用户管理" name="user">
        <user-management />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script lang="ts" setup>
import { ref } from 'vue'
import { useI18n } from '@/hooks/web/useI18n'
import MenuAuthorization from './menu/index.vue'
import ResourceAuthorization from './resource/index.vue'
import PermissionTemplates from './template/index.vue'
import PermissionAudit from './audit/index.vue'
import UserManagement from './user/index.vue'

const { t } = useI18n()
const activeName = ref('menu')
</script>

<style lang="less" scoped>
.permissions-container {
  padding: 24px;
  background: #fff;
  min-height: calc(100vh - 120px);
}
.router-title {
  font-size: 16px;
  font-weight: 500;
  margin: 0 0 16px 0;
  color: #303133;
}
</style>
