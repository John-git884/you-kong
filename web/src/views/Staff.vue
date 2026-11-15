<script setup>
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import TopBar from '../components/TopBar.vue'
import { api } from '../api'
import { yuan } from '../utils'

const SHOP_ID = 1

const route = useRoute()
const router = useRouter()
const serviceId = Number(route.query.serviceId)

const staff = ref([])
const service = ref(null)
const loading = ref(true)
const error = ref('')

onMounted(async () => {
  try {
    const [list, services] = await Promise.all([api.staffList(SHOP_ID), api.serviceList(SHOP_ID)])
    staff.value = list
    service.value = services.find((s) => s.id === serviceId) || null
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
})

function pick(person) {
  router.push({ path: '/time', query: { staffId: person.id, serviceId } })
}
</script>

<template>
  <div class="view">
    <TopBar title="选择技师" />

    <div class="page">
      <div v-if="service" class="hero" style="padding: 10px 0 22px">
        <p class="hero__eyebrow">已选服务</p>
        <h1 class="hero__name" style="font-size: 22px">{{ service.name }}</h1>
        <div class="hero__meta">
          <span>{{ service.durationMinutes }} 分钟</span>
          <span>{{ yuan(service.priceCent) }}</span>
        </div>
      </div>

      <div class="section" style="margin-top: 0">
        <div class="section__head">
          <span class="section__title">可预约的技师</span>
        </div>

        <div v-if="loading" class="loading">加载中</div>
        <div v-else-if="error" class="notice">{{ error }}</div>
        <div v-else-if="!staff.length" class="empty">暂时没有可选的技师</div>

        <div v-else class="card">
          <button v-for="(person, index) in staff" :key="person.id" class="row" @click="pick(person)">
            <div class="avatar" :class="{ 'avatar--sage': index % 2 === 1 }">
              {{ person.name.slice(0, 1) }}
            </div>
            <div class="row__main">
              <div class="row__title">{{ person.name }}</div>
              <div class="row__sub">{{ person.title }}</div>
            </div>
            <svg class="row__chevron" width="16" height="16" viewBox="0 0 24 24" fill="none"
              stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round">
              <path d="M9 6l6 6-6 6" />
            </svg>
          </button>
        </div>
      </div>

      <p class="section__hint" style="margin-top: 14px">
        排班以店内实际安排为准，临时调整请电话联系门店。
      </p>
    </div>
  </div>
</template>
