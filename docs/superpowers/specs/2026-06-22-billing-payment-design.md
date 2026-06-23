# 费用管理与支付设计方案

## 需求来源

- `需求分析文档.pdf`: `FR-FEE-01` 到 `FR-FEE-07` 要求费用列表、新增费用、详情、支付、修改、删除和患者查看费用。
- `概要设计文档.pdf`: 费用管理映射 `feiyong`，属于独立费用服务。

## 当前状态

`emr-billing-service` 的费用记录使用内存 Map 保存。接口已经覆盖新增、列表、详情、编辑、支付、删除，但服务重启后数据丢失，支付时间也没有持久记录。

## 设计目标

把费用数据接入 `emr_billing.feiyong`，支持患者、管理员和医生相关页面查看费用。支付为模拟支付，不接第三方支付平台；支付动作只更新 `payStatus` 和 `payTime`。

## 数据表与实体

| 表 | 实体 | 说明 |
| --- | --- | --- |
| `feiyong` | `FeeEntity` | 费用记录 |

字段映射：

- `feeNo`: 费用编号。
- `patientId`, `patientName`: 患者快照。
- `businessType`, `businessId`: 费用来源，如 `prescription`, `test_request`, `admission`, `manual`。
- `feeItem`: 费用项目。
- `amount`: 金额，使用 `BigDecimal`。
- `payStatus`: `unpaid`, `paid`, `refunded`。
- `payTime`: 支付时间。
- `remark`: 备注。

## 接口设计

`POST /fees`

必填字段：`patientId`, `patientName`, `feeItem`, `amount`。`businessType` 默认为 `manual`。为兼容旧前端，请求字段 `relatedBusinessType` 和 `relatedBusinessId` 继续映射到 `businessType`, `businessId`。

金额规则：

- `amount` 不能为负数。
- `amount=0` 允许用于演示免费项目，但仍可创建。

`GET /fees`

支持过滤：`patientId`, `payStatus`, `businessType`, `businessId`。患者端必须结合网关身份限制只能查询本人费用。

`GET /fees/{id}`

返回单条费用。

`PUT /fees/{id}`

只有 `unpaid` 状态允许修改金额、项目、备注和业务来源。`paid` 或 `refunded` 状态禁止修改金额，避免支付后账目变化。

`POST /fees/{id}/pay`

只允许 `unpaid` 状态支付。支付后：

- `payStatus=paid`
- `payTime=now`
- 返回兼容字段 `status=paid`

`DELETE /fees/{id}`

`unpaid` 可物理删除。`paid` 不允许删除；如需退费，后续扩展 `refund` 接口。当前表有 `refunded` 状态，但不先实现退费，避免超出期末作业核心范围。

## 自动生成费用的边界

第一阶段只支持手工新增费用。第二阶段可以由处方、检查、住院流程调用费用服务创建费用：

- 处方开具后创建药品费用。
- 检查申请审核通过后创建检查费用。
- 入院登记后创建住院押金或床位费用。

这类自动费用不在本轮必须实现范围内，但 `businessType/businessId` 已为后续预留。

## 兼容性

- 接口路径不变。
- 继续返回 `payStatus` 和 `status` 两个字段，兼容前端可能读取任一字段。
- 数据库表已经存在，不需要新增 SQL。
- 金额类型从任意对象收敛为 `BigDecimal`，前端传数字或数字字符串均可兼容。

## 验证方式

- 新增 `BillingPersistenceTest`: 新增费用落库、列表读取数据库、支付更新 `pay_status/pay_time`。
- 验证已支付费用不可修改金额、不可删除。
- 运行 `mvn -q -pl emr-billing-service -am test`。

