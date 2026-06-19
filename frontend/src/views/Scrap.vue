<template>
  <div class="page-container">
    <h2 class="page-title">破损零件报废登记</h2>

    <el-form :model="form" label-width="100px" style="margin-bottom: 16px;">
      <el-row :gutter="16">
        <el-col :span="8">
          <el-form-item label="操作人">
            <el-input v-model="form.operator" placeholder="请输入操作人姓名" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
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
      <el-button type="danger" size="large" @click="submit" :loading="submitting">
        <el-icon><Delete /></el-icon> 确认报废
      </el-button>
      <el-button size="large" @click="resetForm">
        <el-icon><RefreshLeft /></el-icon> 重置
      </el-button>
    </div>

    <el-divider />

    <h3 class="page-title" style="font-size: 16px; margin-top: 0;">报废历史记录</h3>

    <div class="toolbar">
      <div class="toolbar-left">
        <el-input v-model="searchForm.partModel" placeholder="零件型号" clearable style="width: 200px;" />
        <el-select v-model="searchForm.scrapReason" placeholder="报废原因" clearable style="width: 160px;">
          <el-option label="变形" value="变形" />
          <el-option label="断裂" value="断裂" />
          <el-option label="磨损" value="磨损" />
          <el-option label="其他" value="其他" />
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
      <el-table-column prop="quantity" label="报废数量" width="100" align="center" />
      <el-table-column prop="scrapReason" label="报废原因" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="row.scrapReason === '断裂' ? 'danger' : (row.scrapReason === '变形' ? 'warning' : 'info')">
            {{ row.scrapReason }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="operator" label="操作人" width="100" align="center" />
      <el-table-column prop="remark" label="备注" min-width="150" show-overflow-tooltip />
      <el-table-column prop="createTime" label="报废时间" width="170" align="center" />
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
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import BatchInputTable from '@/components/BatchInputTable.vue'
import { scrap, getScrapPage, getPartList } from '@/api'

const submitting = ref(false)
const loading = ref(false)
const recordData = ref([])
const recordTotal = ref(0)
const partList = ref([])

const form = reactive({
  operator: '',
  remark: '',
  items: []
})

const searchForm = reactive({
  partModel: '',
  scrapReason: '',
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
  { prop: 'quantity', label: '报废数量', type: 'number', min: 0, width: 120 },
  {
    prop: 'scrapReason',
    label: '报废原因',
    type: 'select',
    width: 120,
    options: [
      { label: '变形', value: '变形' },
      { label: '断裂', value: '断裂' },
      { label: '磨损', value: '磨损' },
      { label: '其他', value: '其他' }
    ]
  }
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
  },
  scrapReason: (value) => {
    if (!value) {
      return { valid: false, message: '请选择报废原因' }
    }
    return { valid: true }
  }
}

const loadPartList = async () => {
  try {
    const res = await getPartList()
    partList.value = res.data
    columns[0].options = res.data.filter(p => p.stockQuantity > 0).map(p => ({
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
  if (!form.operator.trim()) {
    ElMessage.warning('请输入操作人')
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

  const validItems = form.items.filter(i => i.partId && i.quantity > 0 && i.scrapReason)
  if (validItems.length === 0) {
    ElMessage.warning('请至少填写一行有效的报废记录')
    return
  }
  try {
    await ElMessageBox.confirm(`确认报废 ${validItems.length} 条记录？此操作将扣减库存。`, '确认', {
      type: 'warning'
    })
    submitting.value = true
    await scrap({
      operator: form.operator,
      remark: form.remark,
      items: validItems
    })
    ElMessage.success('报废登记成功')
    resetForm()
    loadRecords()
    loadPartList()
  } catch (e) {
    console.error(e)
  } finally {
    submitting.value = false
  }
}

const resetForm = () => {
  form.operator = ''
  form.remark = ''
  form.items = []
}

const loadRecords = async () => {
  loading.value = true
  try {
    const res = await getScrapPage({
      pageNum: recordPage.pageNum,
      pageSize: recordPage.pageSize,
      partModel: searchForm.partModel || undefined,
      scrapReason: searchForm.scrapReason || undefined,
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
