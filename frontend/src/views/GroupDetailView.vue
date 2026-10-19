<script setup lang="ts">
import { ref, watch } from 'vue'
import axios from 'axios'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import GroupBalanceList from '@/components/GroupBalanceList.vue'
import {
  getGroupApi,
  getGroupMembersApi,
  leaveGroupApi,
  removeGroupMemberApi,
  transferGroupOwnerApi,
  updateGroupApi,
} from '@/api/groupApi'
import { getGroupExpensesApi } from '@/api/expenseApi'
import type { ApiResponse } from '@/types/api'
import type { GroupResponse, GroupMemberResponse } from '@/types/group'
import type { ExpenseResponse } from '@/types/expense'
import GroupSettlementList from '@/components/GroupSettlementList.vue'
import GroupExpenseStats from '@/components/GroupExpenseStats.vue'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()

const router = useRouter()
const auth = useAuthStore()
const leaving = ref(false)
const leaveError = ref('')

async function handleLeaveGroup() {
  if (!group.value || leaving.value) return
  if (!window.confirm(`確定要退出「${group.value.name}」嗎？`)) return

  leaving.value = true
  leaveError.value = ''

  try {
    const result = await leaveGroupApi(group.value.id)
    if (!result.success) {
      leaveError.value = result.message || '退出群組失敗'
      return
    }
    await router.replace('/app')
  } catch (error) {
    leaveError.value = getErrorMessage(error, '退出群組失敗，請稍後重試')
  } finally {
    leaving.value = false
  }
}

const group = ref<GroupResponse | null>(null)
const loading = ref(false)
const errorMessage = ref('')
let latestGroupRequest = 0

const editingGroup = ref(false)
const editName = ref('')
const editDescription = ref('')
const savingGroup = ref(false)
const editError = ref('')

function startGroupEdit() {
  if (!group.value) return
  editName.value = group.value.name
  editDescription.value = group.value.description ?? ''
  editError.value = ''
  editingGroup.value = true
}

async function handleUpdateGroup() {
  if (!group.value || savingGroup.value) return

  const name = editName.value.trim()
  if (!name || name.length > 100) {
    editError.value = '群組名稱必須介於 1 到 100 個字元'
    return
  }

  savingGroup.value = true
  editError.value = ''

  try {
    const result = await updateGroupApi(group.value.id, {
      name,
      description: editDescription.value.trim() || null,
    })
    if (!result.success || !result.data) {
      throw new Error(result.message || '更新群組失敗')
    }

    group.value = result.data
    editingGroup.value = false
  } catch (error) {
    editError.value = getErrorMessage(error, '更新群組失敗，請稍後重試')
  } finally {
    savingGroup.value = false
  }
}

const members = ref<GroupMemberResponse[]>([])
const memberPage = ref(0)
const memberTotalPages = ref(0)
const membersLoading = ref(false)
const membersError = ref('')
let latestMembersRequest = 0

const expenses = ref<ExpenseResponse[]>([])
const expensePage = ref(0)
const expenseTotalPages = ref(0)
const expensesLoading = ref(false)
const expensesError = ref('')
let latestExpensesRequest = 0

function getGroupId() {
  const groupId = route.params.groupId
  if (typeof groupId !== 'string' || !groupId) {
    throw new Error('缺少群組 ID')
  }
  return groupId
}

function getErrorMessage(error: unknown, fallback: string) {
  if (axios.isAxiosError<ApiResponse<null>>(error)) {
    return error.response?.data?.message || fallback
  }
  return error instanceof Error ? error.message : fallback
}

