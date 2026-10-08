<script setup lang="ts">
/**
 * 区域 Tab 栏。
 *
 * 区域列表由父组件从桌台数据里提取后传入，本组件只负责渲染和高亮，
 * 点击时把选中的区域名抛给父组件，自己不持有状态。
 */

const props = defineProps<{
  areas: string[]   // 区域名列表，如 ['全部', 'A区', '包房']
  active: string   // 当前选中的区域名
}>()

const emit = defineEmits<{
  change: [area: string]   // 点击某个 Tab，抛出它的区域名
}>()
</script>

<template>
  <nav class="area-tabs">
    <button
      v-for="area in props.areas"
      :key="area"
      class="area-tab"
      :class="{ active: area === props.active }"
      @click="emit('change', area)"
    >
      {{ area }}
    </button>
  </nav>
</template>

<style scoped>
.area-tabs {
  display: flex;
  gap: 8px;
  width: fit-content;   /* 宽度只包住内容，不向右铺满整行 */
  margin-left: 30px;   /* 整体距左边一点距离 */
  padding: 5px 20px;
  border-radius: 8px;
  background: #424248;
}

.area-tab {
  /* 【微调 Tab 宽度就改这行的第二个值】
     padding: 上下 左右 —— 左右值越大，每个 Tab 越宽
     当前 40px，想更宽就加大（如 48px、56px） */
  padding: 7px 50px;
  border: none;
  border-radius: 6px;
  background: transparent;
  color: #fff;   /* 未选中：白字 */
  font-size: 20px;
  cursor: pointer;
}

.area-tab.active {
  background: #14adad;   /* 选中：青色底 */
  color: #000;   /* 选中：黑字 */
}
</style>
