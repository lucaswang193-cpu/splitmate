<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import axios from 'axios'
import {
  getLoginMethodsApi,
  setPasswordApi,
  linkGoogleApi,
  unlinkGoogleApi,
} from '@/api/authApi'
import type { ApiResponse } from '@/types/api'
import type { LoginMethodsResponse } from '@/types/auth'

type GoogleIdentity = {
  initialize: (options: {
    client_id: string
    callback: (response: { credential: string }) => void
  }) => void
  renderButton: (
    element: HTMLElement,
    options: { theme: 'outline'; size: 'large'; width: number },
  ) => void
  revoke: (
    subject: string,
    callback: (response: { successful: boolean; error?: string }) => void,
  ) => void
}

const methods = ref<LoginMethodsResponse | null>(null)
const busy = ref(false)
const message = ref('')
const errorMessage = ref('')
const googleError = ref('')
const newPassword = ref('')
const confirmPassword = ref('')
const currentPassword = ref('')
const googleButton = ref<HTMLDivElement | null>(null)
const googleForm = ref<HTMLFormElement | null>(null)
const pendingRevocation = ref<string | null>(null)
let google: GoogleIdentity | undefined
let active = true

onBeforeUnmount(() => { active = false })

async function run(action: () => Promise<void>) {
  if (busy.value || !active) return
  busy.value = true
  errorMessage.value = ''
  message.value = ''
  try {
    await action()
  } catch (error) {
    if (!active) return
    errorMessage.value = axios.isAxiosError<ApiResponse<unknown>>(error)
      ? error.response?.data?.message || '無法連線至伺服器，請稍後再試'
      : error instanceof Error ? error.message : '操作失敗，請稍後再試'
  } finally {
    if (active) busy.value = false
  }
}

async function loadMethods() {
  const result = await getLoginMethodsApi()
  if (!result.success || !result.data) {
    throw new Error(result.message || '無法取得登入方式')
  }
  if (active) methods.value = result.data
}

async function handleGoogleCredential(credential: string) {
  if (!active || !methods.value || pendingRevocation.value) return
  if (!googleForm.value?.reportValidity()) return
  if (methods.value.passwordSet && methods.value.googleLinked) return

  await run(async () => {
    if (!methods.value) return
    if (!methods.value.passwordSet) {
      if (!methods.value.googleLinked) {
        throw new Error('目前沒有可驗證的 Google 綁定，請重新整理')
      }
      if (newPassword.value !== confirmPassword.value) {
        throw new Error('兩次輸入的密碼不一致')
      }
      if (new TextEncoder().encode(newPassword.value).length > 72) {
        throw new Error('密碼不可超過 72 個 UTF-8 位元組')
      }
      const result = await setPasswordApi({
        credential,
        newPassword: newPassword.value,
      })
      if (!result.success) throw new Error(result.message || '設定密碼失敗')
      if (!active) return
      methods.value.passwordSet = true
      newPassword.value = ''
      confirmPassword.value = ''
      message.value = '密碼已設定，可以使用 Email 和密碼登入'
    } else {
      const result = await linkGoogleApi({
        credential,
        currentPassword: currentPassword.value,
      })
      if (!result.success) throw new Error(result.message || '綁定 Google 失敗')
      if (!active) return
      methods.value.googleLinked = true
      currentPassword.value = ''
      message.value = 'Google 帳號已綁定'
    }
  })
}

async function revokePending() {
  const subject = pendingRevocation.value
  if (!subject) return
  if (!google) throw new Error('Google SDK 尚未載入，請到 Google 帳戶設定撤銷授權')

  await new Promise<void>((resolve, reject) => {
    const timer = window.setTimeout(
      () => reject(new Error('撤銷 Google 授權逾時，請重試')),
      15000,
    )
    try {
      google!.revoke(subject, (result) => {
        window.clearTimeout(timer)
        if (result.successful) resolve()
        else reject(new Error('Google 授權撤銷失敗，請重試或到 Google 帳戶設定移除'))
      })
    } catch (error) {
      window.clearTimeout(timer)
      reject(error)
    }
  })
  if (!active) return
  pendingRevocation.value = null
  message.value = 'Google 綁定已解除，授權也已撤銷'
}

