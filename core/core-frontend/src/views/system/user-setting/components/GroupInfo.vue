<template>
  <div class="group-info">
    <div class="group-card">
      <div class="group-header">
        <span class="title">{{ t('user.group_info') }}</span>
      </div>
      <el-divider />
      <div class="group-content">
        <p class="placeholder-text">分组信息功能开发中...</p>
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { useI18n } from '@/hooks/web/useI18n'

const { t } = useI18n()
</script>

<style lang="less" scoped>
.group-info {
  .group-card {
    padding: 20px 24px 24px;
    border-radius: 4px;
    background: #fff;
    min-height: 200px;

    .group-header {
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
