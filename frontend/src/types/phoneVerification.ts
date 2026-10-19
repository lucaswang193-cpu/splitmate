export interface CreatePhoneVerificationRequest {
  userId: string
  phoneNumber: string
}

export interface ConfirmPhoneVerificationRequest {
  userId: string
  phoneNumber: string
  code: string
}

export type PhoneVerificationStatus =
  | 'PENDING'
  | 'VERIFIED'
  | 'EXPIRED'
  | 'FAILED'
  | 'CANCELLED'

export interface PhoneVerificationResponse {
  id: number
  phoneNumber: string
  expiresAt: string
  verifiedAt: string | null
  attemptCount: number
  status: PhoneVerificationStatus
  createdAt: string
  updatedAt: string
}
export interface PhoneVerificationFailureResponse {
  attemptCount: number
  remainingAttempts: number
  status: PhoneVerificationStatus
}