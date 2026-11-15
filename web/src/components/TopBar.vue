<script setup>
import { onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'

defineProps({
  title: { type: String, default: '' },
  back: { type: Boolean, default: true }
})

const router = useRouter()
const scrolled = ref(false)

function onScroll() {
  scrolled.value = window.scrollY > 4
}

function goBack() {
  if (window.history.length > 1) {
    router.back()
  } else {
    router.push('/')
  }
}

onMounted(() => window.addEventListener('scroll', onScroll, { passive: true }))
onUnmounted(() => window.removeEventListener('scroll', onScroll))
</script>

<template>
  <header class="topbar" :class="{ scrolled }">
    <button v-if="back" class="topbar__back" aria-label="返回" @click="goBack">
      <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor"
        stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round">
        <path d="M15 18l-6-6 6-6" />
      </svg>
    </button>
    <span class="topbar__title">{{ title }}</span>
    <slot name="aside" />
  </header>
</template>
