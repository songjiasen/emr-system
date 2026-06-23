# 病历管理接口文档

本文档整理病历管理模块当前已实现的基础接口，覆盖医生新增病历、病历列表、病历详情和患者查看病历。

## 统一响应结构

```json
{
  "code": 0,
  "message": "success",
  "data": {}
}
```

## 1. 新增病历

- 接口地址: `/medical-records`
- 请求方式: `POST`
- 鉴权要求: 医生或管理员；通过网关访问需携带 `Token`
- 所属服务: `emr-record-service`

### 请求参数

```json
{
  "appointmentId": 1,
  "appointmentNo": "APPT202606220001",
  "patientId": 1001,
  "patientName": "测试患者",
  "doctorId": 1,
  "doctorName": "王医生",
  "visitTime": "2026-06-23 10:00:00",
  "chiefComplaint": "胸闷一天",
  "presentIllness": "活动后明显",
  "pastHistory": "高血压病史",
  "diagnosis": "疑似心律失常",
  "treatmentAdvice": "完善心电图检查",
  "fileUrl": "/uploads/report.pdf"
}
```

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `appointmentId` | number | 否 | 预约 ID |
| `appointmentNo` | string | 否 | 预约编号 |
| `patientId` | number | 是 | 患者 ID |
| `patientName` | string | 是 | 患者姓名 |
| `doctorId` | number | 是 | 医生 ID |
| `doctorName` | string | 是 | 医生姓名 |
| `visitTime` | string | 是 | 就诊时间 |
| `chiefComplaint` | string | 否 | 主诉 |
| `presentIllness` | string | 否 | 现病史 |
| `pastHistory` | string | 否 | 既往史 |
| `diagnosis` | string | 否 | 诊断结果 |
| `treatmentAdvice` | string | 否 | 诊疗医嘱 |
| `fileUrl` | string | 否 | 病历附件地址 |

### 响应示例

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "id": 1,
    "recordNo": "MR202606220001",
    "appointmentId": 1,
    "appointmentNo": "APPT202606220001",
    "patientId": 1001,
    "patientName": "测试患者",
    "doctorId": 1,
    "doctorName": "王医生",
    "visitTime": "2026-06-23 10:00:00",
    "chiefComplaint": "胸闷一天",
    "presentIllness": "活动后明显",
    "pastHistory": "高血压病史",
    "diagnosis": "疑似心律失常",
    "treatmentAdvice": "完善心电图检查",
    "fileUrl": "/uploads/report.pdf",
    "archiveStatus": "not_submitted"
  }
}
```

### 业务规则

- 系统自动生成 `recordNo`，前缀为 `MR`。
- 新建病历默认归档状态为 `not_submitted`。
- 当前会写入 `emr_record.binglixinxi` 表。
- 医生通过网关访问时只能以本人医生 ID 创建病历。

## 2. 病历列表

- 接口地址: `/medical-records`
- 请求方式: `GET`
- 鉴权要求: 患者、医生、护士、主任、管理员；通过网关访问需携带 `Token`
- 所属服务: `emr-record-service`

### 请求参数

| 字段 | 类型 | 必填 | 默认值 | 说明 |
| --- | --- | --- | --- | --- |
| `patientId` | number | 否 | - | 按患者过滤，用于患者查看自己的病历 |
| `doctorId` | number | 否 | - | 按医生过滤，用于医生端病历管理 |
| `archiveStatus` | string | 否 | - | 按归档状态过滤 |
| `page` | number | 否 | `1` | 页码 |
| `limit` | number | 否 | `10` | 每页数量 |

### 响应示例

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "rows": [
      {
        "id": 1,
        "recordNo": "MR202606220001",
        "appointmentId": 1,
        "appointmentNo": "APPT202606220001",
        "patientId": 1001,
        "patientName": "测试患者",
        "doctorId": 1,
        "doctorName": "王医生",
        "visitTime": "2026-06-23 10:00:00",
        "chiefComplaint": "胸闷一天",
        "diagnosis": "疑似心律失常",
        "treatmentAdvice": "完善心电图检查",
        "archiveStatus": "not_submitted"
      }
    ],
    "total": 1,
    "page": 1,
    "limit": 10
  }
}
```

### 业务规则

- 患者通过网关访问时会强制限定为本人 `patientId`。
- 医生通过网关访问时会强制限定为本人 `doctorId`。
- 管理员、主任、护士可按授权范围查询。

## 3. 病历详情

- 接口地址: `/medical-records/{id}`
- 请求方式: `GET`
- 鉴权要求: 患者、医生、护士、主任、管理员；通过网关访问需携带 `Token`
- 所属服务: `emr-record-service`

### 路径参数

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `id` | number | 是 | 病历 ID |

### 业务规则

- 病历不存在时返回 `code=400` 和错误消息。
- 当前接口返回完整病历内容，包括主诉、现病史、既往史、诊断结果和诊疗医嘱。
