<template>
  <div class="group-info">
    <div class="group-card">
      <div class="group-header">
        <span class="title">{{ t('user.group_info') }}</span>
      </div>
      <el-divider />
      <div class="group-content">
        <el-descriptions :column="1" border>
          <el-descriptions-item :label="t('user.roles')">
            <el-tag v-for="role in userRoles" :key="role.id" class="role-tag">
              {{ role.name }}
            </el-tag>
            <span v-if="userRoles.length === 0" class="empty-text">-</span>
          </el-descriptions-item>
          <el-descriptions-item :label="t('user.groups')">
            <el-tag v-for="group in userGroups" :key="group.id" class="role-tag">
              {{ group.name }}
            </el-tag>
            <span v-if="userGroups.length === 0" class="empty-text">-</span>
          </el-descriptions-item>
        </el-descriptions>
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { ref, onMounted } from 'vue'
import { useI18n } from '@/hooks/web/useI18n'
import { useUserStoreWithOut } from '@/store/modules/user'

const { t } = useI18n()
const userStore = useUserStoreWithOut()

interface Role {
  id: string
  name: string
}

interface Group {
  id: string
  name: string
}

const userRoles = ref<Role[]>([])
const userGroups = ref<Group[]>([])
const loading = ref(false)

const loadGroupInfo = async () => {
  loading.value = true
  try {
    // TODO: 从后端 API 获取用户分组和角色信息
    // 暂时使用模拟数据
    await new Promise(resolve => setTimeout(resolve, 500))

    // 获取当前用户信息
    const userInfo = userStore.getuserInfo || {}

    // 模拟数据 - 管理员默认拥有管理员角色和默认分组
    userRoles.value = [{ id: '1', name: '管理员' }]
    userGroups.value = [{ id: '1', name: '默认分组' }]
  } catch (error) {
    console.error('Failed to load group info:', error)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadGroupInfo()
})
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

    .group-content {
      margin-top: 16px;
    }

    .role-tag {
      margin-right: 8px;
    }

    .empty-text {
      color: #8f959e;
      font-size: 14px;
    }
  }
}
</style>
