<script setup lang="ts">
import { ref, watch } from 'vue'
import axios from 'axios'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { getExpenseApi, deleteExpenseApi } from '@/api/expenseApi'
import { getGroupApi } from '@/api/groupApi'
import type { ApiResponse } from '@/types/api'
import type { ExpenseResponse } from '@/types/expense'

const route = useRoute()
const router = useRouter()

const expense = ref<ExpenseResponse | null>(null)
const baseCurrency = ref('')
const loading = ref(false)
const errorMessage = ref('')
const deleting = ref(false)
const deleted = ref(false)
const deleteError = ref('')
let latestRequest = 0

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

async function loadExpense() {
  const requestId = ++latestRequest
  loading.value = true
  errorMessage.value = ''
  deleteError.value = ''
  deleting.value = false
  deleted.value = false
  expense.value = null
  baseCurrency.value = ''

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

    const groupResult = await getGroupApi(result.data.groupId)
    if (requestId !== latestRequest) return
    if (!groupResult.success || !groupResult.data) {
      throw new Error(groupResult.message || '無法取得群組')
    }

    baseCurrency.value = groupResult.data.baseCurrency
    expense.value = result.data
  } catch (error) {
    if (requestId !== latestRequest) return
    errorMessage.value = getErrorMessage(error, '載入失敗，請稍後重試')
  } finally {
    if (requestId === latestRequest) loading.value = false
  }
}

async function handleDelete() {
  if (!expense.value || deleting.value || deleted.value) return

  const current = expense.value
  const confirmed = window.confirm(
    `確定刪除「${current.title}」？\n\n` +
    '支出與分攤紀錄將移除，無法復原。\n' +
    '既有還款紀錄會保留，群組餘額將重新計算。',
  )
  if (!confirmed) return

  const requestId = latestRequest
  deleting.value = true
  deleteError.value = ''

  try {
    const result = await deleteExpenseApi(current.id)
    if (requestId !== latestRequest) return

    // 刪除成功的 data 是 null，只檢查 success
    if (!result.success) {
      throw new Error(result.message || '刪除失敗')
    }
    deleted.value = true
  } catch (error) {
    if (requestId !== latestRequest) return
    deleteError.value = getErrorMessage(
      error,
      '無法確認刪除結果，請返回群組查看支出是否仍存在',
    )
  } finally {
    if (requestId === latestRequest) deleting.value = false
  }

  if (deleted.value && requestId === latestRequest) {
    try {
      await router.replace(`/groups/${current.groupId}`)
    } catch {
      deleteError.value = '支出已刪除，請點選返回群組'
    }
  }
}

watch(() => route.params.expenseId, loadExpense, { immediate: true })
</script>

<template>
  <main>
    <RouterLink :to="expense ? `/groups/${expense.groupId}` : '/app'">
      {{ expense ? '返回群組' : '返回我的群組' }}
    </RouterLink>

    <p v-if="loading" role="status">載入中…</p>

    <div v-else-if="errorMessage">
      <p role="alert">{{ errorMessage }}</p>
      <button @click="loadExpense()">重試</button>
    </div>

    <div v-else-if="expense">
      <h1>{{ expense.title }}</h1>

      <div class="actions">
        <RouterLink
          v-if="!deleting && !deleted"
          :to="`/expenses/${expense.id}/edit`"
        >
          修改支出
        </RouterLink>
        <button
          class="delete-button"
          :disabled="deleting || deleted"
          @click="handleDelete"
        >
          {{ deleting ? '刪除中…' : '刪除支出' }}
        </button>
      </div>

      <p v-if="deleteError" role="alert">{{ deleteError }}</p>
      <p v-if="deleted" role="status">支出已刪除</p>

      <template v-if="!deleted">
        <dl>
          <dt>分類</dt>
          <dd>{{ expense.categoryName ?? '未分類' }}</dd>

          <dt>付款人</dt>
          <dd>{{ expense.payerName }}</dd>

          <dt>支出金額</dt>
          <dd>{{ formatAmount(expense.amount, expense.currency) }}</dd>

          <dt>換算匯率</dt>
          <dd>{{ expense.exchangeRate }}</dd>

          <dt>基本幣別金額</dt>
          <dd>{{ formatAmount(expense.baseAmount, baseCurrency) }}</dd>

          <dt>消費時間</dt>
          <dd>{{ expense.expenseAt.replace('T', ' ') }}</dd>

          <dt>備註</dt>
          <dd class="note">{{ expense.note || '無' }}</dd>
        </dl>

        <section>
          <h2>分攤明細</h2>
          <p v-if="expense.shares.length === 0">目前沒有分攤資料</p>

          <div v-else class="table-scroll">
            <table>
              <caption>本筆支出（{{ baseCurrency }}）</caption>
              <thead>
                <tr>
                  <th scope="col">成員</th>
                  <th scope="col">應分攤</th>
                  <th scope="col">支出墊付</th>
                  <th scope="col">本筆差額</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="share in expense.shares" :key="share.id">
                  <th scope="row">{{ share.userName }}</th>
                  <td>{{ formatAmount(share.shareAmount, baseCurrency) }}</td>
                  <td>{{ formatAmount(share.paidAmount, baseCurrency) }}</td>
                  <td>
                    {{
                      formatAmount(
                        share.paidAmount - share.shareAmount,
                        baseCurrency,
                      )
                    }}
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </section>
      </template>
    </div>
  </main>
</template>

<style scoped>
main {
  min-width: 0;
  overflow-wrap: anywhere;
}

.actions {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 16px;
  margin: 16px 0;
}

.delete-button {
  color: #b42318;
}

dt {
  margin-top: 12px;
  font-weight: 600;
}

dd {
  margin: 4px 0 0;
}

.note {
  white-space: pre-wrap;
}

section {
  margin-top: 24px;
}

.table-scroll {
  max-width: 100%;
  overflow-x: auto;
}

table {
  width: 100%;
  border-collapse: collapse;
}

caption {
  padding: 8px 0;
  text-align: left;
}

th,
td {
  padding: 10px 12px;
  border-bottom: 1px solid #ddd;
  text-align: left;
}

td {
  white-space: nowrap;
  font-variant-numeric: tabular-nums;
}
</style>
