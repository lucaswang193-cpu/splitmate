<script setup lang="ts">
import { ref, watch } from 'vue'
import axios from 'axios'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { getGroupApi, getGroupMembersApi } from '@/api/groupApi'
import { createSettlementApi } from '@/api/settlementApi'
import type { ApiResponse } from '@/types/api'
import type { GroupResponse, GroupMemberResponse } from '@/types/group'

const route = useRoute()
const router = useRouter()

const group = ref<GroupResponse | null>(null)
const members = ref<GroupMemberResponse[]>([])
const loading = ref(false)
const loadError = ref('')
const submitting = ref(false)
const submitError = ref('')
const uncertain = ref(false)
const completed = ref(false)

const fromUserId = ref('')
const toUserId = ref('')
const amount = ref('')
let latestRequest = 0

function errorMessage(error: unknown, fallback: string) {
  if (axios.isAxiosError<ApiResponse<null>>(error)) {
    return error.response?.data?.message || fallback
  }
  return error instanceof Error ? error.message : fallback
}

async function loadInitialData() {
  const requestId = ++latestRequest
  loading.value = true
  loadError.value = ''
  submitError.value = ''
  group.value = null
  members.value = []
  fromUserId.value = ''
  toUserId.value = ''
  amount.value = ''
  uncertain.value = false
  completed.value = false

  try {
    const groupId = route.params.groupId
    if (typeof groupId !== 'string' || !groupId) {
      throw new Error('缺少群組 ID')
    }

    const groupResult = await getGroupApi(groupId)
    if (requestId !== latestRequest) return
    if (!groupResult.success || !groupResult.data) {
      throw new Error(groupResult.message || '無法取得群組')
    }

    const allMembers: GroupMemberResponse[] = []
    let page = 0
    let totalPages = 1

    // 載入全部成員，避免選單只顯示第一頁
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
    group.value = groupResult.data
  } catch (error) {
    if (requestId !== latestRequest) return
    loadError.value = errorMessage(error, '載入失敗，請稍後重試')
  } finally {
    if (requestId === latestRequest) loading.value = false
  }
}

function parseAmount() {
  const text = amount.value.trim()
  if (!/^\d+(\.\d{1,2})?$/.test(text)) {
    throw new Error('請輸入正數金額，最多兩位小數')
  }

  const [whole = '0', fraction = ''] = text.split('.')
  const cents = Number(whole) * 100 + Number(fraction.padEnd(2, '0'))

  if (!Number.isSafeInteger(cents) || cents < 1 || cents > 999999999) {
    throw new Error('金額需介於 0.01 與 9,999,999.99')
  }

  return cents / 100
}

async function handleCreate() {
  if (submitting.value || uncertain.value || completed.value) return
  submitError.value = ''

  let settlementAmount: number

  try {
    if (!group.value) throw new Error('群組尚未載入')

    const memberIds = new Set(members.value.map((member) => member.userId))
    if (!memberIds.has(fromUserId.value) || !memberIds.has(toUserId.value)) {
      throw new Error('請選擇付款人與收款人')
    }
    if (fromUserId.value === toUserId.value) {
      throw new Error('付款人與收款人不能相同')
    }

    settlementAmount = parseAmount()
  } catch (error) {
    submitError.value = errorMessage(error, '請確認表單內容')
    return
  }

  const currentGroup = group.value
  if (!currentGroup) return

  const requestId = latestRequest
  submitting.value = true

  try {
    const result = await createSettlementApi({
      groupId: currentGroup.id,
      fromUserId: fromUserId.value,
      toUserId: toUserId.value,
      amount: settlementAmount,
      currency: currentGroup.baseCurrency,
    })

    if (requestId !== latestRequest) return

    if (!result.success || !result.data) {
      uncertain.value = true
      submitError.value = '無法確認是否已記錄，請先查看結算紀錄'
      return
    }

    completed.value = true
  } catch (error) {
    if (requestId !== latestRequest) return

    const status = axios.isAxiosError(error) ? error.response?.status : undefined

    // 明確拒絕的請求可重新操作；逾時或伺服器錯誤不自動重送
    if (status === 400 || status === 401 || status === 403) {
      submitError.value = errorMessage(error, '無法新增結算，請確認資料與登入狀態')
    } else {
      uncertain.value = true
      submitError.value = '無法確認是否已記錄，請先查看結算紀錄，避免重複記帳'
    }
  } finally {
    if (requestId === latestRequest) submitting.value = false
  }

  if (completed.value && requestId === latestRequest) {
    try {
      await router.push(`/groups/${currentGroup.id}`)
    } catch {
      submitError.value = '結算已記錄，請點選返回群組'
    }
  }
}

watch(
  () => route.params.groupId,
  () => {
    submitting.value = false
    //不用等回傳Promise，因為loadInitialData()會檢查latestRequest，所以不用await也沒關係
    void loadInitialData()
  },
  { immediate: true },
)
</script>

<template>
  <main>
    <RouterLink :to="group ? `/groups/${group.id}` : '/app'">
      返回群組
    </RouterLink>

    <h1>記錄還款</h1>
    <p v-if="loading" role="status">載入中…</p>

    <div v-else-if="loadError">
      <p role="alert">{{ loadError }}</p>
      <button @click="loadInitialData()">重試</button>
    </div>

    <section v-else-if="group">
      <h2>{{ group.name }}</h2>
      <p>結算幣別：{{ group.baseCurrency }}</p>

      <p v-if="members.length < 2">至少需要兩位成員才能記錄還款</p>

      <form v-else @submit.prevent="handleCreate">
        <fieldset :disabled="submitting || uncertain || completed">
          <legend>還款資料</legend>

          <label for="from-user">付款人</label>
          <select id="from-user" v-model="fromUserId" required>
            <option disabled value="">請選擇付款人</option>
            <option
              v-for="member in members"
              :key="member.userId"
              :value="member.userId"
            >
              {{ member.userName }}（{{ member.userEmail }}）
            </option>
          </select>

          <label for="to-user">收款人</label>
          <select id="to-user" v-model="toUserId" required>
            <option disabled value="">請選擇收款人</option>
            <option
              v-for="member in members"
              :key="member.userId"
              :value="member.userId"
              :disabled="member.userId === fromUserId"
            >
              {{ member.userName }}（{{ member.userEmail }}）
            </option>
          </select>

          <label for="amount">還款金額（{{ group.baseCurrency }}）</label>
          <input
            id="amount"
            v-model="amount"
            type="text"
            inputmode="decimal"
            required
          />
        </fieldset>

        <p v-if="submitError" role="alert">{{ submitError }}</p>
        <p v-if="completed" role="status">結算已記錄</p>

        <button
          type="submit"
          :disabled="submitting || uncertain || completed"
        >
          {{ submitting ? '記錄中…' : '記錄還款' }}
        </button>
      </form>
    </section>
  </main>
</template>

<style scoped>
main {
  min-width: 0;
  overflow-wrap: anywhere;
}

fieldset {
  display: grid;
  gap: 10px;
  min-width: 0;
  margin-bottom: 12px;
}

input,
select {
  box-sizing: border-box;
  width: 100%;
  min-width: 0;
  max-width: 100%;
}
</style>