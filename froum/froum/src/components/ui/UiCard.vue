<template>
  <component
    :is="to ? 'router-link' : as"
    :to="to"
    class="ui-card kumo-surface"
    :class="{ 'magnetic-card': interactive }"
  >
    <slot />
  </component>
</template>

<script setup>
defineProps({
  as: {
    type: String,
    default: 'div'
  },
  to: {
    type: [String, Object],
    default: null
  },
  interactive: {
    type: Boolean,
    default: false
  }
})
</script>

<style scoped>
.ui-card {
  display: block;
  color: inherit;
  text-decoration: none;
}
</style>
