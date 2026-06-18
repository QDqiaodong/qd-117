# 组装机专用顶针小件库存登记系统

装配车间配套小件物料台账系统，管控设备顶针、限位垫片的基础入库、领用、盘点单向库存变动与破损报废登记。

---

## 技术栈

| 层级 | 技术 | 版本 |
|------|------|------|
| 前端 | Vue 3 + Vite | Vue 3.4 / Vite 5 |
| UI 组件 | Element Plus | 2.4+ |
| 状态管理 | Pinia | 2.1+ |
| HTTP | Axios | 1.6+ |
| 后端 | Spring Boot | 3.3.0 |
| JDK | Eclipse Temurin | 17 |
| ORM | MyBatis Plus | 3.5.7 |
| 数据库 | MySQL | 8.0 (Alpine) |
| 缓存 | Redis | 7 (Alpine) |
| 容器 | Docker / Docker Compose | 3.8+ |
| Web Server | Nginx | Alpine |

---

## 固定端口表（禁止随意修改）

所有服务**仅绑定 IPv4 回环地址 `127.0.0.1`**，绝不开放到公网或局域网。

| 服务 | 宿主机端口 | 容器内端口 | 说明 |
|------|-----------|-----------|------|
| 前端 Nginx | **3008** | 80 | 页面访问入口 |
| 后端 SpringBoot | **8088** | 8088 | REST API (context-path: /api) |
| MySQL | **3309** | 3306 | 数据库 inventory_db |
| Redis | **6380** | 6379 | 小件规格参数有序集合缓存 |

> 避开了所有常用默认端口：80, 443, 8080, 3306, 6379, 5432, 9200

---

## 快速启动

### 方式一：Docker Compose 一键启动（推荐）

```bash
# 1. 检查端口是否被占用
./build-success.sh     # 先看一眼，或者手动执行 lsof

# 2. 启动（首次构建需要下载依赖，耐心等待）
docker compose up -d --build

# 3. 查看服务健康状态
docker compose ps

# 4. 启动完成后查看访问地址
./build-success.sh
```

启动成功后访问：
- **http://localhost:3008** （与 http://127.0.0.1:3008 等价，必须一致）

### 方式二：本地开发模式

```bash
# 1. 先启动 MySQL + Redis
docker compose up -d mysql redis

# 2. 后端启动 (端口 8088)
cd backend
mvn spring-boot:run

# 3. 前端启动 (端口 3008)
cd ../frontend
npm config set registry https://mirrors.ustc.edu.cn/npm/
npm install
npm run dev
```

### 生产环境

```bash
docker compose --env-file .env -f docker-compose.prod.yml up -d --build
./build-success.sh
```

---

## 项目结构

```
qd-117/
├── .env                           # 全局环境变量（端口、密码、DOCKER_REGISTRY）
├── docker-compose.yml             # 开发环境编排
├── docker-compose.prod.yml        # 生产环境编排
├── build-success.sh               # 构建完成自检+地址打印脚本
├── .gitignore
├── mysql/
│   └── init/init.sql              # 5 张表幂等初始化
├── backend/
│   ├── pom.xml
│   ├── settings.xml               # 网易 Maven 镜像源
│   ├── Dockerfile.dev / Dockerfile.prod
│   ├── .dockerignore
│   └── src/main/
│       ├── java/com/inventory/
│       │   ├── InventoryApplication.java
│       │   ├── cache/PartSpecCache.java         # Redis ZSET 规格缓存
│       │   ├── common/                            # 通用返回/分页
│       │   ├── config/                            # CORS/Redis/MyBatisPlus
│       │   ├── controller/ (5个)                  # REST API
│       │   ├── dto/        (4个)                  # 批量录入DTO
│       │   ├── entity/     (5个)                  # 数据库实体
│       │   ├── exception/                         # 全局异常处理
│       │   ├── mapper/     (5个)                  # MyBatis Plus Mapper
│       │   └── service/    (5个)                  # 业务逻辑层
│       └── resources/
│           ├── application.yml / application-dev.yml / application-prod.yml
│           └── mapper/ (5个 XML)
└── frontend/
    ├── package.json
    ├── vite.config.js              # 严格端口 3008 + 127.0.0.1 绑定
    ├── index.html
    ├── nginx.conf                  # 反向代理 /api -> backend:8088
    ├── Dockerfile.dev / Dockerfile.prod
    ├── .dockerignore
    └── src/
        ├── main.js / App.vue
        ├── router/index.js         # 6个页面路由
        ├── api/                    # axios 封装 + 接口清单
        ├── styles/global.scss
        ├── components/BatchInputTable.vue   # 批量录入通用组件
        └── views/                  # 6个页面
```

---

## 核心功能

### 1. 小件入库建档
录入零件型号、入库数量、存放货架编号；型号不存在自动建档，已存在则累加库存。

