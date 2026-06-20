<template>
  <div class="page-container">
    <h2 class="page-title">产线领用出库登记</h2>

    <el-form :model="form" label-width="100px" style="margin-bottom: 16px;">
      <el-row :gutter="16">
        <el-col :span="8">
          <el-form-item label="领用产线">
            <el-select v-model="form.productionLine" placeholder="请选择产线" style="width: 100%;">
              <el-option label="一号装配线" value="一号装配线" />
              <el-option label="二号装配线" value="二号装配线" />
              <el-option label="三号装配线" value="三号装配线" />
              <el-option label="四号装配线" value="四号装配线" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="操作人">
            <el-input v-model="form.operator" placeholder="请输入操作人姓名" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="领用人">
            <el-input v-model="form.receiver" placeholder="请输入领用人姓名" />
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="16">
        <el-col :span="24">
          <el-form-item label="备注">
            <el-input v-model="form.remark" placeholder="可选" />
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
        <el-icon><Check /></el-icon> 确认出库
      </el-button>
      <el-button size="large" @click="resetForm">
        <el-icon><RefreshLeft /></el-icon> 重置
      </el-button>
    </div>

    <el-divider />

    <h3 class="page-title" style="font-size: 16px; margin-top: 0;">出库记录</h3>

    <div class="toolbar">
      <div class="toolbar-left">
        <el-input v-model="searchForm.partModel" placeholder="零件型号" clearable style="width: 200px;" />
        <el-select v-model="searchForm.productionLine" placeholder="领用产线" clearable style="width: 160px;">
          <el-option label="一号装配线" value="一号装配线" />
          <el-option label="二号装配线" value="二号装配线" />
          <el-option label="三号装配线" value="三号装配线" />
          <el-option label="四号装配线" value="四号装配线" />
        </el-select>
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
      <el-table-column prop="quantity" label="领用数量" width="100" align="center" />
      <el-table-column prop="productionLine" label="领用产线" width="120" align="center" />
      <el-table-column prop="operator" label="操作人" width="100" align="center" />
      <el-table-column prop="receiver" label="领用人" width="100" align="center" />
      <el-table-column prop="remark" label="备注" min-width="150" show-overflow-tooltip />
      <el-table-column prop="createTime" label="出库时间" width="170" align="center" />
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

    <el-dialog v-model="errorDialogVisible" title="数据校验失败" width="560px" :close-on-click-modal="true" top="8vh">
      <el-alert type="error" show-icon :closable="false" style="margin-bottom: 16px;">
        共发现 {{ validationErrors.length }} 条字段级错误，请修正后再提交。
      </el-alert>
      <div class="validation-errors">
        <div v-for="(group, gIdx) in groupedValidationErrors" :key="gIdx" class="error-row-group">
          <div class="error-row-header" @click="jumpToErrorRow(group.rowIndex)">
            <el-icon color="#f56c6c"><Warning /></el-icon>
            <span class="row-label">第 {{ group.rowNumber }} 行</span>
            <span class="locate-hint"><el-icon><Aim /></el-icon> 点击定位</span>
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
        <el-button type="primary" @click="errorDialogVisible = false">我知道了，去修改</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Aim, Warning } from '@element-plus/icons-vue'
import BatchInputTable from '@/components/BatchInputTable.vue'
import { stockOut, getStockOutPage, getPartList } from '@/api'

const submitting = ref(false)
const loading = ref(false)
const recordData = ref([])
const recordTotal = ref(0)
const errorDialogVisible = ref(false)
const validationErrors = ref([])
const fieldLabelMap = {
  partId: '选择零件',
  quantity: '领用数量'
}
const partList = ref([])

const form = reactive({
  productionLine: '',
  operator: '',
  receiver: '',
  remark: '',
  items: []
})

const searchForm = reactive({
  partModel: '',
  productionLine: '',
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

const columns = reactive([
  {
    prop: 'partId',
    label: '选择零件',
    type: 'select',
    minWidth: 200,
    options: []
  },
  { prop: 'quantity', label: '领用数量', type: 'number', min: 0, width: 120 }
])

const validators = {
  partId: (value) => {
    if (!value) {
      return { valid: false, message: '请选择零件' }
    }
    return { valid: true }
  },
  quantity: (value, row) => {
    if (value === null || value === undefined || value <= 0) {
      return { valid: false, message: '数量必须大于0' }
    }
    const part = partList.value.find(p => p.id === row.partId)
    if (part && value > part.stockQuantity) {
      return { valid: false, message: `库存不足(当前:${part.stockQuantity})` }
    }
    return { valid: true }
  }
}

const loadPartList = async () => {
  try {
    const res = await getPartList()
    partList.value = res.data
    columns[0].options = res.data.map(p => ({
      label: `${p.partModel} - ${p.partName} (库存:${p.stockQuantity})`,
      value: p.id
    }))
  } catch (e) {
    console.error(e)
  }
}

const onItemsChange = (items) => {
  form.items = items
}

const submit = async () => {
  if (!form.productionLine) {
    ElMessage.warning('请选择领用产线')
    return
  }
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

  const validItems = form.items.filter(i => i.partId && i.quantity > 0)
  if (validItems.length === 0) {
    ElMessage.warning('请至少选择一个零件并填写领用数量')
    return
  }
  try {
    await ElMessageBox.confirm(`确认出库 ${validItems.length} 条记录？`, '确认')
    submitting.value = true
    await stockOut({
      productionLine: form.productionLine,
      operator: form.operator,
      receiver: form.receiver,
      remark: form.remark,
      items: validItems
    })
    ElMessage.success('出库成功')
    resetForm()
    loadRecords()
    loadPartList()
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
  form.productionLine = ''
  form.operator = ''
  form.receiver = ''
  form.remark = ''
  form.items = []
}

const loadRecords = async () => {
  loading.value = true
  try {
    const res = await getStockOutPage({
      pageNum: recordPage.pageNum,
      pageSize: recordPage.pageSize,
      partModel: searchForm.partModel || undefined,
      productionLine: searchForm.productionLine || undefined,
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

onMounted(() => {
  loadPartList()
  loadRecords()
})
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
