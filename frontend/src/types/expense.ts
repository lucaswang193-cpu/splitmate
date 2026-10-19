// 每位成員的分攤資料
export interface ExpenseShareResponse {
  id: number
  expenseId: string
  userId: string
  userName: string
  shareAmount: number
  paidAmount: number
  status: string
  createdAt: string
  updatedAt: string
}

// 支出資料
export interface ExpenseResponse {
  id: string
  groupId: string
  payerId: string
  payerName: string
  title: string
  categoryId: number | null
  categoryName: string | null
  currency: string
  amount: number
  exchangeRate: number
  baseAmount: number
  expenseAt: string
  note: string | null
  shares: ExpenseShareResponse[]
  createdAt: string
  updatedAt: string
}
// 每位成員的分攤資料
export interface ExpenseSplitRequest {
  userId: string
  shareAmount: number
  paidAmount: number | null
}

// 新增支出
export interface CreateExpenseRequest {
  id: string
  groupId: string
  payerId: string
  title: string
  categoryId: number | null
  currency: string
  amount: number
  exchangeRate: number
  baseAmount: number
  expenseAt: string
  note: string | null
  splits: ExpenseSplitRequest[]
}
// 修改既有支出，不接受更換 ID 或所屬群組
export interface UpdateExpenseRequest {
  payerId: string
  title: string
  categoryId: number | null
  currency: string
  amount: number
  exchangeRate: number
  baseAmount: number
  expenseAt: string
  note: string | null
  splits: ExpenseSplitRequest[]
}
