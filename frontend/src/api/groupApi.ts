import http from '@/api/http'
import type { ApiResponse, PageResponse } from '@/types/api'
import type { GroupMemberResponse,CreateGroupRequest,GroupResponse,JoinGroupRequest,UpdateGroupRequest } from '@/types/group'

// 取得目前使用者加入的群組紀錄
export async function getMyGroupsApi(page = 0, size = 10) {
  const response = await http.get<
    ApiResponse<PageResponse<GroupMemberResponse>>
  >('/group-members/me', {
    params: { page, size },
  })

  return response.data
}
//建立群組
export async function createGroupApi(request: CreateGroupRequest) {
  const response = await http.post<ApiResponse<GroupResponse>>('/groups', request)
  return response.data
}

// 使用邀請碼加入群組
export async function joinGroupApi(request: JoinGroupRequest) {
  const response = await http.post<ApiResponse<GroupMemberResponse>>(
    '/group-members/join',
    request,
  )

  return response.data
}
// 依群組 ID 取得群組詳情
export async function getGroupApi(groupId: string) {
  const response = await http.get<ApiResponse<GroupResponse>>(
    `/groups/${encodeURIComponent(groupId)}`,
  )

  return response.data
}

export async function updateGroupApi(groupId: string, request: UpdateGroupRequest) {
  const response = await http.put<ApiResponse<GroupResponse>>(
    `/groups/${encodeURIComponent(groupId)}`,
    request,
  )
  return response.data
}
// 分頁取得指定群組的成員
export async function getGroupMembersApi(
  groupId: string,
  page = 0,
  size = 10,
) {
  const response = await http.get<
    ApiResponse<PageResponse<GroupMemberResponse>>
  >(`/group-members/groups/${encodeURIComponent(groupId)}`, {
    params: { page, size },
  })

  return response.data
}
// 退出群組
export async function leaveGroupApi(groupId: string) {
  const response = await http.delete<ApiResponse<null>>(
    `/group-members/groups/${encodeURIComponent(groupId)}/me`,
  )
  return response.data
}
// 轉讓群組擁有者
export async function transferGroupOwnerApi(
  groupId: string,
  newOwnerId: string,
) {
  const response = await http.put<ApiResponse<GroupResponse>>(
    `/groups/${encodeURIComponent(groupId)}/owner`,
    { newOwnerId },
  )
  return response.data
}

// 群組擁有者移除成員
export async function removeGroupMemberApi(groupId: string, userId: string) {
  const response = await http.delete<ApiResponse<null>>(
    `/group-members/groups/${encodeURIComponent(groupId)}/users/${encodeURIComponent(userId)}`,
  )
  return response.data
}
