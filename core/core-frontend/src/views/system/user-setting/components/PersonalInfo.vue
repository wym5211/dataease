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
        <el-button type="primary" :loading="saving" @click="handleSave">
          {{ t('common.save') }}
        </el-button>
      </div>
    </el-card>
  </div>
</template>

<script lang="ts" setup>
import { ref, reactive } from 'vue'
import { useI18n } from '@/hooks/web/useI18n'
import { ElMessage } from 'element-plus-secondary'
import type { FormInstance, FormRules } from 'element-plus-secondary'

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
const saving = ref(false)

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

// 保存原始数据用于取消时恢复
const originalData = ref<PersonalInfo>({ ...formData })

const formRules: FormRules = {
  nickName: [{ required: true, message: t('user.nick_name_required'), trigger: 'blur' }],
  email: [
    {
      validator: (_rule, value, callback) => {
        if (!value) {
          callback()
          return
        }
        if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value)) {
          callback(new Error(t('user.email_invalid')))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ],
  phone: [
    {
      validator: (_rule, value, callback) => {
        if (!value) {
          callback()
          return
        }
        if (!/^1[3-9]\d{9}$/.test(value)) {
          callback(new Error(t('user.phone_invalid')))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

const handleEdit = () => {
  originalData.value = { ...formData }
  isEditing.value = true
}

const handleCancel = () => {
  // 恢复原始数据
  Object.assign(formData, originalData.value)
  isEditing.value = false
  formRef.value?.clearValidate()
}

const handleSave = async () => {
  if (!formRef.value) return

  try {
    const valid = await formRef.value.validate()
    if (!valid) return

    saving.value = true

    // TODO: 调用 API 保存用户信息
    // 模拟 API 调用
    await new Promise(resolve => setTimeout(resolve, 1000))

    // 更新原始数据
    originalData.value = { ...formData }

    ElMessage.success(t('user.save_success'))
    isEditing.value = false
  } catch (error) {
    ElMessage.error(t('user.save_failed'))
  } finally {
    saving.value = false
  }
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
