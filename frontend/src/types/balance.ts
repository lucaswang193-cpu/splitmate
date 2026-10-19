export interface BalanceResponse {
  userId: string
  userName: string
  totalShareAmount: number // 應分攤總額
  totalPaidAmount: number  // 已支付總額
  netAmount: number       // 正數應收，負數應付
}