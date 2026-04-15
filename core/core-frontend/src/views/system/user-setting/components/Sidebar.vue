<template>
  <div class="sidebar-container">
    <div
      v-for="item in tabs"
      :key="item.key"
      :class="['tab-item', { active: activeTab === item.key }]"
      @click="handleClick(item.key)"
    >
      <el-icon>
        <component :is="item.icon" />
      </el-icon>
      <span>{{ item.label }}</span>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { useI18n } from '@/hooks/web/useI18n'
import { User, Lock, Setting, Grid } from '@element-plus/icons-vue'
import type { Component } from 'vue'

type TabType = 'personal' | 'security' | 'preferences' | 'group'

interface TabItem {
  key: TabType
  icon: Component
  label: string
}

defineProps<{
  activeTab: TabType
}>()

const emit = defineEmits<{
  (e: 'tab-change', tab: TabType): void
}>()

const { t } = useI18n()

const tabs: TabItem[] = [
  { key: 'personal', icon: User, label: t('user.personal_info') },
  { key: 'security', icon: Lock, label: t('user.security_settings') },
  { key: 'preferences', icon: Setting, label: t('user.preferences') },
  { key: 'group', icon: Grid, label: t('user.group_info') }
]

const handleClick = (key: TabType) => {
  emit('tab-change', key)
}
</script>

<style lang="less" scoped>
.sidebar-container {
  .tab-item {
    display: flex;
    align-items: center;
    height: 36px;
    padding: 0 8px;
    margin-bottom: 4px;
    border-radius: 4px;
    cursor: pointer;
    color: #1f2329;
    font-size: 14px;
    transition: all 0.2s;

    &:hover {
      background: #f5f6f7;
    }

    &.active {
      background: #3370ff;
      color: #fff;
    }

    .ed-icon {
      margin-right: 8px;
      font-size: 16px;
    }

    span {
      flex: 1;
    }
  }
}
</style>
