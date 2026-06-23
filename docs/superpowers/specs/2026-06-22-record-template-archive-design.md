# 病历、模板与归档设计方案

## 需求来源

- `需求分析文档.pdf`: `FR-MR-01` 到 `FR-MR-07` 要求病历列表、新增、编辑、删除、详情、编号和附件；`FR-TPL-01` 到 `FR-TPL-06` 要求模板管理和使用模板；`FR-ARCHIVE-01` 到 `FR-ARCHIVE-05` 要求归档申请、审核、归档管理和状态跟踪。
- `概要设计文档.pdf`: 病历管理映射 `binglixinxi`，模板映射 `binglimoban`，归档映射 `bingliguidangshenqing`, `bingliguidang`。

## 当前状态

`emr-record-service` 的病历、模板、归档都还是内存实现。病历服务已有 `MedicalRecordService` 接口和 DTO/VO，模板与归档逻辑集中在 `TemplateArchiveController`。当前接口路径已经覆盖基本能力，但数据重启丢失。

## 设计目标

把病历主数据、模板、归档申请、归档记录全部接入 `emr_record` 数据库。病历是后续医嘱、处方、检查、费用和 AI 检索的核心业务对象，因此优先保障病历编号、患者/医生快照、归档状态和附件地址稳定。

## 数据表与实体

| 表 | 实体 | 说明 |
| --- | --- | --- |
| `binglixinxi` | `MedicalRecordEntity` | 病历主表 |
| `binglimoban` | `MedicalRecordTemplateEntity` | 病历模板 |
| `bingliguidangshenqing` | `ArchiveApplicationEntity` | 归档申请 |
| `bingliguidang` | `MedicalRecordArchiveEntity` | 归档快照 |

病历编号规则保持 `MRyyyyMMdd` 前缀。归档申请编号保持 `ARCHAPPyyyyMMdd` 前缀。归档编号保持 `ARCHyyyyMMdd` 前缀。

## 病历接口设计

`POST /medical-records`

必填字段：`patientId`, `patientName`, `doctorId`, `doctorName`, `visitTime`。可选字段：`appointmentId`, `appointmentNo`, `chiefComplaint`, `presentIllness`, `pastHistory`, `diagnosis`, `treatmentAdvice`, `fileUrl`。

创建后：

- 写入 `binglixinxi`。
- `archiveStatus` 默认 `not_submitted`。
- 返回现有 `MedicalRecordResponse` 结构。

`GET /medical-records`

支持 `patientId`, `doctorId`, `archiveStatus`, `page`, `limit`。患者端查询必须结合网关透传身份限制只能看本人数据；医生端可看本人创建或授权范围内数据。

`PUT /medical-records/{id}`

允许更新病历正文类字段。若 `archiveStatus=archived`，默认禁止修改；如课程演示需要修改，则必须先回退归档状态，这里先不支持。

`DELETE /medical-records/{id}`

当前表没有 `status` 字段。为了不改 SQL，数据库版删除采用物理删除，但如果该病历已存在归档申请或已归档，返回 `400` 禁止删除。

`POST /medical-records/upload`

继续保留演示上传能力。后续只保存 `fileUrl` 字段，不把文件二进制写入数据库。

## 模板接口设计

模板接口保持现有路径：

- `POST /medical-record-templates`
- `GET /medical-record-templates`
- `GET /medical-record-templates/{id}`
- `PUT /medical-record-templates/{id}`
- `DELETE /medical-record-templates/{id}`

删除模板时设置 `status=0`，不物理删除。列表默认返回全部模板，建议新增 `status` 可选过滤；前端使用模板时只取 `status=1`。

使用模板不单独新增后端接口。前端读取模板详情后，把 `content` 填入病历录入表单中的 `presentIllness`, `diagnosis` 或 `treatmentAdvice`，再调用新增病历接口。

## 归档接口设计

`POST /medical-record-archives/applications`

归档申请必须基于已存在病历。服务端从 `binglixinxi` 回表补齐 `recordNo`, `patientId`, `patientName`, `doctorId`, `doctorName`，不能完全信任前端传来的快照字段。创建后：

- 写入 `bingliguidangshenqing.status=pending`。
- 更新 `binglixinxi.archive_status=pending`。

`GET /medical-record-archives/applications`

支持 `status`, `recordId`, `patientId`, `doctorId` 过滤。

`POST /medical-record-archives/applications/{id}/audit`

审核结果只允许 `approved` 或 `rejected`。通过时：

- 更新申请状态为 `approved`。
- 写入 `bingliguidang`，`archiveContent` 保存病历快照。
- 更新病历 `archive_status=archived`。

拒绝时：

- 更新申请状态为 `rejected`。
- 更新病历 `archive_status=rejected`。

`GET /medical-record-archives`

支持 `patientId`, `recordId` 过滤。

## 与工作流服务的边界

本服务可以独立完成归档申请和审核，保证期末演示闭环。后续若接入 `emr-workflow-service`，归档申请创建时同步创建 `businessType=record_archive` 的审核任务；工作流审核通过后回调本服务审核接口。

## 兼容性

- 接口路径和已有返回字段保持不变。
- 数据库表已经存在，不需要新增 SQL。
- 病历删除从“内存移除”变为“数据库物理删除”，但已归档或待审核病历会被保护，防止破坏归档链路。
- 已有前端 API 文件可继续使用，只需要补页面状态提示。

## 验证方式

- 新增 `MedicalRecordPersistenceTest`: 创建病历后查 `binglixinxi`，列表能读出直接插入数据库的数据。
- 新增 `TemplateArchivePersistenceTest`: 模板软删除、归档申请更新病历状态、审核通过生成归档记录。
- 运行 `mvn -q -pl emr-record-service -am test`。

