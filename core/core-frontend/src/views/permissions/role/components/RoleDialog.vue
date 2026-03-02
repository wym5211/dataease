<template>
  <el-dialog
    v-model="dialogVisible"
    :title="dialogTitle"
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
      <el-form-item label="角色名称" prop="name">
        <el-input v-model="formData.name" placeholder="请输入角色名称" />
      </el-form-item>

      <el-form-item label="角色编码" prop="code">
        <el-input v-model="formData.code" placeholder="请输入角色编码" :disabled="isEdit" />
      </el-form-item>

      <el-form-item label="描述" prop="description">
        <el-input
          v-model="formData.description"
          type="textarea"
          :rows="3"
          placeholder="请输入角色描述"
        />
      </el-form-item>

      <el-form-item label="状态" prop="status">
        <el-radio-group v-model="formData.status">
          <el-radio label="1">启用</el-radio>
          <el-radio label="0">禁用</el-radio>
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
import { createRole, updateRole } from '../api'
import type { RoleForm, Role } from '../types'

interface Props {
  modelValue: boolean
  role?: Role
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

const isEdit = computed(() => !!props.role)

const dialogTitle = computed(() => (isEdit.value ? '编辑角色' : '创建角色'))

const formData = ref<RoleForm>({
  name: '',
  code: '',
  description: '',
  status: 1 // 保持数字类型
})

// 动态表单验证规则
const formRules = computed<FormRules>(() => {
  return {
    name: [
      { required: true, message: '请输入角色名称', trigger: 'blur' },
      { min: 2, max: 50, message: '角色名称长度在 2 到 50 个字符', trigger: 'blur' }
    ],
    // 编辑模式下不验证角色编码（禁用字段）
    code: isEdit.value
      ? []
      : [
          { required: true, message: '请输入角色编码', trigger: 'blur' },
          { min: 2, max: 50, message: '角色编码长度在 2 到 50 个字符', trigger: 'blur' },
          {
            pattern: /^[a-zA-Z0-9_]+$/,
            message: '角色编码只能包含字母、数字和下划线',
            trigger: 'blur'
          }
        ],
    status: [{ required: true, message: '请选择状态', trigger: 'change' }]
  }
})

// 定义resetForm函数（必须在watch之前定义，避免"Cannot access before initialization"错误）
const resetForm = () => {
  formData.value = {
    name: '',
    code: '',
    description: '',
    status: '1' as any // 使用字符串 '1' 作为默认值
  }
  formRef.value?.clearValidate()
}

watch(
  () => props.role,
  role => {
    if (role) {
      // 将 status 转换为字符串（Element Plus radio 使用字符串值）
      formData.value = {
        name: role.name,
        code: role.code,
        description: role.description || '',
        status: String(role.status ?? 1)
      }
    } else {
      resetForm()
    }
  }
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
      // 提交时将 status 转换为数字
      const submitData = {
        ...formData.value,
        status: Number(formData.value.status)
      }

      if (isEdit.value && props.role) {
        await updateRole(props.role.id, submitData)
        ElMessage.success('角色更新成功')
      } else {
        await createRole(submitData)
        ElMessage.success('角色创建成功')
      }
      emit('success')
      handleClose()
    } catch (error) {
      ElMessage.error(isEdit.value ? '角色更新失败' : '角色创建失败')
    } finally {
      submitting.value = false
    }
  })
}
</script>

<style scoped>
.el-textarea {
  width: 100%;
}
</style>
