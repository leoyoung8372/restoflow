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
      <div class="card-info">
        <i class="iconfont icon-renwu-ren"></i>
        <span>{{ cardText(table) }}</span>
      </div>
    </div>
  </div>
</template>

<style scoped>
.grid {
  display: grid;
  /* auto-fill + minmax：卡片宽度自适应，屏幕宽就多排几张，窄就少排几张。
     【改卡片宽度就改这里的 190px】数值越大卡片越宽、每行排得越少 */
  grid-template-columns: repeat(auto-fill, minmax(190px, 1fr));
  /* ============================================================
     【卡片之间的间距】
     就是下面这一行的 gap 值——它同时控制【左右间距】和【上下间距】。
     想分开控制的话可以写成两值：gap: 上下 左右; 例如 gap: 10px 16px;
     ============================================================ */
  gap:10px;
  /* 底部内边距留大一些：底部悬浮的状态筛选栏会盖住这个区域，
     留出空间让最后一行卡片滚到底时仍能完整看到 */
  padding: 24px 32px 120px;
}

.card {
  display: flex;
  flex-direction: column;
  height: 180px;   /* 【改卡片高度就改这里】，与上面的宽度值一起决定卡片的高宽比 */
  /* 卡片本身不设 padding：否则下半部分的 .card-info 会被内边距挡住，
     无法贴到卡片边缘。padding 移到 .card-name 上 */
  border-radius: 6px;
  overflow: hidden;   /* 让底部色条的两端被裁成卡片同款圆角 */
  background: #fff;
  cursor: pointer;
}

.card-name {
  padding: 20px 20px 0;   /* 上、左右留白；底部不留，交给 .card-info */
  font-size: 20px;
  font-weight: 500;
}

/* 卡片底部信息条：横向铺满卡片宽度，高度固定。
   flex 子项默认不拉伸，所以用 flex-shrink:0 保住高度不被压缩 */
.card-info {
  display: flex;
  align-items: center;
  gap: 6px;   /* 图标与人数文字的距离 */
  flex-shrink: 0;
  width: 100%;   /* 横向占满卡片 */
  height: 36px;   /* 【改底部信息条高度就改这里】 */
  padding: 0 20px;   /* 左右内边距，与卡片名对齐 */
  font-size: 16px;
  margin-top: auto;   /* 靠向卡片底部；高度固定，剩下的空间自然留在上方 */

  background-color: rgba(66, 66, 66, 0.2);
}

/* 图标字形在字体里天然偏高，用相对定位压下来与文字对齐 */
.card-info .iconfont {
  font-size: 18px;
  position: relative;
  top: 0px;   /* 【微调图标垂直位置】负值上移、正值下移 */
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
