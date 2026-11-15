<script setup>
import { onMounted, ref } from 'vue'
import TopBar from '../components/TopBar.vue'
import { api } from '../api'

const SHOP_ID = 1

const list = ref([])
const loading = ref(true)
const error = ref('')

onMounted(async () => {
  try {
    list.value = await api.adminStaff(SHOP_ID)
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div class="view">
    <TopBar title="员工与排班" />

    <div class="page">
      <div v-if="error" class="notice">{{ error }}</div>
      <div v-if="loading" class="loading">加载中</div>
      <div v-else-if="!list.length" class="empty">还没有员工</div>

      <div v-else class="card" style="margin-top: 8px">
        <router-link v-for="(person, index) in list" :key="person.id" :to="`/admin/staff/${person.id}`"
          custom v-slot="{ navigate }">
          <button class="row" @click="navigate">
            <div class="avatar" :class="{ 'avatar--sage': index % 2 === 1 }">
              {{ person.name.slice(0, 1) }}
            </div>
            <div class="row__main">
              <div class="row__title">
                {{ person.name }}
                <span class="pill" :class="person.active ? 'pill--ok' : 'pill--muted'">
                  {{ person.active ? '在职' : '停用' }}
                </span>
              </div>
              <div class="row__sub">{{ person.title || '未填写岗位' }}</div>
            </div>
            <svg class="row__chevron" width="16" height="16" viewBox="0 0 24 24" fill="none"
              stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round">
              <path d="M9 6l6 6-6 6" />
            </svg>
          </button>
        </router-link>
      </div>

      <div class="actions">
        <router-link to="/admin/staff/new" custom v-slot="{ navigate }">
          <button class="btn btn--primary" @click="navigate">新增员工</button>
        </router-link>
      </div>

      <p class="section__hint" style="margin-top: 14px">
        停用的员工不会出现在顾客端，但历史预约仍然保留。
      </p>
    </div>
  </div>
</template>
