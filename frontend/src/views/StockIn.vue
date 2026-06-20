<template>
  <div class="page-container">
    <h2 class="page-title">小件入库建档</h2>

    <el-form :model="form" label-width="100px" style="margin-bottom: 16px;">
      <el-row :gutter="16">
        <el-col :span="8">
          <el-form-item label="操作人">
            <el-input v-model="form.operator" placeholder="请输入操作人姓名" />
          </el-form-item>
        </el-col>
      </el-row>
    </el-form>

    <BatchInputTable
      ref="batchTableRef"
      v-model="form.items"
      :columns="columns"
      :initial-data="form.items"
      :validators="validators"
      @change="onItemsChange"
    />

    <div style="margin-top: 20px; text-align: center;">
      <el-button type="primary" size="large" @click="submit" :loading="submitting">
        <el-icon><Check /></el-icon> 确认入库
      </el-button>
      <el-button size="large" @click="resetForm">
        <el-icon><RefreshLeft /></el-icon> 重置
      </el-button>
    </div>

    <el-divider />

    <h3 class="page-title" style="font-size: 16px; margin-top: 0;">入库记录</h3>

    <div class="toolbar">
      <div class="toolbar-left">
        <el-input
          v-model="searchForm.partModel"
          placeholder="零件型号"
          clearable
          style="width: 200px;"
        />
        <el-date-picker
          v-model="searchForm.dateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          value-format="YYYY-MM-DD"
          style="width: 300px;"
        />
        <el-button type="primary" @click="loadRecords">
          <el-icon><Search /></el-icon> 查询
        </el-button>
      </div>
    </div>

    <el-table :data="recordData" stripe border v-loading="loading" style="width: 100%;">
      <el-table-column prop="partModel" label="零件型号" min-width="140" />
      <el-table-column prop="quantity" label="入库数量" width="100" align="center" />
      <el-table-column prop="shelfNo" label="货架编号" width="120" align="center" />
      <el-table-column prop="operator" label="操作人" width="100" align="center" />
      <el-table-column prop="remark" label="备注" min-width="150" show-overflow-tooltip />
      <el-table-column prop="createTime" label="入库时间" width="170" align="center" />
    </el-table>

    <div style="margin-top: 20px; text-align: right;">
      <el-pagination
        v-model:current-page="recordPage.pageNum"
        v-model:page-size="recordPage.pageSize"
        :total="recordTotal"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="loadRecords"
        @current-change="loadRecords"
      />
    </div>

    <el-dialog
      v-model="errorDialogVisible"
      title="数据校验失败"
      width="560px"
      :close-on-click-modal="true"
      top="8vh"
    >
      <el-alert
        type="error"
        show-icon
        :closable="false"
        style="margin-bottom: 16px;"
      >
        共发现 {{ validationErrors.length }} 条字段级错误，请修正后再提交。
      </el-alert>
      <div class="validation-errors">
        <div
          v-for="(group, gIdx) in groupedValidationErrors"
          :key="gIdx"
          class="error-row-group"
        >
          <div class="error-row-header" @click="jumpToErrorRow(group.rowIndex)">
            <el-icon color="#f56c6c"><Warning /></el-icon>
            <span class="row-label">第 {{ group.rowNumber }} 行</span>
            <span class="locate-hint">
              <el-icon><Aim /></el-icon> 点击定位
            </span>
          </div>
          <ul class="error-item-list">
            <li v-for="(err, eIdx) in group.errors" :key="eIdx">
              <span class="error-field">{{ err.fieldLabel }}：</span>
              <span class="error-msg">{{ err.message }}</span>
            </li>
          </ul>
        </div>
      </div>
      <template #footer>
        <el-button type="primary" @click="errorDialogVisible = false">
          我知道了，去修改
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Aim, Warning } from '@element-plus/icons-vue'
import BatchInputTable from '@/components/BatchInputTable.vue'
import { stockIn, getStockInPage } from '@/api'

const submitting = ref(false)
const loading = ref(false)
const recordData = ref([])
const recordTotal = ref(0)
const errorDialogVisible = ref(false)
const validationErrors = ref([])

const fieldLabelMap = {
  partModel: '零件型号',
  partName: '零件名称',
  partType: '零件类型',
  specParams: '规格参数',
  shelfNo: '货架编号',
  quantity: '入库数量',
  unit: '单位',
  remark: '备注'
}

const form = reactive({
  operator: '',
  items: []
})

const searchForm = reactive({
  partModel: '',
  dateRange: []
})

const recordPage = reactive({
  pageNum: 1,
  pageSize: 10
})

const batchTableRef = ref(null)

const groupedValidationErrors = computed(() => {
  const map = new Map()
  validationErrors.value.forEach(err => {
    const key = err.rowIndex
    if (!map.has(key)) {
      map.set(key, {
        rowIndex: err.rowIndex,
        rowNumber: err.rowNumber,
        errors: []
      })
    }
    map.get(key).errors.push({
      prop: err.prop,
      fieldLabel: fieldLabelMap[err.prop] || err.prop,
      message: err.message
    })
  })
  return Array.from(map.values()).sort((a, b) => a.rowIndex - b.rowIndex)
})

