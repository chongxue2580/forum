<script setup>
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import ArticleCard from './ArticleCard.vue'

const props = defineProps({
  articles: {
    type: Array,
    default: () => []
  },
  showTitle: {
    type: Boolean,
    default: true
  },
  title: {
    type: String,
    default: '最新文章'
  },
  loading: {
    type: Boolean,
    default: false
  },
  currentPage: {
    type: Number,
    default: 1
  },
  totalPages: {
    type: Number,
    default: 1
  },
  totalItems: {
    type: Number,
    default: 0
  },
  pageSize: {
    type: Number,
    default: 10
  },
  showPagination: {
    type: Boolean,
    default: true
  }
})

const emit = defineEmits(['page-change'])
const router = useRouter()
const activeFilter = ref('all')

const visibleArticles = computed(() => {
  if (!props.articles?.length) return []
  return props.articles
})

const pageNumbers = computed(() => {
  const total = props.totalPages
  const current = props.currentPage
  const pages = []

  if (total <= 1) return pages
  pages.push(1)

  let startPage = Math.max(2, current - 1)
  let endPage = Math.min(total - 1, current + 1)

  if (current <= 3) endPage = Math.min(total - 1, 4)
  if (current >= total - 2) startPage = Math.max(2, total - 3)
  if (startPage > 2) pages.push('...')
  for (let i = startPage; i <= endPage; i += 1) pages.push(i)
  if (endPage < total - 1) pages.push('...')
  pages.push(total)

  return pages
})

const setFilter = (filter) => {
  activeFilter.value = filter

  if (filter === 'all') {
    router.push({ path: '/articles' })
  } else if (filter === 'popular') {
    router.push({ path: '/articles', query: { tab: 'hot' } })
  } else if (filter === 'recommended') {
    router.push({ path: '/articles', query: { tab: 'recommended' } })
  }
}

const changePage = (page) => {
  if (page < 1 || page > props.totalPages || page === props.currentPage) return
  emit('page-change', page)
}
</script>

<template>
  <section class="article-list">
    <div v-if="showTitle" class="article-list-header">
      <div>
        <span class="kumo-eyebrow">
          <font-awesome-icon :icon="['fas', 'file-alt']" />
          内容流
        </span>
        <h2 class="kumo-heading">{{ title }}</h2>
      </div>
      <div class="kumo-tabs" aria-label="文章筛选">
        <button
          class="kumo-tab"
          :class="{ active: activeFilter === 'all' }"
          type="button"
          @click="setFilter('all')"
        >
          所有
        </button>
        <button
          class="kumo-tab"
          :class="{ active: activeFilter === 'popular' }"
          type="button"
          @click="setFilter('popular')"
        >
          热门
        </button>
        <button
          class="kumo-tab"
          :class="{ active: activeFilter === 'recommended' }"
          type="button"
          @click="setFilter('recommended')"
        >
          推荐
        </button>
      </div>
    </div>

    <div v-if="loading" class="list-state kumo-surface">
      <font-awesome-icon :icon="['fas', 'spinner']" spin />
      <span>正在加载文章...</span>
    </div>

    <div v-else-if="visibleArticles.length === 0" class="empty-state kumo-surface">
      <font-awesome-icon :icon="['fas', 'file-alt']" />
      <h3>暂无文章内容</h3>
      <p>发布第一篇文章，让社区从这里开始讨论。</p>
      <router-link to="/article/new" class="kumo-button kumo-button--brand">
        <font-awesome-icon :icon="['fas', 'edit']" />
        发布文章
      </router-link>
    </div>

    <template v-else>
      <div class="article-list-container">
        <article-card
          v-for="(article, index) in visibleArticles"
          :key="article.id"
          :article="article"
          class="stagger-item"
          :style="{ animationDelay: `${Math.min(index, 8) * 55}ms` }"
        />
      </div>

      <div v-if="showPagination && totalPages > 1" class="pagination kumo-surface">
        <div class="pagination-info">
          共 {{ totalItems }} 篇，每页 {{ pageSize }} 篇
        </div>
        <div class="pagination-controls">
          <button
            class="kumo-button"
            type="button"
            :disabled="currentPage <= 1"
            @click="changePage(currentPage - 1)"
          >
            <font-awesome-icon :icon="['fas', 'chevron-left']" />
            上一页
          </button>

          <button
            v-for="(page, index) in pageNumbers"
            :key="`${page}-${index}`"
            class="page-number"
            :class="{ active: page === currentPage, ellipsis: page === '...' }"
            type="button"
            :disabled="page === '...'"
            @click="page !== '...' && changePage(page)"
          >
            {{ page }}
          </button>

          <button
            class="kumo-button"
            type="button"
            :disabled="currentPage >= totalPages"
            @click="changePage(currentPage + 1)"
          >
            下一页
            <font-awesome-icon :icon="['fas', 'chevron-right']" />
          </button>
        </div>
      </div>
    </template>
  </section>
</template>

<style scoped>
.article-list {
  display: grid;
  gap: 1.2rem;
}

.article-list-header {
  display: flex;
  align-items: end;
  justify-content: space-between;
  gap: 1rem;
}

.article-list-header > div:first-child {
  display: grid;
  gap: 0.65rem;
}

h2 {
  margin: 0;
  font-size: clamp(1.6rem, 3vw, 2.2rem);
}

.article-list-container {
  display: grid;
  gap: 1rem;
}

.list-state,
.empty-state {
  display: grid;
  place-items: center;
  gap: 0.85rem;
  min-height: 16rem;
  padding: 2rem;
  text-align: center;
}

.list-state svg,
.empty-state > svg {
  color: var(--kumo-bg-brand);
  font-size: 2rem;
}

.empty-state h3 {
  margin: 0;
  color: var(--kumo-text-default);
  font-size: 1.35rem;
  font-weight: 820;
}

.empty-state p {
  margin: 0;
  color: var(--kumo-text-muted);
}

.pagination {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  padding: 0.8rem;
}

.pagination-info {
  color: var(--kumo-text-muted);
  font-size: 0.9rem;
  font-weight: 720;
}

.pagination-controls {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 0.45rem;
}

.page-number {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 2.45rem;
  height: 2.45rem;
  border: 1px solid var(--kumo-hairline);
  border-radius: 999px;
  background: transparent;
  color: var(--kumo-text-muted);
  font-weight: 780;
  cursor: pointer;
  transition: var(--transition);
}

.page-number:hover:not(:disabled),
.page-number.active {
  background: var(--kumo-bg-brand);
  color: var(--kumo-text-inverse);
}

.page-number.ellipsis {
  border-color: transparent;
  cursor: default;
}

button:disabled {
  opacity: 0.48;
  cursor: not-allowed;
}

@media (max-width: 760px) {
  .article-list-header,
  .pagination {
    align-items: stretch;
    flex-direction: column;
  }

  .pagination-controls {
    justify-content: center;
  }
}
</style>
