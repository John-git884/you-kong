<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import TopBar from '../components/TopBar.vue'
import { api } from '../api'

const SHOP_ID = 1
const WEEK_LABELS = ['周一', '周二', '周三', '周四', '周五', '周六', '周日']

const route = useRoute()
const router = useRouter()

const isNew = computed(() => !route.params.id)
const staffId = Number(route.params.id) || 0

const name = ref('')
const title = ref('')
const active = ref(true)
const rows = ref(
  WEEK_LABELS.map((label, index) => ({
    day: index + 1,
    label,
    rest: false,
    start: '10:00',
    end: '20:00'
  }))
)

const loading = ref(!isNew.value)
const saving = ref(false)
const error = ref('')

onMounted(async () => {
  if (isNew.value) return
  try {
    const [staffList, schedules] = await Promise.all([
      api.adminStaff(SHOP_ID),
      api.schedules(staffId)
    ])
    const person = staffList.find((s) => s.id === staffId)
    if (person) {
      name.value = person.name
      title.value = person.title || ''
      active.value = person.active
    }
    rows.value = rows.value.map((row) => {
      const matched = schedules.find((s) => s.dayOfWeek === row.day)
      return matched
        ? { ...row, rest: false, start: matched.startTime.slice(0, 5), end: matched.endTime.slice(0, 5) }
        : { ...row, rest: true }
    })
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
})

async function save() {
  error.value = ''
  if (!name.value.trim()) {
    error.value = '请填写姓名'
    return
  }
  const working = rows.value.filter((row) => !row.rest)
  for (const row of working) {
    if (row.start >= row.end) {
      error.value = `${row.label}的开始时间要早于结束时间`
      return
    }
  }

  saving.value = true
  try {
    let id = staffId
    const payload = { name: name.value.trim(), title: title.value.trim(), active: active.value }
    if (isNew.value) {
      const created = await api.createStaff(SHOP_ID, payload)
      id = created.id
    } else {
      await api.updateStaff(staffId, payload)
    }
    await api.saveSchedules(
      id,
      working.map((row) => ({ dayOfWeek: row.day, startTime: row.start, endTime: row.end }))
    )
    router.replace('/admin/staff')
  } catch (e) {
    error.value = e.message
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <div class="view">
    <TopBar :title="isNew ? '新增员工' : '员工资料'" />

    <div class="page">
      <div v-if="loading" class="loading">加载中</div>

      <template v-else>
        <label class="field">
          <span class="field__label">姓名</span>
          <input v-model="name" class="input" placeholder="例如：小鹿" maxlength="16" />
        </label>

        <label class="field">
          <span class="field__label">岗位</span>
          <input v-model="title" class="input" placeholder="例如：美甲师" maxlength="16" />
        </label>

        <div class="field">
          <span class="field__label">状态</span>
          <label class="check" style="height: 26px">
            <input v-model="active" type="checkbox" /> 在职，可在顾客端被预约
          </label>
        </div>

        <div class="section">
          <div class="section__head">
            <span class="section__title">每周排班</span>
            <span class="section__hint">不勾选的日期视为休息</span>
          </div>

          <div class="card" style="padding: 4px 16px">
            <div v-for="row in rows" :key="row.day" class="sched">
              <span class="sched__label">{{ row.label }}</span>
              <input v-model="row.start" class="sched__time" type="time" :disabled="row.rest" />
              <input v-model="row.end" class="sched__time" type="time" :disabled="row.rest" />
              <label class="check">
                <input v-model="row.rest" type="checkbox" /> 休息
              </label>
            </div>
          </div>
        </div>

        <div v-if="error" class="notice">{{ error }}</div>

        <div class="actions">
          <button class="btn btn--primary" :disabled="saving" @click="save">
            {{ saving ? '保存中' : '保存' }}
          </button>
        </div>
      </template>
    </div>
  </div>
</template>
