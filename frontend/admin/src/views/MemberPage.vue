<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getMemberLevels, createMemberLevel, updateMemberLevel, updateMemberLevelStatus, deleteMemberLevel } from '@/api/member-level'
import { getMembers, createMember, updateMember, deleteMember } from '@/api/member'
import type { MemberLevel, MemberListItem } from '@/types'

// ==================== Tab 控制 ====================
const activeTab = ref<'members' | 'levels'>('members')

// ==================== 会员等级 ====================
const levels = ref<MemberLevel[]>([])
const levelsLoading = ref(false)
const levelDialogVisible = ref(false)
const levelFormMode = ref<'create' | 'edit'>('create')
const levelForm = reactive({ id: 0, name: '', discountRate: '', sort: 0 })

async function fetchLevels() {
  levelsLoading.value = true
  try {
    const res = await getMemberLevels()
    levels.value = res.data
  } finally {
    levelsLoading.value = false
  }
}

function openLevelCreate() {
  levelFormMode.value = 'create'
  levelForm.id = 0
  levelForm.name = ''
  levelForm.discountRate = ''
  levelForm.sort = 0
  levelDialogVisible.value = true
}

function openLevelEdit(row: MemberLevel) {
  levelFormMode.value = 'edit'
  levelForm.id = row.id
  levelForm.name = row.name
  levelForm.discountRate = row.discountRate
  levelForm.sort = row.sort
  levelDialogVisible.value = true
}

async function handleLevelSubmit() {
  if (!levelForm.name) { ElMessage.warning('请输入等级名称'); return }
  if (!levelForm.discountRate) { ElMessage.warning('请输入折扣率'); return }
  try {
    if (levelFormMode.value === 'create') {
      await createMemberLevel({ name: levelForm.name, discountRate: levelForm.discountRate, sort: levelForm.sort })
      ElMessage.success('等级创建成功')
    } else {
      await updateMemberLevel(levelForm.id, { name: levelForm.name, discountRate: levelForm.discountRate, sort: levelForm.sort })
      ElMessage.success('等级更新成功')
    }
    levelDialogVisible.value = false
    fetchLevels()
  } catch {
    // handled
  }
}

async function handleToggleLevelStatus(row: MemberLevel) {
  const newStatus = row.status === 'ENABLED' ? 'DISABLED' : 'ENABLED'
  const label = newStatus === 'ENABLED' ? '启用' : '停用'
  try {
    await ElMessageBox.confirm(`确认${label}等级「${row.name}」？`, '确认操作', { type: 'warning' })
    await updateMemberLevelStatus(row.id, newStatus)
    ElMessage.success(`${label}成功`)
    fetchLevels()
  } catch {
    // cancelled
  }
}

async function handleLevelDelete(row: MemberLevel) {
  try {
    await ElMessageBox.confirm(`确认删除等级「${row.name}」？`, '确认删除', { type: 'warning' })
    await deleteMemberLevel(row.id)
    ElMessage.success('删除成功')
    fetchLevels()
  } catch {
    // cancelled
  }
}

function discountText(rate: string) {
  return `${parseFloat(rate) * 10}折`
}

// ==================== 会员列表 ====================
const members = ref<MemberListItem[]>([])
const memberTotal = ref(0)
const memberLoading = ref(false)

const memberFilters = reactive({
  keyword: '',
  levelId: undefined as number | undefined,
  page: 1,
  size: 20,
})

async function fetchMembers() {
  memberLoading.value = true
  try {
    const params: any = { page: memberFilters.page, size: memberFilters.size }
    if (memberFilters.keyword) params.keyword = memberFilters.keyword
    if (memberFilters.levelId) params.levelId = memberFilters.levelId
    const res = await getMembers(params)
    members.value = res.data.list
    memberTotal.value = res.data.total
  } finally {
    memberLoading.value = false
  }
}

function handleMemberSearch() {
  memberFilters.page = 1
  fetchMembers()
}

function handleMemberReset() {
  memberFilters.keyword = ''
  memberFilters.levelId = undefined
  memberFilters.page = 1
  fetchMembers()
}

// ==================== 会员编辑 ====================
const memberDialogVisible = ref(false)
const memberFormMode = ref<'create' | 'edit'>('create')
const memberFormLoading = ref(false)
const memberForm = reactive({
  id: 0,
  name: '',
  phones: [''] as string[],
  levelId: undefined as number | undefined,
  remark: '',
})

function addPhone() {
  memberForm.phones.push('')
}

function removePhone(index: number) {
  if (memberForm.phones.length <= 1) {
    ElMessage.warning('至少保留一个手机号')
    return
  }
  memberForm.phones.splice(index, 1)
}

function openMemberCreate() {
  memberFormMode.value = 'create'
  memberForm.id = 0
  memberForm.name = ''
  memberForm.phones = ['']
  memberForm.levelId = undefined
  memberForm.remark = ''
  memberDialogVisible.value = true
}

