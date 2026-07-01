<template>
  <div class="tags-view">
    <ui-page-hero
      title="标签云"
      description="用标签快速切入主题，发现高频技术关键词和相关内容集合。"
    >
      <template #eyebrow>
        <span class="kumo-eyebrow">
          <font-awesome-icon :icon="['fas', 'tags']" />
          Tags
        </span>
      </template>
      <template #aside>
        <div class="hero-count">
          <strong>{{ popularTags.length }}</strong>
          <span>个标签</span>
        </div>
      </template>
    </ui-page-hero>

    <section class="tag-panel kumo-surface">
      <div class="search-input">
        <font-awesome-icon :icon="['fas', 'search']" />
        <input
          v-model="searchQuery"
          type="search"
          placeholder="搜索标签"
          aria-label="搜索标签"
        >
        <button v-if="searchQuery" type="button" aria-label="清除搜索" @click="searchQuery = ''">
          <font-awesome-icon :icon="['fas', 'times']" />
        </button>
      </div>

      <div v-if="loading" class="state-panel">
        <font-awesome-icon :icon="['fas', 'spinner']" spin />
        <span>正在加载标签...</span>
      </div>
      <tag-list v-else-if="filteredTags.length" :tags="filteredTags" />
      <div v-else class="state-panel">
        <font-awesome-icon :icon="['fas', 'tags']" />
        <span>暂无匹配标签</span>
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useStore } from 'vuex'
import TagList from '../components/TagList.vue'
import UiPageHero from '../components/ui/UiPageHero.vue'

const store = useStore()
const loading = ref(true)
const searchQuery = ref('')

const popularTags = computed(() => store.state.popularTags || [])
const filteredTags = computed(() => {
  if (!searchQuery.value) return popularTags.value
  const query = searchQuery.value.toLowerCase()
  return popularTags.value.filter(tag => tag.name?.toLowerCase().includes(query))
})

onMounted(async () => {
  loading.value = true
  try {
    if (popularTags.value.length === 0) {
      await store.dispatch('fetchPopularTags')
    }
  } catch (error) {
    store.commit('SET_ERROR', error?.message || '标签加载失败')
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.tags-view {
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

.tag-panel {
  display: grid;
  gap: 1rem;
  padding: 1rem;
}

.search-input {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  align-items: center;
  gap: 0.65rem;
  width: min(100%, 30rem);
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
  min-height: 12rem;
  color: var(--kumo-text-muted);
  text-align: center;
}

.state-panel svg {
  color: var(--kumo-bg-brand);
  font-size: 2rem;
}
</style>
