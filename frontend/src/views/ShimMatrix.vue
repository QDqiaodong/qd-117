<template>
  <div class="page-container">
    <h2 class="page-title">限位垫片厚度视图</h2>

    <div class="stats-grid">
      <div class="stat-card">
        <div class="stat-label">垫片型号数</div>
        <div class="stat-value">{{ summary.totalTypes }}</div>
      </div>
      <div class="stat-card blue">
        <div class="stat-label">库存总数量</div>
        <div class="stat-value">{{ summary.totalStock }}</div>
      </div>
      <div class="stat-card green">
        <div class="stat-label">厚度种类</div>
        <div class="stat-value">{{ summary.thicknessCount }}</div>
      </div>
      <div class="stat-card orange">
        <div class="stat-label">外径种类</div>
        <div class="stat-value">{{ summary.outerDiameterCount }}</div>
      </div>
    </div>

    <div class="toolbar">
      <div class="toolbar-left">
        <el-input
          v-model="keyword"
          placeholder="筛选型号 / 货架号"
          clearable
          style="width: 220px;"
        />
        <el-select
          v-model="stockFilter"
          placeholder="库存筛选"
          clearable
          style="width: 190px;"
        >
          <el-option label="无库存" value="zero" />
          <el-option label="紧缺" value="danger" />
          <el-option label="偏低" value="warning" />
          <el-option label="有库存" value="has" />
        </el-select>
        <el-tooltip content="规格来自入库时的规格参数(厚度/外径)；低存量与无库存规格高亮，便于装配前提前备料" placement="top">
          <el-icon class="hint-icon"><InfoFilled /></el-icon>
        </el-tooltip>
      </div>
      <div>
        <el-button type="success" @click="loadData">
          <el-icon><Refresh /></el-icon> 刷新
        </el-button>
      </div>
    </div>

    <div v-loading="loading">
      <el-empty
        v-if="!loading && !matrix.cells.length"
        description="暂无限位垫片规格数据，请在「小件入库建档」中填写规格参数(厚度/外径)"
      />

      <template v-else>
        <div class="alert-strip" v-if="warningCount > 0">
          <el-icon><WarningFilled /></el-icon>
          <span>
            检出 <b>{{ zeroCount }}</b> 个无库存规格、<b>{{ lowCount }}</b> 个低存量规格，建议装配前优先补料，避免临时翻货架。
          </span>
        </div>

        <div class="matrix-scroll">
          <table class="matrix-table">
            <thead>
              <tr>
                <th class="corner">厚度(mm) \\ 外径(mm)</th>
                <th v-for="od in filteredOuterDiameters" :key="od">{{ od }}</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="t in filteredThicknesses" :key="t">
                <th class="row-head">{{ t }}</th>
                <td
                  v-for="od in filteredOuterDiameters"
                  :key="od"
                  :class="cellClass(getCell(t, od))"
                >
                  <template v-if="getCell(t, od)">
                    <div class="qty">{{ getCell(t, od).quantity }}</div>
                    <div class="shelf">货架: {{ getCell(t, od).shelfNo }}</div>
                    <div class="model" :title="getCell(t, od).partModel">
                      {{ getCell(t, od).partModel }}
                    </div>
                  </template>
                  <template v-else>
                    <span class="empty">—</span>
                  </template>
                </td>
              </tr>
              <tr v-if="!filteredThicknesses.length">
                <td :colspan="filteredOuterDiameters.length + 1" class="no-match">
                  无匹配的垫片规格，请调整筛选条件
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </template>
    </div>

    <div class="legend">
      <span class="lg ok">充足</span>
      <span class="lg warn">偏低</span>
      <span class="lg danger">紧缺</span>
      <span class="lg zero">无库存</span>
      <span v-if="matrix.skipped > 0" class="lg skip">
        {{ matrix.skipped }} 个型号未填写完整规格(厚度/外径)，已隐藏
      </span>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getShimMatrix } from '@/api'

const loading = ref(false)
const keyword = ref('')
const stockFilter = ref('')

const matrix = reactive({
  thicknesses: [],
  outerDiameters: [],
  cells: [],
  totalTypes: 0,
  totalStock: 0,
  skipped: 0
})

const cellMap = computed(() => {
  const map = {}
  for (const c of matrix.cells) {
    if (!map[c.thickness]) map[c.thickness] = {}
    map[c.thickness][c.outerDiameter] = c
  }
  return map
})

const summary = computed(() => ({
  totalTypes: matrix.totalTypes,
  totalStock: matrix.totalStock,
  thicknessCount: matrix.thicknesses.length,
  outerDiameterCount: matrix.outerDiameters.length
}))

const toNum = (s) => {
  const n = parseFloat(s)
  return isNaN(n) ? Number.MAX_VALUE : n
}

