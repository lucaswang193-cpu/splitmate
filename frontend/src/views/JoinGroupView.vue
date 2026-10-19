<script setup lang="ts">
import { ref } from 'vue'
import axios from 'axios'
import { RouterLink, useRouter } from 'vue-router'
import { joinGroupApi } from '@/api/groupApi'
import type { ApiResponse } from '@/types/api'

const router = useRouter()
const inviteCode = ref('')
const loading = ref(false)
const errorMessage = ref('')

async function handleJoin() {
  if (loading.value) return
  errorMessage.value = ''

  const code = inviteCode.value.trim().toUpperCase()
  if (code.length < 6 || code.length > 12) {
    errorMessage.value = '邀請碼需為 6～12 個字元'
    return
  }

  loading.value = true
  try {
    const result = await joinGroupApi({ inviteCode: code })
    if (!result.success || !result.data) {
      throw new Error(result.message || '加入群組失敗')
    }
    await router.push('/app')
  } catch (error) {
    errorMessage.value = axios.isAxiosError<ApiResponse<null>>(error)
      ? error.response?.data?.message || '無法連線至伺服器，請稍後再試'
      : error instanceof Error ? error.message : '加入群組失敗'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <main>
    <h1>加入群組</h1>
    <form @submit.prevent="handleJoin">
      <fieldset :disabled="loading">
        <legend>群組邀請</legend>
        <label for="invite-code">邀請碼</label>
        <input
          id="invite-code"
          v-model="inviteCode"
          minlength="6"
          maxlength="12"
          autocomplete="off"
          autocapitalize="characters"
          :spellcheck="false"
          required
        />
        <p v-if="errorMessage" role="alert">{{ errorMessage }}</p>
        <button type="submit">{{ loading ? '加入中…' : '加入群組' }}</button>
      </fieldset>
    </form>
    <RouterLink to="/app">返回我的群組</RouterLink>
  </main>
</template>