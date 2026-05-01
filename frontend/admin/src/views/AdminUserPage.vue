<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getAdminUsers, createAdminUser, updateAdminUser, updateAdminUserStatus, resetAdminUserPassword, deleteAdminUser } from '@/api/admin-user'
import type { AdminUser } from '@/types'

const users = ref<AdminUser[]>([])
const loading = ref(false)

async function fetchUsers() {
  loading.value = true
  try {
    const res = await getAdminUsers()
    users.value = res.data
  } finally {
    loading.value = false
  }
}

// ==================== 新增 ====================
const createVisible = ref(false)
const createLoading = ref(false)
const createForm = ref({ username: '', password: '', realName: '', role: 'STAFF' as 'MANAGER' | 'STAFF' })

function openCreate() {
  createForm.value = { username: '', password: '', realName: '', role: 'STAFF' }
  createVisible.value = true
}

async function handleCreate() {
  const f = createForm.value
  if (!f.username) { ElMessage.warning('请输入用户名'); return }
  if (!f.password || f.password.length < 6) { ElMessage.warning('密码不能少于6位'); return }
  if (!f.realName) { ElMessage.warning('请输入姓名'); return }
  createLoading.value = true
  try {
    await createAdminUser(f)
    ElMessage.success('账号创建成功')
    createVisible.value = false
    fetchUsers()
  } catch {
    // handled
  } finally {
    createLoading.value = false
  }
}

// ==================== 编辑 ====================
const editVisible = ref(false)
const editLoading = ref(false)
const editForm = ref({ id: 0, realName: '', role: 'STAFF' as 'MANAGER' | 'STAFF' })

function openEdit(row: AdminUser) {
  editForm.value = { id: row.id, realName: row.realName, role: row.role as 'MANAGER' | 'STAFF' }
  editVisible.value = true
}

async function handleEdit() {
  const f = editForm.value
  if (!f.realName) { ElMessage.warning('请输入姓名'); return }
  editLoading.value = true
  try {
    await updateAdminUser(f.id, { realName: f.realName, role: f.role })
    ElMessage.success('更新成功')
    editVisible.value = false
    fetchUsers()
  } catch {
    // handled
  } finally {
    editLoading.value = false
  }
}

// ==================== 重置密码 ====================
const pwdVisible = ref(false)
const pwdLoading = ref(false)
const pwdForm = ref({ id: 0, realName: '', newPassword: '' })

function openResetPwd(row: AdminUser) {
  pwdForm.value = { id: row.id, realName: row.realName, newPassword: '' }
  pwdVisible.value = true
}

async function handleResetPwd() {
  if (!pwdForm.value.newPassword || pwdForm.value.newPassword.length < 6) {
    ElMessage.warning('新密码不能少于6位')
    return
  }
  pwdLoading.value = true
  try {
    await resetAdminUserPassword(pwdForm.value.id, pwdForm.value.newPassword)
    ElMessage.success('密码重置成功')
    pwdVisible.value = false
  } catch {
    // handled
  } finally {
    pwdLoading.value = false
  }
}

// ==================== 启停用 ====================
async function handleToggleStatus(row: AdminUser) {
  const newStatus = row.status === 'ENABLED' ? 'DISABLED' : 'ENABLED'
  const label = newStatus === 'ENABLED' ? '启用' : '禁用'
  try {
    await ElMessageBox.confirm(`确认${label}账号「${row.realName}」？`, '确认操作', { type: 'warning' })
    await updateAdminUserStatus(row.id, newStatus)
    ElMessage.success(`${label}成功`)
    fetchUsers()
  } catch {
    // cancelled
  }
}

// ==================== 删除 ====================
async function handleDelete(row: AdminUser) {
  try {
    await ElMessageBox.confirm(`确认删除账号「${row.realName}」？此操作不可恢复。`, '确认删除', { type: 'warning' })
    await deleteAdminUser(row.id)
    ElMessage.success('删除成功')
    fetchUsers()
  } catch {
    // cancelled
  }
}

function isBoss(row: AdminUser) {
  return row.role === 'BOSS'
}

onMounted(() => {
  fetchUsers()
})
</script>

