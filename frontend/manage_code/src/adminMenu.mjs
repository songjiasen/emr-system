const ADMIN_ROLES = new Set(['admin', 'super_admin']);

export const ADMIN_NAV_GROUPS = [
  { name: 'dashboard', label: '仪表盘', path: '/dashboard' },
  { name: 'appointments', label: '预约管理', path: '/appointments' },
  {
    label: '用户科室',
    children: [
      { name: 'departments', label: '科室管理', path: '/departments' },
      { name: 'managed-users', label: '人员管理', path: '/managed-users' }
    ]
  },
  {
    label: '就诊病历',
    children: [
      { name: 'records', label: '病历列表', path: '/records' },
      { name: 'triage', label: '分诊记录', path: '/triage' },
      { name: 'admissions', label: '入院管理', path: '/admissions' },
      { name: 'discharges', label: '出院记录', path: '/discharges' },
      { name: 'templates', label: '病历模板', path: '/templates' }
    ]
  },
  {
    label: '诊疗管理',
    children: [
      { name: 'orders', label: '医嘱管理', path: '/orders' },
      { name: 'prescriptions', label: '处方管理', path: '/prescriptions' },
      { name: 'tests', label: '检查申请', path: '/tests' }
    ]
  },
  {
    label: '审核归档',
    children: [
      { name: 'workflow-tasks', label: '审核任务', path: '/workflow-tasks' },
      { name: 'workflow-audits', label: '审核记录', path: '/workflow-audits' },
      { name: 'archive-applications', label: '归档申请', path: '/archive-applications' },
      { name: 'archives', label: '归档记录', path: '/archives' }
    ]
  },
  { name: 'billing', label: '费用系统', path: '/billing' },
  {
    label: '系统内容',
    children: [
      { name: 'news', label: '资讯管理', path: '/news' },
      { name: 'carousels', label: '轮播管理', path: '/carousels' },
      { name: 'messages', label: '留言管理', path: '/messages' },
      { name: 'syslogs', label: '系统日志', path: '/syslogs' },
      { name: 'config', label: '系统配置', path: '/config' },
      { name: 'menus', label: '菜单管理', path: '/menus' }
    ]
  },
  { name: 'ai', label: 'AI 智能', path: '/ai' }
];

const ROLE_TAB_ACCESS = {
  doctor: new Set(['dashboard', 'appointments', 'records', 'templates', 'orders', 'prescriptions', 'tests', 'archive-applications', 'ai']),
  nurse: new Set(['dashboard', 'records', 'triage', 'admissions', 'discharges', 'orders', 'ai']),
  director: new Set(['dashboard', 'records', 'orders', 'prescriptions', 'tests', 'workflow-tasks', 'workflow-audits', 'archive-applications', 'archives', 'ai'])
};

function normalizeRoleCode(roleCode) {
  return typeof roleCode === 'string' ? roleCode.trim() : '';
}

export function getAllAdminNavItems() {
  return ADMIN_NAV_GROUPS.flatMap((group) => group.children || [group]);
}

/**
 * 判断当前角色是否能进入后台指定页签。
 * 这里和网关粗粒度权限保持同一业务口径，前端只负责隐藏无效入口，真正鉴权仍以网关为准。
 */
export function isAdminTabAccessible(roleCode, tabName) {
  if (!tabName) {
    return false;
  }

  const safeRoleCode = normalizeRoleCode(roleCode);
  if (ADMIN_ROLES.has(safeRoleCode)) {
    return true;
  }

  const allowedTabs = ROLE_TAB_ACCESS[safeRoleCode];
  return allowedTabs ? allowedTabs.has(tabName) : tabName === 'dashboard';
}

/**
 * 返回角色可访问的平铺菜单项。
 * 后续路由兜底和数据加载也复用这个结果，避免菜单隐藏了但逻辑仍请求无权限接口。
 */
export function getAccessibleAdminTabs(roleCode) {
  return getAllAdminNavItems().filter((item) => isAdminTabAccessible(roleCode, item.name));
}

/**
 * 返回角色可见的分组菜单。
 * 子菜单全部被过滤后会移除整个分组，避免后台出现空标题误导用户。
 */
export function getVisibleAdminNavGroups(roleCode) {
  return ADMIN_NAV_GROUPS.reduce((groups, group) => {
    if (!group.children) {
      if (isAdminTabAccessible(roleCode, group.name)) {
        groups.push(group);
      }
      return groups;
    }

    const children = group.children.filter((item) => isAdminTabAccessible(roleCode, item.name));
    if (children.length > 0) {
      groups.push({ ...group, children });
    }
    return groups;
  }, []);
}
