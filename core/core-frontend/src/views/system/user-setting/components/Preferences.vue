<template>
  <div class="preferences">
    <div class="preferences-card">
      <div class="preferences-header">
        <span class="title">{{ t('user.preferences') }}</span>
      </div>
      <el-divider />
      <div class="preferences-content">
        <p class="placeholder-text">偏好设置功能开发中...</p>
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { useI18n } from '@/hooks/web/useI18n'

const { t } = useI18n()
</script>

<style lang="less" scoped>
.preferences {
  .preferences-card {
    padding: 20px 24px 24px;
    border-radius: 4px;
    background: #fff;
    min-height: 200px;

    .preferences-header {
      .title {
        color: #1f2329;
        font-size: 16px;
        font-weight: 500;
        line-height: 24px;
      }
    }

    .placeholder-text {
      color: #8f959e;
      font-size: 14px;
      text-align: center;
      margin-top: 40px;
    }
  }
}
</style>