<template>
  <div class="admin-user-page">
    <div class="page-card table-card">
      <div class="card-header">
        <div class="header-info">
          <span class="text-muted">管理后台的所有登录账号</span>
        </div>
        <el-button type="primary" @click="openCreate">
          <el-icon><Plus /></el-icon>新增账号
        </el-button>
      </div>

      <el-table :data="users" v-loading="loading" stripe style="width: 100%">
        <el-table-column prop="username" label="用户名" width="160" />

        <el-table-column prop="realName" label="姓名" width="120" />

        <el-table-column prop="role" label="角色" width="120" align="center">
          <template #default="{ row }">
            <el-tag
              :type="row.role === 'BOSS' ? 'danger' : row.role === 'MANAGER' ? 'warning' : 'info'"
              size="small"
              effect="dark"
            >
              {{ row.roleLabel }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ENABLED' ? 'success' : 'danger'" size="small" effect="plain">
              {{ row.status === 'ENABLED' ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="操作" min-width="260">
          <template #default="{ row }">
            <template v-if="!isBoss(row)">
              <el-button link type="primary" size="small" @click="openEdit(row)">编辑</el-button>
              <el-button link type="warning" size="small" @click="openResetPwd(row)">重置密码</el-button>
              <el-button
                link
                :type="row.status === 'ENABLED' ? 'warning' : 'success'"
                size="small"
                @click="handleToggleStatus(row)"
              >
                {{ row.status === 'ENABLED' ? '禁用' : '启用' }}
              </el-button>
              <el-button link type="danger" size="small" @click="handleDelete(row)">删除</el-button>
            </template>
            <span v-else class="text-muted">当前账号</span>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- ==================== 新增弹窗 ==================== -->
    <el-dialog v-model="createVisible" title="新增账号" width="460px" :close-on-click-modal="false" destroy-on-close>
      <el-form label-width="80px" @submit.prevent="handleCreate">
        <el-form-item label="用户名" required>
          <el-input v-model="createForm.username" placeholder="登录用户名" />
        </el-form-item>
        <el-form-item label="密码" required>
          <el-input v-model="createForm.password" type="password" placeholder="至少6位" show-password />
        </el-form-item>
        <el-form-item label="姓名" required>
          <el-input v-model="createForm.realName" placeholder="真实姓名" />
        </el-form-item>
        <el-form-item label="角色" required>
          <el-radio-group v-model="createForm.role">
            <el-radio value="MANAGER">店长</el-radio>
            <el-radio value="STAFF">店员</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="createLoading" @click="handleCreate">创建</el-button>
      </template>
    </el-dialog>

    <!-- ==================== 编辑弹窗 ==================== -->
    <el-dialog v-model="editVisible" title="编辑账号" width="420px" destroy-on-close>
      <el-form label-width="80px" @submit.prevent="handleEdit">
        <el-form-item label="姓名" required>
          <el-input v-model="editForm.realName" />
        </el-form-item>
        <el-form-item label="角色" required>
          <el-radio-group v-model="editForm.role">
            <el-radio value="MANAGER">店长</el-radio>
            <el-radio value="STAFF">店员</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="editLoading" @click="handleEdit">保存</el-button>
      </template>
    </el-dialog>

    <!-- ==================== 重置密码弹窗 ==================== -->
    <el-dialog v-model="pwdVisible" title="重置密码" width="420px" destroy-on-close>
      <p class="pwd-tip">正在重置「<strong>{{ pwdForm.realName }}</strong>」的密码</p>
      <el-form label-width="80px" @submit.prevent="handleResetPwd">
        <el-form-item label="新密码" required>
          <el-input v-model="pwdForm.newPassword" type="password" placeholder="至少6位" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="pwdVisible = false">取消</el-button>
        <el-button type="primary" :loading="pwdLoading" @click="handleResetPwd">确认重置</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style lang="scss" scoped>
.admin-user-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.table-card {
  padding: 20px;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.text-muted {
  color: #999;
  font-size: 13px;
}

.pwd-tip {
  margin: 0 0 16px;
  font-size: 14px;
  color: #666;

  strong {
    color: #ff5a00;
  }
}
</style>
