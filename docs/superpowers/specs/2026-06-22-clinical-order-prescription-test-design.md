# 医嘱、处方与检查项目设计方案

## 需求来源

- `需求分析文档.pdf`: `FR-ORDER` 要求医嘱列表、录入、编辑、删除、详情、审核、执行和执行记录；`FR-PRES` 要求处方列表、开具、详情、修改、删除；`FR-TEST` 要求检查申请列表、新增、详情、审核、修改、删除。
- `概要设计文档.pdf`: 诊疗模块映射 `yizhuxinxi`, `yizhuzhixingjilu`, `kaifang`, `jianchaxiang`。

## 当前状态

`emr-clinical-service` 的 `ClinicalController` 使用内存 Map 保存医嘱、执行记录、处方、检查申请。接口路径和基础字段已经存在，但没有数据库持久化，也没有严格状态约束。

## 设计目标

把诊疗主流程拆为数据库实体和 Service，保留现有接口路径。医生录入医嘱、处方、检查申请；主任审核医嘱和检查；护士执行已审核通过的医嘱；所有列表支持分页和条件查询。

## 数据表与实体

| 表 | 实体 | 说明 |
| --- | --- | --- |
| `yizhuxinxi` | `MedicalOrderEntity` | 医嘱信息 |
| `yizhuzhixingjilu` | `MedicalOrderExecutionEntity` | 医嘱执行记录 |
| `kaifang` | `PrescriptionEntity` | 开药处方 |
| `jianchaxiang` | `TestRequestEntity` | 检查申请 |

编号规则：

- 医嘱编号: `ORDERyyyyMMdd` 加短随机后缀。
- 处方编号: `PRESyyyyMMdd` 加短随机后缀。
- 检查申请编号: `TESTyyyyMMdd` 加短随机后缀。

## 医嘱设计

接口：

- `POST /medical-orders`
- `GET /medical-orders`
- `GET /medical-orders/{id}`
- `PUT /medical-orders/{id}`
- `DELETE /medical-orders/{id}`
- `POST /medical-orders/{id}/audit-result`
- `POST /medical-orders/{id}/execute`
- `GET /medical-orders/{id}/executions`

状态流：

- `pending_audit`: 医生新建后待审核。
- `approved`: 主任审核通过。
- `rejected`: 主任审核驳回。
- `executed`: 护士已执行。

关键规则：

- 医嘱内容 `content` 必填。
- `pending_audit` 和 `rejected` 可编辑；`approved` 可编辑但编辑后回到 `pending_audit`；`executed` 禁止编辑。
- 审核结果只允许 `approved` 或 `rejected`。
- 执行医嘱前必须是 `approved` 状态，执行后新增 `yizhuzhixingjilu` 并更新医嘱状态为 `executed`。
- 删除医嘱时，若已执行则禁止删除；未执行可物理删除，因为表没有状态字段表示删除。

## 处方设计

接口：

- `POST /prescriptions`
- `GET /prescriptions`
- `GET /prescriptions/{id}`
- `PUT /prescriptions/{id}`
- `DELETE /prescriptions/{id}`

状态流：

- `created`: 已开具。
- `cancelled`: 已取消。

关键规则：

- `patientId`, `patientName`, `doctorId`, `doctorName`, `medicineName` 必填。
- `recordId` 可选，用于关联病历。
- 删除接口不物理删除，设置 `status=cancelled`。
- 列表支持 `patientId`, `doctorId`, `status`, `recordId`。

## 检查申请设计

接口：

- `POST /test-requests`
- `GET /test-requests`
- `GET /test-requests/{id}`
- `PUT /test-requests/{id}`
- `DELETE /test-requests/{id}`
- `POST /test-requests/{id}/audit-result`

状态流：

- `pending_audit`: 待审核。
- `approved`: 审核通过。
- `rejected`: 审核驳回。
- `finished`: 检查完成并有结果。

关键规则：

- `testItem` 必填。
- 审核结果只允许 `approved` 或 `rejected`。
- 可在 `PUT /test-requests/{id}` 中录入 `resultContent`；若当前状态为 `approved` 且传入结果内容，则状态可更新为 `finished`。
- 已 `finished` 的检查申请禁止删除。

## 与其他服务的边界

- 病历关联通过 `recordId`，不跨库外键。
- 医嘱和检查审核可先在本服务内完成；接入工作流后由 `emr-workflow-service` 创建审核任务，审核完成后调用本服务的 `audit-result` 回写接口。
- 费用服务后续可按处方、检查、住院等业务生成费用；本服务暂不直接写 `feiyong`，避免服务间强耦合。

## 兼容性

- 保留所有现有接口路径。
- 返回字段继续使用驼峰命名。
- 删除策略按表结构区分：处方软取消，医嘱/检查在未进入关键状态前允许物理删除。
- 不新增数据库表，不修改现有初始化 SQL。

## 验证方式

- 新增 `ClinicalPersistenceTest`: 覆盖医嘱创建落库、审核状态更新、执行记录落库、处方取消、检查申请审核。
- 运行 `mvn -q -pl emr-clinical-service -am test`。
- 手工演示路径：医生开医嘱，主任审核，护士执行；医生开处方；医生开检查申请，主任审核并录入结果。

