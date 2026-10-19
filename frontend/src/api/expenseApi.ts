import http from '@/api/http'
import type { ApiResponse, PageResponse } from '@/types/api'
import type { ExpenseResponse, CreateExpenseRequest,UpdateExpenseRequest } from '@/types/expense'
import type { BalanceResponse } from '@/types/balance'

// 分頁取得群組支出
export async function getGroupExpensesApi(
  groupId: string,
  page = 0,
  size = 10,
) {
  const response = await http.get<
    ApiResponse<PageResponse<ExpenseResponse>>
  >('/expenses', {
    params: { groupId, page, size, sort: 'expenseAt,desc' },
  })

  return response.data
}

// 每份新草稿產生一次 ID，重試時保留原草稿
export function createExpenseDraft(
  data: Omit<CreateExpenseRequest, 'id'>,
): CreateExpenseRequest {
  return { ...data, id: crypto.randomUUID() }
}

// 新增支出
export async function createExpenseApi(request: CreateExpenseRequest) {
  const response = await http.post<ApiResponse<ExpenseResponse>>(
    '/expenses',
    request,
  )

  return response.data
}

// 分頁取得群組成員的分帳餘額
export async function getGroupBalancesApi(
  groupId: string,
  page = 0,
  size = 10,
) {
  const response = await http.get<
    ApiResponse<PageResponse<BalanceResponse>>
  >(`/expenses/groups/${encodeURIComponent(groupId)}/balances`, {
    params: { page, size },
  })

  return response.data
}
// 取得單筆支出，包含每位成員的分攤資料
export async function getExpenseApi(expenseId: string) {
  const response = await http.get<ApiResponse<ExpenseResponse>>(
    `/expenses/${encodeURIComponent(expenseId)}`,
  )

  return response.data
}
// 修改指定支出
export async function updateExpenseApi(
  expenseId: string,
  request: UpdateExpenseRequest,
) {
  const response = await http.put<ApiResponse<ExpenseResponse>>(
    `/expenses/${encodeURIComponent(expenseId)}`,
    request,
  )

  return response.data
}
// 刪除支出及其分攤紀錄
export async function deleteExpenseApi(expenseId: string) {
  const response = await http.delete<ApiResponse<null>>(
    `/expenses/${encodeURIComponent(expenseId)}`,
  )

  return response.data
}