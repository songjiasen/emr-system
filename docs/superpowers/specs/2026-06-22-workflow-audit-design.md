# 工作流审核设计方案

## 需求来源

- `需求分析文档.pdf`: `FR-WF-02` 病历管理工作流、`FR-WF-03` 医嘱审核工作流、`FR-WF-04` 医嘱执行工作流。
- `概要设计文档.pdf`: 工作流模块包含分诊建档、病历管理工作流、医嘱审核、医嘱执行工作流。数据库映射使用 `shenherenwu`, `shenhejilu`。

## 当前状态

`emr-workflow-service` 已有任务创建、任务列表、任务审核、审核记录列表接口，但全部保存在内存 Map 中。当前服务没有回写业务服务状态，适合作为演示任务中心，但还不能支撑跨服务审核闭环。

## 设计目标

把审核任务和审核记录持久化到 `emr_workflow` 数据库，作为主任和管理员处理审核事项的统一入口。业务服务仍保留自己的状态字段，工作流服务负责记录审核过程，并通过轻量回写机制同步业务状态。

## 数据表与实体

| 表 | 实体 | 说明 |
| --- | --- | --- |
| `shenherenwu` | `WorkflowTaskEntity` | 审核任务 |
| `shenhejilu` | `WorkflowAuditRecordEntity` | 审核记录 |

支持业务类型：

- `medical_order`: 医嘱审核，对应 `emr_clinical.yizhuxinxi`。
- `test_request`: 检查申请审核，对应 `emr_clinical.jianchaxiang`。
- `record_archive`: 病历归档审核，对应 `emr_record.bingliguidangshenqing`。
- `medical_record`: 病历质量审核，对应 `emr_record.binglixinxi`，作为扩展能力。

## 接口设计

`POST /workflow/tasks`

必填字段：`businessType`, `businessId`, `applicantId`, `applicantName`, `assigneeRole`。可选字段：`businessNo`。

创建规则：

- 同一 `businessType + businessId` 若存在 `pending` 任务，不允许重复创建。
- `assigneeRole` 只允许 `director` 或 `admin`。
- 新任务默认 `status=pending`。

`GET /workflow/tasks`

支持过滤：`businessType`, `businessId`, `status`, `assigneeRole`, `applicantId`。

`POST /workflow/tasks/{id}/audit`

必填字段：`auditorId`, `auditorName`, `auditResult`。`auditResult` 只允许 `approved` 或 `rejected`。

审核规则：

- 只能审核 `pending` 任务。
- 审核后更新任务状态。
- 新增 `shenhejilu`。
- 根据 `businessType` 调用对应业务服务回写接口。

`GET /workflow/audit-records`

支持过滤：`taskId`, `auditorId`, `auditResult`。

## 业务回写设计

为了控制期末作业复杂度，回写采用服务内部 HTTP 客户端方式，使用网关内网地址或服务配置地址：

- `medical_order`: `POST /medical-orders/{id}/audit-result`
- `test_request`: `POST /test-requests/{id}/audit-result`
- `record_archive`: `POST /medical-record-archives/applications/{id}/audit`

配置项：

- `EMR_CLINICAL_BASE_URL`
- `EMR_RECORD_BASE_URL`

如果回写失败，审核任务不应静默成功。设计上先写审核记录和任务状态前进行回写；回写成功后再提交数据库事务。若跨服务事务做不到，则记录失败并返回 `502`，由用户重试。

## 与医嘱执行的关系

医嘱执行不由工作流服务直接完成。工作流只负责让医嘱从 `pending_audit` 变成 `approved` 或 `rejected`；护士执行仍调用 `emr-clinical-service` 的 `/medical-orders/{id}/execute`。

## 兼容性

- 保留现有接口路径。
- 现有内存字段 `lastAuditRecordId` 可继续返回，来自最新审核记录 ID。
- 对于还未接入回写的业务类型，允许只记录工作流任务，但设计中必须显式标记为扩展业务类型。
- 数据库表已经存在，不需要新增 SQL。

## 验证方式

- 新增 `WorkflowPersistenceTest`: 创建任务落库、重复 pending 任务拦截、审核任务生成审核记录。
- 使用 MockWebServer 或测试替身验证业务回写请求。
- 运行 `mvn -q -pl emr-workflow-service -am test`。

