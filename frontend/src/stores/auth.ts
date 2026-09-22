import { defineStore } from 'pinia'
import { ref } from 'vue'
import { login as loginApi, type LoginParams, type SessionUser } from '@/api/auth'
import { TOKEN_KEY, USER_KEY } from '@/api/request'

/**
 * 登录状态管理。
 *
 * 集中回答两个问题：现在登录了吗？登录的是谁？
 * 其他页面只需 useAuthStore() 就能读到，不必各自去翻 localStorage。
 *
 * 数据同时写入 localStorage：内存（ref）负责响应式更新，localStorage 负责刷新页面后不丢。
 */

/** 从 localStorage 读用户信息；数据损坏或不存在时返回 null */
function loadUserFromStorage(): SessionUser | null {
  const raw = localStorage.getItem(USER_KEY)
  if (!raw) return null
  try {
    return JSON.parse(raw) as SessionUser
  } catch {
    // localStorage 里存了非 JSON 内容（手动改过等），当作未登录处理
    return null
  }
}

export const useAuthStore = defineStore('auth', () => {
  // ===== state =====
  /** 登录凭证；刷新页面时从 localStorage 恢复 */
  const token = ref<string | null>(localStorage.getItem(TOKEN_KEY))
  /** 当前登录用户；刷新页面时从 localStorage 恢复 */
  const user = ref<SessionUser | null>(loadUserFromStorage())

  // ===== actions =====

  /**
   * 登录：调用后端接口，成功后保存 token 与用户信息。
   * 失败时异常会向上抛（request 拦截器已弹过提示），由调用方决定是否额外处理。
   */
  async function login(params: LoginParams): Promise<void> {
    const res = await loginApi(params)

    token.value = res.token
    user.value = res.user

    // 同步写入 localStorage：request.ts 的拦截器从这里读 token
    localStorage.setItem(TOKEN_KEY, res.token)
    localStorage.setItem(USER_KEY, JSON.stringify(res.user))
  }

  /**
   * 登出：清除本地登录状态。
   * 后端还有 token 需要作废，接口尚未实现，等做登出功能时补上调用。
   */
  function logout(): void {
    token.value = null
    user.value = null
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(USER_KEY)
  }

  return { token, user, login, logout }
})
