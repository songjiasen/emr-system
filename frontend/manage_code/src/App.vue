<template>
  <main v-if="!hasAdminToken" class="auth-page admin-auth-page">
    <section class="auth-card">
      <div class="auth-brand">
        <span class="brand-icon">✚</span>
        <h1>安心医疗系统</h1>
        <p>请输入管理员账号进入后台管理</p>
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
      <h1><span class="brand-icon">✚</span> 安心医疗系统</h1>
      <nav>
        <template v-for="group in navGroups" :key="group.label">
          <button
            v-if="!group.children"
            :class="{ active: activeTab === group.name }"
            type="button"
            @click="goToAdminTab(group.name)"
          >
            {{ group.label }}
          </button>
          <section v-else class="sidebar-group">
            <p>{{ group.label }}</p>
            <button
              v-for="child in group.children"
              :key="child.name"
              class="sidebar-child"
              :class="{ active: activeTab === child.name }"
              type="button"
              @click="goToAdminTab(child.name)"
            >
              {{ child.label }}
            </button>
          </section>
        </template>
      </nav>
    </aside>

    <section class="workspace">
      <header class="workspace-head">
        <div>
          <p class="eyebrow">管理端</p>
          <h2>安心医疗系统</h2>
        </div>
        <div class="session">
          <el-tag type="success">系统正常</el-tag>
          <span>{{ adminSession.username }}</span>
          <el-tag>{{ adminSession.roleCode }}</el-tag>
          <el-button v-if="hasAdminToken" link type="primary" @click="logoutAdminAction">退出</el-button>
        </div>
      </header>

      <el-tabs v-model="activeTab" class="workspace-tabs" @tab-change="goToAdminTab">
        <el-tab-pane v-if="canAccessAdminTab('dashboard')" label="仪表盘" name="dashboard">
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

        <el-tab-pane v-if="canAccessAdminTab('appointments')" label="预约管理" name="appointments">
          <section class="two-column">
            <div class="panel">
              <h3>预约筛选</h3>
              <el-form label-position="top" :model="appointmentFilterForm">
                <el-form-item label="患者">
                  <el-select v-model="appointmentFilterForm.patientId" clearable filterable placeholder="选择患者">
                    <el-option
                      v-for="item in patientOptions"
                      :key="item.id"
                      :label="relationLabel(item, 'patient')"
                      :value="item.id"
                    />
                  </el-select>
                </el-form-item>
                <el-form-item label="医生">
                  <el-select v-model="appointmentFilterForm.doctorId" clearable filterable placeholder="选择医生">
                    <el-option
                      v-for="item in doctorOptions"
                      :key="item.id"
                      :label="relationLabel(item, 'doctor')"
                      :value="item.id"
                    />
                  </el-select>
                </el-form-item>
                <el-form-item label="状态">
                  <el-select v-model="appointmentFilterForm.status" clearable placeholder="全部状态">
                    <el-option label="待就诊" value="pending" />
                    <el-option label="已确认" value="confirmed" />
                    <el-option label="已取消" value="cancelled" />
                    <el-option label="已完成" value="finished" />
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
                <el-table-column label="操作" width="180">
                  <template #default="{ row }">
                    <el-button v-if="isNurseRole()" link type="primary" :disabled="row.status !== 'pending'" @click.stop="openTriageFromAppointment(row)">分诊</el-button>
                    <el-button v-else-if="isDoctorRole() && row.status === 'pending'" link type="primary" @click.stop="confirmAppointmentAction(row)">确认</el-button>
                    <el-button v-else link type="danger" :disabled="row.status === 'cancelled'" @click.stop="cancelAppointmentAction(row)">取消</el-button>
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

        <el-tab-pane v-if="canAccessAdminTab('departments')" label="科室管理" name="departments">
          <section class="single-column">
            <div class="panel wide-panel">
              <div class="panel-head">
                <h3>科室列表</h3>
                <div class="button-row">
                  <el-button type="primary" @click="openDepartmentDialog('create')">新增科室</el-button>
                  <el-button :disabled="!selectedDepartment" @click="openDepartmentDialog('edit')">编辑科室</el-button>
                  <el-button :disabled="!selectedDepartment" type="danger" plain @click="deleteDepartmentAction">删除科室</el-button>
                  <el-button @click="loadDepartments">刷新</el-button>
                </div>
              </div>
              <el-table :data="departments" height="360" @row-click="selectDepartment">
                <el-table-column prop="name" label="科室" min-width="130" />
                <el-table-column prop="sortNo" label="排序" width="80" />
                <el-table-column prop="status" label="状态" width="90" :formatter="statusFormatter" />
                <el-table-column label="操作" width="90">
                  <template #default="{ row }">
                    <el-button link type="primary" @click.stop="openDepartmentDialog('edit', row)">编辑</el-button>
                  </template>
                </el-table-column>
              </el-table>
              <el-pagination
                class="table-pagination"
                layout="prev, pager, next, total"
                :current-page="pagers.departments.page"
                :page-size="pagers.departments.limit"
                :total="pagers.departments.total"
                @current-change="(page) => changePage('departments', page, loadDepartments)"
              />
            </div>

            <el-dialog v-model="adminDialogs.department" :title="departmentDialogMode === 'edit' ? '编辑科室' : '新增科室'" width="520px">
              <el-form class="dialog-form" label-position="top" :model="departmentForm">
                <el-form-item label="科室名称">
                  <el-input v-model="departmentForm.name" />
                </el-form-item>
                <el-form-item label="排序">
                  <el-input-number v-model="departmentForm.sortNo" :min="1" />
                </el-form-item>
                <div class="dialog-footer">
                  <el-button @click="adminDialogs.department = false">取消</el-button>
                  <el-button v-if="departmentDialogMode === 'edit'" type="primary" @click="updateDepartmentAction">保存修改</el-button>
                  <el-button v-else type="primary" @click="createDepartmentAction">确认新增</el-button>
                </div>
              </el-form>
            </el-dialog>

            <div v-if="isDirectorOrAdmin" class="panel" style="margin-top:16px">
              <div class="panel-head">
                <h3>检查项目管理</h3>
                <div class="button-row">
                  <el-button type="primary" @click="openTestItemDialog('create')">新增检查项目</el-button>
                  <el-button @click="loadTestItems">刷新</el-button>
                </div>
              </div>
              <el-table :data="testItems" height="260" @row-click="selectTestItem">
                <el-table-column prop="itemName" label="项目名称" min-width="120" />
                <el-table-column prop="departmentName" label="负责科室" width="100" />
                <el-table-column prop="unitPrice" label="单价" width="80" />
                <el-table-column label="操作" width="120">
                  <template #default="{ row }">
                    <el-button link type="primary" @click.stop="openTestItemDialog('edit', row)">编辑</el-button>
                    <el-button link type="danger" @click.stop="selectTestItem(row); deleteTestItemAction()">禁用</el-button>
                  </template>
                </el-table-column>
              </el-table>
            </div>

            <el-dialog v-model="adminDialogs.testItem" :title="testItemDialogMode === 'edit' ? '编辑检查项目' : '新增检查项目'" width="520px">
              <el-form class="dialog-form" label-position="top" :model="testItemForm">
                <el-form-item label="项目名称">
                  <el-input v-model="testItemForm.itemName" />
                </el-form-item>
                <el-form-item label="负责科室">
                  <el-select v-model="testItemForm.departmentId" filterable placeholder="选择科室" @change="onTestItemDeptChange">
                    <el-option
                      v-for="item in departments"
                      :key="item.id"
                      :label="item.name"
                      :value="item.id"
                    />
                  </el-select>
                </el-form-item>
                <el-form-item label="单价 (元)">
                  <el-input-number v-model="testItemForm.unitPrice" :precision="2" :min="0" style="width:100%" />
                </el-form-item>
                <el-form-item label="排序">
                  <el-input-number v-model="testItemForm.sortOrder" :min="0" style="width:100%" />
                </el-form-item>
                <div class="dialog-footer">
                  <el-button @click="adminDialogs.testItem = false">取消</el-button>
                  <el-button v-if="testItemDialogMode === 'edit'" type="primary" @click="updateTestItemAction">保存修改</el-button>
                  <el-button v-else type="primary" @click="createTestItemAction">确认新增</el-button>
                </div>
              </el-form>
            </el-dialog>
          </section>
        </el-tab-pane>

        <el-tab-pane v-if="canAccessAdminTab('managed-users')" label="人员管理" name="managed-users">
          <section class="single-column">
            <div class="panel wide-panel">
              <div class="panel-head">
                <h3>人员列表</h3>
                <div class="button-row">
                  <el-button type="primary" @click="openUserDialog('create')">新增人员</el-button>
                  <el-button :disabled="!selectedUser" @click="openUserDialog('edit')">编辑人员</el-button>
                  <el-button :disabled="!selectedUser" type="danger" plain @click="deleteUserAction">删除人员</el-button>
                  <el-button @click="loadUsers">刷新</el-button>
                </div>
              </div>
              <el-table :data="users" height="360" @row-click="selectUser">
                <el-table-column prop="username" label="账号" min-width="130" />
                <el-table-column prop="name" label="姓名" min-width="100" />
                <el-table-column prop="userType" label="类型" width="110" />
                <el-table-column prop="departmentName" label="科室" min-width="110" />
                <el-table-column prop="status" label="状态" width="90" :formatter="statusFormatter" />
                <el-table-column label="操作" width="90">
                  <template #default="{ row }">
                    <el-button link type="primary" @click.stop="openUserDialog('edit', row)">编辑</el-button>
                  </template>
                </el-table-column>
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

            <el-dialog v-model="adminDialogs.user" :title="userDialogMode === 'edit' ? '编辑人员' : '新增人员'" width="560px">
              <el-form class="dialog-form" label-position="top" :model="userForm">
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
                <el-form-item v-if="userNeedsDepartment()" label="所属科室">
                  <el-select v-model="userForm.departmentId" @change="syncUserDepartment">
                    <el-option
                      v-for="department in departments"
                      :key="department.id"
                      :label="department.name"
                      :value="department.id"
                    />
                  </el-select>
                </el-form-item>
                <div class="dialog-footer">
                  <el-button @click="adminDialogs.user = false">取消</el-button>
                  <el-button v-if="userDialogMode === 'edit'" type="primary" @click="updateUserAction">保存修改</el-button>
                  <el-button v-else type="primary" @click="createUserAction">确认新增</el-button>
                </div>
              </el-form>
            </el-dialog>
          </section>
        </el-tab-pane>

        <el-tab-pane v-if="canAccessAdminTab('records')" label="病历列表" name="records">
          <section class="single-column">
            <div class="panel wide-panel">
              <div class="panel-head">
                <h3>病历列表</h3>
                <div class="button-row">
                  <el-button type="primary" @click="openRecordDialog('create')">新增病历</el-button>
                  <el-button :disabled="!selectedRecord" @click="openRecordDialog('edit')">编辑病历</el-button>
                  <el-button :disabled="!selectedRecord" type="danger" plain @click="deleteRecordAction">删除病历</el-button>
                  <el-button :disabled="!selectedRecord || selectedRecord.archiveStatus === 'pending' || selectedRecord.archiveStatus === 'archived'" @click="createArchiveApplicationAction">申请归档</el-button>
                  <el-button @click="loadRecords">刷新</el-button>
                </div>
              </div>
              <el-table :data="records" height="360" @row-click="selectRecord">
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

              <div v-if="selectedRecord" class="record-relations-panel" v-loading="loadingRecordRelations">
                <h4 class="relations-title">本次就诊关联事项</h4>
                <div class="relations-grid">
                  <div class="relation-card">
                    <div class="relation-card-header">
                      <span class="relation-icon">🔬</span>
                      <span>检查申请</span>
                      <el-tag v-if="recordRelatedTests.length === 0" type="info" size="small">无</el-tag>
                      <el-tag v-else-if="recordRelatedTests.every(t => t.status === 'finished')" type="success" size="small">全部完成</el-tag>
                      <el-tag v-else type="warning" size="small">{{ recordRelatedTests.filter(t => t.status !== 'finished').length }} 项待处理</el-tag>
                    </div>
                    <div v-if="recordRelatedTests.length > 0" class="relation-items">
                      <div v-for="item in recordRelatedTests" :key="item.id" class="relation-item">
                        <span class="relation-item-name">{{ item.testItem || '-' }}</span>
                        <el-tag :type="item.status === 'finished' ? 'success' : item.status === 'paid' ? 'primary' : 'warning'" size="small">{{ statusText(item.status) }}</el-tag>
                      </div>
                    </div>
                  </div>
                  <div class="relation-card">
                    <div class="relation-card-header">
                      <span class="relation-icon">💊</span>
                      <span>医嘱</span>
                      <el-tag v-if="recordRelatedOrders.length === 0" type="info" size="small">无</el-tag>
                      <el-tag v-else-if="recordRelatedOrders.every(o => o.status === 'executed')" type="success" size="small">全部执行</el-tag>
                      <el-tag v-else type="warning" size="small">{{ recordRelatedOrders.filter(o => o.status !== 'executed').length }} 条待处理</el-tag>
                    </div>
                    <div v-if="recordRelatedOrders.length > 0" class="relation-items">
                      <div v-for="item in recordRelatedOrders" :key="item.id" class="relation-item">
                        <span class="relation-item-name">{{ item.content || '-' }}</span>
                        <el-tag :type="item.status === 'executed' ? 'success' : 'warning'" size="small">{{ statusText(item.status) }}</el-tag>
                      </div>
                    </div>
                  </div>
                  <div class="relation-card">
                    <div class="relation-card-header">
                      <span class="relation-icon">📋</span>
                      <span>处方</span>
                      <el-tag v-if="recordRelatedPrescriptions.length === 0" type="info" size="small">无</el-tag>
                      <el-tag v-else-if="recordRelatedPrescriptions.every(p => p.status === 'paid' || p.status === 'dispensed')" type="success" size="small">全部就绪</el-tag>
                      <el-tag v-else type="warning" size="small">{{ recordRelatedPrescriptions.filter(p => p.status !== 'paid' && p.status !== 'dispensed').length }} 张待处理</el-tag>
                    </div>
                    <div v-if="recordRelatedPrescriptions.length > 0" class="relation-items">
                      <div v-for="item in recordRelatedPrescriptions" :key="item.id" class="relation-item">
                        <span class="relation-item-name">{{ item.medicineName || '-' }}</span>
                        <el-tag :type="item.status === 'paid' || item.status === 'dispensed' ? 'success' : 'warning'" size="small">{{ statusText(item.status) }}</el-tag>
                      </div>
                    </div>
                  </div>
                </div>
                <div v-if="selectedRecord.archiveStatus === 'archived'" class="archive-complete-tip">
                  <el-tag type="success" effect="dark">📁 已归档</el-tag>
                </div>
              </div>
            </div>

            <el-dialog v-model="adminDialogs.record" :title="recordDialogMode === 'edit' ? '编辑病历' : '新增病历'" width="620px">
              <el-form class="dialog-form" label-position="top" :model="recordForm">
                <div class="record-relation-stack">
                  <el-form-item label="预约">
                    <el-select v-model="recordForm.appointmentId" clearable filterable placeholder="选择预约" @change="syncAppointmentToRecord">
                      <el-option
                        v-for="item in appointmentOptions"
                        :key="item.id"
                        :label="appointmentLabel(item)"
                        :value="item.id"
                      />
                    </el-select>
                  </el-form-item>
                  <el-form-item v-if="recordForm.appointmentId" label="预约号">
                    <el-input v-model="recordForm.appointmentNo" disabled />
                  </el-form-item>
                  <el-form-item v-if="!recordForm.appointmentId" label="患者">
                    <el-select v-model="recordForm.patientId" filterable placeholder="选择患者" @change="(value) => syncPatientToForm(recordForm, value)">
                      <el-option
                        v-for="item in patientOptions"
                        :key="item.id"
                        :label="relationLabel(item, 'patient')"
                        :value="item.id"
                      />
                    </el-select>
                  </el-form-item>
                  <el-form-item v-else label="患者">
                    <el-input v-model="recordForm.patientName" disabled />
                  </el-form-item>
                  <el-form-item v-if="!recordForm.appointmentId" label="医生">
                    <el-select v-model="recordForm.doctorId" filterable placeholder="选择医生" @change="(value) => syncDoctorToForm(recordForm, value)">
                      <el-option
                        v-for="item in doctorOptions"
                        :key="item.id"
                        :label="relationLabel(item, 'doctor')"
                        :value="item.id"
                      />
                    </el-select>
                  </el-form-item>
                  <el-form-item v-else label="医生">
                    <el-input v-model="recordForm.doctorName" disabled />
                  </el-form-item>
                </div>
                <el-form-item label="就诊时间">
                  <el-date-picker
                    v-model="recordForm.visitTime"
                    type="datetime"
                    format="YYYY-MM-DD HH:mm:ss"
                    value-format="YYYY-MM-DD HH:mm:ss"
                    placeholder="选择就诊时间"
                    style="width:100%"
                  />
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
                <div class="dialog-footer">
                  <el-button @click="adminDialogs.record = false">取消</el-button>
                  <el-button v-if="recordDialogMode === 'edit'" type="primary" @click="updateRecordAction">保存修改</el-button>
                  <el-button v-else type="primary" @click="createRecordAction">确认新增</el-button>
                </div>
              </el-form>
            </el-dialog>
          </section>
        </el-tab-pane>

        <el-tab-pane v-if="canAccessAdminTab('triage')" label="分诊记录" name="triage">
          <section class="single-column">
            <div class="panel wide-panel">
              <div class="panel-head">
                <h3>分诊记录</h3>
                <div class="button-row">
                  <el-button type="primary" @click="openTriageDialog()">新增分诊</el-button>
                  <el-button @click="loadTriageRecords">刷新</el-button>
                </div>
              </div>
              <el-table :data="triageRecords" height="360">
                <el-table-column prop="triageNo" label="分诊号" min-width="130" />
                <el-table-column prop="patientName" label="患者" width="120" />
                <el-table-column prop="chiefComplaint" label="主诉" min-width="180" />
                <el-table-column prop="triageLevel" label="级别" width="100" />
              </el-table>
              <el-pagination
                class="table-pagination"
                layout="prev, pager, next, total"
                :current-page="pagers.triageRecords.page"
                :page-size="pagers.triageRecords.limit"
                :total="pagers.triageRecords.total"
                @current-change="(page) => changePage('triageRecords', page, loadTriageRecords)"
              />
            </div>
          </section>
        </el-tab-pane>

        <el-tab-pane v-if="canAccessAdminTab('admissions')" label="入院管理" name="admissions">
          <section class="single-column">
            <div class="panel wide-panel">
              <div class="panel-head">
                <h3>入院列表</h3>
                <div class="button-row">
                  <el-button type="primary" @click="openAdmissionDialog()">登记入院</el-button>
                  <el-button @click="loadAdmissions">刷新</el-button>
                </div>
              </div>
              <el-table :data="admissions" height="360">
                <el-table-column prop="businessNo" label="住院号" min-width="130" />
                <el-table-column prop="patientName" label="患者" min-width="90" />
                <el-table-column prop="bedNo" label="床位" width="100" />
                <el-table-column prop="status" label="状态" width="110" :formatter="statusFormatter" />
                <el-table-column label="操作" width="130">
                  <template #default="{ row }">
                    <el-button link type="primary" @click.stop="openDischargeDialog(row)">办理出院</el-button>
                  </template>
                </el-table-column>
              </el-table>
              <el-pagination
                class="table-pagination"
                layout="prev, pager, next, total"
                :current-page="pagers.admissions.page"
                :page-size="pagers.admissions.limit"
                :total="pagers.admissions.total"
                @current-change="(page) => changePage('admissions', page, loadAdmissions)"
              />
            </div>

          </section>
        </el-tab-pane>

        <el-tab-pane v-if="canAccessAdminTab('discharges')" label="出院记录" name="discharges">
          <section class="single-column">
            <div class="panel wide-panel">
              <div class="panel-head">
                <h3>出院记录</h3>
                <el-button @click="loadDischarges">刷新</el-button>
              </div>
              <el-table :data="discharges" height="360">
                <el-table-column prop="dischargeNo" label="出院号" min-width="130" />
                <el-table-column prop="patientName" label="患者" width="110" />
                <el-table-column prop="dischargeTime" label="出院时间" min-width="150" />
                <el-table-column prop="dischargeReason" label="出院原因" min-width="160" />
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
          </section>
        </el-tab-pane>

        <el-tab-pane v-if="canAccessAdminTab('templates')" label="病历模板" name="templates">
          <section class="single-column">
            <div class="panel wide-panel">
              <div class="panel-head">
                <h3>病历模板</h3>
                <div class="button-row">
                  <el-button type="primary" @click="openTemplateDialog('create')">新增模板</el-button>
                  <el-button :disabled="!selectedTemplate" @click="openTemplateDialog('edit')">编辑模板</el-button>
                  <el-button :disabled="!selectedTemplate" type="danger" plain @click="deleteTemplateAction">删除模板</el-button>
                  <el-button :disabled="!selectedTemplate" @click="applyTemplateToRecord">套用到病历</el-button>
                  <el-button @click="loadTemplates">刷新</el-button>
                </div>
              </div>
              <el-table :data="templates" height="180" @row-click="selectTemplate">
                <el-table-column prop="templateName" label="模板" min-width="140" />
                <el-table-column prop="templateType" label="类型" width="100" />
                <el-table-column prop="status" label="状态" width="90" :formatter="statusFormatter" />
                <el-table-column label="操作" width="90">
                  <template #default="{ row }">
                    <el-button link type="primary" @click.stop="openTemplateDialog('edit', row)">编辑</el-button>
                  </template>
                </el-table-column>
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

            <el-dialog v-model="adminDialogs.template" :title="templateDialogMode === 'edit' ? '编辑模板' : '新增模板'" width="560px">
              <el-form class="dialog-form" label-position="top" :model="templateForm">
                <el-form-item label="模板名称">
                  <el-input v-model="templateForm.templateName" />
                </el-form-item>
                <el-form-item label="模板类型">
                  <el-input v-model="templateForm.templateType" />
                </el-form-item>
                <el-form-item label="模板内容">
                  <el-input v-model="templateForm.content" type="textarea" :rows="3" />
                </el-form-item>
                <div class="dialog-footer">
                  <el-button @click="adminDialogs.template = false">取消</el-button>
                  <el-button v-if="templateDialogMode === 'edit'" type="primary" @click="updateTemplateAction">保存修改</el-button>
                  <el-button v-else type="primary" @click="createTemplateAction">确认新增</el-button>
                </div>
              </el-form>
            </el-dialog>
          </section>
        </el-tab-pane>

        <el-tab-pane v-if="canAccessAdminTab('orders')" label="医嘱管理" name="orders">
          <section class="single-column">
            <div class="panel wide-panel">
              <div class="panel-head">
                <h3>医嘱列表</h3>
                <div class="button-row">
                  <el-button v-if="canAccessAdminTab('orders') && !isNurseOnly" type="primary" @click="openOrderDialog('create')">新增医嘱</el-button>
                  <el-button v-if="canAccessAdminTab('orders') && !isNurseOnly" :disabled="!selectedOrder" @click="openOrderDialog('edit')">编辑医嘱</el-button>
                  <el-button v-if="canAccessAdminTab('orders') && !isNurseOnly" :disabled="!selectedOrder" type="danger" plain @click="deleteOrderAction">删除医嘱</el-button>
                  <el-button @click="loadOrders">刷新</el-button>
                </div>
              </div>
              <el-table :data="orders" height="360" @row-click="selectOrder">
                <el-table-column prop="content" label="医嘱" min-width="160" />
                <el-table-column prop="status" label="状态" width="100" :formatter="statusFormatter" />
                <el-table-column label="操作" width="170">
                  <template #default="{ row }">
                    <el-button v-if="canAccessAdminTab('workflow-audits') && row.status === 'pending_audit'" link type="primary" @click="auditOrderAction(row)">通过</el-button>
                    <el-button v-if="isNurseOnly && row.status === 'approved'" link type="success" @click="executeOrderAction(row)">执行</el-button>
                    <span v-if="isNurseOnly && row.status === 'executed'" class="executed-tag">已执行</span>
                    <el-button v-if="row.status === 'pending_audit' && !isNurseOnly" link @click.stop="openOrderDialog('edit', row)">编辑</el-button>
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
            </div>

            <el-dialog v-model="adminDialogs.order" :title="orderDialogMode === 'edit' ? '编辑医嘱' : '新增医嘱'" width="560px">
              <el-form class="dialog-form" label-position="top" :model="orderForm">
                <el-form-item label="关联病历">
                  <el-select v-model="orderForm.recordId" filterable placeholder="选择病历" @change="(value) => syncRecordToForm(orderForm, value)">
                    <el-option
                      v-for="item in recordOptions"
                      :key="item.id"
                      :label="recordLabel(item)"
                      :value="item.id"
                    />
                  </el-select>
                </el-form-item>
                <el-form-item label="患者">
                  <el-input v-model="orderForm.patientName" disabled />
                </el-form-item>
                <el-form-item label="医生">
                  <el-input v-model="orderForm.doctorName" disabled />
                </el-form-item>
                <el-form-item label="医嘱内容">
                  <el-input v-model="orderForm.content" type="textarea" :rows="3" />
                </el-form-item>
                <div class="dialog-footer">
                  <el-button @click="adminDialogs.order = false">取消</el-button>
                  <el-button v-if="orderDialogMode === 'edit'" type="primary" @click="updateOrderAction">保存修改</el-button>
                  <el-button v-else type="primary" @click="createOrderAction">确认新增</el-button>
                </div>
              </el-form>
            </el-dialog>
          </section>
        </el-tab-pane>

        <el-tab-pane v-if="canAccessAdminTab('prescriptions')" label="处方管理" name="prescriptions">
          <section class="single-column">
            <div class="panel wide-panel">
              <div class="panel-head">
                <h3>处方列表</h3>
                <div class="button-row">
                  <el-button type="primary" @click="openPrescriptionDialog('create')">新增处方</el-button>
                  <el-button :disabled="!selectedPrescription" @click="openPrescriptionDialog('edit')">编辑处方</el-button>
                  <el-button @click="loadPrescriptions">刷新</el-button>
                </div>
              </div>
              <el-table :data="prescriptions" height="360" @row-click="selectPrescription">
                <el-table-column prop="medicineName" label="处方" min-width="150" />
                <el-table-column prop="quantity" label="数量" width="90" />
                <el-table-column prop="status" label="状态" width="100" :formatter="statusFormatter" />
                <el-table-column label="操作" width="150">
                  <template #default="{ row }">
                    <el-button v-if="adminSession.roleCode === 'director' && row.status === 'pending_audit'" link type="primary" @click="auditPrescriptionAction(row)">审核</el-button>
                    <el-button v-if="row.status === 'pending_audit' && adminSession.roleCode !== 'director'" link type="primary" @click.stop="openPrescriptionDialog('edit', row)">编辑</el-button>
                    <el-button v-if="row.status === 'pending_audit' && adminSession.roleCode !== 'director'" link type="danger" @click.stop="selectPrescription(row); deletePrescriptionAction()">删除</el-button>
                  </template>
                </el-table-column>
              </el-table>
              <el-pagination
                class="table-pagination"
                layout="prev, pager, next, total"
                :current-page="pagers.prescriptions.page"
                :page-size="pagers.prescriptions.limit"
                :total="pagers.prescriptions.total"
                @current-change="(page) => changePage('prescriptions', page, loadPrescriptions)"
              />
            </div>

            <el-dialog v-model="adminDialogs.prescription" :title="prescriptionDialogMode === 'edit' ? '编辑处方' : '新增处方'" width="520px">
              <el-form class="dialog-form" label-position="top" :model="prescriptionForm">
                <el-form-item label="关联病历">
                  <el-select v-model="prescriptionForm.recordId" filterable placeholder="选择病历" @change="(value) => syncRecordToForm(prescriptionForm, value)">
                    <el-option
                      v-for="item in recordOptions"
                      :key="item.id"
                      :label="recordLabel(item)"
                      :value="item.id"
                    />
                  </el-select>
                </el-form-item>
                <el-form-item label="患者">
                  <el-input v-model="prescriptionForm.patientName" disabled />
                </el-form-item>
                <el-form-item label="医生">
                  <el-input v-model="prescriptionForm.doctorName" disabled />
                </el-form-item>
                <el-form-item label="药品名称">
                  <el-select v-model="prescriptionForm.medicineName" filterable placeholder="选择药品" @change="onPrescriptionMedicineChange">
                    <el-option
                      v-for="item in medicines"
                      :key="item.id"
                      :label="`${item.medicineName} (${item.specification || '-'} / ${item.unit}) ¥${item.unitPrice}`"
                      :value="item.medicineName"
                    />
                  </el-select>
                </el-form-item>
                <el-form-item label="数量">
                  <el-input-number v-model="prescriptionForm.quantity" :min="1" />
                </el-form-item>
                <el-form-item label="单价 (元)">
                  <el-input-number v-model="prescriptionForm.unitPrice" :precision="2" :min="0" disabled />
                </el-form-item>
                <div class="dialog-footer">
                  <el-button @click="adminDialogs.prescription = false">取消</el-button>
                  <el-button v-if="prescriptionDialogMode === 'edit'" type="primary" @click="updatePrescriptionAction">保存修改</el-button>
                  <el-button v-else type="primary" @click="createPrescriptionAction">确认新增</el-button>
                </div>
              </el-form>
            </el-dialog>
          </section>
        </el-tab-pane>

        <el-tab-pane v-if="canAccessAdminTab('tests')" label="检查申请" name="tests">
          <section class="single-column">
            <div class="panel wide-panel">
              <div class="panel-head">
                <h3>检查申请列表</h3>
                <div class="button-row">
                  <el-button type="primary" @click="openTestRequestDialog('create')">新增检查</el-button>
                  <el-button :disabled="!selectedTestRequest" @click="openTestRequestDialog('edit')">编辑检查</el-button>
                  <el-button @click="loadTestRequests">刷新</el-button>
                </div>
              </div>
              <el-table :data="testRequests" height="360" @row-click="selectTestRequest">
                <el-table-column prop="testItem" label="检查" min-width="150" />
                <el-table-column prop="status" label="状态" width="100" :formatter="statusFormatter" />
                <el-table-column prop="resultContent" label="结果" min-width="150" />
                <el-table-column label="操作" width="100">
                  <template #default="{ row }">
                    <el-button v-if="canAccessAdminTab('workflow-audits') && row.status === 'pending_audit'" link type="primary" @click="auditTestAction(row)">审核</el-button>
                    <el-button v-if="row.status === 'paid'" link type="success" @click.stop="openTestRequestDialog('edit', row)">填写结果</el-button>
                    <el-button v-if="row.status === 'pending_audit'" link @click.stop="openTestRequestDialog('edit', row)">编辑</el-button>
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

            <el-dialog v-model="adminDialogs.testRequest" :title="testRequestDialogMode === 'edit' ? '编辑检查' : '新增检查'" width="560px">
              <el-form class="dialog-form" label-position="top" :model="testRequestForm">
                <el-form-item label="关联病历">
                  <el-select v-model="testRequestForm.recordId" filterable placeholder="选择病历" @change="(value) => syncRecordToForm(testRequestForm, value)">
                    <el-option
                      v-for="item in recordOptions"
                      :key="item.id"
                      :label="recordLabel(item)"
                      :value="item.id"
                    />
                  </el-select>
                </el-form-item>
                <el-form-item label="患者">
                  <el-input v-model="testRequestForm.patientName" disabled />
                </el-form-item>
                <el-form-item label="医生">
                  <el-input v-model="testRequestForm.doctorName" disabled />
                </el-form-item>
                <el-form-item label="检查项目">
                  <el-select v-model="testRequestForm.testItem" filterable placeholder="选择检查项目" @change="onTestItemChange">
                    <el-option
                      v-for="item in testItems"
                      :key="item.id"
                      :label="`${item.itemName}（${item.departmentName || '未指定科室'}） ¥${item.unitPrice}`"
                      :value="item.itemName"
                    />
                  </el-select>
                </el-form-item>
                <el-form-item label="单价 (元)">
                  <el-input-number v-model="testRequestForm.unitPrice" :precision="2" :min="0" disabled />
                </el-form-item>
                <el-form-item label="负责科室">
                  <el-input v-model="testRequestForm.departmentName" disabled placeholder="选择检查项目后自动填入" />
                </el-form-item>
                <el-form-item label="检查原因">
                  <el-input v-model="testRequestForm.testReason" />
                </el-form-item>
                <el-form-item v-if="testRequestDialogMode === 'edit' && (testRequestForm.status === 'paid' || testRequestForm.resultContent)" label="检查结果">
                  <el-input v-model="testRequestForm.resultContent" type="textarea" :rows="3" />
                </el-form-item>
                <div class="dialog-footer">
                  <el-button @click="adminDialogs.testRequest = false">取消</el-button>
                  <el-button v-if="testRequestDialogMode === 'edit'" type="primary" @click="updateTestRequestAction">保存修改</el-button>
                  <el-button v-else type="primary" @click="createTestRequestAction">确认新增</el-button>
                </div>
              </el-form>
            </el-dialog>
          </section>
        </el-tab-pane>

        <el-tab-pane v-if="canAccessAdminTab('workflow-tasks')" label="审核任务" name="workflow-tasks">
          <section class="single-column">
            <div class="panel">
              <div class="panel-head">
                <h3>审核任务</h3>
                <div class="button-row">
                  <el-button type="primary" @click="openWorkflowDialog">创建审核任务</el-button>
                  <el-button @click="loadWorkflowTasks">刷新任务</el-button>
                </div>
              </div>
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
            </div>

            <el-dialog v-model="adminDialogs.workflow" title="创建审核任务" width="560px">
              <el-form class="dialog-form" label-position="top" :model="workflowForm">
                <el-form-item label="业务类型">
                  <el-select v-model="workflowForm.businessType" @change="resetWorkflowBusinessSelection">
                    <el-option label="病历审核" value="medical_record" />
                    <el-option label="医嘱审核" value="medical_order" />
                    <el-option label="检查审核" value="test_request" />
                    <el-option label="归档审核" value="record_archive" />
                  </el-select>
                </el-form-item>
                <el-form-item label="业务记录">
                  <el-select v-model="workflowForm.businessId" filterable placeholder="选择业务记录" @change="syncWorkflowBusiness">
                    <el-option
                      v-for="item in workflowBusinessOptions"
                      :key="item.id"
                      :label="workflowBusinessLabel(item)"
                      :value="item.id"
                    />
                  </el-select>
                </el-form-item>
                <el-form-item label="审核角色">
                  <el-input v-model="workflowForm.assigneeRole" />
                </el-form-item>
                <div class="dialog-footer">
                  <el-button @click="adminDialogs.workflow = false">取消</el-button>
                  <el-button type="primary" @click="createWorkflowTaskAction">确认创建</el-button>
                </div>
              </el-form>
            </el-dialog>
          </section>
        </el-tab-pane>

        <el-tab-pane v-if="canAccessAdminTab('workflow-audits')" label="审核记录" name="workflow-audits">
          <section class="single-column">
            <div class="panel">
              <div class="panel-head">
                <h3>审核记录</h3>
                <el-button @click="loadWorkflowAuditRecords">刷新</el-button>
              </div>
              <el-table :data="workflowAuditRecords" height="360">
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
            </div>
          </section>
        </el-tab-pane>

        <el-tab-pane v-if="canAccessAdminTab('archive-applications')" label="归档申请" name="archive-applications">
          <section class="single-column">
            <div class="panel">
              <div class="panel-head">
                <h3>归档申请</h3>
                <el-button @click="loadArchiveApplications">刷新</el-button>
              </div>
              <el-table :data="archiveApplications" height="360">
                <el-table-column prop="recordNo" label="病历号" min-width="130" />
                <el-table-column prop="patientName" label="患者" min-width="90" />
                <el-table-column prop="status" label="状态" width="100" :formatter="statusFormatter" />
                <el-table-column label="操作" width="100">
                  <template #default="{ row }">
                    <el-button v-if="adminSession.roleCode === 'director' && row.status === 'pending'" link type="primary" @click="auditArchiveAction(row)">归档</el-button>
                    <el-button v-else-if="adminSession.roleCode === 'director' && row.status === 'approved'" link type="info" disabled>已归档</el-button>
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
            </div>
          </section>
        </el-tab-pane>

        <el-tab-pane v-if="canAccessAdminTab('archives')" label="归档记录" name="archives">
          <section class="single-column">
            <div class="panel">
              <div class="panel-head">
                <h3>归档记录</h3>
                <el-button @click="loadArchives">刷新</el-button>
              </div>
              <el-table :data="archives" height="360">
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

        <el-tab-pane v-if="canAccessAdminTab('billing')" label="费用系统" name="billing">
          <section class="single-column">
            <div class="panel">
              <div class="panel-head">
                <h3>费用列表</h3>
                <div class="button-row">
                  <el-button type="primary" @click="openFeeDialog">新增费用</el-button>
                  <el-button @click="loadFees">刷新</el-button>
                </div>
              </div>
              <el-table :data="fees" height="360">
                <el-table-column prop="feeNo" label="费用号" min-width="130" />
                <el-table-column prop="patientName" label="患者" min-width="100" />
                <el-table-column prop="feeItem" label="费用项目" min-width="110" />
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

            <el-dialog v-model="adminDialogs.fee" title="新增费用" width="520px">
              <el-form class="dialog-form" label-position="top" :model="feeForm">
                <el-form-item label="患者">
                  <el-select v-model="feeForm.patientId" filterable placeholder="选择患者" @change="(value) => syncPatientToForm(feeForm, value)">
                    <el-option
                      v-for="item in patientOptions"
                      :key="item.id"
                      :label="relationLabel(item, 'patient')"
                      :value="item.id"
                    />
                  </el-select>
                </el-form-item>
                <el-form-item label="患者姓名">
                  <el-input v-model="feeForm.patientName" disabled />
                </el-form-item>
                <el-form-item label="费用类型">
                  <el-select v-model="feeForm.feeItemCode" filterable placeholder="选择费用项目" @change="syncFeeItemToForm">
                    <el-option
                      v-for="item in feeItems"
                      :key="item.itemCode"
                      :label="`${item.itemName}（￥${item.amount}）`"
                      :value="item.itemCode"
                    />
                  </el-select>
                </el-form-item>
                <el-form-item label="金额">
                  <el-input-number v-model="feeForm.amount" :min="0" :precision="2" disabled />
                </el-form-item>
                <div class="dialog-footer">
                  <el-button @click="adminDialogs.fee = false">取消</el-button>
                  <el-button type="primary" @click="createFeeAction">确认新增</el-button>
                </div>
              </el-form>
            </el-dialog>
          </section>
        </el-tab-pane>

        <el-tab-pane v-if="canAccessAdminTab('news')" label="资讯管理" name="news">
          <section class="single-column">
            <div class="panel wide-panel">
              <div class="panel-head">
                <h3>资讯列表</h3>
                <div class="button-row">
                  <el-button type="primary" @click="adminDialogs.news = true">发布资讯</el-button>
                  <el-button @click="loadNews">刷新</el-button>
                </div>
              </div>
              <el-table :data="newsItems" height="360">
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
            </div>

            <el-dialog v-model="adminDialogs.news" title="发布资讯" width="560px">
              <el-form class="dialog-form" label-position="top" :model="newsForm">
                <el-form-item label="标题">
                  <el-input v-model="newsForm.title" />
                </el-form-item>
                <el-form-item label="分类">
                  <el-input v-model="newsForm.category" />
                </el-form-item>
                <el-form-item label="内容">
                  <el-input v-model="newsForm.content" type="textarea" :rows="3" />
                </el-form-item>
                <div class="dialog-footer">
                  <el-button @click="adminDialogs.news = false">取消</el-button>
                  <el-button type="primary" @click="createNewsAction">确认发布</el-button>
                </div>
              </el-form>
            </el-dialog>
          </section>
        </el-tab-pane>

        <el-tab-pane v-if="canAccessAdminTab('carousels')" label="轮播管理" name="carousels">
          <section class="single-column">
            <div class="panel wide-panel">
              <div class="panel-head">
                <h3>轮播列表</h3>
                <div class="button-row">
                  <el-button type="primary" @click="adminDialogs.carousel = true">新增轮播</el-button>
                  <el-button @click="loadCarousels">刷新</el-button>
                </div>
              </div>
              <el-table :data="carousels" height="360">
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
            </div>

            <el-dialog v-model="adminDialogs.carousel" title="新增轮播" width="560px">
              <el-form class="dialog-form" label-position="top" :model="carouselForm">
                <el-form-item label="标题">
                  <el-input v-model="carouselForm.title" />
                </el-form-item>
                <el-form-item label="图片地址">
                  <el-input v-model="carouselForm.imageUrl" />
                </el-form-item>
                <el-form-item label="跳转地址">
                  <el-input v-model="carouselForm.linkUrl" />
                </el-form-item>
                <div class="dialog-footer">
                  <el-button @click="adminDialogs.carousel = false">取消</el-button>
                  <el-button type="primary" @click="createCarouselAction">确认新增</el-button>
                </div>
              </el-form>
            </el-dialog>
          </section>
        </el-tab-pane>

        <el-tab-pane v-if="canAccessAdminTab('messages')" label="留言管理" name="messages">
          <section class="single-column">
            <div class="panel wide-panel">
              <div class="panel-head">
                <h3>留言列表</h3>
                <el-button @click="loadMessages">刷新</el-button>
              </div>
              <el-table :data="messages" height="360">
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
            </div>
          </section>
        </el-tab-pane>

        <el-tab-pane v-if="canAccessAdminTab('syslogs')" label="系统日志" name="syslogs">
          <section class="single-column">
            <div class="panel wide-panel">
              <div class="panel-head">
                <h3>系统日志</h3>
                <el-button @click="loadSyslogs">刷新</el-button>
              </div>
              <el-table :data="syslogs" height="360">
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
            </div>
          </section>
        </el-tab-pane>

        <el-tab-pane v-if="canAccessAdminTab('config')" label="系统配置" name="config">
          <section class="single-column">
            <div class="panel">
              <div class="panel-head">
                <h3>系统配置</h3>
              </div>
              <el-form class="dialog-form" label-position="top" :model="configForm">
                <el-form-item label="配置键">
                  <el-input v-model="configForm.configKey" />
                </el-form-item>
                <el-form-item label="配置值">
                  <el-input v-model="configForm.configValue" />
                </el-form-item>
                <div class="dialog-footer">
                  <el-button type="primary" @click="saveConfigAction">保存配置</el-button>
                </div>
              </el-form>
            </div>
          </section>
        </el-tab-pane>

        <el-tab-pane v-if="canAccessAdminTab('menus')" label="菜单管理" name="menus">
          <section class="single-column">
            <div class="panel wide-panel">
              <div class="panel-head">
                <h3>菜单管理</h3>
                <el-button @click="loadMenuAction()">读取菜单</el-button>
              </div>
              <el-form class="dialog-form" label-position="top" :model="menuForm">
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
                <div class="dialog-footer">
                  <el-button type="primary" @click="saveMenuAction">保存菜单</el-button>
                </div>
              </el-form>
              <el-descriptions v-if="menuSnapshot" :column="1" border class="menu-preview">
                <el-descriptions-item label="角色">{{ menuSnapshot.roleCode }}</el-descriptions-item>
                <el-descriptions-item label="名称">{{ menuSnapshot.name }}</el-descriptions-item>
                <el-descriptions-item label="菜单JSON">{{ menuSnapshot.menujson }}</el-descriptions-item>
              </el-descriptions>
            </div>
          </section>
        </el-tab-pane>

        <el-tab-pane v-if="canAccessAdminTab('ai')" label="AI 智能" name="ai">
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

      <el-dialog v-model="adminDialogs.triage" title="新增分诊" width="560px">
        <el-form class="dialog-form" label-position="top" :model="triageForm">
          <el-form-item label="患者">
            <el-select
              v-model="triageForm.patientId"
              filterable
              placeholder="选择患者"
              @change="(value) => syncPatientToForm(triageForm, value)"
            >
              <el-option
                v-for="item in patientOptions"
                :key="item.id"
                :label="relationLabel(item, 'patient')"
                :value="item.id"
              />
            </el-select>
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
          <div class="dialog-footer">
            <el-button @click="adminDialogs.triage = false">取消</el-button>
            <el-button type="primary" @click="createTriageAction">创建分诊</el-button>
          </div>
        </el-form>
      </el-dialog>

      <el-dialog v-model="adminDialogs.admission" title="登记入院" width="560px">
        <el-form class="dialog-form" label-position="top" :model="admissionForm">
          <el-form-item label="患者">
            <el-select v-model="admissionForm.patientId" filterable placeholder="选择患者" @change="(value) => syncPatientToForm(admissionForm, value)">
              <el-option
                v-for="item in patientOptions"
                :key="item.id"
                :label="relationLabel(item, 'patient')"
                :value="item.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="主管医生">
            <el-select v-model="admissionForm.doctorId" filterable placeholder="选择医生" @change="(value) => syncDoctorToForm(admissionForm, value)">
              <el-option
                v-for="item in doctorOptions"
                :key="item.id"
                :label="relationLabel(item, 'doctor')"
                :value="item.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="病区">
            <el-input v-model="admissionForm.wardNo" />
          </el-form-item>
          <el-form-item label="病区床位">
            <el-input v-model="admissionForm.bedNo" />
          </el-form-item>
          <el-form-item label="入院原因">
            <el-input v-model="admissionForm.reason" />
          </el-form-item>
          <div class="dialog-footer">
            <el-button @click="adminDialogs.admission = false">取消</el-button>
            <el-button type="primary" @click="createAdmissionAction">登记入院</el-button>
          </div>
        </el-form>
      </el-dialog>

      <el-dialog v-model="adminDialogs.discharge" title="办理出院" width="560px">
        <el-form class="dialog-form" label-position="top" :model="dischargeForm">
          <el-form-item label="患者">
            <el-input :model-value="selectedAdmission?.patientName || ''" disabled />
          </el-form-item>
          <el-form-item label="床位">
            <el-input :model-value="selectedAdmission?.bedNo || ''" disabled />
          </el-form-item>
          <el-form-item label="出院时间">
            <el-input v-model="dischargeForm.dischargeTime" />
          </el-form-item>
          <el-form-item label="出院原因">
            <el-input v-model="dischargeForm.dischargeReason" />
          </el-form-item>
          <el-form-item label="出院小结">
            <el-input v-model="dischargeForm.dischargeSummary" type="textarea" :rows="3" />
          </el-form-item>
          <div class="dialog-footer">
            <el-button @click="adminDialogs.discharge = false">取消</el-button>
            <el-button type="primary" @click="dischargeAdmissionAction">办理出院</el-button>
          </div>
        </el-form>
      </el-dialog>

      <router-view />
    </section>
  </main>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { login, logout, validateToken } from './api/auth';
