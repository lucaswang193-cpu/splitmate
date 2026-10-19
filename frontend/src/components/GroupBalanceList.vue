<script setup lang="ts">
import { ref, watch } from 'vue'
import axios from 'axios'
import { getGroupBalancesApi } from '@/api/expenseApi'
import type { ApiResponse } from '@/types/api'
import type { BalanceResponse } from '@/types/balance'

const props = defineProps<{
  groupId: string
  currency: string
}>()

const balances = ref<BalanceResponse[]>([])
const page = ref(0)
const totalPages = ref(0)
const loading = ref(false)
const errorMessage = ref('')
let latestRequest = 0

function formatAmount(amount: number) {
  return new Intl.NumberFormat('zh-TW', {
    style: 'currency',
    currency: props.currency,
    currencyDisplay: 'code',
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(amount)
}

function balanceLabel(amount: number) {
  if (amount > 0) return `應收 ${formatAmount(amount)}`
  if (amount < 0) return `應付 ${formatAmount(Math.abs(amount))}`
  return '已平衡'
}

async function loadBalances(targetPage = page.value) {
  const requestId = ++latestRequest
  loading.value = true
  errorMessage.value = ''

  try {
    const result = await getGroupBalancesApi(props.groupId, targetPage)
    if (requestId !== latestRequest) return

    if (!result.success || !result.data) {
      throw new Error(result.message || '無法取得分帳餘額')
    }

    balances.value = result.data.content
    page.value = result.data.page
    totalPages.value = result.data.totalPages
  } catch (error) {
    if (requestId !== latestRequest) return

    errorMessage.value = axios.isAxiosError<ApiResponse<null>>(error)
      ? error.response?.data?.message || '載入失敗，請稍後重試'
      : error instanceof Error ? error.message : '載入失敗'
  } finally {
    if (requestId === latestRequest) loading.value = false
  }
}

watch(
  () => props.groupId,
  () => {
    balances.value = []
    page.value = 0
    totalPages.value = 0
    void loadBalances(0)
  },
  { immediate: true },
)
</script>

<template>
  <section>
    <h2>分帳餘額</h2>

    <p v-if="loading" role="status">載入中…</p>

    <div v-else-if="errorMessage">
      <p role="alert">{{ errorMessage }}</p>
      <button @click="loadBalances()">重試</button>
    </div>

    <p v-else-if="balances.length === 0">目前沒有成員餘額</p>

    <div v-else class="table-scroll">
      <table>
        <caption>分帳餘額（已計入結算）</caption>
        <thead>
          <tr>
            <th scope="col">成員</th>
            <th scope="col">應分攤</th>
            <th scope="col">支出墊付</th>
            <th scope="col">應收／應付</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="balance in balances" :key="balance.userId">
            <th scope="row">{{ balance.userName }}</th>
            <td>{{ formatAmount(balance.totalShareAmount) }}</td>
            <td>{{ formatAmount(balance.totalPaidAmount) }}</td>
            <td>{{ balanceLabel(balance.netAmount) }}</td>
          </tr>
        </tbody>
      </table>
    </div>

    <nav v-if="totalPages > 1" aria-label="餘額分頁">
      <button
        :disabled="loading || page === 0"
        @click="loadBalances(page - 1)"
      >
        上一頁
      </button>
      <span>第 {{ page + 1 }} / {{ totalPages }} 頁</span>
      <button
        :disabled="loading || page >= totalPages - 1"
        @click="loadBalances(page + 1)"
      >
        下一頁
      </button>
    </nav>
  </section>
</template>

<style scoped>
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

tbody th {
  overflow-wrap: anywhere;
  min-width: 80px;
}

td {
  white-space: nowrap;
  font-variant-numeric: tabular-nums;
}

nav {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 12px;
}
</style>