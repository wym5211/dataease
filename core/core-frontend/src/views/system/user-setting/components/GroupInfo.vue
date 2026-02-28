<template>
  <div class="group-info">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>{{ t('user.group_info') }}</span>
          <el-button type="primary" link @click="handleAddGroup">
            {{ t('user.add_group') }}
          </el-button>
        </div>
      </template>

      <el-table :data="groups" border>
        <el-table-column type="index" :label="t('user.serial_number')" width="60" />
        <el-table-column :label="t('user.group_name')" prop="name" />
        <el-table-column :label="t('user.group_description')" prop="description" />
        <el-table-column :label="t('user.group_role')" prop="role" width="120">
          <template #default="{ row }">
            <el-tag :type="row.role === 'admin' ? 'danger' : 'primary'">
              {{ row.role === 'admin' ? t('user.admin') : t('user.member') }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="t('user.join_time')" prop="joinTime" width="180" />
        <el-table-column :label="t('common.operation')" width="150">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleView(row)">
              {{ t('user.view') }}
            </el-button>
            <el-button type="danger" link @click="handleLeave(row)">
              {{ t('user.leave') }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script lang="ts" setup>
import { ref } from 'vue'
import { useI18n } from '@/hooks/web/useI18n'
import { ElMessage, ElMessageBox } from 'element-plus'

const { t } = useI18n()

interface Group {
  id: number
  name: string
  description: string
  role: string
  joinTime: string
}

const groups = ref<Group[]>([
  {
    id: 1,
    name: '数据分析组',
    description: '负责数据分析和报表制作',
    role: 'admin',
    joinTime: '2024-01-15 10:00:00'
  },
  {
    id: 2,
    name: 'BI开发组',
    description: 'BI系统开发和维护',
    role: 'member',
    joinTime: '2024-02-01 14:30:00'
  }
])

const handleAddGroup = () => {
  // TODO: 打开添加分组对话框
  ElMessage.info(t('user.add_group_tip'))
}

const handleView = (row: Group) => {
  // TODO: 查看分组详情
  ElMessage.info(t('user.view_group_detail', { name: row.name }))
}

const handleLeave = (row: Group) => {
  ElMessageBox.confirm(
    t('user.leave_group_confirm', { name: row.name }),
    t('common.tip'),
    {
      confirmButtonText: t('common.confirm'),
      cancelButtonText: t('common.cancel'),
      type: 'warning'
    }
  ).then(() => {
    // TODO: 调用 API 离开分组
    ElMessage.success(t('user.leave_group_success'))
  })
}
</script>

<style lang="less" scoped>
.group-info {
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    font-weight: 500;
  }
}
</style>
