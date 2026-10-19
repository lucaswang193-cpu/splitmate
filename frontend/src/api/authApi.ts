import type { ApiResponse } from "@/types/api";
import type { GoogleLoginRequest, LinkGoogleRequest, LoginMethodsResponse, LoginRequest, LoginResponse, RegisterRequest, SetPasswordRequest, UnlinkGoogleRequest, UnlinkGoogleResponse, UserResponse } from "@/types/auth";
import type { ConfirmPhoneVerificationRequest, CreatePhoneVerificationRequest, PhoneVerificationResponse } from "@/types/phoneVerification";
import http from "./http";



// 登入
export async function login(request: LoginRequest): Promise<ApiResponse<LoginResponse>> {
  const response = await http.post<ApiResponse<LoginResponse>>("/auth/login", request);
  return response.data;
}

//Google 登入
export async function googleLoginApi(
  request: GoogleLoginRequest,
): Promise<ApiResponse<LoginResponse>> {
  const response = await http.post<ApiResponse<LoginResponse>>(
    '/auth/google',
    request,
  )

  return response.data
}

// 註冊
export async function register(request: RegisterRequest): Promise<ApiResponse<null>> {
  const response = await http.post<ApiResponse<null>>("/users", request);
  return response.data;
}
// 取得目前登入的使用者，JWT 由 http.ts 自動附加
export async function getMeApi(){
    const response = await http.get<ApiResponse<UserResponse>>("/auth/me");
    return response.data;
}
// 申請驗證碼；目前後端尚未串接簡訊發送。
export async function createPhoneVerificationApi(
  request: CreatePhoneVerificationRequest,
): Promise<ApiResponse<PhoneVerificationResponse>> {
  const response = await http.post<ApiResponse<PhoneVerificationResponse>>(
    '/auth/phone-verifications',
    request,
  )
  return response.data
}

// 確認使用者輸入的驗證碼。
export async function confirmPhoneVerificationApi(
  request: ConfirmPhoneVerificationRequest,
): Promise<ApiResponse<PhoneVerificationResponse>> {
  const response = await http.post<ApiResponse<PhoneVerificationResponse>>(
    '/auth/phone-verifications/confirm',
    request,
  )
  return response.data
}

export async function getLoginMethodsApi() {
  const response = await http.get<ApiResponse<LoginMethodsResponse>>(
    '/auth/login-methods',
  )
  return response.data
}

export async function setPasswordApi(request: SetPasswordRequest) {
  const response = await http.post<ApiResponse<null>>('/auth/password', request)
  return response.data
}

export async function linkGoogleApi(request: LinkGoogleRequest) {
  const response = await http.post<ApiResponse<null>>('/auth/google/link', request)
  return response.data
}

export async function unlinkGoogleApi(request: UnlinkGoogleRequest) {
  const response = await http.post<ApiResponse<UnlinkGoogleResponse>>(
    '/auth/google/unlink',
    request,
  )
  return response.data
}