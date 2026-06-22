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
      <el-table-column label="规格参数" min-width="200">
        <template #default="{ row }">
          <el-tooltip
            :disabled="!row.specParams"
            placement="top"
            effect="dark"
            :show-after="300"
          >
            <template #content>
              <div class="spec-tooltip">
                <div class="spec-tooltip-title">完整规格参数</div>
                <pre class="spec-tooltip-json">{{ formatTooltipJson(row.specParams) }}</pre>
              </div>
            </template>
            <div class="spec-display">
              <template v-if="row.partType === '顶针'">
                <el-tag v-if="getSpecValue(row, ['diameter', '直径', 'Diameter', 'D'])" type="primary" size="small" effect="plain" class="spec-tag">
                  直径 {{ getSpecValue(row, ['diameter', '直径', 'Diameter', 'D']) }}
                </el-tag>
                <el-tag v-if="getSpecValue(row, ['length', '长度', 'Length', 'L'])" type="primary" size="small" effect="plain" class="spec-tag">
                  长度 {{ getSpecValue(row, ['length', '长度', 'Length', 'L']) }}
                </el-tag>
                <span v-if="!hasPinSpecs(row)" class="spec-raw">{{ row.specParams || '-' }}</span>
              </template>
              <template v-else-if="row.partType === '限位垫片'">
                <el-tag v-if="getSpecValue(row, ['thickness', '厚度', 'Thickness', 'T'])" type="success" size="small" effect="plain" class="spec-tag">
                  厚度 {{ getSpecValue(row, ['thickness', '厚度', 'Thickness', 'T']) }}
                </el-tag>
                <el-tag v-if="getSpecValue(row, ['innerDiameter', '孔径', 'InnerDiameter', 'ID', '内直径', 'holeDiameter', 'hole'])" type="success" size="small" effect="plain" class="spec-tag">
                  孔径 {{ getSpecValue(row, ['innerDiameter', '孔径', 'InnerDiameter', 'ID', '内直径', 'holeDiameter', 'hole']) }}
                </el-tag>
                <span v-if="!hasShimSpecs(row)" class="spec-raw">{{ row.specParams || '-' }}</span>
              </template>
              <template v-else>
                <span class="spec-raw">{{ row.specParams || '-' }}</span>
              </template>
            </div>
          </el-tooltip>
        </template>
      </el-table-column>
      <el-table-column prop="shelfNo" label="货架编号" width="120" align="center" />
      <el-table-column prop="stockQuantity" label="库存数量" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="getWarningTagType(row.warningLevel)">
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
      keyword: keyword.value ? keyword.value.trim() || undefined : undefined,
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

const tryParseJson = (text) => {
  if (!text) return null
  try {
    const parsed = JSON.parse(text)
    if (parsed && typeof parsed === 'object' && !Array.isArray(parsed)) {
      return parsed
    }
    return null
  } catch {
    return null
  }
}

const stripToNumber = (text) => {
  if (!text) return null
  const match = text.match(/(\d+(?:\.\d+)?)/)
  return match ? match[1] : (text.trim() ? text.trim() : null)
}

const getSpecValue = (row, keys) => {
  if (!row || !row.specParams) return null
  const json = tryParseJson(row.specParams)
  if (json) {
    for (const key of keys) {
      const value = json[key]
      if (value !== null && value !== undefined && String(value).trim() !== '') {
        const numVal = stripToNumber(String(value))
        if (numVal) return numVal
      }
    }
  }
  const raw = row.specParams
  for (const key of keys) {
    const pattern = new RegExp(key + '\\s*[:：]?\\s*(\\d+(?:\\.\\d+)?)')
    const match = raw.match(pattern)
    if (match) {
      return match[1]
    }
  }
  return null
}

const hasPinSpecs = (row) => {
  const diameter = getSpecValue(row, ['diameter', '直径', 'Diameter', 'D'])
  const length = getSpecValue(row, ['length', '长度', 'Length', 'L'])
  return !!(diameter || length)
}

const hasShimSpecs = (row) => {
  const thickness = getSpecValue(row, ['thickness', '厚度', 'Thickness', 'T'])
  const hole = getSpecValue(row, ['innerDiameter', '孔径', 'InnerDiameter', 'ID', '内直径', 'holeDiameter', 'hole'])
  return !!(thickness || hole)
}

const formatTooltipJson = (text) => {
  if (!text) return '（无规格参数）'
  const json = tryParseJson(text)
  if (json) {
    return JSON.stringify(json, null, 2)
  }
  return text
}

const getWarningTagType = (level) => {
  if (level === 'danger') return 'danger'
  if (level === 'warning') return 'warning'
  return 'success'
}

onMounted(loadData)
</script>

<style lang="scss" scoped>
.spec-display {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px;
  line-height: 1.6;

  .spec-tag {
    border-radius: 4px;
    font-weight: 500;
  }

  .spec-raw {
    color: #606266;
    font-size: 13px;
  }
}
</style>

<style lang="scss">
.spec-tooltip {
  max-width: 420px;

  .spec-tooltip-title {
    font-size: 13px;
    font-weight: 600;
    color: #fff;
    margin-bottom: 8px;
    padding-bottom: 6px;
    border-bottom: 1px solid rgba(255, 255, 255, 0.2);
  }

  .spec-tooltip-json {
    margin: 0;
    padding: 0;
    font-family: 'SF Mono', Monaco, 'Cascadia Code', Consolas, 'Courier New', monospace;
    font-size: 12px;
    line-height: 1.6;
    color: #e6e6e6;
    white-space: pre-wrap;
    word-break: break-all;
  }
}
</style>
