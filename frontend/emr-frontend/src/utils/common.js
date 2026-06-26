import { ElMessage } from 'element-plus';

export function unwrap(response) {
  if (response.data.code !== 0) {
    throw new Error(response.data.message || '操作失败');
  }
  return response.data.data;
}

export function showError(error) {
  ElMessage.error(error?.message || error?.response?.data?.message || '操作失败');
}

export function statusText(status) {
  const map = {
    'pending': '待处理',
    'pending_audit': '待审核',
    'approved': '已审核',
    'rejected': '已驳回',
    'paid': '已支付',
    'unpaid': '未支付',
    'executed': '已执行',
    'finished': '已完成',
    'in_hospital': '住院中',
    'discharged': '已出院',
    'published': '已发布',
    'draft': '草稿',
    'confirmed': '已确认',
    'cancelled': '已取消',
    'archived': '已归档',
    'completed': '已完成'
  };
  return map[status] || status;
}

export function rowsOf(pageData) {
  return Array.isArray(pageData?.rows) ? pageData.rows : [];
}

export function defaultPager() {
  return { page: 1, limit: 10, total: 0 };
}
