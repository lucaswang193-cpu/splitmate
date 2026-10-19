<script setup lang="ts">
import { ref, watch } from 'vue'
import axios from 'axios'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { getGroupApi, getGroupMembersApi } from '@/api/groupApi'
import { createExpenseApi, createExpenseDraft } from '@/api/expenseApi'
import { getAllExpenseCategoriesApi } from '@/api/categoryApi'
import type { ApiResponse } from '@/types/api'
import type { GroupResponse, GroupMemberResponse } from '@/types/group'
import type { CreateExpenseRequest } from '@/types/expense'
import type { ExpenseCategoryResponse } from '@/types/category'

const route = useRoute()
const router = useRouter()

const group = ref<GroupResponse | null>(null)
const members = ref<GroupMemberResponse[]>([])
const categories = ref<ExpenseCategoryResponse[]>([])
const loading = ref(false)
const loadError = ref('')
const submitting = ref(false)
const submitError = ref('')
const completed = ref(false)

const title = ref('')
const amount = ref('')
const payerId = ref('')
const categoryId = ref<number | null>(null)
const note = ref('')
const selectedUserIds = ref<string[]>([])
const expenseAt = ref(localDateTime())

// 重試沿用原本的 UUID、分類與其他支出內容
const draft = ref<CreateExpenseRequest | null>(null)
let latestRequest = 0

function localDateTime() {
  const today = new Date()
  const date = [
    today.getFullYear(),
    String(today.getMonth() + 1).padStart(2, '0'),
    String(today.getDate()).padStart(2, '0'),
  ].join('-')
  const time = [
    String(today.getHours()).padStart(2, '0'),
    String(today.getMinutes()).padStart(2, '0'),
    String(today.getSeconds()).padStart(2, '0'),
  ].join(':')
  return `${date}T${time}`
}

function getErrorMessage(error: unknown, fallback: string) {
  if (axios.isAxiosError<ApiResponse<null>>(error)) {
    return error.response?.data?.message || fallback
  }
  return error instanceof Error ? error.message : fallback
}

async function loadInitialData() {
  const requestId = ++latestRequest
  const groupId = route.params.groupId

  loading.value = true
  loadError.value = ''
  submitError.value = ''
  submitting.value = false
  group.value = null
  members.value = []
  categories.value = []
  draft.value = null
  completed.value = false
  title.value = ''
  amount.value = ''
  payerId.value = ''
  categoryId.value = null
  note.value = ''
  selectedUserIds.value = []
  expenseAt.value = localDateTime()

  try {
    if (typeof groupId !== 'string' || !groupId) {
      throw new Error('缺少群組 ID')
    }

    const [groupResult, categoryList] = await Promise.all([
      getGroupApi(groupId),
      getAllExpenseCategoriesApi(),
    ])
    if (requestId !== latestRequest) return

    if (!groupResult.success || !groupResult.data) {
      throw new Error(groupResult.message || '無法取得群組')
    }

    const allMembers: GroupMemberResponse[] = []
    let page = 0
    let totalPages = 1

    do {
      const result = await getGroupMembersApi(groupId, page, 100)
      if (requestId !== latestRequest) return
      if (!result.success || !result.data) {
        throw new Error(result.message || '無法取得成員')
      }

      allMembers.push(...result.data.content)
      totalPages = result.data.totalPages
      page++
    } while (page < totalPages)

    members.value = Array.from(
      new Map(allMembers.map((member) => [member.userId, member])).values(),
    )
    categories.value = categoryList
    group.value = groupResult.data
  } catch (error) {
    if (requestId !== latestRequest) return
    loadError.value = getErrorMessage(error, '載入失敗，請稍後重試')
  } finally {
    if (requestId === latestRequest) loading.value = false
  }
}

// 以整數分計算平均分攤與尾差
function toCents(value: string) {
  const text = value.trim()
  if (!/^\d+(\.\d{1,2})?$/.test(text)) {
    throw new Error('金額必須是正數，最多兩位小數')
  }

  const [whole = '0', fraction = ''] = text.split('.')
  const cents = Number(whole) * 100 + Number(fraction.padEnd(2, '0'))

  if (!Number.isSafeInteger(cents) || cents < 1 || cents > 999999999) {
    throw new Error('金額需介於 0.01 與 9,999,999.99')
  }

  return cents
}

