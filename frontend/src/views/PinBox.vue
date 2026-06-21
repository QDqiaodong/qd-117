<template>
  <div class="page-container">
    <h2 class="page-title">顶针盒号管理</h2>

    <div class="toolbar">
      <div class="toolbar-left">
        <el-input
          v-model="searchForm.partModel"
          placeholder="零件型号"
          clearable
          style="width: 180px;"
        />
        <el-input
          v-model="searchForm.boxNo"
          placeholder="盒号"
          clearable
          style="width: 160px;"
        />
        <el-select
          v-model="searchForm.status"
          placeholder="状态"
          clearable
          style="width: 140px;"
        >
          <el-option label="在库" value="IN_STOCK" />
          <el-option label="已出库" value="OUT_OF_STOCK" />
          <el-option label="已报废" value="SCRAPPED" />
        </el-select>
        <el-select
          v-model="searchForm.productionLine"
          placeholder="领用产线"
          clearable
          style="width: 160px;"
        >
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
        <el-button type="primary" @click="loadData">
          <el-icon><Search /></el-icon> 查询
        </el-button>
        <el-button @click="resetSearch">
          <el-icon><Refresh /></el-icon> 重置
        </el-button>
      </div>
    </div>

    <el-table :data="tableData" stripe border v-loading="loading" style="width: 100%;">
      <el-table-column prop="boxNo" label="盒号" width="140" align="center" />
      <el-table-column prop="partModel" label="零件型号" min-width="140" />
      <el-table-column prop="status" label="状态" width="100" align="center">
        <template #default="{ row }">
          <el-tag v-if="row.status === 'IN_STOCK'" type="success" size="small">在库</el-tag>
          <el-tag v-else-if="row.status === 'OUT_OF_STOCK'" type="warning" size="small">已出库</el-tag>
          <el-tag v-else type="danger" size="small">已报废</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="shelfNo" label="货架编号" width="110" align="center" />
      <el-table-column prop="productionLine" label="领用产线" width="120" align="center">
        <template #default="{ row }">
          {{ row.productionLine || '-' }}
        </template>
      </el-table-column>
      <el-table-column prop="remark" label="备注" min-width="150" show-overflow-tooltip />
      <el-table-column prop="createTime" label="入库时间" width="170" align="center" />
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
import { getPinBoxPage } from '@/api'

const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(20)

const searchForm = reactive({
  partModel: '',
  boxNo: '',
  status: '',
  productionLine: '',
  dateRange: []
})

const resetSearch = () => {
  searchForm.partModel = ''
  searchForm.boxNo = ''
  searchForm.status = ''
  searchForm.productionLine = ''
  searchForm.dateRange = []
  pageNum.value = 1
  loadData()
}

const loadData = async () => {
  loading.value = true
  try {
    const res = await getPinBoxPage({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      partModel: searchForm.partModel || undefined,
      boxNo: searchForm.boxNo || undefined,
      status: searchForm.status || undefined,
      productionLine: searchForm.productionLine || undefined,
      startTime: searchForm.dateRange?.[0] ? `${searchForm.dateRange[0]} 00:00:00` : undefined,
      endTime: searchForm.dateRange?.[1] ? `${searchForm.dateRange[1]} 23:59:59` : undefined
    })
    tableData.value = res.data.records
    total.value = res.data.total
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

onMounted(loadData)
</script>

<style lang="scss" scoped>
</style>
