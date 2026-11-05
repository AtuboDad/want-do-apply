<template>
  <div class="p-2 app-container">
    <el-card shadow="hover" class="mb-3">
      <el-form ref="queryFormRef" :model="queryParams" :inline="true">
        <el-form-item label="处理状态" prop="status">
          <el-select v-model="queryParams.status" placeholder="全部" clearable style="width: 130px">
            <el-option v-for="s in REPORT_STATUS" :key="s.value" :label="s.label" :value="s.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="举报对象" prop="targetType">
          <el-select v-model="queryParams.targetType" placeholder="全部" clearable style="width: 130px">
            <el-option v-for="t in TARGET_TYPES" :key="t.value" :label="t.label" :value="t.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="举报人ID" prop="userId">
          <el-input v-model="queryParams.userId" placeholder="举报人用户ID" clearable style="width: 170px" @keyup.enter="handleQuery" />
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
            <h3>举报处理</h3>
            <el-tag v-if="pending > 0" type="danger" effect="dark" size="small">待处理 {{ pending }}</el-tag>
            <span v-else class="hint">暂无待处理举报</span>
          </div>
          <div>
            <el-button
              v-hasPermi="['apply:report:remove']"
              type="danger"
              plain
              icon="Delete"
              :disabled="multiple"
              @click="handleDelete()"
            >
              删除记录
            </el-button>
            <el-button icon="Refresh" circle @click="getList" />
          </div>
        </div>
      </template>

      <el-table v-loading="loading" border :data="list" @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="50" align="center" />
        <el-table-column label="举报对象" width="230">
          <template #default="{ row }">
            <el-tag size="small" effect="plain">{{ targetTypeText(row.targetType) }}</el-tag>
            <span class="tid">#{{ row.targetId }}</span>
            <div class="sub">{{ row.targetSummary || '（内容已删除）' }}</div>
          </template>
        </el-table-column>
        <el-table-column label="举报原因" min-width="240">
          <template #default="{ row }">
            <el-tag size="small" type="warning" effect="plain">{{ reasonLabel(row.reasonType) }}</el-tag>
            <div class="content">{{ row.content || '（未填写补充说明）' }}</div>
          </template>
        </el-table-column>
        <el-table-column label="举报人" width="150">
          <template #default="{ row }">
            {{ row.reporterNickName || '-' }}
            <div class="sub">ID {{ row.reporterId }}</div>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="reportStatusType(row.status)" effect="light">{{ reportStatusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="处理意见" min-width="160">
          <template #default="{ row }">
            <div v-if="row.handleRemark">{{ row.handleRemark }}</div>
            <div v-else class="sub">-</div>
            <div v-if="row.handleTime" class="sub">{{ row.handleTime }}</div>
          </template>
        </el-table-column>
        <el-table-column label="举报时间" prop="createTime" width="165" align="center" />
        <el-table-column label="操作" width="200" align="center" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.status === '0'"
              v-hasPermi="['apply:report:handle']"
              link
              type="primary"
              @click="openHandle(row)"
            >
              处理
            </el-button>
            <el-button
              v-if="row.status === '0'"
              v-hasPermi="['apply:report:handle']"
              link
              type="info"
              @click="quickReject(row)"
            >
              忽略
            </el-button>
            <el-button v-hasPermi="['apply:report:remove']" link type="danger" @click="handleDelete(row)">删除</el-button>
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

    <el-dialog v-model="handleVisible" title="处理举报" width="480px">
      <el-descriptions v-if="current" :column="1" border size="small" class="mb-3">
        <el-descriptions-item label="对象">
          {{ targetTypeText(current.targetType) }} #{{ current.targetId }}
        </el-descriptions-item>
        <el-descriptions-item label="内容">{{ current.targetSummary || '（内容已删除）' }}</el-descriptions-item>
        <el-descriptions-item label="举报说明">{{ current.content || '无' }}</el-descriptions-item>
      </el-descriptions>
      <el-form label-width="90px">
        <el-form-item label="处理意见">
          <el-input
            v-model="handleForm.handleRemark"
            type="textarea"
            :rows="3"
            maxlength="200"
            show-word-limit
            placeholder="如：内容违规，已下架"
          />
        </el-form-item>
        <el-form-item label="同步下架">
          <el-switch v-model="handleForm.takeDown" />
          <span class="tip">开启后同步下架被举报内容</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="handleVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitHandle">确认处理</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="ApplyReport" lang="ts">
import { onMounted, reactive, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { listReport, handleReport, delReport, pendingReport } from '@/api/apply';
import type { ApplyReportVO, AdminQuery } from '@/api/apply/types';
import {
  REPORT_STATUS,
  TARGET_TYPES,
  reportStatusText,
  reportStatusType,
  targetTypeText
} from '@/utils/apply-format';

const REASON_TYPES: Record<string, string> = {
  spam: '垃圾广告',
  abuse: '辱骂攻击',
  fake: '虚假信息',
  porn: '色情低俗',
  other: '其他'
};

const loading = ref(false);
const saving = ref(false);
const list = ref<ApplyReportVO[]>([]);
const total = ref(0);
const pending = ref(0);
const ids = ref<(number | string)[]>([]);
const multiple = ref(true);
const dateRange = ref<string[]>([]);
const handleVisible = ref(false);
const current = ref<ApplyReportVO | null>(null);

const queryParams = reactive<AdminQuery>({
  pageNum: 1,
  pageSize: 10,
  status: '0',
  targetType: '',
  userId: ''
});

const handleForm = reactive({ handleRemark: '', takeDown: true });

function reasonLabel(v?: string) {
  return REASON_TYPES[v || 'other'] || v || '其他';
}

async function getList() {
  loading.value = true;
  try {
    const params: AdminQuery = { ...queryParams };
    if (dateRange.value && dateRange.value.length === 2) {
      params.beginTime = dateRange.value[0];
      params.endTime = dateRange.value[1];
    }
    const res = await listReport(params);
    list.value = res.data.rows || [];
    total.value = res.data.total || 0;
  } finally {
    loading.value = false;
  }
  refreshPending();
}

async function refreshPending() {
  try {
    const res = await pendingReport();
    pending.value = Number(res.data || 0);
  } catch {
    pending.value = 0;
  }
}

function handleQuery() {
  queryParams.pageNum = 1;
  getList();
}

function resetQuery() {
  queryParams.status = '0';
  queryParams.targetType = '';
  queryParams.userId = '';
  dateRange.value = [];
  handleQuery();
}

function handleSelectionChange(rows: ApplyReportVO[]) {
  ids.value = rows.map((r) => r.id);
  multiple.value = rows.length === 0;
}

function openHandle(row: ApplyReportVO) {
  current.value = row;
  handleForm.handleRemark = '';
  handleForm.takeDown = true;
  handleVisible.value = true;
}

async function submitHandle() {
  if (!current.value) return;
  saving.value = true;
  try {
    await handleReport(
      {
        id: current.value.id,
        targetType: current.value.targetType,
        targetId: current.value.targetId,
        status: '1',
        handleRemark: handleForm.handleRemark
      },
      handleForm.takeDown
    );
    ElMessage.success('处理完成');
    handleVisible.value = false;
    getList();
  } finally {
    saving.value = false;
  }
}

async function quickReject(row: ApplyReportVO) {
  await ElMessageBox.confirm('确认忽略该举报（不移除内容）？', '提示', { type: 'warning' });
  await handleReport(
    {
      id: row.id,
      targetType: row.targetType,
      targetId: row.targetId,
      status: '2',
      handleRemark: '经核实未违规'
    },
    false
  );
  ElMessage.success('已忽略');
  getList();
}

async function handleDelete(row?: ApplyReportVO) {
  const targets = row ? [row.id] : ids.value;
  if (!targets.length) return;
  await ElMessageBox.confirm(`确认删除选中的 ${targets.length} 条举报记录？仅删除记录，不影响内容`, '提示', {
    type: 'warning'
  });
  await delReport(targets.join(','));
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
  align-items: center;
  gap: 10px;
}

.hint {
  font-size: 12px;
  color: #9aa0a6;
}

.tid {
  margin-left: 6px;
  font-weight: 600;
  color: #1a1a1a;
}

.content {
  margin-top: 4px;
  font-size: 13px;
  color: #4b5563;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.sub {
  font-size: 12px;
  color: #9aa0a6;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 300px;
}

.tip {
  margin-left: 10px;
  font-size: 12px;
  color: #9aa0a6;
}
</style>