function openMemberEdit(row: MemberListItem) {
  memberFormMode.value = 'edit'
  memberForm.id = row.id
  memberForm.name = row.name
  memberForm.phones = [...row.phones]
  memberForm.levelId = row.levelId
  memberForm.remark = row.remark || ''
  memberDialogVisible.value = true
}

async function handleMemberSubmit() {
  if (!memberForm.name) { ElMessage.warning('请输入会员姓名'); return }
  const validPhones = memberForm.phones.filter((p) => p.trim())
  if (validPhones.length === 0) { ElMessage.warning('请至少输入一个手机号'); return }
  if (!memberForm.levelId) { ElMessage.warning('请选择会员等级'); return }
  memberFormLoading.value = true
  try {
    const data = {
      name: memberForm.name,
      phones: validPhones,
      levelId: memberForm.levelId,
      remark: memberForm.remark || undefined,
    }
    if (memberFormMode.value === 'create') {
      await createMember(data)
      ElMessage.success('会员创建成功')
    } else {
      await updateMember(memberForm.id, data)
      ElMessage.success('会员更新成功')
    }
    memberDialogVisible.value = false
    fetchMembers()
    fetchLevels()
  } catch {
    // handled
  } finally {
    memberFormLoading.value = false
  }
}

async function handleMemberDelete(row: MemberListItem) {
  try {
    await ElMessageBox.confirm(`确认删除会员「${row.name}」？`, '确认删除', { type: 'warning' })
    await deleteMember(row.id)
    ElMessage.success('删除成功')
    fetchMembers()
    fetchLevels()
  } catch {
    // cancelled
  }
}

onMounted(() => {
  fetchLevels()
  fetchMembers()
})
</script>

