<template>
  <div class="p-2 app-container dashboard">
    <el-row :gutter="16">
      <el-col v-for="c in cards" :key="c.key" :xs="12" :sm="12" :md="6">
        <el-card shadow="hover" class="metric-card">
          <div class="metric">
            <div class="metric-label">{{ c.label }}</div>
            <div class="metric-value" :style="{ color: c.color }">
              <span v-if="c.prefix" class="prefix">{{ c.prefix }}</span>{{ c.value }}
            </div>
            <div v-if="c.sub" class="metric-sub">{{ c.sub }}</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="mt-4">
      <el-col :xs="24" :md="16">
        <el-card shadow="hover">
          <template #header>
            <div class="card-head">
              <h3>近 7 天趋势</h3>
              <div class="legend">
                <span class="dot order" />审批单
                <span class="dot vote" />批条
              </div>
            </div>
          </template>
          <div v-loading="loading" class="chart">
            <div v-for="t in trend" :key="t.date" class="chart-col">
              <div class="bars">
                <div class="bar order" :style="{ height: barHeight(t.orderCount, maxOrder) }" :title="'审批单 ' + t.orderCount" />
                <div class="bar vote" :style="{ height: barHeight(t.voteCount, maxVote) }" :title="'批条 ' + t.voteCount" />
              </div>
              <div class="chart-label">{{ t.date.slice(5) }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :md="8">
        <el-card shadow="hover">
          <template #header>
            <h3>品类分布</h3>
          </template>
          <div v-loading="loading">
            <div v-for="c in categories" :key="c.name" class="cat-row">
              <div class="cat-name">{{ categoryLabel(c.name) }}</div>
              <div class="cat-bar">
                <div class="cat-fill" :style="{ width: percent(c.value) }" />
              </div>
              <div class="cat-val">{{ c.value }}</div>
            </div>
            <el-empty v-if="!categories.length" description="暂无数据" :image-size="70" />
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="mt-4">
      <el-col :span="24">
        <el-card shadow="hover">
          <template #header>
            <div class="card-head">
              <h3>快捷入口</h3>
              <el-button icon="Refresh" circle size="small" @click="load" />
            </div>
          </template>
          <div class="quick">
            <div class="quick-item" @click="go('/apply/order')">
              <span class="q-icon">📋</span>
              <span class="q-title">审批单管理</span>
              <span class="q-desc">下架、置顶、删除违规内容</span>
            </div>
            <div class="quick-item" @click="go('/apply/report')">
              <span class="q-icon">🚨</span>
              <span class="q-title">举报处理</span>
              <span class="q-desc">待处理 {{ data.reportPending || 0 }} 条</span>
            </div>
            <div class="quick-item" @click="go('/apply/user')">
              <span class="q-icon">👤</span>
              <span class="q-title">小程序用户</span>
              <span class="q-desc">封禁、信用分调整</span>
            </div>
            <div class="quick-item" @click="go('/apply/group')">
              <span class="q-icon">👥</span>
              <span class="q-title">审批群</span>
              <span class="q-desc">查看与解散违规群</span>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup name="ApplyDashboard" lang="ts">
import { computed, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { getDashboard } from '@/api/apply';
import { categoryLabel } from '@/utils/apply-format';
import type { DashboardVO } from '@/api/apply/types';

const router = useRouter();
const loading = ref(false);
const data = ref<DashboardVO>({
  userTotal: 0,
  userToday: 0,
  orderTotal: 0,
  orderToday: 0,
  pendingTotal: 0,
  voteTotal: 0,
  voteToday: 0,
  rejectRate: 0,
  savedAmount: 0,
  reportPending: 0,
  trend: [],
  categories: []
});

const cards = computed(() => [
  { key: 'u', label: '小程序用户', value: data.value.userTotal, sub: `今日 +${data.value.userToday}`, color: '#1a1a1a' },
  { key: 'o', label: '审批单总数', value: data.value.orderTotal, sub: `今日 +${data.value.orderToday}`, color: '#FF6B35' },
  { key: 'p', label: '待审批', value: data.value.pendingTotal, sub: `累计批条 ${data.value.voteTotal}`, color: '#F59E0B' },
  { key: 'r', label: '驳回率', value: data.value.rejectRate + '%', sub: `帮用户省下 ¥${data.value.savedAmount}`, color: '#22C55E' }
]);

const trend = computed(() => data.value.trend || []);
const categories = computed(() => data.value.categories || []);
const maxOrder = computed(() => Math.max(1, ...trend.value.map((t) => t.orderCount || 0)));
const maxVote = computed(() => Math.max(1, ...trend.value.map((t) => t.voteCount || 0)));
const maxCat = computed(() => Math.max(1, ...categories.value.map((c) => c.value || 0)));

function barHeight(v: number, max: number) {
  const h = Math.round(((v || 0) / max) * 100);
  return Math.max(h, 2) + '%';
}

function percent(v: number) {
  return Math.round(((v || 0) / maxCat.value) * 100) + '%';
}

async function load() {
  loading.value = true;
  try {
    const res = await getDashboard();
    data.value = { ...data.value, ...res.data };
  } finally {
    loading.value = false;
  }
}

function go(path: string) {
  router.push(path);
}

onMounted(load);
</script>

<style scoped lang="scss">
.metric-card {
  margin-bottom: 16px;
}

.metric-label {
  font-size: 13px;
  color: #6b7280;
}

.metric-value {
  font-size: 30px;
  font-weight: 700;
  line-height: 1.3;
  margin-top: 4px;
}

.prefix {
  font-size: 18px;
  margin-right: 2px;
}

.metric-sub {
  font-size: 12px;
  color: #9aa0a6;
  margin-top: 4px;
}

.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.legend {
  font-size: 12px;
  color: #6b7280;
  display: flex;
  align-items: center;
  gap: 4px;
}

.dot {
  display: inline-block;
  width: 8px;
  height: 8px;
  border-radius: 2px;
  margin-left: 10px;
}

.dot.order {
  background: #ff6b35;
}

.dot.vote {
  background: #22c55e;
}

.chart {
  display: flex;
  align-items: flex-end;
  gap: 12px;
  height: 220px;
  padding: 10px 0;
}

.chart-col {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  height: 100%;
}

.bars {
  flex: 1;
  display: flex;
  align-items: flex-end;
  gap: 4px;
  width: 100%;
  justify-content: center;
}

.bar {
  width: 14px;
  border-radius: 4px 4px 0 0;
  min-height: 3px;
  transition: height 0.3s;
}

.bar.order {
  background: linear-gradient(180deg, #ff8a3d, #ff6b35);
}

.bar.vote {
  background: linear-gradient(180deg, #4ade80, #22c55e);
}

.chart-label {
  font-size: 12px;
  color: #9aa0a6;
  margin-top: 8px;
}

.cat-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
}

.cat-name {
  width: 56px;
  font-size: 13px;
  color: #6b7280;
}

.cat-bar {
  flex: 1;
  height: 8px;
  background: #f0f0f2;
  border-radius: 999px;
  overflow: hidden;
}

.cat-fill {
  height: 100%;
  background: linear-gradient(90deg, #ff8a3d, #ff6b35);
  border-radius: 999px;
}

.cat-val {
  width: 40px;
  text-align: right;
  font-size: 13px;
  color: #1a1a1a;
  font-weight: 600;
}

.quick {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 12px;
}

.quick-item {
  border: 1px solid #f0f0f2;
  border-radius: 10px;
  padding: 16px;
  cursor: pointer;
  transition: all 0.2s;
}

.quick-item:hover {
  border-color: #ff6b35;
  background: #fff9f5;
}

.q-icon {
  font-size: 20px;
}

.q-title {
  display: block;
  font-weight: 600;
  margin-top: 6px;
}

.q-desc {
  display: block;
  font-size: 12px;
  color: #9aa0a6;
  margin-top: 2px;
}

.mt-4 {
  margin-top: 16px;
}
</style>
