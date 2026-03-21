<template>
  <el-dialog
    v-model="dialogVisible"
    :title="dialogTitle"
    width="600px"
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
      <el-form-item label="用户名" prop="username">
        <el-input v-model="formData.username" placeholder="请输入用户名" :disabled="isEdit" />
      </el-form-item>

      <el-form-item label="姓名" prop="nickName">
        <el-input v-model="formData.nickName" placeholder="请输入姓名" />
      </el-form-item>

      <el-form-item label="邮箱" prop="email">
        <el-input v-model="formData.email" placeholder="请输入邮箱" />
      </el-form-item>

      <el-form-item label="手机号" prop="phone">
        <el-input v-model="formData.phone" placeholder="请输入手机号" />
      </el-form-item>

      <el-form-item label="密码" prop="password" v-if="!isEdit">
        <el-input
          v-model="formData.password"
          type="password"
          placeholder="请输入密码"
          show-password
        />
      </el-form-item>

      <el-form-item label="角色" prop="roleIds">
        <el-select v-model="formData.roleIds" multiple placeholder="请选择角色" style="width: 100%">
          <el-option
            v-for="role in roleOptions"
            :key="role.roleId"
            :label="role.roleName"
            :value="role.roleId"
          />
        </el-select>
      </el-form-item>

      <el-form-item label="用户组" prop="groupIds">
        <el-select
          v-model="formData.groupIds"
          multiple
          placeholder="请选择用户组"
          style="width: 100%"
        >
          <el-option
            v-for="group in groupOptions"
            :key="group.groupId"
            :label="group.groupName"
            :value="group.groupId"
          />
        </el-select>
      </el-form-item>

      <el-form-item label="状态" prop="status">
        <el-radio-group v-model="formData.status">
          <el-radio :value="1">启用</el-radio>
          <el-radio :value="0">禁用</el-radio>
        </el-radio-group>
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
import { ElMessage } from 'element-plus-secondary'
import type { FormInstance, FormRules } from 'element-plus-secondary'
import { createUser, updateUser } from '../api'
import type { UserForm, Role, Group, User } from '../types'

interface Props {
  modelValue: boolean
  user?: User
  roleOptions: Role[]
  groupOptions: Group[]
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

const isEdit = computed(() => !!props.user)

const dialogTitle = computed(() => (isEdit.value ? '编辑用户' : '创建用户'))

const formData = ref<UserForm>({
  username: '',
  nickName: '',
  email: '',
  phone: '',
  password: '',
  roleIds: [],
  groupIds: [],
  status: 1
})

const formRules: FormRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度在 3 到 20 个字符', trigger: 'blur' }
  ],
  nickName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }
  ],
  phone: [{ pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号格式', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度在 6 到 20 个字符', trigger: 'blur' }
  ],
  roleIds: [{ required: true, message: '请选择角色', trigger: 'change', type: 'array' }]
}

// 定义resetForm函数（必须在watch之前定义，避免"Cannot access before initialization"错误）
const resetForm = () => {
  formData.value = {
    username: '',
    nickName: '',
    email: '',
    phone: '',
    password: '',
    roleIds: [],
    groupIds: [],
    status: 1
  }
  formRef.value?.clearValidate()
}

watch(
  () => props.user,
  user => {
    if (user) {
      formData.value = {
        username: user.username,
        nickName: user.nickName,
        email: user.email,
        phone: user.phone || '',
        password: '',
        roleIds: user.roles?.map((r: Role) => r.roleId) || [],
        groupIds: user.groups?.map((g: Group) => g.groupId) || [],
        status: user.status
      }
    } else {
      resetForm()
    }
  },
  { immediate: true }
)

const handleClose = () => {
  dialogVisible.value = false
  resetForm()
}

const handleSubmit = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async valid => {
    if (!valid) return

    submitting.value = true
    try {
      if (isEdit.value && props.user) {
        await updateUser(props.user.userId, formData.value)
        ElMessage.success('用户更新成功')
      } else {
        await createUser(formData.value)
        ElMessage.success('用户创建成功')
      }
      emit('success')
      handleClose()
    } catch (error) {
      ElMessage.error(isEdit.value ? '用户更新失败' : '用户创建失败')
    } finally {
      submitting.value = false
    }
  })
}
</script>

<style scoped>
.el-select {
  width: 100%;
}
</style>