import { fetchAppointments, fetchAppointmentDetail, confirmAppointment, cancelAppointment } from './api/appointment';
import { createDepartment, fetchDepartments, updateDepartment, deleteDepartment } from './api/department';
import { createManagedUser, fetchManagedUsers, updateManagedUser, deleteManagedUser } from './api/userManagement';
import { fetchDoctors as fetchPublicDoctors } from './api/doctor';
import { createTriageRecord, fetchTriageRecords, createAdmission, fetchAdmissions, dischargeAdmission, fetchDischarges } from './api/inpatient';
import { createMedicalRecord, fetchMedicalRecords, fetchMedicalRecordDetail, updateMedicalRecord, deleteMedicalRecord, createMedicalRecordTemplate, fetchMedicalRecordTemplates, updateMedicalRecordTemplate, deleteMedicalRecordTemplate, createArchiveApplication, fetchArchiveApplications, auditArchiveApplication, fetchArchives } from './api/medicalRecord';
import { createMedicalOrder, fetchMedicalOrders, updateMedicalOrder, deleteMedicalOrder, updateMedicalOrderAuditResult, executeMedicalOrder, createPrescription, fetchPrescriptions, updatePrescription, deletePrescription, auditPrescriptionResult, fetchMedicines, fetchTestItems, createTestItem, updateTestItem, deleteTestItem, createTestRequest, fetchTestRequests, updateTestAuditResult, updateTestRequest, deleteTestRequest } from './api/clinical';
import { createWorkflowTask, fetchWorkflowTasks, auditWorkflowTask, fetchAuditRecords } from './api/workflow';
import { createFee, fetchFeeItems, fetchFees, payFee } from './api/billing';
import { createNews, fetchNews, fetchMessages, replyMessage, createCarousel, fetchCarousels, deleteCarousel, saveConfig, saveMenu, fetchMenu, fetchSyslogs } from './api/system';
import { ocrMedicalRecord, recommendMedicine, auditPrescription, smartSearch } from './api/ai';
import { getAccessibleAdminTabs, getVisibleAdminNavGroups, isAdminTabAccessible } from './adminMenu.mjs';
import { buildRecordSubmitPayload } from './recordForm.mjs';
import {
  applyAppointmentToRecordForm,
  applyDoctorToForm,
  applyPatientToForm,
  applyRecordToClinicalForm,
  appointmentLabel,
  mergeRelationOptions,
  recordLabel,
  relationLabel
} from './relationSelect.mjs';
import { AUTH_EVENT_NAME, clearAuthState, getStoredToken, readStoredSession, saveAuthState } from './utils/session';

