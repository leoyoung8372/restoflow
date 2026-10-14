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
  peopleCount: number | null   // 用餐人数，空台时 null
  remark: string | null   // 桌台备注，空台或未填时 null
  firstOrderAt: string | null   // 首单时间；订单明细模块未实现，当前恒为 null
  amount: string | null   // 消费金额，字符串如 "88.00"，空台时 null
}

/** 开台入参，对应后端 OpenTableRequest。操作人不在此传入——由后端从 token 取 */
export interface OpenTableParams {
  peopleCount: number
  remark: string
}

/**
 * 查询全部桌台卡片（看板用）。
 * request 的响应拦截器已剥掉 { code, message, data } 外层，这里直接拿到数组。
 */
export function getTableList(): Promise<TableCard[]> {
  return request.get('/tables')
}

/** 开台：为空闲桌台创建订单 */
export function openTable(tableId: number, params: OpenTableParams): Promise<void> {
  return request.post(`/tables/${tableId}/open`, params)
}
