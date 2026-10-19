<script setup lang="ts">
import { ref, watch } from 'vue'
import axios from 'axios'
import { getGroupCategoryStatsApi } from '@/api/statisticsApi'
import type { ApiResponse } from '@/types/api'
import type { GroupExpenseStatsResponse } from '@/types/statistics'

const props = defineProps<{ groupId: string }>()

const startDate = ref('')
const endDate = ref('')
const stats = ref<GroupExpenseStatsResponse | null>(null)
const loading = ref(false)
const errorMessage = ref('')
const displayedRange = ref('')
let latestRequest = 0

function formatDate(date: Date) {
  const pad = (value: number) => String(value).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
}

function parseDate(value: string) {
  if (!/^\d{4}-\d{2}-\d{2}$/.test(value)) {
    throw new Error('請選擇有效日期')
  }

  const date = new Date(`${value}T00:00:00`)
  if (Number.isNaN(date.getTime()) || formatDate(date) !== value) {
    throw new Error('請選擇有效日期')
  }
  return date
}

function formatAmount(amount: number) {
  if (!stats.value) return ''

  return new Intl.NumberFormat('zh-TW', {
    style: 'currency',
    currency: stats.value.baseCurrency,
    currencyDisplay: 'code',
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(amount)
}

function selectMonth(offset: number) {
  const now = new Date()
  startDate.value = formatDate(
    new Date(now.getFullYear(), now.getMonth() + offset, 1),
  )
  endDate.value = formatDate(
    new Date(now.getFullYear(), now.getMonth() + offset + 1, 0),
  )
  void loadStats()
}

async function loadStats() {
  const requestId = ++latestRequest
  const selectedStart = startDate.value
  const selectedEnd = endDate.value

  loading.value = true
  errorMessage.value = ''
  stats.value = null
  displayedRange.value = ''

  try {
    if (!props.groupId) throw new Error('缺少群組 ID')

    const start = parseDate(selectedStart)
    const end = parseDate(selectedEnd)

    if (start.getTime() > end.getTime()) {
      throw new Error('開始日期不可晚於結束日期')
    }

    // 使用者選擇的結束日包含整天，API 則傳隔天零點
    const exclusiveEnd = new Date(end)
    exclusiveEnd.setDate(exclusiveEnd.getDate() + 1)

    const result = await getGroupCategoryStatsApi(
      props.groupId,
      `${selectedStart}T00:00:00`,
      `${formatDate(exclusiveEnd)}T00:00:00`,
    )

    if (requestId !== latestRequest) return
    if (!result.success || !result.data) {
      throw new Error(result.message || '無法取得支出統計')
    }

    stats.value = result.data
    displayedRange.value = `${selectedStart} 至 ${selectedEnd}`
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
  () => selectMonth(0),
  { immediate: true },
)
</script>

<template>
  <section class="expense-stats">
    <h2>分類支出統計</h2>

    <form @submit.prevent="loadStats">
      <fieldset :disabled="loading">
        <legend>消費日期區間</legend>

        <div class="presets">
          <button type="button" @click="selectMonth(0)">本月</button>
          <button type="button" @click="selectMonth(-1)">上月</button>
        </div>

        <div class="date-range">
          <label>
            開始日期
            <input v-model="startDate" type="date" required />
          </label>

          <label>
            結束日期
            <input
              v-model="endDate"
              type="date"
              :min="startDate || undefined"
              required
            />
          </label>

          <button type="submit">查詢</button>
        </div>
      </fieldset>
    </form>

    <p v-if="loading" role="status">載入統計中…</p>

    <div v-else-if="errorMessage">
      <p role="alert">{{ errorMessage }}</p>
      <button @click="loadStats()">重試</button>
    </div>

    <template v-else-if="stats">
      <p>{{ displayedRange }}</p>

      <dl class="summary">
        <div>
          <dt>支出總額</dt>
          <dd>{{ formatAmount(stats.totalAmount) }}</dd>
        </div>
        <div>
          <dt>支出筆數</dt>
          <dd>{{ stats.expenseCount }} 筆</dd>
        </div>
      </dl>

      <p v-if="stats.expenseCount === 0">這段期間沒有支出</p>

      <div v-else class="table-scroll">
        <table>
          <caption>分類占比（{{ stats.baseCurrency }}，不含還款）</caption>
          <thead>
            <tr>
              <th scope="col">分類</th>
              <th scope="col">筆數</th>
              <th scope="col">金額</th>
              <th scope="col">占比</th>
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="category in stats.categories"
              :key="category.categoryId ?? 'uncategorized'"
            >
              <th scope="row">{{ category.categoryName }}</th>
              <td>{{ category.expenseCount }}</td>
              <td class="amount">
                {{ formatAmount(category.totalAmount) }}
              </td>
              <td>
                <div class="percentage">
                  <meter
                    min="0"
                    max="100"
                    :value="category.percentage"
                    :aria-label="`${category.categoryName}支出占比`"
                  />
                  <span>{{ category.percentage.toFixed(2) }}%</span>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </template>
  </section>
</template>

<style scoped>
.expense-stats {
  min-width: 0;
  margin-top: 24px;
  overflow-wrap: anywhere;
}

fieldset {
  min-width: 0;
  padding: 12px;
  border: 1px solid #ccc;
}

.presets,
.date-range {
  display: flex;
  align-items: end;
  flex-wrap: wrap;
  gap: 12px;
}

.presets {
  margin-bottom: 12px;
}

.date-range label {
  display: grid;
  gap: 6px;
  min-width: 0;
}

input {
  box-sizing: border-box;
  max-width: 100%;
}

.summary {
  display: flex;
  flex-wrap: wrap;
  gap: 16px 32px;
}

.summary dd {
  margin: 4px 0 0;
  font-weight: 600;
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

.amount {
  white-space: nowrap;
  font-variant-numeric: tabular-nums;
}

.percentage {
  display: flex;
  align-items: center;
  gap: 8px;
  white-space: nowrap;
  font-variant-numeric: tabular-nums;
}

meter {
  width: 100px;
  height: 16px;
  flex-shrink: 0;
}
</style>