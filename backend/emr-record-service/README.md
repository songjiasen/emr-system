# emr-record-service

病历服务，默认端口 `8104`。

## 服务职责

- 医生新增、编辑、删除、查询病历。
- 患者查看自己的病历。
- 病历编号生成。
- 既往史记录、病历附件、病历模板。
- 病历归档申请和归档记录管理。

## 对应需求

- `FR-MR`: 病历管理。
- `FR-TPL`: 病历模板。
- `FR-ARCHIVE`: 病历归档。

## 主要数据

- `binglixinxi`
- `binglimoban`
- `bingliguidang`
- `bingliguidangshenqing`

## 当前实现说明

- 病历主表、病历模板、归档申请、归档记录已使用 MySQL/H2 持久化。
- `binglixinxi` 额外补充了 `appointment_id`、`appointment_no` 两个字段，用于把预约挂号和病历录入主流程串起来。
- 医生通过网关新增、编辑、删除病历时只能操作自己名下的病历；患者只能查看自己的病历。
- 护士、主任、管理员按网关角色权限访问病历查询和归档相关接口；服务侧会基于可信身份头再次校验归属。
- 新建病历默认 `archiveStatus=not_submitted`，归档申请提交后会更新为 `pending`，审核通过后更新为 `archived`。
- 模板删除为软删除，写 `status=0`；病历删除仍是物理删除，但只允许未进入归档流程的病历删除。