const columns = [
  { prop: 'partModel', label: '零件型号', placeholder: '必填', minWidth: 140 },
  { prop: 'partName', label: '零件名称', placeholder: '可选', minWidth: 140 },
  {
    prop: 'partType',
    label: '零件类型',
    type: 'select',
    width: 120,
    options: [
      { label: '顶针', value: '顶针' },
      { label: '限位垫片', value: '限位垫片' }
    ]
  },
  { prop: 'specParams', label: '规格参数', placeholder: '如:直径5,长度50,材质SKD11', minWidth: 200 },
  { prop: 'shelfNo', label: '货架编号', placeholder: '如:A-01-03', minWidth: 120 },
  { prop: 'quantity', label: '入库数量', type: 'number', min: 0, width: 120 },
  { prop: 'unit', label: '单位', placeholder: '默认:件', width: 100, defaultValue: '件' },
  { prop: 'remark', label: '备注', placeholder: '可选', minWidth: 150 }
]

const validators = {
  partModel: (value) => {
    if (!value || value.trim() === '') {
      return { valid: false, message: '型号不能为空' }
    }
    return { valid: true }
  },
  partType: (value) => {
    if (!value) {
      return { valid: false, message: '请选择零件类型' }
    }
    return { valid: true }
  },
  shelfNo: (value) => {
    if (!value || value.trim() === '') {
      return { valid: false, message: '货架编号不能为空' }
    }
    return { valid: true }
  },
  quantity: (value) => {
    if (value === null || value === undefined || value <= 0) {
      return { valid: false, message: '数量必须大于0' }
    }
    return { valid: true }
  }
}

const onItemsChange = (items) => {
  form.items = items
}

const submit = async () => {
  if (form.operator.trim() === '') {
    ElMessage.warning('请输入操作人')
    return
  }

  const validation = batchTableRef.value.validate()
  if (!validation.valid) {
    validationErrors.value = validation.errors
    errorDialogVisible.value = true
    return
  }

  const validItems = form.items.filter(i => i.partModel && i.partType && i.shelfNo && i.quantity > 0)
  if (validItems.length === 0) {
    ElMessage.warning('请至少填写一行有效的入库数据')
    return
  }
  try {
    await ElMessageBox.confirm(`确认入库 ${validItems.length} 条记录？`, '确认')
    submitting.value = true
    await stockIn({
      operator: form.operator,
      items: validItems
    })
    ElMessage.success('入库成功')
    resetForm()
    loadRecords()
  } catch (e) {
    console.error(e)
  } finally {
    submitting.value = false
  }
}

const jumpToErrorRow = (rowIndex) => {
  if (batchTableRef.value && typeof batchTableRef.value.scrollToRow === 'function') {
    batchTableRef.value.scrollToRow(rowIndex)
  }
  errorDialogVisible.value = false
}

const resetForm = () => {
  form.operator = ''
  form.items = []
}

const loadRecords = async () => {
  loading.value = true
  try {
    const res = await getStockInPage({
      pageNum: recordPage.pageNum,
      pageSize: recordPage.pageSize,
      partModel: searchForm.partModel || undefined,
      startTime: searchForm.dateRange?.[0] ? `${searchForm.dateRange[0]} 00:00:00` : undefined,
      endTime: searchForm.dateRange?.[1] ? `${searchForm.dateRange[1]} 23:59:59` : undefined
    })
    recordData.value = res.data.records
    recordTotal.value = res.data.total
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

onMounted(loadRecords)
</script>

<style lang="scss" scoped>
.validation-errors {
  max-height: 50vh;
  overflow-y: auto;
  padding-right: 8px;
}

.error-row-group {
  border: 1px solid #fbc4c4;
  border-radius: 6px;
  margin-bottom: 12px;
  background: #fef0f0;
  overflow: hidden;

  &:last-child {
    margin-bottom: 0;
  }
}

.error-row-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 14px;
  background: #fde2e2;
  cursor: pointer;
  user-select: none;
  transition: background 0.2s;

  &:hover {
    background: #f9c7c7;
  }

  .row-label {
    font-weight: 600;
    color: #c45656;
    font-size: 14px;
  }

  .locate-hint {
    margin-left: auto;
    display: flex;
    align-items: center;
    gap: 4px;
    color: #909399;
    font-size: 12px;
  }
}

.error-item-list {
  list-style: none;
  margin: 0;
  padding: 10px 14px 10px 38px;

  li {
    padding: 4px 0;
    line-height: 1.5;
    font-size: 13px;
    color: #606266;
  }

  .error-field {
    color: #e6a23c;
    font-weight: 500;
  }

  .error-msg {
    color: #f56c6c;
  }
}
</style>
