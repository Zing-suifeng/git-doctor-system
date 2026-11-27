<script setup>
import { ref } from 'vue'
import axios from 'axios'
import { ElMessage } from 'element-plus'

const repoUrl = ref('')
const loading = ref(false)
const report = ref(null)

async function doCheck() {
  if (!repoUrl.value.trim()) {
    ElMessage.warning('请先填写 GitHub 仓库地址')
    return
  }
  loading.value = true
  report.value = null
  try {
    const resp = await axios.post('/api/doctor/check', { repoUrl: repoUrl.value })
    if (resp.data.code === 200) {
      report.value = resp.data.data
    } else {
      ElMessage.error(resp.data.msg || '体检失败')
    }
  } catch (e) {
    const msg = e.response?.data?.msg || '请求失败，请检查后端是否启动'
    ElMessage.error(msg)
  } finally {
    loading.value = false
  }
}

function levelType(level) {
  if (level === '可接入') return 'success'
  if (level === '需整改') return 'warning'
  return 'danger'
}

function riskTag(level) {
  return level === 'HIGH' ? 'danger' : (level === 'MEDIUM' ? 'warning' : 'info')
}
</script>

<template>
  <div class="page">
    <el-card class="search-card">
      <template #header>
        <div class="card-header">
          <span>Git 仓库接入体检工具</span>
        </div>
      </template>
      <p class="hint">输入公开的 Git 仓库地址，自动检查文档完整度、Windows 脚本、硬编码路径与密钥泄露风险。</p>
      <el-input
        v-model="repoUrl"
        placeholder="例:https://github.com/owner/repo"
        clearable
        @keyup.enter="doCheck"
      />
      <div class="action">
        <el-button type="primary" :loading="loading" @click="doCheck">
          {{ loading ? '体检中...' : '开始体检' }}
        </el-button>
      </div>
    </el-card>

    <el-card v-if="report" class="result-card">
      <template #header>
        <div class="card-header">
          <span>体检报告 · {{ report.repoFullName }}</span>
          <el-tag :type="levelType(report.level)" size="small">{{ report.level }}</el-tag>
        </div>
      </template>

      <el-row :gutter="20">
        <el-col :span="8">
          <div class="score-box">
            <div class="score-num">{{ report.score }}</div>
            <div class="score-label">接入就绪度 / 100</div>
          </div>
        </el-col>
        <el-col :span="16">
          <p class="summary">{{ report.summary }}</p>
          <p class="meta">体检时间：{{ report.checkedAt }}</p>
        </el-col>
      </el-row>

      <el-divider content-position="left">各项检测</el-divider>
      <div v-for="d in report.dimensions" :key="d.name" class="dimension">
        <div class="dimension-head">
          <span>{{ d.name }}</span>
          <span>{{ d.score }} / {{ d.fullScore }}</span>
        </div>
        <el-progress
          :percentage="d.fullScore ? Math.round(d.score * 100 / d.fullScore) : 0"
          :stroke-width="14"
        />
        <div class="dimension-detail">{{ d.detail }}</div>
      </div>

      <el-divider content-position="left">风险清单（{{ report.risks.length }}）</el-divider>
      <el-table :data="report.risks" size="small" border v-if="report.risks.length">
        <el-table-column prop="level" label="等级" width="90">
          <template #default="{ row }">
            <el-tag :type="riskTag(row.level)" size="small">{{ row.level }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="type" label="风险类型" width="170" />
        <el-table-column prop="file" label="文件" min-width="220" show-overflow-tooltip />
        <el-table-column prop="line" label="行号" width="70" />
        <el-table-column prop="message" label="说明" min-width="260" show-overflow-tooltip />
      </el-table>
      <el-empty v-else description="未发现风险项" :image-size="60" />

      <template v-if="report.aiAdvice">
        <el-divider content-position="left">AI 接入建议</el-divider>
        <el-alert type="info" :closable="false" show-icon>
          <template #title><span class="ai-tag">由 DeepSeek 生成，仅供参考</span></template>
          <div class="ai-advice">{{ report.aiAdvice }}</div>
        </el-alert>
      </template>
    </el-card>
  </div>
</template>

<style scoped>
.page {
  max-width: 1000px;
  margin: 32px auto;
  padding: 0 20px;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 500;
}
.hint {
  color: #606266;
  font-size: 14px;
  margin: 0 0 16px;
}
.action {
  margin-top: 16px;
  text-align: right;
}
.result-card {
  margin-top: 20px;
}
.score-box {
  text-align: center;
  padding: 12px 0;
}
.score-num {
  font-size: 52px;
  font-weight: 500;
  line-height: 1.1;
}
.score-label {
  color: #909399;
  font-size: 13px;
}
.summary {
  margin: 0 0 8px;
  font-size: 15px;
}
.meta {
  color: #909399;
  font-size: 13px;
  margin: 0;
}
.dimension {
  margin-bottom: 16px;
}
.dimension-head {
  display: flex;
  justify-content: space-between;
  font-size: 14px;
  margin-bottom: 6px;
}
.dimension-detail {
  color: #909399;
  font-size: 12px;
  margin-top: 4px;
}
.ai-tag {
  font-weight: 500;
}
.ai-advice {
  white-space: pre-wrap;
  line-height: 1.7;
  font-size: 14px;
  margin-top: 8px;
}
</style>