async function handleUnlink() {
  if (!methods.value?.passwordSet || !methods.value.googleLinked) return
  if (!window.confirm('確定解除 Google 綁定並撤銷授權？之後請使用 Email 和密碼登入。')) return

  await run(async () => {
    const result = await unlinkGoogleApi({ currentPassword: currentPassword.value })
    if (!result.success || !result.data) {
      throw new Error(result.message || '解除 Google 綁定失敗')
    }
    if (!active || !methods.value) return
    methods.value.googleLinked = false
    currentPassword.value = ''
    pendingRevocation.value = result.data.googleSubject
    message.value = 'Splitmate 的 Google 綁定已解除'
    await revokePending()
  })
}

onMounted(() => {
  google = (window as Window & {
    google?: { accounts: { id: GoogleIdentity } }
  }).google?.accounts.id
  const clientId = import.meta.env.VITE_GOOGLE_CLIENT_ID
  if (google && clientId && googleButton.value) {
    try {
      google.initialize({
        client_id: clientId,
        callback: ({ credential }) => { void handleGoogleCredential(credential) },
      })
      google.renderButton(googleButton.value, {
        theme: 'outline', size: 'large', width: 280,
      })
    } catch {
      googleError.value = 'Google 登入元件載入失敗，請重新整理'
    }
  } else {
    googleError.value = 'Google 登入元件尚未就緒，請檢查設定或重新整理'
  }
  void run(loadMethods)
})
</script>

<template>
  <main>
    <RouterLink to="/app">返回首頁</RouterLink>
    <h1>帳號安全</h1>
    <p v-if="busy" role="status">處理中...</p>
    <p v-if="message" role="status">{{ message }}</p>
    <p v-if="errorMessage" role="alert">{{ errorMessage }}</p>
    <p v-if="googleError" role="alert">{{ googleError }}</p>
    <button v-if="!methods && !busy" @click="run(loadMethods)">重新載入</button>

    <section v-if="methods">
      <h2>登入方式</h2>
      <dl>
        <dt>密碼登入</dt><dd>{{ methods.passwordSet ? '已設定' : '未設定' }}</dd>
        <dt>Google</dt><dd>{{ methods.googleLinked ? '已綁定' : '未綁定' }}</dd>
      </dl>
    </section>

    <form ref="googleForm" v-show="methods && (!methods.passwordSet || !methods.googleLinked) && !pendingRevocation"
      @submit.prevent>
      <fieldset :disabled="busy || !methods">
        <legend>{{ methods?.passwordSet ? '綁定 Google' : '設定密碼' }}</legend>
        <template v-if="methods && !methods.passwordSet">
          <label for="new-password">新密碼</label>
          <input id="new-password" v-model="newPassword" type="password"
            autocomplete="new-password" minlength="8" maxlength="72" required />
          <label for="confirm-password">確認新密碼</label>
          <input id="confirm-password" v-model="confirmPassword" type="password"
            autocomplete="new-password" minlength="8" maxlength="72" required />
        </template>
        <template v-else>
          <label for="link-password">目前密碼</label>
          <input id="link-password" v-model="currentPassword" type="password"
            autocomplete="current-password" maxlength="255" required />
        </template>
        <div ref="googleButton"></div>
      </fieldset>
    </form>

    <form v-if="methods?.googleLinked && methods.passwordSet" @submit.prevent="handleUnlink">
      <fieldset :disabled="busy">
        <legend>解除 Google 綁定</legend>
        <label for="unlink-password">目前密碼</label>
        <input id="unlink-password" v-model="currentPassword" type="password"
          autocomplete="current-password" maxlength="255" required />
        <button type="submit">解除綁定並撤銷授權</button>
      </fieldset>
    </form>

    <section v-if="pendingRevocation">
      <h2>Google 授權尚未確認撤銷</h2>
      <button :disabled="busy" @click="run(revokePending)">重試撤銷授權</button>
      <a href="https://myaccount.google.com/connections" target="_blank" rel="noopener noreferrer">
        Google 帳戶設定
      </a>
    </section>
  </main>
</template>
