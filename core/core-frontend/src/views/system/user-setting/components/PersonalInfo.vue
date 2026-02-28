<template>
  <div class="personal-info">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>{{ t('user.personal_info') }}</span>
          <el-button v-if="!isEditing" type="primary" link @click="handleEdit">
            {{ t('common.edit') }}
          </el-button>
        </div>
      </template>

      <el-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        label-width="120px"
        :disabled="!isEditing"
      >
        <el-form-item :label="t('user.username')">
          <el-input v-model="formData.username" disabled />
        </el-form-item>

        <el-form-item :label="t('user.nick_name')" prop="nickName">
          <el-input v-model="formData.nickName" :placeholder="t('user.nick_name_placeholder')" />
        </el-form-item>

        <el-form-item :label="t('user.email')" prop="email">
          <el-input v-model="formData.email" :placeholder="t('user.email_placeholder')" />
        </el-form-item>

        <el-form-item :label="t('user.phone')" prop="phone">
          <el-input v-model="formData.phone" :placeholder="t('user.phone_placeholder')" />
        </el-form-item>

        <el-form-item :label="t('user.gender')" prop="gender">
          <el-radio-group v-model="formData.gender">
            <el-radio label="male">{{ t('user.gender_male') }}</el-radio>
            <el-radio label="female">{{ t('user.gender_female') }}</el-radio>
            <el-radio label="other">{{ t('user.gender_other') }}</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item :label="t('user.department')">
          <el-input v-model="formData.department" :placeholder="t('user.department_placeholder')" />
        </el-form-item>

        <el-form-item :label="t('user.position')">
          <el-input v-model="formData.position" :placeholder="t('user.position_placeholder')" />
        </el-form-item>

        <el-form-item :label="t('user.language')">
          <el-select v-model="formData.language" :placeholder="t('user.language_placeholder')">
            <el-option label="简体中文" value="zh_CN" />
            <el-option label="English" value="en_US" />
          </el-select>
        </el-form-item>
      </el-form>

      <div v-if="isEditing" class="form-actions">
        <el-button @click="handleCancel">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" @click="handleSave">{{ t('common.save') }}</el-button>
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

interface PersonalInfo {
  username: string
  nickName: string
  email: string
  phone: string
  gender: string
  department: string
  position: string
  language: string
}

const formRef = ref<FormInstance>()
const isEditing = ref(false)

const formData = reactive<PersonalInfo>({
  username: 'admin',
  nickName: '',
  email: '',
  phone: '',
  gender: 'other',
  department: '',
  position: '',
  language: 'zh_CN'
})

const formRules: FormRules = {
  nickName: [{ required: true, message: t('user.nick_name_required'), trigger: 'blur' }],
  email: [{ type: 'email', message: t('user.email_invalid'), trigger: 'blur' }],
  phone: [
    {
      pattern: /^1[3-9]\d{9}$/,
      message: t('user.phone_invalid'),
      trigger: 'blur'
    }
  ]
}

const handleEdit = () => {
  isEditing.value = true
}

const handleCancel = () => {
  isEditing.value = false
  formRef.value?.resetFields()
}

const handleSave = async () => {
  if (!formRef.value) return

  await formRef.value.validate(valid => {
    if (valid) {
      // TODO: 调用 API 保存用户信息
      ElMessage.success(t('user.save_success'))
      isEditing.value = false
    }
  })
}
</script>

<style lang="less" scoped>
.personal-info {
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    font-weight: 500;
  }

  .form-actions {
    display: flex;
    justify-content: flex-end;
    gap: 8px;
    margin-top: 16px;
  }
}
</style>
