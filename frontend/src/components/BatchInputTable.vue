<template>
  <div class="batch-input">
    <div class="batch-toolbar">
      <el-button type="primary" size="small" @click="addRow">
        <el-icon><Plus /></el-icon> 新增一行
      </el-button>
      <el-button type="danger" size="small" @click="clearAll" v-if="showClear">
        <el-icon><Delete /></el-icon> 清空全部
      </el-button>
    </div>

    <el-table
      :data="tableData"
      border
      style="width: 100%; margin-top: 12px;"
      :row-class-name="rowClassName"
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
          <template v-if="col.type === 'select'">
            <el-select
              v-model="row[col.prop]"
              placeholder="请选择"
              size="small"
              style="width: 100%;"
              @change="handleCellChange($index, col.prop)"
            >
              <el-option
                v-for="opt in col.options"
                :key="opt.value"
                :label="opt.label"
                :value="opt.value"
              />
            </el-select>
          </template>
          <template v-else-if="col.type === 'number'">
            <el-input-number
              v-model="row[col.prop]"
              :min="col.min || 1"
              size="small"
              style="width: 100%;"
              @change="handleCellChange($index, col.prop)"
              controls-position="right"
            />
          </template>
          <template v-else>
            <el-input
              v-model="row[col.prop]"
              :placeholder="col.placeholder || '请输入'"
              size="small"
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
import { ref, watch } from 'vue'

const props = defineProps({
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
  }
})

const emit = defineEmits(['update:modelValue', 'change'])

const createEmptyRow = () => {
  const row = {}
  props.columns.forEach(col => {
    row[col.prop] = col.defaultValue !== undefined ? col.defaultValue : ''
  })
  return row
}

const tableData = ref(props.initialData.length > 0 ? [...props.initialData] : [createEmptyRow()])

const addRow = () => {
  tableData.value.push(createEmptyRow())
  emitChange()
}

const removeRow = (index) => {
  if (tableData.value.length <= 1) {
    tableData.value = [createEmptyRow()]
  } else {
    tableData.value.splice(index, 1)
  }
  emitChange()
}

const clearAll = () => {
  tableData.value = [createEmptyRow()]
  emitChange()
}

const handleCellChange = (index, prop) => {
  emitChange()
}

const emitChange = () => {
  emit('update:modelValue', tableData.value)
  emit('change', tableData.value)
}

watch(tableData, (val) => {
  emit('update:modelValue', val)
}, { deep: true })

const rowClassName = ({ rowIndex }) => {
  return rowIndex % 2 === 0 ? '' : 'row-alt'
}
</script>

<style lang="scss" scoped>
.batch-input {
  .batch-toolbar {
    display: flex;
    gap: 12px;
  }
}

:deep(.row-alt) {
  background-color: #fafafa;
}
</style>