const route = useRoute();
const router = useRouter();
const activeTab = ref('dashboard');
const adminSession = ref(createEmptyAdminSession());
const hasAdminToken = ref(Boolean(getStoredToken()));
const adminSessionReady = ref(!hasAdminToken.value);
const userType = ref('doctors');
const selectedAppointment = ref(null);
const selectedDepartment = ref(null);
const selectedUser = ref(null);
const selectedRecord = ref(null);
const selectedRecordDetail = ref(null);
const recordRelatedOrders = ref([]);
const recordRelatedTests = ref([]);
const recordRelatedPrescriptions = ref([]);
const loadingRecordRelations = ref(false);
const selectedAdmission = ref(null);
const selectedTemplate = ref(null);
const selectedOrder = ref(null);
const selectedPrescription = ref(null);
const selectedTestRequest = ref(null);
const adminDialogs = reactive({
  department: false,
  user: false,
  record: false,
  triage: false,
  admission: false,
  discharge: false,
  template: false,
  order: false,
  prescription: false,
  testRequest: false,
  testItem: false,  workflow: false,
  fee: false,
  news: false,
  carousel: false,
  config: false,
  menu: false
});
const departmentDialogMode = ref('create');
const userDialogMode = ref('create');
const recordDialogMode = ref('create');
const templateDialogMode = ref('create');
const orderDialogMode = ref('create');
const prescriptionDialogMode = ref('create');
const testRequestDialogMode = ref('create');