const passFilter = (c) => {
  if (keyword.value) {
    const kw = keyword.value.toLowerCase()
    const hit =
      (c.partModel || '').toLowerCase().includes(kw) ||
      (c.shelfNo || '').toLowerCase().includes(kw)
    if (!hit) return false
  }
  if (stockFilter.value === 'zero' && !(c.quantity <= 0)) return false
  if (stockFilter.value === 'danger' && c.warningLevel !== 'danger') return false
  if (stockFilter.value === 'warning' && c.warningLevel !== 'warning') return false
  if (stockFilter.value === 'has' && !(c.quantity > 0)) return false
  return true
}

const filteredThicknesses = computed(() => {
  const set = new Set()
  for (const c of matrix.cells) {
    if (passFilter(c)) set.add(c.thickness)
  }
  return [...set].sort((a, b) => toNum(a) - toNum(b))
})

const filteredOuterDiameters = computed(() => {
  const set = new Set()
  for (const c of matrix.cells) {
    if (passFilter(c)) set.add(c.outerDiameter)
  }
  return [...set].sort((a, b) => toNum(a) - toNum(b))
})

const zeroCount = computed(() => matrix.cells.filter((c) => c.quantity <= 0).length)
const lowCount = computed(
  () => matrix.cells.filter((c) => c.warningLevel === 'danger' || c.warningLevel === 'warning').length
)
const warningCount = computed(() => zeroCount.value + lowCount.value)

const getCell = (t, od) => {
  return cellMap.value[t]?.[od] || null
}

const cellClass = (c) => {
  if (!c) return 'c-empty'
  if (c.quantity <= 0) return 'c-zero'
  if (c.warningLevel === 'danger') return 'c-danger'
  if (c.warningLevel === 'warning') return 'c-warn'
  return 'c-ok'
}

const loadData = async () => {
  loading.value = true
  try {
    const res = await getShimMatrix()
    const data = res.data
    matrix.thicknesses = data.thicknesses || []
    matrix.outerDiameters = data.outerDiameters || []
    matrix.cells = data.cells || []
    matrix.totalTypes = data.totalTypes || 0
    matrix.totalStock = data.totalStock || 0
    matrix.skipped = data.skipped || 0
    if (matrix.skipped > 0) {
      ElMessage.info(`${matrix.skipped} 个垫片未填写完整规格(厚度/外径)，已隐藏`)
    }
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

.alert-strip {
  display: flex;
  align-items: center;
  gap: 8px;
  background: #fef0f0;
  color: #f56c6c;
  border: 1px solid #fbc4c4;
  border-radius: 6px;
  padding: 8px 14px;
  margin-bottom: 12px;
  font-size: 13px;

  b {
    font-size: 15px;
  }
}

.matrix-scroll {
  overflow-x: auto;
  margin-top: 8px;
}

.matrix-table {
  border-collapse: collapse;
  width: 100%;
  min-width: 600px;

  th,
  td {
    border: 1px solid #ebeef5;
    text-align: center;
    padding: 0;
  }

  thead th {
    background: #f5f7fa;
    color: #606266;
    font-weight: 600;
    padding: 10px 14px;
    position: sticky;
    top: 0;
    z-index: 2;
    white-space: nowrap;
  }

  th.corner {
    background: #304156;
    color: #fff;
    position: sticky;
    left: 0;
    z-index: 3;
    white-space: nowrap;
  }

  th.row-head {
    background: #f5f7fa;
    font-weight: 600;
    color: #303133;
    padding: 10px 14px;
    position: sticky;
    left: 0;
    z-index: 1;
    white-space: nowrap;
  }

  td {
    vertical-align: middle;
    min-width: 96px;

    .qty {
      font-size: 18px;
      font-weight: 700;
      padding: 8px 6px 2px;
    }

    .shelf {
      font-size: 12px;
      color: #606266;
    }

    .model {
      font-size: 11px;
      color: #909399;
      padding: 2px 6px 8px;
      max-width: 120px;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
      margin: 0 auto;
    }

    .empty {
      color: #dcdfe6;
    }
  }

  td.c-ok {
    background: #f0f9eb;

    .qty {
      color: #67c23a;
    }
  }

  td.c-warn {
    background: #fdf6ec;

    .qty {
      color: #e6a23c;
    }
  }

  td.c-danger {
    background: #fef0f0;

    .qty {
      color: #f56c6c;
    }
  }

  td.c-zero {
    background: #f4f4f5;

    .qty {
      color: #909399;
    }
  }

  td.c-empty {
    background: #fafafa;
  }

  .no-match {
    text-align: center;
    color: #909399;
    padding: 24px;
  }
}

.legend {
  margin-top: 16px;
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  font-size: 13px;

  .lg {
    padding: 2px 10px;
    border-radius: 4px;
  }

  .ok {
    background: #f0f9eb;
    color: #67c23a;
  }

  .warn {
    background: #fdf6ec;
    color: #e6a23c;
  }

  .danger {
    background: #fef0f0;
    color: #f56c6c;
  }

  .zero {
    background: #f4f4f5;
    color: #909399;
  }

  .skip {
    background: #ecf5ff;
    color: #409eff;
  }
}
</style>
