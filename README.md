# 安心医疗系统

本项目用于期末作业，按照两份需求与概要设计文档规划为前后端分离的微服务项目。当前仓库采用 monorepo 结构，前端、后端、部署脚本和项目文档统一放在当前目录中。

## 目录说明

- `docs/`: 项目文档、接口文档、数据库说明。
- `backend/`: Java 17 + Spring Boot 3.3 微服务后端。
- `frontend/`: Vue 3 前端项目，包含患者端和后台管理端。
- `deploy/`: Docker Compose、Nginx、MySQL 初始化脚本、Nacos 配置。
- `scripts/`: 本地启动、构建脚本。
- `storage/`: 本地上传文件目录。

## 后端服务

- `emr-common`: 公共模块，放统一返回、异常、常量和工具类。
- `emr-gateway`: API 网关，负责统一入口、跨域、路由和鉴权前置处理。
- `emr-auth-service`: 认证权限，负责登录、注册、Token、角色权限。
- `emr-user-service`: 用户与科室，负责管理员、医生、患者、护士、主任和科室。
- `emr-visit-service`: 就诊流程，负责预约挂号、分诊、入院、出院、病房。
- `emr-record-service`: 病历，负责病历信息、既往史、模板、附件、归档。
- `emr-clinical-service`: 诊疗，负责医嘱、处方、检查申请、执行记录。
- `emr-workflow-service`: 工作流，负责医嘱审核、病历审核、归档审核。
- `emr-billing-service`: 费用，负责费用记录、支付状态、患者费用查询。
- `emr-system-service`: 系统，负责资讯、留言、菜单、轮播图、配置、日志。
- `emr-ai-service`: AI，负责 OCR、荐药、处方审核、智能检索。

## 前端项目

- `frontend/emr-frontend`: 患者端。
- `frontend/manage_code`: 后台管理端。

## 当前可演示功能

当前实现已经覆盖期末作业答辩主流程，核心业务服务为数据库持久化版本，AI 能力按设计保留 `mock provider` 兜底：

- 患者端：登录注册、预约挂号、查看病历、查看/支付费用、查看处方检查、资讯留言、AI 智能检索与荐药。
- 管理端：后台登录、科室维护、多角色用户维护、病历录入、医嘱/处方/检查管理、审核任务、病历归档、费用录入、资讯配置、留言回复、AI OCR/荐药/审方。
- 网关：保留前端历史前缀 `/cl584734139`，转发到认证、用户、就诊、病历、诊疗、工作流、费用、系统、AI 九个微服务；受保护接口使用 `Token` 请求头，网关校验后向下游注入可信身份头。
- 数据库：`deploy/mysql/init/` 提供 MySQL 5.7 初始化 SQL，各核心业务服务默认连接对应 MySQL 库；测试场景使用 H2 隔离库。
- 权限边界：患者只能访问自己的预约、病历、处方检查和费用；医生只能维护自己接诊相关数据；护士负责分诊、入院和医嘱执行；主任/管理员处理审核和系统管理。

## 本地运行

前置环境：

- Java 17
- Maven
- Node.js 18 或更高版本
- MySQL 5.7，如需要验证初始化 SQL

安装前端依赖：

```bash
cd frontend/emr-frontend && npm install
cd ../manage_code && npm install
```

启动后端（本机 Java + Maven）：

```bash
scripts/start-backend.sh
```

本机启动会连接 `localhost:3306` 的 MySQL，默认账号密码按各服务 `application.yml` 配置读取。若在受限沙箱或容器环境运行，需要确保进程有本机数据库访问权限。

如果本机没有安装 Java/Maven，也可以直接用 Docker Compose 拉起整套微服务：

```bash
scripts/start-backend-docker.sh
```

启动前端：

```bash
scripts/start-frontend.sh
```

macOS 一键检测并启动：

```bash
chmod +x scripts/start-mac.sh
scripts/start-mac.sh
```

如果 macOS 本机 MySQL 密码不是默认的 `root`：

```bash
scripts/start-mac.sh --mysql-user root --mysql-password 你的密码
```

详细说明见 `scripts/README-mac.md`。

Windows 一键检测并启动：

```powershell
.\scripts\start-windows.bat
```

如果 Windows 本机 MySQL 密码不是默认的 `root`：

```powershell
.\scripts\start-windows.bat -MySqlUser root -MySqlPassword 你的密码
```

详细说明见 `scripts/README-windows.md`。

访问地址：

- 患者端：`http://localhost:5173`
- 后台端：`http://localhost:5174`
- 网关：`http://localhost:8080`

停止 Docker 微服务栈：

```bash
scripts/stop-backend-docker.sh
```

## 检查命令

当前机器如果还没有 Java/Maven 或前端依赖，可以先执行静态检查：

```bash
scripts/verify-static.sh
```

完整构建命令：

```bash
scripts/build-all.sh
```

如果提示缺少 Maven 或 `node_modules`，先安装对应环境或依赖后再执行。
