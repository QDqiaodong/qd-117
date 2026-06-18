<template>
  <el-container class="app-container">
    <el-header class="app-header">
      <div class="header-left">
        <el-icon size="28" color="#409EFF"><Setting /></el-icon>
        <h1>组装机专用顶针小件库存登记系统</h1>
      </div>
      <div class="header-right">
        <span class="user-info">{{ currentDate }}</span>
      </div>
    </el-header>

    <el-container>
      <el-aside width="220px" class="app-aside">
        <el-menu
          :default-active="activeMenu"
          router
          class="side-menu"
          background-color="#304156"
          text-color="#bfcbd9"
          active-text-color="#409EFF"
        >
          <el-menu-item index="/">
            <el-icon><DataBoard /></el-icon>
            <span>库存总览</span>
          </el-menu-item>
          <el-menu-item index="/stock-in">
            <el-icon><AddBox /></el-icon>
            <span>小件入库建档</span>
          </el-menu-item>
          <el-menu-item index="/stock-out">
            <el-icon><MinusBox /></el-icon>
            <span>产线领用出库</span>
          </el-menu-item>
          <el-menu-item index="/stock-check">
            <el-icon><Finished /></el-icon>
            <span>季度库存清点</span>
          </el-menu-item>
          <el-menu-item index="/scrap">
            <el-icon><Delete /></el-icon>
            <span>破损零件报废</span>
          </el-menu-item>
          <el-menu-item index="/records">
            <el-icon><Document /></el-icon>
            <span>流水记录查询</span>
          </el-menu-item>
        </el-menu>
      </el-aside>

      <el-main class="app-main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()
const activeMenu = computed(() => route.path)

const currentDate = ref(new Date().toLocaleDateString('zh-CN', {
  year: 'numeric',
  month: 'long',
  day: 'numeric',
  weekday: 'long'
}))
</script>

<style lang="scss" scoped>
.app-container {
  height: 100vh;
}

.app-header {
  background: linear-gradient(90deg, #1e3a8a 0%, #3b82f6 100%);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);

  .header-left {
    display: flex;
    align-items: center;
    gap: 12px;

    h1 {
      margin: 0;
      font-size: 20px;
      font-weight: 600;
    }
  }

  .header-right {
    .user-info {
      font-size: 14px;
      opacity: 0.9;
    }
  }
}

.app-aside {
  background: #304156;
  overflow-y: auto;

  .side-menu {
    border: none;
  }
}

.app-main {
  background: #f0f2f5;
  padding: 20px;
  overflow-y: auto;
}
</style>
