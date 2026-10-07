<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'

/**
 * 看板顶栏：首页按钮 + 未结账汇总 + 搜索框。
 *
 * 汇总数据由父组件传入；搜索词通过 update:keyword 事件抛给父组件，
 * 与父组件的 v-model:keyword 配对。组件自身不改 props。
 */

const props = defineProps<{
  unsettled: { tables: number; people: number; amount: number }   // 未结账汇总
}>()

const emit = defineEmits<{
  'update:keyword': [value: string]   // 搜索词变化，配合父组件 v-model:keyword
}>()

const router = useRouter()

// 金额是否可见。纯显示偏好，父组件不需要知道，所以放在组件内部
const showAmount = ref(true)
</script>

<template>
  <header class="topbar">
    <el-button class="home-btn" @click="router.push('/')">首页</el-button>

    <div class="summary">
      <span>未结账信息：</span>
      <span>{{ props.unsettled.tables }} 桌</span>
      <span>{{ props.unsettled.people }} 人</span>
      <span class="amount">
        <!-- ¥ 始终显示，只有金额数字随眼睛图标切换 -->
        ¥ {{ showAmount ? props.unsettled.amount : '***' }}
        <i
          class="iconfont"
          :class="showAmount ? 'icon-yanjing_xianshi_o' : 'icon-yanjing_yincang_o'"
          @click="showAmount = !showAmount"
        ></i>
      </span>
    </div>

    <el-input
      class="search"
      placeholder="桌台名/首字母"
      clearable
      @update:model-value="emit('update:keyword', $event)"
    />
  </header>
</template>

<style scoped>
.topbar {
  position: relative;   /* 作为搜索框绝对定位的基准 */
  display: flex;
  align-items: center;
  gap: 24px;
  padding: 18px 32px;
}

.summary {
  display: flex;
  align-items: center;
  gap: 20px;
  font-size: 18px;
  color: #fff;
}

.amount {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.amount .iconfont {
  font-size: 22px;   /* 眼睛图标比周围文字略大，便于点击 */
  cursor: pointer;
}

/* 绝对定位 + left:50% + translateX(-50%)，让搜索框相对整行居中
   （用 margin-left:auto 只能靠右，因为左侧内容比右侧宽） */
.search {
  position: absolute;
  left: 50%;
  transform: translateX(-50%);
  width: 620px;
}

/* Element Plus 组件渲染在子组件内部，scoped 样式够不着，需要 :deep() 穿透 */

/* 首页按钮：深色底 + 白字 + 加大高度，与顶部栏融为一体 */
.home-btn {
  --el-button-bg-color: transparent;
  --el-button-text-color: #fff;
  --el-button-border-color: rgb(255 255 255 / 40%);
  --el-button-hover-bg-color: rgb(255 255 255 / 12%);
  --el-button-hover-text-color: #fff;
  --el-button-size: 480px;   /* 按钮高度，Element Plus 用这个变量控制 */
  font-size: 17px;
}

/* 搜索框：加大高度与首页按钮齐平；白底，否则透明底 + 深色文字在深色栏上看不见 */
.search :deep(.el-input__wrapper) {
  --el-input-height: 48px;
  font-size: 17px;
  background: #fff;
}
</style>
