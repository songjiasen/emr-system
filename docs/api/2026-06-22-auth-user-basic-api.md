# 登录注册与医生查询接口文档

本文档整理当前已实现的基础接口，覆盖患者注册、用户登录、医生列表和医生详情。

## 统一响应结构

```json
{
  "code": 0,
  "message": "success",
  "data": {}
}
```

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `code` | number | 状态码，`0` 表示成功 |
| `message` | string | 响应消息 |
| `data` | object | 响应数据 |

## 1. 用户登录

- 接口地址: `/auth/login`
- 请求方式: `POST`
- 鉴权要求: 无
- 所属服务: `emr-auth-service`

### 请求参数

```json
{
  "username": "admin",
  "password": "admin123",
  "roleCode": "admin"
}
```

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `username` | string | 是 | 登录账号 |
| `password` | string | 是 | 登录密码 |
| `roleCode` | string | 是 | 角色编码，支持 `admin`, `doctor`, `patient`, `nurse`, `director` |

### 响应示例

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "userId": 1,
    "username": "admin",
    "roleCode": "admin",
    "tableName": "users",
    "token": "YW...demo"
  }
}
```

### 业务规则

- `username`, `password`, `roleCode` 不能为空。
- `roleCode=doctor` 时来源表为 `yisheng`。
- `roleCode=patient` 时来源表为 `huanzhe`。
- 其他后台角色来源表为 `users`。
- 当前登录读取 `emr_user` 账号表，密码使用 BCrypt 校验，登录成功后写入 `emr_auth.token`。

## 2. 患者注册

- 接口地址: `/auth/patient/register`
- 请求方式: `POST`
- 鉴权要求: 无
- 所属服务: `emr-auth-service`

### 请求参数

```json
{
  "username": "patient_demo",
  "password": "patient123",
  "name": "测试患者",
  "gender": "女",
  "phone": "13800000000"
}
```

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `username` | string | 是 | 患者账号 |
| `password` | string | 是 | 登录密码 |
| `name` | string | 是 | 患者姓名 |
| `gender` | string | 否 | 性别 |
| `phone` | string | 否 | 手机号 |

### 响应示例

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "patientId": 1001,
    "username": "patient_demo",
    "name": "测试患者",
    "gender": "女",
    "phone": "13800000000",
    "roleCode": "patient"
  }
}
```

### 业务规则

- 注册成功后固定返回 `patient` 角色。
- 注册会写入 `emr_user.huanzhe` 表，并校验账号唯一。
- 密码使用 BCrypt 加密存储。

## 3. 医生列表

- 接口地址: `/doctors`
- 请求方式: `GET`
- 鉴权要求: 公开查询；通过网关访问时仍会清理伪造身份头
- 所属服务: `emr-user-service`

### 请求参数

| 字段 | 类型 | 必填 | 默认值 | 说明 |
| --- | --- | --- | --- | --- |
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
        "username": "doctor_heart",
        "name": "王医生",
        "gender": "男",
        "phone": "13900000001",
        "departmentId": 1,
        "departmentName": "心内科",
        "specialty": "高血压、冠心病",
        "profile": "从事心内科临床工作多年，擅长常见心血管疾病诊疗。"
      }
    ],
    "total": 2,
    "page": 1,
    "limit": 10
  }
}
```

### 业务规则

- 用于患者预约挂号前选择医生。
- 当前查询 `emr_user.yisheng` 表，只返回启用医生。

## 4. 医生详情

- 接口地址: `/doctors/{id}`
- 请求方式: `GET`
- 鉴权要求: 公开查询；通过网关访问时仍会清理伪造身份头
- 所属服务: `emr-user-service`

### 路径参数

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `id` | number | 是 | 医生 ID |

### 响应示例

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "id": 1,
    "username": "doctor_heart",
    "name": "王医生",
    "gender": "男",
    "phone": "13900000001",
    "departmentId": 1,
    "departmentName": "心内科",
    "specialty": "高血压、冠心病",
    "profile": "从事心内科临床工作多年，擅长常见心血管疾病诊疗。"
  }
}
```

### 业务规则

- 医生不存在时返回 `code=400` 和错误消息。
- 后续预约挂号接口需要使用医生 ID、姓名、科室等字段生成预约记录。
