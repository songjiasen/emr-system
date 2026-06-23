# 数据库结构说明

本文档根据需求分析文档和概要设计文档整理，配合 `deploy/mysql/init/` 下的初始化 SQL 使用。

## 执行说明

- 适用数据库: MySQL 5.7。
- 字符集: `utf8mb4`。
- 执行前置条件: 已启动 MySQL，且当前账号具备建库、建表权限。
- 重复执行: SQL 使用 `CREATE DATABASE IF NOT EXISTS` 和 `CREATE TABLE IF NOT EXISTS`，可重复执行，不会清空已有数据。
- 数据影响: 仅创建库表和索引，不插入业务数据，不删除或修改已有数据。

## 微服务数据库

| SQL文件 | 数据库 | 服务 | 主要表 |
| --- | --- | --- | --- |
| `00-create-databases.sql` | 多库 | 全部服务 | 创建各服务数据库 |
| `01-emr-auth.sql` | `emr_auth` | `emr-auth-service` | `token` |
| `02-emr-user.sql` | `emr_user` | `emr-user-service` | `users`, `yisheng`, `huanzhe`, `keshileixing` |
| `03-emr-visit.sql` | `emr_visit` | `emr-visit-service` | `yuyueguahao`, `fenzhenjiandang`, `ruyuanxinxi`, `chuyuanxinxi` |
| `04-emr-record.sql` | `emr_record` | `emr-record-service` | `binglixinxi`, `binglimoban`, `bingliguidangshenqing`, `bingliguidang` |
| `05-emr-clinical.sql` | `emr_clinical` | `emr-clinical-service` | `yizhuxinxi`, `yizhuzhixingjilu`, `kaifang`, `jianchaxiang` |
| `06-emr-workflow.sql` | `emr_workflow` | `emr-workflow-service` | `shenherenwu`, `shenhejilu` |
| `07-emr-billing.sql` | `emr_billing` | `emr-billing-service` | `feiyong` |
| `08-emr-system.sql` | `emr_system` | `emr-system-service` | `news`, `messages`, `menu`, `config`, `syslog` |
| `09-emr-ai.sql` | `emr_ai` | `emr-ai-service` | `ai_request_log` |

## 表设计口径

- 表名优先沿用原概要设计文档中的拼音命名，例如 `yisheng`, `huanzhe`, `binglixinxi`, `yuyueguahao`。
- 原文档没有明确表名但有明确功能的模块，使用中文业务名拼音补齐，例如 `yizhuxinxi`, `fenzhenjiandang`, `shenherenwu`。
- 护士和主任在需求中是角色，但原文档没有给出独立表名，因此先归入 `users.role_code`，避免额外拆表。
- 微服务之间不建立跨库外键，跨服务关联通过业务 ID、编号和必要的名称冗余完成。
- 关键编号字段设置唯一索引，例如预约编号、病历编号、归档编号、费用编号。

## 后续实现顺序建议

1. 先实现 `emr-auth-service` 和 `emr-user-service`，跑通登录、患者注册、医生/患者查询。
2. 再实现 `emr-visit-service` 和 `emr-record-service`，跑通预约挂号到医生录入病历。
3. 然后实现 `emr-clinical-service` 和 `emr-workflow-service`，跑通医嘱审核和护士执行。
4. 最后补费用、资讯留言、AI 能力和归档完整流程。
