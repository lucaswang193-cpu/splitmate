export interface LoginRequest {
  email: string
  password: string
}

// Google 登入時送出的資料，對應後端 GoogleLoginRequest
export interface GoogleLoginRequest {
  credential: string
}

// 登入成功後回傳的資料
export interface LoginResponse {
  userId: string
  name: string
  email: string
  phoneNumber: string | null
  phoneVerified: boolean
  avatarUrl: string | null
  accessToken: string
}

// 註冊時送出的資料，對應後端 CreateUserRequest
export interface RegisterRequest {
  name: string
  email: string
  password: string
  phoneNumber: string | null
  avatarUrl: string | null
}

// 使用者資料，對應後端 UserResponse
export interface UserResponse {
  id: string
  name: string
  email: string
  phoneNumber: string | null
  phoneVerified: boolean
  phoneVerifiedAt: string | null
  avatarUrl: string | null
  createdAt: string
  updatedAt: string
}

export interface LoginMethodsResponse {
  passwordSet: boolean
  googleLinked: boolean
}

export interface SetPasswordRequest {
  credential: string
  newPassword: string
}

export interface LinkGoogleRequest {
  credential: string
  currentPassword: string
}

export interface UnlinkGoogleRequest {
  currentPassword: string
}

export interface UnlinkGoogleResponse {
  googleSubject: string
}