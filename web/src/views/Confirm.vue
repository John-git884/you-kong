<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import TopBar from '../components/TopBar.vue'
import { api } from '../api'
import { cnDate, yuan } from '../utils'

const SHOP_ID = 1

const route = useRoute()
const router = useRouter()

const staffId = Number(route.query.staffId)
const serviceId = Number(route.query.serviceId)
const date = String(route.query.date || '')
const time = String(route.query.time || '')
const startAt = `${date}T${time}:00`

const service = ref(null)
const staff = ref(null)
const name = ref('')
const phone = ref(localStorage.getItem('youkong.phone') || '')
const submitting = ref(false)
const error = ref('')

const endTime = computed(() => {
  if (!service.value) return ''
  const [h, m] = time.split(':').map(Number)
  const total = h * 60 + m + service.value.durationMinutes
  const hh = String(Math.floor(total / 60) % 24).padStart(2, '0')
  const mm = String(total % 60).padStart(2, '0')
  return `${hh}:${mm}`
})

onMounted(async () => {
  try {
    const [staffList, services] = await Promise.all([api.staffList(SHOP_ID), api.serviceList(SHOP_ID)])
    staff.value = staffList.find((s) => s.id === staffId) || null
    service.value = services.find((s) => s.id === serviceId) || null
  } catch (e) {
    error.value = e.message
  }
})

async function submit() {
  error.value = ''
  if (!name.value.trim()) {
    error.value = '请填写称呼'
    return
  }
  if (!/^1\d{10}$/.test(phone.value.trim())) {
    error.value = '请填写 11 位手机号'
    return
  }
  submitting.value = true
  try {
    const booking = await api.createBooking({
      staffId,
      serviceId,
      startAt,
      customerName: name.value.trim(),
      customerPhone: phone.value.trim()
    })
    localStorage.setItem('youkong.phone', phone.value.trim())
    router.replace(`/done/${booking.id}`)
  } catch (e) {
    error.value = e.message
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="view">
    <TopBar title="确认预约" />

    <div class="page">
      <div class="card" style="padding: 18px 16px; margin-top: 8px">
        <div class="summary">
          <div class="summary__line">
            <span class="summary__key">服务</span>
            <span class="summary__val">{{ service ? service.name : '—' }}</span>
          </div>
          <div class="summary__line">
            <span class="summary__key">技师</span>
            <span class="summary__val">{{ staff ? staff.name : '—' }}</span>
          </div>
          <div class="summary__line">
            <span class="summary__key">时间</span>
            <span class="summary__val">{{ cnDate(date) }} {{ time }} – {{ endTime }}</span>
          </div>
          <div class="summary__line summary__total">
            <span class="summary__key">到店支付</span>
            <span class="summary__val">{{ service ? yuan(service.priceCent) : '—' }}</span>
          </div>
        </div>
      </div>

      <label class="field">
        <span class="field__label">怎么称呼您</span>
        <input v-model="name" class="input" placeholder="例如：王女士" maxlength="16" />
      </label>

      <label class="field">
        <span class="field__label">手机号</span>
        <input v-model="phone" class="input" placeholder="用于接收预约提醒" inputmode="numeric" maxlength="11" />
      </label>

      <div v-if="error" class="notice">{{ error }}</div>

      <div class="actions">
        <button class="btn btn--primary" :disabled="submitting" @click="submit">
          {{ submitting ? '正在提交' : '确认预约' }}
        </button>
      </div>

      <p class="section__hint" style="margin-top: 16px; text-align: center">
        提交后请保持手机畅通，到店后向店员核对姓名即可
      </p>
    </div>
  </div>
</template>
