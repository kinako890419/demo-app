export interface User {
  user_id: number
  user_name: string
  user_email: string
  created_at: string
}

export interface LoginResponse {
  token: string
  user: User
}
