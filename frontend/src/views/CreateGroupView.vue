<script setup lang="ts">
import { ref } from 'vue'
import axios from 'axios'
import { RouterLink, useRouter } from 'vue-router'
import { createGroupApi } from '@/api/groupApi'
import type { ApiResponse } from '@/types/api'

const router = useRouter()
const name = ref('')
const description = ref('')
const baseCurrency = ref('TWD')
const loading = ref(false)
const errorMessage = ref('')
const currencies = [
  'TWD', 'USD', 'JPY', 'KRW', 'EUR', 'CNY', 'HKD',
  'SGD', 'IDR', 'THB', 'GBP', 'VND', 'AUD', 'NZD',
]

async function handleCreate() {
  if (loading.value) return
  errorMessage.value = ''
  if (!name.value.trim()) {
    errorMessage.value = '請輸入群組名稱'
    return
  }
  loading.value = true
  try {
    const result = await createGroupApi({
      name: name.value.trim(),
      description: description.value.trim() || null,
      baseCurrency: baseCurrency.value,
    })
    if (!result.success || !result.data) {
      throw new Error(result.message || '建立群組失敗')
    }
    await router.push('/app')
  } catch (error) {
    errorMessage.value = axios.isAxiosError<ApiResponse<null>>(error)
      ? error.response?.data?.message || '無法連線至伺服器，請稍後再試'
      : error instanceof Error ? error.message : '建立群組失敗'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <main>
    <h1>建立群組</h1>
    <form @submit.prevent="handleCreate">
      <fieldset :disabled="loading">
        <legend>群組資料</legend>
        <label for="name">群組名稱</label>
        <input id="name" v-model="name" maxlength="100" required />
        <label for="description">說明（選填）</label>
        <textarea id="description" v-model="description" rows="3" />
        <label for="currency">基本幣別</label>
        <select id="currency" v-model="baseCurrency" required>
          <option v-for="code in currencies" :key="code" :value="code">
            {{ code }}
          </option>
        </select>
        <p v-if="errorMessage" role="alert">{{ errorMessage }}</p>
        <button type="submit">{{ loading ? '建立中…' : '建立群組' }}</button>
      </fieldset>
    </form>
    <RouterLink to="/app">返回我的群組</RouterLink>
  </main>
</template>