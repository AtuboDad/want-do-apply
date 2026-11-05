<template>
  <div class="p-2 app-container">
    <el-card shadow="hover" class="mb-3">
      <el-form ref="queryFormRef" :model="queryParams" :inline="true">
        <el-form-item label="昵称" prop="keyword">
          <el-input v-model="queryParams.keyword" placeholder="昵称关键字" clearable style="width: 200px" @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-select v-model="queryParams.status" placeholder="全部" clearable style="width: 120px">
            <el-option label="正常" value="0" />
            <el-option label="已封禁" value="1" />
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
            <h3>小程序用户</h3>
            <span class="hint">封禁后该用户无法发布、批阅与评论</span>
          </div>
          <el-button icon="Refresh" circle @click="getList" />
        </div>
      </template>

      <el-table v-loading="loading" border :data="list">
        <el-table-column label="用户" min-width="200">
          <template #default="{ row }">
            <div class="user-cell">
              <el-avatar :size="34" :src="row.avatar" />
              <div>
                <div class="nick">{{ row.nickName || '匿名用户' }}</div>
                <div class="sub">ID {{ row.id }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="性别" width="80" align="center">
          <template #default="{ row }">
            {{ row.gender === '1' ? '男' : row.gender === '2' ? '女' : '-' }}
          </template>
        </el-table-column>
        <el-table-column label="手机号" width="140" align="center">
          <template #default="{ row }">{{ row.phone || '-' }}</template>
        </el-table-column>
        <el-table-column label="信用分" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="creditType(row.creditScore)" effect="light">{{ row.creditScore ?? 100 }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="发起/批阅" width="120" align="center">
          <template #default="{ row }">
            <span class="num">{{ row.applyCount || 0 }}</span>
            <span class="sep">/</span>
            <span class="num">{{ row.voteCount || 0 }}</span>
          </template>
        </el-table-column>
        <el-table-column label="被采纳" prop="adoptedCount" width="90" align="center" />
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === '1' ? 'danger' : 'success'" effect="light">
              {{ row.status === '1' ? '已封禁' : '正常' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="最近登录" prop="lastLoginTime" width="165" align="center">
          <template #default="{ row }">{{ row.lastLoginTime || '-' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="170" align="center" fixed="right">
          <template #default="{ row }">
            <el-button v-hasPermi="['apply:user:edit']" link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button
              v-hasPermi="['apply:user:edit']"
              link
              :type="row.status === '1' ? 'success' : 'danger'"
              @click="toggleBan(row)"
            >
              {{ row.status === '1' ? '解封' : '封禁' }}
            </el-button>
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

    <el-dialog v-model="editVisible" title="编辑用户" width="460px">
      <el-form v-if="form" label-width="90px">
        <el-form-item label="昵称">
          <el-input v-model="form.nickName" maxlength="64" show-word-limit />
        </el-form-item>
        <el-form-item label="信用分">
          <el-input-number v-model="form.creditScore" :min="0" :max="100" />
          <span class="tip">低于 60 分将被限制发言</span>
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio value="0">正常</el-radio>
            <el-radio value="1">封禁</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="form.status === '1'" label="封禁原因">
          <el-input v-model="form.banRemark" type="textarea" :rows="3" maxlength="200" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitEdit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="ApplyAppUser" lang="ts">
import { onMounted, reactive, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { listAppUser, updateAppUser } from '@/api/apply';
import type { AppUserVO, AdminQuery } from '@/api/apply/types';

const loading = ref(false);
const saving = ref(false);
const list = ref<AppUserVO[]>([]);
const total = ref(0);
const editVisible = ref(false);

const queryParams = reactive<AdminQuery>({
  pageNum: 1,
  pageSize: 10,
  keyword: '',
  status: ''
});

const form = ref<{ id: number; nickName: string; status: string; banRemark: string; creditScore: number } | null>(null);

function creditType(score?: number) {
  const n = score ?? 100;
  if (n >= 90) return 'success';
  if (n >= 60) return 'warning';
  return 'danger';
}

async function getList() {
  loading.value = true;
  try {
    const res = await listAppUser({ ...queryParams });
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
  queryParams.status = '';
  handleQuery();
}

function openEdit(row: AppUserVO) {
  form.value = {
    id: row.id,
    nickName: row.nickName || '',
    status: row.status || '0',
    banRemark: row.banRemark || '',
    creditScore: row.creditScore ?? 100
  };
  editVisible.value = true;
}

async function submitEdit() {
  if (!form.value) return;
  saving.value = true;
  try {
    await updateAppUser({
      id: form.value.id,
      status: form.value.status,
      banRemark: form.value.banRemark,
      creditScore: form.value.creditScore,
      nickName: form.value.nickName
    });
    ElMessage.success('保存成功');
    editVisible.value = false;
    getList();
  } finally {
    saving.value = false;
  }
}

async function toggleBan(row: AppUserVO) {
  const banning = row.status !== '1';
  let banRemark = '';
  if (banning) {
    const { value } = await ElMessageBox.prompt('请输入封禁原因', '封禁用户', {
      confirmButtonText: '确定封禁',
      cancelButtonText: '取消',
      inputPlaceholder: '如：发布违规内容',
      inputValidator: (v: string) => (v && v.trim() ? true : '封禁原因不能为空')
    });
    banRemark = value;
  } else {
    await ElMessageBox.confirm(`确认解封「${row.nickName}」？`, '提示', { type: 'warning' });
  }
  await updateAppUser({
    id: row.id,
    status: banning ? '1' : '0',
    banRemark: banning ? banRemark : '',
    creditScore: row.creditScore ?? 100
  });
  ElMessage.success(banning ? '已封禁' : '已解封');
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

.user-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}

.nick {
  font-weight: 600;
  color: #1a1a1a;
}

.sub {
  font-size: 12px;
  color: #9aa0a6;
}

.num {
  font-weight: 600;
  color: #1a1a1a;
}

.sep {
  color: #d0d3d8;
  margin: 0 3px;
}

.tip {
  margin-left: 10px;
  font-size: 12px;
  color: #9aa0a6;
}
</style>