<template>
  <div class="member-page">
    <el-tabs v-model="activeTab" class="page-tabs">
      <!-- ==================== 会员列表 Tab ==================== -->
      <el-tab-pane label="会员列表" name="members">
        <div class="page-card filter-card">
          <div class="filter-bar">
            <el-input
              v-model="memberFilters.keyword"
              placeholder="搜索姓名 / 手机号"
              clearable
              style="width: 200px"
              @keyup.enter="handleMemberSearch"
              @clear="handleMemberSearch"
            >
              <template #prefix><el-icon><Search /></el-icon></template>
            </el-input>

            <el-select v-model="memberFilters.levelId" placeholder="全部等级" clearable style="width: 150px" @change="handleMemberSearch">
              <el-option v-for="l in levels.filter(l => l.status === 'ENABLED')" :key="l.id" :label="l.name" :value="l.id" />
            </el-select>

            <el-button type="primary" @click="handleMemberSearch">
              <el-icon><Search /></el-icon>查询
            </el-button>
            <el-button @click="handleMemberReset">重置</el-button>

            <div style="flex: 1" />
            <el-button type="primary" @click="openMemberCreate">
              <el-icon><Plus /></el-icon>新增会员
            </el-button>
          </div>
        </div>

        <div class="page-card table-card">
          <el-table :data="members" v-loading="memberLoading" stripe style="width: 100%">
            <el-table-column prop="name" label="姓名" width="100" />

            <el-table-column label="手机号" min-width="200">
              <template #default="{ row }">
                <div class="phone-tags">
                  <el-tag v-for="(phone, i) in row.phones" :key="i" size="small" effect="plain" class="phone-tag">
                    {{ phone }}
                  </el-tag>
                </div>
              </template>
            </el-table-column>

            <el-table-column prop="levelName" label="等级" width="130">
              <template #default="{ row }">
                <el-tag type="warning" size="small" effect="plain">{{ row.levelName }}</el-tag>
              </template>
            </el-table-column>

            <el-table-column prop="remark" label="备注" min-width="140">
              <template #default="{ row }">
                {{ row.remark || '-' }}
              </template>
            </el-table-column>

            <el-table-column prop="createTime" label="创建时间" width="140" />

            <el-table-column label="操作" width="140" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" size="small" @click="openMemberEdit(row)">编辑</el-button>
                <el-button link type="danger" size="small" @click="handleMemberDelete(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>

          <el-pagination
            v-model:current-page="memberFilters.page"
            v-model:page-size="memberFilters.size"
            :total="memberTotal"
            :page-sizes="[10, 20, 50]"
            layout="total, sizes, prev, pager, next"
            @current-change="() => fetchMembers()"
            @size-change="() => { memberFilters.page = 1; fetchMembers() }"
          />
        </div>
      </el-tab-pane>

      <!-- ==================== 会员等级 Tab ==================== -->
      <el-tab-pane label="会员等级" name="levels">
        <div class="page-card table-card">
          <div style="margin-bottom: 16px">
            <el-button type="primary" @click="openLevelCreate">
              <el-icon><Plus /></el-icon>新增等级
            </el-button>
          </div>

          <el-table :data="levels" v-loading="levelsLoading" stripe style="width: 100%">
            <el-table-column prop="name" label="等级名称" min-width="140" />

            <el-table-column label="服务折扣" width="100" align="center">
              <template #default="{ row }">
                <span class="text-orange font-bold">{{ discountText(row.discountRate) }}</span>
              </template>
            </el-table-column>

            <el-table-column prop="discountRate" label="折扣率" width="90" align="center">
              <template #default="{ row }">
                {{ row.discountRate }}
              </template>
            </el-table-column>

            <el-table-column prop="sort" label="排序" width="70" align="center" />

            <el-table-column label="状态" width="90" align="center">
              <template #default="{ row }">
                <el-tag :type="row.status === 'ENABLED' ? 'success' : 'info'" size="small" effect="dark">
                  {{ row.status === 'ENABLED' ? '启用' : '停用' }}
                </el-tag>
              </template>
            </el-table-column>

            <el-table-column prop="memberCount" label="会员数" width="90" align="center" />

            <el-table-column label="操作" width="200">
              <template #default="{ row }">
                <el-button link type="primary" size="small" @click="openLevelEdit(row)">编辑</el-button>
                <el-button
                  link
                  :type="row.status === 'ENABLED' ? 'warning' : 'success'"
                  size="small"
                  @click="handleToggleLevelStatus(row)"
                >
                  {{ row.status === 'ENABLED' ? '停用' : '启用' }}
                </el-button>
                <el-button link type="danger" size="small" @click="handleLevelDelete(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-tab-pane>
    </el-tabs>

    <!-- ==================== 会员编辑弹窗 ==================== -->
    <el-dialog
      v-model="memberDialogVisible"
      :title="memberFormMode === 'create' ? '新增会员' : '编辑会员'"
      width="520px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <el-form label-width="80px" @submit.prevent="handleMemberSubmit">
        <el-form-item label="姓名" required>
          <el-input v-model="memberForm.name" placeholder="请输入会员姓名" />
        </el-form-item>

        <el-form-item label="手机号" required>
          <div class="phone-input-list">
            <div v-for="(_, index) in memberForm.phones" :key="index" class="phone-input-row">
              <el-input v-model="memberForm.phones[index]" placeholder="手机号" maxlength="11" style="flex: 1" />
              <el-button
                v-if="memberForm.phones.length > 1"
                link
                type="danger"
                @click="removePhone(index)"
              >
                <el-icon><Delete /></el-icon>
              </el-button>
            </div>
            <el-button size="small" @click="addPhone">
              <el-icon><Plus /></el-icon>添加手机号
            </el-button>
          </div>
        </el-form-item>

        <el-form-item label="会员等级" required>
          <el-select v-model="memberForm.levelId" placeholder="请选择等级" style="width: 100%">
            <el-option v-for="l in levels.filter(l => l.status === 'ENABLED')" :key="l.id" :label="l.name" :value="l.id" />
          </el-select>
        </el-form-item>

        <el-form-item label="备注">
          <el-input v-model="memberForm.remark" type="textarea" :rows="2" placeholder="可选" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="memberDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="memberFormLoading" @click="handleMemberSubmit">
          {{ memberFormMode === 'create' ? '创建' : '保存' }}
        </el-button>
      </template>
    </el-dialog>

    <!-- ==================== 等级编辑弹窗 ==================== -->
    <el-dialog
      v-model="levelDialogVisible"
      :title="levelFormMode === 'create' ? '新增会员等级' : '编辑会员等级'"
      width="420px"
      destroy-on-close
    >
      <el-form label-width="80px">
        <el-form-item label="等级名称" required>
          <el-input v-model="levelForm.name" placeholder="如：500档会员" />
        </el-form-item>
        <el-form-item label="服务折扣率" required>
          <el-input v-model="levelForm.discountRate" placeholder="如 0.95 表示 9.5折" />
          <div class="form-tip">输入折扣率，如 0.95 = 9.5折、0.85 = 8.5折</div>
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="levelForm.sort" :min="0" :max="999" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="levelDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleLevelSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style lang="scss" scoped>
.member-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.page-tabs {
  :deep(.el-tabs__header) {
    margin: 0;
    padding: 0 20px;
    background: #fff;
    border-radius: 12px 12px 0 0;
    box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04);
  }

  :deep(.el-tabs__content) {
    padding: 0;
  }
}

.filter-card {
  padding: 16px 20px;
}

.table-card {
  padding: 20px;
}

.text-orange {
  color: #ff5a00;
}

.font-bold {
  font-weight: 600;
}

/* 手机号标签 */
.phone-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

.phone-tag {
  border-color: rgba(255, 90, 0, 0.2);
  color: #666;
}

/* 手机号输入列表 */
.phone-input-list {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.phone-input-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.form-tip {
  font-size: 12px;
  color: #999;
  margin-top: 4px;
  line-height: 1.4;
}
</style>
