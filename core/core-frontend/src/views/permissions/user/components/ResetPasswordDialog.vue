<template>
  <el-dialog
    v-model="dialogVisible"
    title="重置密码"
    width="500px"
    :close-on-click-modal="false"
    @close="handleClose"
  >
    <el-form
      ref="formRef"
      :model="formData"
      :rules="formRules"
      label-width="100px"
      @submit.prevent="handleSubmit"
    >
      <el-form-item label="用户名">
        <el-input :value="username" disabled />
      </el-form-item>

      <el-form-item label="姓名">
        <el-input :value="nickName" disabled />
      </el-form-item>

      <el-form-item label="新密码" prop="newPassword">
        <el-input
          v-model="formData.newPassword"
          type="password"
          placeholder="请输入新密码"
          show-password
        />
      </el-form-item>

      <el-form-item label="确认密码" prop="confirmPassword">
        <el-input
          v-model="formData.confirmPassword"
          type="password"
          placeholder="请再次输入新密码"
          show-password
        />
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="handleClose">取消</el-button>
      <el-button type="primary" @click="handleSubmit" :loading="submitting"> 确定 </el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { resetPassword } from '../api'

interface Props {
  modelValue: boolean
  user?: any
}

interface Emits {
  (e: 'update:modelValue', value: boolean): void
  (e: 'success'): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

const formRef = ref<FormInstance>()
const submitting = ref(false)

const dialogVisible = computed({
  get: () => props.modelValue,
  set: val => emit('update:modelValue', val)
})

const username = computed(() => props.user?.username || '')
const nickName = computed(() => props.user?.nickName || '')

const formData = ref({
  newPassword: '',
  confirmPassword: ''
})

const validateConfirmPassword = (rule: any, value: any, callback: any) => {
  if (value === '') {
    callback(new Error('请再次输入密码'))
  } else if (value !== formData.value.newPassword) {
    callback(new Error('两次输入密码不一致'))
  } else {
    callback()
  }
}

const formRules: FormRules = {
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度在 6 到 20 个字符', trigger: 'blur' }
  ],
  confirmPassword: [{ required: true, validator: validateConfirmPassword, trigger: 'blur' }]
}

watch(
  () => props.modelValue,
  val => {
    if (val) {
      formData.value = {
        newPassword: '',
        confirmPassword: ''
      }
      formRef.value?.clearValidate()
    }
  }
)

const handleClose = () => {
  dialogVisible.value = false
  formData.value = {
    newPassword: '',
    confirmPassword: ''
  }
}

const handleSubmit = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async valid => {
    if (!valid) return

    submitting.value = true
    try {
      await resetPassword(props.user.userId, formData.value.newPassword)
      ElMessage.success('密码重置成功')
      emit('success')
      handleClose()
    } catch (error) {
      ElMessage.error('密码重置失败')
    } finally {
      submitting.value = false
    }
  })
}
</script>

<style scoped>
.el-input {
  width: 100%;
}
</style>
