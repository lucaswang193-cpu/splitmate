<script setup lang="ts">
import { ref, watch } from 'vue'
import axios from 'axios'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { getExpenseApi, updateExpenseApi } from '@/api/expenseApi'
import { getGroupApi, getGroupMembersApi } from '@/api/groupApi'
import { getAllExpenseCategoriesApi } from '@/api/categoryApi'
import type { ApiResponse } from '@/types/api'
import type { ExpenseResponse, UpdateExpenseRequest } from '@/types/expense'
import type { GroupMemberResponse } from '@/types/group'
import type { ExpenseCategoryResponse } from '@/types/category'

interface SplitRow {
  userId: string
  name: string
  selected: boolean
  shareAmount: string
  paidAmount: string
}

const route = useRoute()
const router = useRouter()

const expense = ref<ExpenseResponse | null>(null)
const members = ref<GroupMemberResponse[]>([])
const categories = ref<ExpenseCategoryResponse[]>([])
const rows = ref<SplitRow[]>([])
const baseCurrency = ref('')

const title = ref('')
const payerId = ref('')
const categoryId = ref<number | null>(null)
const amount = ref('')
const expenseAt = ref('')
const note = ref('')

const loading = ref(false)
const saving = ref(false)
const saved = ref(false)
const uncertain = ref(false)
const loadError = ref('')
const saveError = ref('')
let latestRequest = 0

function message(error: unknown, fallback: string) {
  if (axios.isAxiosError<ApiResponse<null>>(error)) {
    return error.response?.data?.message || fallback
  }
  return error instanceof Error ? error.message : fallback
}

