<script setup lang="ts">
import { computed, onUnmounted, ref } from 'vue'
import axios from 'axios'
import { useAuthStore } from '@/stores/auth'
import { createPhoneVerificationApi, confirmPhoneVerificationApi } from '@/api/authApi'
import type { ApiResponse } from '@/types/api'
import type { PhoneVerificationFailureResponse } from '@/types/phoneVerification'

const auth = useAuthStore()
const phone = ref(auth.user?.phoneNumber ?? '')
const requestedPhone = ref('')
const code = ref('')
const busy = ref(false)
const message = ref('')
const error = ref('')
const remaining = ref(0)
const deadline = ref(0)
const canConfirm = computed(() => !!requestedPhone.value && /^\d{6}$/.test(code.value))
const timer = window.setInterval(() => {
  remaining.value = Math.max(0, Math.ceil((deadline.value - Date.now()) / 1000))
}, 1000)
onUnmounted(() => window.clearInterval(timer))

async function submit(confirm: boolean) {
  if (busy.value || (!confirm && remaining.value > 0)) return
  const userId = auth.user?.id
  if (!userId) {
    error.value = '請先登入'
    return
  }
  if (confirm && !canConfirm.value) {
    error.value = '請輸入 6 位數驗證碼'
    return
  }
  const phoneNumber = confirm ? requestedPhone.value : phone.value.trim()
  if (!phoneNumber || phoneNumber.length > 30) {
    error.value = '請輸入有效的手機號碼'
    return
  }
  busy.value = true
  error.value = ''
  message.value = ''
  try {
    const result = confirm
      ? await confirmPhoneVerificationApi({ userId, phoneNumber, code: code.value })
      : await createPhoneVerificationApi({ userId, phoneNumber })
    if (!result.success || !result.data) throw new Error(result.message || '操作失敗')
    if (confirm) {
      requestedPhone.value = ''
      code.value = ''
      message.value = '手機驗證成功'
    } else {
      requestedPhone.value = result.data.phoneNumber
      code.value = ''
      deadline.value = Date.now() + 60_000
      remaining.value = 60
      message.value = '驗證申請已建立'
    }
    try {
      await auth.fetchMe()
    } catch {
      error.value = '操作已成功，但帳號資料更新失敗，請重新整理頁面'
    }
  } catch (cause) {
    if (axios.isAxiosError<ApiResponse<PhoneVerificationFailureResponse>>(cause)) {
      const response = cause.response?.data
      const details = response?.data

      error.value = response?.message || '連線失敗，請確認帳號狀態後再試'

      if (confirm && response?.errorCode === 'PHONE_VERIFICATION_FAILED' && details) {
        switch (details.status) {
          case 'PENDING':
            error.value =
              `驗證碼錯誤，已嘗試 ${details.attemptCount} 次，` +
              `還可嘗試 ${details.remainingAttempts} 次`
            break
          case 'FAILED':
            error.value = '已達驗證次數上限，請重新申請驗證碼'
            break
          case 'EXPIRED':
            error.value = '驗證碼已過期，請重新申請'
            break
          case 'CANCELLED':
            error.value = '這筆驗證已取消，請重新申請'
            break
        }

        if (['FAILED', 'EXPIRED', 'CANCELLED'].includes(details.status)) {
          requestedPhone.value = ''
          code.value = ''
        }
      }
    } else {
      error.value = cause instanceof Error ? cause.message : '操作失敗'
    }
  } finally {
    busy.value = false
  }
}
</script>

<template>
  <main>
    <RouterLink to="/app">返回首頁</RouterLink>
    <h1>手機驗證</h1>
    <p>目前手機：{{ auth.user?.phoneNumber || '尚未設定' }}</p>
    <p>{{ auth.user?.phoneVerified ? '已驗證' : '尚未驗證' }}</p>
    <form @submit.prevent="submit(false)">
      <label for="phone">手機號碼</label>
      <input
        id="phone"
        v-model="phone"
        type="tel"
        autocomplete="tel"
        maxlength="30"
        :disabled="busy"
        required
      />
      <button :disabled="busy || remaining > 0">
        {{ remaining > 0 ? `${remaining} 秒後可重發` : '申請驗證碼' }}
      </button>
    </form>
    <form v-if="requestedPhone" @submit.prevent="submit(true)">
      <p>驗證手機：{{ requestedPhone }}</p>
      <label for="code">驗證碼</label>
      <input
        id="code"
        v-model="code"
        inputmode="numeric"
        autocomplete="one-time-code"
        pattern="[0-9]{6}"
        maxlength="6"
        :disabled="busy"
        required
      />
      <button :disabled="busy || !canConfirm">確認驗證</button>
    </form>
    <p v-if="message" role="status">{{ message }}</p>
    <p v-if="error" role="alert">{{ error }}</p>
  </main>
</template>
