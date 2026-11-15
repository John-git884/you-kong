<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import TopBar from '../components/TopBar.vue'
import StatusPill from '../components/StatusPill.vue'
import { api } from '../api'
import { cnDate, hhmm } from '../utils'

const router = useRouter()
const phone = ref(localStorage.getItem('youkong.phone') || '')
const list = ref([])
const loading = ref(false)
const searched = ref(false)
const error = ref('')

async function search() {
  error.value = ''
  const value = phone.value.trim()
  if (!/^1\d{10}$/.test(value)) {
    error.value = '请填写 11 位手机号'
    return
  }
  loading.value = true
  try {
    list.value = await api.mine(value)
    localStorage.setItem('youkong.phone', value)
    searched.value = true
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
}

async function cancel(item) {
  if (!window.confirm('确定要取消这个预约吗？取消后档期会释放给其他人。')) {
    return
  }
  try {
    await api.cancel(item.id)
    await search()
  } catch (e) {
    error.value = e.message
  }
}

function reschedule(item) {
  router.push({
    path: '/time',
    query: {
      staffId: item.staffId,
      serviceId: item.serviceId,
      bookingId: item.id,
      date: item.startAt.slice(0, 10)
    }
  })
}

const canCancel = (item) => item.status === 'PENDING' || item.status === 'CONFIRMED'

onMounted(() => {
  if (phone.value) search()
})
</script>

<template>
  <div class="view">
    <TopBar title="我的预约" />

    <div class="page">
      <div style="display: flex; gap: 10px; align-items: flex-end; margin-top: 8px">
        <label class="field" style="flex: 1; margin-top: 0">
          <span class="field__label">手机号</span>
          <input v-model="phone" class="input" placeholder="预约时填写的手机号"
            inputmode="numeric" maxlength="11" @keyup.enter="search" />
        </label>
        <button class="btn btn--ghost" style="height: 48px; min-height: 48px; padding: 0 18px"
          :disabled="loading" @click="search">
          查询
        </button>
      </div>

      <div v-if="error" class="notice">{{ error }}</div>

      <p v-if="!searched && !loading && !error" class="section__hint" style="margin-top: 22px">
        填预约时用的手机号，可以看到对应的预约记录。
      </p>

      <div v-if="loading" class="loading">查询中</div>

      <div v-else-if="searched && !list.length" class="empty">
        这个手机号还没有预约记录
      </div>

      <div v-else-if="list.length" class="section">
        <div class="section__head">
          <span class="section__title">共 {{ list.length }} 条</span>
        </div>

        <div v-for="item in list" :key="item.id" class="card" style="padding: 16px">
          <div style="display: flex; align-items: center; gap: 10px">
            <span style="font-size: 15px; font-weight: 500">{{ item.serviceName }}</span>
            <StatusPill :status="item.status" />
            <span style="margin-left: auto; font-size: 12px; color: var(--ink-3)">
              #{{ String(item.id).padStart(4, '0') }}
            </span>
          </div>
          <div class="row__sub" style="margin-top: 6px">
            {{ cnDate(item.startAt) }} {{ hhmm(item.startAt) }} – {{ hhmm(item.endAt) }} ·
            {{ item.staffName }}
          </div>
          <div v-if="canCancel(item)" style="display: flex; justify-content: flex-end; gap: 4px">
            <button class="btn btn--quiet btn--sm" @click="reschedule(item)">改期</button>
            <button class="btn btn--quiet btn--sm" style="color: var(--clay)" @click="cancel(item)">
              取消预约
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
