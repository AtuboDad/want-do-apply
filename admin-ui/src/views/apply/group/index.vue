<template>
  <div class="p-2 app-container">
    <el-card shadow="hover" class="mb-3">
      <el-form ref="queryFormRef" :model="queryParams" :inline="true">
        <el-form-item label="群名称" prop="keyword">
          <el-input v-model="queryParams.keyword" placeholder="群名称关键字" clearable style="width: 200px" @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item label="群主ID" prop="userId">
          <el-input v-model="queryParams.userId" placeholder="群主用户ID" clearable style="width: 180px" @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-select v-model="queryParams.status" placeholder="全部" clearable style="width: 120px">
            <el-option label="正常" value="0" />
            <el-option label="已解散" value="1" />
          </el-select>
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
            <h3>审批群</h3>
            <span class="hint">群内发起的审批单仅在群成员间可见</span>
          </div>
          <div>
            <el-button
              v-hasPermi="['apply:group:remove']"
              type="danger"
              plain
              icon="Delete"
              :disabled="multiple"
              @click="handleDelete()"
            >
              解散
            </el-button>
            <el-button icon="Refresh" circle @click="getList" />
          </div>
        </div>
      </template>

      <el-table v-loading="loading" border :data="list" @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="50" align="center" />
        <el-table-column label="群名称" min-width="220">
          <template #default="{ row }">
            <div class="name">
              <span>{{ row.name }}</span>
              <el-tag v-if="row.status === '1'" size="small" type="info" effect="plain">已解散</el-tag>
            </div>
            <div class="sub">{{ row.notice || '暂无群公告' }}</div>
          </template>
        </el-table-column>
        <el-table-column label="群主" width="150">
          <template #default="{ row }">
            {{ row.ownerNickName || '-' }}
            <div class="sub">ID {{ row.ownerId }}</div>
          </template>
        </el-table-column>
        <el-table-column label="成员数" prop="memberCount" width="100" align="center" />
        <el-table-column label="邀请码" width="130" align="center">
          <template #default="{ row }">
            <el-tag size="small" effect="plain" class="code">{{ row.inviteCode }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === '1' ? 'info' : 'success'" effect="light">
              {{ row.status === '1' ? '已解散' : '正常' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" prop="createTime" width="165" align="center" />
        <el-table-column label="操作" width="110" align="center" fixed="right">
          <template #default="{ row }">
            <el-button v-hasPermi="['apply:group:remove']" link type="danger" @click="handleDelete(row)">解散</el-button>
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

<script setup name="ApplyGroup" lang="ts">
import { onMounted, reactive, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { listGroup, delGroup } from '@/api/apply';
import type { ApplyGroupVO, AdminQuery } from '@/api/apply/types';

const loading = ref(false);
const list = ref<ApplyGroupVO[]>([]);
const total = ref(0);
const ids = ref<(number | string)[]>([]);
const multiple = ref(true);

const queryParams = reactive<AdminQuery>({
  pageNum: 1,
  pageSize: 10,
  keyword: '',
  userId: '',
  status: ''
});

async function getList() {
  loading.value = true;
  try {
    const res = await listGroup({ ...queryParams });
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
  queryParams.userId = '';
  queryParams.status = '';
  handleQuery();
}

function handleSelectionChange(rows: ApplyGroupVO[]) {
  ids.value = rows.map((r) => r.id);
  multiple.value = rows.length === 0;
}

async function handleDelete(row?: ApplyGroupVO) {
  const targets = row ? [row.id] : ids.value;
  if (!targets.length) return;
  await ElMessageBox.confirm(
    `确认解散选中的 ${targets.length} 个审批群？解散后群内审批单不再对成员可见`,
    '提示',
    { type: 'warning' }
  );
  await delGroup(targets.join(','));
  ElMessage.success('操作成功');
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

.name {
  display: flex;
  align-items: center;
  gap: 6px;
  font-weight: 600;
  color: #1a1a1a;
}

.code {
  font-family: 'SFMono-Regular', Consolas, monospace;
  letter-spacing: 0.5px;
}

.sub {
  font-size: 12px;
  color: #9aa0a6;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 320px;
}
</style>
