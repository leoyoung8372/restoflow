<script setup lang="ts">
/**
 * 底部悬浮的状态筛选栏。
 *
 * 状态种类固定，所以选项列表直接写在组件内，不由父组件传入。
 * 点击时只抛出选中的状态值，是否筛选由父组件决定。
 */

const props = defineProps<{
  active: number   // 当前选中的状态值，0 表示"全部"
}>()

const emit = defineEmits<{
  change: [value: number]   // 点击某个状态，抛出它的值
}>()

/**
 * 选项列表。value 对应 dining_table.status 的码值（0 表示全部），
 * color 与卡片配色一致，用于左边的小方块。
 */
const options = [
  { label: '全部', value: 0, color: '#424248' },
  { label: '空台', value: 1, color: '#ffffff' },
  { label: '待下单', value: 2, color: '#67c23a' },
  { label: '待结账', value: 3, color: '#f56c6c' },
  { label: '已结账', value: 4, color: '#e6a23c' },
]
</script>

<template>
  <!-- 无缝衔接：选项之间不加间距，靠右边框分隔 -->
  <div class="status-filter">
    <div
      v-for="item in options"
      :key="item.value"
      class="status-item"
      :class="{ active: item.value === props.active }"
      @click="emit('change', item.value)"
    >
      <span class="dot" :style="{ background: item.color }"></span>
      <span class="label">{{ item.label }}</span>
      <!-- 数量：功能未实现，先留空 -->
      <span class="count"></span>
    </div>
  </div>
</template>

<style scoped>
.status-filter {
  display: flex;
  overflow: hidden;   /* 配合圆角，把两端的选项裁圆 */
  border-radius: 10px;
  background: #fff;
  box-shadow: 0 4px 16px rgb(0 0 0 / 20%);
}

.status-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 0 24px;
  height: 56px;
  color: #c9c9c9;   /* 未选中：浅灰字 */
  font-size: 16px;
  cursor: pointer;
}

/* 选项之间用细线分隔，实现"无缝衔接但不糊成一片" */
.status-item + .status-item {
  border-left: 1px solid #ebeef5;
}

.status-item.active {
  background: #c9c9c9;   /* 选中：浅灰底 */
  color: #000;   /* 选中：黑字 */
}

/* 状态对应的颜色小方块；白色方块靠外描边才看得见 */
.dot {
  width: 14px;
  height: 14px;
  border-radius: 3px;
  border: 1px solid rgb(0 0 0 / 25%);
}

.count {
  min-width: 20px;   /* 预留位置，将来显示数量 */
}
</style>
