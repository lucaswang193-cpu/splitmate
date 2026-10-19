<script setup lang="ts">
import { ref, watch } from 'vue'
import axios from 'axios'
import { getGroupSettlementsApi } from '@/api/settlementApi'
import type { ApiResponse } from '@/types/api'
import type { SettlementResponse } from '@/types/settlement'

const props = defineProps<{ groupId: string }>()

const settlements = ref<SettlementResponse[]>([])
const page = ref(0)
const totalPages = ref(0)
const loading = ref(false)
const errorMessage = ref('')
let latestRequest = 0

function formatAmount(amount: number, currency: string) {
  return new Intl.NumberFormat('zh-TW', {
    style: 'currency',
    currency,
    currencyDisplay: 'code',
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(amount)
}

function formatDate(value: string) {
  // 後端回傳 LocalDateTime，保留原本時間，不另外轉換時區
  return value.replace('T', ' ').slice(0, 19)
}

async function loadSettlements(targetPage = page.value) {
  const requestId = ++latestRequest
  loading.value = true
  errorMessage.value = ''

  try {
    const result = await getGroupSettlementsApi(props.groupId, targetPage)
    if (requestId !== latestRequest) return

    if (!result.success || !result.data) {
      throw new Error(result.message || '無法取得結算紀錄')
    }

    settlements.value = result.data.content
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
    settlements.value = []
    page.value = 0
    totalPages.value = 0
    void loadSettlements(0)
  },
  { immediate: true },
)
</script>

<template>
  <section>
    <h2>結算紀錄</h2>
    <button :disabled="loading" @click="loadSettlements(0)">
      重新整理
    </button>

    <p v-if="loading" role="status">載入中…</p>

    <div v-else-if="errorMessage">
      <p role="alert">{{ errorMessage }}</p>
      <button @click="loadSettlements()">重試</button>
    </div>

    <p v-else-if="settlements.length === 0">目前沒有結算紀錄</p>

    <div v-else class="table-scroll">
      <table>
        <caption>已記錄的還款</caption>
        <thead>
          <tr>
            <th scope="col">紀錄編號</th>
            <th scope="col">付款人</th>
            <th scope="col">收款人</th>
            <th scope="col">金額</th>
            <th scope="col">記錄時間</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="settlement in settlements" :key="settlement.id">
            <td>{{ settlement.id }}</td>
            <td>{{ settlement.fromUserName }}</td>
            <td>{{ settlement.toUserName }}</td>
            <td class="amount">
              {{ formatAmount(settlement.amount, settlement.currency) }}
            </td>
            <td class="date">{{ formatDate(settlement.settledAt) }}</td>
          </tr>
        </tbody>
      </table>
    </div>

    <nav v-if="totalPages > 1" aria-label="結算紀錄分頁">
      <button
        :disabled="loading || page === 0"
        @click="loadSettlements(page - 1)"
      >
        上一頁
      </button>
      <span>第 {{ page + 1 }} / {{ totalPages }} 頁</span>
      <button
        :disabled="loading || page >= totalPages - 1"
        @click="loadSettlements(page + 1)"
      >
        下一頁
      </button>
    </nav>
  </section>
</template>

<style scoped>
section {
  min-width: 0;
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
  overflow-wrap: anywhere;
}

.amount,
.date {
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