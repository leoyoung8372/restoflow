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
        <!-- ¥ 与金额分成两个元素：¥ 固定在左侧不动，
             只有后面的数字在"金额/***"之间切换 -->
        <span class="amount-box">
          <span class="yuan">¥</span>
          <span class="num" :class="{ masked: !showAmount }">
            {{ showAmount ? props.unsettled.amount : '***' }}
          </span>
        </span>
        <i
          class="iconfont"
          :class="showAmount ? 'icon-yanjing_xianshi_o' : 'icon-yanjing_yincang_o'"
          @click="showAmount = !showAmount"
        ></i>
      </span>
    </div>

    <!-- 放大镜图标绝对定位在搜索框内部左侧，不占据 flex 空间，
         因此不会把输入框挤窄 -->
    <div class="search">
      <i class="iconfont icon-Magnifier search-icon"></i>
      <el-input
        placeholder="桌台名/首字母"
        clearable
        @update:model-value="emit('update:keyword', $event)"
      />
    </div>
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
  gap: 1px;   /* 金额与眼睛图标之间的距离 */
}

/* 金额区固定宽度 + 左对齐：¥ 钉在最左边不动，眼睛图标的位置也不会变 */
.amount-box {
  display: inline-flex;
  align-items: center;
  gap: 5px;   /* ¥ 与金额数字之间的距离 */
  width: 80px;
}

/* 星号在字体里天生偏上，用相对定位把它压下来与数字对齐。
   注意不能用 line-height —— 行高只影响行盒，改不动字形本身的位置 */
.num.masked {
  position: relative;
  top: 6px;   /* 偏上就加大；改小则上移 */
}

.amount .iconfont {
  font-size: 25px;   /* 眼睛图标比周围文字略大，便于点击 */
  cursor: pointer;
  /* 图标字形在字体里天生偏高，flex 居中只对行盒生效、管不到字形本身，
     所以用相对定位把它压下来 */
  position: relative;
  top: 3px;   /* 【微调眼睛图标的垂直位置】偏上就加大，偏下就改小或写负值 */
}

/* 绝对定位 + left:50% + translateX(-50%)，让搜索框相对整行居中
   （用 margin-left:auto 只能靠右，因为左侧内容比右侧宽） */
.search {
  position: absolute;
  left: 50%;
  transform: translateX(-50%);
  width: 620px;
}

/* 放大镜：绝对定位浮在输入框左侧之上，不参与排版 */
.search-icon {
  position: absolute;
  left: 14px;
  top: 50%;
  transform: translateY(-50%);   /* 配合 top:50% 实现垂直居中 */
  z-index: 1;
  font-size: 20px;
  color: #c9c9c9;   /* 与占位符同色，在深色底上可见 */
  pointer-events: none;   /* 点图标时事件穿透到下面的输入框，不挡聚焦 */
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
/* 高度必须设在 .el-input 上，不能设在 .el-input__wrapper 上——
   Element Plus 的链路是：.el-input 定义 --el-input-height
                        → 内部算出 --el-input-inner-height
                        → .el-input__inner 用它作真实高度。
   CSS 变量只向下继承，设在 wrapper 上到不了外面那层 */
.search :deep(.el-input) {
  --el-input-height: 40px;   /* 【改输入框高度就改这里】 */
  /* 深色底必须配浅色文字，否则输入的内容看不见。
     这三个变量分别控制：输入的文字、占位符、右侧清除按钮的图标 */
  --el-input-text-color: #fff;
  --el-input-placeholder-color: #c9c9c9;
  --el-input-icon-color: #c9c9c9;
}

.search :deep(.el-input__wrapper) {
  font-size: 17px;
  background: #424148;   /* 深色底，与顶栏近似但略深 */
  padding-left: 42px;   /* 给左侧放大镜腾位置，避免输入的文字盖住图标 */
  border-radius: 8px;   /* 圆角，比默认的 4px 稍圆润 */
  box-shadow: none;   /* 去掉 Element Plus 用 box-shadow 模拟的白色边框 */
}
</style>
