<template>
  <div class="page-container">
    <h2 class="page-title">批量录入校对</h2>

    <el-form :model="form" label-width="100px" style="margin-bottom: 16px;">
      <el-row :gutter="16">
        <el-col :span="8">
          <el-form-item label="操作人">
            <el-input v-model="form.operator" placeholder="请输入操作人姓名" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="领用产线">
            <el-select v-model="form.productionLine" placeholder="出库专用" clearable style="width: 100%;">
              <el-option label="一号装配线" value="一号装配线" />
              <el-option label="二号装配线" value="二号装配线" />
              <el-option label="三号装配线" value="三号装配线" />
              <el-option label="四号装配线" value="四号装配线" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="领用人">
            <el-input v-model="form.receiver" placeholder="出库专用" clearable />
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="16">
        <el-col :span="24">
          <el-form-item label="备注">
            <el-input v-model="form.remark" placeholder="可选" />
          </el-form-item>
        </el-col>
      </el-row>
    </el-form>

    <el-tabs v-model="activeTab" type="card" @tab-change="handleTabChange">
      <el-tab-pane label="入库录入" name="stockIn">
        <BatchInputTable
          ref="stockInTableRef"
          v-model="form.stockInItems"
          :columns="stockInColumns"
          :initial-data="form.stockInItems"
          :validators="stockInValidators"
          @change="onStockInItemsChange"
          @validation-change="onValidationChange"
        />
      </el-tab-pane>
      <el-tab-pane label="出库录入" name="stockOut">
        <BatchInputTable
          ref="stockOutTableRef"
          v-model="form.stockOutItems"
          :columns="stockOutColumns"
          :initial-data="form.stockOutItems"
          :validators="stockOutValidators"
          @change="onStockOutItemsChange"
          @validation-change="onValidationChange"
        />
      </el-tab-pane>
      <el-tab-pane label="报废录入" name="scrap">
        <BatchInputTable
          ref="scrapTableRef"
          v-model="form.scrapItems"
          :columns="scrapColumns"
          :initial-data="form.scrapItems"
          :validators="scrapValidators"
          @change="onScrapItemsChange"
          @validation-change="onValidationChange"
        />
      </el-tab-pane>
    </el-tabs>

    <div style="margin-top: 20px; text-align: center;">
      <el-button type="primary" size="large" @click="runConsistencyCheck" :loading="checking">
        <el-icon><Search /></el-icon> 执行一致性检查
      </el-button>
      <el-button size="large" @click="resetForm">
        <el-icon><RefreshLeft /></el-icon> 重置全部
      </el-button>
    </div>

    <el-divider />

    <div class="consistency-result" v-if="consistencyIssues.length > 0">
      <div class="result-header">
        <h3 class="page-title" style="font-size: 16px; margin: 0;">
          <el-icon color="#e6a23c"><Warning /></el-icon>
          一致性检查问题 ({{ consistencyIssues.length }} 项)
        </h3>
        <el-tag :type="issueSummary.type" effect="dark" size="large">
          {{ issueSummary.text }}
        </el-tag>
      </div>

      <el-alert
        v-for="(issue, idx) in consistencyIssues"
        :key="idx"
        :title="issue.title"
        :type="issue.type"
        :description="issue.description"
        show-icon
        closable
        style="margin-bottom: 12px;"
      >
        <template #default>
          <div v-if="issue.details && issue.details.length > 0">
            <ul style="margin: 8px 0 0 20px; padding: 0;">
              <li v-for="(d, di) in issue.details" :key="di" style="margin-bottom: 4px;">
                {{ d }}
              </li>
            </ul>
          </div>
        </template>
      </el-alert>
    </div>

    <div v-else-if="hasRunCheck" class="consistency-result success">
      <el-icon color="#67c23a" size="48"><Check /></el-icon>
      <p style="margin-top: 12px; color: #67c23a; font-size: 16px;">所有数据一致性检查通过！</p>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import BatchInputTable from '@/components/BatchInputTable.vue'
import { getPartList } from '@/api'

const activeTab = ref('stockIn')
const checking = ref(false)
const hasRunCheck = ref(false)
const partList = ref([])

const stockInTableRef = ref(null)
const stockOutTableRef = ref(null)
const scrapTableRef = ref(null)

const form = reactive({
  operator: '',
  productionLine: '',
  receiver: '',
  remark: '',
  stockInItems: [],
  stockOutItems: [],
  scrapItems: []
})

const consistencyIssues = ref([])

