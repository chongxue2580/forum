<template>
  <div class="tag-list">
    <div v-if="tags.length === 0" class="empty-state kumo-surface">
      <font-awesome-icon :icon="['fas', 'tags']" />
      <p>暂无标签内容</p>
    </div>

    <div v-else class="tag-cloud">
      <router-link
        v-for="(tag, index) in tags"
        :key="tag.id || tag.name"
        :to="`/tag/${tag.id}`"
        class="tag-item magnetic-card stagger-item"
        :class="getTagClass(tag.count || tag.articleCount || 0)"
        :style="{
          '--tag-size': getTagSize(tag.count || tag.articleCount || 0),
          animationDelay: `${Math.min(index, 18) * 32}ms`
        }"
      >
        <font-awesome-icon :icon="['fas', 'hashtag']" />
        <span>{{ tag.name }}</span>
        <strong>{{ tag.count || tag.articleCount || 0 }}</strong>
      </router-link>
    </div>
  </div>
</template>

<script setup>
const props = defineProps({
  tags: {
    type: Array,
    default: () => []
  }
})

const getRange = () => {
  const counts = props.tags.map(tag => tag.count || tag.articleCount || 0)
  return {
    min: Math.min(...counts, 0),
    max: Math.max(...counts, 1)
  }
}

const getTagSize = (count) => {
  const { min, max } = getRange()
  const ratio = (count - min) / (max - min || 1)
  return `${0.85 + ratio * 0.28}rem`
}

const getTagClass = (count) => {
  const { min, max } = getRange()
  const ratio = (count - min) / (max - min || 1)
  if (ratio > 0.66) return 'tag-hot'
  if (ratio > 0.33) return 'tag-warm'
  return 'tag-calm'
}
</script>

<style scoped>
.tag-list {
  width: 100%;
}

.tag-cloud {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0.75rem;
}

.tag-item {
  display: inline-flex;
  align-items: center;
  gap: 0.45rem;
  min-height: 2.45rem;
  padding: 0.55rem 0.8rem;
  border: 1px solid var(--kumo-hairline);
  border-radius: 999px;
  background: var(--kumo-bg-elevated);
  color: var(--kumo-text-default);
  font-size: var(--tag-size);
  font-weight: 760;
  text-decoration: none;
  box-shadow: var(--kumo-shadow-sm);
}

.tag-item svg {
  color: var(--kumo-bg-brand);
  font-size: 0.8em;
}

.tag-item strong {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 1.55rem;
  height: 1.55rem;
  padding: 0 0.35rem;
  border-radius: 999px;
  font-size: 0.72em;
  font-weight: 850;
}

.tag-calm strong {
  background: var(--kumo-bg-subtle);
  color: var(--kumo-text-muted);
}

.tag-warm {
  background: var(--kumo-bg-warm);
}

.tag-warm strong {
  background: var(--kumo-status-warning-tint);
  color: var(--kumo-status-warning);
}

.tag-hot {
  background: var(--kumo-bg-brand-soft);
  color: var(--kumo-bg-brand-strong);
}

.tag-hot strong {
  background: var(--kumo-bg-brand);
  color: var(--kumo-text-inverse);
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
