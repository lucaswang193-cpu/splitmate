export interface ApiResponse<T> {
  success: boolean
  message: string
  errorCode: string | null
  data: T | null
}

export interface PageResponse<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}