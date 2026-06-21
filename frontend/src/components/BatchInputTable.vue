<template>
  <div class="batch-input">
    <div class="batch-toolbar">
      <el-button type="primary" size="small" @click="addRow">
        <el-icon><Plus /></el-icon> 新增一行
      </el-button>
      <el-button type="danger" size="small" @click="clearAll" v-if="showClear">
        <el-icon><Delete /></el-icon> 清空全部
      </el-button>
      <slot name="toolbar" :unfilled-count="unfilledCount" :total-count="displayData.length"></slot>
      <div class="error-summary" v-if="errorRows.length > 0">
        <el-tag type="danger">
          <el-icon><Warning /></el-icon>
          错误行：{{ errorRows.map(r => r + 1).join('、') }}
        </el-tag>
      </div>
    </div>

    <el-table
      ref="tableRef"
      :data="displayData"
      border
      style="width: 100%; margin-top: 12px;"
      :row-class-name="rowClassName"
      :cell-class-name="cellClassName"
    >
      <el-table-column label="序号" type="index" width="60" align="center" />

      <el-table-column
        v-for="col in columns"
        :key="col.prop"
        :label="col.label"
        :width="col.width"
        :min-width="col.minWidth"
        align="center"
      >
        <template #default="{ row, $index }">
          <template v-if="col.type === 'readonly'">
            <span class="readonly-cell">{{ row[col.prop] !== undefined && row[col.prop] !== null ? row[col.prop] : '-' }}</span>
          </template>
          <template v-else-if="col.type === 'select'">
            <el-select
              v-model="row[col.prop]"
              placeholder="请选择"
              size="small"
              style="width: 100%;"
              :class="{ 'cell-error': hasError($index, col.prop) }"
              @change="handleCellChange($index, col.prop)"
            >
              <el-option
                v-for="opt in (col.optionsFn ? col.optionsFn(row, $index) : col.options)"
                :key="opt.value"
                :label="opt.label"
                :value="opt.value"
              />
            </el-select>
          </template>
          <template v-else-if="col.type === 'number'">
            <el-input-number
              v-model="row[col.prop]"
              :min="col.min !== undefined ? col.min : 0"
              size="small"
              style="width: 100%;"
              :class="{ 'cell-error': hasError($index, col.prop) }"
              @change="handleInputNumberChange($index, col.prop)"
              controls-position="right"
            />
          </template>
          <template v-else>
            <el-input
              v-model="row[col.prop]"
              :placeholder="col.placeholder || '请输入'"
              size="small"
              :class="{ 'cell-error': hasError($index, col.prop) }"
              @change="handleCellChange($index, col.prop)"
            />
          </template>
        </template>
      </el-table-column>

      <el-table-column label="操作" width="80" align="center" fixed="right">
        <template #default="{ $index }">
          <el-button
            type="danger"
            link
            size="small"
            @click="removeRow($index)"
          >
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup>
import { ref, watch, computed, reactive, nextTick } from 'vue'

const props = defineProps({
  modelValue: {
    type: Array,
    default: () => []
  },
  columns: {
    type: Array,
    required: true
  },
  initialData: {
    type: Array,
    default: () => []
  },
  showClear: {
    type: Boolean,
    default: true
  },
  validators: {
    type: Object,
    default: () => ({})
  },
  rowExtraClassFn: {
    type: Function,
    default: null
  },
  rowFilterFn: {
    type: Function,
    default: null
  },
  rowUnfilledFn: {
    type: Function,
    default: null
  }
})

const emit = defineEmits(['update:modelValue', 'change', 'validation-change'])

const rowErrors = reactive({})
const tableRef = ref(null)
const highlightRowIndex = ref(-1)
let highlightTimer = null

const createEmptyRow = () => {
  const row = { _touched: false }
  props.columns.forEach(col => {
    if (col.type === 'readonly') return
    row[col.prop] = col.defaultValue !== undefined ? col.defaultValue : ''
  })
  return row
}

const initTableData = () => {
  const source = props.modelValue && props.modelValue.length > 0
    ? props.modelValue
    : (props.initialData && props.initialData.length > 0 ? props.initialData : null)
  return source ? [...source] : [createEmptyRow()]
}

const tableData = ref(initTableData())

