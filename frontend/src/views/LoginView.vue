<script setup lang="ts">
import { onMounted, onBeforeUnmount, ref } from 'vue'
import axios from 'axios'
import { RouterLink, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import type { ApiResponse } from '@/types/api'

type GoogleIdentity = {
  initialize: (options: {
    client_id: string
    callback: (response: { credential: string }) => void
  }) => void
  renderButton: (
    element: HTMLElement,
    options: { theme: 'outline'; size: 'large'; text: 'signin_with' },
  ) => void
}

const router = useRouter()
const auth = useAuthStore()
const email = ref('')
const password = ref('')
const loading = ref(false)
const errorMessage = ref('')
const googleError = ref('')
const googleButton = ref<HTMLDivElement | null>(null)
let active = true

onBeforeUnmount(() => { active = false })

async function submitLogin(action: () => Promise<void>) {
  if (loading.value || !active) return
  loading.value = true
  errorMessage.value = ''

  try {
    await action()
    if (active) await router.replace('/app')
  } catch (error) {
    if (!active) return
    errorMessage.value = axios.isAxiosError<ApiResponse<null>>(error)
      ? error.response?.data?.message || '無法連線至伺服器，請稍後再試'
      : error instanceof Error ? error.message : '登入失敗，請稍後再試'
  } finally {
    if (active) loading.value = false
  }
}

function handleLogin() {
  return submitLogin(() =>
    auth.login({ email: email.value.trim(), password: password.value }),
  )
}

onMounted(() => {
  const clientId = import.meta.env.VITE_GOOGLE_CLIENT_ID
  const google = (window as Window & {
    google?: { accounts: { id: GoogleIdentity } }
  }).google

  if (!clientId || !google || !googleButton.value) {
    googleError.value = 'Google 登入尚未就緒，請確認設定或重新整理'
    return
  }

  google.accounts.id.initialize({
    client_id: clientId,
    callback: ({ credential }) => {
      void submitLogin(() => auth.loginWithGoogle(credential))
    },
  })
  google.accounts.id.renderButton(googleButton.value, {
    theme: 'outline',
    size: 'large',
    text: 'signin_with',
  })
})
</script>

<template>
  <main>
    <h1>登入 Splitmate</h1>
    <form @submit.prevent="handleLogin">
      <fieldset :disabled="loading">
        <legend>帳號登入</legend>
        <label for="email">Email</label>
        <input id="email" v-model="email" type="email"
          autocomplete="username" maxlength="255" required />
        <label for="password">密碼</label>
        <input id="password" v-model="password" type="password"
          autocomplete="current-password" minlength="8" maxlength="255" required />
        <button type="submit">{{ loading ? '登入中…' : '登入' }}</button>
      </fieldset>
    </form>
    <fieldset :disabled="loading">
      <legend>Google 登入</legend>
      <div ref="googleButton"></div>
      <p v-if="googleError" role="alert">{{ googleError }}</p>
    </fieldset>
    <p v-if="errorMessage" role="alert">{{ errorMessage }}</p>
    <RouterLink to="/register">還沒有帳號？建立帳號</RouterLink>
  </main>
</template>