# 分诊建档与住院管理设计方案

## 需求来源

- `需求分析文档.pdf`: `FR-IP-01` 到 `FR-IP-05` 要求入院登记、入院列表、出院办理、出院列表、病房分配；`FR-WF-01` 要求护士分诊建档。
- `概要设计文档.pdf`: 模块映射到 `fenzhenjiandang`, `ruyuanxinxi`, `chuyuanxinxi`，属于就诊流程和工作流模块。

## 当前状态

`emr-visit-service` 中预约挂号已接数据库；`InpatientController` 仍使用 `ConcurrentHashMap` 保存分诊、入院、出院数据，服务重启会丢失。当前接口路径已可用：

- `POST /triage-records`
- `GET /triage-records`
- `POST /inpatients/admissions`
- `GET /inpatients/admissions`
- `POST /inpatients/admissions/{id}/discharge`
- `GET /inpatients/discharges`

## 设计目标

把分诊、入院、出院从内存数据改为 `emr_visit` 数据库持久化，并保持现有接口字段和返回结构不变。分诊由护士创建，入院由护士或管理员办理，出院必须基于已有入院记录并同步把入院状态改为 `discharged`。

## 数据表与实体

| 表 | 实体 | 说明 |
| --- | --- | --- |
| `fenzhenjiandang` | `TriageRecordEntity` | 分诊建档，记录患者、护士、主诉、分诊级别、状态 |
| `ruyuanxinxi` | `AdmissionEntity` | 入院信息，记录患者、医生、护士、病房、床位、入院时间、状态 |
| `chuyuanxinxi` | `DischargeEntity` | 出院信息，记录入院记录、出院时间、原因、小结、办理人 |

编号规则：

- 分诊编号: `TRIAGEyyyyMMdd` 加短随机后缀或数据库序列后缀。
- 入院编号: `ADMyyyyMMdd` 加短随机后缀或数据库序列后缀。
- 出院编号: `DISyyyyMMdd` 加短随机后缀或数据库序列后缀。

## 接口设计

### 分诊建档

`POST /triage-records`

必填字段：`patientId`, `patientName`。护士字段 `nurseId`, `nurseName` 从请求体或网关透传用户头兜底。默认 `triageLevel=normal`, `status=created`。

`GET /triage-records`

支持分页，建议补充可选过滤：`patientId`, `nurseId`, `status`, `triageLevel`。保持 `rows/total/page/limit` 结构。

### 入院登记

`POST /inpatients/admissions`

必填字段：`patientId`, `patientName`, `admissionTime`。病房与床位可为空，但若传入 `wardNo + bedNo`，需要校验同一床位不存在 `in_hospital` 状态的占用记录。

`GET /inpatients/admissions`

支持分页和过滤：`patientId`, `status`, `wardNo`, `bedNo`。

### 出院办理

`POST /inpatients/admissions/{id}/discharge`

必须找到入院记录，且当前 `status=in_hospital`。办理成功后：

- 新增 `chuyuanxinxi` 记录。
- 更新 `ruyuanxinxi.status=discharged`。
- 返回出院记录，并兼容现有 `status=discharged` 字段。

`GET /inpatients/discharges`

支持分页和过滤：`patientId`, `admissionId`。

## 状态流

分诊状态：

- `created`: 已建档。
- `assigned`: 已分派医生或后续流程接收。
- `closed`: 分诊流程结束。

入院状态：

- `in_hospital`: 当前住院。
- `discharged`: 已出院。

出院记录创建后不可重复为同一个 `admissionId` 创建第二条出院记录，重复提交返回 `400` 和明确提示。

## 实现方案

1. 为 `emr-visit-service` 新增 `TriageRecordEntity`, `AdmissionEntity`, `DischargeEntity` 和对应 Mapper。
2. 抽出 `InpatientService`，把原 `InpatientController` 中的内存 Map 逻辑迁移到 Service。
3. Controller 只负责参数接收和异常响应，所有业务校验放入 Service。
4. 使用 H2 测试库新增 `InpatientPersistenceTest`，覆盖分诊落库、入院落库、出院落库并更新入院状态。
5. 更新 `README.md`，说明预约、分诊、住院均已数据库持久化。

## 兼容性

- 接口路径不变。
- 返回字段保留 `businessNo`，但内部可按表字段分别映射为 `triageNo`, `admissionNo`, `dischargeNo`。
- 现有前端无需改接口地址；如果前端展示具体编号，可继续读 `businessNo`。
- 数据库表已经存在，不需要新增 SQL 文件。

## 验证方式

- 运行 `mvn -q -pl emr-visit-service -am test -Dtest=InpatientPersistenceTest -Dsurefire.failIfNoSpecifiedTests=false`。
- 运行 `mvn -q -pl emr-visit-service -am test`。
- 手工验证流程：创建分诊、创建入院、办理出院、查询入院列表和出院列表。