const appointments = ref([]);
const departments = ref([]);
const users = ref([]);
const relationPatients = ref([]);
const relationDoctors = ref([]);
const records = ref([]);
const triageRecords = ref([]);
const admissions = ref([]);
const discharges = ref([]);
const templates = ref([]);
const orders = ref([]);
const prescriptions = ref([]);
const medicines = ref([]);
const testRequests = ref([]);
const testItems = ref([]);
const selectedTestItem = ref(null);
const testItemDialogMode = ref('create');
const testItemForm = ref(makeDefaultTestItemForm());
const workflowTasks = ref([]);
const workflowAuditRecords = ref([]);
const archiveApplications = ref([]);
const archives = ref([]);
const fees = ref([]);
const feeItems = ref([]);
const newsItems = ref([]);
const messages = ref([]);
const carousels = ref([]);
const syslogs = ref([]);
const aiResult = ref('等待执行');

const adminLoginForm = ref({ username: 'admin', password: 'admin123', roleCode: 'admin' });
const appointmentFilterForm = ref({ patientId: null, doctorId: null, status: '' });
const departmentForm = ref(makeDefaultDepartmentForm());
const userForm = ref(makeDefaultUserForm());
const recordForm = ref(makeDefaultRecordForm());
const triageForm = ref(makeDefaultTriageForm());
const admissionForm = ref(makeDefaultAdmissionForm());
const dischargeForm = ref(makeDefaultDischargeForm());
const templateForm = ref(makeDefaultTemplateForm());
const orderForm = ref(makeDefaultOrderForm());
const prescriptionForm = ref(makeDefaultPrescriptionForm());
const testRequestForm = ref(makeDefaultTestRequestForm());
const workflowForm = ref(makeDefaultWorkflowForm());
const feeForm = ref(makeDefaultFeeForm());
const newsForm = ref(makeDefaultNewsForm());
const carouselForm = ref(makeDefaultCarouselForm());
const configForm = ref(makeDefaultConfigForm());
const menuForm = ref(makeDefaultMenuForm());
const menuSnapshot = ref(null);
const aiForm = ref({ fileUrl: '/uploads/demo-record.png', diagnosis: '高血压', searchKeyword: '高血压', prescriptionText: '硝苯地平控释片 每日一次' });
const navGroups = computed(() => getVisibleAdminNavGroups(adminSession.value.roleCode));
const isNurseOnly = computed(() => adminSession.value.roleCode === 'nurse');
const navItems = computed(() => getAccessibleAdminTabs(adminSession.value.roleCode));
const patientOptions = computed(() => mergeRelationOptions([
  ...relationPatients.value,
  ...(userType.value === 'patients' ? users.value : []),
  ...appointments.value,
  ...records.value,
  ...triageRecords.value,
  ...admissions.value,
  ...fees.value
], 'patient'));
const doctorOptions = computed(() => mergeRelationOptions([
  ...relationDoctors.value,
  ...(userType.value === 'doctors' ? users.value : []),
  ...appointments.value,
  ...records.value,
  ...admissions.value
], 'doctor'));
const appointmentOptions = computed(() => mergeRelationOptions(appointments.value, 'appointment'));
const recordOptions = computed(() => mergeRelationOptions(records.value, 'record'));
const workflowBusinessOptions = computed(() => {
  if (workflowForm.value.businessType === 'medical_order') {
    return orders.value.map((row) => ({ id: row.id, name: row.content || `医嘱 ${row.id}`, raw: row }));
  }
  if (workflowForm.value.businessType === 'test_request') {
    return testRequests.value.map((row) => ({ id: row.id, name: row.testItem || `检查 ${row.id}`, raw: row }));
  }
  if (workflowForm.value.businessType === 'record_archive') {
    return archiveApplications.value.map((row) => ({ id: row.id, name: row.recordNo || `归档申请 ${row.id}`, raw: row }));
  }
  return recordOptions.value;
});

