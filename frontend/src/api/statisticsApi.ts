import http from '@/api/http'
import type { ApiResponse } from '@/types/api'
import type { GroupExpenseStatsResponse } from '@/types/statistics'

// from 包含起點，to 不包含終點
export async function getGroupCategoryStatsApi(
  groupId: string,
  from: string,
  to: string,
) {
  const response = await http.get<ApiResponse<GroupExpenseStatsResponse>>(
    `/expenses/groups/${encodeURIComponent(groupId)}/category-stats`,
    {
      params: { from, to },
    },
  )

  return response.data
}