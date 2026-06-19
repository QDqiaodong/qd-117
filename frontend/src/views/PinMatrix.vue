<template>
  <div class="page-container">
    <h2 class="page-title">顶针规格矩阵</h2>

    <div class="stats-grid">
      <div class="stat-card">
        <div class="stat-label">顶针型号数</div>
        <div class="stat-value">{{ summary.totalTypes }}</div>
      </div>
      <div class="stat-card blue">
        <div class="stat-label">库存总数量</div>
        <div class="stat-value">{{ summary.totalStock }}</div>
      </div>
      <div class="stat-card green">
        <div class="stat-label">材质种类</div>
        <div class="stat-value">{{ summary.materialCount }}</div>
      </div>
      <div class="stat-card orange">
        <div class="stat-label">规格组合数</div>
        <div class="stat-value">{{ summary.specCount }}</div>
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
          style="width: 170px;"
        >
          <el-option label="紧缺 (≤10)" value="low" />
          <el-option label="有库存 (>0)" value="has" />
        </el-select>
        <el-tooltip content="规格来自入库时的规格参数(直径/长度/材质)" placement="top">
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
        description="暂无顶针规格数据，请在「小件入库建档」中填写规格参数(直径/长度/材质)"
      />

      <template v-else-if="filteredMaterials.length">
        <el-tabs v-model="activeMaterial" type="card">
          <el-tab-pane
            v-for="m in filteredMaterials"
            :key="m"
            :name="m"
            :label="`${m} (${materialStat(m).types}种 / ${materialStat(m).stock}件)`"
          >
            <div class="matrix-scroll">
              <table class="matrix-table">
                <thead>
                  <tr>
                    <th class="corner">直径(mm) \\ 长度(mm)</th>
                    <th v-for="l in activeLengths" :key="l">{{ l }}</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="d in activeDiameters" :key="d">
                    <th class="row-head">{{ d }}</th>
                    <td
                      v-for="l in activeLengths"
                      :key="l"
                      :class="cellClass(getCell(m, d, l))"
                    >
                      <template v-if="getCell(m, d, l)">
                        <div class="qty">{{ getCell(m, d, l).quantity }}</div>
                        <div class="shelf">货架: {{ getCell(m, d, l).shelfNo }}</div>
                        <div class="model" :title="getCell(m, d, l).partModel">
                          {{ getCell(m, d, l).partModel }}
                        </div>
                      </template>
                      <template v-else>
                        <span class="empty">—</span>
                      </template>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </el-tab-pane>
        </el-tabs>
      </template>

      <el-empty v-else description="无匹配的顶针规格，请调整筛选条件" />
    </div>

    <div class="legend">
      <span class="lg ok">充足 (&gt;50)</span>
      <span class="lg warn">偏少 (11-50)</span>
      <span class="lg danger">紧缺 (≤10)</span>
      <span class="lg zero">无库存</span>
      <span v-if="matrix.skipped > 0" class="lg skip">
        {{ matrix.skipped }} 个型号未填写完整规格(直径/长度)，已隐藏
      </span>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, watch, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getPinMatrix } from '@/api'

const loading = ref(false)
const keyword = ref('')
const stockFilter = ref('')
const activeMaterial = ref('')

const matrix = reactive({
  materials: [],
  diameters: [],
  lengths: [],
  cells: [],
  totalTypes: 0,
  totalStock: 0,
  skipped: 0
})

const cellMap = computed(() => {
  const map = {}
  for (const c of matrix.cells) {
    if (!map[c.material]) map[c.material] = {}
    if (!map[c.material][c.diameter]) map[c.material][c.diameter] = {}
    map[c.material][c.diameter][c.length] = c
  }
  return map
})

const summary = computed(() => ({
  totalTypes: matrix.totalTypes,
  totalStock: matrix.totalStock,
  materialCount: matrix.materials.length,
  specCount: matrix.cells.length
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
  if (stockFilter.value === 'low' && !(c.quantity <= 10)) return false
  if (stockFilter.value === 'has' && !(c.quantity > 0)) return false
  return true
}

const filteredMaterials = computed(() => {
  const set = new Set()
  for (const c of matrix.cells) {
    if (passFilter(c)) set.add(c.material)
  }
  return matrix.materials.filter((m) => set.has(m))
})

const activeCells = computed(() =>
  matrix.cells.filter((c) => c.material === activeMaterial.value && passFilter(c))
)

const activeDiameters = computed(() => {
  const set = new Set(activeCells.value.map((c) => c.diameter))
  return [...set].sort((a, b) => toNum(a) - toNum(b))
})

const activeLengths = computed(() => {
  const set = new Set(activeCells.value.map((c) => c.length))
  return [...set].sort((a, b) => toNum(a) - toNum(b))
})

const getCell = (material, d, l) => {
  return cellMap.value[material]?.[d]?.[l] || null
}

const materialStat = (m) => {
  let types = 0
  let stock = 0
  for (const c of matrix.cells) {
    if (c.material === m) {
      types++
      stock += c.quantity
    }
  }
  return { types, stock }
}

const cellClass = (c) => {
  if (!c) return 'c-empty'
  if (c.quantity <= 0) return 'c-zero'
  if (c.quantity <= 10) return 'c-danger'
  if (c.quantity <= 50) return 'c-warn'
  return 'c-ok'
}

watch(filteredMaterials, (list) => {
  if (list.length && !list.includes(activeMaterial.value)) {
    activeMaterial.value = list[0]
  }
})

const loadData = async () => {
  loading.value = true
  try {
    const res = await getPinMatrix()
    const data = res.data
    matrix.materials = data.materials || []
    matrix.diameters = data.diameters || []
    matrix.lengths = data.lengths || []
    matrix.cells = data.cells || []
    matrix.totalTypes = data.totalTypes || 0
    matrix.totalStock = data.totalStock || 0
    matrix.skipped = data.skipped || 0
    if (matrix.materials.length && !matrix.materials.includes(activeMaterial.value)) {
      activeMaterial.value = matrix.materials[0]
    }
    if (matrix.skipped > 0) {
      ElMessage.info(`${matrix.skipped} 个顶针未填写完整规格(直径/长度)，已隐藏`)
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
