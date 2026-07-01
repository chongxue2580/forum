<template>
  <div class="error-page kumo-page">
    <section class="error-container kumo-surface-strong reveal-rise">
      <span class="kumo-eyebrow">403</span>
      <div class="error-code">访问被拒绝</div>
      <h1 class="kumo-heading">当前账号没有权限</h1>
      <p class="kumo-muted">如需访问该页面，请切换到具备权限的账号或返回继续浏览。</p>
      <div class="error-actions">
        <router-link to="/" class="kumo-button kumo-button--brand">
          <font-awesome-icon :icon="['fas', 'home']" />
          返回首页
        </router-link>
        <button class="kumo-button" @click="goBack">
          <font-awesome-icon :icon="['fas', 'arrow-right']" class="icon-flip" />
          返回上一页
        </button>
      </div>
    </section>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'

const router = useRouter()

const goBack = () => {
  router.back()
}
</script>

<style scoped>
.error-page {
  display: grid;
  min-height: 70vh;
  place-items: center;
  padding: clamp(1rem, 5vw, 3rem);
}

.error-container {
  width: min(100%, 42rem);
  padding: clamp(1.5rem, 6vw, 3rem);
  text-align: center;
}

.error-code {
  margin-top: 1rem;
  color: var(--kumo-status-warning);
  font-size: clamp(1.15rem, 4vw, 1.5rem);
  font-weight: 820;
}

.error-container h1 {
  margin: 0.65rem 0 0;
  font-size: clamp(2.25rem, 8vw, 4.5rem);
}

.error-container p {
  max-width: 30rem;
  margin: 1rem auto 0;
}

.error-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 0.75rem;
  margin-top: 2rem;
}

.icon-flip {
  transform: scaleX(-1);
}
</style>
