# emr-billing-service

费用服务，默认端口 `8107`。

## 服务职责

- 费用记录列表、详情。
- 新增、修改、删除费用。
- 患者查看自己的费用。
- 患者支付费用并更新支付状态。

## 对应需求

- `FR-FEE`: 费用管理。

## 主要数据

- `feiyong`

## 当前实现说明

- 费用记录和支付状态已使用 MySQL/H2 持久化。
- 患者通过网关查询费用、费用详情和支付费用时只能操作自己的费用记录。
- 支持按 `patientId`、`payStatus`、`businessType`、`businessId` 过滤费用列表，兼容患者端和后台管理查询。
- `businessType/businessId` 同时兼容旧前端的 `relatedBusinessType/relatedBusinessId` 字段。
- 支付仅支持 `unpaid -> paid`，会同步写入 `payTime`；已支付或已退费费用不允许修改或删除。
