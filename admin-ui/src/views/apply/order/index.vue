<template>
  <div class="p-2 app-container">
    <el-card shadow="hover" class="mb-3">
      <el-form ref="queryFormRef" :model="queryParams" :inline="true">
        <el-form-item label="单号" prop="id">
          <el-input v-model="queryParams.id" placeholder="审批单ID" clearable style="width: 180px" @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item label="商品名称" prop="title">
          <el-input v-model="queryParams.title" placeholder="请输入商品名称" clearable style="width: 180px" @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item label="发起人" prop="nickName">
          <el-input v-model="queryParams.nickName" placeholder="昵称" clearable style="width: 140px" @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-select v-model="queryParams.status" placeholder="全部" clearable style="width: 120px">
            <el-option v-for="d in statusOptions" :key="d.value" :label="d.label" :value="d.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="范围" prop="visibility">
          <el-select v-model="queryParams.visibility" placeholder="全部" clearable style="width: 120px">
            <el-option label="公开广场" value="0" />
            <el-option label="审批群内" value="1" />
          </el-select>
        </el-form-item>
        <el-form-item label="审核" prop="auditStatus">
          <el-select v-model="queryParams.auditStatus" placeholder="全部" clearable style="width: 120px">
            <el-option label="待审核" value="0" />
            <el-option label="已通过" value="1" />
            <el-option label="已下架" value="2" />
          </el-select>
        </el-form-item>
        <el-form-item label="创建时间">
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            value-format="YYYY-MM-DD HH:mm:ss"
            start-placeholder="开始"
            end-placeholder="结束"
            :default-time="[new Date(2000, 1, 1, 0, 0, 0), new Date(2000, 1, 1, 23, 59, 59)]"
            style="width: 320px"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
          <el-button icon="Refresh" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="hover">
      <template #header>
        <div class="between">
          <h3>审批单列表</h3>
          <div>
            <el-button
              v-hasPermi="['apply:order:remove']"
              type="danger"
              plain
              icon="Delete"
              :disabled="multiple"
              @click="handleDelete()"
            >
              删除
            </el-button>
            <el-button icon="Refresh" circle @click="getList" />
          </div>
        </div>
      </template>

      <el-table v-loading="loading" border :data="list" @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="50" align="center" />
        <el-table-column label="ID" prop="id" width="170" align="center" />
        <el-table-column label="商品" min-width="200">
          <template #default="{ row }">
            <div class="title-cell" @click="openDetail(row)">
              <el-tag v-if="row.isTop === '1'" size="small" type="danger" effect="plain">置顶</el-tag>
              <span class="title-text">{{ row.title }}</span>
            </div>
            <div class="sub">{{ row.reason }}</div>
          </template>
        </el-table-column>
        <el-table-column label="价格" prop="price" width="110" align="right">
          <template #default="{ row }">
            <span class="price">¥{{ formatMoney(row.price) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="发起人" width="150">
          <template #default="{ row }">
            <div class="user-cell">
              <el-avatar :size="24" :src="row.userAvatar" />
              <span>{{ row.userNickName || '-' }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="票数(过/驳)" width="120" align="center">
          <template #default="{ row }">
            <span class="pass">{{ row.passCount }}</span>
            <span class="sep">/</span>
            <span class="reject">{{ row.rejectCount }}</span>
            <div class="sub">已表态 {{ row.voteCount }} 人</div>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" effect="light">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="审核" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="auditTagType(row.auditStatus)" effect="plain" size="small">
              {{ auditText(row.auditStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" prop="createTime" width="165" align="center" />
        <el-table-column label="操作" width="210" align="center" fixed="right">
          <template #default="{ row }">
            <el-button v-hasPermi="['apply:order:query']" link type="primary" @click="openDetail(row)">详情</el-button>
            <el-button v-hasPermi="['apply:order:audit']" link type="warning" @click="handleAudit(row)">
              {{ row.auditStatus === '2' ? '恢复' : '下架' }}
            </el-button>
            <el-button v-hasPermi="['apply:order:edit']" link type="info" @click="handleTop(row)">
              {{ row.isTop === '1' ? '取消置顶' : '置顶' }}
            </el-button>
            <el-button v-hasPermi="['apply:order:remove']" link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <pagination
        v-show="total > 0"
        v-model:page="queryParams.pageNum"
        v-model:limit="queryParams.pageSize"
        :total="total"
        @pagination="getList"
      />
    </el-card>

    <!-- 详情 -->
    <el-drawer v-model="detailVisible" title="审批单详情" size="520px">
      <template v-if="current">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="商品">{{ current.title }}</el-descriptions-item>
          <el-descriptions-item label="价格">¥{{ formatMoney(current.price) }}</el-descriptions-item>
          <el-descriptions-item label="发起人">{{ current.userNickName }}（ID {{ current.userId }}）</el-descriptions-item>
          <el-descriptions-item label="购买理由">{{ current.reason }}</el-descriptions-item>
          <el-descriptions-item label="替代品">{{ current.substitute || '无' }}</el-descriptions-item>
          <el-descriptions-item label="票数">
            通过 {{ current.passCount }} / 驳回 {{ current.rejectCount }}（已表态 {{ current.voteCount }} 人）
          </el-descriptions-item>
          <el-descriptions-item label="审批时长">{{ current.durationHours || 24 }} 小时</el-descriptions-item>
          <el-descriptions-item label="状态">{{ statusText(current.status) }}</el-descriptions-item>
          <el-descriptions-item label="范围">{{ current.visibility === '1' ? '群内：' + (current.groupName || '') : '公开广场' }}</el-descriptions-item>
          <el-descriptions-item label="截止">{{ current.expireTime }}</el-descriptions-item>
          <el-descriptions-item label="结算">{{ current.settleTime || '-' }}</el-descriptions-item>
        </el-descriptions>
        <div v-if="current.imageList && current.imageList.length" class="img-list">
          <el-image
            v-for="(img, i) in current.imageList"
            :key="i"
            :src="img"
            :preview-src-list="current.imageList"
            :initial-index="i"
            fit="cover"
            class="detail-img"
          />
        </div>
      </template>
    </el-drawer>
  </div>
</template>

<script setup name="ApplyOrder" lang="ts">
import { onMounted, reactive, ref } from 'vue';
import { useRoute } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import { listOrder, delOrder, auditOrder, topOrder } from '@/api/apply';
import type { ApplyOrderVO, ApplyOrderQuery } from '@/api/apply/types';

const route = useRoute();
const loading = ref(false);
const list = ref<ApplyOrderVO[]>([]);
const total = ref(0);
const ids = ref<(number | string)[]>([]);
const multiple = ref(true);
const dateRange = ref<string[]>([]);
const detailVisible = ref(false);
const current = ref<ApplyOrderVO | null>(null);

const statusOptions = [
  { value: '0', label: '待审批' },
  { value: '1', label: '已通过' },
  { value: '2', label: '已驳回' },
  { value: '3', label: '已撤销' },
  { value: '4', label: '已过期' }
];

const queryParams = reactive<ApplyOrderQuery>({
  pageNum: 1,
  pageSize: 10,
  title: '',
  nickName: '',
  status: '',
  visibility: '',
  auditStatus: ''
});

function formatMoney(v?: number) {
  const n = Number(v || 0);
  return Number.isInteger(n) ? n : n.toFixed(2);
}

function statusText(s: string) {
  return statusOptions.find((x) => x.value === s)?.label || '-';
}

function statusTagType(s: string) {
  if (s === '1') return 'success';
  if (s === '2') return 'danger';
  if (s === '0') return 'warning';
  return 'info';
}

function auditText(s: string) {
  if (s === '0') return '待审核';
  if (s === '2') return '已下架';
  return '正常';
}

function auditTagType(s: string) {
  if (s === '2') return 'danger';
  if (s === '0') return 'warning';
  return 'success';
}

async function getList() {
  loading.value = true;
  try {
    const params: ApplyOrderQuery = { ...queryParams };
    if (dateRange.value && dateRange.value.length === 2) {
      params.beginTime = dateRange.value[0];
      params.endTime = dateRange.value[1];
    }
    const res = await listOrder(params);
    list.value = res.data.rows || [];
    total.value = res.data.total || 0;
  } finally {
    loading.value = false;
  }
}

function handleQuery() {
  queryParams.pageNum = 1;
  getList();
}

function resetQuery() {
  queryParams.id = '';
  queryParams.title = '';
  queryParams.nickName = '';
  queryParams.status = '';
  queryParams.visibility = '';
  queryParams.auditStatus = '';
  dateRange.value = [];
  handleQuery();
}

function handleSelectionChange(rows: ApplyOrderVO[]) {
  ids.value = rows.map((r) => r.id);
  multiple.value = rows.length === 0;
}

function openDetail(row: ApplyOrderVO) {
  current.value = row;
  detailVisible.value = true;
}

async function handleAudit(row: ApplyOrderVO) {
  const offline = row.auditStatus !== '2';
  await ElMessageBox.confirm(
    offline ? `确认下架「${row.title}」？下架后小程序端不可见` : `确认恢复「${row.title}」？`,
    '提示',
    { type: 'warning' }
  );
  await auditOrder(row.id, offline ? '2' : '1', offline ? '后台下架' : '后台恢复');
  ElMessage.success('操作成功');
  getList();
}

async function handleTop(row: ApplyOrderVO) {
  const next = row.isTop === '1' ? '0' : '1';
  await topOrder(row.id, next);
  ElMessage.success(next === '1' ? '已置顶' : '已取消置顶');
  getList();
}

async function handleDelete(row?: ApplyOrderVO) {
  const targets = row ? [row.id] : ids.value;
  await ElMessageBox.confirm(`确认删除选中的 ${targets.length} 条审批单？`, '提示', { type: 'warning' });
  await delOrder(targets.join(','));
  ElMessage.success('删除成功');
  getList();
}

onMounted(() => {
  // 支持从批条/评论页跳转时按审批单 ID 精确查询
  const qid = route.query.id;
  if (qid) {
    queryParams.id = String(qid);
  }
  getList();
});
</script>

<style scoped lang="scss">
.between {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.title-cell {
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
}

.title-text {
  font-weight: 600;
  color: #1a1a1a;
}

.sub {
  font-size: 12px;
  color: #9aa0a6;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 360px;
}

.price {
  color: #ff6b35;
  font-weight: 600;
}

.user-cell {
  display: flex;
  align-items: center;
  gap: 6px;
}

.pass {
  color: #22c55e;
  font-weight: 600;
}

.reject {
  color: #ef4444;
  font-weight: 600;
}

.sep {
  color: #d0d3d8;
  margin: 0 2px;
}

.img-list {
  margin-top: 16px;
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.detail-img {
  width: 140px;
  height: 140px;
  border-radius: 8px;
}
</style>
