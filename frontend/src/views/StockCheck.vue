<template>
  <div class="page-container">
    <h2 class="page-title">季度库存清点记录</h2>

    <el-form :model="form" label-width="100px" style="margin-bottom: 16px;">
      <el-row :gutter="16">
        <el-col :span="8">
          <el-form-item label="盘点季度">
            <el-input v-model="form.quarter" :placeholder="currentQuarter" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="盘点人">
            <el-input v-model="form.checkPerson" placeholder="请输入盘点人姓名" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label=" ">
            <el-button type="warning" @click="loadAllParts">
              <el-icon><List /></el-icon> 载入全部库存
            </el-button>
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
        <el-icon><Check /></el-icon> 保存盘点记录
      </el-button>
      <el-button size="large" @click="resetForm">
        <el-icon><RefreshLeft /></el-icon> 重置
      </el-button>
    </div>

    <el-divider />

    <h3 class="page-title" style="font-size: 16px; margin-top: 0;">盘点历史记录</h3>

    <div class="toolbar">
      <div class="toolbar-left">
        <el-input v-model="searchForm.partModel" placeholder="零件型号" clearable style="width: 200px;" />
        <el-input v-model="searchForm.quarter" placeholder="盘点季度如:2024-Q1" clearable style="width: 180px;" />
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
      <el-table-column prop="systemQuantity" label="系统库存" width="100" align="center" />
      <el-table-column prop="actualQuantity" label="实际库存" width="100" align="center" />
      <el-table-column prop="diffQuantity" label="差异" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="row.diffQuantity > 0 ? 'success' : (row.diffQuantity < 0 ? 'danger' : 'info')">
            {{ row.diffQuantity > 0 ? '+' : '' }}{{ row.diffQuantity }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="shelfNo" label="货架" width="100" align="center" />
      <el-table-column prop="checkPerson" label="盘点人" width="100" align="center" />
      <el-table-column prop="quarter" label="季度" width="100" align="center" />
      <el-table-column prop="remark" label="差异原因" min-width="150" show-overflow-tooltip />
      <el-table-column prop="createTime" label="盘点时间" width="170" align="center" />
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
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import BatchInputTable from '@/components/BatchInputTable.vue'
import { stockCheck, getStockCheckPage, getPartList } from '@/api'

const submitting = ref(false)
const loading = ref(false)
const recordData = ref([])
const recordTotal = ref(0)
const partList = ref([])

const currentQuarter = computed(() => {
  const now = new Date()
  const year = now.getFullYear()
  const q = Math.floor(now.getMonth() / 3) + 1
  return `${year}-Q${q}`
})

const form = reactive({
  quarter: '',
  checkPerson: '',
  items: []
})

const searchForm = reactive({
  partModel: '',
  quarter: '',
  dateRange: []
})

const recordPage = reactive({
  pageNum: 1,
  pageSize: 10
})

const batchTableRef = ref(null)

const columns = reactive([
  {
    prop: 'partId',
    label: '选择零件',
    type: 'select',
    minWidth: 260,
    options: []
  },
  { prop: 'actualQuantity', label: '实际库存', type: 'number', min: 0, width: 120 },
  { prop: 'remark', label: '差异原因备注', placeholder: '有差异时必填', minWidth: 200 }
])

const validators = {
  partId: (value) => {
    if (!value) {
      return { valid: false, message: '请选择零件' }
    }
    return { valid: true }
  },
  actualQuantity: (value) => {
    if (value === null || value === undefined || value < 0) {
      return { valid: false, message: '库存不能为负数' }
    }
    return { valid: true }
  },
  remark: (value, row) => {
    const part = partList.value.find(p => p.id === row.partId)
    const systemQty = part ? part.stockQuantity : 0
    const diff = row.actualQuantity - systemQty
    if (diff !== 0 && (!value || value.trim() === '')) {
      return { valid: false, message: '有差异时请填写原因' }
    }
    return { valid: true }
  }
}

const loadAllParts = async () => {
  try {
    const res = await getPartList()
    form.items = res.data.filter(p => p.stockQuantity > 0).map(p => ({
      partId: p.id,
      actualQuantity: p.stockQuantity,
      remark: ''
    }))
    ElMessage.success(`已载入 ${form.items.length} 项库存`)
  } catch (e) {
    console.error(e)
  }
}

const loadPartList = async () => {
  try {
    const res = await getPartList()
    partList.value = res.data
    columns[0].options = res.data.map(p => ({
      label: `${p.partModel} - ${p.partName} (系统库存:${p.stockQuantity})`,
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
  if (!form.quarter.trim()) {
    ElMessage.warning('请输入盘点季度，如：2024-Q1')
    return
  }
  if (!form.checkPerson.trim()) {
    ElMessage.warning('请输入盘点人姓名')
    return
  }

  const validation = batchTableRef.value.validate()
  if (!validation.valid) {
    const errorMsg = validation.errors.map(e => `第${e.rowNumber}行: ${e.message}`).join('\n')
    ElMessageBox.alert(errorMsg, '数据校验失败', {
      type: 'error',
      dangerouslyUseHTMLString: false
    })
    return
  }

  const validItems = form.items.filter(i => i.partId && i.actualQuantity >= 0)
  if (validItems.length === 0) {
    ElMessage.warning('请至少选择一个零件进行盘点')
    return
  }
  try {
    await ElMessageBox.confirm(`确认保存 ${validItems.length} 条盘点记录？`, '确认')
    submitting.value = true
    await stockCheck({
      quarter: form.quarter,
      checkPerson: form.checkPerson,
      items: validItems
    })
    ElMessage.success('盘点记录保存成功')
    resetForm()
    loadRecords()
  } catch (e) {
    console.error(e)
  } finally {
    submitting.value = false
  }
}

const resetForm = () => {
  form.quarter = ''
  form.checkPerson = ''
  form.items = []
}

const loadRecords = async () => {
  loading.value = true
  try {
    const res = await getStockCheckPage({
      pageNum: recordPage.pageNum,
      pageSize: recordPage.pageSize,
      partModel: searchForm.partModel || undefined,
      quarter: searchForm.quarter || undefined,
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
