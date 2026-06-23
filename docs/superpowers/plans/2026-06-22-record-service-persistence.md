# 病历服务持久化实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将 `emr-record-service` 的病历、模板、归档从内存实现切换到 MySQL/H2 持久化，并保持现有接口路径与返回结构兼容。

**Architecture:** 继续沿用当前仓库里 `auth/user/system/visit` 已采用的 Spring Boot 3 + MyBatis-Plus 分层。病历主链路先单独落地到 `binglixinxi`，随后补模板与归档三张表，最后统一跑全量测试和说明文档。

**Tech Stack:** Java 17, Spring Boot 3.3, MyBatis-Plus 3.5.5, MySQL 5.7, H2, JUnit 5, TestRestTemplate

---

### Task 1: 建立测试基座并写病历持久化红灯测试

**Files:**
- Modify: `backend/emr-record-service/pom.xml`
- Create: `backend/emr-record-service/src/test/resources/application.yml`
- Create: `backend/emr-record-service/src/test/resources/schema.sql`
- Create: `backend/emr-record-service/src/test/java/com/emr/record/MedicalRecordPersistenceTest.java`
- Modify: `backend/emr-record-service/src/test/java/com/emr/record/MedicalRecordControllerTest.java`

- [ ] **Step 1: 为记录服务补 H2/JDBC 测试依赖**

- [ ] **Step 2: 写 `MedicalRecordPersistenceTest`**
测试点：
1. `POST /medical-records` 后 `binglixinxi` 有对应记录。
2. 直接插入数据库的病历能被 `/medical-records` 列表读出。
3. 更新病历会回写数据库。
4. 已归档病历禁止编辑。

- [ ] **Step 3: 运行单测确认红灯**

Run:
```bash
docker run --rm -v /Users/song/Documents/project/emr-system:/workspace -v emr-m2-cache:/root/.m2 -w /workspace/backend maven:3.9.9-eclipse-temurin-17 mvn -q -pl emr-record-service -am test -Dtest=MedicalRecordPersistenceTest -Dsurefire.failIfNoSpecifiedTests=false
```

Expected: 失败点来自“病历未落库或列表未读库”，而不是测试基座错误。

### Task 2: 实现病历主表数据库化

**Files:**
- Modify: `backend/emr-record-service/pom.xml`
- Modify: `backend/emr-record-service/src/main/resources/application.yml`
- Modify: `backend/emr-record-service/src/main/java/com/emr/record/RecordApplication.java`
- Create: `backend/emr-record-service/src/main/java/com/emr/record/entity/MedicalRecordEntity.java`
- Create: `backend/emr-record-service/src/main/java/com/emr/record/mapper/MedicalRecordMapper.java`
- Create: `backend/emr-record-service/src/main/java/com/emr/record/service/impl/DatabaseMedicalRecordService.java`
- Delete: `backend/emr-record-service/src/main/java/com/emr/record/service/impl/InMemoryMedicalRecordService.java`
- Modify: `backend/emr-record-service/src/main/java/com/emr/record/controller/MedicalRecordController.java`
- Modify: `deploy/mysql/init/04-emr-record.sql`
- Modify: `backend/emr-record-service/src/test/resources/schema.sql`

- [ ] **Step 1: 加入 MyBatis-Plus、MySQL、测试 JDBC/H2 依赖**

- [ ] **Step 2: 在启动类加 `@MapperScan`，在 `application.yml` 加数据源配置**

- [ ] **Step 3: 建 `MedicalRecordEntity` 和 `MedicalRecordMapper`**
字段覆盖：
`record_no`, `appointment_id`, `appointment_no`, `patient_id`, `patient_name`, `doctor_id`, `doctor_name`, `visit_time`, `chief_complaint`, `present_illness`, `past_history`, `diagnosis`, `treatment_advice`, `file_url`, `archive_status`

- [ ] **Step 4: 用数据库版服务替换内存版**
规则：
1. 创建时生成 `MR` 编号，默认 `archiveStatus=not_submitted`
2. 列表支持 `patientId/doctorId/archiveStatus`
3. 编辑时若 `archiveStatus=archived` 则拒绝
4. 删除时若已归档或存在归档申请则拒绝

- [ ] **Step 5: 更新建表脚本**
给 `binglixinxi` 补 `appointment_id`, `appointment_no` 两个兼容当前 API 的字段。

- [ ] **Step 6: 运行病历持久化测试确认转绿**

### Task 3: 写模板与归档红灯测试

**Files:**
- Create: `backend/emr-record-service/src/test/java/com/emr/record/TemplateArchivePersistenceTest.java`
- Modify: `backend/emr-record-service/src/test/resources/schema.sql`

- [ ] **Step 1: 写模板与归档持久化测试**
测试点：
1. 模板创建落到 `binglimoban`
2. 删除模板写 `status=0`
3. 创建归档申请后 `bingliguidangshenqing` 有记录，病历 `archive_status=pending`
4. 审核通过后生成 `bingliguidang` 并把病历状态改为 `archived`

- [ ] **Step 2: 运行单测确认红灯**

### Task 4: 实现模板与归档数据库化

**Files:**
- Create: `backend/emr-record-service/src/main/java/com/emr/record/entity/MedicalRecordTemplateEntity.java`
- Create: `backend/emr-record-service/src/main/java/com/emr/record/entity/ArchiveApplicationEntity.java`
- Create: `backend/emr-record-service/src/main/java/com/emr/record/entity/MedicalRecordArchiveEntity.java`
- Create: `backend/emr-record-service/src/main/java/com/emr/record/mapper/MedicalRecordTemplateMapper.java`
- Create: `backend/emr-record-service/src/main/java/com/emr/record/mapper/ArchiveApplicationMapper.java`
- Create: `backend/emr-record-service/src/main/java/com/emr/record/mapper/MedicalRecordArchiveMapper.java`
- Modify: `backend/emr-record-service/src/main/java/com/emr/record/controller/TemplateArchiveController.java`

- [ ] **Step 1: 为模板、归档申请、归档记录补实体和 Mapper**

- [ ] **Step 2: 把 `TemplateArchiveController` 改为数据库读写**
规则：
1. 模板删除改 `status=0`
2. 归档申请必须回表读取病历快照
3. 审核结果只允许 `approved/rejected`
4. 通过时生成归档记录并更新病历状态为 `archived`
5. 驳回时更新病历状态为 `rejected`

- [ ] **Step 3: 运行 `TemplateArchivePersistenceTest` 确认转绿**

### Task 5: 回归验证与说明更新

**Files:**
- Modify: `backend/emr-record-service/README.md`

- [ ] **Step 1: 运行记录服务全量测试**

Run:
```bash
docker run --rm -v /Users/song/Documents/project/emr-system:/workspace -v emr-m2-cache:/root/.m2 -w /workspace/backend maven:3.9.9-eclipse-temurin-17 mvn -q -pl emr-record-service -am test
```

Expected: 全部测试通过。

- [ ] **Step 2: 更新 README**
说明病历、模板、归档已切到数据库，以及 `binglixinxi` 新增预约关联字段的兼容口径。