const addRow = () => {
  tableData.value.push(createEmptyRow())
  validateRow(tableData.value.length - 1)
  emitChange()
}

const removeRow = (index) => {
  if (tableData.value.length <= 1) {
    tableData.value = [createEmptyRow()]
    Object.keys(rowErrors).forEach(key => delete rowErrors[key])
  } else {
    tableData.value.splice(index, 1)
    Object.keys(rowErrors).forEach(key => delete rowErrors[key])
    tableData.value.forEach((_, i) => {
      props.columns.forEach(col => {
        const validator = props.validators[col.prop]
        if (validator) {
          const value = tableData.value[i][col.prop]
          const row = tableData.value[i]
          const result = validator(value, row, i)
          if (result && !result.valid) {
            if (!rowErrors[i]) rowErrors[i] = {}
            rowErrors[i][col.prop] = result.message
          }
        }
      })
    })
  }
  emitChange()
  emitValidationChange()
}

const scrollToRow = async (rowIndex) => {
  if (rowIndex < 0 || rowIndex >= tableData.value.length) return
  await nextTick()
  const table = tableRef.value
  if (table) {
    const rows = table.$el.querySelectorAll('.el-table__body-wrapper tbody tr')
    if (rows[rowIndex]) {
      rows[rowIndex].scrollIntoView({ behavior: 'smooth', block: 'center' })
    }
  }
  triggerRowHighlight(rowIndex)
}

const triggerRowHighlight = (rowIndex) => {
  if (highlightTimer) {
    clearTimeout(highlightTimer)
    highlightTimer = null
  }
  highlightRowIndex.value = rowIndex
  highlightTimer = setTimeout(() => {
    highlightRowIndex.value = -1
  }, 2500)
}

const clearAll = () => {
  tableData.value = [createEmptyRow()]
  Object.keys(rowErrors).forEach(key => delete rowErrors[key])
  emitChange()
  emitValidationChange()
}

const markRowTouched = (index) => {
  const row = tableData.value[index]
  if (row) {
    row._touched = true
  }
}

const handleCellChange = (index, prop) => {
  markRowTouched(index)
  validateCell(index, prop)
  emitChange()
}

const handleInputNumberChange = (index, prop) => {
  markRowTouched(index)
  validateCell(index, prop)
  emitChange()
}

const emitChange = () => {
  emit('update:modelValue', tableData.value)
  emit('change', tableData.value)
}

const emitValidationChange = () => {
  emit('validation-change', {
    valid: errorRows.value.length === 0,
    errorRows: errorRows.value,
    errors: getAllErrors()
  })
}

const validateCell = (rowIndex, prop) => {
  const validator = props.validators[prop]
  if (validator) {
    const value = tableData.value[rowIndex][prop]
    const row = tableData.value[rowIndex]
    const result = validator(value, row, rowIndex, tableData.value)
    if (result && !result.valid) {
      if (!rowErrors[rowIndex]) rowErrors[rowIndex] = {}
      rowErrors[rowIndex][prop] = result.message
    } else {
      if (rowErrors[rowIndex]) {
        delete rowErrors[rowIndex][prop]
        if (Object.keys(rowErrors[rowIndex]).length === 0) {
          delete rowErrors[rowIndex]
        }
      }
    }
  }
  emitValidationChange()
}

const validateRow = (rowIndex) => {
  props.columns.forEach(col => {
    validateCell(rowIndex, col.prop)
  })
}

const validateAll = () => {
  tableData.value.forEach((_, index) => validateRow(index))
  return {
    valid: errorRows.value.length === 0,
    errorRows: errorRows.value,
    errors: getAllErrors()
  }
}

const hasError = (rowIndex, prop) => {
  return rowErrors[rowIndex] && rowErrors[rowIndex][prop]
}

const errorRows = computed(() => {
  return Object.keys(rowErrors).map(Number).sort((a, b) => a - b)
})

const displayData = computed(() => {
  if (!props.rowFilterFn) {
    return tableData.value
  }
  return tableData.value.filter((row, index) => props.rowFilterFn(row, index))
})

const unfilledCount = computed(() => {
  if (!props.rowUnfilledFn) {
    return 0
  }
  return tableData.value.filter((row, index) => props.rowUnfilledFn(row, index)).length
})

