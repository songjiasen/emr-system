# 剩余功能设计方案总览

## 需求来源

本设计总览严格对照两份原始文档：

- `需求分析文档.pdf`: 第 4 章功能性需求，覆盖 `FR-MR`, `FR-ORDER`, `FR-IP`, `FR-PRES`, `FR-TEST`, `FR-FEE`, `FR-TPL`, `FR-ARCHIVE`, `FR-AI`, `FR-WF` 等模块。
- `概要设计文档.pdf`: 第 4 章模块与数据库表映射、第 5 章后端结构、第 6 章前端结构，要求使用 Spring Boot 3、MyBatis-Plus、MySQL 5.7、Vue 3 前后端分离结构。

当前仓库已经按微服务拆分，不再采用概要设计中的单体 `server_code` 目录。表名、业务字段、接口能力继续沿用概要设计文档口径；服务边界按当前 `backend/emr-*-service` 结构落地。

## 当前完成状态

这些能力已经按设计方案落到微服务代码、数据库初始化脚本、接口文档和前后端联调入口，并完成后端全量测试、前端构建和网关冒烟验证：

| 模块 | 当前状态 | 主要文件 |
| --- | --- | --- |
| 网关鉴权与审计 | 已接 Token 校验、角色白名单、用户头透传、写操作审计 | `backend/emr-gateway` |
| 认证权限 | 已接 `emr_auth.token` 和 `emr_user` 账号表，密码 BCrypt 加密，Token 可过期/注销 | `backend/emr-auth-service` |
| 用户与科室 | 管理员、医生、患者、科室已接数据库，医生默认演示数据兼容旧入口 | `backend/emr-user-service` |
| 预约挂号 | 创建、列表、详情、取消已接 `emr_visit.yuyueguahao`，并补齐患者/医生数据范围校验 | `backend/emr-visit-service` |
| 系统内容 | 资讯、留言、轮播、配置、菜单、操作日志已接 `emr_system` 数据库 | `backend/emr-system-service` |
| 前端基础会话 | 患者端和后台端已有 Token 保存、恢复、401/403 处理 | `frontend/emr-frontend`, `frontend/manage_code` |

## 设计文件拆分

| 设计文件 | 覆盖需求 | 目标服务 |
| --- | --- | --- |
| `2026-06-22-visit-inpatient-triage-design.md` | `FR-IP`, `FR-WF-01` | `emr-visit-service` |
| `2026-06-22-record-template-archive-design.md` | `FR-MR`, `FR-TPL`, `FR-ARCHIVE` | `emr-record-service` |
| `2026-06-22-clinical-order-prescription-test-design.md` | `FR-ORDER`, `FR-PRES`, `FR-TEST` | `emr-clinical-service` |
| `2026-06-22-workflow-audit-design.md` | `FR-WF-02`, `FR-WF-03`, `FR-WF-04` | `emr-workflow-service` |
| `2026-06-22-billing-payment-design.md` | `FR-FEE` | `emr-billing-service` |
| `2026-06-22-ai-service-design.md` | `FR-AI` | `emr-ai-service` |
| `2026-06-22-frontend-integration-design.md` | 两端页面联调、演示闭环 | `frontend/emr-frontend`, `frontend/manage_code` |

## 已执行实现顺序

1. 先做 `emr-record-service`: 病历是医嘱、处方、检查、归档、AI 检索的上游核心数据。
2. 再做 `emr-clinical-service`: 医嘱、处方、检查是医生诊疗主流程。
3. 接着做 `emr-workflow-service`: 用统一审核任务承接医嘱审核、检查审核、病历归档审核。
4. 然后做 `emr-visit-service` 剩余分诊/入院/出院: 它和病历主链路关联，但不阻塞病历录入演示。
5. 再做 `emr-billing-service`: 费用可由诊疗项目手工创建或后续扩展自动生成。
6. 再做 `emr-ai-service`: 保留 mock provider 兜底，补调用日志和智能检索真实数据来源。
7. 最后做前端联调和接口文档刷新，确保患者端与后台端能完整演示。

## 通用设计约束

- 所有剩余后端服务统一使用 MyBatis-Plus、MySQL 5.7、H2 测试库，沿用已完成服务的实体/Mapper/Service/Controller 分层。
- 不新增 Laravel migration 或其他迁移文件；数据库结构继续以 `deploy/mysql/init/*.sql` 为准。
- 服务之间不建跨库外键，通过业务 ID、业务编号和名称快照关联。
- 增删改操作继续走网关审计，服务内部保留必要业务校验和状态流控制。
- 患者只能查看自己的病历、预约、费用；医生只能维护自己接诊相关数据；主任/管理员处理审核和系统管理。
- 删除操作优先软删除或状态变更，只有现有表缺少状态字段时才在方案中明确兼容策略。
