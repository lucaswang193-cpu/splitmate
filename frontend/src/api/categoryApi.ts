import http from '@/api/http'
import type { ApiResponse, PageResponse } from '@/types/api'
import type { ExpenseCategoryResponse } from '@/types/category'

// 分頁取得共用支出分類
export async function getExpenseCategoriesApi(page = 0, size = 100) {
  const response = await http.get<
    ApiResponse<PageResponse<ExpenseCategoryResponse>>
  >('/expense-categories', {
    params: { page, size, sort: 'id,asc' },
  })

  return response.data
}

// 表單下拉選單使用完整分類清單
export async function getAllExpenseCategoriesApi() {
  const categories: ExpenseCategoryResponse[] = []
  let page = 0
  let totalPages = 1

  do {
    const result = await getExpenseCategoriesApi(page)

    if (!result.success || !result.data) {
      throw new Error(result.message || '無法取得支出分類')
    }

    categories.push(...result.data.content)
    totalPages = result.data.totalPages
    page++
  } while (page < totalPages)

  return categories
}