const stockInColumns = [
  { prop: 'partModel', label: '零件型号', placeholder: '必填', minWidth: 140 },
  { prop: 'partName', label: '零件名称', placeholder: '可选', minWidth: 140 },
  {
    prop: 'partType',
    label: '零件类型',
    type: 'select',
    width: 120,
    options: [
      { label: '顶针', value: '顶针' },
      { label: '限位垫片', value: '限位垫片' }
    ]
  },
  { prop: 'specParams', label: '规格参数', placeholder: '如:直径5,长度50,材质SKD11', minWidth: 200 },
  { prop: 'shelfNo', label: '货架编号', placeholder: '如:A-01-03', minWidth: 120 },
  { prop: 'quantity', label: '入库数量', type: 'number', min: 0, width: 120 },
  { prop: 'unit', label: '单位', placeholder: '默认:件', width: 100, defaultValue: '件' }
]

const stockOutColumns = reactive([
  {
    prop: 'partId',
    label: '选择零件',
    type: 'select',
    minWidth: 200,
    options: []
  },
  { prop: 'partModel', label: '零件型号', placeholder: '自动填充', minWidth: 140, disabled: true },
  { prop: 'shelfNo', label: '货架编号', placeholder: '自动填充', minWidth: 120, disabled: true },
  { prop: 'quantity', label: '领用数量', type: 'number', min: 0, width: 120 }
])

