import request from './request'

/**
 * 认证接口：登录。
 *
 * 这里定义的 TypeScript 类型，字段名必须与后端 Java DTO 完全一致——
 * 传输靠字段名匹配，写错了拿到的是 undefined，且不会报错。
 */

/** 登录入参，对应后端 LoginRequest */
export interface LoginParams {
  /** 工号，如 admin */
  username: string
  /** 明文密码，仅用于本次校验 */
  password: string
}

/** 会话用户，对应后端 SessionUser（不含密码） */
export interface SessionUser {
  id: number
  username: string
  realName: string
  /** 角色：admin / manager / supervisor / chef */
  role: string
}

/** 登录返回，对应后端 LoginResponse */
export interface LoginResult {
  /** 登录凭证，有效期 6 小时 */
  token: string
  user: SessionUser
}

/**
 * 登录。
 *
 * request 的响应拦截器已经剥掉了 { code, message, data } 外层，
 * 所以这里拿到的直接是 LoginResult。
 *
 * @param params 工号与密码
 * @returns token 与用户信息
 */
export function login(params: LoginParams): Promise<LoginResult> {
  return request.post('/auth/login', params)
}