function formatAmount(amount: number, currency: string) {
  return new Intl.NumberFormat('zh-TW', {
    style: 'currency',
    currency,
    currencyDisplay: 'code',
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(amount)
}

async function loadGroup() {
  const requestId = ++latestGroupRequest
  loading.value = true
  errorMessage.value = ''
  group.value = null

  try {
    const result = await getGroupApi(getGroupId())
    if (requestId !== latestGroupRequest) return

    if (!result.success || !result.data) {
      throw new Error(result.message || '無法取得群組')
    }
    group.value = result.data
  } catch (error) {
    if (requestId !== latestGroupRequest) return
    errorMessage.value = getErrorMessage(error, '載入群組失敗，請稍後重試')
  } finally {
    if (requestId === latestGroupRequest) loading.value = false
  }
}

async function loadMembers(targetPage = memberPage.value) {
  const requestId = ++latestMembersRequest
  membersLoading.value = true
  membersError.value = ''

  try {
    const result = await getGroupMembersApi(getGroupId(), targetPage)
    if (requestId !== latestMembersRequest) return

    if (!result.success || !result.data) {
      throw new Error(result.message || '無法取得成員')
    }
    members.value = result.data.content
    memberPage.value = result.data.page
    memberTotalPages.value = result.data.totalPages
  } catch (error) {
    if (requestId !== latestMembersRequest) return
    membersError.value = getErrorMessage(error, '載入成員失敗，請稍後重試')
  } finally {
    if (requestId === latestMembersRequest) membersLoading.value = false
  }
}

async function loadExpenses(targetPage = expensePage.value) {
  const requestId = ++latestExpensesRequest
  expensesLoading.value = true
  expensesError.value = ''

  try {
    const result = await getGroupExpensesApi(getGroupId(), targetPage)
    if (requestId !== latestExpensesRequest) return

    if (!result.success || !result.data) {
      throw new Error(result.message || '無法取得支出')
    }
    expenses.value = result.data.content
    expensePage.value = result.data.page
    expenseTotalPages.value = result.data.totalPages
  } catch (error) {
    if (requestId !== latestExpensesRequest) return
    expensesError.value = getErrorMessage(error, '載入支出失敗，請稍後重試')
  } finally {
    if (requestId === latestExpensesRequest) expensesLoading.value = false
  }
}

const transferring = ref(false)
const transferError = ref('')
const removingMemberId = ref<string | null>(null)
const removeError = ref('')
const balanceReload = ref(0)
// 轉讓擁有權
async function handleTransferOwner(member: GroupMemberResponse) {
  if (!group.value || transferring.value || removingMemberId.value) return

  const groupId = group.value.id
  if (!window.confirm(`確定將「${group.value.name}」的擁有權轉讓給 ${member.userName} 嗎？`)) {
    return
  }

  transferring.value = true
  transferError.value = ''

  try {
    const result = await transferGroupOwnerApi(groupId, member.userId)
    if (!result.success || !result.data) {
      throw new Error(result.message || '轉讓擁有權失敗')
    }

    group.value = result.data
    await loadMembers(memberPage.value)
  } catch (error) {
    transferError.value = getErrorMessage(error, '轉讓擁有權失敗，請稍後重試')
  } finally {
    transferring.value = false
  }
}

async function handleRemoveMember(member: GroupMemberResponse) {
  if (!group.value || removingMemberId.value || transferring.value) return
  if (!window.confirm(`確定要將 ${member.userName} 移出群組嗎？`)) return

  removingMemberId.value = member.userId
  removeError.value = ''

  try {
    const result = await removeGroupMemberApi(group.value.id, member.userId)
    if (!result.success) {
      throw new Error(result.message || '移除成員失敗')
    }

    await loadMembers(memberPage.value)
    if (members.value.length === 0 && memberPage.value > 0) {
      await loadMembers(memberPage.value - 1)
    }
    balanceReload.value++
  } catch (error) {
    removeError.value = getErrorMessage(error, '移除成員失敗，請稍後重試')
  } finally {
    removingMemberId.value = null
  }
}

watch(
  () => route.params.groupId,
  () => {
    editingGroup.value = false
    editError.value = ''
    members.value = []
    memberPage.value = 0
    memberTotalPages.value = 0

    expenses.value = []
    expensePage.value = 0
    expenseTotalPages.value = 0

    void loadGroup()
    void loadMembers(0)
    void loadExpenses(0)
  },
  { immediate: true },
)
</script>

<template>
  <main>
    <RouterLink to="/app">返回我的群組</RouterLink>

    <p v-if="loading" role="status">載入中…</p>

    <div v-else-if="errorMessage">
      <p role="alert">{{ errorMessage }}</p>
      <button @click="loadGroup()">重試</button>
    </div>

    <div v-else-if="group" class="group-detail">
      <h1>{{ group.name }}</h1>
      <form v-if="editingGroup" @submit.prevent="handleUpdateGroup">
        <fieldset :disabled="savingGroup">
          <legend>編輯群組</legend>
          <label for="edit-group-name">群組名稱</label>
          <input id="edit-group-name" v-model="editName" maxlength="100" required />
          <label for="edit-group-description">說明（選填）</label>
          <textarea id="edit-group-description" v-model="editDescription" rows="3" />
          <p v-if="editError" role="alert">{{ editError }}</p>
          <button type="submit">{{ savingGroup ? '儲存中…' : '儲存' }}</button>
          <button type="button" @click="editingGroup = false">取消</button>
        </fieldset>
      </form>
      <div v-else>
        <p>{{ group.description || '尚未填寫說明' }}</p>
        <button
          v-if="auth.user?.id === group.ownerId"
          type="button"
          @click="startGroupEdit"
        >
          編輯群組
        </button>
      </div>

      <dl>
        <dt>群組擁有者</dt>
        <dd>{{ group.ownerName }}</dd>
        <dt>基本幣別</dt>
        <dd>{{ group.baseCurrency }}</dd>
        <dt>邀請碼</dt>
        <dd>{{ group.inviteCode }}</dd>
      </dl>
      <div v-if="auth.user && group.ownerId !== auth.user.id">
        <button type="button" :disabled="leaving" @click="handleLeaveGroup">
          {{ leaving ? '退出中…' : '退出群組' }}
        </button>
        <p v-if="leaveError" role="alert">{{ leaveError }}</p>
      </div>

      <GroupBalanceList
        :key="`${group.id}-${balanceReload}`"
        :group-id="group.id"
        :currency="group.baseCurrency"
      />
      <RouterLink :to="`/groups/${group.id}/settlements/new`"> 記錄還款 </RouterLink>
      <GroupSettlementList :key="group.id" :group-id="group.id" />

      <section>
        <h2>群組成員</h2>
        <p v-if="transferError" role="alert">{{ transferError }}</p>
        <p v-if="removeError" role="alert">{{ removeError }}</p>
        <p v-if="membersLoading" role="status">載入成員中…</p>

        <div v-else-if="membersError">
          <p role="alert">{{ membersError }}</p>
          <button @click="loadMembers()">重試</button>
        </div>

        <p v-else-if="members.length === 0">目前沒有成員</p>

        <ul v-else>
          <li v-for="member in members" :key="member.id">
            {{ member.userName }}（{{ member.role }}）
            <span>{{ member.userEmail }}</span>
            <button
              v-if="auth.user?.id === group.ownerId && member.userId !== group.ownerId"
              type="button"
              :disabled="transferring || removingMemberId !== null"
              @click="handleTransferOwner(member)"
            >
              {{ transferring ? '處理中…' : '轉讓擁有權' }}
            </button>
            <button
              v-if="auth.user?.id === group.ownerId && member.userId !== group.ownerId"
              type="button"
              :disabled="removingMemberId !== null || transferring"
              @click="handleRemoveMember(member)"
            >
              {{ removingMemberId === member.userId ? '移除中…' : '移除成員' }}
            </button>
          </li>
        </ul>

        <nav v-if="memberTotalPages > 1" aria-label="成員分頁">
          <button
            :disabled="membersLoading || memberPage === 0"
            @click="loadMembers(memberPage - 1)"
          >
            上一頁
          </button>
          <span>第 {{ memberPage + 1 }} / {{ memberTotalPages }} 頁</span>
          <button
            :disabled="membersLoading || memberPage >= memberTotalPages - 1"
            @click="loadMembers(memberPage + 1)"
          >
            下一頁
          </button>
        </nav>
      </section>

      <section>
        <h2>群組支出</h2>
        <RouterLink :to="`/groups/${group.id}/expenses/new`"> 新增支出 </RouterLink>

        <p v-if="expensesLoading" role="status">載入支出中…</p>

        <div v-else-if="expensesError">
          <p role="alert">{{ expensesError }}</p>
          <button @click="loadExpenses()">重試</button>
        </div>

        <p v-else-if="expenses.length === 0">目前沒有支出</p>

        <ul v-else>
          <li v-for="expense in expenses" :key="expense.id">
            <RouterLink :to="`/expenses/${expense.id}`">
              {{ expense.title }}
            </RouterLink>
            <span>（{{ expense.categoryName ?? '未分類' }}）</span>
            <span>
              {{ formatAmount(expense.amount, expense.currency) }}
              · {{ expense.payerName }} 付款 · {{ expense.expenseAt.replace('T', ' ') }}
            </span>
          </li>
        </ul>

        <nav v-if="expenseTotalPages > 1" aria-label="支出分頁">
          <button
            :disabled="expensesLoading || expensePage === 0"
            @click="loadExpenses(expensePage - 1)"
          >
            上一頁
          </button>
          <span>第 {{ expensePage + 1 }} / {{ expenseTotalPages }} 頁</span>
          <button
            :disabled="expensesLoading || expensePage >= expenseTotalPages - 1"
            @click="loadExpenses(expensePage + 1)"
          >
            下一頁
          </button>
        </nav>
      </section>
      <GroupExpenseStats :key="group.id" :group-id="group.id" />
    </div>
  </main>
</template>

<style scoped>
.group-detail {
  min-width: 0;
  overflow-wrap: anywhere;
}

section {
  margin-top: 24px;
}

li {
  margin-bottom: 8px;
}

nav {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 12px;
}
</style>
