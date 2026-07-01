<template>
  <div class="category-list">
    <div v-if="categories.length === 0" class="empty-state kumo-surface">
      <font-awesome-icon :icon="['fas', 'folder-open']" />
      <p>暂无分类内容</p>
    </div>

    <div v-else class="category-grid">
      <router-link
        v-for="(category, index) in categories"
        :key="category.id"
        :to="`/category/${category.id}`"
        class="category-card kumo-surface magnetic-card stagger-item"
        :style="{ animationDelay: `${Math.min(index, 10) * 45}ms` }"
      >
        <span class="category-icon">
          <font-awesome-icon :icon="getCategoryIcon(category)" />
        </span>
        <span class="category-content">
          <strong>{{ category.name }}</strong>
          <small>{{ category.description || `${category.articleCount || 0} 篇文章` }}</small>
        </span>
        <span class="category-count">{{ category.articleCount || 0 }}</span>
        <font-awesome-icon :icon="['fas', 'chevron-right']" class="category-arrow" />
      </router-link>
    </div>
  </div>
</template>

<script setup>
defineProps({
  categories: {
    type: Array,
    default: () => []
  }
})

const getCategoryIcon = (category) => {
  const categoryIcons = {
    前端开发: ['fas', 'code'],
    后端开发: ['fas', 'server'],
    移动开发: ['fas', 'mobile-alt'],
    人工智能: ['fas', 'robot'],
    开发工具: ['fas', 'tools']
  }

  if (categoryIcons[category.name]) return categoryIcons[category.name]

  const icons = [
    'folder',
    'code',
    'server',
    'database',
    'mobile-alt',
    'cloud',
    'network-wired',
    'robot'
  ]
  return ['fas', icons[(category.id || 0) % icons.length]]
}
</script>

<style scoped>
.category-list {
  width: 100%;
}

.category-grid {
  display: grid;
  gap: 0.8rem;
}

.category-card {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto auto;
  align-items: center;
  gap: 0.85rem;
  padding: 0.9rem;
  color: var(--kumo-text-default);
  text-decoration: none;
}

.category-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 2.65rem;
  height: 2.65rem;
  border-radius: 1rem;
  background: var(--kumo-bg-brand-soft);
  color: var(--kumo-bg-brand-strong);
  transition: var(--transition);
}

.category-card:hover .category-icon {
  background: var(--kumo-bg-brand);
  color: var(--kumo-text-inverse);
}

.category-content {
  display: grid;
  min-width: 0;
  gap: 0.15rem;
}

.category-content strong {
  overflow: hidden;
  color: var(--kumo-text-default);
  font-weight: 820;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.category-content small {
  overflow: hidden;
  color: var(--kumo-text-muted);
  font-size: 0.84rem;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.category-count {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 2.15rem;
  height: 2.15rem;
  border-radius: 999px;
  background: var(--kumo-bg-subtle);
  color: var(--kumo-text-muted);
  font-weight: 820;
}

.category-arrow {
  color: var(--kumo-text-subtle);
  transition: var(--transition);
}

.category-card:hover .category-arrow {
  color: var(--kumo-bg-brand-strong);
  transform: translateX(4px);
}

.empty-state {
  display: grid;
  place-items: center;
  min-height: 10rem;
  padding: 2rem;
  color: var(--kumo-text-muted);
  text-align: center;
}

.empty-state svg {
  color: var(--kumo-bg-brand);
  font-size: 2rem;
}
</style>
