<template>
  <div class="user-setting flex-align-center">
    <div class="user-setting-container">
      <div class="user-tabs">
        <div class="tabs-title flex-align-center">{{ t('user.user_setting') }}</div>
        <el-divider />
        <Sidebar :active-tab="activeTab" @tab-change="handleTabChange" />
      </div>
      <div class="setting-content">
        <component :is="currentComponent" />
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { ref, computed } from 'vue'
import { useI18n } from '@/hooks/web/useI18n'
import Sidebar from './components/Sidebar.vue'
import PersonalInfo from './components/PersonalInfo.vue'
import SecuritySettings from './components/SecuritySettings.vue'
import Preferences from './components/Preferences.vue'
import GroupInfo from './components/GroupInfo.vue'

const { t } = useI18n()

type TabType = 'personal' | 'security' | 'preferences' | 'group'

const activeTab = ref<TabType>('personal')

const components = {
  personal: PersonalInfo,
  security: SecuritySettings,
  preferences: Preferences,
  group: GroupInfo
}

const currentComponent = computed(() => components[activeTab.value])

const handleTabChange = (tab: TabType) => {
  activeTab.value = tab
}
</script>

<style lang="less" scoped>
.user-setting {
  width: 100%;
  flex-direction: column;
  padding-top: 24px;
  background: #f5f6f7;

  .user-setting-container {
    display: flex;
    font-style: normal;
  }

  .user-tabs {
    width: 200px;
    height: fit-content;
    border-radius: 4px;
    background: #fff;
    padding: 16px;
    box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);

    .ed-divider {
      margin: 4px 0;
      border-color: rgba(31, 35, 41, 0.15);
    }

    .tabs-title {
      padding-left: 8px;
      color: #8d9199;
      font-size: 14px;
      font-style: normal;
      font-weight: 500;
      line-height: 22px;
      height: 40px;
    }
  }

  .setting-content {
    margin-left: 16px;
    width: 864px;
  }
}
</style>
