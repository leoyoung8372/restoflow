<script setup lang="ts">
/**
 * 向上滑 / 向下滑按钮。
 *
 * 只负责在点击时抛事件，具体滚哪个区域、滚多少由父组件决定——
 * 本组件不知道页面结构，因此可以在别处复用。
 */

const emit = defineEmits<{
  up: []   // 点了"向上滑"
  down: []   // 点了"向下滑"
}>()
</script>

<template>
  <!-- 两个按钮共用一个圆角外框，按钮之间不加间距，形成无缝衔接的一整体 -->
  <div class="scroll-btns">
    <button class="scroll-btn" @click="emit('up')">
      <i class="iconfont icon-xiangshang"></i>
      <span>向上滑</span>
    </button>
    <button class="scroll-btn" @click="emit('down')">
      <i class="iconfont icon-xiangxia"></i>
      <span>向下滑</span>
    </button>
  </div>
</template>

<style scoped>
.scroll-btns {
  display: flex;
  border-radius: 8px;
  overflow: hidden;   /* 配合圆角，把两个直角按钮裁成圆角 */
  box-shadow: 0 2px 10px rgb(0 0 0 / 18%);
}

/* 无缝衔接的关键：按钮之间没有 margin，也没有给外框加 gap */
.scroll-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;   /* 图标与文字之间留一点空隙 */
  height: 52px;
  padding: 0 20px;
  border: none;
  background: #fff;
  color: #000;
  font-size: 17px;
  cursor: pointer;
}

.scroll-btn .iconfont {
  font-size: 20px;   /* 图标比文字略大 */
}

.scroll-btn:first-child {
  border-right: 1px solid #ddd;   /* 两个按钮之间的分隔线，让边界看得出来但不分开 */
}

.scroll-btn:hover {
  background: #f5f5f5;
}
</style>
