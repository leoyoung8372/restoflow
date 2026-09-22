import axios from 'axios'
import { ElMessage } from 'element-plus'

/**
 * axios 统一封装。
 *
 * 页面里调用接口时只需写：
 *   const data = await request.post('/auth/login', { username, password })
 *
 * 本文件负责三件事，页面无需重复处理：
 *   1. 自动加 /api 前缀
 *   2. 自动携带 token
 *   3. 剥掉 { code, message, data } 外层，只把 data 交给页面；
 *      出错时统一提示，401 时清除登录状态并跳回登录页
 */

// localStorage 的 key 常量：本文件读、stores/auth.ts 写，共用同一份定义，
// 避免两处各写一个字符串，改了一处忘了另一处导致登录失效
export const TOKEN_KEY = 'token'
export const USER_KEY = 'user'

// ① 创建实例：baseURL 让页面里只写 '/auth/login' 就等于 '/api/auth/login'
const request = axios.create({
  baseURL: '/api',
  timeout: 10000, // 超过 10 秒没响应就中断，避免界面一直转圈
})

// ② 请求拦截器：发出前给每个请求自动带上 token
request.interceptors.request.use((config) => {
  // 登录成功后 token 存在 localStorage，这里直接读，不依赖 Pinia
  const token = localStorage.getItem(TOKEN_KEY)
  if (token) {
    // 格式与后端约定一致：Authorization: Bearer <token>
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// ③ 响应拦截器：拿到响应后先剥壳，或统一处理错误
request.interceptors.response.use(
  // 成功分支：HTTP 2xx 才会进这里
  // 后端返回 { code, message, data }，页面只关心 data，所以这里剥掉外层
  (response) => response.data.data,

  // 失败分支：HTTP 非 2xx（400/401/403/404/409/500）都会进这里
  (error) => {
    const status = error.response?.status
    // 后端的错误提示在响应体里，如 { code:0, message:"工号或密码错误", data:null }
    const message = error.response?.data?.message ?? '网络异常，请稍后重试'

    if (status === 401 && !error.config?.url?.includes('/auth/login')) {
      // token 失效（登录接口的 401 是"密码错"，不是 token 失效，要排除）：
      // 清掉本地登录状态，整页跳回登录页。
      // 用 location.href 而非 router，是为了强制刷新、清干净残留状态
      localStorage.removeItem(TOKEN_KEY)
      localStorage.removeItem(USER_KEY)
      if (window.location.pathname !== '/login') {
        window.location.href = '/login'
      }
    } else {
      // 统一弹提示：包括登录失败，页面不必再写一遍
      ElMessage.error(message)
    }

    // 抛出错误，让调用方可以用 try/catch 决定是否额外处理
    return Promise.reject(new Error(message))
  },
)

export default request