### 2. 产线领用出库登记
选择零件与领用产线，批量扣减库存；库存不足自动拦截并抛出明确错误。

### 3. 季度库存清点记录
一键载入全部库存，录入实物存量，自动计算差异，留存差异原因记录。

### 4. 破损零件报废登记
登记变形/断裂/磨损/其他报废原因，扣减对应库存数量。

### 5. 流水记录综合查询
入库 / 出库 / 盘点 / 报废四类流水 Tab 切换，多条件组合筛选。

---

## Docker 分层缓存策略

本项目严格遵守**仅 Docker 原生分层缓存**原则，不使用任何 `# syntax=` 语法。

### 前端 Dockerfile 分层
| 层 | COPY 内容 | 触发重下载条件 |
|----|----------|--------------|
| 1 | `package.json` (+ package-lock.json) | 依赖清单变更 |
| 2 | `.` 剩余源码 | 业务代码变更（仅重跑 build，不重下依赖） |

> npm 镜像：`https://mirrors.ustc.edu.cn/npm/`（中科大）

### 后端 Dockerfile 分层
| 层 | COPY 内容 | 触发重下载条件 |
|----|----------|--------------|
| 1 | `settings.xml` + `pom.xml` | 依赖清单变更 |
| 2 | `src/` 源码 | 业务代码变更（仅重跑编译，不重下依赖） |

> Maven 镜像：`https://maven.163yun.com/repository/public/`（网易）

### 构建上下文瘦身
- `frontend/.dockerignore`：排除 node_modules、dist、日志、IDE 配置等
- `backend/.dockerignore`：排除 target、.m2、日志、IDE 配置等

### 全链路 DOCKER_REGISTRY 统一前缀
所有基础镜像（node、nginx、maven、eclipse-temurin、mysql、redis）均通过 `${DOCKER_REGISTRY}` 前缀统一管理，在 `.env` 中配置：

```bash
# 示例：使用 DockerHub 镜像站加速
DOCKER_REGISTRY=docker.m.daocloud.io/
# 留空则直连 DockerHub
DOCKER_REGISTRY=
```

---

## Redis 规格参数缓存设计

使用 **ZSET（有序集合）** 存储小件规格参数：

| Key | Score | Member |
|-----|-------|--------|
| `inventory:part:specs` | 更新时间戳（秒） | 规格 JSON（含型号、名称、规格、货架、库存等） |
| `inventory:part:type:顶针` | 同上 | 顶针类规格 JSON |
| `inventory:part:type:限位垫片` | 同上 | 垫片类规格 JSON |

- 新增/修改/删除小件时自动同步缓存
- 可按类型或全量查询，保证最近更新的排在前面
- 提供 `/api/parts/refresh-cache` 接口手动全量刷新

---

## REST API 一览

所有接口前缀：`/api`

| 模块 | 方法 | 路径 | 说明 |
|------|------|------|------|
| 小件 | GET  | `/parts/page` | 分页查询库存 |
| 小件 | GET  | `/parts/specs` | 从 Redis 获取规格（支持按类型） |
| 小件 | POST | `/parts/refresh-cache` | 刷新规格缓存 |
| 入库 | POST | `/stock-in` | 批量入库 |
| 入库 | GET  | `/stock-in/page` | 入库记录分页 |
| 出库 | POST | `/stock-out` | 批量出库 |
| 出库 | GET  | `/stock-out/page` | 出库记录分页 |
| 盘点 | POST | `/stock-check` | 保存盘点记录 |
| 盘点 | GET  | `/stock-check/page` | 盘点记录分页 |
| 报废 | POST | `/scrap` | 批量报废 |
| 报废 | GET  | `/scrap/page` | 报废记录分页 |

---

## 端口自检与一致性验证

交付前必须执行以下三项检查，保证 `localhost` 与 `127.0.0.1` 完全等价：

```bash
# 1. 端口监听检查
lsof -nP -iTCP:3008 -sTCP:LISTEN
lsof -nP -iTCP:8088 -sTCP:LISTEN

# 2. 127.0.0.1 访问
curl -sS http://127.0.0.1:3008 | head

# 3. localhost 访问（必须与上面完全一致）
curl -sS http://localhost:3008 | head
```

> 若出现端口占用，脚本会明确打印 PID 和进程名，禁止自动换端口。

---

## 数据库表结构

共 5 张表，详见 [mysql/init/init.sql](mysql/init/init.sql)：

| 表名 | 说明 |
|------|------|
| `small_part` | 小件库存主表（型号、名称、类型、规格、货架、库存数量） |
| `stock_in_record` | 入库流水 |
| `stock_out_record` | 出库领用流水 |
| `stock_check_record` | 季度盘点记录（含系统库存、实际库存、差异） |
| `scrap_record` | 报废登记流水 |

所有表均已创建相关索引，启动时幂等初始化（`IF NOT EXISTS`），避免旧数据卷 schema 不一致问题。
