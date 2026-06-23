<template>
  <main v-if="!hasAdminToken" class="auth-page admin-auth-page">
    <section class="auth-card">
      <div class="auth-brand">
        <p class="eyebrow">管理端</p>
        <h1>EMR 后台</h1>
      </div>

      <el-form label-position="top" :model="adminLoginForm">
        <el-form-item label="账号">
          <el-input v-model="adminLoginForm.username" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="adminLoginForm.password" type="password" show-password />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="adminLoginForm.roleCode">
            <el-option label="超级管理员" value="super_admin" />
            <el-option label="管理员" value="admin" />
            <el-option label="医生" value="doctor" />
            <el-option label="护士" value="nurse" />
            <el-option label="主任" value="director" />
          </el-select>
        </el-form-item>
        <el-button type="primary" class="auth-submit" @click="submitAdminLogin">登录</el-button>
      </el-form>
    </section>
  </main>

  <main v-else class="admin-shell">
    <aside class="sidebar">
      <h1>EMR 后台</h1>
      <nav>
        <button
          v-for="item in navItems"
          :key="item.name"
          :class="{ active: activeTab === item.name }"
          type="button"
          @click="activeTab = item.name"
        >
          {{ item.label }}
        </button>
      </nav>
    </aside>

    <section class="workspace">
      <header class="workspace-head">
        <div>
          <p class="eyebrow">管理端</p>
          <h2>电子病历管理系统</h2>
        </div>
        <div class="session">
          <span>{{ adminSession.username }}</span>
          <el-tag>{{ adminSession.roleCode }}</el-tag>
          <el-button v-if="hasAdminToken" link type="primary" @click="logoutAdminAction">退出</el-button>
        </div>
      </header>

      <el-tabs v-model="activeTab" class="workspace-tabs">
        <el-tab-pane label="仪表盘" name="dashboard">
          <section class="dashboard-layout">
            <div class="overview-grid">
              <article class="overview-card">
                <span class="overview-label">科室总数</span>
                <strong class="overview-value">{{ departments.length }}</strong>
              </article>
              <article class="overview-card">
                <span class="overview-label">当前人员列表</span>
                <strong class="overview-value">{{ users.length }}</strong>
              </article>
              <article class="overview-card">
                <span class="overview-label">当前页预约</span>
                <strong class="overview-value">{{ appointments.length }}</strong>
              </article>
              <article class="overview-card">
                <span class="overview-label">当前页病历</span>
                <strong class="overview-value">{{ records.length }}</strong>
              </article>
              <article class="overview-card">
                <span class="overview-label">待审核流程</span>
                <strong class="overview-value">{{ workflowTasks.filter((item) => item.status !== 'approved').length }}</strong>
              </article>
              <article class="overview-card">
                <span class="overview-label">待支付费用</span>
                <strong class="overview-value">{{ fees.filter((item) => item.status !== 'paid').length }}</strong>
              </article>
            </div>

            <section class="two-column">
              <div class="panel">
                <div class="panel-head">
                  <h3>最近预约</h3>
                  <el-button @click="loadAppointments">刷新</el-button>
                </div>
                <el-table :data="appointments.slice(0, 6)" height="320">
                  <el-table-column prop="appointmentNo" label="预约号" min-width="130" />
                  <el-table-column prop="patientName" label="患者" width="100" />
                  <el-table-column prop="doctorName" label="医生" width="100" />
                  <el-table-column prop="appointmentTime" label="时间" min-width="150" />
                  <el-table-column prop="status" label="状态" width="100" :formatter="statusFormatter" />
                </el-table>
              </div>

              <div class="panel">
                <div class="panel-head">
                  <h3>待办提醒</h3>
                  <el-button @click="loadAdminWorkspaceData">刷新</el-button>
                </div>
                <el-table :data="workflowTasks.slice(0, 6)" height="150">
                  <el-table-column prop="businessType" label="流程类型" min-width="120" />
                  <el-table-column prop="status" label="状态" width="100" :formatter="statusFormatter" />
                </el-table>
                <el-table :data="archiveApplications.slice(0, 6)" height="150">
                  <el-table-column prop="recordNo" label="病历号" min-width="130" />
                  <el-table-column prop="patientName" label="患者" width="100" />
                  <el-table-column prop="status" label="状态" width="100" :formatter="statusFormatter" />
                </el-table>
              </div>
            </section>
          </section>
        </el-tab-pane>

        <el-tab-pane label="预约管理" name="appointments">
          <section class="two-column">
            <div class="panel">
              <h3>预约筛选</h3>
              <el-form label-position="top" :model="appointmentFilterForm">
                <el-form-item label="患者 ID">
                  <el-input-number v-model="appointmentFilterForm.patientId" :min="1" :controls="false" />
                </el-form-item>
                <el-form-item label="医生 ID">
                  <el-input-number v-model="appointmentFilterForm.doctorId" :min="1" :controls="false" />
                </el-form-item>
                <el-form-item label="状态">
                  <el-select v-model="appointmentFilterForm.status" clearable placeholder="全部状态">
                    <el-option label="待就诊" value="pending" />
                    <el-option label="已取消" value="cancelled" />
                    <el-option label="已完成" value="completed" />
                  </el-select>
                </el-form-item>
                <div class="button-row">
                  <el-button type="primary" @click="loadAppointments">查询预约</el-button>
                  <el-button @click="resetAppointmentFilters">重置筛选</el-button>
                </div>
              </el-form>
              <el-descriptions v-if="selectedAppointment" :column="1" border class="menu-preview">
                <el-descriptions-item label="预约号">{{ selectedAppointment.appointmentNo }}</el-descriptions-item>
                <el-descriptions-item label="患者">{{ selectedAppointment.patientName }}</el-descriptions-item>
                <el-descriptions-item label="医生">{{ selectedAppointment.doctorName }}</el-descriptions-item>
                <el-descriptions-item label="状态">{{ statusText(selectedAppointment.status) }}</el-descriptions-item>
                <el-descriptions-item label="备注">{{ selectedAppointment.remark || '-' }}</el-descriptions-item>
              </el-descriptions>
            </div>

            <div class="panel">
              <div class="panel-head">
                <h3>预约列表</h3>
                <el-button @click="loadAppointments">刷新</el-button>
              </div>
              <el-table :data="appointments" height="360" @row-click="selectAppointment">
                <el-table-column prop="appointmentNo" label="预约号" min-width="130" />
                <el-table-column prop="patientName" label="患者" width="100" />
                <el-table-column prop="doctorName" label="医生" width="100" />
                <el-table-column prop="appointmentTime" label="预约时间" min-width="150" />
                <el-table-column prop="status" label="状态" width="100" :formatter="statusFormatter" />
                <el-table-column prop="cancelReason" label="取消原因" min-width="140" />
                <el-table-column label="操作" width="100">
                  <template #default="{ row }">
                    <el-button link type="danger" :disabled="row.status === 'cancelled'" @click="cancelAppointmentAction(row)">取消</el-button>
                  </template>
                </el-table-column>
              </el-table>
              <el-pagination
                class="table-pagination"
                layout="prev, pager, next, total"
                :current-page="pagers.appointments.page"
                :page-size="pagers.appointments.limit"
                :total="pagers.appointments.total"
                @current-change="(page) => changePage('appointments', page, loadAppointments)"
              />
            </div>
          </section>
        </el-tab-pane>

        <el-tab-pane label="用户科室" name="users">
          <section class="three-column">
            <div class="panel">
              <h3>科室维护</h3>
              <el-form label-position="top" :model="departmentForm">
                <el-form-item label="科室名称">
                  <el-input v-model="departmentForm.name" />
                </el-form-item>
                <el-form-item label="排序">
                  <el-input-number v-model="departmentForm.sortNo" :min="1" />
                </el-form-item>
                <div class="button-row">
                  <el-button type="primary" @click="createDepartmentAction">新增科室</el-button>
                  <el-button :disabled="!selectedDepartment" @click="updateDepartmentAction">更新科室</el-button>
                  <el-button :disabled="!selectedDepartment" type="danger" plain @click="deleteDepartmentAction">删除科室</el-button>
                  <el-button @click="loadDepartments">刷新</el-button>
                </div>
              </el-form>
            </div>

            <div class="panel">
              <h3>人员维护</h3>
              <el-form label-position="top" :model="userForm">
                <el-form-item label="用户类型">
                  <el-select v-model="userType" @change="handleUserTypeChange">
                    <el-option label="管理员" value="admins" />
                    <el-option label="医生" value="doctors" />
                    <el-option label="患者" value="patients" />
                    <el-option label="护士" value="nurses" />
                    <el-option label="主任" value="directors" />
                  </el-select>
                </el-form-item>
                <el-form-item label="账号">
                  <el-input v-model="userForm.username" />
                </el-form-item>
                <el-form-item label="姓名">
                  <el-input v-model="userForm.name" />
                </el-form-item>
                <el-form-item label="手机号">
                  <el-input v-model="userForm.phone" />
                </el-form-item>
                <div class="button-row">
                  <el-button type="primary" @click="createUserAction">新增人员</el-button>
                  <el-button :disabled="!selectedUser" @click="updateUserAction">更新人员</el-button>
                  <el-button :disabled="!selectedUser" type="danger" plain @click="deleteUserAction">删除人员</el-button>
                </div>
              </el-form>
            </div>

            <div class="panel wide-panel">
              <h3>用户与科室列表</h3>
              <el-table :data="departments" height="170" @row-click="selectDepartment">
                <el-table-column prop="name" label="科室" min-width="130" />
                <el-table-column prop="sortNo" label="排序" width="80" />
                <el-table-column prop="status" label="状态" width="90" :formatter="statusFormatter" />
              </el-table>
              <el-pagination
                class="table-pagination"
                layout="prev, pager, next, total"
                :current-page="pagers.departments.page"
                :page-size="pagers.departments.limit"
                :total="pagers.departments.total"
                @current-change="(page) => changePage('departments', page, loadDepartments)"
              />
              <el-table :data="users" height="200" @row-click="selectUser">
                <el-table-column prop="username" label="账号" min-width="130" />
                <el-table-column prop="name" label="姓名" min-width="100" />
                <el-table-column prop="userType" label="类型" width="110" />
                <el-table-column prop="status" label="状态" width="90" :formatter="statusFormatter" />
              </el-table>
              <el-pagination
                class="table-pagination"
                layout="prev, pager, next, total"
                :current-page="pagers.users.page"
                :page-size="pagers.users.limit"
                :total="pagers.users.total"
                @current-change="(page) => changePage('users', page, loadUsers)"
              />
            </div>
          </section>
        </el-tab-pane>

        <el-tab-pane label="就诊病历" name="records">
          <section class="three-column">
            <div class="panel">
              <h3>新增病历</h3>
              <el-form label-position="top" :model="recordForm">
                <el-form-item label="患者姓名">
                  <el-input v-model="recordForm.patientName" />
                </el-form-item>
                <el-form-item label="医生姓名">
                  <el-input v-model="recordForm.doctorName" />
                </el-form-item>
                <el-form-item label="主诉">
                  <el-input v-model="recordForm.chiefComplaint" />
                </el-form-item>
                <el-form-item label="诊断">
                  <el-input v-model="recordForm.diagnosis" />
                </el-form-item>
                <el-form-item label="治疗建议">
                  <el-input v-model="recordForm.treatmentAdvice" type="textarea" :rows="3" />
                </el-form-item>
                <el-form-item label="附件地址">
                  <el-input v-model="recordForm.fileUrl" placeholder="/uploads/record-demo.pdf" />
                </el-form-item>
                <div class="button-row">
                  <el-button type="primary" @click="createRecordAction">保存病历</el-button>
                  <el-button :disabled="!selectedRecord" @click="updateRecordAction">更新病历</el-button>
                  <el-button :disabled="!selectedRecord" type="danger" plain @click="deleteRecordAction">删除病历</el-button>
                  <el-button @click="loadRecords">刷新</el-button>
                </div>
              </el-form>
            </div>

            <div class="panel">
              <h3>分诊入院</h3>
              <el-form label-position="top" :model="triageForm">
                <el-form-item label="患者姓名">
                  <el-input v-model="triageForm.patientName" />
                </el-form-item>
                <el-form-item label="主诉">
                  <el-input v-model="triageForm.chiefComplaint" />
                </el-form-item>
                <el-form-item label="分诊级别">
                  <el-select v-model="triageForm.triageLevel">
                    <el-option label="普通" value="normal" />
                    <el-option label="急诊" value="urgent" />
                  </el-select>
                </el-form-item>
                <div class="button-row">
                  <el-button type="primary" @click="createTriageAction">分诊</el-button>
                  <el-button @click="createAdmissionAction">入院</el-button>
                </div>
              </el-form>
              <el-divider />
              <el-form label-position="top" :model="admissionForm">
                <el-form-item label="病区床位">
                  <el-input v-model="admissionForm.bedNo" />
                </el-form-item>
                <el-form-item label="出院时间">
                  <el-input v-model="dischargeForm.dischargeTime" />
                </el-form-item>
                <el-button @click="dischargeAdmissionAction">办理出院</el-button>
              </el-form>
            </div>

            <div class="panel">
              <div class="panel-head">
                <h3>病历列表</h3>
                <el-button @click="createArchiveApplicationAction">申请归档</el-button>
              </div>
              <el-table :data="records" height="190" @row-click="selectRecord">
                <el-table-column prop="recordNo" label="病历号" min-width="130" />
                <el-table-column prop="patientName" label="患者" min-width="90" />
                <el-table-column prop="diagnosis" label="诊断" min-width="150" />
                <el-table-column prop="archiveStatus" label="归档状态" width="110" :formatter="statusFormatter" />
              </el-table>
              <el-pagination
                class="table-pagination"
                layout="prev, pager, next, total"
                :current-page="pagers.records.page"
                :page-size="pagers.records.limit"
                :total="pagers.records.total"
                @current-change="(page) => changePage('records', page, loadRecords)"
              />
              <el-descriptions v-if="selectedRecordDetail" :column="1" border class="menu-preview">
                <el-descriptions-item label="主诉">{{ selectedRecordDetail.chiefComplaint || '-' }}</el-descriptions-item>
                <el-descriptions-item label="现病史">{{ selectedRecordDetail.presentIllness || '-' }}</el-descriptions-item>
                <el-descriptions-item label="治疗建议">{{ selectedRecordDetail.treatmentAdvice || '-' }}</el-descriptions-item>
                <el-descriptions-item label="附件">
                  <a v-if="selectedRecordDetail.fileUrl" :href="selectedRecordDetail.fileUrl" target="_blank" rel="noreferrer">查看附件</a>
                  <span v-else>-</span>
                </el-descriptions-item>
              </el-descriptions>
              <el-table :data="admissions" height="190">
                <el-table-column prop="businessNo" label="住院号" min-width="130" />
                <el-table-column prop="patientName" label="患者" min-width="90" />
                <el-table-column prop="bedNo" label="床位" width="100" />
                <el-table-column prop="status" label="状态" width="110" :formatter="statusFormatter" />
              </el-table>
              <el-pagination
                class="table-pagination"
                layout="prev, pager, next, total"
                :current-page="pagers.admissions.page"
                :page-size="pagers.admissions.limit"
                :total="pagers.admissions.total"
                @current-change="(page) => changePage('admissions', page, loadAdmissions)"
              />
              <el-table :data="triageRecords" height="120">
                <el-table-column prop="triageNo" label="分诊号" min-width="130" />
                <el-table-column prop="patientName" label="患者" width="90" />
                <el-table-column prop="triageLevel" label="级别" width="90" />
              </el-table>
              <el-pagination
                class="table-pagination"
                layout="prev, pager, next, total"
                :current-page="pagers.triageRecords.page"
                :page-size="pagers.triageRecords.limit"
                :total="pagers.triageRecords.total"
                @current-change="(page) => changePage('triageRecords', page, loadTriageRecords)"
              />
              <el-table :data="discharges" height="120">
                <el-table-column prop="dischargeNo" label="出院号" min-width="130" />
                <el-table-column prop="patientName" label="患者" width="90" />
                <el-table-column prop="dischargeTime" label="出院时间" min-width="150" />
              </el-table>
              <el-pagination
                class="table-pagination"
                layout="prev, pager, next, total"
                :current-page="pagers.discharges.page"
                :page-size="pagers.discharges.limit"
                :total="pagers.discharges.total"
                @current-change="(page) => changePage('discharges', page, loadDischarges)"
              />
            </div>

            <div class="panel">
              <h3>病历模板</h3>
              <el-form label-position="top" :model="templateForm">
                <el-form-item label="模板名称">
                  <el-input v-model="templateForm.templateName" />
                </el-form-item>
                <el-form-item label="模板类型">
                  <el-input v-model="templateForm.templateType" />
                </el-form-item>
                <el-form-item label="模板内容">
                  <el-input v-model="templateForm.content" type="textarea" :rows="3" />
                </el-form-item>
                <div class="button-row">
                  <el-button type="primary" @click="createTemplateAction">新增模板</el-button>
                  <el-button :disabled="!selectedTemplate" @click="updateTemplateAction">更新模板</el-button>
                  <el-button :disabled="!selectedTemplate" type="danger" plain @click="deleteTemplateAction">删除模板</el-button>
                  <el-button :disabled="!selectedTemplate" @click="applyTemplateToRecord">套用到病历</el-button>
                  <el-button @click="loadTemplates">刷新</el-button>
                </div>
              </el-form>
              <el-table :data="templates" height="180" @row-click="selectTemplate">
                <el-table-column prop="templateName" label="模板" min-width="140" />
                <el-table-column prop="templateType" label="类型" width="100" />
                <el-table-column prop="status" label="状态" width="90" :formatter="statusFormatter" />
              </el-table>
              <el-pagination
                class="table-pagination"
                layout="prev, pager, next, total"
                :current-page="pagers.templates.page"
                :page-size="pagers.templates.limit"
                :total="pagers.templates.total"
                @current-change="(page) => changePage('templates', page, loadTemplates)"
              />
            </div>
          </section>
        </el-tab-pane>

        <el-tab-pane label="诊疗管理" name="clinical">
          <section class="three-column">
            <div class="panel">
              <h3>医嘱</h3>
              <el-form label-position="top" :model="orderForm">
                <el-form-item label="患者">
                  <el-input v-model="orderForm.patientName" />
                </el-form-item>
                <el-form-item label="医生">
                  <el-input v-model="orderForm.doctorName" />
                </el-form-item>
                <el-form-item label="医嘱内容">
                  <el-input v-model="orderForm.content" type="textarea" :rows="3" />
                </el-form-item>
                <div class="button-row">
                  <el-button type="primary" @click="createOrderAction">新增医嘱</el-button>
                  <el-button :disabled="!selectedOrder" @click="updateOrderAction">更新医嘱</el-button>
                  <el-button :disabled="!selectedOrder" type="danger" plain @click="deleteOrderAction">删除医嘱</el-button>
                  <el-button @click="loadOrders">刷新</el-button>
                </div>
              </el-form>
            </div>

            <div class="panel">
              <h3>处方与检查</h3>
              <el-form label-position="top" :model="prescriptionForm">
                <el-form-item label="药品名称">
                  <el-input v-model="prescriptionForm.medicineName" />
                </el-form-item>
                <el-form-item label="数量">
                  <el-input-number v-model="prescriptionForm.quantity" :min="1" />
                </el-form-item>
                <div class="button-row">
                  <el-button type="primary" @click="createPrescriptionAction">新增处方</el-button>
                  <el-button :disabled="!selectedPrescription" @click="updatePrescriptionAction">更新处方</el-button>
                  <el-button :disabled="!selectedPrescription" type="danger" plain @click="deletePrescriptionAction">删除处方</el-button>
                </div>
              </el-form>
              <el-divider />
              <el-form label-position="top" :model="testRequestForm">
                <el-form-item label="检查项目">
                  <el-input v-model="testRequestForm.testItem" />
                </el-form-item>
                <el-form-item label="检查原因">
                  <el-input v-model="testRequestForm.testReason" />
                </el-form-item>
                <el-form-item label="检查结果">
                  <el-input v-model="testRequestForm.resultContent" type="textarea" :rows="3" />
                </el-form-item>
                <div class="button-row">
                  <el-button @click="createTestRequestAction">新增检查</el-button>
                  <el-button :disabled="!selectedTestRequest" @click="updateTestRequestAction">更新检查</el-button>
                  <el-button :disabled="!selectedTestRequest" type="danger" plain @click="deleteTestRequestAction">删除检查</el-button>
                </div>
              </el-form>
            </div>

            <div class="panel wide-panel">
              <h3>诊疗列表</h3>
              <el-table :data="orders" height="160" @row-click="selectOrder">
                <el-table-column prop="content" label="医嘱" min-width="160" />
                <el-table-column prop="status" label="状态" width="100" :formatter="statusFormatter" />
                <el-table-column label="操作" width="170">
                  <template #default="{ row }">
                    <el-button link type="primary" @click="auditOrderAction(row)">通过</el-button>
                    <el-button link @click="executeOrderAction(row)">执行</el-button>
                  </template>
                </el-table-column>
              </el-table>
              <el-pagination
                class="table-pagination"
                layout="prev, pager, next, total"
                :current-page="pagers.orders.page"
                :page-size="pagers.orders.limit"
                :total="pagers.orders.total"
                @current-change="(page) => changePage('orders', page, loadOrders)"
              />
              <el-table :data="prescriptions" height="140" @row-click="selectPrescription">
                <el-table-column prop="medicineName" label="处方" min-width="150" />
                <el-table-column prop="quantity" label="数量" width="90" />
                <el-table-column prop="status" label="状态" width="100" :formatter="statusFormatter" />
              </el-table>
              <el-pagination
                class="table-pagination"
                layout="prev, pager, next, total"
                :current-page="pagers.prescriptions.page"
                :page-size="pagers.prescriptions.limit"
                :total="pagers.prescriptions.total"
                @current-change="(page) => changePage('prescriptions', page, loadPrescriptions)"
              />
              <el-table :data="testRequests" height="140" @row-click="selectTestRequest">
                <el-table-column prop="testItem" label="检查" min-width="150" />
                <el-table-column prop="status" label="状态" width="100" :formatter="statusFormatter" />
                <el-table-column prop="resultContent" label="结果" min-width="150" />
                <el-table-column label="操作" width="100">
                  <template #default="{ row }">
                    <el-button link type="primary" @click="auditTestAction(row)">审核</el-button>
                  </template>
                </el-table-column>
              </el-table>
              <el-pagination
                class="table-pagination"
                layout="prev, pager, next, total"
                :current-page="pagers.testRequests.page"
                :page-size="pagers.testRequests.limit"
                :total="pagers.testRequests.total"
                @current-change="(page) => changePage('testRequests', page, loadTestRequests)"
              />
            </div>
          </section>
        </el-tab-pane>

        <el-tab-pane label="审核归档" name="workflow">
          <section class="two-column">
            <div class="panel">
              <h3>创建审核任务</h3>
              <el-form label-position="top" :model="workflowForm">
                <el-form-item label="业务类型">
                  <el-select v-model="workflowForm.businessType">
                    <el-option label="病历审核" value="medical_record" />
                    <el-option label="医嘱审核" value="medical_order" />
                    <el-option label="检查审核" value="test_request" />
                    <el-option label="归档审核" value="record_archive" />
                  </el-select>
                </el-form-item>
                <el-form-item label="业务ID">
                  <el-input-number v-model="workflowForm.businessId" :min="1" />
                </el-form-item>
                <el-form-item label="审核角色">
                  <el-input v-model="workflowForm.assigneeRole" />
                </el-form-item>
                <div class="button-row">
                  <el-button type="primary" @click="createWorkflowTaskAction">创建任务</el-button>
                  <el-button @click="loadWorkflowTasks">刷新</el-button>
                </div>
              </el-form>
            </div>

            <div class="panel">
              <h3>审核与归档</h3>
              <el-table :data="workflowTasks" height="220">
                <el-table-column prop="businessType" label="类型" min-width="130" />
                <el-table-column prop="businessId" label="业务ID" width="100" />
                <el-table-column prop="status" label="状态" width="100" :formatter="statusFormatter" />
                <el-table-column label="操作" width="100">
                  <template #default="{ row }">
                    <el-button link type="primary" @click="auditWorkflowAction(row)">审核</el-button>
                  </template>
                </el-table-column>
              </el-table>
              <el-pagination
                class="table-pagination"
                layout="prev, pager, next, total"
                :current-page="pagers.workflowTasks.page"
                :page-size="pagers.workflowTasks.limit"
                :total="pagers.workflowTasks.total"
                @current-change="(page) => changePage('workflowTasks', page, loadWorkflowTasks)"
              />
              <el-table :data="workflowAuditRecords" height="180">
                <el-table-column prop="taskNo" label="任务号" min-width="130" />
                <el-table-column prop="auditorName" label="审核人" width="100" />
                <el-table-column prop="auditResult" label="结果" width="100" :formatter="statusFormatter" />
                <el-table-column prop="auditOpinion" label="意见" min-width="150" />
              </el-table>
              <el-pagination
                class="table-pagination"
                layout="prev, pager, next, total"
                :current-page="pagers.workflowAuditRecords.page"
                :page-size="pagers.workflowAuditRecords.limit"
                :total="pagers.workflowAuditRecords.total"
                @current-change="(page) => changePage('workflowAuditRecords', page, loadWorkflowAuditRecords)"
              />
              <el-table :data="archiveApplications" height="220">
                <el-table-column prop="recordNo" label="病历号" min-width="130" />
                <el-table-column prop="patientName" label="患者" min-width="90" />
                <el-table-column prop="status" label="状态" width="100" :formatter="statusFormatter" />
                <el-table-column label="操作" width="100">
                  <template #default="{ row }">
                    <el-button link type="primary" @click="auditArchiveAction(row)">归档</el-button>
                  </template>
                </el-table-column>
              </el-table>
              <el-pagination
                class="table-pagination"
                layout="prev, pager, next, total"
                :current-page="pagers.archiveApplications.page"
                :page-size="pagers.archiveApplications.limit"
                :total="pagers.archiveApplications.total"
                @current-change="(page) => changePage('archiveApplications', page, loadArchiveApplications)"
              />
              <el-table :data="archives" height="180">
                <el-table-column prop="recordNo" label="归档病历号" min-width="130" />
                <el-table-column prop="patientName" label="患者" min-width="90" />
                <el-table-column prop="status" label="状态" width="100" :formatter="statusFormatter" />
              </el-table>
              <el-pagination
                class="table-pagination"
                layout="prev, pager, next, total"
                :current-page="pagers.archives.page"
                :page-size="pagers.archives.limit"
                :total="pagers.archives.total"
                @current-change="(page) => changePage('archives', page, loadArchives)"
              />
            </div>
          </section>
        </el-tab-pane>

        <el-tab-pane label="费用系统" name="billing">
          <section class="two-column">
            <div class="panel">
              <h3>费用录入</h3>
              <el-form label-position="top" :model="feeForm">
                <el-form-item label="患者姓名">
                  <el-input v-model="feeForm.patientName" />
                </el-form-item>
                <el-form-item label="费用类型">
                  <el-input v-model="feeForm.feeType" />
                </el-form-item>
                <el-form-item label="金额">
                  <el-input-number v-model="feeForm.amount" :min="0" :precision="2" />
                </el-form-item>
                <div class="button-row">
                  <el-button type="primary" @click="createFeeAction">新增费用</el-button>
                  <el-button @click="loadFees">刷新</el-button>
                </div>
              </el-form>
            </div>

            <div class="panel">
              <h3>费用列表</h3>
              <el-table :data="fees" height="360">
                <el-table-column prop="feeNo" label="费用号" min-width="130" />
                <el-table-column prop="patientName" label="患者" min-width="100" />
                <el-table-column prop="amount" label="金额" width="100" />
                <el-table-column prop="status" label="状态" width="100" :formatter="statusFormatter" />
                <el-table-column label="操作" width="100">
                  <template #default="{ row }">
                    <el-button link type="primary" @click="payFeeAction(row)">支付</el-button>
                  </template>
                </el-table-column>
              </el-table>
              <el-pagination
                class="table-pagination"
                layout="prev, pager, next, total"
                :current-page="pagers.fees.page"
                :page-size="pagers.fees.limit"
                :total="pagers.fees.total"
                @current-change="(page) => changePage('fees', page, loadFees)"
              />
            </div>
          </section>
        </el-tab-pane>

        <el-tab-pane label="系统内容" name="system">
          <section class="three-column">
            <div class="panel">
              <h3>资讯发布</h3>
              <el-form label-position="top" :model="newsForm">
                <el-form-item label="标题">
                  <el-input v-model="newsForm.title" />
                </el-form-item>
                <el-form-item label="分类">
                  <el-input v-model="newsForm.category" />
                </el-form-item>
                <el-form-item label="内容">
                  <el-input v-model="newsForm.content" type="textarea" :rows="3" />
                </el-form-item>
                <el-button type="primary" @click="createNewsAction">发布资讯</el-button>
              </el-form>
            </div>

            <div class="panel">
              <h3>轮播图</h3>
              <el-form label-position="top" :model="carouselForm">
                <el-form-item label="标题">
                  <el-input v-model="carouselForm.title" />
                </el-form-item>
                <el-form-item label="图片地址">
                  <el-input v-model="carouselForm.imageUrl" />
                </el-form-item>
                <el-form-item label="跳转地址">
                  <el-input v-model="carouselForm.linkUrl" />
                </el-form-item>
                <div class="button-row">
                  <el-button type="primary" @click="createCarouselAction">新增轮播</el-button>
                  <el-button @click="loadCarousels">刷新</el-button>
                </div>
              </el-form>
            </div>

            <div class="panel">
              <h3>系统配置</h3>
              <el-form label-position="top" :model="configForm">
                <el-form-item label="配置键">
                  <el-input v-model="configForm.configKey" />
                </el-form-item>
                <el-form-item label="配置值">
                  <el-input v-model="configForm.configValue" />
                </el-form-item>
                <div class="button-row">
                  <el-button type="primary" @click="saveConfigAction">保存配置</el-button>
                  <el-button @click="loadSyslogs">刷新日志</el-button>
                </div>
              </el-form>
            </div>

            <div class="panel">
              <h3>菜单管理</h3>
              <el-form label-position="top" :model="menuForm">
                <el-form-item label="角色编码">
                  <el-select v-model="menuForm.roleCode">
                    <el-option label="管理员" value="admin" />
                    <el-option label="医生" value="doctor" />
                    <el-option label="患者" value="patient" />
                    <el-option label="主任" value="director" />
                    <el-option label="护士" value="nurse" />
                  </el-select>
                </el-form-item>
                <el-form-item label="菜单名称">
                  <el-input v-model="menuForm.name" />
                </el-form-item>
                <el-form-item label="菜单 JSON">
                  <el-input v-model="menuForm.menujson" type="textarea" :rows="4" />
                </el-form-item>
                <div class="button-row">
                  <el-button type="primary" @click="saveMenuAction">保存菜单</el-button>
                  <el-button @click="loadMenuAction()">读取菜单</el-button>
                </div>
              </el-form>
            </div>

            <div class="panel wide-panel">
              <h3>资讯、轮播、留言与日志</h3>
              <el-table :data="newsItems" height="120">
                <el-table-column prop="title" label="资讯标题" min-width="140" />
                <el-table-column prop="category" label="分类" width="100" />
                <el-table-column prop="publishStatus" label="发布状态" width="110" :formatter="statusFormatter" />
              </el-table>
              <el-pagination
                class="table-pagination"
                layout="prev, pager, next, total"
                :current-page="pagers.newsItems.page"
                :page-size="pagers.newsItems.limit"
                :total="pagers.newsItems.total"
                @current-change="(page) => changePage('newsItems', page, loadNews)"
              />
              <el-table :data="carousels" height="130">
                <el-table-column prop="title" label="轮播标题" min-width="140" />
                <el-table-column prop="imageUrl" label="图片" min-width="160" />
                <el-table-column label="操作" width="90">
                  <template #default="{ row }">
                    <el-button link type="danger" @click="deleteCarouselAction(row)">停用</el-button>
                  </template>
                </el-table-column>
              </el-table>
              <el-pagination
                class="table-pagination"
                layout="prev, pager, next, total"
                :current-page="pagers.carousels.page"
                :page-size="pagers.carousels.limit"
                :total="pagers.carousels.total"
                @current-change="(page) => changePage('carousels', page, loadCarousels)"
              />
              <el-table :data="messages" height="150">
                <el-table-column prop="title" label="留言" min-width="140" />
                <el-table-column prop="userName" label="咨询人" width="100" />
                <el-table-column label="操作" width="100">
                  <template #default="{ row }">
                    <el-button link type="primary" @click="replyMessageAction(row)">回复</el-button>
                  </template>
                </el-table-column>
              </el-table>
              <el-pagination
                class="table-pagination"
                layout="prev, pager, next, total"
                :current-page="pagers.messages.page"
                :page-size="pagers.messages.limit"
                :total="pagers.messages.total"
                @current-change="(page) => changePage('messages', page, loadMessages)"
              />
              <el-table :data="syslogs" height="130">
                <el-table-column prop="operation" label="操作" min-width="140" />
                <el-table-column prop="requestUri" label="请求地址" min-width="160" />
                <el-table-column prop="username" label="操作人" width="110" />
              </el-table>
              <el-pagination
                class="table-pagination"
                layout="prev, pager, next, total"
                :current-page="pagers.syslogs.page"
                :page-size="pagers.syslogs.limit"
                :total="pagers.syslogs.total"
                @current-change="(page) => changePage('syslogs', page, loadSyslogs)"
              />
              <el-descriptions v-if="menuSnapshot" :column="1" border class="menu-preview">
                <el-descriptions-item label="角色">{{ menuSnapshot.roleCode }}</el-descriptions-item>
                <el-descriptions-item label="名称">{{ menuSnapshot.name }}</el-descriptions-item>
                <el-descriptions-item label="菜单JSON">{{ menuSnapshot.menujson }}</el-descriptions-item>
              </el-descriptions>
            </div>
          </section>
        </el-tab-pane>

        <el-tab-pane label="AI 智能" name="ai">
          <section class="two-column">
            <div class="panel">
              <h3>AI 能力</h3>
              <el-form label-position="top" :model="aiForm">
                <el-form-item label="文件地址">
                  <el-input v-model="aiForm.fileUrl" />
                </el-form-item>
                <el-form-item label="诊断">
                  <el-input v-model="aiForm.diagnosis" />
                </el-form-item>
                <el-form-item label="智能搜索关键词">
                  <el-input v-model="aiForm.searchKeyword" />
                </el-form-item>
                <el-form-item label="处方文本">
                  <el-input v-model="aiForm.prescriptionText" type="textarea" :rows="3" />
                </el-form-item>
                <div class="button-row">
                  <el-button type="primary" @click="runOcrAction">OCR</el-button>
                  <el-button @click="runMedicineAction">荐药</el-button>
                  <el-button @click="runAuditAction">审方</el-button>
                  <el-button @click="runSmartSearchAction">搜索</el-button>
                </div>
              </el-form>
            </div>

            <div class="panel">
              <h3>AI 结果</h3>
              <pre class="result-box">{{ aiResult }}</pre>
            </div>
          </section>
        </el-tab-pane>
      </el-tabs>

      <router-view />
    </section>
  </main>
