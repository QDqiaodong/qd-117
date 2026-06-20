<template>
  <div class="page-container">
    <h2 class="page-title">流水记录查询</h2>

    <el-tabs v-model="activeTab" @tab-change="onTabChange">
      <el-tab-pane name="in">
        <template #label>
          <span class="tab-label">
            <span class="tab-ribbon ribbon-in"></span>
            <span class="tab-icon icon-in">+</span>
            入库记录
          </span>
        </template>
      </el-tab-pane>
      <el-tab-pane name="out">
        <template #label>
          <span class="tab-label">
            <span class="tab-ribbon ribbon-out"></span>
            <span class="tab-icon icon-out">−</span>
            出库记录
          </span>
        </template>
      </el-tab-pane>
      <el-tab-pane name="check">
        <template #label>
          <span class="tab-label">
            <span class="tab-ribbon ribbon-check"></span>
            <span class="tab-icon icon-check">≈</span>
            盘点记录
          </span>
        </template>
      </el-tab-pane>
      <el-tab-pane name="scrap">
        <template #label>
          <span class="tab-label">
            <span class="tab-ribbon ribbon-scrap"></span>
            <span class="tab-icon icon-scrap">−</span>
            报废记录
          </span>
        </template>
      </el-tab-pane>
    </el-tabs>

    <div class="toolbar">
      <div class="toolbar-left">
        <el-input
          v-model="searchForm.partModel"
          placeholder="零件型号"
          clearable
          style="width: 200px;"
        />
        <el-select
          v-if="activeTab === 'out'"
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
        <el-input
          v-if="activeTab === 'check'"
          v-model="searchForm.quarter"
          placeholder="盘点季度"
          clearable
          style="width: 160px;"
        />
        <el-select
          v-if="activeTab === 'check'"
          v-model="searchForm.confirmStatus"
          placeholder="确认状态"
          clearable
          style="width: 140px;"
        >
          <el-option label="未确认差异" :value="0" />
          <el-option label="已闭环" :value="1" />
        </el-select>
        <el-select
          v-if="activeTab === 'scrap'"
          v-model="searchForm.scrapReason"
          placeholder="报废原因"
          clearable
          style="width: 160px;"
        >
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
        <el-button type="primary" @click="loadData">
          <el-icon><Search /></el-icon> 查询
        </el-button>
        <el-button @click="resetSearch">
          <el-icon><Refresh /></el-icon> 重置
        </el-button>
      </div>
    </div>

    <template v-if="activeTab === 'in'">
      <el-table :data="tableData" stripe border v-loading="loading" style="width: 100%;">
        <el-table-column prop="partModel" label="零件型号" min-width="140" />
        <el-table-column prop="quantity" label="入库数量" width="120" align="center">
          <template #default="{ row }">
            <span class="qty-increase">+{{ row.quantity }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="shelfNo" label="货架编号" width="120" align="center" />
        <el-table-column prop="operator" label="操作人" width="100" align="center" />
        <el-table-column prop="remark" label="备注" min-width="150" show-overflow-tooltip />
        <el-table-column prop="createTime" label="入库时间" width="170" align="center" />
      </el-table>
    </template>

    <template v-if="activeTab === 'out'">
      <el-table :data="tableData" stripe border v-loading="loading" style="width: 100%;">
        <el-table-column prop="partModel" label="零件型号" min-width="140" />
        <el-table-column prop="quantity" label="领用数量" width="120" align="center">
          <template #default="{ row }">
            <span class="qty-decrease">−{{ row.quantity }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="productionLine" label="领用产线" width="120" align="center" />
        <el-table-column prop="operator" label="操作人" width="100" align="center" />
        <el-table-column prop="receiver" label="领用人" width="100" align="center" />
        <el-table-column prop="remark" label="备注" min-width="150" show-overflow-tooltip />
        <el-table-column prop="createTime" label="出库时间" width="170" align="center" />
      </el-table>
    </template>

    <template v-if="activeTab === 'check'">
      <el-table :data="tableData" stripe border v-loading="loading" style="width: 100%;">
        <el-table-column prop="partModel" label="零件型号" min-width="140" />
        <el-table-column prop="systemQuantity" label="快照账面" width="100" align="center" />
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
        <el-table-column label="确认状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.diffQuantity === 0" type="info" size="small">无差异</el-tag>
            <el-tag v-else-if="row.confirmStatus === 1" type="success" size="small">已闭环</el-tag>
            <el-tag v-else type="warning" size="small">待确认</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="差异原因" min-width="130" show-overflow-tooltip />
        <el-table-column prop="handleConclusion" label="处理结论" min-width="150" show-overflow-tooltip />
        <el-table-column prop="confirmPerson" label="确认人" width="90" align="center" />
        <el-table-column prop="createTime" label="盘点时间" width="170" align="center" />
        <el-table-column label="操作" width="110" align="center" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.diffQuantity !== 0 && row.confirmStatus !== 1"
              type="primary"
              size="small"
              link
              @click="openConfirmDialog(row)"
            >
              确认差异
            </el-button>
            <span v-else style="color: #c0c4cc;">-</span>
          </template>
        </el-table-column>
      </el-table>
    </template>

    <template v-if="activeTab === 'scrap'">
      <el-table :data="tableData" stripe border v-loading="loading" style="width: 100%;">
        <el-table-column prop="partModel" label="零件型号" min-width="140" />
        <el-table-column prop="quantity" label="报废数量" width="120" align="center">
          <template #default="{ row }">
            <span class="qty-decrease">−{{ row.quantity }}</span>
          </template>
        </el-table-column>
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
    </template>

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

    <el-dialog v-model="confirmDialogVisible" title="差异确认处理" width="560px" :close-on-click-modal="true" top="10vh">
      <el-alert
        :type="confirmRow.diffQuantity > 0 ? 'success' : 'danger'"
        show-icon
        :closable="false"
        style="margin-bottom: 16px;"
      >
        <span>零件型号：<strong>{{ confirmRow.partModel }}</strong></span>
        <span style="margin-left: 20px;">差异数量：
          <strong :style="{ color: confirmRow.diffQuantity > 0 ? '#67c23a' : '#f56c6c' }">
            {{ confirmRow.diffQuantity > 0 ? '+' : '' }}{{ confirmRow.diffQuantity }}
          </strong>
          （{{ confirmRow.diffQuantity > 0 ? '盘盈' : '盘亏' }}）
        </span>
      </el-alert>
      <el-form :model="confirmForm" label-width="100px">
        <el-form-item label="快照账面">
          <span>{{ confirmRow.systemQuantity }}</span>
        </el-form-item>
        <el-form-item label="实际库存">
          <span>{{ confirmRow.actualQuantity }}</span>
        </el-form-item>
        <el-form-item label="差异原因">
          <span>{{ confirmRow.remark || '（未填写）' }}</span>
        </el-form-item>
        <el-form-item label="处理结论" required>
          <el-input
            v-model="confirmForm.handleConclusion"
            type="textarea"
            :rows="3"
            placeholder="请填写处理结论，如：已补入库 / 已出库未登记 / 同意盘亏核销 等"
          />
        </el-form-item>
        <el-form-item label="确认人" required>
          <el-input v-model="confirmForm.confirmPerson" placeholder="请输入确认人姓名" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="confirmDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="confirmSubmitting" @click="submitConfirm">确认闭环</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getStockInPage, getStockOutPage, getStockCheckPage, getScrapPage, confirmStockCheckDiff } from '@/api'

const activeTab = ref('in')
const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const confirmDialogVisible = ref(false)
const confirmSubmitting = ref(false)
const confirmRow = reactive({})
const confirmForm = reactive({
  handleConclusion: '',
  confirmPerson: ''
})

const searchForm = reactive({
  partModel: '',
  productionLine: '',
  quarter: '',
  confirmStatus: null,
  scrapReason: '',
  dateRange: []
})

const onTabChange = () => {
  pageNum.value = 1
  loadData()
}

const resetSearch = () => {
  searchForm.partModel = ''
  searchForm.productionLine = ''
  searchForm.quarter = ''
  searchForm.confirmStatus = null
  searchForm.scrapReason = ''
  searchForm.dateRange = []
  pageNum.value = 1
  loadData()
}

const loadData = async () => {
  loading.value = true
  try {
    const baseParams = {
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      partModel: searchForm.partModel || undefined,
      startTime: searchForm.dateRange?.[0] ? `${searchForm.dateRange[0]} 00:00:00` : undefined,
      endTime: searchForm.dateRange?.[1] ? `${searchForm.dateRange[1]} 23:59:59` : undefined
    }

    let res
    switch (activeTab.value) {
      case 'in':
        res = await getStockInPage(baseParams)
        break
      case 'out':
        res = await getStockOutPage({ ...baseParams, productionLine: searchForm.productionLine || undefined })
        break
      case 'check':
        res = await getStockCheckPage({
          ...baseParams,
          quarter: searchForm.quarter || undefined,
          confirmStatus: searchForm.confirmStatus !== null && searchForm.confirmStatus !== undefined ? searchForm.confirmStatus : undefined
        })
        break
      case 'scrap':
        res = await getScrapPage({ ...baseParams, scrapReason: searchForm.scrapReason || undefined })
        break
    }

    if (res) {
      tableData.value = res.data.records
      total.value = res.data.total
    }
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

const openConfirmDialog = (row) => {
  Object.assign(confirmRow, row)
  confirmForm.handleConclusion = ''
  confirmForm.confirmPerson = ''
  confirmDialogVisible.value = true
}

const submitConfirm = async () => {
  if (!confirmForm.handleConclusion.trim()) {
    ElMessage.warning('请填写处理结论')
    return
  }
  if (!confirmForm.confirmPerson.trim()) {
    ElMessage.warning('请输入确认人')
    return
  }
  try {
    confirmSubmitting.value = true
    await confirmStockCheckDiff({
      recordId: confirmRow.id,
      handleConclusion: confirmForm.handleConclusion,
      confirmPerson: confirmForm.confirmPerson
    })
    ElMessage.success('差异确认成功，已闭环')
    confirmDialogVisible.value = false
    loadData()
  } catch (e) {
    console.error(e)
  } finally {
    confirmSubmitting.value = false
  }
}

onMounted(loadData)
</script>

<style lang="scss" scoped>
.tab-label {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.tab-ribbon {
  width: 4px;
  height: 16px;
  border-radius: 2px;
  display: inline-block;

  &.ribbon-in {
    background: #67c23a;
  }

  &.ribbon-out {
    background: #f56c6c;
  }

  &.ribbon-check {
    background: #409eff;
  }

  &.ribbon-scrap {
    background: #e6a23c;
  }
}

.tab-icon {
  font-weight: 700;
  font-size: 14px;
  line-height: 1;

  &.icon-in {
    color: #67c23a;
  }

  &.icon-out {
    color: #f56c6c;
  }

  &.icon-check {
    color: #409eff;
  }

  &.icon-scrap {
    color: #e6a23c;
  }
}

.qty-increase {
  color: #67c23a;
  font-weight: 600;
  font-size: 15px;
}

.qty-decrease {
  color: #f56c6c;
  font-weight: 600;
  font-size: 15px;
}
</style>
