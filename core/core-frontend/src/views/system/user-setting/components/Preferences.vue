<template>
  <div class="preferences">
    <el-card shadow="never">
      <template #header>
        <span>{{ t('user.preferences') }}</span>
      </template>

      <el-form ref="formRef" :model="formData" label-width="150px">
        <div class="section">
          <div class="section-title">{{ t('user.display_settings') }}</div>

          <el-form-item :label="t('user.theme')">
            <el-radio-group v-model="formData.theme">
              <el-radio label="light">{{ t('user.theme_light') }}</el-radio>
              <el-radio label="dark">{{ t('user.theme_dark') }}</el-radio>
              <el-radio label="auto">{{ t('user.theme_auto') }}</el-radio>
            </el-radio-group>
          </el-form-item>

          <el-form-item :label="t('user.language')">
            <el-select v-model="formData.language" :placeholder="t('user.language_placeholder')">
              <el-option label="简体中文" value="zh_CN" />
              <el-option label="English" value="en_US" />
            </el-select>
          </el-form-item>

          <el-form-item :label="t('user.timezone')">
            <el-select v-model="formData.timezone" :placeholder="t('user.timezone_placeholder')">
              <el-option label="(GMT+08:00) 北京时间" value="Asia/Shanghai" />
              <el-option label="(GMT+09:00) 东京时间" value="Asia/Tokyo" />
              <el-option label="(GMT-05:00) 纽约时间" value="America/New_York" />
              <el-option label="(GMT+00:00) 伦敦时间" value="Europe/London" />
            </el-select>
          </el-form-item>

          <el-form-item :label="t('user.date_format')">
            <el-select v-model="formData.dateFormat" :placeholder="t('user.date_format_placeholder')">
              <el-option label="YYYY-MM-DD" value="yyyy-MM-dd" />
              <el-option label="YYYY/MM/DD" value="yyyy/MM/dd" />
              <el-option label="DD/MM/YYYY" value="dd/MM/yyyy" />
              <el-option label="MM/DD/YYYY" value="MM/dd/yyyy" />
            </el-select>
          </el-form-item>
        </div>

        <el-divider />

        <div class="section">
          <div class="section-title">{{ t('user.notification_settings') }}</div>

          <el-form-item :label="t('user.email_notification')">
            <el-switch v-model="formData.emailNotification" />
          </el-form-item>

          <el-form-item :label="t('user.system_notification')">
            <el-switch v-model="formData.systemNotification" />
          </el-form-item>

          <el-form-item :label="t('user.task_notification')">
            <el-switch v-model="formData.taskNotification" />
          </el-form-item>
        </div>

        <el-divider />

        <div class="section">
          <div class="section-title">{{ t('user.other_settings') }}</div>

          <el-form-item :label="t('user.auto_save')">
            <el-switch v-model="formData.autoSave" />
          </el-form-item>

          <el-form-item :label="t('user.show_tips')">
            <el-switch v-model="formData.showTips" />
          </el-form-item>

          <el-form-item :label="t('user.default_dashboard')">
            <el-select v-model="formData.defaultDashboard" :placeholder="t('user.select_dashboard')">
              <el-option label="默认仪表板" value="default" />
              <el-option label="销售分析" value="sales" />
              <el-option label="用户统计" value="users" />
            </el-select>
          </el-form-item>
        </div>

        <el-form-item>
          <el-button type="primary" @click="handleSave">{{ t('common.save') }}</el-button>
          <el-button @click="handleReset">{{ t('user.reset_to_default') }}</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script lang="ts" setup>
import { reactive } from 'vue'
import { useI18n } from '@/hooks/web/useI18n'
import { ElMessage } from 'element-plus'

const { t } = useI18n()

const formData = reactive({
  theme: 'light',
  language: 'zh_CN',
  timezone: 'Asia/Shanghai',
  dateFormat: 'yyyy-MM-dd',
  emailNotification: true,
  systemNotification: true,
  taskNotification: false,
  autoSave: true,
  showTips: true,
  defaultDashboard: 'default'
})

const handleSave = () => {
  // TODO: 调用 API 保存偏好设置
  ElMessage.success(t('user.save_success'))
}

const handleReset = () => {
  // TODO: 重置为默认设置
  ElMessage.success(t('user.reset_success'))
}
</script>

<style lang="less" scoped>
.preferences {
  .section {
    margin-bottom: 24px;

    &:last-child {
      margin-bottom: 0;
    }

    .section-title {
      font-size: 16px;
      font-weight: 500;
      margin-bottom: 16px;
      color: #1f2329;
    }
  }
}
</style>
