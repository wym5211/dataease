<template>
  <div class="security-settings">
    <div class="security-card">
      <div class="security-header">
        <span class="title">{{ t('user.security_settings') }}</span>
      </div>
      <el-divider />
      <div class="security-content">
        <update-pwd />
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { useI18n } from '@/hooks/web/useI18n'
import UpdatePwd from '../../modify-pwd/UpdatePwd.vue'

const { t } = useI18n()
</script>

<style lang="less" scoped>
.security-settings {
  .security-card {
    padding: 20px 24px 24px;
    border-radius: 4px;
    background: #fff;

    .security-header {
      .title {
        color: #1f2329;
        font-size: 16px;
        font-weight: 500;
        line-height: 24px;
      }
    }
  }
}
</style>
