<template>
  <div class="security-settings">
    <el-card shadow="never">
      <template #header>
        <span>{{ t('user.security_settings') }}</span>
      </template>

      <div class="section">
        <div class="section-title">{{ t('user.change_password') }}</div>
        <el-form
          ref="passwordFormRef"
          :model="passwordForm"
          :rules="passwordRules"
          label-width="120px"
        >
          <el-form-item :label="t('user.current_password')" prop="oldPassword">
            <el-input
              v-model="passwordForm.oldPassword"
              type="password"
              :placeholder="t('user.current_password_placeholder')"
              show-password
            />
          </el-form-item>

          <el-form-item :label="t('user.new_password')" prop="newPassword">
            <el-input
              v-model="passwordForm.newPassword"
              type="password"
              :placeholder="t('user.new_password_placeholder')"
              show-password
            />
          </el-form-item>

          <el-form-item :label="t('user.confirm_password')" prop="confirmPassword">
            <el-input
              v-model="passwordForm.confirmPassword"
              type="password"
              :placeholder="t('user.confirm_password_placeholder')"
              show-password
            />
          </el-form-item>

          <el-form-item>
            <el-button type="primary" @click="handleChangePassword">
              {{ t('user.change_password_button') }}
            </el-button>
          </el-form-item>
        </el-form>
      </div>

      <el-divider />

      <div class="section">
        <div class="section-title">{{ t('user.login_history') }}</div>
        <el-table :data="loginHistory" border>
          <el-table-column :label="t('user.login_time')" prop="loginTime" width="180" />
          <el-table-column :label="t('user.login_ip')" prop="ip" width="150" />
          <el-table-column :label="t('user.login_location')" prop="location" />
          <el-table-column :label="t('user.login_device')" prop="device" width="150" />
          <el-table-column :label="t('user.login_status')" prop="status" width="100">
            <template #default="{ row }">
              <el-tag :type="row.status === 'success' ? 'success' : 'danger'">
                {{ row.status === 'success' ? t('user.success') : t('user.failed') }}
              </el-tag>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <el-divider />

      <div class="section">
        <div class="section-title">{{ t('user.active_sessions') }}</div>
        <el-table :data="activeSessions" border>
          <el-table-column :label="t('user.device')" prop="device" width="200" />
          <el-table-column :label="t('user.login_time')" prop="loginTime" width="180" />
          <el-table-column :label="t('user.last_activity')" prop="lastActivity" width="180" />
          <el-table-column :label="t('user.current')" prop="current" width="100">
            <template #default="{ row }">
              <el-tag v-if="row.current" type="success">{{ t('user.yes') }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column :label="t('common.operation')" width="100">
            <template #default="{ row }">
              <el-button v-if="!row.current" type="danger" link @click="handleRevoke(row)">
                {{ t('user.revoke') }}
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </el-card>
  </div>
</template>

<script lang="ts" setup>
import { ref, reactive } from 'vue'
import { useI18n } from '@/hooks/web/useI18n'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'

const { t } = useI18n()

const passwordFormRef = ref<FormInstance>()

const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const validateConfirmPassword = (rule: any, value: any, callback: any) => {
  if (value === '') {
    callback(new Error(t('user.confirm_password_required')))
  } else if (value !== passwordForm.newPassword) {
    callback(new Error(t('user.password_mismatch')))
  } else {
    callback()
  }
}

const passwordRules: FormRules = {
  oldPassword: [
    { required: true, message: t('user.current_password_required'), trigger: 'blur' }
  ],
  newPassword: [
    { required: true, message: t('user.new_password_required'), trigger: 'blur' },
    { min: 8, message: t('user.password_too_short'), trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, validator: validateConfirmPassword, trigger: 'blur' }
  ]
}

const loginHistory = ref([
  {
    loginTime: '2024-03-01 10:30:00',
    ip: '192.168.1.100',
    location: '中国-北京',
    device: 'Windows 10 / Chrome 120',
    status: 'success'
  },
  {
    loginTime: '2024-02-28 15:20:00',
    ip: '192.168.1.100',
    location: '中国-北京',
    device: 'Windows 10 / Chrome 120',
    status: 'success'
  }
])

const activeSessions = ref([
  {
    device: 'Windows 10 / Chrome 120',
    loginTime: '2024-03-01 10:30:00',
    lastActivity: '2024-03-01 11:00:00',
    current: true
  },
  {
    device: 'Mac OS / Safari 17',
    loginTime: '2024-02-28 09:00:00',
    lastActivity: '2024-02-28 18:00:00',
    current: false
  }
])

const handleChangePassword = async () => {
  if (!passwordFormRef.value) return

  await passwordFormRef.value.validate((valid) => {
    if (valid) {
      // TODO: 调用 API 修改密码
      ElMessage.success(t('user.password_change_success'))
      passwordFormRef.value?.resetFields()
    }
  })
}

const handleRevoke = (row: any) => {
  // TODO: 调用 API 撤销会话
  ElMessage.success(t('user.session_revoked'))
}
</script>

<style lang="less" scoped>
.security-settings {
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
