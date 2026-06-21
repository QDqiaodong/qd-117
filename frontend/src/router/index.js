import { createRouter, createWebHashHistory } from 'vue-router'

const routes = [
  {
    path: '/',
    name: 'Dashboard',
    component: () => import('@/views/Dashboard.vue'),
    meta: { title: '库存总览' }
  },
  {
    path: '/pin-matrix',
    name: 'PinMatrix',
    component: () => import('@/views/PinMatrix.vue'),
    meta: { title: '顶针规格矩阵' }
  },
  {
    path: '/shim-matrix',
    name: 'ShimMatrix',
    component: () => import('@/views/ShimMatrix.vue'),
    meta: { title: '垫片厚度视图' }
  },
  {
    path: '/stock-in',
    name: 'StockIn',
    component: () => import('@/views/StockIn.vue'),
    meta: { title: '小件入库建档' }
  },
  {
    path: '/stock-out',
    name: 'StockOut',
    component: () => import('@/views/StockOut.vue'),
    meta: { title: '产线领用出库' }
  },
  {
    path: '/line-quota',
    name: 'LineQuota',
    component: () => import('@/views/LineQuota.vue'),
    meta: { title: '产线领用配额' }
  },
  {
    path: '/stock-check',
    name: 'StockCheck',
    component: () => import('@/views/StockCheck.vue'),
    meta: { title: '季度库存清点' }
  },
  {
    path: '/stock-check-hotzone',
    name: 'StockCheckHotZone',
    component: () => import('@/views/StockCheckHotZone.vue'),
    meta: { title: '季度清点热区' }
  },
  {
    path: '/scrap',
    name: 'Scrap',
    component: () => import('@/views/Scrap.vue'),
    meta: { title: '破损零件报废' }
  },
  {
    path: '/scrap-reason',
    name: 'ScrapReason',
    component: () => import('@/views/ScrapReason.vue'),
    meta: { title: '破损原因字典' }
  },
  {
    path: '/pin-box',
    name: 'PinBox',
    component: () => import('@/views/PinBox.vue'),
    meta: { title: '顶针盒号管理' }
  },
  {
    path: '/records',
    name: 'Records',
    component: () => import('@/views/Records.vue'),
    meta: { title: '流水记录查询' }
  },
  {
    path: '/batch-proofread',
    name: 'BatchProofread',
    component: () => import('@/views/BatchProofread.vue'),
    meta: { title: '批量录入校对' }
  }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  document.title = to.meta.title ? `${to.meta.title} - 组装机顶针小件库存登记系统` : '组装机顶针小件库存登记系统'
  next()
})

export default router
