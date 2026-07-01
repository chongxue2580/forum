<template>
  <component
    :is="to ? 'router-link' : 'button'"
    :to="to"
    :type="to ? undefined : type"
    class="kumo-button ui-button"
    :class="[
      variant === 'brand' && 'kumo-button--brand',
      variant === 'ghost' && 'kumo-button--ghost',
      iconOnly && 'kumo-button--icon',
      block && 'ui-button--block'
    ]"
  >
    <slot />
  </component>
</template>

<script setup>
defineProps({
  to: {
    type: [String, Object],
    default: null
  },
  type: {
    type: String,
    default: 'button'
  },
  variant: {
    type: String,
    default: 'default',
    validator: value => ['default', 'brand', 'ghost'].includes(value)
  },
  iconOnly: {
    type: Boolean,
    default: false
  },
  block: {
    type: Boolean,
    default: false
  }
})
</script>

<style scoped>
.ui-button {
  text-decoration: none;
}

.ui-button--block {
  width: 100%;
}
</style>
