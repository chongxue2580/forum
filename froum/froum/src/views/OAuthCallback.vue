<template>
  <div class="oauth-callback">
    <div class="oauth-panel">
      <font-awesome-icon :icon="['fas', 'spinner']" spin class="oauth-spinner" />
      <h1>正在登录</h1>
      <p>{{ message }}</p>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useStore } from 'vuex'

const route = useRoute()
const router = useRouter()
const store = useStore()
const message = ref('请稍候')

const fail = async (errorMessage) => {
  sessionStorage.removeItem('oauthRedirect')
  sessionStorage.removeItem('oauthProvider')
  await router.replace({
    path: '/login',
    query: { oauthError: errorMessage || '第三方登录失败，请重新尝试' }
  })
}

onMounted(async () => {
  const provider = String(route.query.provider || sessionStorage.getItem('oauthProvider') || '').trim().toLowerCase()
  const code = route.query.code ? String(route.query.code) : ''
  const state = route.query.state ? String(route.query.state) : ''
  const providerError = route.query.error ? String(route.query.error) : ''

  if (providerError) {
    await fail('第三方授权已取消或失败')
    return
  }

  if (!provider || !code || !state) {
    await fail('第三方登录参数不完整')
    return
  }

  try {
    message.value = '正在完成账号验证'
    await store.dispatch('oauthLogin', { provider, code, state })
    const redirectPath = sessionStorage.getItem('oauthRedirect') || '/'
    sessionStorage.removeItem('oauthRedirect')
    sessionStorage.removeItem('oauthProvider')
    await router.replace(redirectPath)
  } catch (err) {
    await fail(err.message || '第三方登录失败，请重新尝试')
  }
})
</script>

<style scoped>
.oauth-callback {
  min-height: calc(100vh - 200px);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: clamp(2rem, 8vw, 5rem) 1rem;
}

.oauth-panel {
  width: 100%;
  max-width: 28rem;
  padding: clamp(1.5rem, 5vw, 2.5rem);
  text-align: center;
  border: 1px solid var(--kumo-hairline);
  border-radius: var(--kumo-radius-lg);
  background: var(--kumo-bg-elevated);
  box-shadow: var(--kumo-shadow-md);
  backdrop-filter: var(--kumo-blur);
  animation: reveal-rise 520ms cubic-bezier(0.2, 0.8, 0.2, 1) both;
}

.oauth-spinner {
  color: var(--kumo-bg-brand);
  font-size: 2rem;
  margin-bottom: 1rem;
}

.oauth-panel h1 {
  margin: 0 0 0.5rem;
  color: var(--kumo-text-default);
  font-size: 1.4rem;
  font-weight: 820;
}

.oauth-panel p {
  margin: 0;
  color: var(--kumo-text-muted);
}
</style>
