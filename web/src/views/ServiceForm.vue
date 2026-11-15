<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import TopBar from '../components/TopBar.vue'
import { api } from '../api'

const SHOP_ID = 1

const route = useRoute()
const router = useRouter()

const isNew = computed(() => !route.params.id)
const serviceId = Number(route.params.id) || 0

const name = ref('')
const duration = ref(60)
const priceYuan = ref(0)
const loading = ref(!isNew.value)
const saving = ref(false)
const error = ref('')

onMounted(async () => {
  if (isNew.value) return
  try {
    const list = await api.adminServices(SHOP_ID)
    const item = list.find((s) => s.id === serviceId)
    if (item) {
      name.value = item.name
      duration.value = item.durationMinutes
      priceYuan.value = item.priceCent / 100
    }
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
})

async function save() {
  error.value = ''
  if (!name.value.trim()) {
    error.value = '请填写服务名称'
    return
  }
  if (!duration.value || duration.value < 15) {
    error.value = '服务时长不能少于 15 分钟'
    return
  }
  saving.value = true
  const payload = {
    name: name.value.trim(),
    durationMinutes: Number(duration.value),
    priceCent: Math.round(Number(priceYuan.value) * 100)
  }
  try {
    if (isNew.value) {
      await api.createService(SHOP_ID, payload)
    } else {
      await api.updateService(serviceId, payload)
    }
    router.replace('/admin/services')
  } catch (e) {
    error.value = e.message
  } finally {
    saving.value = false
  }
}

async function remove() {
  if (!window.confirm('删除后无法恢复，确定删除这个服务吗？')) return
  error.value = ''
  try {
    await api.deleteService(serviceId)
    router.replace('/admin/services')
  } catch (e) {
    error.value = e.message
  }
}
</script>

<template>
  <div class="view">
    <TopBar :title="isNew ? '新增服务' : '编辑服务'" />

    <div class="page">
      <div v-if="loading" class="loading">加载中</div>

      <template v-else>
        <label class="field">
          <span class="field__label">服务名称</span>
          <input v-model="name" class="input" placeholder="例如：法式延长" maxlength="24" />
        </label>

        <div class="field-row">
          <label class="field">
            <span class="field__label">时长（分钟）</span>
            <input v-model="duration" class="input" type="number" step="15" min="15" max="480" />
          </label>
          <label class="field">
            <span class="field__label">价格（元）</span>
            <input v-model="priceYuan" class="input" type="number" step="1" min="0" />
          </label>
        </div>

        <p class="section__hint" style="margin-top: 10px">
          时长会换算成时间段：15 分钟为一格，60 分钟的服务占 4 格。
        </p>

        <div v-if="error" class="notice">{{ error }}</div>

        <div class="actions">
          <button class="btn btn--primary" :disabled="saving" @click="save">
            {{ saving ? '保存中' : '保存' }}
          </button>
        </div>

        <div v-if="!isNew" style="text-align: center; margin-top: 8px">
          <button class="btn btn--quiet" style="color: var(--clay)" @click="remove">删除这个服务</button>
        </div>
      </template>
    </div>
  </div>
</template>
