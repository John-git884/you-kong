<script setup>
import { STATUS_TEXT, STATUS_TONE } from '../utils'

defineProps({
  status: { type: String, required: true }
})
</script>

<template>
  <span class="pill" :class="`pill--${STATUS_TONE[status] || 'muted'}`">
    {{ STATUS_TEXT[status] || status }}
  </span>
</template>
