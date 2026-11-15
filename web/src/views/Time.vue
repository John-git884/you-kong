<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import TopBar from '../components/TopBar.vue'
import { api } from '../api'
import { nextDays } from '../utils'

const route = useRoute()
const router = useRouter()
const staffId = Number(route.query.staffId)
const serviceId = Number(route.query.serviceId)
const bookingId = Number(route.query.bookingId) || 0

const initialDate = route.query.date ? new Date(`${route.query.date}T00:00:00`) : new Date()
const days = nextDays(7, initialDate)
const activeDate = ref(days[0].date)
const slots = ref([])
const loading = ref(true)
const error = ref('')
const submitting = ref(false)

const groups = computed(() => {
  const bucket = { 上午: [], 下午: [], 晚上: [] }
  for (const slot of slots.value) {
    const hour = Number(slot.slice(0, 2))
    if (hour < 12) bucket['上午'].push(slot)
    else if (hour < 18) bucket['下午'].push(slot)
    else bucket['晚上'].push(slot)
  }
  return Object.entries(bucket).filter(([, list]) => list.length)
})

let requestSeq = 0

async function load() {
  const seq = ++requestSeq
  loading.value = true
  error.value = ''
  try {
    const data = await api.availability(staffId, serviceId, activeDate.value)
    if (seq !== requestSeq) return   // 快速切换日期时，丢弃过期响应
    slots.value = data.slots || []
  } catch (e) {
    if (seq !== requestSeq) return
    error.value = e.message
    slots.value = []
  } finally {
    if (seq === requestSeq) loading.value = false
  }
}

onMounted(load)
watch(activeDate, load)

async function choose(slot) {
  const startAt = `${activeDate.value}T${slot}:00`
  if (!bookingId) {
    router.push({ path: '/confirm', query: { staffId, serviceId, date: activeDate.value, time: slot } })
    return
  }
  submitting.value = true
  error.value = ''
  try {
    await api.reschedule(bookingId, startAt)
    router.replace('/mine')
  } catch (e) {
    error.value = e.message
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="view">
    <TopBar :title="bookingId ? '改期' : '选择时间'" />

    <div class="days">
      <button v-for="day in days" :key="day.date" class="day"
        :class="{ active: day.date === activeDate }" @click="activeDate = day.date">
        <span class="day__label">{{ day.label }}</span>
        <span class="day__num">{{ day.day }}</span>
      </button>
    </div>

    <div class="page">
      <div v-if="loading" class="loading">正在查询空闲时段</div>
      <div v-else-if="error" class="notice">{{ error }}</div>
      <div v-else-if="!slots.length" class="empty">
        这一天已经排满了<br />换一天看看吧
      </div>

      <template v-else>
        <div v-for="[label, list] in groups" :key="label" class="slot-group">
          <div class="slot-group__label">{{ label }}</div>
          <div class="slots">
            <button v-for="slot in list" :key="slot" class="slot" :disabled="submitting"
              @click="choose(slot)">
              {{ slot }}
            </button>
          </div>
        </div>
      </template>
    </div>
  </div>
</template>
