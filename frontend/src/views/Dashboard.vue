<template>
  <div class="page-container">
    <h2 class="page-title">库存总览</h2>

    <div class="stats-grid">
      <div class="stat-card">
        <div class="stat-label">小件种类总数</div>
        <div class="stat-value">{{ stats.totalTypes }}</div>
      </div>
      <div class="stat-card blue">
        <div class="stat-label">库存总数量</div>
        <div class="stat-value">{{ stats.totalStock }}</div>
      </div>
      <div class="stat-card green">
        <div class="stat-label">顶针种类</div>
        <div class="stat-value">{{ stats.ejectorTypes }}</div>
      </div>
      <div class="stat-card orange">
        <div class="stat-label">限位垫片种类</div>
        <div class="stat-value">{{ stats.spacerTypes }}</div>
      </div>
    </div>

    <div class="toolbar">
      <div class="toolbar-left">
        <el-input
          v-model="keyword"
          placeholder="搜索型号/名称"
          clearable
          style="width: 220px;"
          @change="loadData"
        />
        <el-select
          v-model="partTypeFilter"
          placeholder="零件类型"
          clearable
          style="width: 160px;"
          @change="loadData"
        >
          <el-option label="顶针" value="顶针" />
          <el-option label="限位垫片" value="限位垫片" />
        </el-select>
        <el-button type="primary" @click="loadData">
          <el-icon><Search /></el-icon> 查询
        </el-button>
        <el-button @click="resetSearch">
          <el-icon><Refresh /></el-icon> 重置
        </el-button>
      </div>
      <div>
        <el-button type="success" @click="refreshCache">
          <el-icon><RefreshRight /></el-icon> 刷新缓存
        </el-button>
      </div>
    </div>

    <el-table
      :data="tableData"
      stripe
      border
      v-loading="loading"
      style="width: 100%;"
    >
      <el-table-column prop="partModel" label="零件型号" min-width="140" />
      <el-table-column prop="partName" label="零件名称" min-width="140" />
      <el-table-column prop="partType" label="零件类型" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="row.partType === '顶针' ? 'primary' : 'success'" size="small">
            {{ row.partType }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="specParams" label="规格参数" min-width="180" show-overflow-tooltip />
      <el-table-column prop="shelfNo" label="货架编号" width="120" align="center" />
      <el-table-column prop="stockQuantity" label="库存数量" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="row.stockQuantity <= 10 ? 'danger' : (row.stockQuantity <= 50 ? 'warning' : 'success')">
            {{ row.stockQuantity }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="unit" label="单位" width="80" align="center" />
      <el-table-column prop="remark" label="备注" min-width="150" show-overflow-tooltip />
      <el-table-column prop="updateTime" label="更新时间" width="170" align="center" />
    </el-table>

    <div style="margin-top: 20px; text-align: right;">
      <el-pagination
        v-model:current-page="pageNum"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="loadData"
        @current-change="loadData"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getPartPage, getPartList, refreshPartCache } from '@/api'

const loading = ref(false)
const tableData = ref([])
const keyword = ref('')
const partTypeFilter = ref('')
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)

const stats = reactive({
  totalTypes: 0,
  totalStock: 0,
  ejectorTypes: 0,
  spacerTypes: 0
})

const loadData = async () => {
  loading.value = true
  try {
    const res = await getPartPage({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      keyword: keyword.value || undefined,
      partType: partTypeFilter.value || undefined
    })
    tableData.value = res.data.records
    total.value = res.data.total
    await loadStats()
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

const loadStats = async () => {
  try {
    const res = await getPartList()
    const list = res.data
    stats.totalTypes = list.length
    stats.totalStock = list.reduce((sum, item) => sum + (item.stockQuantity || 0), 0)
    stats.ejectorTypes = list.filter(i => i.partType === '顶针').length
    stats.spacerTypes = list.filter(i => i.partType === '限位垫片').length
  } catch (e) {
    console.error(e)
  }
}

const resetSearch = () => {
  keyword.value = ''
  partTypeFilter.value = ''
  pageNum.value = 1
  loadData()
}

const refreshCache = async () => {
  try {
    await refreshPartCache()
    ElMessage.success('缓存刷新成功')
    loadData()
  } catch (e) {
    console.error(e)
  }
}

onMounted(loadData)
</script>
