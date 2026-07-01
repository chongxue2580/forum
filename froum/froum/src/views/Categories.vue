<template>
  <div class="categories-view">
    <ui-page-hero
      title="分类浏览"
      description="按技术领域浏览内容，把文章从信息流重新整理成可探索的知识地图。"
    >
      <template #eyebrow>
        <span class="kumo-eyebrow">
          <font-awesome-icon :icon="['fas', 'th-large']" />
          Categories
        </span>
      </template>
      <template #actions>
        <router-link to="/" class="kumo-button">
          <font-awesome-icon :icon="['fas', 'home']" />
          返回首页
        </router-link>
      </template>
      <template #aside>
        <div class="hero-count">
          <strong>{{ categories.length }}</strong>
          <span>个分类</span>
        </div>
      </template>
    </ui-page-hero>

    <section class="category-panel kumo-surface">
      <div v-if="categories.length > 0" class="search-row">
        <div class="search-input">
          <font-awesome-icon :icon="['fas', 'search']" />
          <input
            v-model="searchQuery"
            type="search"
            placeholder="搜索分类名称或描述"
            aria-label="搜索分类"
          >
          <button v-if="searchQuery" type="button" aria-label="清除搜索" @click="searchQuery = ''">
            <font-awesome-icon :icon="['fas', 'times']" />
          </button>
        </div>
      </div>

      <div v-if="loading" class="state-panel">
        <font-awesome-icon :icon="['fas', 'spinner']" spin />
        <span>正在加载分类...</span>
      </div>

      <template v-else>
        <category-list v-if="filteredCategories.length" :categories="filteredCategories" />
        <div v-else class="state-panel">
          <font-awesome-icon :icon="['fas', 'search']" />
          <h2>未找到匹配分类</h2>
          <p>{{ searchQuery }}</p>
          <button class="kumo-button" type="button" @click="searchQuery = ''">清除搜索</button>
        </div>
      </template>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useStore } from 'vuex'
import CategoryList from '../components/CategoryList.vue'
import UiPageHero from '../components/ui/UiPageHero.vue'

const store = useStore()
const loading = ref(true)
const searchQuery = ref('')

const categories = computed(() => store.state.categories || [])

const filteredCategories = computed(() => {
  if (!searchQuery.value) return categories.value

  const query = searchQuery.value.toLowerCase()
  return categories.value.filter(category =>
    category.name?.toLowerCase().includes(query) ||
    category.description?.toLowerCase().includes(query) ||
    category.subcategories?.some(sub => sub.name?.toLowerCase().includes(query))
  )
})

const loadCategories = async () => {
  loading.value = true
  try {
    if (categories.value.length === 0) {
      await store.dispatch('fetchCategories')
    }
  } catch (error) {
    store.commit('SET_ERROR', error?.message || '分类加载失败')
  } finally {
    loading.value = false
  }
}

onMounted(loadCategories)
</script>

<style scoped>
.categories-view {
  display: grid;
  gap: 1.25rem;
}

.hero-count {
  display: grid;
  gap: 0.2rem;
  padding: 1.2rem;
  border: 1px solid var(--kumo-hairline);
  border-radius: var(--kumo-radius-lg);
  background: var(--kumo-bg-base);
}

.hero-count strong {
  color: var(--kumo-bg-brand-strong);
  font-size: 3.4rem;
  font-weight: 900;
  line-height: 1;
}

.hero-count span {
  color: var(--kumo-text-muted);
  font-weight: 740;
}

.category-panel {
  display: grid;
  gap: 1rem;
  padding: 1rem;
}

.search-row {
  display: flex;
  justify-content: flex-start;
}

.search-input {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  align-items: center;
  gap: 0.65rem;
  width: min(100%, 34rem);
  min-height: 2.95rem;
  padding: 0 0.45rem 0 0.9rem;
  border: 1px solid var(--kumo-hairline);
  border-radius: 999px;
  background: var(--kumo-bg-base);
  color: var(--kumo-text-subtle);
}

.search-input input {
  min-width: 0;
  border: 0;
  background: transparent;
  color: var(--kumo-text-default);
  outline: none;
}

.search-input button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 2.1rem;
  height: 2.1rem;
  border: 0;
  border-radius: 999px;
  background: var(--kumo-bg-subtle);
  color: var(--kumo-text-muted);
  cursor: pointer;
}

.state-panel {
  display: grid;
  place-items: center;
  gap: 0.8rem;
  min-height: 14rem;
  padding: 2rem;
  color: var(--kumo-text-muted);
  text-align: center;
}

.state-panel svg {
  color: var(--kumo-bg-brand);
  font-size: 2rem;
}

.state-panel h2,
.state-panel p {
  margin: 0;
}
</style>
