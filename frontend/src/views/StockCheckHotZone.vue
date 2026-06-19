<template>
  <div class="page-container">
    <h2 class="page-title">季度清点热区面板</h2>

    <div class="toolbar">
      <div class="toolbar-left">
        <el-input
          v-model="quarter"
          :placeholder="currentQuarter"
          style="width: 200px;"
        >
          <template #prepend>盘点季度</template>
        </el-input>
        <el-button type="primary" @click="loadData">
          <el-icon><Search /></el-icon> 查询
        </el-button>
        <el-button @click="resetQuarter">本季度</el-button>
        <el-tooltip content="按货架编号汇总盘盈/盘亏；蓝色块为顶针、橙色块为限位垫片，点击货架展开明细" placement="top">
          <el-icon class="hint-icon"><InfoFilled /></el-icon>
        </el-tooltip>
      </div>
      <div>
        <el-button type="success" @click="loadData">
          <el-icon><Refresh /></el-icon> 刷新
        </el-button>
      </div>
    </div>

    <div class="stats-grid">
      <div class="stat-card">
        <div class="stat-label">涉及货架数</div>
        <div class="stat-value">{{ summary.totalShelves }}</div>
      </div>
      <div class="stat-card green">
        <div class="stat-label">盘盈总数</div>
        <div class="stat-value">+{{ summary.totalGain }}</div>
      </div>
      <div class="stat-card orange">
        <div class="stat-label">盘亏总数</div>
        <div class="stat-value">-{{ summary.totalLoss }}</div>
      </div>
      <div class="stat-card blue">
        <div class="stat-label">盘点行数</div>
        <div class="stat-value">{{ summary.totalRows }}</div>
      </div>
    </div>

    <div v-loading="loading">
      <el-empty
        v-if="!loading && !shelves.length"
        :description="quarter ? `暂无 ${quarter} 季度盘点记录` : '暂无盘点记录'"
      />

      <div v-else class="shelf-grid">
        <div
          v-for="shelf in shelves"
          :key="shelf.shelfNo"
          class="shelf-card"
          :class="netClass(shelf)"
          @click="toggle(shelf.shelfNo)"
        >
          <div class="shelf-head">
            <span class="shelf-no">货架 {{ shelf.shelfNo }}</span>
            <span class="shelf-meta">{{ shelf.rowCount }} 行</span>
            <el-icon class="expand-icon" :class="{ open: isOpen(shelf.shelfNo) }">
              <ArrowDown />
            </el-icon>
          </div>

          <div class="block-row">
            <div class="block block-pin" :class="{ dim: shelf.pinGain === 0 && shelf.pinLoss === 0 }">
              <div class="block-title">顶针</div>
              <div class="block-vals">
                <span class="gain">+{{ shelf.pinGain }}</span>
                <span class="sep">/</span>
                <span class="loss">-{{ shelf.pinLoss }}</span>
              </div>
            </div>
            <div class="block block-gasket" :class="{ dim: shelf.gasketGain === 0 && shelf.gasketLoss === 0 }">
              <div class="block-title">限位垫片</div>
              <div class="block-vals">
                <span class="gain">+{{ shelf.gasketGain }}</span>
                <span class="sep">/</span>
                <span class="loss">-{{ shelf.gasketLoss }}</span>
              </div>
            </div>
          </div>

          <div class="shelf-total">
            <span>盘盈 <b class="gain">+{{ shelf.totalGain }}</b></span>
            <span class="dot">·</span>
            <span>盘亏 <b class="loss">-{{ shelf.totalLoss }}</b></span>
          </div>

          <div v-if="isOpen(shelf.shelfNo)" class="shelf-detail" @click.stop>
            <el-table :data="shelf.rows" size="small" border stripe>
              <el-table-column prop="partModel" label="零件型号" min-width="130" />
              <el-table-column prop="partType" label="类型" width="90" align="center">
                <template #default="{ row }">
                  <el-tag
                    size="small"
                    :type="row.partType === '顶针' ? 'primary' : (row.partType === '限位垫片' ? 'warning' : 'info')"
                  >
                    {{ row.partType || '未知' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="systemQuantity" label="系统库存" width="90" align="center" />
              <el-table-column prop="actualQuantity" label="实际库存" width="90" align="center" />
              <el-table-column prop="diffQuantity" label="差异" width="90" align="center">
                <template #default="{ row }">
                  <el-tag
                    size="small"
                    :type="row.diffQuantity > 0 ? 'success' : (row.diffQuantity < 0 ? 'danger' : 'info')"
                  >
                    {{ row.diffQuantity > 0 ? '+' : '' }}{{ row.diffQuantity }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="checkPerson" label="盘点人" width="90" align="center" />
              <el-table-column prop="remark" label="差异原因" min-width="140" show-overflow-tooltip />
              <el-table-column prop="createTime" label="盘点时间" width="160" align="center" />
            </el-table>
          </div>
        </div>
      </div>
    </div>

    <div class="legend">
      <span class="lg pin">顶针(蓝)</span>
      <span class="lg gasket">限位垫片(橙)</span>
      <span class="lg gain">盘盈(+)</span>
      <span class="lg loss">盘亏(-)</span>
      <span class="lg net-gain">净盘盈货架</span>
      <span class="lg net-loss">净盘亏货架</span>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { getStockCheckHotZone } from '@/api'

const loading = ref(false)
const quarter = ref('')
const expandedSet = ref(new Set())

const summary = reactive({
  totalShelves: 0,
  totalGain: 0,
  totalLoss: 0,
  totalRows: 0
})

const shelves = ref([])

const currentQuarter = computed(() => {
  const now = new Date()
  const year = now.getFullYear()
  const q = Math.floor(now.getMonth() / 3) + 1
  return `${year}-Q${q}`
})

const isOpen = (no) => expandedSet.value.has(no)

const toggle = (no) => {
  const s = new Set(expandedSet.value)
  if (s.has(no)) {
    s.delete(no)
  } else {
    s.add(no)
  }
  expandedSet.value = s
}

const netClass = (shelf) => {
  if (shelf.totalGain === 0 && shelf.totalLoss === 0) return 'net-zero'
  if (shelf.totalGain > shelf.totalLoss) return 'net-gain'
  if (shelf.totalLoss > shelf.totalGain) return 'net-loss'
  return 'net-zero'
}

const resetQuarter = () => {
  quarter.value = currentQuarter.value
  loadData()
}

const loadData = async () => {
  loading.value = true
  try {
    const res = await getStockCheckHotZone({
      quarter: quarter.value || undefined
    })
    const data = res.data
    shelves.value = data.shelves || []
    summary.totalShelves = data.totalShelves || 0
    summary.totalGain = data.totalGain || 0
    summary.totalLoss = data.totalLoss || 0
    summary.totalRows = data.totalRows || 0
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

onMounted(loadData)
</script>

<style lang="scss" scoped>
.hint-icon {
  font-size: 18px;
  color: #909399;
  cursor: help;
}

.shelf-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 14px;
  margin-top: 8px;
}

.shelf-card {
  background: #fff;
  border: 1px solid #ebeef5;
  border-left: 4px solid #c0c4cc;
  border-radius: 8px;
  padding: 12px 14px;
  cursor: pointer;
  transition: box-shadow 0.2s, transform 0.2s;

  &:hover {
    box-shadow: 0 4px 14px rgba(0, 0, 0, 0.08);
    transform: translateY(-1px);
  }

  &.net-gain {
    border-left-color: #67c23a;
    background: linear-gradient(180deg, #f6ffed 0%, #ffffff 60%);
  }

  &.net-loss {
    border-left-color: #f56c6c;
    background: linear-gradient(180deg, #fff0f0 0%, #ffffff 60%);
  }

  &.net-zero {
    border-left-color: #c0c4cc;
    background: #fafafa;
  }
}

.shelf-head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;

  .shelf-no {
    font-size: 16px;
    font-weight: 700;
    color: #303133;
  }

  .shelf-meta {
    font-size: 12px;
    color: #909399;
    background: #f0f2f5;
    padding: 1px 8px;
    border-radius: 10px;
  }

  .expand-icon {
    margin-left: auto;
    color: #909399;
    transition: transform 0.2s;

    &.open {
      transform: rotate(180deg);
    }
  }
}

.block-row {
  display: flex;
  gap: 8px;
  margin-bottom: 10px;
}

.block {
  flex: 1;
  border-radius: 6px;
  padding: 8px 10px;
  color: #fff;

  &.block-pin {
    background: linear-gradient(135deg, #4facfe 0%, #2575fc 100%);
  }

  &.block-gasket {
    background: linear-gradient(135deg, #fa709a 0%, #fee140 100%);
  }

  &.dim {
    opacity: 0.45;
  }

  .block-title {
    font-size: 12px;
    opacity: 0.9;
    margin-bottom: 2px;
  }

  .block-vals {
    font-size: 14px;
    font-weight: 700;

    .gain {
      color: #f0fff4;
    }

    .loss {
      color: #fff0f0;
    }

    .sep {
      opacity: 0.6;
      margin: 0 4px;
    }
  }
}

.shelf-total {
  font-size: 13px;
  color: #606266;

  b {
    font-size: 14px;
  }

  .gain {
    color: #67c23a;
  }

  .loss {
    color: #f56c6c;
  }

  .dot {
    margin: 0 6px;
    color: #c0c4cc;
  }
}

.shelf-detail {
  margin-top: 12px;
  padding-top: 10px;
  border-top: 1px dashed #ebeef5;
}

.legend {
  margin-top: 18px;
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  font-size: 13px;

  .lg {
    padding: 2px 10px;
    border-radius: 4px;
  }

  .pin {
    background: #ecf5ff;
    color: #2575fc;
  }

  .gasket {
    background: #fef0f0;
    color: #fa709a;
  }

  .gain {
    background: #f0f9eb;
    color: #67c23a;
  }

  .loss {
    background: #fef0f0;
    color: #f56c6c;
  }

  .net-gain {
    border-left: 3px solid #67c23a;
    color: #67c23a;
    padding-left: 8px;
  }

  .net-loss {
    border-left: 3px solid #f56c6c;
    color: #f56c6c;
    padding-left: 8px;
  }
}
</style>
