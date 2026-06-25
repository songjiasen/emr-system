import assert from 'node:assert/strict';
import test from 'node:test';

import {
  ADMIN_NAV_GROUPS,
  getAccessibleAdminTabs,
  getVisibleAdminNavGroups,
  isAdminTabAccessible
} from './adminMenu.mjs';

function namesFor(roleCode) {
  return getAccessibleAdminTabs(roleCode).map((item) => item.name);
}

test('管理员保留后台全部菜单入口', () => {
  const allNames = ADMIN_NAV_GROUPS.flatMap((group) => group.children || [group]).map((item) => item.name);

  assert.deepEqual(namesFor('admin'), allNames);
  assert.deepEqual(namesFor('super_admin'), allNames);
});

test('护士只展示护理职责相关菜单', () => {
  const names = namesFor('nurse');

  assert.ok(names.includes('dashboard'));
  assert.ok(names.includes('triage'));
  assert.ok(names.includes('admissions'));
  assert.ok(names.includes('discharges'));
  assert.ok(names.includes('records'));
  assert.ok(names.includes('orders'));
  assert.ok(names.includes('ai'));
  assert.ok(!names.includes('appointments'));
  assert.ok(!names.includes('managed-users'));
  assert.ok(!names.includes('billing'));
  assert.ok(!names.includes('menus'));
  assert.ok(!names.includes('syslogs'));
});

test('医生不展示系统内容和审核归档菜单', () => {
  const names = namesFor('doctor');

  assert.ok(names.includes('records'));
  assert.ok(names.includes('templates'));
  assert.ok(names.includes('orders'));
  assert.ok(names.includes('prescriptions'));
  assert.ok(names.includes('tests'));
  assert.ok(!names.includes('workflow-tasks'));
  assert.ok(!names.includes('messages'));
  assert.ok(!names.includes('config'));
});

test('主任只展示审核归档和只读诊疗相关菜单', () => {
  const names = namesFor('director');

  assert.ok(names.includes('workflow-tasks'));
  assert.ok(names.includes('workflow-audits'));
  assert.ok(names.includes('archive-applications'));
  assert.ok(names.includes('archives'));
  assert.ok(names.includes('records'));
  assert.ok(names.includes('orders'));
  assert.ok(names.includes('prescriptions'));
  assert.ok(names.includes('tests'));
  assert.ok(!names.includes('appointments'));
  assert.ok(!names.includes('departments'));
  assert.ok(!names.includes('billing'));
});

test('过滤后没有空菜单分组，未知角色只保留安全落点', () => {
  const groups = getVisibleAdminNavGroups('guest');

  assert.deepEqual(groups, [{ name: 'dashboard', label: '仪表盘', path: '/dashboard' }]);
  assert.equal(isAdminTabAccessible('guest', 'menus'), false);
  assert.equal(isAdminTabAccessible('guest', 'dashboard'), true);
});