const scrapColumns = reactive([
  {
    prop: 'partId',
    label: '选择零件',
    type: 'select',
    minWidth: 260,
    options: []
  },
  { prop: 'partModel', label: '零件型号', placeholder: '自动填充', minWidth: 140, disabled: true },
  { prop: 'shelfNo', label: '货架编号', placeholder: '自动填充', minWidth: 120, disabled: true },
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

const stockInValidators = {
  partModel: (value) => {
    if (!value || value.trim() === '') {
      return { valid: false, message: '型号不能为空' }
    }
    return { valid: true }
  },
  partType: (value) => {
    if (!value) {
      return { valid: false, message: '请选择零件类型' }
    }
    return { valid: true }
  },
  shelfNo: (value) => {
    if (!value || value.trim() === '') {
      return { valid: false, message: '货架编号不能为空' }
    }
    return { valid: true }
  },
  quantity: (value) => {
    if (value === null || value === undefined || value <= 0) {
      return { valid: false, message: '数量必须大于0' }
    }
    return { valid: true }
  }
}

const stockOutValidators = {
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
  }
}

const scrapValidators = {
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

const issueSummary = computed(() => {
  const errors = consistencyIssues.value.filter(i => i.type === 'error').length
  const warnings = consistencyIssues.value.filter(i => i.type === 'warning').length
  if (errors > 0) {
    return { type: 'danger', text: `错误 ${errors} 项，警告 ${warnings} 项` }
  } else if (warnings > 0) {
    return { type: 'warning', text: `警告 ${warnings} 项` }
  }
  return { type: 'success', text: '全部通过' }
})

const loadPartList = async () => {
  try {
    const res = await getPartList()
    partList.value = res.data
    stockOutColumns[0].options = res.data.map(p => ({
      label: `${p.partModel} - ${p.partName} (库存:${p.stockQuantity})`,
      value: p.id
    }))
    scrapColumns[0].options = res.data.filter(p => p.stockQuantity > 0).map(p => ({
      label: `${p.partModel} - ${p.partName} (库存:${p.stockQuantity})`,
      value: p.id
    }))
  } catch (e) {
    console.error(e)
  }
}

const handleTabChange = () => {
  hasRunCheck.value = false
  consistencyIssues.value = []
}

const onStockInItemsChange = (items) => {
  form.stockInItems = items
  hasRunCheck.value = false
}

const onStockOutItemsChange = (items) => {
  items.forEach(row => {
    if (row.partId) {
      const part = partList.value.find(p => p.id === row.partId)
      if (part) {
        row.partModel = part.partModel
        row.shelfNo = part.shelfNo
      }
    }
  })
  form.stockOutItems = items
  hasRunCheck.value = false
}

const onScrapItemsChange = (items) => {
  items.forEach(row => {
    if (row.partId) {
      const part = partList.value.find(p => p.id === row.partId)
      if (part) {
        row.partModel = part.partModel
        row.shelfNo = part.shelfNo
      }
    }
  })
  form.scrapItems = items
  hasRunCheck.value = false
}

const onValidationChange = () => {
  hasRunCheck.value = false
}

const getAllValidRows = () => {
  const stockInValid = form.stockInItems.filter(i => i.partModel && i.partType && i.shelfNo && i.quantity > 0)
  const stockOutValid = form.stockOutItems.filter(i => i.partId && i.quantity > 0)
  const scrapValid = form.scrapItems.filter(i => i.partId && i.quantity > 0 && i.scrapReason)
  return { stockInValid, stockOutValid, scrapValid }
}

const runConsistencyCheck = async () => {
  if (form.operator.trim() === '') {
    ElMessage.warning('请输入操作人')
    return
  }

  checking.value = true
  consistencyIssues.value = []
  hasRunCheck.value = true

  try {
    const stockInValidation = stockInTableRef.value ? stockInTableRef.value.validate() : { valid: true, errors: [] }
    const stockOutValidation = stockOutTableRef.value ? stockOutTableRef.value.validate() : { valid: true, errors: [] }
    const scrapValidation = scrapTableRef.value ? scrapTableRef.value.validate() : { valid: true, errors: [] }

    if (!stockInValidation.valid || !stockOutValidation.valid || !scrapValidation.valid) {
      const allErrors = [
        ...stockInValidation.errors.map(e => ({ ...e, section: '入库' })),
        ...stockOutValidation.errors.map(e => ({ ...e, section: '出库' })),
        ...scrapValidation.errors.map(e => ({ ...e, section: '报废' }))
      ]
      consistencyIssues.value.push({
        type: 'error',
        title: '单条数据校验失败',
        description: `共发现 ${allErrors.length} 条字段级错误，请先修正后再检查一致性。`,
        details: allErrors.map(e => `[${e.section}] 第${e.rowNumber}行: ${e.message}`)
      })
    }

    const { stockInValid, stockOutValid, scrapValid } = getAllValidRows()

    if (stockInValid.length === 0 && stockOutValid.length === 0 && scrapValid.length === 0) {
      consistencyIssues.value.push({
        type: 'warning',
        title: '未录入有效数据',
        description: '三个分类下均无有效数据行，请至少录入一条数据。',
        details: []
      })
    }

    checkPartModelShelfConsistency(stockInValid, stockOutValid, scrapValid)
    checkQuantityBalance(stockInValid, stockOutValid, scrapValid)
    checkDuplicatePartModels(stockInValid)
    checkPartTypeConsistency(stockInValid)

  } finally {
    checking.value = false
  }
}

const checkPartModelShelfConsistency = (stockInValid, stockOutValid, scrapValid) => {
  const modelShelfMap = new Map()

  stockInValid.forEach((row, idx) => {
    const model = row.partModel.trim()
    const shelf = row.shelfNo.trim()
    if (!modelShelfMap.has(model)) {
      modelShelfMap.set(model, new Set())
    }
    modelShelfMap.get(model).add({ shelf, source: '入库', row: idx + 1 })
  })

  stockOutValid.forEach((row, idx) => {
    if (row.partModel) {
      const model = row.partModel.trim()
      const shelf = row.shelfNo ? row.shelfNo.trim() : ''
      if (!modelShelfMap.has(model)) {
        modelShelfMap.set(model, new Set())
      }
      if (shelf) {
        modelShelfMap.get(model).add({ shelf, source: '出库', row: idx + 1 })
      }
    }
  })

  scrapValid.forEach((row, idx) => {
    if (row.partModel) {
      const model = row.partModel.trim()
      const shelf = row.shelfNo ? row.shelfNo.trim() : ''
      if (!modelShelfMap.has(model)) {
        modelShelfMap.set(model, new Set())
      }
      if (shelf) {
        modelShelfMap.get(model).add({ shelf, source: '报废', row: idx + 1 })
      }
    }
  })

  const inconsistentModels = []
  modelShelfMap.forEach((entries, model) => {
    const shelves = new Set(Array.from(entries).map(e => e.shelf))
    if (shelves.size > 1) {
      const details = Array.from(entries).map(e => `[${e.source}] 第${e.row}行: 货架=${e.shelf}`)
      inconsistentModels.push({
        model,
        shelves: Array.from(shelves),
        details
      })
    }
  })

  if (inconsistentModels.length > 0) {
    consistencyIssues.value.push({
      type: 'error',
      title: '型号-货架一致性错误',
      description: `发现 ${inconsistentModels.length} 个零件型号在不同行中对应了不同的货架编号。`,
      details: inconsistentModels.flatMap(m => [
        `型号 "${m.model}" 出现了 ${m.shelves.length} 个不同货架: ${m.shelves.join('、')}`,
        ...m.details.map(d => `  ${d}`)
      ])
    })
  }
}

const checkQuantityBalance = (stockInValid, stockOutValid, scrapValid) => {
  const modelQuantityMap = new Map()

  stockInValid.forEach(row => {
    const model = row.partModel.trim()
    if (!modelQuantityMap.has(model)) {
      modelQuantityMap.set(model, { stockIn: 0, stockOut: 0, scrap: 0, existing: 0 })
    }
    modelQuantityMap.get(model).stockIn += Number(row.quantity)
  })

  stockOutValid.forEach(row => {
    if (row.partModel) {
      const model = row.partModel.trim()
      if (!modelQuantityMap.has(model)) {
        modelQuantityMap.set(model, { stockIn: 0, stockOut: 0, scrap: 0, existing: 0 })
      }
      modelQuantityMap.get(model).stockOut += Number(row.quantity)
      const part = partList.value.find(p => p.partModel === model)
      if (part) {
        modelQuantityMap.get(model).existing = part.stockQuantity
      }
    }
  })

  scrapValid.forEach(row => {
    if (row.partModel) {
      const model = row.partModel.trim()
      if (!modelQuantityMap.has(model)) {
        modelQuantityMap.set(model, { stockIn: 0, stockOut: 0, scrap: 0, existing: 0 })
      }
      modelQuantityMap.get(model).scrap += Number(row.quantity)
      const part = partList.value.find(p => p.partModel === model)
      if (part) {
        modelQuantityMap.get(model).existing = part.stockQuantity
      }
    }
  })

  const issues = []
  modelQuantityMap.forEach((qty, model) => {
    const totalAvailable = qty.existing + qty.stockIn
    const totalOut = qty.stockOut + qty.scrap

    if (qty.stockOut > 0 && qty.stockIn === 0 && qty.existing === 0) {
      issues.push(`型号 "${model}": 出库 ${qty.stockOut} 件，但无库存和入库记录`)
    }

    if (qty.scrap > 0 && qty.stockIn === 0 && qty.existing === 0) {
      issues.push(`型号 "${model}": 报废 ${qty.scrap} 件，但无库存和入库记录`)
    }

    if (totalOut > totalAvailable && totalAvailable > 0) {
      issues.push(`型号 "${model}": 出/报废合计 ${totalOut} 件，超过可用库存 ${totalAvailable} 件 (现有:${qty.existing} + 本次入库:${qty.stockIn})`)
    }
  })

  if (issues.length > 0) {
    consistencyIssues.value.push({
      type: 'warning',
      title: '数量平衡性警告',
      description: '部分型号的出库/报废数量与可用库存存在不平衡，请核对。',
      details: issues
    })
  }
}

const checkDuplicatePartModels = (stockInValid) => {
  const modelRows = new Map()

  stockInValid.forEach((row, idx) => {
    const model = row.partModel.trim()
    if (!modelRows.has(model)) {
      modelRows.set(model, [])
    }
    modelRows.get(model).push(idx + 1)
  })

  const duplicates = []
  modelRows.forEach((rows, model) => {
    if (rows.length > 1) {
      duplicates.push(`型号 "${model}" 在入库第 ${rows.join('、')} 行重复出现`)
    }
  })

  if (duplicates.length > 0) {
    consistencyIssues.value.push({
      type: 'warning',
      title: '入库型号重复警告',
      description: `发现 ${duplicates.length} 个型号在入库中重复录入，建议合并。`,
      details: duplicates
    })
  }
}

const checkPartTypeConsistency = (stockInValid) => {
  const modelTypeMap = new Map()

  stockInValid.forEach((row, idx) => {
    const model = row.partModel.trim()
    const type = row.partType
    if (!modelTypeMap.has(model)) {
      modelTypeMap.set(model, new Map())
    }
    if (!modelTypeMap.get(model).has(type)) {
      modelTypeMap.get(model).set(type, [])
    }
    modelTypeMap.get(model).get(type).push(idx + 1)
  })

  const inconsistencies = []
  modelTypeMap.forEach((typeMap, model) => {
    if (typeMap.size > 1) {
      const details = []
      typeMap.forEach((rows, type) => {
        details.push(`  类型 "${type}": 第 ${rows.join('、')} 行`)
      })
      inconsistencies.push(`型号 "${model}" 出现了 ${typeMap.size} 种类型定义`)
      inconsistencies.push(...details)
    }
  })

  if (inconsistencies.length > 0) {
    consistencyIssues.value.push({
      type: 'error',
      title: '入库型号-类型一致性错误',
      description: '相同型号被定义为不同的零件类型。',
      details: inconsistencies
    })
  }
}

const resetForm = () => {
  form.operator = ''
  form.productionLine = ''
  form.receiver = ''
  form.remark = ''
  form.stockInItems = []
  form.stockOutItems = []
  form.scrapItems = []
  consistencyIssues.value = []
  hasRunCheck.value = false
  activeTab.value = 'stockIn'
}

onMounted(() => {
  loadPartList()
})
</script>

<style lang="scss" scoped>
.consistency-result {
  .result-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 16px;
  }

  &.success {
    text-align: center;
    padding: 40px;
    background: #f0f9eb;
    border-radius: 8px;
  }
}
</style>
