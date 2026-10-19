import { computed,ref } from "vue";
import { defineStore } from "pinia";
import axios from "axios";
import {login as loginApi,getMeApi,googleLoginApi} from "@/api/authApi";
import type { LoginRequest, UserResponse } from "@/types/auth";

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string | null>(localStorage.getItem('accessToken'))
  const user = ref<UserResponse | null>(null)
  const isAuthenticated = computed(() => !!token.value && !!user.value)

  function logout() {
    token.value = null
    user.value = null
    localStorage.removeItem('accessToken')
  }

  // 重新整理頁面後，可以用既有 Token 取得使用者資料
  async function fetchMe() {
    try {
      const result = await getMeApi()
      if (!result.success || !result.data) {
        throw new Error(result.message || '無法取得使用者資料')
      }
      user.value = result.data
      return result.data
    } catch (error) {
      // Token 無效才清除登入狀態，網路錯誤交給頁面處理
      if (axios.isAxiosError(error) && error.response?.status === 401) {
        logout()
      }
      throw error
    }
  }

  async function login(request: LoginRequest) {
    const result = await loginApi(request)
    if (!result.success || !result.data) {
      throw new Error(result.message || '登入失敗')
    }

    token.value = result.data.accessToken
    localStorage.setItem('accessToken', token.value)
    user.value = null
    await fetchMe()
  }

  async function loginWithGoogle(credential: string) {
    const result = await googleLoginApi({ credential })
    if (!result.success || !result.data) {
      throw new Error(result.message || 'Google 登入失敗')
    }

    token.value = result.data.accessToken
    localStorage.setItem('accessToken', token.value)
    user.value = null
    await fetchMe()
  }

  return { token, user, isAuthenticated, login, loginWithGoogle, fetchMe, logout }
})
