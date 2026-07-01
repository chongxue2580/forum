<template>
  <router-link :to="`/article/${article.id}`" class="article-card kumo-surface magnetic-card">
    <div v-if="coverImage" class="article-cover">
      <img :src="coverImage" :alt="article.title" loading="lazy">
    </div>

    <div class="article-body">
      <div class="article-kicker">
        <span class="kumo-status kumo-status--info">
          <font-awesome-icon :icon="['fas', 'folder']" />
          {{ categoryName }}
        </span>
        <span class="article-time">
          <font-awesome-icon :icon="['fas', 'clock']" />
          {{ formatTime(article.createdAt || article.createTime) }}
        </span>
      </div>

      <h2>{{ article.title }}</h2>
      <p>{{ summary }}</p>

      <div v-if="normalizedTags.length" class="article-tags" aria-label="文章标签">
        <span v-for="tag in normalizedTags" :key="tag" class="article-tag">
          #{{ tag }}
        </span>
      </div>

      <div class="article-footer">
        <router-link :to="`/user/${authorId}`" class="author" @click.stop>
          <span class="author-avatar">
            <img v-if="authorAvatar" :src="authorAvatar" :alt="authorName">
            <span v-else>{{ authorInitial }}</span>
          </span>
          <span>{{ authorName }}</span>
        </router-link>

        <div class="article-stats" aria-label="文章统计">
          <span>
            <font-awesome-icon :icon="['fas', 'eye']" />
            {{ article.viewCount || article.views || 0 }}
          </span>
          <span>
            <font-awesome-icon :icon="['fas', 'thumbs-up']" />
            {{ article.likeCount || article.likes || 0 }}
          </span>
          <span>
            <font-awesome-icon :icon="['fas', 'comment']" />
            {{ commentCount }}
          </span>
        </div>
      </div>
    </div>
  </router-link>
</template>

<script setup>
import { computed } from 'vue'
import { useStore } from 'vuex'
import { formatFriendlyTime } from '../utils/dateUtils'
import { resolveAvatarFrom } from '../utils/avatar'

const props = defineProps({
  article: {
    type: Object,
    required: true
  }
})

const store = useStore()

const categoryName = computed(() => {
  const { category, categoryInfo, categoryName: directName } = props.article
  if (directName) return directName
  if (categoryInfo?.name) return categoryInfo.name
  if (typeof category === 'string' && category.trim()) return category.trim()
  if (category?.name) return category.name

  if (typeof category === 'number') {
    const categories = store.state.categories || []
    return categories[category]?.name || '未分类'
  }

  return '未分类'
})

const coverImage = computed(() => props.article.coverImage || props.article.coverImageUrl || '')

const summary = computed(() => {
  const directSummary = props.article.summary || props.article.excerpt
  if (directSummary) return directSummary

  const content = props.article.content || ''
  const plainText = content
    .replace(/<[^>]*>/g, ' ')
    .replace(/!\[[^\]]*]\([^)]*\)/g, ' ')
    .replace(/[#*_`[\]()]/g, ' ')
    .replace(/\s+/g, ' ')
    .trim()

  return plainText.length > 120 ? `${plainText.substring(0, 120)}...` : plainText
})

const normalizedTags = computed(() => {
  if (!props.article.tags) return []
  if (Array.isArray(props.article.tags)) {
    return props.article.tags
      .map(tag => typeof tag === 'string' ? tag : tag?.name)
      .filter(Boolean)
      .slice(0, 4)
  }
  return String(props.article.tags)
    .split(',')
    .map(tag => tag.trim())
    .filter(Boolean)
    .slice(0, 4)
})

const author = computed(() => props.article.author || props.article.user || {})
const authorId = computed(() => author.value?.id || props.article.authorId || 1)
const authorName = computed(() => author.value?.nickname || author.value?.username || author.value?.name || '匿名')
const authorAvatar = computed(() => resolveAvatarFrom(author.value))
const authorInitial = computed(() => authorName.value.charAt(0).toUpperCase())

const commentCount = computed(() => {
  if (Array.isArray(props.article.comments)) return props.article.comments.length
  return props.article.commentCount || props.article.comments || 0
})

const formatTime = (time) => formatFriendlyTime(time)
</script>

<style scoped>
.article-card {
  display: grid;
  grid-template-columns: minmax(12rem, 18rem) minmax(0, 1fr);
  min-height: 13rem;
  overflow: hidden;
  color: var(--kumo-text-default);
  text-decoration: none;
}

.article-cover {
  min-height: 13rem;
  overflow: hidden;
  background:
    linear-gradient(135deg, var(--kumo-bg-brand-soft), var(--kumo-bg-accent-soft));
}

.article-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 420ms ease;
}

.article-card:hover .article-cover img {
  transform: scale(1.05);
}

.article-body {
  display: flex;
  flex-direction: column;
  gap: 0.9rem;
  padding: clamp(1rem, 2vw, 1.45rem);
}

.article-kicker {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0.7rem;
}

.article-time {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  color: var(--kumo-text-subtle);
  font-size: 0.82rem;
  font-weight: 690;
}

h2 {
  margin: 0;
  color: var(--kumo-text-default);
  font-size: clamp(1.15rem, 2vw, 1.48rem);
  font-weight: 820;
  line-height: 1.28;
}

p {
  display: -webkit-box;
  min-height: 3.2rem;
  margin: 0;
  overflow: hidden;
  color: var(--kumo-text-muted);
  line-height: 1.65;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.article-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 0.45rem;
}

.article-tag {
  padding: 0.3rem 0.55rem;
  border: 1px solid var(--kumo-hairline);
  border-radius: 999px;
  background: var(--kumo-bg-subtle);
  color: var(--kumo-text-muted);
  font-size: 0.78rem;
  font-weight: 720;
}

.article-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  margin-top: auto;
  padding-top: 0.8rem;
  border-top: 1px solid var(--kumo-hairline);
}

.author {
  display: inline-flex;
  align-items: center;
  gap: 0.55rem;
  min-width: 0;
  color: var(--kumo-text-default);
  font-size: 0.9rem;
  font-weight: 760;
  text-decoration: none;
}

.author-avatar {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 2rem;
  height: 2rem;
  flex: 0 0 auto;
  overflow: hidden;
  border-radius: 999px;
  background: var(--kumo-bg-brand-soft);
  color: var(--kumo-bg-brand-strong);
  font-weight: 840;
}

.author-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.article-stats {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 0.45rem;
}

.article-stats span {
  display: inline-flex;
  align-items: center;
  gap: 0.3rem;
  padding: 0.32rem 0.5rem;
  border-radius: 999px;
  background: var(--kumo-bg-subtle);
  color: var(--kumo-text-muted);
  font-size: 0.78rem;
  font-weight: 730;
}

@media (max-width: 760px) {
  .article-card {
    grid-template-columns: 1fr;
  }

  .article-cover {
    aspect-ratio: 16 / 9;
    min-height: auto;
  }

  .article-footer {
    align-items: flex-start;
    flex-direction: column;
  }

  .article-stats {
    justify-content: flex-start;
  }
}
</style>
