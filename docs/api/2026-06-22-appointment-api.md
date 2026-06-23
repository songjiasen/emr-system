# 预约挂号接口文档

本文档整理预约挂号模块当前已实现的基础接口，覆盖预约新增、列表、详情和取消预约。

## 统一响应结构

```json
{
  "code": 0,
  "message": "success",
  "data": {}
}
```

## 1. 新增预约挂号

- 接口地址: `/appointments`
- 请求方式: `POST`
- 鉴权要求: 患者、管理员可调用；通过网关访问需携带 `Token`
- 所属服务: `emr-visit-service`

### 请求参数

```json
{
  "patientId": 1001,
  "patientName": "测试患者",
  "doctorId": 1,
  "doctorName": "王医生",
  "departmentId": 1,
  "departmentName": "心内科",
  "appointmentTime": "2026-06-23 09:30:00",
  "remark": "初诊"
}
```

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `patientId` | number | 是 | 患者 ID |
| `patientName` | string | 是 | 患者姓名 |
| `doctorId` | number | 是 | 医生 ID |
| `doctorName` | string | 是 | 医生姓名 |
| `departmentId` | number | 否 | 科室 ID |
| `departmentName` | string | 否 | 科室名称 |
| `appointmentTime` | string | 是 | 预约就诊时间 |
| `remark` | string | 否 | 备注 |

### 响应示例

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "id": 1,
    "appointmentNo": "APPT202606220001",
    "patientId": 1001,
    "patientName": "测试患者",
    "doctorId": 1,
    "doctorName": "王医生",
    "departmentId": 1,
    "departmentName": "心内科",
    "appointmentTime": "2026-06-23 09:30:00",
    "status": "pending",
    "cancelReason": null,
    "remark": "初诊"
  }
}
```

### 业务规则

- 系统自动生成 `appointmentNo`，前缀为 `APPT`。
- 新建预约默认状态为 `pending`。
- 当前会写入 `emr_visit.yuyueguahao` 表。
- 患者通过网关访问时只能为自己创建预约。

## 2. 预约列表

- 接口地址: `/appointments`
- 请求方式: `GET`
- 鉴权要求: 患者、医生、管理员可调用；通过网关访问需携带 `Token`
- 所属服务: `emr-visit-service`

### 请求参数

| 字段 | 类型 | 必填 | 默认值 | 说明 |
| --- | --- | --- | --- | --- |
| `patientId` | number | 否 | - | 按患者过滤 |
| `doctorId` | number | 否 | - | 按医生过滤 |
| `status` | string | 否 | - | 按状态过滤 |
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
        "appointmentNo": "APPT202606220001",
        "patientId": 1001,
        "patientName": "测试患者",
        "doctorId": 1,
        "doctorName": "王医生",
        "departmentId": 1,
        "departmentName": "心内科",
        "appointmentTime": "2026-06-23 09:30:00",
        "status": "pending",
        "cancelReason": null,
        "remark": "初诊"
      }
    ],
    "total": 1,
    "page": 1,
    "limit": 10
  }
}
```

## 3. 预约详情

- 接口地址: `/appointments/{id}`
- 请求方式: `GET`
- 鉴权要求: 患者、医生、管理员可调用；通过网关访问需携带 `Token`
- 所属服务: `emr-visit-service`

### 路径参数

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `id` | number | 是 | 预约 ID |

### 业务规则

- 预约不存在时返回 `code=400` 和错误消息。

## 4. 取消预约

- 接口地址: `/appointments/{id}/cancel`
- 请求方式: `POST`
- 鉴权要求: 患者、管理员可调用；通过网关访问需携带 `Token`
- 所属服务: `emr-visit-service`

### 请求参数

```json
{
  "cancelReason": "时间冲突"
}
```

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `cancelReason` | string | 否 | 取消原因 |

### 响应示例

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "id": 1,
    "appointmentNo": "APPT202606220001",
    "patientId": 1001,
    "patientName": "测试患者",
    "doctorId": 1,
    "doctorName": "王医生",
    "departmentId": 1,
    "departmentName": "心内科",
    "appointmentTime": "2026-06-23 09:30:00",
    "status": "cancelled",
    "cancelReason": "时间冲突",
    "remark": "初诊"
  }
}
```

### 业务规则

- 取消后状态变为 `cancelled`。
- 已完成预约不能取消。
- 患者通过网关访问时只能取消自己的预约；医生只能查看自己的预约。
