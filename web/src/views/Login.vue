<script setup>
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { api, setToken } from '../api'

const route = useRoute()
const router = useRouter()

const username = ref('')
const password = ref('')
const submitting = ref(false)
const error = ref('')

async function submit() {
  error.value = ''
  if (!username.value.trim() || !password.value) {
    error.value = '请输入账号和密码'
    return
  }
  submitting.value = true
  try {
    const data = await api.login(username.value.trim(), password.value)
    setToken(data.token)
    router.replace(route.query.redirect || '/admin')
  } catch (e) {
    error.value = e.message
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="view">
    <div class="auth">
      <p class="hero__eyebrow">店主后台</p>
      <h1 class="auth__brand">有空</h1>
      <p class="auth__sub">登录后管理预约、服务与员工排班</p>

      <label class="field" style="margin-top: 0">
        <span class="field__label">账号</span>
        <input v-model="username" class="input" placeholder="请输入账号" autocomplete="username" />
      </label>

      <label class="field">
        <span class="field__label">密码</span>
        <input v-model="password" class="input" type="password" placeholder="请输入密码"
          autocomplete="current-password" @keyup.enter="submit" />
      </label>

      <div v-if="error" class="notice">{{ error }}</div>

      <div class="actions">
        <button class="btn btn--primary" :disabled="submitting" @click="submit">
          {{ submitting ? '登录中' : '登录' }}
        </button>
      </div>

      <div style="text-align: center; margin-top: 18px">
        <router-link class="section__hint" to="/">返回顾客端</router-link>
      </div>
    </div>
  </div>
</template>
