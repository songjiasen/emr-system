# AI 智能服务设计方案

## 需求来源

- `需求分析文档.pdf`: `FR-AI-01` 到 `FR-AI-04` 要求 AI 病历识别、智能荐药、处方审核、智能检索。
- `概要设计文档.pdf`: 技术栈提到百度 AI SDK，模块映射包括 AI OCR、NLP 能力；当前微服务中单独拆为 `emr-ai-service`。

## 当前状态

`emr-ai-service` 当前使用 mock provider 返回固定内容，保证演示不依赖外部 Key。`emr_ai.ai_request_log` 表已经存在，但当前服务没有写调用日志，也没有查询真实病历数据。

## 设计目标

保留 mock provider 作为默认兜底，同时新增 AI 调用日志持久化。智能检索优先接入 `emr-record-service` 的病历数据；OCR、荐药、处方审核在没有真实 AI Key 时继续返回可演示结果，但要记录输入摘要、输出摘要、状态和错误。

## 数据表与实体

| 表 | 实体 | 说明 |
| --- | --- | --- |
| `ai_request_log` | `AiRequestLogEntity` | AI 调用记录 |

调用类型：

- `ocr`
- `recommend_medicine`
- `prescription_audit`
- `smart_search`

Provider 策略：

- 默认 `mock`，不依赖外部网络。
- 可配置 `baidu`，后续接真实 SDK。

## 接口设计

`POST /ai/ocr`

输入字段：`fileUrl`, `imageBase64`, `userId`, `username`。至少要有 `fileUrl` 或 `imageBase64`。mock 模式返回识别文本和结构化字段；真实模式调用 OCR SDK。

`POST /ai/recommend-medicine`

输入字段：`diagnosis`, `pastHistory`, `allergyHistory`, `patientId`, `userId`, `username`。mock 模式根据诊断关键字返回建议，不做医疗承诺。

`POST /ai/prescription-audit`

输入字段：`prescriptionText`, `medicines`, `diagnosis`, `patientId`。返回 `passed`, `warnings`, `suggestions`。mock 模式对空处方、重复药品、禁忌关键词做基础规则判断。

`POST /ai/smart-search`

输入字段：`keyword`, `patientId`, `doctorId`, `userId`, `username`。第一阶段不做向量检索，使用病历服务或本服务配置的记录 API 做关键词搜索，返回 `recordNo`, `patientName`, `diagnosis`, `summary`。

## 调用日志设计

每次 AI 调用都写 `ai_request_log`：

- 成功时 `status=success`，写 `inputSummary`, `resultSummary`。
- 失败时 `status=failed`，写 `errorMessage`，接口返回业务错误。
- `requestNo` 使用 `AIyyyyMMdd` 加短随机后缀。

输入摘要不能保存完整身份证号、手机号、长篇病历正文。只保存诊断、关键词、文件名或前 200 字摘要。

## 与病历服务的边界

智能检索不直接跨库查 `emr_record`。设计为通过 `EMR_RECORD_BASE_URL` 调用 `GET /medical-records` 或后续专门检索接口。若记录服务不可用，返回空结果并记录失败日志，不让 AI 服务启动失败。

## 安全与合规提示

- AI 结果只能作为辅助建议，前端展示时应标注由医生最终确认。
- 不在 AI 日志中保存完整隐私数据。
- 所有 AI 接口继续走网关 Token 鉴权。

## 兼容性

- 接口路径不变。
- 返回仍包含 `provider` 字段。
- 默认 mock provider 保持可演示，不需要外部 Key。
- 新增日志写库不影响原有前端调用。

## 验证方式

- 新增 `AiRequestLogPersistenceTest`: 四类接口调用后均写 `ai_request_log`。
- 新增 mock 规则测试：空处方审核失败、普通处方通过、智能检索能返回病历服务结果。
- 运行 `mvn -q -pl emr-ai-service -am test`。