async function loadData() {
  const requestId = ++latestRequest
  loading.value = true
  saving.value = false
  saved.value = false
  uncertain.value = false
  loadError.value = ''
  saveError.value = ''
  expense.value = null
  members.value = []
  categories.value = []
  rows.value = []

  try {
    const expenseId = route.params.expenseId
    if (typeof expenseId !== 'string' || !expenseId) {
      throw new Error('缺少支出 ID')
    }

    const result = await getExpenseApi(expenseId)
    if (requestId !== latestRequest) return
    if (!result.success || !result.data) {
      throw new Error(result.message || '無法取得支出')
    }
    const original = result.data

    const [groupResult, categoryList] = await Promise.all([
      getGroupApi(original.groupId),
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
      const memberResult = await getGroupMembersApi(original.groupId, page, 100)
      if (requestId !== latestRequest) return
      if (!memberResult.success || !memberResult.data) {
        throw new Error(memberResult.message || '無法取得成員')
      }
      allMembers.push(...memberResult.data.content)
      totalPages = memberResult.data.totalPages
      page++
    } while (page < totalPages)

    const uniqueMembers = Array.from(
      new Map(allMembers.map((member) => [member.userId, member])).values(),
    )
    const memberIds = new Set(uniqueMembers.map((member) => member.userId))

    // 避免編輯時默默刪掉已離開群組者的原始分攤
    if (
      !memberIds.has(original.payerId) ||
      original.shares.some((share) => !memberIds.has(share.userId))
    ) {
      throw new Error('原始付款人或分攤成員已不在群組，目前無法編輯此筆支出')
    }

    members.value = uniqueMembers
    categories.value = categoryList
    baseCurrency.value = groupResult.data.baseCurrency
    title.value = original.title
    payerId.value = original.payerId
    categoryId.value = original.categoryId
    amount.value = String(original.amount)
    expenseAt.value = original.expenseAt
    note.value = original.note ?? ''

    rows.value = uniqueMembers.map((member) => {
      const share = original.shares.find((item) => item.userId === member.userId)
      return {
        userId: member.userId,
        name: `${member.userName}（${member.userEmail}）`,
        selected: share !== undefined,
        shareAmount: String(share?.shareAmount ?? 0),
        paidAmount: String(share?.paidAmount ?? 0),
      }
    })

    expense.value = original
  } catch (error) {
    if (requestId !== latestRequest) return
    loadError.value = message(error, '載入失敗，請稍後重試')
  } finally {
    if (requestId === latestRequest) loading.value = false
  }
}

function toCents(value: string, label: string) {
  const text = value.trim()
  if (!/^\d+(\.\d{1,2})?$/.test(text)) {
    throw new Error(`${label}需為非負數，最多兩位小數`)
  }
  const [whole = '0', fraction = ''] = text.split('.')
  const cents = Number(whole) * 100 + Number(fraction.padEnd(2, '0'))
  if (!Number.isSafeInteger(cents) || cents > 999999999) {
    throw new Error(`${label}不可超過 9,999,999.99`)
  }
  return cents
}

function buildRequest(): UpdateExpenseRequest {
  const original = expense.value
  if (!original) throw new Error('支出尚未載入')
  if (!title.value.trim()) throw new Error('請輸入支出名稱')
  if (!expenseAt.value) throw new Error('請選擇消費時間')
  if (!members.value.some((member) => member.userId === payerId.value)) {
    throw new Error('請選擇付款人')
  }
  if (
    categoryId.value !== null &&
    !categories.value.some((category) => category.id === categoryId.value)
  ) {
    throw new Error('請選擇有效分類')
  }

  const selected = rows.value.filter((row) => row.selected)
  if (selected.length === 0) throw new Error('請至少保留一位分攤成員')

  // 第一版只開放基本幣別且匯率為 1 的支出調整總金額
  const editableAmount =
    original.currency === baseCurrency.value && original.exchangeRate === 1
  const totalCents = toCents(
    editableAmount ? amount.value : String(original.baseAmount),
    '支出金額',
  )
  if (totalCents < 1) throw new Error('支出金額必須大於 0')

  let shareTotal = 0
  let paidTotal = 0
  const splits = selected.map((row) => {
    const shareCents = toCents(row.shareAmount, '分攤金額')
    const paidCents = toCents(row.paidAmount, '墊付金額')
    shareTotal += shareCents
    paidTotal += paidCents
    return {
      userId: row.userId,
      shareAmount: shareCents / 100,
      paidAmount: paidCents / 100,
    }
  })

  if (shareTotal !== totalCents) {
    throw new Error('分攤金額加總必須等於基本幣別金額')
  }
  if (paidTotal !== totalCents) {
    throw new Error('墊付金額加總必須等於基本幣別金額')
  }

  return {
    payerId: payerId.value,
    title: title.value.trim(),
    categoryId: categoryId.value,
    currency: original.currency,
    amount: editableAmount ? totalCents / 100 : original.amount,
    exchangeRate: original.exchangeRate,
    baseAmount: editableAmount ? totalCents / 100 : original.baseAmount,
    expenseAt: expenseAt.value,
    note: note.value.trim() || null,
    splits,
  }
}

async function handleSave() {
  if (saving.value || saved.value || uncertain.value || !expense.value) return
  saveError.value = ''

  let request: UpdateExpenseRequest
  try {
    request = buildRequest()
  } catch (error) {
    saveError.value = message(error, '請確認表單')
    return
  }

  const expenseId = expense.value.id
  const requestId = latestRequest
  saving.value = true

  try {
    const result = await updateExpenseApi(expenseId, request)
    if (requestId !== latestRequest) return
    if (!result.success || !result.data) {
      uncertain.value = true
      throw new Error('無法確認儲存結果，請返回詳情查看')
    }
    saved.value = true
  } catch (error) {
    if (requestId !== latestRequest) return
    const status = axios.isAxiosError(error) ? error.response?.status : undefined
    if (status !== 400 && status !== 401 && status !== 403 && status !== 409) {
      uncertain.value = true
    }
    saveError.value = message(error, '無法確認儲存結果，請返回詳情查看')
  } finally {
    if (requestId === latestRequest) saving.value = false
  }

  if (saved.value && requestId === latestRequest) {
    try {
      await router.push(`/expenses/${expenseId}`)
    } catch {
      saveError.value = '已儲存，請點選返回支出詳情'
    }
  }
}

watch(() => route.params.expenseId, loadData, { immediate: true })
</script>

<template>
  <main>
    <RouterLink :to="expense ? `/expenses/${expense.id}` : '/app'">
      {{ expense ? '返回支出詳情' : '返回我的群組' }}
    </RouterLink>
    <h1>修改支出</h1>

    <p v-if="loading" role="status">載入中…</p>
    <div v-else-if="loadError">
      <p role="alert">{{ loadError }}</p>
      <button @click="loadData()">重試</button>
    </div>

    <form v-else-if="expense" @submit.prevent="handleSave">
      <fieldset :disabled="saving || saved || uncertain">
        <legend>支出資料</legend>

        <label for="title">名稱</label>
        <input id="title" v-model="title" maxlength="200" required />

        <label for="category">分類</label>
        <select id="category" v-model="categoryId">
          <option :value="null">未分類</option>
          <option v-for="category in categories" :key="category.id" :value="category.id">
            {{ category.name }}
          </option>
        </select>

        <label for="payer">付款人</label>
        <select id="payer" v-model="payerId" required>
          <option v-for="member in members" :key="member.userId" :value="member.userId">
            {{ member.userName }}（{{ member.userEmail }}）
          </option>
        </select>

        <label for="amount">金額（{{ expense.currency }}）</label>
        <input
          id="amount"
          v-model="amount"
          inputmode="decimal"
          :readonly="expense.currency !== baseCurrency || expense.exchangeRate !== 1"
          required
        />

        <p v-if="expense.currency !== baseCurrency || expense.exchangeRate !== 1">
          基本幣別金額：{{ baseCurrency }} {{ expense.baseAmount }}
          ／匯率：{{ expense.exchangeRate }}
        </p>

        <label for="expense-at">消費時間</label>
        <input id="expense-at" v-model="expenseAt" type="datetime-local" step="1" required />

        <fieldset>
          <legend>分攤與墊付（{{ baseCurrency }}）</legend>
          <div v-for="row in rows" :key="row.userId" class="split-row">
            <label>
              <input v-model="row.selected" type="checkbox" />
              {{ row.name }}
            </label>
            <label>
              應分攤
              <input
                v-model="row.shareAmount"
                inputmode="decimal"
                :disabled="!row.selected"
                required
              />
            </label>
            <label>
              支出墊付
              <input
                v-model="row.paidAmount"
                inputmode="decimal"
                :disabled="!row.selected"
                required
              />
            </label>
          </div>
        </fieldset>

        <label for="note">備註</label>
        <textarea id="note" v-model="note" rows="3" />
      </fieldset>

      <p v-if="saveError" role="alert">{{ saveError }}</p>
      <p v-if="saved" role="status">已儲存</p>
      <button type="submit" :disabled="saving || saved || uncertain">
        {{ saving ? '儲存中…' : '儲存修改' }}
      </button>
    </form>
  </main>
</template>

<style scoped>
main {
  min-width: 0;
  overflow-wrap: anywhere;
}

fieldset {
  display: grid;
  gap: 12px;
  min-width: 0;
  margin-bottom: 12px;
}

input:not([type='checkbox']),
select,
textarea {
  box-sizing: border-box;
  width: 100%;
  min-width: 0;
}

.split-row {
  display: grid;
  gap: 8px;
  padding: 12px 0;
  border-bottom: 1px solid #ddd;
}
</style>