function makeDefaultDepartmentForm() {
  return { name: '全科医学科', sortNo: 3 };
}

function makeDefaultUserForm() {
  return { username: 'doctor_demo', name: '赵医生', phone: '13900000003', departmentId: 1, departmentName: '心内科', specialty: '慢病管理' };
}

function makeDefaultRecordForm() {
  return {
    appointmentId: 1,
    appointmentNo: 'YY202606220001',
    patientId: 1,
    patientName: '患者演示',
    doctorId: 1,
    doctorName: '王医生',
    visitTime: nowDatetime(),
    chiefComplaint: '头晕一周',
    presentIllness: '近一周反复头晕，活动后明显。',
    pastHistory: '高血压病史三年。',
    diagnosis: '高血压',
    treatmentAdvice: '规律服药，低盐饮食，定期复查。',
    fileUrl: '/uploads/demo-record.pdf'
  };
}

function makeDefaultTriageForm() {
  return { patientId: 1, patientName: '患者演示', nurseId: 1, nurseName: '护士演示', chiefComplaint: '头晕一周', triageLevel: 'normal' };
}

function makeDefaultAdmissionForm() {
  return { patientId: 1, patientName: '患者演示', doctorId: 1, doctorName: '王医生', nurseId: 1, nurseName: '护士演示', wardNo: 'A1', bedNo: 'A1-08', admissionTime: '2026-06-22 14:00', reason: '观察治疗' };
}

function makeDefaultDischargeForm() {
  return { dischargeTime: '2026-06-25 10:00', dischargeReason: '病情稳定', dischargeSummary: '按医嘱复诊' };
}

function makeDefaultTemplateForm() {
  return { templateName: '门诊首诊模板', templateType: '门诊', content: '主诉：\\n现病史：\\n诊断：\\n处理意见：' };
}

function makeDefaultOrderForm() {
  return { recordId: 1, patientId: 1, patientName: '患者演示', doctorId: 1, doctorName: '王医生', orderType: '长期医嘱', content: '每日监测血压两次' };
}

function makeDefaultPrescriptionForm() {
  return { recordId: 1, patientId: 1, patientName: '患者演示', doctorId: 1, doctorName: '王医生', medicineName: '硝苯地平控释片', quantity: 7, unitPrice: 32.50, usageText: '每日一次' };
}

function makeDefaultTestRequestForm() {
  return { recordId: 1, patientId: 1, patientName: '患者演示', doctorId: 1, doctorName: '王医生', testItem: '血常规', unitPrice: 0, testReason: '评估基础指标', resultContent: '', status: 'pending_audit' };
}

function makeDefaultTestItemForm() {
  return { itemName: '', departmentId: null, departmentName: '', unitPrice: 0, sortOrder: 0 };
}

function makeDefaultWorkflowForm() {
  return { businessType: 'medical_record', businessId: 1, businessNo: 'BL202606220001', applicantId: 1, applicantName: '王医生', assigneeRole: 'director' };
}

function makeDefaultFeeForm() {
  return { patientId: 1, patientName: '患者演示', feeItemCode: '', amount: 0, relatedBusinessType: 'appointment', relatedBusinessId: 1 };
}

function makeDefaultNewsForm() {
  return { title: '高血压随访提醒', category: '慢病管理', content: '规律监测血压，按医嘱服药。', publishStatus: 'published' };
}

function makeDefaultCarouselForm() {
  return { title: '安心医疗服务', imageUrl: '/uploads/banner-emr.png', linkUrl: '/news/1', sortNo: 1 };
}

function makeDefaultConfigForm() {
  return { configKey: 'hospital_name', configValue: '安心医疗', remark: '演示配置' };
}

function makeDefaultMenuForm() {
  return { roleCode: 'admin', name: '管理员菜单', menujson: '[{"name":"仪表盘","path":"/dashboard"},{"name":"系统管理","path":"/system"}]' };
}

function findRelationOption(options, id) {
  return options.find((item) => item.id === id) || null;
}

function findFeeItemOption(itemCode) {
  return feeItems.value.find((item) => item.itemCode === itemCode) || null;
}

function syncFeeItemToForm(itemCode = feeForm.value.feeItemCode) {
  const selected = findFeeItemOption(itemCode);
  if (!selected) {
    feeForm.value.amount = 0;
    return;
  }
  feeForm.value.feeItemCode = selected.itemCode;
  feeForm.value.amount = Number(selected.amount || 0);
}

function applyDefaultFeeItemToForm() {
  if (!feeForm.value.feeItemCode) {
    const first = Array.isArray(feeItems.value) ? feeItems.value.find(() => true) : null;
    if (first) {
      feeForm.value.feeItemCode = first.itemCode;
    }
  }
  syncFeeItemToForm();
}

function syncPatientToForm(form, patientId) {
  const option = findRelationOption(patientOptions.value, patientId);
  if (option) {
    applyPatientToForm(form, option);
  }
}

function syncDoctorToForm(form, doctorId) {
  const option = findRelationOption(doctorOptions.value, doctorId);
  if (option) {
    applyDoctorToForm(form, option);
  }
}