const getAllErrors = () => {
  const errors = []
  Object.keys(rowErrors).forEach(rowIndex => {
    const row = rowErrors[rowIndex]
    Object.keys(row).forEach(prop => {
      errors.push({
        rowIndex: Number(rowIndex),
        rowNumber: Number(rowIndex) + 1,
        prop,
        message: row[prop]
      })
    })
  })
  return errors
}

const clearRowErrors = (rowIndex) => {
  delete rowErrors[rowIndex]
}

const cellClassName = ({ rowIndex, column }) => {
  const prop = column.property
  if (hasError(rowIndex, prop)) {
    return 'cell-error-cell'
  }
  return ''
}

const rowClassName = ({ row, rowIndex }) => {
  const base = rowIndex % 2 === 0 ? '' : 'row-alt'
  const classes = [base]
  if (rowErrors[rowIndex]) {
    classes.push('row-error')
  }
  if (highlightRowIndex.value === rowIndex) {
    classes.push('row-highlight')
  }
  if (props.rowUnfilledFn && props.rowUnfilledFn(row, rowIndex)) {
    classes.push('row-unfilled')
  }
  if (props.rowExtraClassFn) {
    const extra = props.rowExtraClassFn(row, rowIndex)
    if (extra) {
      classes.push(extra)
    }
  }
  return classes.join(' ').trim()
}

watch(tableData, (val) => {
  emit('update:modelValue', val)
}, { deep: true })

watch(() => props.modelValue, (newVal) => {
  if (newVal === tableData.value) return
  if (newVal && newVal.length > 0) {
    tableData.value = newVal.map(row => ({ ...row }))
  } else {
    tableData.value = [createEmptyRow()]
  }
  Object.keys(rowErrors).forEach(key => delete rowErrors[key])
  emitValidationChange()
})

defineExpose({
  validate: validateAll,
  validateRow,
  validateCell,
  getErrors: getAllErrors,
  errorRows,
  hasError,
  scrollToRow
})
</script>

<style lang="scss" scoped>
.batch-input {
  .batch-toolbar {
    display: flex;
    gap: 12px;
    align-items: center;
    flex-wrap: wrap;
  }

  .error-summary {
    margin-left: auto;
  }
}

:deep(.row-alt) {
  background-color: #fafafa;
}

:deep(.row-error) {
  background-color: #fef0f0 !important;
}

:deep(.row-unfilled) {
  background-color: #fff7e6 !important;

  &.row-alt {
    background-color: #fff1d6 !important;
  }

  td:first-child .cell::before {
    content: '';
    display: inline-block;
    width: 6px;
    height: 6px;
    border-radius: 50%;
    background-color: #e6a23c;
    margin-right: 6px;
    vertical-align: middle;
    animation: unfilledDot 1.2s ease-in-out infinite;
  }
}

@keyframes unfilledDot {
  0%, 100% {
    opacity: 1;
    transform: scale(1);
  }
  50% {
    opacity: 0.5;
    transform: scale(1.3);
  }
}

:deep(.row-highlight) {
  background-color: #fff7e6 !important;
  animation: rowHighlightPulse 1s ease-in-out infinite alternate;
}

@keyframes rowHighlightPulse {
  from {
    background-color: #fff7e6 !important;
  }
  to {
    background-color: #ffd666 !important;
  }
}

:deep(.cell-error-cell) {
  background-color: #fef0f0 !important;
}

:deep(.readonly-cell) {
  color: #606266;
  font-weight: 500;
  background: #f5f7fa;
  padding: 4px 8px;
  border-radius: 4px;
  display: inline-block;
  min-width: 50px;
}

:deep(.cell-error) {
  .el-input__wrapper,
  .el-textarea__inner,
  .el-select__wrapper,
  .el-input-number__decrease,
  .el-input-number__increase,
  .el-input-number .el-input__inner {
    border-color: #f56c6c !important;
    box-shadow: 0 0 0 1px #f56c6c inset !important;
  }

  &.is-focus {
    .el-input__wrapper,
    .el-select__wrapper {
      box-shadow: 0 0 0 2px rgba(245, 108, 108, 0.2) !important;
    }
  }
}
</style>