function buildDraft(): CreateExpenseRequest {
  if (!group.value) throw new Error('群組資料尚未載入')
  if (!title.value.trim()) throw new Error('請輸入支出名稱')
  if (!expenseAt.value) throw new Error('請選擇消費時間')

  if (
    categoryId.value !== null &&
    !categories.value.some((category) => category.id === categoryId.value)
  ) {
    throw new Error('請選擇有效的支出分類')
  }

  const memberIds = new Set(members.value.map((member) => member.userId))
  if (!memberIds.has(payerId.value)) {
    throw new Error('請選擇群組內的付款人')
  }

  const selected = [...new Set(selectedUserIds.value)]
  if (selected.length === 0) throw new Error('請至少選擇一位分攤成員')
  if (selected.some((id) => !memberIds.has(id))) {
    throw new Error('分攤成員不在群組中')
  }

  const cents = toCents(String(amount.value))
  const average = Math.floor(cents / selected.length)
  const remainder = cents % selected.length
  const total = cents / 100

  const splits = selected.map((userId, index) => ({
    userId,
    shareAmount: (average + (index < remainder ? 1 : 0)) / 100,
    paidAmount: userId === payerId.value ? total : 0,
  }))

  // 未參與分攤的付款人也必須記錄墊付款
  if (!selected.includes(payerId.value)) {
    splits.push({
      userId: payerId.value,
      shareAmount: 0,
      paidAmount: total,
    })
  }

  return createExpenseDraft({
    groupId: group.value.id,
    payerId: payerId.value,
    title: title.value.trim(),
    categoryId: categoryId.value,
    currency: group.value.baseCurrency,
    amount: total,
    exchangeRate: 1,
    baseAmount: total,
    expenseAt: expenseAt.value,
    note: note.value.trim() || null,
    splits,
  })
}

async function handleCreate() {
  if (loading.value || submitting.value || completed.value) return

  submitError.value = ''
  try {
    if (!draft.value) draft.value = buildDraft()
  } catch (error) {
    submitError.value = getErrorMessage(error, '請確認表單內容')
    return
  }

  const request = draft.value
  const requestId = latestRequest
  submitting.value = true

  try {
    const result = await createExpenseApi(request)
    if (requestId !== latestRequest) return
    if (!result.success || !result.data) {
      throw new Error(result.message || '建立支出失敗')
    }

    completed.value = true
  } catch (error) {
    if (requestId !== latestRequest) return

    submitError.value = getErrorMessage(error, '送出失敗，請稍後重試')

    // 明確的輸入錯誤可修改；網路錯誤保留原稿供重試
    if (axios.isAxiosError(error) && error.response?.status === 400) {
      draft.value = null
    }
  } finally {
    if (requestId === latestRequest) submitting.value = false
  }

  if (completed.value && requestId === latestRequest) {
    try {
      await router.push(`/groups/${request.groupId}`)
    } catch {
      submitError.value = '支出已建立，請點選返回群組'
    }
  }
}

watch(() => route.params.groupId, loadInitialData, { immediate: true })
</script>

<template>
  <main>
    <RouterLink :to="group ? `/groups/${group.id}` : '/app'"> 返回群組 </RouterLink>
    <h1>新增支出</h1>

    <p v-if="loading" role="status">載入中…</p>

    <div v-else-if="loadError">
      <p role="alert">{{ loadError }}</p>
      <button @click="loadInitialData()">重試</button>
    </div>

    <section v-else-if="group">
      <h2>{{ group.name }}</h2>
      <p>基本幣別：{{ group.baseCurrency }}</p>

      <p v-if="members.length === 0">沒有可用成員，無法新增支出</p>

      <form v-else @submit.prevent="handleCreate">
        <fieldset :disabled="submitting || draft !== null || completed">
          <legend>支出資料</legend>

          <label for="title">支出名稱</label>
          <input id="title" v-model="title" maxlength="200" required />

          <label for="category">分類（選填）</label>
          <select id="category" v-model="categoryId">
            <option :value="null">未分類</option>
            <option v-for="category in categories" :key="category.id" :value="category.id">
              {{ category.name }}
            </option>
          </select>

          <label for="amount">金額（{{ group.baseCurrency }}）</label>
          <input id="amount" v-model="amount" type="text" inputmode="decimal" required />

          <label for="payer">付款人</label>
          <select id="payer" v-model="payerId" required>
            <option disabled value="">請選擇付款人</option>
            <option v-for="member in members" :key="member.userId" :value="member.userId">
              {{ member.userName }}
            </option>
          </select>

          <label for="expense-at">消費時間</label>
          <input id="expense-at" v-model="expenseAt" type="datetime-local" step="1" required />

          <fieldset>
            <legend>平均分攤成員</legend>
            <label v-for="member in members" :key="member.userId">
              <input v-model="selectedUserIds" type="checkbox" :value="member.userId" />
              {{ member.userName }}
            </label>
          </fieldset>

          <label for="note">備註（選填）</label>
          <textarea id="note" v-model="note" rows="3" />
        </fieldset>

        <p v-if="submitError" role="alert">{{ submitError }}</p>
        <p v-if="completed" role="status">支出已建立</p>

        <button type="submit" :disabled="submitting || completed">
          {{ submitting ? '送出中…' : draft ? '重試送出' : '建立支出' }}
        </button>
      </form>
    </section>
  </main>
</template>