function syncAppointmentToRecord(appointmentId) {
  const option = findRelationOption(appointmentOptions.value, appointmentId);
  applyAppointmentToRecordForm(recordForm.value, option);
}

function syncRecordToForm(form, recordId) {
  const option = findRelationOption(recordOptions.value, recordId);
  if (option) {
    applyRecordToClinicalForm(form, option);
  }
}

function syncWorkflowBusiness(businessId) {
  const option = findRelationOption(workflowBusinessOptions.value, businessId);
  const raw = option?.raw || {};
  workflowForm.value.businessId = businessId || null;
  workflowForm.value.businessNo = raw.recordNo || raw.orderNo || raw.testNo || raw.applicationNo || raw.businessNo || String(businessId || '');
}

function resetWorkflowBusinessSelection() {
  workflowForm.value.businessId = null;
  workflowForm.value.businessNo = '';
}

function workflowBusinessLabel(option) {
  if (workflowForm.value.businessType === 'medical_record') {
    return recordLabel(option);
  }
  const raw = option?.raw || {};
  const owner = raw.patientName ? ` / ${raw.patientName}` : '';
  return `${option?.name || raw.businessNo || `业务 ${option?.id}`}${owner}`;
}

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

function nowDatetime() {
  const d = new Date();
  const pad = (n) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`;
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
  pending: '待确认',
  pending_audit: '待审核',
  created: '已创建',
  confirmed: '已确认',
  approved: '已通过',
  rejected: '已驳回',
  executed: '已执行',
  dispensed: '已发药',
  finished: '已完成',
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

function isNurseRole() {
  return adminSession.value.roleCode === 'nurse';
}

function isDoctorRole() {
  return adminSession.value.roleCode === 'doctor';
}

/**
 * 判断当前后台账号是否能进入指定页签。
 * 所有菜单、页签和路由兜底都走这里，保证展示入口和网关权限口径一致。
 */
function canAccessAdminTab(tabName) {
  return isAdminTabAccessible(adminSession.value.roleCode, tabName);
}

function userNeedsDepartment() {
  return userType.value === 'doctors' || userType.value === 'nurses';
}

/**
 * 解析后台页签。
 * 如果当前角色没有目标页签权限，统一回到仪表盘，避免直接访问 URL 时继续停在无效功能页。
 */
function resolveAdminTab(tabName) {
  const items = navItems.value;
  if (!items || items.length === 0) {
    return 'dashboard';
  }
  return items.some((item) => item.name === tabName) ? tabName : 'dashboard';
}

function findAdminNavItem(tabName) {
  const items = navItems.value;
  if (!Array.isArray(items) || items.length === 0) {
    return null;
  }
  const safeTab = resolveAdminTab(tabName);
  return items.find((item) => item?.name === safeTab) || null;
}

function goToAdminTab(tabName) {
  const item = findAdminNavItem(tabName);
  if (!item || route.name === item.name) {
    return;
  }
  router.push({ name: item.name });
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
    loadAdminRouteData(activeTab.value);
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
    roleCode: data?.roleCode || 'guest',
    departmentId: data?.departmentId ?? null,
    departmentName: data?.departmentName || ''
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
  feeItems.value = [];
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
  selectedAdmission.value = null;
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
  adminSessionReady.value = true;
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
    const status = appointmentFilterForm.value.status || (isNurseRole() ? 'pending' : '');
    if (status) {
      params.status = status;
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
 * 医生确认本人预约。
 * 确认后预约才进入后续接诊演示，前端只替换当前行，保留筛选和分页上下文。
 */
async function confirmAppointmentAction(row) {
  if (!row?.id) {
    ElMessage.warning('缺少预约记录，暂时无法确认');
    return;
  }

  try {
    const data = unwrap(await confirmAppointment(row.id));
    replaceRow(appointments.value, data);
    if (selectedAppointment.value?.id === data.id) {
      selectedAppointment.value = data;
    }
    ElMessage.success('预约已确认');
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
  goToAdminTab('dashboard');
  ElMessage.success('已退出登录');
}

function openDepartmentDialog(mode, row = null) {
  departmentDialogMode.value = mode;
  if (mode === 'edit') {
    if (row) {
      selectDepartment(row);
    }
    if (!selectedDepartment.value?.id) {
      ElMessage.warning('请先选择一条科室记录');
      return;
    }
  } else {
    selectedDepartment.value = null;
    departmentForm.value = makeDefaultDepartmentForm();
  }
  adminDialogs.department = true;
}

function openUserDialog(mode, row = null) {
  userDialogMode.value = mode;
  if (mode === 'edit') {
    if (row) {
      selectUser(row);
    }
    if (!selectedUser.value?.id) {
      ElMessage.warning('请先选择一条人员记录');
      return;
    }
  } else {
    selectedUser.value = null;
    userForm.value = makeDefaultUserForm();
  }
  if (departments.value.length === 0) {
    loadDepartments();
  }
  adminDialogs.user = true;
}

function openRecordDialog(mode, row = null) {
  loadRelationUsers();
  if (canAccessAdminTab('appointments')) {
    loadAppointments();
  }
  recordDialogMode.value = mode;
  if (mode === 'edit') {
    if (row) {
      selectRecord(row);
    }
    if (!selectedRecord.value?.id) {
      ElMessage.warning('请先选择一条病历');
      return;
    }
  } else {
    selectedRecord.value = null;
    selectedRecordDetail.value = null;
    recordForm.value = makeDefaultRecordForm();
    syncDoctorFormToSession(recordForm.value);
  }
  adminDialogs.record = true;
}

function openTriageDialog() {
  loadRelationUsers();
  selectedAdmission.value = null;
  triageForm.value = makeDefaultTriageForm();
  adminDialogs.triage = true;
}

function openAdmissionDialog() {
  loadRelationUsers();
  selectedAdmission.value = null;
  admissionForm.value = makeDefaultAdmissionForm();
  adminDialogs.admission = true;
}

function openDischargeDialog(row) {
  if (!row?.id) {
    ElMessage.warning('请先选择一条入院记录');
    return;
  }
  selectedAdmission.value = row;
  dischargeForm.value = makeDefaultDischargeForm();
  adminDialogs.discharge = true;
}

/**
 * 从预约记录进入分诊。
 * 这里只带入预约和患者上下文，护士归属仍以后端网关注入的登录身份为准。
 */
function openTriageFromAppointment(row) {
  if (!row?.id || !row?.patientId) {
    ElMessage.warning('预约信息不完整，暂时无法分诊');
    return;
  }
  selectedAdmission.value = null;
  triageForm.value = {
    appointmentId: row.id,
    patientId: row.patientId,
    patientName: row.patientName || '患者演示',
    nurseId: adminSession.value.userId || 1,
    nurseName: adminSession.value.username || '护士演示',
    chiefComplaint: row.remark || '',
    triageLevel: 'normal'
  };
  admissionForm.value = {
    ...makeDefaultAdmissionForm(),
    patientId: row.patientId,
    patientName: row.patientName || '患者演示',
    doctorId: row.doctorId || 1,
    doctorName: row.doctorName || '王医生',
    nurseId: adminSession.value.userId || 1,
    nurseName: adminSession.value.username || '护士演示'
  };
  adminDialogs.triage = true;
}

function openTemplateDialog(mode, row = null) {
  templateDialogMode.value = mode;
  if (mode === 'edit') {
    if (row) {
      selectTemplate(row);
    }
    if (!selectedTemplate.value?.id) {
      ElMessage.warning('请先选择一条模板');
      return;
    }
  } else {
    selectedTemplate.value = null;
    templateForm.value = makeDefaultTemplateForm();
  }
  adminDialogs.template = true;
}

function openOrderDialog(mode, row = null) {
  loadRecords();
  orderDialogMode.value = mode;
  if (mode === 'edit') {
    if (row) {
      selectOrder(row);
    }
    if (!selectedOrder.value?.id) {
      ElMessage.warning('请先选择一条医嘱');
      return;
    }
  } else {
    selectedOrder.value = null;
    orderForm.value = makeDefaultOrderForm();
  }
  adminDialogs.order = true;
}

function openPrescriptionDialog(mode, row = null) {
  loadRecords();
  loadMedicines();
  prescriptionDialogMode.value = mode;
  if (mode === 'edit') {
    if (row) {
      selectPrescription(row);
    }
    if (!selectedPrescription.value?.id) {
      ElMessage.warning('请先选择一条处方');
      return;
    }
  } else {
    selectedPrescription.value = null;
    prescriptionForm.value = makeDefaultPrescriptionForm();
  }
  adminDialogs.prescription = true;
}

function openTestRequestDialog(mode, row = null) {
  loadRecords();
  loadTestItems();
  testRequestDialogMode.value = mode;
  if (mode === 'edit') {
    if (row) {
      selectTestRequest(row);
    }
    if (!selectedTestRequest.value?.id) {
      ElMessage.warning('请先选择一条检查申请');
      return;
    }
  } else {
    selectedTestRequest.value = null;
    testRequestForm.value = makeDefaultTestRequestForm();
    syncDoctorFormToSession(testRequestForm.value);
  }
  adminDialogs.testRequest = true;
}

function openWorkflowDialog() {
  loadWorkflowTaskContext();
  adminDialogs.workflow = true;
}

async function openFeeDialog() {
  loadRelationUsers();
  feeForm.value = makeDefaultFeeForm();
  await loadFeeItems();
  applyDefaultFeeItemToForm();
  adminDialogs.fee = true;
}

async function createDepartmentAction() {
  try {
    await createDepartment(departmentForm.value);
    await loadDepartments();
    adminDialogs.department = false;
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
    adminDialogs.department = false;
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
    departmentForm.value = makeDefaultDepartmentForm();
    ElMessage.success('科室已删除');
  } catch (error) {
    showError(error);
  }
}

async function createUserAction() {
  try {
    await createManagedUser(userType.value, userForm.value);
    await loadUsers();
    adminDialogs.user = false;
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
 * 静默加载患者和医生候选。
 * 这些数据只用于下拉辅助选择；角色无权限时不打断当前页面，继续使用业务列表中已出现的候选。
 */
async function loadRelationUsers() {
  try {
    relationDoctors.value = rowsOf(unwrap(await fetchPublicDoctors({ page: 1, limit: 100 })));
  } catch (error) {
    relationDoctors.value = [];
  }

  if (!canAccessAdminTab('managed-users')) {
    relationPatients.value = [];
    return;
  }

  try {
    relationPatients.value = rowsOf(unwrap(await fetchManagedUsers('patients', { page: 1, limit: 100 })));
  } catch (error) {
    relationPatients.value = [];
  }
}

/**
 * 切换用户类型时回到第一页。
 * 不同角色的数据量差异较大，保留旧页码容易落到空页，所以这里统一重置再查询。
 */
function handleUserTypeChange() {
  pagers.users.page = 1;
  selectedUser.value = null;
  userForm.value = makeDefaultUserForm();
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

function syncUserDepartment(departmentId) {
  const department = departments.value.find((item) => item.id === departmentId);
  if (!department) {
    return;
  }
  userForm.value.departmentName = department.name;
}

async function updateUserAction() {
  if (!selectedUser.value?.id) {
    ElMessage.warning('请先选择一条人员记录');
    return;
  }

  try {
    await updateManagedUser(userType.value, selectedUser.value.id, userForm.value);
    await loadUsers();
    adminDialogs.user = false;
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
    userForm.value = makeDefaultUserForm();
    ElMessage.success('人员已删除');
  } catch (error) {
    showError(error);
  }
}

async function createRecordAction() {
  try {
    const payload = buildRecordSubmitPayload(recordForm.value);
    const data = unwrap(await createMedicalRecord(payload));
    recordForm.value.visitTime = payload.visitTime;
    await loadRecords();
    selectedRecord.value = data;
    orderForm.value.recordId = data.id;
    workflowForm.value.businessId = data.id;
    workflowForm.value.businessNo = data.recordNo;
    adminDialogs.record = false;
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
    await loadAppointments();
    adminDialogs.triage = false;
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
    adminDialogs.admission = false;
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
  const admission = selectedAdmission.value;
  if (!admission) {
    ElMessage.warning('请先在住院列表选择一条入院记录');
    return;
  }

  try {
    await dischargeAdmission(admission.id, dischargeForm.value);
    await loadAdmissions();
    loadDischarges();
    selectedAdmission.value = null;
    adminDialogs.discharge = false;
    ElMessage.success('出院办理完成');
  } catch (error) {
    showError(error);
  }
}

async function createTemplateAction() {
  try {
    await createMedicalRecordTemplate(templateForm.value);
    await loadTemplates();
    adminDialogs.template = false;
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
    adminDialogs.template = false;
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
    templateForm.value = makeDefaultTemplateForm();
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
    visitTime: row?.visitTime || nowDatetime(),
    chiefComplaint: row?.chiefComplaint || '',
    presentIllness: row?.presentIllness || '',
    pastHistory: row?.pastHistory || '',
    diagnosis: row?.diagnosis || '',
    treatmentAdvice: row?.treatmentAdvice || '',
    fileUrl: row?.fileUrl || ''
  };
  loadRecordDetail(row.id);
  loadRecordRelations(row.id);
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

async function loadRecordRelations(recordId) {
  if (!recordId) {
    recordRelatedOrders.value = [];
    recordRelatedTests.value = [];
    recordRelatedPrescriptions.value = [];
    return;
  }

  loadingRecordRelations.value = true;
  try {
    const results = await Promise.allSettled([
      fetchMedicalOrders({ recordId, limit: 100 }),
      fetchTestRequests({ recordId, limit: 100 }),
      fetchPrescriptions({ recordId, limit: 100 })
    ]);
    recordRelatedOrders.value = results[0].status === 'fulfilled' ? rowsOf(unwrap(results[0].value)) : [];
    recordRelatedTests.value = results[1].status === 'fulfilled' ? rowsOf(unwrap(results[1].value)) : [];
    recordRelatedPrescriptions.value = results[2].status === 'fulfilled' ? rowsOf(unwrap(results[2].value)) : [];
  } catch (error) {
    recordRelatedOrders.value = [];
    recordRelatedTests.value = [];
    recordRelatedPrescriptions.value = [];
  } finally {
    loadingRecordRelations.value = false;
  }
}

function hasUnfinishedRelations() {
  const unfinishedTests = recordRelatedTests.value.filter((item) => item.status !== 'finished');
  const unfinishedOrders = recordRelatedOrders.value.filter((item) => item.status !== 'executed');
  const unfinishedPrescriptions = recordRelatedPrescriptions.value.filter((item) => item.status !== 'paid' && item.status !== 'dispensed');
  return {
    tests: unfinishedTests,
    orders: unfinishedOrders,
    prescriptions: unfinishedPrescriptions,
    get any() {
      return this.tests.length > 0 || this.orders.length > 0 || this.prescriptions.length > 0;
    }
  };
}

async function updateRecordAction() {
  if (!selectedRecord.value?.id) {
    ElMessage.warning('请先选择一条病历');
    return;
  }

  try {
    const payload = buildRecordSubmitPayload(recordForm.value);
    const data = unwrap(await updateMedicalRecord(selectedRecord.value.id, payload));
    recordForm.value.visitTime = payload.visitTime;
    await loadRecords();
    selectedRecord.value = data;
    await loadRecordDetail(data.id);
    adminDialogs.record = false;
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
 * 提交前校验关联的检查/医嘱/处方是否全部完成，未完成则阻止归档并列出待处理项。
 */
async function createArchiveApplicationAction() {
  if (!selectedRecord.value) {
    ElMessage.warning('请先选择一条病历');
    return;
  }

  const unfinished = hasUnfinishedRelations();
  if (unfinished.any) {
    const parts = [];
    if (unfinished.tests.length > 0) {
      parts.push(`${unfinished.tests.length} 项检查未完成`);
    }
    if (unfinished.orders.length > 0) {
      parts.push(`${unfinished.orders.length} 条医嘱未执行`);
    }
    if (unfinished.prescriptions.length > 0) {
      parts.push(`${unfinished.prescriptions.length} 张处方未处理`);
    }
    ElMessage.warning(`该病历下还有 ${parts.join('、')}，请先处理完毕后再提交归档`);
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
    await loadRecords();
    selectedRecord.value = null;
    selectedRecordDetail.value = null;
    ElMessage.success('归档申请已提交');
  } catch (error) {
    showError(error);
  }
}

async function createOrderAction() {
  try {
    await createMedicalOrder(orderForm.value);
    await loadOrders();
    adminDialogs.order = false;
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
    adminDialogs.order = false;
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
    const nurseId = adminSession.value.userId || 1;
    const nurseName = adminSession.value.username || '护士演示';
    unwrap(await executeMedicalOrder(row.id, { nurseId, nurseName, executionResult: '已执行', remark: '按时完成' }));
    const idx = orders.value.findIndex((o) => o.id === row.id);
    if (idx >= 0) {
      orders.value[idx] = { ...orders.value[idx], status: 'executed' };
    }
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
    adminDialogs.prescription = false;
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

async function loadMedicines() {
  try {
    medicines.value = unwrap(await fetchMedicines()) || [];
  } catch (error) {
    showError(error);
  }
}

function onPrescriptionMedicineChange(medicineName) {
  const selected = Array.isArray(medicines.value) ? medicines.value.find((m) => m.medicineName === medicineName) : null;
  if (selected) {
    prescriptionForm.value.unitPrice = selected.unitPrice || 0;
  }
}

function selectPrescription(row) {
  selectedPrescription.value = row;
  prescriptionForm.value = {
    recordId: row?.recordId || 1,
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
    adminDialogs.prescription = false;
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

async function auditPrescriptionAction(row) {
  try {
    await auditPrescriptionResult(row.id, {
      auditResult: 'approved',
      auditOpinion: '审核通过'
    });
    await loadPrescriptions();
    ElMessage.success('处方审核已通过');
  } catch (error) {
    showError(error);
  }
}

async function createTestRequestAction() {
  try {
    await createTestRequest(testRequestForm.value);
    await loadTestRequests();
    adminDialogs.testRequest = false;
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
    recordId: row?.recordId || 1,
    patientId: row?.patientId || 1,
    patientName: row?.patientName || '',
    doctorId: row?.doctorId || 1,
    doctorName: row?.doctorName || '',
    testItem: row?.testItem || '',
    testReason: row?.testReason || '',
    resultContent: row?.resultContent || '',
    status: row?.status || 'pending_audit'
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
    adminDialogs.testRequest = false;
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

async function loadTestItems() {
  try {
    testItems.value = unwrap(await fetchTestItems()) || [];
  } catch (error) {
    showError(error);
  }
}

function onTestItemChange(itemName) {
  const selected = Array.isArray(testItems.value) ? testItems.value.find((m) => m.itemName === itemName) : null;
  if (selected) {
    testRequestForm.value.unitPrice = selected.unitPrice || 0;
    testRequestForm.value.departmentName = selected.departmentName || '';
  }
}

function onTestItemDeptChange(deptId) {
  const dept = departments.value.find((d) => d.id === deptId);
  testItemForm.value.departmentName = dept ? dept.name : '';
}

function openTestItemDialog(mode, row = null) {
  loadTestItems();
  loadDepartments();
  testItemDialogMode.value = mode;
  if (mode === 'edit' && row) {
    testItemForm.value = {
      itemName: row.itemName || '',
      departmentId: row.departmentId || null,
      departmentName: row.departmentName || '',
      unitPrice: row.unitPrice || 0,
      sortOrder: row.sortOrder || 0
    };
  } else {
    testItemForm.value = makeDefaultTestItemForm();
  }
  adminDialogs.testItem = true;
}

async function createTestItemAction() {
  try {
    await createTestItem(testItemForm.value);
    adminDialogs.testItem = false;
    await loadTestItems();
    ElMessage.success('检查项目已添加');
  } catch (error) {
    showError(error);
  }
}

async function updateTestItemAction() {
  try {
    await updateTestItem(selectedTestItem.value.id, testItemForm.value);
    adminDialogs.testItem = false;
    await loadTestItems();
    ElMessage.success('检查项目已更新');
  } catch (error) {
    showError(error);
  }
}

async function deleteTestItemAction() {
  if (!selectedTestItem.value?.id) {
    ElMessage.warning('请先选择一个检查项目');
    return;
  }
  try {
    await deleteTestItem(selectedTestItem.value.id);
    await loadTestItems();
    ElMessage.success('检查项目已禁用');
  } catch (error) {
    showError(error);
  }
}

async function createWorkflowTaskAction() {
  try {
    await createWorkflowTask(workflowForm.value);
    await loadWorkflowTasks();
    adminDialogs.workflow = false;
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
      auditUserId: adminSession.value.userId || 4,
      auditUserName: adminSession.value.realName || adminSession.value.username || '主任',
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
    if (!feeForm.value.feeItemCode) {
      ElMessage.warning('请选择费用项目');
      return;
    }
    await createFee(feeForm.value);
    await loadFees();
    adminDialogs.fee = false;
    ElMessage.success('费用已新增');
  } catch (error) {
    showError(error);
  }
}

async function loadFeeItems() {
  try {
    feeItems.value = unwrap(await fetchFeeItems()) || [];
    applyDefaultFeeItemToForm();
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
    adminDialogs.news = false;
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
    adminDialogs.carousel = false;
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
    adminDialogs.config = false;
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
    adminDialogs.menu = false;
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
  if (!Array.isArray(rows)) return;
  const index = rows.findIndex((item) => item?.id === row?.id);
  if (index >= 0) {
    rows[index] = row;
  };
}

function syncDoctorFormToSession(form) {
  if (!form || adminSession.value.roleCode !== 'doctor') {
    return;
  }
  form.doctorId = adminSession.value.userId || form.doctorId;
  form.doctorName = adminSession.value.realName || adminSession.value.username || form.doctorName;
}

function getAdminTabLoader(tabName) {
  const loaders = {
    appointments: loadAppointmentContext,
    departments: loadDepartments,
    'managed-users': loadUsers,
    records: loadRecordContext,
    triage: loadTriageContext,
    admissions: loadAdmissionContext,
    discharges: loadDischarges,
    templates: loadTemplates,
    orders: loadOrderContext,
    prescriptions: loadPrescriptionContext,
    tests: loadTestRequestContext,
    'workflow-tasks': loadWorkflowTaskContext,
    'workflow-audits': loadWorkflowAuditRecords,
    'archive-applications': loadArchiveApplications,
    archives: loadArchives,
    billing: loadBillingContext,
    news: loadNews,
    messages: loadMessages,
    carousels: loadCarousels,
    menus: () => loadMenuAction({ silent: true }),
    syslogs: loadSyslogs
  };
  return loaders[tabName];
}

function loadAppointmentContext() {
  loadRelationUsers();
  loadAppointments();
}

function loadRecordContext() {
  loadRelationUsers();
  loadRecords();
  if (canAccessAdminTab('appointments')) {
    loadAppointments();
  }
}

function loadTriageContext() {
  loadRelationUsers();
  loadTriageRecords();
}

function loadAdmissionContext() {
  loadRelationUsers();
  loadAdmissions();
}

function loadOrderContext() {
  loadRelationUsers();
  loadOrders();
  loadRecords();
}

function loadPrescriptionContext() {
  loadRelationUsers();
  loadPrescriptions();
  loadRecords();
}

function loadTestRequestContext() {
  loadRelationUsers();
  loadTestRequests();
  loadRecords();
}

function loadWorkflowTaskContext() {
  loadRelationUsers();
  loadWorkflowTasks();
  loadRecords();
  loadOrders();
  loadTestRequests();
  loadArchiveApplications();
}

function loadBillingContext() {
  loadRelationUsers();
  loadFeeItems();
  loadFees();
}

/**
 * 按当前角色权限批量加载后台数据。
 * 菜单被隐藏的模块不会再预加载，避免登录后无意义地触发网关 403。
 */
function loadAllowedAdminTabs(tabNames) {
  tabNames.forEach((tabName) => {
    if (!canAccessAdminTab(tabName)) {
      return;
    }

    const loader = getAdminTabLoader(tabName);
    if (typeof loader === 'function') {
      loader();
    }
  });
}

/**
 * 统一加载后台工作台数据。
 * 登录成功或刷新后 Token 校验通过时只拉取当前角色有权限的模块，避免角色菜单隐藏后仍请求无权限接口。
 */
function loadAdminWorkspaceData() {
  loadAllowedAdminTabs(navItems.value.map((item) => item.name).filter((name) => name !== 'dashboard' && name !== 'ai'));
}

/**
 * 根据后台当前路由加载页面数据。
 * 菜单点击、浏览器前进后退和刷新恢复都走这里，避免每次进入都批量请求所有服务。
 */
function loadAdminRouteData(tabName) {
  if (!hasAdminToken.value) {
    return;
  }

  const tab = resolveAdminTab(tabName);
  if (tab === 'dashboard') {
    loadAllowedAdminTabs(['appointments', 'departments', 'managed-users', 'records', 'workflow-tasks', 'archive-applications', 'billing', 'triage', 'admissions', 'orders', 'prescriptions', 'tests']);
    return;
  }

  loadAllowedAdminTabs([tab]);
}

function syncAdminRouteState(routeName) {
  const nextTab = resolveAdminTab(routeName);
  if (activeTab.value !== nextTab) {
    activeTab.value = nextTab;
  }
  if (routeName && routeName !== nextTab) {
    const item = findAdminNavItem(nextTab);
    if (item && route.name !== item.name) {
      void router.replace({ name: item.name }).catch(() => {});
    }
  }
  if (adminSessionReady.value) {
    loadAdminRouteData(nextTab);
  }
}

/**
 * 处理后台统一认证事件。
 * 401 代表登录态失效，需要清空会话；403 只提示当前角色无权访问，保留现有登录态。
 */
function handleAdminAuthEvent(event) {
  const status = event?.detail?.status;
  if (status === 401) {
    resetAdminAuth();
    goToAdminTab('dashboard');
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
  await verifyAdminSession();
  adminSessionReady.value = true;
  syncAdminRouteState(route.name);
});

watch(
  () => route.name,
  (routeName) => {
    syncAdminRouteState(routeName);
  },
  { immediate: true }
);

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
    radial-gradient(circle at 72% 20%, rgba(136, 205, 194, 0.22), transparent 18%),
    radial-gradient(circle at 24% 68%, rgba(186, 221, 180, 0.18), transparent 20%),
    #f6f8f6;
  color: #1f2937;
}

.auth-card {
  width: min(380px, 100%);
  padding: 34px 30px 32px;
  background: #ffffff;
  border: 1px solid #e3ece6;
  border-radius: 14px;
  box-shadow: 0 18px 45px rgba(18, 63, 62, 0.14);
}

.auth-brand {
  display: grid;
  justify-items: center;
  gap: 8px;
  margin-bottom: 22px;
  text-align: center;
}

.auth-brand h1 {
  margin: 0;
  color: #145c5b;
  font-size: 26px;
  letter-spacing: 0;
}

.auth-brand p {
  margin: 0;
  color: #738382;
  font-size: 13px;
}

.brand-icon {
  display: inline-grid;
  width: 26px;
  height: 26px;
  place-items: center;
  color: #7fd0c1;
  border: 1px solid rgba(127, 208, 193, 0.7);
  border-radius: 4px;
}

.auth-submit {
  width: 100%;
}

.admin-shell {
  display: grid;
  grid-template-columns: 164px minmax(0, 1fr);
  min-height: 100vh;
  background: #f2f4f8;
  color: #1f2937;
}

.sidebar {
  padding: 0;
  background: linear-gradient(180deg, #104f53 0%, #286f73 100%);
  color: #ffffff;
}

.sidebar h1 {
  display: flex;
  align-items: center;
  gap: 8px;
  min-height: 38px;
  padding: 0 10px;
  margin: 0;
  font-size: 15px;
  letter-spacing: 0;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}

.sidebar nav {
  display: grid;
  gap: 4px;
  padding-top: 22px;
}

.sidebar button {
  width: 100%;
  min-height: 38px;
  padding: 0 18px;
  color: rgba(255, 255, 255, 0.82);
  text-align: left;
  background: transparent;
  border: 0;
  border-radius: 0;
  cursor: pointer;
}

.sidebar button.active,
.sidebar button:hover {
  color: #ffffff;
  background: rgba(255, 255, 255, 0.1);
}

.sidebar-group {
  display: grid;
  gap: 2px;
}

.sidebar-group p {
  margin: 10px 18px 4px;
  color: rgba(255, 255, 255, 0.58);
  font-size: 12px;
}

.sidebar button.sidebar-child {
  min-height: 32px;
  padding-left: 34px;
  font-size: 13px;
}

.workspace {
  min-width: 0;
  padding: 0 28px 28px;
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
  min-height: 34px;
  justify-content: space-between;
  margin: 0 -28px 34px;
  padding: 0 28px;
  background: #ffffff;
  border-bottom: 1px solid #e7ecf2;
  box-shadow: 0 2px 10px rgba(15, 36, 44, 0.04);
}

.session {
  justify-content: flex-end;
}

.button-row {
  flex-wrap: wrap;
}

.eyebrow {
  margin: 0;
  color: #53656a;
  font-size: 13px;
}

h2,
h3 {
  margin: 0;
  letter-spacing: 0;
}

h2 {
  font-size: 14px;
  font-weight: 500;
}

h3 {
  margin-bottom: 16px;
  font-size: 18px;
}

.workspace-tabs {
  padding: 0;
  background: #ffffff;
  border: 1px solid #e5eaf0;
  border-radius: 0;
}

.workspace-tabs :deep(.el-tabs__header) {
  display: none;
}

.workspace-tabs :deep(.el-tabs__content) {
  padding: 24px 16px 24px;
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
  background: #fbfdff;
  border: 1px solid #e9eef4;
  border-radius: 2px;
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
.single-column,
.three-column {
  display: grid;
  gap: 18px;
  align-items: start;
}

.single-column {
  grid-template-columns: 1fr;
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
  background: #ffffff;
  border: 1px solid #e7edf3;
  border-radius: 0;
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

.record-relations-panel {
  margin-top: 16px;
  padding: 16px;
  background: #f8fafc;
  border-radius: 8px;
  border: 1px solid #e2e8f0;
}

.relations-title {
  margin: 0 0 12px;
  font-size: 14px;
  font-weight: 600;
  color: #334155;
}

.relations-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 12px;
}

.relation-card {
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  padding: 12px;
}

.relation-card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  font-weight: 600;
  color: #475569;
  margin-bottom: 8px;
}

.relation-icon {
  font-size: 16px;
}

.relation-items {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.relation-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 4px 0;
  border-bottom: 1px dashed #f1f5f9;
}

.relation-item:last-child {
  border-bottom: none;
}

.relation-item-name {
  font-size: 13px;
  color: #64748b;
}

.archive-complete-tip {
  margin-top: 12px;
  text-align: right;
}

.dialog-form {
  padding-top: 4px;
}

.dialog-form :deep(.el-select),
.dialog-form :deep(.el-input-number),
.panel :deep(.el-select),
.panel :deep(.el-input-number) {
  width: 100%;
}

.record-relation-stack {
  display: grid;
  grid-template-columns: 1fr;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 10px;
}

.result-box {
  min-height: 320px;
  max-height: 460px;
  overflow: auto;
  padding: 14px;
  margin: 0;
  background: #0f4e52;
  color: #d1fae5;
  border-radius: 8px;
  white-space: pre-wrap;
}

.executed-tag {
  display: inline-block;
  padding: 2px 10px;
  color: #16a34a;
  background: #dcfce7;
  border-radius: 4px;
  font-size: 13px;
  font-weight: 500;
}

:deep(.el-button--primary:not(.is-link)) {
  --el-button-bg-color: #0b74b8;
  --el-button-border-color: #0b74b8;
  --el-button-text-color: #ffffff;
  --el-button-hover-bg-color: #075f99;
  --el-button-hover-border-color: #075f99;
  --el-button-hover-text-color: #ffffff;
  --el-button-active-bg-color: #064f7f;
  --el-button-active-border-color: #064f7f;
  --el-button-active-text-color: #ffffff;
  background-color: #0b74b8;
  border-color: #0b74b8;
  color: #ffffff;
  font-weight: 600;
}

:deep(.el-button--primary:not(.is-link):hover) {
  background-color: #075f99;
  border-color: #075f99;
  color: #ffffff;
}

:deep(.el-button--danger:not(.is-link):not(.is-plain)) {
  --el-button-bg-color: #d12d2d;
  --el-button-border-color: #d12d2d;
  --el-button-text-color: #ffffff;
  --el-button-hover-bg-color: #b91c1c;
  --el-button-hover-border-color: #b91c1c;
  --el-button-hover-text-color: #ffffff;
  --el-button-active-bg-color: #991b1b;
  --el-button-active-border-color: #991b1b;
  --el-button-active-text-color: #ffffff;
  background-color: #d12d2d;
  border-color: #d12d2d;
  color: #ffffff;
  font-weight: 600;
}

:deep(.el-button--danger:not(.is-link):not(.is-plain):hover) {
  background-color: #b91c1c;
  border-color: #b91c1c;
  color: #ffffff;
}

:deep(.el-button--danger.is-plain:not(.is-link)) {
  --el-button-bg-color: #fff1f1;
  --el-button-border-color: #ef9a9a;
  --el-button-text-color: #b42318;
  --el-button-hover-bg-color: #d12d2d;
  --el-button-hover-border-color: #d12d2d;
  --el-button-hover-text-color: #ffffff;
  --el-button-active-bg-color: #b91c1c;
  --el-button-active-border-color: #b91c1c;
  background-color: #fff1f1;
  border-color: #ef9a9a;
  color: #b42318;
}

:deep(.el-button--danger.is-plain:not(.is-link):hover) {
  background-color: #d12d2d;
  border-color: #d12d2d;
  color: #ffffff;
}

:deep(.el-button:not(.el-button--primary):not(.el-button--danger):not(.is-link)) {
  --el-button-bg-color: #ffffff;
  --el-button-border-color: #cbd5e1;
  --el-button-text-color: #334155;
  --el-button-hover-bg-color: #f0f7ff;
  --el-button-hover-border-color: #5aa6d9;
  --el-button-hover-text-color: #075985;
  background-color: #ffffff;
  border-color: #cbd5e1;
  color: #334155;
  font-weight: 600;
}

:deep(.el-button:not(.el-button--primary):not(.el-button--danger):not(.is-link):hover) {
  background-color: #f0f7ff;
  border-color: #5aa6d9;
  color: #075985;
}

:deep(.el-button.is-link) {
  --el-button-bg-color: transparent;
  --el-button-border-color: transparent;
  --el-button-hover-bg-color: transparent;
  --el-button-hover-border-color: transparent;
  background-color: transparent;
  border-color: transparent;
  font-weight: 600;
}

:deep(.el-button.is-link.el-button--primary) {
  --el-button-text-color: #075985;
  --el-button-hover-text-color: #0c4a6e;
  color: #075985;
}

:deep(.el-button.is-link.el-button--primary:hover) {
  background-color: #eaf5ff;
  color: #0c4a6e;
}

:deep(.el-button.is-link.el-button--danger) {
  --el-button-text-color: #b42318;
  --el-button-hover-text-color: #8f1d14;
  background-color: transparent;
  color: #b42318;
}

:deep(.el-button.is-link.el-button--danger:hover) {
  background-color: #fff1f1;
  color: #8f1d14;
}

:deep(.el-button.is-disabled),
:deep(.el-button.is-disabled:hover) {
  background-color: #eef2f5;
  border-color: #d9e1e8;
  color: #7a8790;
}

:deep(.el-table th.el-table__cell) {
  background: #fbfcfe;
  color: #52606d;
  font-weight: 500;
}

:deep(.el-table td.el-table__cell),
:deep(.el-table th.el-table__cell) {
  border-color: #edf1f5;
}

:deep(.el-pagination.is-background .el-pager li.is-active) {
  background-color: #0d7dcc;
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
