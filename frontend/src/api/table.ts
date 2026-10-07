import request from './request'

/**
 * 桌台接口。
 *
 * 这里的类型字段名必须与后端 TableCardVO 完全一致——传输靠字段名匹配。
 */

/** 桌台卡片，对应后端 TableCardVO */
export interface TableCard {
  id: number
  name: string   // 桌台名，如 A1、包9
  seats: number   // 座位数
  status: number   // 1空台 2待下单 3待结账 4已结账
  areaId: number
  areaName: string   // 区域名，如 A区（后端组装）
  peopleCount: number | null   // 用餐人数；订单域未实现，当前恒为 null
  firstOrderAt: string | null   // 首单时间 yyyy-MM-dd HH:mm:ss；同上
  amount: string | null   // 消费金额，字符串如 "88.00"；同上
}

/**
 * 查询全部桌台卡片（看板用）。
 * request 的响应拦截器已剥掉 { code, message, data } 外层，这里直接拿到数组。
 */
export function getTableList(): Promise<TableCard[]> {
  return request.get('/tables')
}
