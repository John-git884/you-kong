<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import TopBar from '../components/TopBar.vue'
import { api } from '../api'
import { yuan } from '../utils'

const SHOP_ID = 1

const router = useRouter()
const shop = ref(null)
const services = ref([])
const loading = ref(true)
const error = ref('')

onMounted(async () => {
  try {
    const [shopInfo, list] = await Promise.all([api.shop(SHOP_ID), api.serviceList(SHOP_ID)])
    shop.value = shopInfo
    services.value = list
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
})

const hhmm = (time) => (time || '').slice(0, 5)

function choose(service) {
  router.push({ path: '/staff', query: { serviceId: service.id } })
}
</script>

<template>
  <div class="view">
    <TopBar :back="false" title="有空">
      <template #aside>
        <router-link class="topbar__aside" to="/mine">我的预约</router-link>
      </template>
    </TopBar>

    <div class="hero">
      <p class="hero__eyebrow">Booking</p>
      <h1 class="hero__name">{{ shop ? shop.name : '正在加载' }}</h1>
      <div v-if="shop" class="hero__meta">
        <span>营业 {{ hhmm(shop.openTime) }} – {{ hhmm(shop.closeTime) }}</span>
        <span>{{ services.length }} 项服务</span>
      </div>
    </div>

    <div class="page">
      <div class="section" style="margin-top: 0">
        <div class="section__head">
          <span class="section__title">选择服务</span>
          <span class="section__hint">下一步选技师</span>
        </div>

        <div v-if="loading" class="loading">加载中</div>
        <div v-else-if="error" class="notice">{{ error }}</div>
        <div v-else-if="!services.length" class="empty">还没有上架的服务</div>

        <div v-else class="card">
          <button v-for="item in services" :key="item.id" class="row" @click="choose(item)">
            <div class="row__main">
              <div class="row__title">{{ item.name }}</div>
              <div class="row__sub">约 {{ item.durationMinutes }} 分钟</div>
            </div>
            <span class="row__price">{{ yuan(item.priceCent) }}</span>
            <svg class="row__chevron" width="16" height="16" viewBox="0 0 24 24" fill="none"
              stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round">
              <path d="M9 6l6 6-6 6" />
            </svg>
          </button>
        </div>
      </div>

      <div class="section">
        <div class="section__head">
          <span class="section__title">店主入口</span>
        </div>
        <div class="card">
          <router-link class="row" to="/admin" custom v-slot="{ navigate }">
            <button class="row" style="border: 0" @click="navigate">
              <div class="row__main">
                <div class="row__title">今日预约台</div>
                <div class="row__sub">确认、核销、取消都在这里</div>
              </div>
              <svg class="row__chevron" width="16" height="16" viewBox="0 0 24 24" fill="none"
                stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round">
                <path d="M9 6l6 6-6 6" />
              </svg>
            </button>
          </router-link>
        </div>
      </div>
    </div>
  </div>
</template>
