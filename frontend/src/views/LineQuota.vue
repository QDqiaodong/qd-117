<template>
  <div class="page-container">
    <h2 class="page-title">产线领用配额</h2>

    <el-alert type="info" show-icon :closable="false" style="margin-bottom: 16px;">
      按季度为各装配线（一号至四号）的小件类型（顶针 / 限位垫片）配置可领用配额上限，领用出库时自动校验并扣减；超额时返回配额剩余与超出数量。
      <span style="margin-left: 8px; color: #409EFF;">当前季度：{{ currentQuarter }}</span>
    </el-alert>

    <div class="toolbar">
      <div class="toolbar-left">
        <el-input v-model="searchForm.quarter" placeholder="季度 如 2026-Q2" clearable style="width: 160px;" />
        <el-select v-model="searchForm.productionLine" placeholder="领用产线" clearable style="width: 160px;">
          <el-option v-for="line in productionLines" :key="line" :label="line" :value="line" />
        </el-select>
        <el-select v-model="searchForm.partType" placeholder="小件类型" clearable style="width: 140px;">
          <el-option v-for="t in partTypes" :key="t" :label="t" :value="t" />
        </el-select>
        <el-button type="primary" @click="loadData">
          <el-icon><Search /></el-icon> 查询
        </el-button>
        <el-button @click="resetSearch">
          <el-icon><Refresh /></el-icon> 重置
        </el-button>
      </div>
      <div>
        <el-button type="success" @click="openCreate">
          <el-icon><Plus /></el-icon> 新增配额
        </el-button>
      </div>
    </div>

    <el-table :data="tableData" stripe border v-loading="loading" style="width: 100%;">
      <el-table-column prop="quarter" label="季度" width="120" align="center" />
      <el-table-column prop="productionLine" label="领用产线" width="130" align="center" />
      <el-table-column prop="partType" label="小件类型" width="120" align="center">
        <template #default="{ row }">
          <el-tag :type="row.partType === '顶针' ? 'primary' : 'success'" size="small">{{ row.partType }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="maxQuantity" label="配额上限" width="100" align="center" />
      <el-table-column prop="usedQuantity" label="已领用" width="100" align="center" />
      <el-table-column label="配额剩余" width="120" align="center">
        <template #default="{ row }">
          <el-tag :type="row.remainingQuantity <= 0 ? 'danger' : (row.remainingQuantity <= row.maxQuantity * 0.2 ? 'warning' : 'success')">
            {{ row.remainingQuantity }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="使用进度" min-width="180">
        <template #default="{ row }">
          <el-progress
            :percentage="usagePercent(row)"
            :color="progressColor(row)"
            :stroke-width="14"
            :text-inside="true"
          />
        </template>
      </el-table-column>
      <el-table-column prop="remark" label="备注" min-width="140" show-overflow-tooltip />
      <el-table-column prop="updateTime" label="更新时间" width="170" align="center" />
      <el-table-column label="操作" width="220" align="center" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="openEdit(row)">编辑</el-button>
          <el-button size="small" type="warning" plain @click="recalculate(row)">重算</el-button>
          <el-button size="small" type="danger" plain @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div style="margin-top: 20px; text-align: right;">
      <el-pagination
        v-model:current-page="pageNum"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="loadData"
        @current-change="loadData"
      />
    </div>

    <el-dialog v-model="dialogVisible" :title="editMode ? '编辑配额' : '新增配额'" width="520px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="季度">
          <el-input v-model="form.quarter" placeholder="如 2026-Q2">
            <template #append>
              <el-button @click="form.quarter = currentQuarter">本季度</el-button>
            </template>
          </el-input>
        </el-form-item>
        <el-form-item label="领用产线">
          <el-select v-model="form.productionLine" placeholder="请选择产线" style="width: 100%;">
            <el-option v-for="line in productionLines" :key="line" :label="line" :value="line" />
          </el-select>
        </el-form-item>
        <el-form-item label="小件类型">
          <el-select v-model="form.partType" placeholder="请选择类型" style="width: 100%;">
            <el-option v-for="t in partTypes" :key="t" :label="t" :value="t" />
          </el-select>
        </el-form-item>
        <el-form-item label="配额上限">
          <el-input-number v-model="form.maxQuantity" :min="0" :step="100" style="width: 100%;" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="可选" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getLineQuotaPage,
  createLineQuota,
  updateLineQuota,
  deleteLineQuota,
  recalculateLineQuota,
  getCurrentQuarter,
  getLineQuotaEnums
} from '@/api'

const loading = ref(false)
const submitting = ref(false)
const tableData = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const currentQuarter = ref('')
const productionLines = ref([])
const partTypes = ref([])

const searchForm = reactive({
  quarter: '',
  productionLine: '',
  partType: ''
})

const dialogVisible = ref(false)
const editMode = ref(false)
const form = reactive({
  id: null,
  quarter: '',
  productionLine: '',
  partType: '',
  maxQuantity: 0,
  remark: ''
})

const usagePercent = (row) => {
  if (!row.maxQuantity || row.maxQuantity <= 0) return 0
  const p = Math.round(((row.usedQuantity || 0) / row.maxQuantity) * 100)
  return Math.min(100, Math.max(0, p))
}

const progressColor = (row) => {
  const p = usagePercent(row)
  if (p >= 100) return '#f56c6c'
  if (p >= 80) return '#e6a23c'
  return '#67c23a'
}

const loadData = async () => {
  loading.value = true
  try {
    const res = await getLineQuotaPage({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      quarter: searchForm.quarter || undefined,
      productionLine: searchForm.productionLine || undefined,
      partType: searchForm.partType || undefined
    })
    tableData.value = res.data.records
    total.value = res.data.total
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

const resetSearch = () => {
  searchForm.quarter = ''
  searchForm.productionLine = ''
  searchForm.partType = ''
  pageNum.value = 1
  loadData()
}

const openCreate = () => {
  editMode.value = false
  form.id = null
  form.quarter = currentQuarter.value
  form.productionLine = productionLines.value[0] || ''
  form.partType = partTypes.value[0] || ''
  form.maxQuantity = 0
  form.remark = ''
  dialogVisible.value = true
}

const openEdit = (row) => {
  editMode.value = true
  form.id = row.id
  form.quarter = row.quarter
  form.productionLine = row.productionLine
  form.partType = row.partType
  form.maxQuantity = row.maxQuantity
  form.remark = row.remark
  dialogVisible.value = true
}

const submit = async () => {
  if (!form.quarter || !form.productionLine || !form.partType) {
    ElMessage.warning('请完整填写季度、产线与类型')
    return
  }
  if (form.maxQuantity == null || form.maxQuantity < 0) {
    ElMessage.warning('配额上限不能为空')
    return
  }
  submitting.value = true
  try {
    const payload = {
      quarter: form.quarter,
      productionLine: form.productionLine,
      partType: form.partType,
      maxQuantity: form.maxQuantity,
      remark: form.remark
    }
    if (editMode.value) {
      payload.id = form.id
      await updateLineQuota(payload)
      ElMessage.success('更新成功')
    } else {
      await createLineQuota(payload)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    loadData()
  } catch (e) {
    console.error(e)
  } finally {
    submitting.value = false
  }
}

const recalculate = async (row) => {
  try {
    await ElMessageBox.confirm(`确认按出库记录重算已领用量？(${row.quarter} ${row.productionLine} ${row.partType})`, '确认')
    await recalculateLineQuota({
      quarter: row.quarter,
      productionLine: row.productionLine,
      partType: row.partType
    })
    ElMessage.success('重算完成')
    loadData()
  } catch (e) {
    console.error(e)
  }
}

const remove = async (row) => {
  try {
    await ElMessageBox.confirm(`确认删除该配额？(${row.quarter} ${row.productionLine} ${row.partType})`, '确认', { type: 'warning' })
    await deleteLineQuota(row.id)
    ElMessage.success('删除成功')
    loadData()
  } catch (e) {
    console.error(e)
  }
}

const loadEnums = async () => {
  try {
    const [enumRes, quarterRes] = await Promise.all([getLineQuotaEnums(), getCurrentQuarter()])
    productionLines.value = enumRes.data.productionLines || []
    partTypes.value = enumRes.data.partTypes || []
    currentQuarter.value = quarterRes.data
  } catch (e) {
    console.error(e)
  }
}

onMounted(async () => {
  await loadEnums()
  loadData()
})
</script>
