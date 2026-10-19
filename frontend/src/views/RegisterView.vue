<script setup lang="ts">
import { ref } from 'vue'
import axios from 'axios'
import { RouterLink, useRouter } from 'vue-router'
import { register } from '@/api/authApi'
import type { ApiResponse } from '@/types/api'

const router = useRouter()
const name = ref('')
const email = ref('')
const password = ref('')
const phoneNumber = ref('')
const loading = ref(false)
const errorMessage = ref('')

async function handleRegister() {
  if (loading.value) return
  loading.value = true
  errorMessage.value = ''

  try {
    const result = await register({
      name: name.value.trim(),
      email: email.value.trim(),
      password: password.value,
      phoneNumber: phoneNumber.value.trim() || null,
      avatarUrl: null,
    })
    if (!result.success) {
      throw new Error(result.message || '註冊失敗')
    }
    await router.push('/login')
  } catch (error) {
    errorMessage.value = axios.isAxiosError<ApiResponse<null>>(error)
      ? error.response?.data?.message || '無法連線至伺服器，請稍後再試'
      : error instanceof Error ? error.message : '註冊失敗，請稍後再試'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <main>
    <h1>建立 Splitmate 帳號</h1>
    <form @submit.prevent="handleRegister">
      <fieldset :disabled="loading">
        <legend>註冊資料</legend>
        <label for="name">姓名</label>
        <input id="name" v-model="name" autocomplete="name" maxlength="100" required />

        <label for="email">Email</label>
        <input id="email" v-model="email" type="email" autocomplete="username" maxlength="255" required />

        <label for="password">密碼</label>
        <input id="password" v-model="password" type="password" autocomplete="new-password" minlength="8" maxlength="255" required />

        <label for="phone">手機號碼（選填）</label>
        <input id="phone" v-model="phoneNumber" type="tel" autocomplete="tel" maxlength="30" />

        <p v-if="errorMessage" role="alert">{{ errorMessage }}</p>
        <button type="submit">{{ loading ? '註冊中…' : '建立帳號' }}</button>
      </fieldset>
    </form>
    <RouterLink to="/login">返回登入</RouterLink>
  </main>
</template>