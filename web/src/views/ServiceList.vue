<script setup>
import { onMounted, ref } from 'vue'
import TopBar from '../components/TopBar.vue'
import { api } from '../api'
import { yuan } from '../utils'

const SHOP_ID = 1

const list = ref([])
const loading = ref(true)
const error = ref('')

async function load() {
  loading.value = true
  error.value = ''
  try {
    list.value = await api.adminServices(SHOP_ID)
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<template>
  <div class="view">
    <TopBar title="服务项目" />

    <div class="page">
      <div v-if="error" class="notice">{{ error }}</div>
      <div v-if="loading" class="loading">加载中</div>
      <div v-else-if="!list.length" class="empty">还没有服务项目</div>

      <div v-else class="card" style="margin-top: 8px">
        <router-link v-for="item in list" :key="item.id" :to="`/admin/services/${item.id}`" custom
          v-slot="{ navigate }">
          <button class="row" @click="navigate">
            <div class="row__main">
              <div class="row__title">{{ item.name }}</div>
              <div class="row__sub">{{ item.durationMinutes }} 分钟</div>
            </div>
            <span class="row__price">{{ yuan(item.priceCent) }}</span>
            <svg class="row__chevron" width="16" height="16" viewBox="0 0 24 24" fill="none"
              stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round">
              <path d="M9 6l6 6-6 6" />
            </svg>
          </button>
        </router-link>
      </div>

      <div class="actions">
        <router-link to="/admin/services/new" custom v-slot="{ navigate }">
          <button class="btn btn--primary" @click="navigate">新增服务</button>
        </router-link>
      </div>
    </div>
  </div>
</template>
