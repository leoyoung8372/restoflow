<script setup lang="ts">
import type { TableCard } from '@/api/table'

/**
 * 桌台卡片区域。
 *
 * 只负责把传入的桌台数组渲染成网格，数据从父组件通过 props 传入，
 * 自己不调接口、不持有状态。
 */
defineProps<{
  tables: TableCard[]   // 要展示的桌台（父组件已按区域、状态筛好）
}>()

/** 卡片左下角文字：空台显示座位数，已开台显示"已坐/座位"。
 *  订单域未实现，peopleCount 目前恒为 null，因此会退化为显示座位数。 */
function cardText(table: TableCard): string {
  return table.peopleCount == null ? `${table.seats}人` : `${table.peopleCount}/${table.seats}`
}
</script>

<template>
  <div class="grid">
    <div
      v-for="table in tables"
      :key="table.id"
      class="card"
      :class="`status-${table.status}`"
    >
      <div class="card-name">{{ table.name }}</div>
      <div class="card-info">{{ cardText(table) }}</div>
    </div>
  </div>
</template>

<style scoped>
.grid {
  display: grid;
  /* auto-fill + minmax：卡片宽度自适应，屏幕宽就多排几张，窄就少排几张 */
  grid-template-columns: repeat(auto-fill, minmax(190px, 1fr));
  gap: 10px;
  /* 底部内边距留大一些：底部悬浮的状态筛选栏会盖住这个区域，
     留出空间让最后一行卡片滚到底时仍能完整看到 */
  padding: 24px 32px 120px;
}

.card {
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  height: 160px;
  padding: 20px;
  border-radius: 12px;
  background: #fff;
  cursor: pointer;
}

.card-name {
  font-size: 20px;
  font-weight: 500;
}

.card-info {
  font-size: 16px;
  opacity: 0.85;
}

/* 状态配色，数字对应 dining_table.status：1空台 2待下单 3待结账 4已结账 */
.status-1 {
  color: #303133;   /* 空台：白底黑字 */
}

.status-2 {
  background: #67c23a;   /* 待下单：绿 */
  color: #fff;
}

.status-3 {
  background: #f56c6c;   /* 待结账：红 */
  color: #fff;
}

.status-4 {
  background: #e6a23c;   /* 已结账：橙 */
  color: #fff;
}
</style>
