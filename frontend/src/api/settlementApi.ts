import http from '@/api/http'
import type { ApiResponse, PageResponse } from '@/types/api'
import type {
  CreateSettlementRequest,
  SettlementResponse,
} from '@/types/settlement'

// 新增結算紀錄
export async function createSettlementApi(
  request: CreateSettlementRequest,
) {
  const response = await http.post<ApiResponse<SettlementResponse>>(
    '/settlements',
    request,
  )

  return response.data
}

// 分頁取得群組結算紀錄，最新紀錄排在前面
export async function getGroupSettlementsApi(
  groupId: string,
  page = 0,
  size = 10,
) {
  const response = await http.get<
    ApiResponse<PageResponse<SettlementResponse>>
  >(`/settlements/groups/${encodeURIComponent(groupId)}`, {
    params: {
      page,
      size,
      sort: 'id,desc',
    },
  })

  return response.data
}