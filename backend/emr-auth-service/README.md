# emr-auth-service

认证权限服务，默认端口 `8101`。

## 服务职责

- 用户登录、注销。
- 患者注册。
- Token 生成、校验、过期处理。
- 修改密码、忘记密码。
- 角色和权限基础校验。

## 对应需求

- `FR-AUTH`: 用户登录、患者注册、密码修改、Token 认证、登录过期。
- 安全需求中的 Token 认证、密码加密、角色访问控制。

## 主要数据

- `token`
- `emr_user.users`
- `emr_user.yisheng`
- `emr_user.huanzhe`
- 登录账号、角色编码、Token 有效期等认证数据。

## 当前实现说明

- 账号主数据从 `emr_user` 库读取，覆盖管理员、医生、患者、护士、主任五类角色。
- Token 会话落在 `emr_auth.token`，支持过期校验、注销失效、改密后旧 Token 清理。
- 服务启动时会补齐演示账号，但只在账号不存在时插入，不覆盖已有真实数据。
- `/auth/token/validate` 从 `Token` 请求头读取登录态，供网关换取可信用户上下文。
- 患者注册直接写入 `emr_user.huanzhe`，密码使用 BCrypt 加密；注册成功后需要再登录获取 Token。