</template>

<script setup>
import { onBeforeUnmount, onMounted, reactive, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { login, logout, validateToken } from './api/auth';
import { fetchAppointments, fetchAppointmentDetail, cancelAppointment } from './api/appointment';
import { createDepartment, fetchDepartments, updateDepartment, deleteDepartment } from './api/department';
import { createManagedUser, fetchManagedUsers, updateManagedUser, deleteManagedUser } from './api/userManagement';
import { createTriageRecord, fetchTriageRecords, createAdmission, fetchAdmissions, dischargeAdmission, fetchDischarges } from './api/inpatient';
import { createMedicalRecord, fetchMedicalRecords, fetchMedicalRecordDetail, updateMedicalRecord, deleteMedicalRecord, createMedicalRecordTemplate, fetchMedicalRecordTemplates, updateMedicalRecordTemplate, deleteMedicalRecordTemplate, createArchiveApplication, fetchArchiveApplications, auditArchiveApplication, fetchArchives } from './api/medicalRecord';
import { createMedicalOrder, fetchMedicalOrders, updateMedicalOrder, deleteMedicalOrder, updateMedicalOrderAuditResult, executeMedicalOrder, createPrescription, fetchPrescriptions, updatePrescription, deletePrescription, createTestRequest, fetchTestRequests, updateTestAuditResult, updateTestRequest, deleteTestRequest } from './api/clinical';
import { createWorkflowTask, fetchWorkflowTasks, auditWorkflowTask, fetchAuditRecords } from './api/workflow';
import { createFee, fetchFees, payFee } from './api/billing';
import { createNews, fetchNews, fetchMessages, replyMessage, createCarousel, fetchCarousels, deleteCarousel, saveConfig, saveMenu, fetchMenu, fetchSyslogs } from './api/system';
import { ocrMedicalRecord, recommendMedicine, auditPrescription, smartSearch } from './api/ai';
import { AUTH_EVENT_NAME, clearAuthState, getStoredToken, readStoredSession, saveAuthState } from './utils/session';

const navItems = [
  { name: 'dashboard', label: '仪表盘' },
  { name: 'appointments', label: '预约管理' },
  { name: 'users', label: '用户科室' },
  { name: 'records', label: '就诊病历' },
  { name: 'clinical', label: '诊疗管理' },
  { name: 'workflow', label: '审核归档' },
  { name: 'billing', label: '费用系统' },
  { name: 'system', label: '系统内容' },
  { name: 'ai', label: 'AI 智能' }
];

const activeTab = ref('dashboard');
const adminSession = ref(createEmptyAdminSession());
const hasAdminToken = ref(Boolean(getStoredToken()));
const userType = ref('doctors');
const selectedAppointment = ref(null);
const selectedDepartment = ref(null);
const selectedUser = ref(null);
const selectedRecord = ref(null);
const selectedRecordDetail = ref(null);
const selectedTemplate = ref(null);
const selectedOrder = ref(null);
const selectedPrescription = ref(null);
const selectedTestRequest = ref(null);

const appointments = ref([]);
const departments = ref([]);
const users = ref([]);
const records = ref([]);
const triageRecords = ref([]);
const admissions = ref([]);
const discharges = ref([]);
const templates = ref([]);
const orders = ref([]);
const prescriptions = ref([]);
const testRequests = ref([]);
const workflowTasks = ref([]);
const workflowAuditRecords = ref([]);
const archiveApplications = ref([]);
const archives = ref([]);
const fees = ref([]);
const newsItems = ref([]);
const messages = ref([]);
const carousels = ref([]);
const syslogs = ref([]);
const aiResult = ref('等待执行');

const adminLoginForm = ref({ username: 'admin', password: 'admin123', roleCode: 'admin' });
const appointmentFilterForm = ref({ patientId: null, doctorId: null, status: '' });
const departmentForm = ref({ name: '全科医学科', sortNo: 3 });
const userForm = ref({ username: 'doctor_demo', name: '赵医生', phone: '13900000003', departmentId: 1, departmentName: '心内科', specialty: '慢病管理' });
const recordForm = ref({
  appointmentId: 1,
  appointmentNo: 'YY202606220001',
  patientId: 1,
  patientName: '患者演示',
  doctorId: 1,
  doctorName: '王医生',
  visitTime: '2026-06-22 10:00',
  chiefComplaint: '头晕一周',
  presentIllness: '近一周反复头晕，活动后明显。',
  pastHistory: '高血压病史三年。',
  diagnosis: '高血压',
  treatmentAdvice: '规律服药，低盐饮食，定期复查。',
  fileUrl: '/uploads/demo-record.pdf'
});
const triageForm = ref({ patientId: 1, patientName: '患者演示', nurseId: 1, nurseName: '护士演示', chiefComplaint: '头晕一周', triageLevel: 'normal' });
const admissionForm = ref({ patientId: 1, patientName: '患者演示', doctorId: 1, doctorName: '王医生', nurseId: 1, nurseName: '护士演示', wardNo: 'A1', bedNo: 'A1-08', admissionTime: '2026-06-22 14:00', reason: '观察治疗' });
const dischargeForm = ref({ dischargeTime: '2026-06-25 10:00', dischargeReason: '病情稳定', dischargeSummary: '按医嘱复诊' });
const templateForm = ref({ templateName: '门诊首诊模板', templateType: '门诊', content: '主诉：\\n现病史：\\n诊断：\\n处理意见：' });
const orderForm = ref({ recordId: 1, patientId: 1, patientName: '患者演示', doctorId: 1, doctorName: '王医生', orderType: '长期医嘱', content: '每日监测血压两次' });
const prescriptionForm = ref({ patientId: 1, patientName: '患者演示', doctorId: 1, doctorName: '王医生', medicineName: '硝苯地平控释片', quantity: 7, usageText: '每日一次' });
const testRequestForm = ref({ patientId: 1, patientName: '患者演示', doctorId: 1, doctorName: '王医生', testItem: '血常规', testReason: '评估基础指标', resultContent: '' });
const workflowForm = ref({ businessType: 'medical_record', businessId: 1, businessNo: 'BL202606220001', applicantId: 1, applicantName: '王医生', assigneeRole: 'director' });
const feeForm = ref({ patientId: 1, patientName: '患者演示', feeType: '挂号费', amount: 30, relatedBusinessType: 'appointment', relatedBusinessId: 1 });
const newsForm = ref({ title: '高血压随访提醒', category: '慢病管理', content: '规律监测血压，按医嘱服药。', publishStatus: 'published' });
const carouselForm = ref({ title: '智慧医院服务', imageUrl: '/uploads/banner-emr.png', linkUrl: '/news/1', sortNo: 1 });
const configForm = ref({ configKey: 'hospital_name', configValue: '智慧医院', remark: '演示配置' });
const menuForm = ref({ roleCode: 'admin', name: '管理员菜单', menujson: '[{"name":"仪表盘","path":"/dashboard"},{"name":"系统管理","path":"/system"}]' });
const menuSnapshot = ref(null);
const aiForm = ref({ fileUrl: '/uploads/demo-record.png', diagnosis: '高血压', searchKeyword: '高血压', prescriptionText: '硝苯地平控释片 每日一次' });

/**
 * 创建后台游客态。
 * 未登录时页面仍可稳定渲染，但不会误导用户认为当前账号具备真实操作权限。
 */
function createEmptyAdminSession() {
  return {
    userId: null,
    username: '未登录管理员',
    roleCode: 'guest'
  };
}

function unwrap(response) {
  const body = response?.data;
  if (!body || body.code !== 0) {
    throw new Error(body?.message || '接口返回异常');
  }
  return body.data;
}

function rowsOf(pageData) {
  return Array.isArray(pageData?.rows) ? pageData.rows : [];
}

function createPager() {
  return {
    page: 1,
    limit: 10,
    total: 0
  };
}

const pagers = reactive({
  appointments: createPager(),
  departments: createPager(),
  users: createPager(),
  records: createPager(),
  triageRecords: createPager(),
  admissions: createPager(),
  discharges: createPager(),
  templates: createPager(),
  orders: createPager(),
  prescriptions: createPager(),
  testRequests: createPager(),
  workflowTasks: createPager(),
  workflowAuditRecords: createPager(),
  archiveApplications: createPager(),
  archives: createPager(),
  fees: createPager(),
  newsItems: createPager(),
  messages: createPager(),
  carousels: createPager(),
  syslogs: createPager()
});

/**
 * 把分页接口返回值同步到本地列表与分页状态。
 * 后端各服务虽然拆分独立，但列表口径保持统一，前端在这里集中兜底 page/limit/total 字段。
 */
function applyPageData(target, pagerKey, pageData) {
  const pager = pagers[pagerKey];
  if (!pager) {
    target.value = rowsOf(pageData);
    return;
  }

  target.value = rowsOf(pageData);
  pager.page = Number(pageData?.pageNum ?? pageData?.page ?? pager.page ?? 1);
  pager.limit = Number(pageData?.pageSize ?? pageData?.limit ?? pager.limit ?? 10);
  pager.total = Number(pageData?.total ?? target.value.length ?? 0);
}

function changePage(pagerKey, page, loader) {
  const pager = pagers[pagerKey];
  if (!pager || typeof loader !== 'function') {
    return;
  }
  pager.page = page;
  loader();
}

function resetPager(pagerKey) {
  const pager = pagers[pagerKey];
  if (!pager) {
    return;
  }
  pager.page = 1;
  pager.limit = 10;
  pager.total = 0;
}

function resetAllPagers() {
  Object.keys(pagers).forEach((key) => {
    resetPager(key);
  });
}

const STATUS_TEXT = {
  pending: '待处理',
  pending_audit: '待审核',
  approved: '已通过',
  rejected: '已驳回',
  executed: '已执行',
  not_submitted: '未提交',
  archived: '已归档',
  cancelled: '已取消',
  in_hospital: '住院中',
  discharged: '已出院',
  unpaid: '未支付',
  paid: '已支付',
  published: '已发布'
};

function statusText(value) {
  return STATUS_TEXT[value] || value || '-';
}

function statusFormatter(_row, _column, value) {
  return statusText(value);
}

function showError(error) {
  const status = error?.response?.status;
  if (status === 401 || status === 403) {
    return;
  }
  ElMessage.error(error?.response?.data?.message || error?.message || '操作失败');
}

/**
 * 后台登录成功后保存 Token，网关和各服务通过同一请求头继续识别登录态。
 */
async function submitAdminLogin() {
  try {
    const data = unwrap(await login(adminLoginForm.value));
    persistAdminSession(data);
    activeTab.value = 'dashboard';
    loadAdminWorkspaceData();
    ElMessage.success('后台登录成功');
  } catch (error) {
    showError(error);
  }
}

/**
 * 持久化后台登录态。
 * 登录成功和 Token 校验成功都走同一入口，保证页面展示和本地存储口径一致。
 */
function persistAdminSession(data) {
  const sessionSnapshot = {
    userId: data?.userId ?? null,
    username: data?.username || '未命名管理员',
    roleCode: data?.roleCode || 'guest'
  };
  saveAuthState(data?.token || getStoredToken(), sessionSnapshot);
  adminSession.value = sessionSnapshot;
  hasAdminToken.value = Boolean(getStoredToken());
}

/**
 * 清空后台工作台状态。
 * 401 失效或主动退出后先重置列表，再回到登录页，避免界面残留旧账号数据。
 */
function clearAdminWorkspace() {
  resetAllPagers();
  appointments.value = [];
  departments.value = [];
  users.value = [];
  records.value = [];
  triageRecords.value = [];
  admissions.value = [];
  discharges.value = [];
  templates.value = [];
  orders.value = [];
  prescriptions.value = [];
  testRequests.value = [];
  workflowTasks.value = [];
  workflowAuditRecords.value = [];
  archiveApplications.value = [];
  archives.value = [];
  fees.value = [];
  newsItems.value = [];
  messages.value = [];
  carousels.value = [];
  syslogs.value = [];
  menuSnapshot.value = null;
  aiResult.value = '等待执行';
  selectedAppointment.value = null;
  selectedDepartment.value = null;
  selectedUser.value = null;
  selectedRecord.value = null;
  selectedRecordDetail.value = null;
  selectedTemplate.value = null;
  selectedOrder.value = null;
  selectedPrescription.value = null;
  selectedTestRequest.value = null;
}

/**
 * 清理后台认证态。
 * 支持在静默恢复失败时不重复提示，也支持主动退出时回收本地所有会话痕迹。
 */
function resetAdminAuth({ resetTab = true } = {}) {
  clearAuthState();
  hasAdminToken.value = false;
  adminSession.value = createEmptyAdminSession();
  clearAdminWorkspace();
  if (resetTab) {
    activeTab.value = 'dashboard';
  }
}

/**
 * 恢复本地后台会话快照。
 * 这里只恢复展示字段，真正是否还能访问接口要等 Token 校验通过后再决定。
 */
function restoreAdminSession() {
  const storedSession = readStoredSession();
  if (storedSession) {
    adminSession.value = { ...createEmptyAdminSession(), ...storedSession };
  }
  hasAdminToken.value = Boolean(getStoredToken());
}

/**
 * 校验后台 Token 是否仍然有效。
 * 成功后刷新本地快照，失败则交给统一认证事件处理链回收登录态。
 */
async function verifyAdminSession() {
  if (!getStoredToken()) {
    return false;
  }
  try {
    const data = unwrap(await validateToken());
    persistAdminSession(data);
    activeTab.value = 'dashboard';
    return true;
  } catch (error) {
    return false;
  }
}

/**
 * 查询预约列表。
 * 后台预约管理允许按患者、医生、状态组合过滤，空值会被剔除，避免把 null 误传给后端。
 */
async function loadAppointments() {
  try {
    const params = {
      page: pagers.appointments.page,
      limit: pagers.appointments.limit
    };
    if (appointmentFilterForm.value.patientId) {
      params.patientId = appointmentFilterForm.value.patientId;
    }
    if (appointmentFilterForm.value.doctorId) {
      params.doctorId = appointmentFilterForm.value.doctorId;
    }
    if (appointmentFilterForm.value.status) {
      params.status = appointmentFilterForm.value.status;
    }
    applyPageData(appointments, 'appointments', unwrap(await fetchAppointments(params)));
  } catch (error) {
    showError(error);
  }
}

/**
 * 重置预约筛选条件。
 * 恢复成默认空筛选后立刻重查，保证后台看到的是完整预约池，而不是残留旧条件。
 */
function resetAppointmentFilters() {
  appointmentFilterForm.value = { patientId: null, doctorId: null, status: '' };
  pagers.appointments.page = 1;
  loadAppointments();
}

/**
 * 后台取消预约。
 * 这里复用患者端的取消接口，并补一个固定原因，方便演示预约管理对异常预约的处理链路。
 */
async function cancelAppointmentAction(row) {
  if (!row?.id) {
    ElMessage.warning('缺少预约记录，暂时无法取消');
    return;
  }

  try {
    const data = unwrap(await cancelAppointment(row.id, { cancelReason: '后台预约管理取消' }));
    replaceRow(appointments.value, data);
    ElMessage.success('预约已取消');
  } catch (error) {
    showError(error);
  }
}

/**
 * 查看预约详情。
 * 点击列表行后回表读取完整预约数据，保证左侧详情面板展示的是后端最新状态。
 */
async function selectAppointment(row) {
  if (!row?.id) {
    selectedAppointment.value = null;
    return;
  }

  try {
    selectedAppointment.value = unwrap(await fetchAppointmentDetail(row.id));
  } catch (error) {
    showError(error);
  }
}

/**
 * 退出后台登录。
 * 优先通知后端使 Token 失效；即使接口异常，也要回收本地会话，避免页面继续保留旧权限。
 */
async function logoutAdminAction() {
  try {
    if (getStoredToken()) {
      await logout();
    }
  } catch (error) {
    // 退出接口失败时仍按本地退出处理，避免用户被卡在失效会话里。
  }
  resetAdminAuth();
  ElMessage.success('已退出登录');
}

async function createDepartmentAction() {
  try {
    await createDepartment(departmentForm.value);
    await loadDepartments();
    ElMessage.success('科室已新增');
  } catch (error) {
    showError(error);
  }
}

async function loadDepartments() {
  try {
    applyPageData(departments, 'departments', unwrap(await fetchDepartments({
      page: pagers.departments.page,
      limit: pagers.departments.limit
    })));
  } catch (error) {
    showError(error);
  }
}

function selectDepartment(row) {
  selectedDepartment.value = row;
  departmentForm.value = {
    name: row?.name || '',
    sortNo: row?.sortNo || 1
  };
}

async function updateDepartmentAction() {
  if (!selectedDepartment.value?.id) {
    ElMessage.warning('请先选择一条科室记录');
    return;
  }

  try {
    await updateDepartment(selectedDepartment.value.id, departmentForm.value);
    await loadDepartments();
    ElMessage.success('科室已更新');
  } catch (error) {
    showError(error);
  }
}

async function deleteDepartmentAction() {
  if (!selectedDepartment.value?.id) {
    ElMessage.warning('请先选择一条科室记录');
    return;
  }

  try {
    await deleteDepartment(selectedDepartment.value.id);
    await loadDepartments();
    selectedDepartment.value = null;
    departmentForm.value = { name: '全科医学科', sortNo: 3 };
    ElMessage.success('科室已删除');
  } catch (error) {
    showError(error);
  }
}

async function createUserAction() {
  try {
    await createManagedUser(userType.value, userForm.value);
    await loadUsers();
    ElMessage.success('人员已新增');
  } catch (error) {
    showError(error);
  }
}

async function loadUsers() {
  try {
    applyPageData(users, 'users', unwrap(await fetchManagedUsers(userType.value, {
      page: pagers.users.page,
      limit: pagers.users.limit
    })));
  } catch (error) {
    showError(error);
  }
}

/**
 * 切换用户类型时回到第一页。
 * 不同角色的数据量差异较大，保留旧页码容易落到空页，所以这里统一重置再查询。
 */
function handleUserTypeChange() {
  pagers.users.page = 1;
  loadUsers();
}

function selectUser(row) {
  selectedUser.value = row;
  userForm.value = {
    username: row?.username || '',
    name: row?.name || '',
    phone: row?.phone || '',
    departmentId: row?.departmentId || 1,
    departmentName: row?.departmentName || '心内科',
    specialty: row?.specialty || '慢病管理'
  };
}

async function updateUserAction() {
  if (!selectedUser.value?.id) {
    ElMessage.warning('请先选择一条人员记录');
    return;
  }

  try {
    await updateManagedUser(userType.value, selectedUser.value.id, userForm.value);
    await loadUsers();
    ElMessage.success('人员已更新');
  } catch (error) {
    showError(error);
  }
}

async function deleteUserAction() {
  if (!selectedUser.value?.id) {
    ElMessage.warning('请先选择一条人员记录');
    return;
  }

  try {
    await deleteManagedUser(userType.value, selectedUser.value.id);
    await loadUsers();
    selectedUser.value = null;
    userForm.value = { username: 'doctor_demo', name: '赵医生', phone: '13900000003', departmentId: 1, departmentName: '心内科', specialty: '慢病管理' };
    ElMessage.success('人员已删除');
  } catch (error) {
    showError(error);
  }
}

async function createRecordAction() {
  try {
    const data = unwrap(await createMedicalRecord(recordForm.value));
    await loadRecords();
    selectedRecord.value = data;
    orderForm.value.recordId = data.id;
    workflowForm.value.businessId = data.id;
    workflowForm.value.businessNo = data.recordNo;
    ElMessage.success('病历已保存');
  } catch (error) {
    showError(error);
  }
}

async function loadRecords() {
  try {
    applyPageData(records, 'records', unwrap(await fetchMedicalRecords({
      page: pagers.records.page,
      limit: pagers.records.limit
    })));
  } catch (error) {
    showError(error);
  }
}

async function createTriageAction() {
  try {
    await createTriageRecord(triageForm.value);
    await loadTriageRecords();
    ElMessage.success('分诊记录已创建');
  } catch (error) {
    showError(error);
  }
}

async function loadTriageRecords() {
  try {
    applyPageData(triageRecords, 'triageRecords', unwrap(await fetchTriageRecords({
      page: pagers.triageRecords.page,
      limit: pagers.triageRecords.limit
    })));
  } catch (error) {
    showError(error);
  }
}

async function createAdmissionAction() {
  try {
    await createAdmission(admissionForm.value);
    await loadAdmissions();
    ElMessage.success('入院登记已创建');
  } catch (error) {
    showError(error);
  }
}

async function loadAdmissions() {
  try {
    applyPageData(admissions, 'admissions', unwrap(await fetchAdmissions({
      page: pagers.admissions.page,
      limit: pagers.admissions.limit
    })));
  } catch (error) {
    showError(error);
  }
}

async function loadDischarges() {
  try {
    applyPageData(discharges, 'discharges', unwrap(await fetchDischarges({
      page: pagers.discharges.page,
      limit: pagers.discharges.limit
    })));
  } catch (error) {
    showError(error);
  }
}

async function dischargeAdmissionAction() {
  const admission = admissions.value[0];
  if (!admission) {
    ElMessage.warning('请先创建或刷新一条入院记录');
    return;
  }

  try {
    await dischargeAdmission(admission.id, dischargeForm.value);
    await loadAdmissions();
    loadDischarges();
    ElMessage.success('出院办理完成');
  } catch (error) {
    showError(error);
  }
}

async function createTemplateAction() {
  try {
    await createMedicalRecordTemplate(templateForm.value);
    await loadTemplates();
    ElMessage.success('病历模板已新增');
  } catch (error) {
    showError(error);
  }
}

async function loadTemplates() {
  try {
    applyPageData(templates, 'templates', unwrap(await fetchMedicalRecordTemplates({
      page: pagers.templates.page,
      limit: pagers.templates.limit
    })));
  } catch (error) {
    showError(error);
  }
}

function selectTemplate(row) {
  selectedTemplate.value = row;
  templateForm.value = {
    templateName: row?.templateName || '',
    templateType: row?.templateType || '',
    content: row?.content || ''
  };
}

async function updateTemplateAction() {
  if (!selectedTemplate.value?.id) {
    ElMessage.warning('请先选择一条模板');
    return;
  }

  try {
    await updateMedicalRecordTemplate(selectedTemplate.value.id, templateForm.value);
    await loadTemplates();
    ElMessage.success('模板已更新');
  } catch (error) {
    showError(error);
  }
}

async function deleteTemplateAction() {
  if (!selectedTemplate.value?.id) {
    ElMessage.warning('请先选择一条模板');
    return;
  }

  try {
    await deleteMedicalRecordTemplate(selectedTemplate.value.id);
    await loadTemplates();
    selectedTemplate.value = null;
    templateForm.value = { templateName: '门诊首诊模板', templateType: '门诊', content: '主诉：\\n现病史：\\n诊断：\\n处理意见：' };
    ElMessage.success('模板已删除');
  } catch (error) {
    showError(error);
  }
}

function applyTemplateToRecord() {
  if (!selectedTemplate.value) {
    ElMessage.warning('请先选择一条模板');
    return;
  }

  recordForm.value.treatmentAdvice = selectedTemplate.value.content || recordForm.value.treatmentAdvice;
  ElMessage.success('模板内容已带入病历表单');
}

function selectRecord(row) {
  selectedRecord.value = row;
  orderForm.value.recordId = row.id;
  workflowForm.value.businessId = row.id;
  workflowForm.value.businessNo = row.recordNo;
  recordForm.value = {
    appointmentId: row?.appointmentId || 1,
    appointmentNo: row?.appointmentNo || '',
    patientId: row?.patientId || 1,
    patientName: row?.patientName || '',
    doctorId: row?.doctorId || 1,
    doctorName: row?.doctorName || '',
    visitTime: row?.visitTime || '2026-06-22 10:00',
    chiefComplaint: row?.chiefComplaint || '',
    presentIllness: row?.presentIllness || '',
    pastHistory: row?.pastHistory || '',
    diagnosis: row?.diagnosis || '',
    treatmentAdvice: row?.treatmentAdvice || '',
    fileUrl: row?.fileUrl || ''
  };
  loadRecordDetail(row.id);
}

async function loadRecordDetail(id) {
  if (!id) {
    selectedRecordDetail.value = null;
    return;
  }

  try {
    selectedRecordDetail.value = unwrap(await fetchMedicalRecordDetail(id));
  } catch (error) {
    showError(error);
  }
}

async function updateRecordAction() {
  if (!selectedRecord.value?.id) {
    ElMessage.warning('请先选择一条病历');
    return;
  }

  try {
    const data = unwrap(await updateMedicalRecord(selectedRecord.value.id, recordForm.value));
    await loadRecords();
    selectedRecord.value = data;
    await loadRecordDetail(data.id);
    ElMessage.success('病历已更新');
  } catch (error) {
    showError(error);
  }
}

async function deleteRecordAction() {
  if (!selectedRecord.value?.id) {
    ElMessage.warning('请先选择一条病历');
    return;
  }

  try {
    await deleteMedicalRecord(selectedRecord.value.id);
    await loadRecords();
    selectedRecord.value = null;
    selectedRecordDetail.value = null;
    ElMessage.success('病历已删除');
  } catch (error) {
    showError(error);
  }
}

/**
 * 归档申请依赖已选病历，缺失时直接提示，避免提交无业务关联的审核单。
 */
async function createArchiveApplicationAction() {
  if (!selectedRecord.value) {
    ElMessage.warning('请先选择一条病历');
    return;
  }

  try {
    await createArchiveApplication({
      recordId: selectedRecord.value.id,
      recordNo: selectedRecord.value.recordNo,
      patientId: selectedRecord.value.patientId,
      patientName: selectedRecord.value.patientName,
      doctorId: selectedRecord.value.doctorId,
      doctorName: selectedRecord.value.doctorName
    });
    await loadArchiveApplications();
    ElMessage.success('归档申请已提交');
  } catch (error) {
    showError(error);
  }
}

async function createOrderAction() {
  try {
    await createMedicalOrder(orderForm.value);
    await loadOrders();
    ElMessage.success('医嘱已新增');
  } catch (error) {
    showError(error);
  }
}

async function loadOrders() {
  try {
    applyPageData(orders, 'orders', unwrap(await fetchMedicalOrders({
      page: pagers.orders.page,
      limit: pagers.orders.limit
    })));
  } catch (error) {
    showError(error);
  }
}

function selectOrder(row) {
  selectedOrder.value = row;
  orderForm.value = {
    recordId: row?.recordId || 1,
    patientId: row?.patientId || 1,
    patientName: row?.patientName || '',
    doctorId: row?.doctorId || 1,
    doctorName: row?.doctorName || '',
    orderType: row?.orderType || '长期医嘱',
    content: row?.content || ''
  };
}

async function updateOrderAction() {
  if (!selectedOrder.value?.id) {
    ElMessage.warning('请先选择一条医嘱');
    return;
  }

  try {
    await updateMedicalOrder(selectedOrder.value.id, orderForm.value);
    await loadOrders();
    ElMessage.success('医嘱已更新');
  } catch (error) {
    showError(error);
  }
}

async function deleteOrderAction() {
  if (!selectedOrder.value?.id) {
    ElMessage.warning('请先选择一条医嘱');
    return;
  }

  try {
    await deleteMedicalOrder(selectedOrder.value.id);
    await loadOrders();
    selectedOrder.value = null;
    ElMessage.success('医嘱已删除');
  } catch (error) {
    showError(error);
  }
}

async function auditOrderAction(row) {
  try {
    await updateMedicalOrderAuditResult(row.id, { auditResult: 'approved', auditOpinion: '医嘱审核通过' });
    await loadOrders();
    ElMessage.success('医嘱审核通过');
  } catch (error) {
    showError(error);
  }
}

async function executeOrderAction(row) {
  try {
    await executeMedicalOrder(row.id, { nurseId: 1, nurseName: '护士演示', executionResult: '已执行', remark: '按时完成' });
    await loadOrders();
    ElMessage.success('医嘱已执行');
  } catch (error) {
    showError(error);
  }
}

async function createPrescriptionAction() {
  try {
    await createPrescription(prescriptionForm.value);
    await loadPrescriptions();
    ElMessage.success('处方已新增');
  } catch (error) {
    showError(error);
  }
}

async function loadPrescriptions() {
  try {
    applyPageData(prescriptions, 'prescriptions', unwrap(await fetchPrescriptions({
      page: pagers.prescriptions.page,
      limit: pagers.prescriptions.limit
    })));
  } catch (error) {
    showError(error);
  }
}

function selectPrescription(row) {
  selectedPrescription.value = row;
  prescriptionForm.value = {
    patientId: row?.patientId || 1,
    patientName: row?.patientName || '',
    doctorId: row?.doctorId || 1,
    doctorName: row?.doctorName || '',
    medicineName: row?.medicineName || '',
    quantity: row?.quantity || 1,
    usageText: row?.usageText || '每日一次'
  };
}

async function updatePrescriptionAction() {
  if (!selectedPrescription.value?.id) {
    ElMessage.warning('请先选择一条处方');
    return;
  }

  try {
    await updatePrescription(selectedPrescription.value.id, prescriptionForm.value);
    await loadPrescriptions();
    ElMessage.success('处方已更新');
  } catch (error) {
    showError(error);
  }
}

async function deletePrescriptionAction() {
  if (!selectedPrescription.value?.id) {
    ElMessage.warning('请先选择一条处方');
    return;
  }

  try {
    await deletePrescription(selectedPrescription.value.id);
    await loadPrescriptions();
    selectedPrescription.value = null;
    ElMessage.success('处方已删除');
  } catch (error) {
    showError(error);
  }
}

async function createTestRequestAction() {
  try {
    await createTestRequest(testRequestForm.value);
    await loadTestRequests();
    ElMessage.success('检查申请已新增');
  } catch (error) {
    showError(error);
  }
}

async function loadTestRequests() {
  try {
    applyPageData(testRequests, 'testRequests', unwrap(await fetchTestRequests({
      page: pagers.testRequests.page,
      limit: pagers.testRequests.limit
    })));
  } catch (error) {
    showError(error);
  }
}

function selectTestRequest(row) {
  selectedTestRequest.value = row;
  testRequestForm.value = {
    patientId: row?.patientId || 1,
    patientName: row?.patientName || '',
    doctorId: row?.doctorId || 1,
    doctorName: row?.doctorName || '',
    testItem: row?.testItem || '',
    testReason: row?.testReason || '',
    resultContent: row?.resultContent || ''
  };
}

async function updateTestRequestAction() {
  if (!selectedTestRequest.value?.id) {
    ElMessage.warning('请先选择一条检查申请');
    return;
  }

  try {
    await updateTestRequest(selectedTestRequest.value.id, testRequestForm.value);
    await loadTestRequests();
    ElMessage.success('检查申请已更新');
  } catch (error) {
    showError(error);
  }
}

async function deleteTestRequestAction() {
  if (!selectedTestRequest.value?.id) {
    ElMessage.warning('请先选择一条检查申请');
    return;
  }

  try {
    await deleteTestRequest(selectedTestRequest.value.id);
    await loadTestRequests();
    selectedTestRequest.value = null;
    ElMessage.success('检查申请已删除');
  } catch (error) {
    showError(error);
  }
}

async function auditTestAction(row) {
  try {
    await updateTestAuditResult(row.id, { auditResult: 'approved', auditOpinion: '检查申请通过' });
    await loadTestRequests();
    ElMessage.success('检查已审核');
  } catch (error) {
    showError(error);
  }
}

async function createWorkflowTaskAction() {
  try {
    await createWorkflowTask(workflowForm.value);
    await loadWorkflowTasks();
    ElMessage.success('审核任务已创建');
  } catch (error) {
    showError(error);
  }
}

async function loadWorkflowTasks() {
  try {
    applyPageData(workflowTasks, 'workflowTasks', unwrap(await fetchWorkflowTasks({
      page: pagers.workflowTasks.page,
      limit: pagers.workflowTasks.limit
    })));
  } catch (error) {
    showError(error);
  }
}

async function loadWorkflowAuditRecords() {
  try {
    applyPageData(workflowAuditRecords, 'workflowAuditRecords', unwrap(await fetchAuditRecords({
      page: pagers.workflowAuditRecords.page,
      limit: pagers.workflowAuditRecords.limit
    })));
  } catch (error) {
    showError(error);
  }
}

async function auditWorkflowAction(row) {
  try {
    await auditWorkflowTask(row.id, { auditResult: 'approved', auditorId: 1, auditorName: '主任演示', auditOpinion: '审批通过' });
    await loadWorkflowTasks();
    loadWorkflowAuditRecords();
    ElMessage.success('流程已审核');
  } catch (error) {
    showError(error);
  }
}

async function loadArchiveApplications() {
  try {
    applyPageData(archiveApplications, 'archiveApplications', unwrap(await fetchArchiveApplications({
      page: pagers.archiveApplications.page,
      limit: pagers.archiveApplications.limit
    })));
  } catch (error) {
    showError(error);
  }
}

async function loadArchives() {
  try {
    applyPageData(archives, 'archives', unwrap(await fetchArchives({
      page: pagers.archives.page,
      limit: pagers.archives.limit
    })));
  } catch (error) {
    showError(error);
  }
}

async function auditArchiveAction(row) {
  try {
    await auditArchiveApplication(row.id, {
      auditResult: 'approved',
      auditUserId: 1,
      auditUserName: '主任演示',
      auditOpinion: '同意归档',
      archiveContent: '病历归档完成'
    });
    await loadArchiveApplications();
    loadArchives();
    loadRecords();
    ElMessage.success('病历已归档');
  } catch (error) {
    showError(error);
  }
}

async function createFeeAction() {
  try {
    await createFee(feeForm.value);
    await loadFees();
    ElMessage.success('费用已新增');
  } catch (error) {
    showError(error);
  }
}

async function loadFees() {
  try {
    applyPageData(fees, 'fees', unwrap(await fetchFees({
      page: pagers.fees.page,
      limit: pagers.fees.limit
    })));
  } catch (error) {
    showError(error);
  }
}

async function payFeeAction(row) {
  try {
    await payFee(row.id);
    await loadFees();
    ElMessage.success('费用已支付');
  } catch (error) {
    showError(error);
  }
}

async function createNewsAction() {
  try {
    await createNews(newsForm.value);
    await loadNews();
    ElMessage.success('资讯已发布');
  } catch (error) {
    showError(error);
  }
}

async function loadNews() {
  try {
    applyPageData(newsItems, 'newsItems', unwrap(await fetchNews({
      page: pagers.newsItems.page,
      limit: pagers.newsItems.limit
    })));
  } catch (error) {
    showError(error);
  }
}

async function loadMessages() {
  try {
    applyPageData(messages, 'messages', unwrap(await fetchMessages({
      page: pagers.messages.page,
      limit: pagers.messages.limit
    })));
  } catch (error) {
    showError(error);
  }
}

async function replyMessageAction(row) {
  try {
    await replyMessage(row.id, { replyContent: '请遵医嘱用药，如有不适及时复诊。', replyUserId: 1, replyUserName: '管理员' });
    await loadMessages();
    ElMessage.success('留言已回复');
  } catch (error) {
    showError(error);
  }
}

async function createCarouselAction() {
  try {
    await createCarousel(carouselForm.value);
    await loadCarousels();
    ElMessage.success('轮播图已新增');
  } catch (error) {
    showError(error);
  }
}

async function loadCarousels() {
  try {
    applyPageData(carousels, 'carousels', unwrap(await fetchCarousels({
      page: pagers.carousels.page,
      limit: pagers.carousels.limit
    })));
  } catch (error) {
    showError(error);
  }
}

async function deleteCarouselAction(row) {
  try {
    await deleteCarousel(row.id);
    await loadCarousels();
    ElMessage.success('轮播图已停用');
  } catch (error) {
    showError(error);
  }
}

async function saveConfigAction() {
  try {
    await saveConfig(configForm.value);
    ElMessage.success('配置已保存');
  } catch (error) {
    showError(error);
  }
}

/**
 * 保存角色菜单。
 * 菜单 JSON 先按原样保存，方便课程演示时直接展示不同角色的菜单配置结果。
 */
async function saveMenuAction() {
  try {
    menuSnapshot.value = unwrap(await saveMenu(menuForm.value));
    ElMessage.success('菜单已保存');
  } catch (error) {
    showError(error);
  }
}

/**
 * 读取角色菜单。
 * 首次没有配置菜单时允许静默跳过，避免后台首次打开工作台就因为缺省菜单而连续弹错。
 */
async function loadMenuAction({ silent = false } = {}) {
  try {
    const data = unwrap(await fetchMenu(menuForm.value.roleCode));
    menuSnapshot.value = data;
    menuForm.value.name = data.name || menuForm.value.name;
    menuForm.value.menujson = data.menujson || menuForm.value.menujson;
  } catch (error) {
    if (!silent) {
      showError(error);
    }
  }
}

async function loadSyslogs() {
  try {
    applyPageData(syslogs, 'syslogs', unwrap(await fetchSyslogs({
      page: pagers.syslogs.page,
      limit: pagers.syslogs.limit
    })));
  } catch (error) {
    showError(error);
  }
}

async function runOcrAction() {
  try {
    aiResult.value = JSON.stringify(unwrap(await ocrMedicalRecord({ fileUrl: aiForm.value.fileUrl })), null, 2);
  } catch (error) {
    showError(error);
  }
}

async function runMedicineAction() {
  try {
    aiResult.value = JSON.stringify(unwrap(await recommendMedicine({ diagnosis: aiForm.value.diagnosis })), null, 2);
  } catch (error) {
    showError(error);
  }
}

async function runAuditAction() {
  try {
    aiResult.value = JSON.stringify(unwrap(await auditPrescription({ prescriptionText: aiForm.value.prescriptionText })), null, 2);
  } catch (error) {
    showError(error);
  }
}

/**
 * 执行 AI 智能搜索。
 * 关键词为空时直接拦截，避免向后端提交无意义请求，同时让演示结果更贴近真实检索入口。
 */
async function runSmartSearchAction() {
  if (!aiForm.value.searchKeyword) {
    ElMessage.warning('请输入搜索关键词');
    return;
  }

  try {
    aiResult.value = JSON.stringify(unwrap(await smartSearch({ keyword: aiForm.value.searchKeyword })), null, 2);
  } catch (error) {
    showError(error);
  }
}

function replaceRow(rows, row) {
  const index = rows.findIndex((item) => item.id === row.id);
  if (index >= 0) {
    rows[index] = row;
  }
}

/**
 * 统一加载后台工作台数据。
 * 登录成功或刷新后 Token 校验通过时批量拉取，避免未登录状态下无意义地触发一串 401。
 */
function loadAdminWorkspaceData() {
  loadAppointments();
  loadDepartments();
  loadUsers();
  loadRecords();
  loadTriageRecords();
  loadAdmissions();
  loadDischarges();
  loadTemplates();
  loadOrders();
  loadPrescriptions();
  loadTestRequests();
  loadWorkflowTasks();
  loadWorkflowAuditRecords();
  loadArchiveApplications();
  loadArchives();
  loadFees();
  loadNews();
  loadMessages();
  loadCarousels();
  loadMenuAction({ silent: true });
  loadSyslogs();
}

/**
 * 处理后台统一认证事件。
 * 401 代表登录态失效，需要清空会话；403 只提示当前角色无权访问，保留现有登录态。
 */
function handleAdminAuthEvent(event) {
  const status = event?.detail?.status;
  if (status === 401) {
    resetAdminAuth();
    ElMessage.warning(event?.detail?.message || '登录状态已过期，请重新登录');
    return;
  }
  if (status === 403) {
    ElMessage.warning(event?.detail?.message || '当前角色无权访问该功能');
  }
}

onMounted(async () => {
  window.addEventListener(AUTH_EVENT_NAME, handleAdminAuthEvent);
  restoreAdminSession();
  if (await verifyAdminSession()) {
    loadAdminWorkspaceData();
  }
});

onBeforeUnmount(() => {
  window.removeEventListener(AUTH_EVENT_NAME, handleAdminAuthEvent);
});
</script>

<style scoped>
.auth-page {
  min-height: 100vh;
  display: grid;
  place-items: center;
  padding: 24px;
  background:
    linear-gradient(135deg, rgba(16, 32, 51, 0.12), rgba(15, 118, 110, 0.1)),
    #f4f7fb;
  color: #1f2937;
}

.auth-card {
  width: min(460px, 100%);
  padding: 28px;
  background: #ffffff;
  border: 1px solid #dbe3ee;
  border-radius: 8px;
  box-shadow: 0 18px 50px rgba(15, 23, 42, 0.14);
}

.auth-brand {
  margin-bottom: 22px;
}

.auth-brand h1 {
  margin: 0;
  font-size: 28px;
  letter-spacing: 0;
}

.auth-submit {
  width: 100%;
}

.admin-shell {
  display: grid;
  grid-template-columns: 220px minmax(0, 1fr);
  min-height: 100vh;
  background: #f4f7fb;
  color: #1f2937;
}

.sidebar {
  padding: 24px 18px;
  background: #102033;
  color: #ffffff;
}

.sidebar h1 {
  margin: 0 0 24px;
  font-size: 22px;
  letter-spacing: 0;
}

.sidebar nav {
  display: grid;
  gap: 8px;
}

.sidebar button {
  width: 100%;
  min-height: 40px;
  padding: 0 12px;
  color: #cbd5e1;
  text-align: left;
  background: transparent;
  border: 0;
  border-radius: 6px;
  cursor: pointer;
}

.sidebar button.active,
.sidebar button:hover {
  color: #ffffff;
  background: #1f6f78;
}

.workspace {
  min-width: 0;
  padding: 24px;
}

.workspace-head,
.session,
.button-row,
.panel-head,
.table-pagination {
  display: flex;
  align-items: center;
  gap: 10px;
}

.workspace-head {
  justify-content: space-between;
  margin-bottom: 18px;
}

.session {
  justify-content: flex-end;
}

.eyebrow {
  margin: 0 0 6px;
  color: #0f766e;
  font-size: 14px;
}

h2,
h3 {
  margin: 0;
  letter-spacing: 0;
}

h2 {
  font-size: 28px;
}

h3 {
  margin-bottom: 16px;
  font-size: 18px;
}

.workspace-tabs {
  padding: 18px;
  background: #ffffff;
  border: 1px solid #dbe3ee;
  border-radius: 8px;
}

.dashboard-layout {
  display: grid;
  gap: 18px;
}

.overview-grid {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 12px;
}

.overview-card {
  display: grid;
  gap: 8px;
  padding: 16px;
  background: #f8fbff;
  border: 1px solid #dbe7f3;
  border-radius: 8px;
}

.overview-label {
  color: #475569;
  font-size: 13px;
}

.overview-value {
  font-size: 28px;
  font-weight: 700;
}

.two-column,
.three-column {
  display: grid;
  gap: 18px;
  align-items: start;
}

.two-column {
  grid-template-columns: minmax(300px, 420px) minmax(430px, 1fr);
}

.three-column {
  grid-template-columns: minmax(260px, 340px) minmax(260px, 340px) minmax(430px, 1fr);
}

.panel {
  min-width: 0;
  padding: 18px;
  background: #fbfcff;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
}

.wide-panel {
  overflow: hidden;
}

.panel-head {
  justify-content: space-between;
  margin-bottom: 12px;
}

.panel-head h3 {
  margin-bottom: 0;
}

.table-pagination {
  justify-content: flex-end;
  margin-top: 10px;
  flex-wrap: wrap;
}

.menu-preview {
  margin-top: 12px;
}

.result-box {
  min-height: 320px;
  max-height: 460px;
  overflow: auto;
  padding: 14px;
  margin: 0;
  background: #111827;
  color: #d1fae5;
  border-radius: 8px;
  white-space: pre-wrap;
}

@media (max-width: 1060px) {
  .admin-shell,
  .two-column,
  .three-column {
    grid-template-columns: 1fr;
  }

  .overview-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .sidebar nav {
    grid-template-columns: repeat(auto-fit, minmax(120px, 1fr));
  }
}

@media (max-width: 760px) {
  .auth-card {
    padding: 20px;
  }

  .workspace,
  .sidebar {
    padding: 16px;
  }

  .workspace-head {
    display: grid;
  }

  .session {
    justify-content: flex-start;
  }

  .overview-grid {
    grid-template-columns: 1fr;
  }
}
</style>
