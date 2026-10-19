// 單一分類的統計
export interface CategoryExpenseStatResponse {
  categoryId: number | null
  categoryName: string
  expenseCount: number
  totalAmount: number
  percentage: number
}

// 指定群組、時間區間的統計
export interface GroupExpenseStatsResponse {
  groupId: string
  baseCurrency: string
  from: string
  to: string
  expenseCount: number
  totalAmount: number
  categories: CategoryExpenseStatResponse[]
}