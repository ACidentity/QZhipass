export interface LoginInfo {
  userId: string
  accessToken: string
}

const USER_ID_KEY = 'user_id'
const ACCESS_TOKEN_KEY = 'access_token'

export function saveLoginInfo(data: LoginInfo) {
  window.localStorage.setItem(USER_ID_KEY, data.userId)
  window.localStorage.setItem(ACCESS_TOKEN_KEY, data.accessToken)
}

export function readLoginInfo(): LoginInfo | null {
  const userId = window.localStorage.getItem(USER_ID_KEY)
  const accessToken = window.localStorage.getItem(ACCESS_TOKEN_KEY)

  if (!userId || !accessToken) {
    return null
  }

  return {
    userId,
    accessToken
  }
}

export function clearLoginInfo() {
  window.localStorage.removeItem(USER_ID_KEY)
  window.localStorage.removeItem(ACCESS_TOKEN_KEY)
}

export function isLoggedIn() {
  return Boolean(readLoginInfo())
}
