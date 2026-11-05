<template>
  <div class="p-2 app-container">
    <el-card shadow="hover" class="mb-3">
      <el-form ref="queryFormRef" :model="queryParams" :inline="true">
        <el-form-item label="批语" prop="keyword">
          <el-input v-model="queryParams.keyword" placeholder="批语关键字" clearable style="width: 200px" @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item label="结论" prop="result">
          <el-select v-model="queryParams.result" placeholder="全部" clearable style="width: 120px">
            <el-option label="通过" value="1" />
            <el-option label="驳回" value="2" />
          </el-select>
        </el-form-item>
        <el-form-item label="审批单ID" prop="orderId">
          <el-input v-model="queryParams.orderId" placeholder="审批单ID" clearable style="width: 180px" @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item label="时间">
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
          <div class="hd">
            <h3>批条列表</h3>
            <span class="hint">用户对审批单给出的亲笔批语</span>
          </div>
          <div>
            <el-button
              v-hasPermi="['apply:vote:remove']"
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
        <el-table-column label="批语" min-width="280">
          <template #default="{ row }">
            <div class="reason">{{ row.reason }}</div>
            <div class="sub">批条 ID {{ row.id }}</div>
          </template>
        </el-table-column>
        <el-table-column label="结论" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.result === '1' ? 'success' : 'danger'" effect="light">
              {{ row.result === '1' ? '通过' : '驳回' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="批阅人" width="160">
          <template #default="{ row }">
            <div class="user-cell">
              <el-avatar :size="24" :src="row.userAvatar" />
              <span>{{ row.userNickName || '-' }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="所属审批单" width="180" align="center">
          <template #default="{ row }">
            <el-button link type="primary" @click="gotoOrder(row.orderId)">#{{ row.orderId }}</el-button>
          </template>
        </el-table-column>
        <el-table-column label="获赞" prop="likeCount" width="80" align="center" />
        <el-table-column label="时间" prop="createTime" width="165" align="center" />
        <el-table-column label="操作" width="110" align="center" fixed="right">
          <template #default="{ row }">
            <el-button v-hasPermi="['apply:vote:remove']" link type="danger" @click="handleDelete(row)">删除</el-button>
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
  </div>
</template>

<script setup name="ApplyVote" lang="ts">
import { onMounted, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import { listVote, delVote } from '@/api/apply';
import type { ApplyVoteVO, AdminQuery } from '@/api/apply/types';

const router = useRouter();
const loading = ref(false);
const list = ref<ApplyVoteVO[]>([]);
const total = ref(0);
const ids = ref<(number | string)[]>([]);
const multiple = ref(true);
const dateRange = ref<string[]>([]);

const queryParams = reactive<AdminQuery>({
  pageNum: 1,
  pageSize: 10,
  keyword: '',
  result: '',
  orderId: ''
});

async function getList() {
  loading.value = true;
  try {
    const params: AdminQuery = { ...queryParams };
    if (dateRange.value && dateRange.value.length === 2) {
      params.beginTime = dateRange.value[0];
      params.endTime = dateRange.value[1];
    }
    const res = await listVote(params);
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
  queryParams.keyword = '';
  queryParams.result = '';
  queryParams.orderId = '';
  dateRange.value = [];
  handleQuery();
}

function handleSelectionChange(rows: ApplyVoteVO[]) {
  ids.value = rows.map((r) => r.id);
  multiple.value = rows.length === 0;
}

function gotoOrder(orderId: number) {
  router.push({ path: '/apply/order', query: { id: String(orderId) } });
}

async function handleDelete(row?: ApplyVoteVO) {
  const targets = row ? [row.id] : ids.value;
  if (!targets.length) return;
  await ElMessageBox.confirm(`确认删除选中的 ${targets.length} 条批条？`, '提示', { type: 'warning' });
  await delVote(targets.join(','));
  ElMessage.success('删除成功');
  getList();
}

onMounted(getList);
</script>

<style scoped lang="scss">
.between {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.hd {
  display: flex;
  align-items: baseline;
  gap: 10px;
}

.hint {
  font-size: 12px;
  color: #9aa0a6;
}

.reason {
  font-weight: 500;
  color: #1a1a1a;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.sub {
  font-size: 12px;
  color: #9aa0a6;
  margin-top: 2px;
}

.user-cell {
  display: flex;
  align-items: center;
  gap: 6px;
}
</style>
