<script setup>
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import StatusPill from '../components/StatusPill.vue'
import { api } from '../api'
import { cnDate, hhmm } from '../utils'

const route = useRoute()
const booking = ref(null)
const loading = ref(true)
const error = ref('')

onMounted(async () => {
  try {
    booking.value = await api.booking(Number(route.params.id))
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div class="view">
    <div class="page" style="padding-top: 0">
      <div class="center">
        <div class="mark">
          <svg width="26" height="26" viewBox="0 0 24 24" fill="none" stroke="currentColor"
            stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M20 6L9 17l-5-5" />
          </svg>
        </div>
        <h1 style="font-size: 21px; margin: 0 0 6px; font-weight: 500">预约已提交</h1>
        <p style="margin: 0; color: var(--ink-2); font-size: 14px">
          店家确认后会给您发提醒
        </p>
      </div>

      <div v-if="loading" class="loading">加载中</div>
      <div v-else-if="error" class="notice">{{ error }}</div>

      <div v-else-if="booking" class="card" style="padding: 18px 16px; margin-top: 26px">
        <div class="summary">
          <div class="summary__line">
            <span class="summary__key">预约号</span>
            <span class="summary__val">#{{ String(booking.id).padStart(4, '0') }}</span>
          </div>
          <div class="summary__line">
            <span class="summary__key">状态</span>
            <span class="summary__val"><StatusPill :status="booking.status" /></span>
          </div>
          <div class="summary__line">
            <span class="summary__key">服务</span>
            <span class="summary__val">{{ booking.serviceName }}</span>
          </div>
          <div class="summary__line">
            <span class="summary__key">技师</span>
            <span class="summary__val">{{ booking.staffName }}</span>
          </div>
          <div class="summary__line">
            <span class="summary__key">时间</span>
            <span class="summary__val">
              {{ cnDate(booking.startAt) }} {{ hhmm(booking.startAt) }} – {{ hhmm(booking.endAt) }}
            </span>
          </div>
        </div>
      </div>

      <div class="actions">
        <router-link to="/mine" custom v-slot="{ navigate }">
          <button class="btn btn--ghost" @click="navigate">我的预约</button>
        </router-link>
        <router-link to="/" custom v-slot="{ navigate }">
          <button class="btn btn--primary" @click="navigate">回首页</button>
        </router-link>
      </div>
    </div>
  </div>
</template>
