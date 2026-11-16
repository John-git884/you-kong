<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import TopBar from '../components/TopBar.vue'
import StatusPill from '../components/StatusPill.vue'
import { api, clearToken } from '../api'
import { hhmm, nextDays, ymd } from '../utils'

const SHOP_ID = 1

const router = useRouter()

function logout() {
  clearToken()
  router.replace('/admin/login')
}

const days = nextDays(7)
const activeDate = ref(ymd(new Date()))
const list = ref([])
const loading = ref(true)
const error = ref('')
const busy = ref(0)

// 已经过了开场时间：此时「取消」在业务上已不成立，只能核销或标记爽约
const started = (item) => new Date(item.startAt).getTime() <= Date.now()

const stats = computed(() => ({
  total: list.value.filter((item) => item.status !== 'CANCELLED').length,
  pending: list.value.filter((item) => item.status === 'PENDING').length,
  confirmed: list.value.filter((item) => item.status === 'CONFIRMED').length
}))

async function load() {
  loading.value = true
  error.value = ''
  try {
    list.value = await api.adminBookings(SHOP_ID, activeDate.value)
  } catch (e) {
    error.value = e.message
    list.value = []
  } finally {
    loading.value = false
  }
}

async function act(item, action) {
  busy.value = item.id
  error.value = ''
  try {
    await api[action](item.id)
    await load()
  } catch (e) {
    error.value = e.message
  } finally {
    busy.value = 0
  }
}

onMounted(load)
watch(activeDate, load)
</script>

<template>
  <div class="view">
    <TopBar title="预约台" :back="false">
      <template #aside>
        <router-link class="topbar__aside" to="/">顾客端</router-link>
        <button class="topbar__action" @click="logout">退出</button>
      </template>
    </TopBar>

    <div class="days">
      <button v-for="day in days" :key="day.date" class="day"
        :class="{ active: day.date === activeDate }" @click="activeDate = day.date">
        <span class="day__label">{{ day.label }}</span>
        <span class="day__num">{{ day.day }}</span>
      </button>
    </div>

    <div class="page">
      <div style="display: grid; grid-template-columns: repeat(3, 1fr); gap: 9px">
        <div class="stat">
          <div class="stat__label">当日单量</div>
          <div class="stat__value">{{ stats.total }}</div>
        </div>
        <div class="stat">
          <div class="stat__label">待确认</div>
          <div class="stat__value" style="color: var(--amber)">{{ stats.pending }}</div>
        </div>
        <div class="stat">
          <div class="stat__label">已确认</div>
          <div class="stat__value" style="color: var(--sage)">{{ stats.confirmed }}</div>
        </div>
      </div>

      <div class="toolbar">
        <router-link to="/admin/services" custom v-slot="{ navigate }">
          <button class="btn btn--ghost btn--sm" @click="navigate">服务项目</button>
        </router-link>
        <router-link to="/admin/staff" custom v-slot="{ navigate }">
          <button class="btn btn--ghost btn--sm" @click="navigate">员工排班</button>
        </router-link>
      </div>

      <div v-if="error" class="notice">{{ error }}</div>

      <div v-if="loading" class="loading">加载中</div>
      <div v-else-if="!list.length" class="empty">这一天还没有预约</div>

      <div v-else class="section" style="margin-top: 18px">
        <div v-for="item in list" :key="item.id" class="card"
          :class="{ 'is-dim': item.status === 'CANCELLED' || item.status === 'COMPLETED' }"
          style="padding: 14px 16px">
          <div style="display: flex; align-items: center; gap: 12px">
            <span style="font-size: 16px; font-weight: 500; font-variant-numeric: tabular-nums">
              {{ hhmm(item.startAt) }}
            </span>
            <span style="color: var(--ink-3); font-size: 13px">{{ hhmm(item.endAt) }}</span>
            <span style="margin-left: auto"><StatusPill :status="item.status" /></span>
          </div>

          <div style="margin-top: 8px; font-size: 14px">
            {{ item.customerName }}
            <span style="color: var(--ink-3); margin-left: 6px">{{ item.customerPhone }}</span>
          </div>
          <div class="row__sub">{{ item.serviceName }} · {{ item.staffName }}</div>

          <div v-if="item.status === 'PENDING' || item.status === 'CONFIRMED'"
            style="display: flex; gap: 8px; justify-content: flex-end; margin-top: 10px">
            <button v-if="item.status === 'PENDING' && !started(item)" class="btn btn--ghost btn--sm"
              :disabled="busy === item.id" @click="act(item, 'confirm')">
              确认
            </button>
            <button v-if="item.status === 'CONFIRMED'" class="btn btn--ghost btn--sm"
              :disabled="busy === item.id" @click="act(item, 'complete')">
              到店核销
            </button>
            <button v-if="started(item)" class="btn btn--ghost btn--sm"
              style="color: var(--danger); border-color: #e8cfc8"
              :disabled="busy === item.id" @click="act(item, 'noShow')">
              标记爽约
            </button>
            <button v-else class="btn btn--quiet btn--sm" style="color: var(--clay)"
              :disabled="busy === item.id" @click="act(item, 'adminCancel')">
              取消
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
