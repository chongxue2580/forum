<template>
  <section class="ui-page-hero kumo-surface-strong reveal-rise">
    <div class="hero-copy">
      <slot name="eyebrow" />
      <h1 class="kumo-heading">{{ title }}</h1>
      <p v-if="description" class="kumo-muted">{{ description }}</p>
      <div v-if="$slots.actions" class="hero-actions">
        <slot name="actions" />
      </div>
    </div>
    <div v-if="$slots.aside" class="hero-aside">
      <slot name="aside" />
    </div>
  </section>
</template>

<script setup>
defineProps({
  title: {
    type: String,
    required: true
  },
  description: {
    type: String,
    default: ''
  }
})
</script>

<style scoped>
.ui-page-hero {
  position: relative;
  display: grid;
  grid-template-columns: minmax(0, 1.4fr) minmax(220px, 0.6fr);
  gap: 1.5rem;
  min-height: 16rem;
  padding: clamp(1.5rem, 4vw, 3rem);
  overflow: hidden;
}

.ui-page-hero::after {
  content: '';
  position: absolute;
  right: -6rem;
  top: -8rem;
  width: 22rem;
  height: 22rem;
  border-radius: 999px;
  background: radial-gradient(circle, var(--kumo-bg-brand-soft), transparent 62%);
  animation: soft-pulse 7s ease-in-out infinite;
  pointer-events: none;
}

.hero-copy,
.hero-aside {
  position: relative;
  z-index: 1;
}

.hero-copy {
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 1rem;
}

.hero-copy h1 {
  max-width: 12ch;
  margin: 0;
  font-size: clamp(2.35rem, 7vw, 5.8rem);
}

.hero-copy p {
  max-width: 44rem;
  margin: 0;
  font-size: clamp(1rem, 2vw, 1.18rem);
}

.hero-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 0.8rem;
  margin-top: 0.4rem;
}

@media (max-width: 760px) {
  .ui-page-hero {
    grid-template-columns: 1fr;
    min-height: auto;
  }

  .hero-copy h1 {
    font-size: clamp(2.15rem, 14vw, 3.4rem);
  }
}
</style>
