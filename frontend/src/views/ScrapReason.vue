<template>
  <div class="page-container">
    <h2 class="page-title">破损原因字典</h2>

    <el-alert type="info" show-icon :closable="false" style="margin-bottom: 16px;">
      维护破损报废原因字典，区分顶针弯曲、针尖磨损、垫片变形、孔径偏差等原因。启用状态的原因将出现在报废登记下拉选项中。
    </el-alert>

    <div class="toolbar">
      <div class="toolbar-left">
        <el-input v-model="searchForm.reasonName" placeholder="原因名称" clearable style="width: 180px;" />
        <el-select v-model="searchForm.partType" placeholder="适用零件类型" clearable style="width: 160px;">
          <el-option v-for="t in partTypes" :key="t" :label="t" :value="t" />
        </el-select>
        <el-select v-model="searchForm.status" placeholder="状态" clearable style="width: 140px;">
          <el-option label="启用" :value="1" />
          <el-option label="禁用" :value="0" />
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
          <el-icon><Plus /></el-icon> 新增原因
        </el-button>
      </div>
    </div>

    <el-table :data="tableData" stripe border v-loading="loading" style="width: 100%;">
      <el-table-column prop="sort" label="排序" width="80" align="center" />
      <el-table-column prop="reasonCode" label="原因编码" width="140" align="center" />
      <el-table-column prop="reasonName" label="原因名称" min-width="140" />
      <el-table-column prop="partType" label="适用零件类型" width="140" align="center">
        <template #default="{ row }">
          <el-tag :type="partTypeTag(row.partType)" size="small">{{ row.partType }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
            {{ row.status === 1 ? '启用' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="remark" label="备注" min-width="160" show-overflow-tooltip />
      <el-table-column prop="updateTime" label="更新时间" width="170" align="center" />
      <el-table-column label="操作" width="220" align="center" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="openEdit(row)">编辑</el-button>
          <el-button size="small" :type="row.status === 1 ? 'warning' : 'success'" plain @click="toggleStatus(row)">
            {{ row.status === 1 ? '禁用' : '启用' }}
          </el-button>
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

    <el-dialog v-model="dialogVisible" :title="editMode ? '编辑原因' : '新增原因'" width="520px">
      <el-form :model="form" label-width="110px">
        <el-form-item label="排序号">
          <el-input-number v-model="form.sort" :min="0" :step="1" style="width: 100%;" />
        </el-form-item>
        <el-form-item label="原因编码">
          <el-input v-model="form.reasonCode" placeholder="如 PIN_BEND、SHIM_DEFORM" :disabled="editMode" />
        </el-form-item>
        <el-form-item label="原因名称">
          <el-input v-model="form.reasonName" placeholder="如 顶针弯曲、垫片变形" />
        </el-form-item>
        <el-form-item label="适用零件类型">
          <el-select v-model="form.partType" placeholder="请选择类型" style="width: 100%;">
            <el-option v-for="t in partTypes" :key="t" :label="t" :value="t" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">禁用</el-radio>
          </el-radio-group>
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
  getScrapReasonPage,
  createScrapReason,
  updateScrapReason,
  deleteScrapReason,
  toggleScrapReasonStatus
} from '@/api'

const loading = ref(false)
const submitting = ref(false)
const tableData = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const partTypes = ['顶针', '限位垫片', '全部']

const searchForm = reactive({
  reasonName: '',
  partType: '',
  status: null
})

const dialogVisible = ref(false)
const editMode = ref(false)
const form = reactive({
  id: null,
  sort: 0,
  reasonCode: '',
  reasonName: '',
  partType: '',
  status: 1,
  remark: ''
})

const partTypeTag = (type) => {
  if (type === '顶针') return 'primary'
  if (type === '限位垫片') return 'success'
  return 'info'
}

const loadData = async () => {
  loading.value = true
  try {
    const res = await getScrapReasonPage({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      reasonName: searchForm.reasonName || undefined,
      partType: searchForm.partType || undefined,
      status: searchForm.status !== null ? searchForm.status : undefined
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
  searchForm.reasonName = ''
  searchForm.partType = ''
  searchForm.status = null
  pageNum.value = 1
  loadData()
}

const openCreate = () => {
  editMode.value = false
  form.id = null
  form.sort = 0
  form.reasonCode = ''
  form.reasonName = ''
  form.partType = '全部'
  form.status = 1
  form.remark = ''
  dialogVisible.value = true
}

const openEdit = (row) => {
  editMode.value = true
  form.id = row.id
  form.sort = row.sort
  form.reasonCode = row.reasonCode
  form.reasonName = row.reasonName
  form.partType = row.partType
  form.status = row.status
  form.remark = row.remark
  dialogVisible.value = true
}

const submit = async () => {
  if (!form.reasonCode.trim()) {
    ElMessage.warning('请输入原因编码')
    return
  }
  if (!form.reasonName.trim()) {
    ElMessage.warning('请输入原因名称')
    return
  }
  if (!form.partType) {
    ElMessage.warning('请选择适用零件类型')
    return
  }
  if (form.sort == null) {
    ElMessage.warning('请输入排序号')
    return
  }
  submitting.value = true
  try {
    const payload = {
      sort: form.sort,
      reasonCode: form.reasonCode,
      reasonName: form.reasonName,
      partType: form.partType,
      status: form.status,
      remark: form.remark
    }
    if (editMode.value) {
      payload.id = form.id
      await updateScrapReason(payload)
      ElMessage.success('更新成功')
    } else {
      await createScrapReason(payload)
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

const toggleStatus = async (row) => {
  const action = row.status === 1 ? '禁用' : '启用'
  try {
    await ElMessageBox.confirm(`确认${action}原因"${row.reasonName}"？`, '确认')
    await toggleScrapReasonStatus(row.id)
    ElMessage.success(`${action}成功`)
    loadData()
  } catch (e) {
    console.error(e)
  }
}

const remove = async (row) => {
  try {
    await ElMessageBox.confirm(`确认删除原因"${row.reasonName}"？`, '确认', { type: 'warning' })
    await deleteScrapReason(row.id)
    ElMessage.success('删除成功')
    loadData()
  } catch (e) {
    console.error(e)
  }
}

onMounted(() => {
  loadData()
})
</script>
