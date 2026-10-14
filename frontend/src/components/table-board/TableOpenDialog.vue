<script setup lang="ts">
import { ref, watch } from 'vue'
import type { TableCard } from '@/api/table'

/**
 * 开台弹窗。
 *
 * 左半是开台信息（操作人只读、人数、桌台备注），右半是 3×4 数字键盘——
 * 键盘按"焦点在哪就输入到哪"工作，与物理键盘行为一致。
 *
 * 本组件只负责收集输入，不调用接口；点"开台"后把数据抛给父组件处理。
 */

const props = defineProps<{
  visible: boolean   // 弹窗是否显示
  table: TableCard | null   // 当前要开台的桌台
  operatorName: string   // 当前登录用户姓名，只读展示
}>()

const emit = defineEmits<{
  'update:visible': [value: boolean]   // 配合父组件 v-model:visible
  confirm: [payload: { peopleCount: number; remark: string }]   // 点"开台"
}>()

const peopleCount = ref('')   // 人数，用字符串存便于逐位追加和退格
const remark = ref('')   // 桌台备注
const focusedField = ref<'peopleCount' | 'remark'>('peopleCount')   // 键盘当前作用于哪个输入框

// 每次打开弹窗都清空上次的输入，避免"上一桌的人数"残留
watch(
  () => props.visible,
  (visible) => {
    if (visible) {
      peopleCount.value = ''
      remark.value = ''
      focusedField.value = 'peopleCount'
    }
  },
)

/** 键盘按下数字：追加到当前聚焦的输入框 */
function appendDigit(digit: string) {
  if (focusedField.value === 'peopleCount') {
    // 人数最多两位（上限 99）
    if (peopleCount.value.length >= 2) return
    peopleCount.value += digit
  } else {
    if (remark.value.length >= 100) return   // 与数据库 varchar(100) 对齐
    remark.value += digit
  }
}

/** 退格：删掉当前聚焦输入框的最后一位 */
function backspace() {
  if (focusedField.value === 'peopleCount') {
    peopleCount.value = peopleCount.value.slice(0, -1)
  } else {
    remark.value = remark.value.slice(0, -1)
  }
}

/** 清空当前聚焦的输入框 */
function clearFocused() {
  if (focusedField.value === 'peopleCount') {
    peopleCount.value = ''
  } else {
    remark.value = ''
  }
}

/** 点开台：把输入交给父组件。人数由父组件做范围校验 */
function handleConfirm() {
  emit('confirm', {
    peopleCount: Number(peopleCount.value) || 0,
    remark: remark.value.trim(),
  })
}

/** 关闭弹窗（右上角 ✕ 或点遮罩触发） */
function handleClose() {
  emit('update:visible', false)
}
</script>

<template>
  <el-dialog
    :model-value="props.visible"
    width="560px"
    :close-on-click-modal="false"
    @update:model-value="handleClose"
  >
    <template #header>
      <span class="dialog-title">开台</span>
      <span class="dialog-table">当前桌号：{{ props.table?.name }}</span>
    </template>

    <div class="dialog-body">
      <!-- 左半：开台信息 -->
      <div class="info-area">
        <div class="field">
          <span class="label">操作人：</span>
          <span class="operator">{{ props.operatorName }}</span>
        </div>

        <div class="field">
          <span class="label">人数：</span>
          <el-input
            v-model="peopleCount"
            class="value-input"
            placeholder="请输入用餐人数"
            @focus="focusedField = 'peopleCount'"
          />
        </div>

        <div class="field">
          <span class="label">桌台备注：</span>
          <el-input
            v-model="remark"
            class="value-input"
            placeholder="选填"
            @focus="focusedField = 'remark'"
          />
        </div>
      </div>

      <!-- 右半：3×4 数字键盘，每个键大小一致 -->
      <div class="keypad">
        <button v-for="n in 9" :key="n" class="key" @click="appendDigit(String(n))">
          {{ n }}
        </button>
        <button class="key" @click="clearFocused">清空</button>
        <button class="key" @click="appendDigit('0')">0</button>
        <!-- ⌫ 是退格符号，用 Unicode 字符而非图标字体——图标库里没有这个图标 -->
        <button class="key" @click="backspace">⌫</button>
      </div>
    </div>

    <template #footer>
      <el-button class="confirm-btn" @click="handleConfirm">开台</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.dialog-title {
  font-size: 20px;
  font-weight: 500;
  margin-right: 16px;
}

.dialog-table {
  color: #909399;
  font-size: 15px;
}

.dialog-body {
  display: flex;
  gap: 24px;
}

/* ===== 左半：信息区 ===== */
.info-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 20px;
  padding-top: 4px;
}

.field {
  display: flex;
  align-items: center;
}

.label {
  width: 84px;
  flex-shrink: 0;
  color: #606266;
  font-size: 15px;
}

.operator {
  font-size: 15px;
  color: #303133;
}

.value-input {
  flex: 1;
}

/* ===== 右半：数字键盘 ===== */
.keypad {
  display: grid;
  grid-template-columns: repeat(3, 1fr);   /* 三列等宽 */
  grid-template-rows: repeat(4, 1fr);   /* 四行等高 */
  gap: 8px;
  width: 240px;
  height: 240px;   /* 与宽度相等 → 接近正方形 */
  flex-shrink: 0;
}

.key {
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px solid #dcdfe6;
  border-radius: 6px;
  background: #fff;
  color: #303133;
  font-size: 20px;
  cursor: pointer;
}

.key:active {
  background: #ecf5ff;   /* 按下时变色，触摸屏上有反馈 */
}

/* ===== 底部按钮 ===== */
.confirm-btn {
  width: 100%;
  height: 48px;
  font-size: 17px;
  background: #ffb800;
  border-color: #ffb800;
  color: #fff;
}

.confirm-btn:hover {
  background: #ffc933;
  border-color: #ffc933;
  color: #fff;
}
</style>
