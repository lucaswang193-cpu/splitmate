<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter, RouterLink } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { getMyGroupsApi } from '@/api/groupApi'
import type { GroupMemberResponse } from '@/types/group'

const router = useRouter()
const auth = useAuthStore()
const memberships = ref<GroupMemberResponse[]>([])
const page = ref(0)
const totalPages = ref(0)
const loading = ref(false)
const errorMessage = ref('')

async function loadGroups(targetPage = page.value) {
  if (loading.value) return
  loading.value = true
  errorMessage.value = ''
  try {
    const result = await getMyGroupsApi(targetPage)
    if (!result.success || !result.data) {
      throw new Error(result.message || '無法取得群組')
    }
    memberships.value = result.data.content
    page.value = result.data.page
    totalPages.value = result.data.totalPages
  } catch {
    errorMessage.value = '載入群組失敗，請稍後重試'
  } finally {
    loading.value = false
  }
}

async function handleLogout() {
  auth.logout()
  await router.replace('/login')
}

onMounted(() => loadGroups())
</script>

<template>
  <main>
    <h1>Splitmate</h1>
    <p>{{ auth.user?.name }}，歡迎回來</p>
    <p>
      <RouterLink :to="{ name: 'phone-verification' }"> 手機驗證 </RouterLink>
    </p>
    <p>
      <RouterLink to="/account/security">帳號安全</RouterLink>
    </p>
    <button @click="handleLogout">登出</button>
    <h2>我的群組</h2>
    <RouterLink to="/groups/new">建立群組</RouterLink>
    <RouterLink to="/groups/join">加入群組</RouterLink>
    <p v-if="loading" role="status">載入中…</p>
    <div v-else-if="errorMessage">
      <p role="alert">{{ errorMessage }}</p>
      <button @click="loadGroups()">重試</button>
    </div>
    <p v-else-if="memberships.length === 0">目前沒有加入任何群組</p>
    <ul v-else>
      <li v-for="member in memberships" :key="member.id">
        <RouterLink :to="`/groups/${member.groupId}`" style="overflow-wrap: anywhere">
          {{ member.groupName }}
        </RouterLink>
        <span>（{{ member.role }}）</span>
      </li>
    </ul>
    <nav v-if="totalPages > 1" aria-label="群組分頁">
      <button :disabled="loading || page === 0" @click="loadGroups(page - 1)">上一頁</button>
      <span>第 {{ page + 1 }} / {{ totalPages }} 頁</span>
      <button :disabled="loading || page >= totalPages - 1" @click="loadGroups(page + 1)">
        下一頁
      </button>
    </nav>
  </main>
</template>
