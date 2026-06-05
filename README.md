# 智能图库中心

> 一个轻量的图片管理与协作平台。浏览公共图库、创建个人或团队空间，让素材收集与团队协作变得简单。

![Vue 3](https://img.shields.io/badge/Frontend-Vue_3-4fc08d?logo=vue.js)
![TypeScript](https://img.shields.io/badge/TypeScript-5.6-3178c6?logo=typescript)
![Spring Boot](https://img.shields.io/badge/Backend-Spring_Boot-6db33f?logo=springboot)
![Ant Design Vue](https://img.shields.io/badge/UI-Ant_Design_Vue_4-1677ff?logo=antdesign)
![License](https://img.shields.io/badge/License-MIT-yellow)

---

## 目录

- [项目概述](#项目概述)
- [技术栈](#技术栈)
- [功能特性](#功能特性)
- [快速开始](#快速开始)
  - [环境要求](#环境要求)
  - [本地开发](#本地开发)
  - [Docker 部署](#docker-部署)
- [项目结构](#项目结构)
- [API 文档](#api-文档)
- [许可证](#许可证)

---

## 项目概述

**数贤智能图库中心**（Yu Picture）是一个全栈图片管理平台，围绕 **收集 → 整理 → 协作** 的流程设计。主要能力：

- **公共图库** — 浏览、搜索、收藏海量图片
- **空间管理** — 私有空间 / 团队空间，按空间隔离图片资源
- **AI 能力** — 文生图、图片扩图（Outpainting）、以图搜图
- **权限体系** — 空间级别的角色权限控制，支持多人协作

---

## 技术栈

### 前端

| 技术 | 用途 |
|------|------|
| Vue 3 (Composition API) | 框架 |
| Vite 6 | 构建工具 |
| TypeScript | 类型安全 |
| Ant Design Vue 4 | UI 组件库 |
| Pinia | 状态管理 |
| Vue Router 4 | 路由 |
| Axios | HTTP 请求 |
| ECharts | 数据可视化 |
| Vue Cropper | 图片裁剪 |

### 后端

| 技术 | 用途 |
|------|------|
| Java 8+ | 运行环境 |
| Spring Boot 2.7 | MVC 框架 |
| MyBatis-Plus | ORM |
| MySQL 8 | 关系型数据库 |
| Redis 7 | 会话存储 / 缓存 |
| Sa-Token | 认证与鉴权 |
| 腾讯云 COS | 图片对象存储 |
| 腾讯云数据万象 CI | 图片压缩 / 缩略图 |
| 阿里云 AI (万象) | 文生图 / 扩图 |
| Knife4j | 接口文档 |
| Maven | 构建管理 |

### 部署

| 技术 | 用途 |
|------|------|
| Docker Compose | 容器编排 |
| Nginx | 反向代理 / 静态资源 |

---

## 功能特性

### 图库
- ✅ 瀑布流（Masonry）图片列表
- ✅ 按分类 / 标签筛选
- ✅ 关键词搜索
- ✅ 图片详情（元数据、主色调、宽高比）
- ✅ 图片收藏到空间

### 空间
- ✅ 私有空间 — 个人图片管理
- ✅ 团队空间 — 成员协作、角色权限
- ✅ 空间配额控制（图片数 / 存储量）
- ✅ 空间分析仪表盘

### AI 能力
- ✅ 文生图（阿里云通义万相）
- ✅ 图片扩图（AI Outpainting）
- ✅ 以图搜图

### 管理
- ✅ 用户管理（管理员）
- ✅ 图片审核
- ✅ 空间管理
- ✅ 批量图片处理

### 体验
- ✅ 图片懒加载
- ✅ 响应式布局（桌面 / 平板 / 手机）
- ✅ 黑白简约 UI 风格
- ✅ 图片 WEBP 压缩自动优化

---

## 快速开始

### 环境要求

- Node.js 18+
- JDK 8+
- Maven 3.6+
- MySQL 8+
- Redis 7+
- 腾讯云 COS 桶（用于图片存储）

### 本地开发

#### 1. 数据库初始化

```bash
# 创建数据库
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS intelligent_picture CHARACTER SET utf8mb4"

# 导入建表脚本
mysql -u root -p intelligent_picture < intelligent-picture-backend/sql/create_table.sql
```

#### 2. 启动后端

```bash
cd intelligent-picture-backend

# 修改 application-dev.yml 中的数据库和 Redis 连接信息

mvn package -DskipTests
java -jar target/intelligent-picture-backend-*.jar --spring.profiles.active=dev
```

或通过 Maven 直接运行：

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

#### 3. 启动前端

```bash
cd intelligent-picture-frontend
npm install
npm run dev
```

前端默认运行在 `http://localhost:5173`，API 请求通过 Vite Proxy 转发到 `http://localhost:8088`。

#### 4. 访问

- 前端页面：`http://localhost:5173`
- 接口文档：`http://localhost:8088/api/doc.html`

### Docker 部署

项目已包含 Docker 部署文件，位于 `intelligent-picture-backend/docker/` 目录下。

**前置条件：**

```bash
# 1. 先构建前端
cd intelligent-picture-frontend
npm install
npm run build
cd ..

# 2. 复制环境变量文件并填入真实值
cp intelligent-picture-backend/docker/.env.example intelligent-picture-backend/docker/.env
# 编辑 .env 填写 COS 等配置
```

**启动所有服务：**

```bash
cd intelligent-picture-backend/docker
docker-compose up -d
```

首次启动会自动构建后端镜像、初始化数据库表。

> 生产环境需在 `intelligent-picture-backend/docker/.env` 文件中配置以下环境变量：
> - `COS_HOST`、`COS_SECRET_ID`、`COS_SECRET_KEY`、`COS_REGION`、`COS_BUCKET`
> - `ALIYUN_AI_API_KEY`（如使用 AI 功能）

---

## 项目结构

```
intelligent-picture-project/
├── intelligent-picture-backend/        # Spring Boot 后端
│   ├── src/
│   │   └── main/
│   │       ├── java/com/picture/backend/
│   │       │   ├── api/                # 第三方 API 封装（阿里云、图搜）
│   │       │   ├── auth/               # Sa-Token 鉴权配置
│   │       │   ├── common/             # 通用返回 / 异常
│   │       │   ├── config/             # COS / CORS 等配置
│   │       │   ├── controller/         # REST 控制器
│   │       │   ├── exception/          # 全局异常处理
│   │       │   ├── manager/            # COS / 文件上传管理
│   │       │   ├── model/              # 实体 / VO / DTO
│   │       │   ├── service/            # 业务逻辑层
│   │       │   └── utils/              # 工具类
│   │       └── resources/
│   │           ├── application.yml     # 主配置
│   │           ├── application-dev.yml # 开发环境配置
│   │           ├── application-pro.yml # 生产环境配置
│   │           └── mapper/             # MyBatis XML
│   ├── sql/                            # 数据库脚本
│   ├── docker/                         # Docker 部署文件
│   │   ├── docker-compose.yml          #   容器编排
│   │   ├── Dockerfile                  #   后端镜像构建
│   │   ├── nginx.conf                  #   Nginx 反向代理配置
│   │   └── .env.example                #   环境变量模板
│   └── pom.xml
│
├── intelligent-picture-frontend/       # Vue 3 前端
│   ├── src/
│   │   ├── api/                        # API 接口 & 类型定义
│   │   ├── components/                 # 公共组件
│   │   │   ├── analyze/                # 空间分析图表组件
│   │   │   └── icons/                  # SVG 图标
│   │   ├── constants/                  # 常量定义
│   │   ├── layouts/                    # 布局组件
│   │   ├── pages/                      # 页面
│   │   │   ├── admin/                  # 管理后台页面
│   │   │   └── user/                   # 用户相关页面
│   │   ├── stores/                     # Pinia 状态管理
│   │   ├── styles/                     # 全局样式
│   │   └── utils/                      # 工具函数
│   ├── index.html
│   ├── vite.config.ts
│   └── package.json
│
├── intelligent_picture.sql             # 数据库初始化脚本（完整）
└── README.md                           # 本文件
```

---

## API 文档

启动后端后访问 Knife4j 接口文档：

```
http://localhost:8088/api/doc.html
```

主要接口分组：

| 分组 | 路径前缀 | 说明 |
|------|----------|------|
| 用户 | `/api/user` | 登录 / 注册 / 个人信息 |
| 图片 | `/api/picture` | 上传 / 查询 / 删除 / 审核 |
| 空间 | `/api/space` | 创建 / 查询 / 分析 |
| 空间用户 | `/api/spaceUser` | 团队成员管理 |
| 空间分析 | `/api/spaceAnalyze` | 用量 / 分类 / 标签分析 |
| 文件 | `/api/file` | 文件上传 |
| 主控 | `/api/main` | 健康检查 |

---

## 许可证

[MIT](LICENSE)

---

*此项目仅供学习交流使用*