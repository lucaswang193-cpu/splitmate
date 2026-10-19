// 新增結算：記錄誰還款給誰
export interface CreateSettlementRequest {
  groupId: string
  fromUserId: string // 付款人 UUID
  toUserId: string   // 收款人 UUID
  amount: number
  currency: string  // 群組基本幣別
}

// 後端回傳的結算紀錄
export interface SettlementResponse {
  id: number
  groupId: string
  fromUserId: string
  fromUserName: string
  toUserId: string
  toUserName: string
  amount: number
  currency: string
  settledAt: string
  createdAt: string
  updatedAt: string
}