<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { getTableList, type TableCard } from '@/api/table'
import BoardTopBar from '@/components/table-board/BoardTopBar.vue'
import AreaTabs from '@/components/table-board/AreaTabs.vue'
import TableGrid from '@/components/table-board/TableGrid.vue'
import ScrollButtons from '@/components/table-board/ScrollButtons.vue'
import StatusFilter from '@/components/table-board/StatusFilter.vue'

/**
 * 桌台看板页面容器。
 *
 * 职责：取数据、持有页面级状态，把数据分发给各个子组件。
 * 随着子组件逐个接入，这里的状态和计算属性也逐个补上。
 */

const tableList = ref<TableCard[]>([])   // 接口返回的全部桌台
const keyword = ref('')   // 搜索词，与 BoardTopBar 双向绑定
const activeArea = ref('全部')   // 当前选中的区域 Tab
const activeStatus = ref(0)   // 当前选中的状态筛选，0 表示"全部"

/** 区域 Tab 列表：从数据里提取去重后的区域名，前面加"全部"。
 *  不写死——管理员增删区域后自动跟着变，没有桌台的区域不会出现。 */
const areaTabs = computed(() => ['全部', ...new Set(tableList.value.map((t) => t.areaName))])


/** 未结账汇总：按文档口径，只有"待结账"计入；待下单还没产生消费，不算 */
const unsettled = computed(() => {
  const list = tableList.value.filter((t) => t.status === 3)
  return {
    tables: list.length,
    people: list.reduce((sum, t) => sum + (t.peopleCount ?? 0), 0),
    amount: list.reduce((sum, t) => sum + Number(t.amount ?? 0), 0),
  }
})

async function loadTables() {
  const list = await getTableList()
  // 按桌台名自然排序：A1、A2、A10 而不是 A1、A10、A2。
  // localeCompare 的 numeric 选项会把名字里的数字当作数字比较，
  // 否则纯字符串比较下 "A10" 会排在 "A2" 前面
  list.sort((a, b) => a.name.localeCompare(b.name, 'zh-CN', { numeric: true }))
  tableList.value = list
}

// 卡片区域的 DOM 引用，用于"向上滑 / 向下滑"
const gridRef = ref<InstanceType<typeof TableGrid> | null>(null)

/** 滚动卡片区域。滚动量取可视高度的 80%，比整屏少一点，便于看清续接位置 */
function scrollGrid(direction: 1 | -1) {
  const el = gridRef.value?.$el as HTMLElement | undefined
  if (!el) return   // 组件还没挂载时直接返回
  el.scrollBy({ top: el.clientHeight * 0.8 * direction, behavior: 'smooth' })
}

onMounted(loadTables)
</script>

<template>
  <div class="board">
    <!-- 顶栏：首页按钮 + 未结账汇总 + 搜索框 -->
    <BoardTopBar
      class="board-topbar"
      :unsettled="unsettled"
      v-model:keyword="keyword"
    />

    <!-- 区域 Tab 栏 -->
    <AreaTabs
      class="board-area-tabs"
      :areas="areaTabs"
      :active="activeArea"
      @change="activeArea = $event"
    />

    <!-- 桌台卡片区域：flex:1 占满剩余高度，卡片多了自己滚动 -->
    <TableGrid
      ref="gridRef"
      class="board-grid"
      :tables="tableList"
    />

    <!-- 状态筛选：悬浮在底部，水平居中 -->
    <StatusFilter
      class="status-filter-box"
      :active="activeStatus"
      @change="activeStatus = $event"
    />

    <!-- 向上滑 / 向下滑：固定在整个视口的右下角 -->
    <ScrollButtons
      class="scroll-buttons"
      @up="scrollGrid(-1)"
      @down="scrollGrid(1)"
    />

    <!-- 待接入：StatusFilter / TableOpenDialog -->
  </div>
</template>

<style scoped>
.board {
  position: relative;   /* 作为下面两个伪元素（背景层、遮罩层）的定位基准 */
  display: flex;
  flex-direction: column;
  height: 100vh;   /* 正好一屏，不溢出（全局已禁止页面滚动） */
  overflow: hidden;   /* 裁掉模糊后溢出的边缘 */
  background: #f0f2f5;   /* 图片加载前的底色 */
}

/* 卡片区域：占满顶栏、区域栏之外的剩余高度。
   全局已禁止页面滚动，所以卡片超出时由这里自己滚 */
.board-grid {
  flex: 1;
  min-height: 0;   /* flex 子项默认不收缩，加这个才允许它小于内容高度 */
  overflow-y: auto;
  /* 隐藏滚动条：视觉上干净，滚动能力（滚轮、触摸拖动）仍在，
     页面右下角的"向上滑/向下滑"按钮承担滚动提示的作用 */
  scrollbar-width: none;   /* Firefox、新版 Chrome */
}

.board-grid::-webkit-scrollbar {
  display: none;   /* Chrome、Safari、Edge */
}

/* 状态筛选：悬浮在底部，水平居中，距底部留出距离（不触底）。
   与上下滑按钮同为页面级定位，组件内部不管"放在哪" */
.status-filter-box {
  position: fixed;
  left: 50%;
  transform: translateX(-50%);   /* 配合 left:50% 实现真正居中 */
  bottom: 40px;
  z-index: 10;
}

/* 上下滑按钮：固定在整个视口的右下角，距右边和底部各 40px。
   定位写在这里而不是组件内部——组件只负责"长得像什么样"，
   "放在哪"是页面的决定，这样同一个组件在别的页面可以放到别处。
   z-index 让按钮浮在卡片之上，避免被卡片盖住 */
.scroll-buttons {
  position: fixed;
  right: 40px;
  bottom: 40px;
  z-index: 10;
}

/* 背景层：铺图片并模糊，做出"朦胧"效果
   向外扩 20px 是为了让模糊产生的虚边被父元素裁掉，不露白 */
.board::before {
  content: '';
  position: absolute;
  inset: -20px;
  z-index: 0;
  background: url('@/assets/images/伽罗_最初的交响_海报.jpg') center / cover no-repeat;
  filter: blur(6px);
}

/* 遮罩层：叠一层半透明白，压低图片存在感，让内容更清晰可读 */
.board::after {
  content: '';
  position: absolute;
  inset: 0;
  z-index: 1;
  background: rgba(0, 0, 0, 0.7);   /* 数值越大图片越淡 */
}

/* 内容压在背景层与遮罩层之上（两者见上面的 ::before / ::after）。
   只列出真正需要压层的三个区域，不用 `.board > *` 一网打尽——
   那会把上下滑按钮也变成相对定位，破坏它的右下角固定 */
.board-topbar,
.board-area-tabs,
.board-grid {
  position: relative;
  z-index: 2;
}
</style>
