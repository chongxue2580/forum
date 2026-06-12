<template>
  <div class="error-page">
    <div class="error-container">
      <div class="error-code">404</div>
      <h1 class="error-title">页面不存在</h1>
      <p class="error-message">
        您访问的页面不存在或已被移除。
      </p>
      <div class="error-actions">
        <router-link to="/" class="btn btn-primary">
          <font-awesome-icon :icon="['fas', 'home']" />
          返回首页
        </router-link>
        <button @click="goBack" class="btn btn-secondary">
          <font-awesome-icon :icon="['fas', 'arrow-right']" class="icon-flip" />
          返回上一页
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router';

const router = useRouter();

const goBack = () => {
  router.back();
};
</script>

<style scoped>
.error-page {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 70vh;
  padding: 2rem;
}

.error-container {
  max-width: 600px;
  text-align: center;
}

.error-code {
  font-size: 8rem;
  font-weight: 700;
  color: var(--primary-color);
  line-height: 1;
  margin-bottom: 1rem;
  text-shadow: 0 4px 8px rgba(0, 0, 0, 0.1);
}

.error-title {
  font-size: 2rem;
  margin-bottom: 1rem;
  color: var(--text-color);
}

.error-message {
  font-size: 1.1rem;
  color: var(--text-light);
  margin-bottom: 2rem;
}

.error-actions {
  display: flex;
  justify-content: center;
  gap: 1rem;
}

.btn {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.75rem 1.5rem;
  border-radius: var(--radius);
  font-weight: 500;
  transition: var(--transition);
  cursor: pointer;
}

.btn-primary {
  background-color: var(--primary-color);
  color: white;
  border: none;
}

.btn-primary:hover {
  background-color: var(--primary-dark);
}

.btn-secondary {
  background-color: var(--bg-gray);
  color: var(--text-color);
  border: 1px solid var(--border-color);
}

.btn-secondary:hover {
  background-color: var(--border-color);
}

.icon-flip {
  transform: scaleX(-1);
}
</style> 