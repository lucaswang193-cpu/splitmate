// 對應後端 GroupMemberResponse
export interface GroupMemberResponse {
  id: number
  groupId: string
  groupName: string
  userId: string
  userName: string
  userEmail: string
  role: string
  joinedAt: string
  createdAt: string
  updatedAt: string
}
export interface CreateGroupRequest {
  name: string
  description: string | null
  baseCurrency: string
}

export interface UpdateGroupRequest {
  name: string
  description: string | null
}

export interface GroupResponse {
  id: string
  name: string
  description: string | null
  ownerId: string
  ownerName: string
  baseCurrency: string
  inviteCode: string
  createdAt: string
  updatedAt: string
}

export interface JoinGroupRequest {
  inviteCode: string
}
