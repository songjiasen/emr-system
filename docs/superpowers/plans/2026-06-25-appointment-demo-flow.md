# 患者预约挂号完整演示流程 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 按测试流程补齐预约确认、检查费用支付和检查结果归档的最小演示闭环。

**Architecture:** 后端只新增预约确认接口，检查费用闭环优先复用现有费用和检查支付接口，由患者端串联“生成费用 -> 支付费用 -> 回写检查 paid”。后台端只补按钮入口和状态展示，不引入新表。

**Tech Stack:** Spring Boot、MyBatis-Plus、Vue 3、Element Plus、Vite。

---

### Task 1: 预约确认后端

**Files:**
- Modify: `backend/emr-visit-service/src/main/java/com/emr/visit/service/AppointmentService.java`
- Modify: `backend/emr-visit-service/src/main/java/com/emr/visit/service/impl/DatabaseAppointmentService.java`
- Modify: `backend/emr-visit-service/src/main/java/com/emr/visit/controller/AppointmentController.java`
- Test: `backend/emr-visit-service/src/test/java/com/emr/visit/AppointmentControllerTest.java`

- [ ] **Step 1: 写失败测试**
  - 在 `AppointmentControllerTest` 中新增 `confirmAppointmentChangesPendingToConfirmed`。
  - 创建预约后调用 `POST /appointments/{id}/confirm`，断言 `status=confirmed`。

- [ ] **Step 2: 运行红灯**
  - Run: `cd backend && mvn -pl emr-visit-service -Dtest=AppointmentControllerTest#confirmAppointmentChangesPendingToConfirmed test`
  - Expected: 404 或编译失败，说明确认接口未实现。

- [ ] **Step 3: 实现接口**
  - `AppointmentService` 增加 `confirmAppointment(Long id)`。
  - `DatabaseAppointmentService` 只允许 `pending -> confirmed`，`cancelled/finished` 报错。
  - `AppointmentController` 增加 `POST /appointments/{id}/confirm`，复用 `ensureAppointmentAccess` 限制医生只能确认本人预约。

- [ ] **Step 4: 运行绿灯**
  - Run: `cd backend && mvn -pl emr-visit-service -Dtest=AppointmentControllerTest test`
  - Expected: PASS。

### Task 2: 后台预约确认入口

**Files:**
- Modify: `frontend/manage_code/src/api/appointment.js`
- Modify: `frontend/manage_code/src/App.vue`

- [ ] **Step 1: 增加 API 封装**
  - `confirmAppointment(id)` 调用 `POST /appointments/{id}/confirm`。

- [ ] **Step 2: 增加按钮**
  - 预约管理列表中医生角色且 `row.status === 'pending'` 时显示“确认”。
  - 点击后调用确认接口，刷新预约列表，提示“预约已确认”。

- [ ] **Step 3: 状态文案**
  - `pending` 文案从“待处理”改为“待确认”。
  - 新增 `confirmed: '已确认'`、`finished: '已完成'`。

### Task 3: 患者端检查费用闭环

**Files:**
- Modify: `frontend/emr-frontend/src/App.vue`

- [ ] **Step 1: 调整去检查**
  - `goToCheckAction` 对 `approved` 检查生成 `businessType=test_request`、`businessId=检查ID`、金额 100 的未支付费用。
  - 若已有同业务未支付费用，则复用，不重复创建。
  - 生成费用后跳转 `fees` 页，不调用 `payTestRequest`。

- [ ] **Step 2: 调整支付成功回写**
  - `payFeeAction` 支付成功后，如果费用来源是 `test_request`，调用 `payTestRequest(businessId)`。
  - 再刷新费用、检查申请和患者余额。

- [ ] **Step 3: 补展示**
  - 检查申请列表增加 `resultContent` 列。
  - `paid` 显示“已支付”，`finished` 显示“已完成”。

### Task 4: 后台检查结果入口

**Files:**
- Modify: `frontend/manage_code/src/App.vue`

- [ ] **Step 1: 按钮入口**
  - 检查申请列表中 `paid` 状态显示“填写结果”。
  - 点击后打开编辑弹窗，并聚焦现有结果字段。

- [ ] **Step 2: 限制结果填写状态**
  - 检查结果输入框只在 `paid` 或已有 `resultContent` 时展示。
  - 保存后沿用现有 `PUT /test-requests/{id}`，后端会把非空结果保存为 `finished`。

### Task 5: 检查状态后端回归测试

**Files:**
- Modify: `backend/emr-clinical-service/src/test/java/com/emr/clinical/ClinicalPersistenceTest.java`

- [ ] **Step 1: 写状态分段测试**
  - 新增测试覆盖：审核通过后仍是 `approved`；调用 `/pay` 后是 `paid`；填写结果后是 `finished`。

- [ ] **Step 2: 运行测试**
  - Run: `cd backend && mvn -pl emr-clinical-service -Dtest=ClinicalPersistenceTest test`
  - Expected: PASS。

### Task 6: 整体验证

- [ ] **Step 1: 后端测试**
  - Run: `cd backend && mvn -pl emr-visit-service,emr-clinical-service test`

- [ ] **Step 2: 前端构建**
  - Run: `cd frontend/manage_code && npm run build`
  - Run: `cd frontend/emr-frontend && npm run build`

- [ ] **Step 3: 手动演示核对**
  - 患者端 `http://localhost:5173`：`patient_demo / 123456`。
  - 后台端 `http://localhost:5174`：医生、主任、护士依次执行截图流程。
