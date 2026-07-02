<template>
  <section class="steep-home-panel steep-surface">
    <div class="steep-panel-copy">
      <span class="steep-tag">Steep Workspace</span>
      <h2 class="steep-display">Soft dawn on a marble dashboard</h2>
      <p class="steep-subtitle">
        用更安静的可视化方式观察论坛内容：文章、问答、分类和标签被组织成一组可扫描的数据卡片。
      </p>
      <div class="steep-actions">
        <router-link class="steep-filled-cta" to="/articles">浏览文章</router-link>
        <router-link class="steep-text-link" to="/questions">查看问答</router-link>
      </div>
    </div>

    <div class="steep-dashboard-shell">
      <aside class="steep-sidebar-nav" aria-label="Steep sidebar preview">
        <div class="steep-nav-brand">
          <span class="steep-avatar-badge">TF</span>
          <strong>Forum</strong>
        </div>
        <span class="steep-nav-item is-active">
          <font-awesome-icon :icon="['fas', 'chart-line']" />
          Overview
        </span>
        <span class="steep-nav-item">
          <font-awesome-icon :icon="['fas', 'file-alt']" />
          Articles
        </span>
        <span class="steep-nav-item">
          <font-awesome-icon :icon="['fas', 'question-circle']" />
          Questions
        </span>
        <div class="steep-team-list">
          <small>Topics</small>
          <span v-for="topic in topicPreview" :key="topic.label">
            <i :style="{ background: topic.color }"></i>
            {{ topic.label }}
          </span>
        </div>
      </aside>

      <div class="steep-dashboard-grid">
        <article class="steep-dashboard-card is-warm steep-donut-card">
          <div class="steep-card-head">
            <span>Content mix</span>
            <strong>{{ totalContent }}</strong>
          </div>
          <div
            class="steep-donut"
            :style="{ '--article-share': articleShare }"
            aria-hidden="true"
          >
            <span></span>
          </div>
          <div class="steep-legend">
            <span><i class="is-rust"></i> 文章 {{ articleCount }}</span>
            <span><i class="is-blue"></i> 问答 {{ questionCount }}</span>
          </div>
        </article>

        <article class="steep-dashboard-card is-cool steep-line-card">
          <div class="steep-card-head">
            <span>Knowledge signal</span>
            <strong>{{ signalScore }}%</strong>
          </div>
          <svg viewBox="0 0 220 96" role="img" aria-label="主题信号趋势">
            <path class="line-last" d="M4 68 C38 44, 66 52, 92 38 S146 30, 174 42 S204 52, 216 24" />
            <path class="line-now" d="M4 78 C38 70, 64 48, 92 56 S146 18, 174 28 S204 36, 216 14" />
            <circle cx="174" cy="28" r="5" />
          </svg>
          <div class="steep-floating-avatar">
            <span class="steep-avatar-badge">AI</span>
          </div>
        </article>

        <article class="steep-dashboard-card steep-region-card">
          <div class="steep-card-head">
            <span>Region</span>
            <strong>Index</strong>
          </div>
          <div class="steep-region-list">
            <div v-for="row in regionRows" :key="row.name" class="steep-region-row">
              <span>{{ row.name }}</span>
              <strong>{{ row.value }}</strong>
            </div>
          </div>
        </article>

        <article class="steep-dashboard-card steep-ai-card">
          <div class="steep-chat-input">
            <span>{{ insightPrompt }}</span>
            <button type="button" aria-label="查看高价值讨论" @click="openPrimaryDiscussion">
              <font-awesome-icon :icon="['fas', 'arrow-right']" />
            </button>
          </div>
          <div class="steep-ai-response">
            <div class="steep-ai-response-head">
              <span class="steep-avatar-badge">AI</span>
              <div>
                <h3>{{ insightTitle }}</h3>
                <p>{{ insightSummary }}</p>
              </div>
            </div>

            <div class="insight-metrics">
              <div v-for="metric in insightMetrics" :key="metric.label" class="insight-metric">
                <strong>{{ metric.value }}</strong>
                <span>{{ metric.label }}</span>
              </div>
            </div>

            <div class="insight-layout">
              <div class="insight-discussions">
                <button
                  v-for="item in highValueDiscussions"
                  :key="`${item.type}-${item.id}`"
                  type="button"
                  class="insight-discussion"
                  @click="openDiscussion(item)"
                >
                  <span class="discussion-score">{{ item.score }}</span>
                  <span class="discussion-body">
                    <strong>{{ item.title }}</strong>
                    <small>{{ item.meta }}</small>
                  </span>
                  <font-awesome-icon :icon="['fas', 'arrow-up-right-from-square']" />
                </button>
              </div>

              <div class="insight-actions">
                <div v-for="action in insightActions" :key="action.title" class="insight-action">
                  <font-awesome-icon :icon="['fas', action.icon]" />
                  <span>
                    <strong>{{ action.title }}</strong>
                    <small>{{ action.detail }}</small>
                  </span>
                </div>
              </div>
            </div>

            <div class="insight-keywords" aria-label="高价值主题">
              <span v-for="keyword in insightKeywords" :key="keyword">#{{ keyword }}</span>
            </div>
          </div>
        </article>
      </div>
    </div>
  </section>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()

const props = defineProps({
  articleCount: {
    type: Number,
    default: 0
  },
  questionCount: {
    type: Number,
    default: 0
  },
  categoryCount: {
    type: Number,
    default: 0
  },
  tagCount: {
    type: Number,
    default: 0
  },
  topics: {
    type: Array,
    default: () => []
  },
  articles: {
    type: Array,
    default: () => []
  },
  hotArticles: {
    type: Array,
    default: () => []
  },
  recommendedArticles: {
    type: Array,
    default: () => []
  },
  questions: {
    type: Array,
    default: () => []
  }
})

const totalContent = computed(() => props.articleCount + props.questionCount)
const safeTotal = computed(() => Math.max(totalContent.value, 1))
const articleShare = computed(() => Math.round((props.articleCount / safeTotal.value) * 100))
const questionShare = computed(() => Math.max(0, 100 - articleShare.value))
const signalScore = computed(() => Math.min(96, Math.max(24, totalContent.value * 4 + props.categoryCount * 5 + props.tagCount * 2)))
const topicPreview = computed(() => {
  const colors = ['#5d2a1a', '#4a90e2', '#777b86']
  const normalized = props.topics.slice(0, 3).map((topic, index) => ({
    label: topic?.name || topic?.label || topic?.title || `Topic ${index + 1}`,
    color: colors[index % colors.length]
  }))

  return normalized.length ? normalized : [
    { label: 'Architecture', color: colors[0] },
    { label: 'Frontend', color: colors[1] },
    { label: 'Backend', color: colors[2] }
  ]
})

const regionRows = computed(() => [
  { name: 'Articles', value: props.articleCount },
  { name: 'Questions', value: props.questionCount },
  { name: 'Categories', value: props.categoryCount },
  { name: 'Tags', value: props.tagCount }
])

const normalizeNumber = (value) => Number.isFinite(Number(value)) ? Number(value) : 0
const getItemTitle = (item, fallback) => item?.title || item?.name || fallback
const getItemSummary = (item) => String(item?.summary || item?.content || item?.description || '')
const stripMarkdown = (value = '') => String(value)
  .replace(/```[\s\S]*?```/g, ' ')
  .replace(/!\[[^\]]*]\([^)]*\)/g, ' ')
  .replace(/\[([^\]]*)]\([^)]*\)/g, '$1')
  .replace(/<[^>]+>/g, ' ')
  .replace(/[#>*_`~\-]+/g, ' ')
  .replace(/\s+/g, ' ')
  .trim()

const normalizeTags = (item) => {
  const tags = item?.tags
  if (!tags) return []
  if (Array.isArray(tags)) {
    return tags
      .map(tag => typeof tag === 'string' ? tag : tag?.name || tag?.label || tag?.title)
      .filter(Boolean)
  }
  return String(tags).split(',').map(tag => tag.trim()).filter(Boolean)
}

const dedupeByTypeAndId = (items) => {
  const seen = new Set()
  return items.filter((item) => {
    const key = `${item.type}-${item.id || item.title}`
    if (seen.has(key)) return false
    seen.add(key)
    return true
  })
}

const articleDiscussions = computed(() => {
  const combined = [
    ...props.hotArticles.map(item => ({ ...item, _sourceBoost: 24 })),
    ...props.recommendedArticles.map(item => ({ ...item, _sourceBoost: 18 })),
    ...props.articles.map(item => ({ ...item, _sourceBoost: 8 }))
  ]

  return dedupeByTypeAndId(combined.map((item, index) => ({
    id: item.id || `article-${index}`,
    type: 'article',
    title: getItemTitle(item, `文章 ${index + 1}`),
    summary: stripMarkdown(getItemSummary(item)),
    tags: normalizeTags(item),
    views: normalizeNumber(item.viewCount ?? item.views),
    answers: normalizeNumber(item.commentCount ?? item.comments?.length ?? item.commentTotal),
    likes: normalizeNumber(item.likeCount ?? item.likes),
    featured: Boolean(item.isFeatured || item.isHighlight || item.isOfficial),
    boost: item._sourceBoost || 0,
    navigable: Boolean(item.id)
  })))
})

const questionDiscussions = computed(() => props.questions.map((item, index) => ({
  id: item.id || `question-${index}`,
  type: 'question',
  title: getItemTitle(item, `问题 ${index + 1}`),
  summary: stripMarkdown(getItemSummary(item)),
  tags: normalizeTags(item),
  views: normalizeNumber(item.viewCount ?? item.views),
  answers: normalizeNumber(item.answerCount ?? item.commentCount),
  likes: normalizeNumber(item.voteCount ?? item.likeCount),
  solved: Boolean(item.solved || item.isSolved),
  boost: Boolean(item.solved || item.isSolved) ? 12 : 6,
  navigable: Boolean(item.id)
})))

const scoredDiscussions = computed(() => {
  return [...articleDiscussions.value, ...questionDiscussions.value]
    .map((item) => {
      const score = Math.min(99, Math.round(
        item.views * 0.08 +
        item.answers * 7 +
        item.likes * 5 +
        item.tags.length * 3 +
        item.boost +
        (item.featured ? 10 : 0)
      ))
      return {
        ...item,
        score: Math.max(18, score),
        meta: [
          item.type === 'article' ? '文章' : '问答',
          `${item.views} 浏览`,
          `${item.answers} ${item.type === 'article' ? '评论' : '回答'}`
        ].join(' · ')
      }
    })
    .sort((a, b) => b.score - a.score)
})

const fallbackDiscussions = computed(() => {
  const topics = topicPreview.value
  return topics.slice(0, 3).map((topic, index) => ({
    id: topic.label,
    type: index === 0 ? 'article' : 'question',
    title: `${topic.label} 方向值得继续沉淀`,
    summary: '当前数据不足，先用热门主题生成观察线索。',
    tags: [topic.label],
    views: 0,
    answers: 0,
    likes: 0,
    score: 42 - index * 4,
    meta: '主题信号 · 待更多讨论',
    navigable: false
  }))
})

const highValueDiscussions = computed(() => {
  const discussions = scoredDiscussions.value.length ? scoredDiscussions.value : fallbackDiscussions.value
  return discussions.slice(0, 3)
})

const insightKeywords = computed(() => {
  const fromDiscussions = highValueDiscussions.value
    .flatMap(item => item.tags)
    .filter(Boolean)
  const fromTopics = topicPreview.value.map(topic => topic.label)
  return [...new Set([...fromDiscussions, ...fromTopics])].slice(0, 6)
})

const averageDiscussionScore = computed(() => {
  if (!highValueDiscussions.value.length) return 0
  const total = highValueDiscussions.value.reduce((sum, item) => sum + item.score, 0)
  return Math.round(total / highValueDiscussions.value.length)
})

const solvedQuestionCount = computed(() => props.questions.filter(item => item.solved || item.isSolved).length)
const unresolvedQuestionCount = computed(() => Math.max(0, props.questions.length - solvedQuestionCount.value))
const articleDiscussionCount = computed(() => articleDiscussions.value.length)
const questionDiscussionCount = computed(() => questionDiscussions.value.length)

const insightPrompt = computed(() => {
  const keyword = insightKeywords.value[0] || '社区内容'
  return `分析最近的高价值技术讨论：${keyword}`
})

const insightTitle = computed(() => {
  if (!scoredDiscussions.value.length) return '等待更多讨论样本'
  if (averageDiscussionScore.value >= 72) return '高价值讨论正在聚集'
  if (unresolvedQuestionCount.value > solvedQuestionCount.value) return '问答协作需要跟进'
  return '今日讨论结构稳定'
})

const insightSummary = computed(() => {
  if (!scoredDiscussions.value.length) {
    return `当前内容池里文章占比 ${articleShare.value}%，问答占比 ${questionShare.value}%。发布更多文章或问题后，这里会自动生成讨论价值分析。`
  }

  const top = highValueDiscussions.value[0]
  const focus = insightKeywords.value.slice(0, 2).join('、') || '核心主题'
  return `最近 ${articleDiscussionCount.value + questionDiscussionCount.value} 条内容中，“${top.title}”价值分最高。主题集中在 ${focus}，建议优先跟进高回答问题并把成熟方案整理为文章。`
})

const insightMetrics = computed(() => [
  { label: '价值均分', value: averageDiscussionScore.value || '--' },
  { label: '分析样本', value: articleDiscussionCount.value + questionDiscussionCount.value },
  { label: '待跟进', value: unresolvedQuestionCount.value }
])

const insightActions = computed(() => {
  const actions = []

  if (unresolvedQuestionCount.value > 0) {
    actions.push({
      icon: 'circle-question',
      title: '优先回答未解决问题',
      detail: `${unresolvedQuestionCount.value} 个问题还可以继续补充方案`
    })
  }

  if (insightKeywords.value.length) {
    actions.push({
      icon: 'tags',
      title: '维护高频主题',
      detail: `聚焦 ${insightKeywords.value.slice(0, 2).join('、')}`
    })
  }

  actions.push({
    icon: 'file-pen',
    title: '沉淀成熟结论',
    detail: `文章占比 ${articleShare.value}%，问答占比 ${questionShare.value}%`
  })

  return actions.slice(0, 3)
})

const openDiscussion = (item) => {
  if (!item?.id || !item.navigable) return
  router.push(item.type === 'article' ? `/article/${item.id}` : `/question/${item.id}`)
}

const openPrimaryDiscussion = () => {
  openDiscussion(highValueDiscussions.value[0])
}
</script>

<style scoped>
.steep-home-panel {
  position: relative;
  display: grid;
  grid-template-columns: minmax(0, 0.36fr) minmax(0, 0.64fr);
  gap: clamp(1.25rem, 3vw, 2rem);
  padding: clamp(1.35rem, 3.4vw, 2.5rem);
  border: 1px solid color-mix(in srgb, var(--steep-dove) 48%, transparent);
  border-radius: 28px;
  background:
    radial-gradient(circle at 28% 12%, rgba(251, 225, 209, 0.64), transparent 28rem),
    var(--steep-pure-white);
  overflow: hidden;
}

.steep-panel-copy {
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 1rem;
  min-width: 0;
}

.steep-panel-copy .steep-display {
  max-width: 9ch;
}

.steep-actions {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 1rem;
  margin-top: 0.4rem;
}

.steep-dashboard-shell {
  display: grid;
  grid-template-columns: minmax(10rem, 0.28fr) minmax(0, 0.72fr);
  gap: 1rem;
  min-width: 0;
  padding: 1rem;
  border-radius: 26px;
  background: var(--steep-fog);
}

.steep-nav-brand {
  display: flex;
  align-items: center;
  gap: 0.65rem;
  margin-bottom: 0.35rem;
  color: var(--steep-ink);
}

.steep-team-list {
  display: grid;
  gap: 0.5rem;
  margin-top: 0.8rem;
}

.steep-team-list small {
  color: var(--steep-graphite);
  font-size: 12px;
  font-weight: 500;
  letter-spacing: 0.04em;
  text-transform: uppercase;
}

.steep-team-list span {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  min-width: 0;
  color: var(--steep-ash);
  font-size: 14px;
  font-weight: 450;
}

.steep-team-list i {
  width: 8px;
  height: 8px;
  border-radius: 999px;
}

.steep-dashboard-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 1rem;
  min-width: 0;
}

.steep-card-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 1rem;
}

.steep-card-head span {
  color: var(--steep-graphite);
  font-size: 14px;
  font-weight: 450;
}

.steep-card-head strong {
  color: var(--steep-ink);
  font-size: 26px;
  font-weight: 480;
  line-height: 1;
}

.steep-donut-card,
.steep-line-card,
.steep-ai-card {
  position: relative;
  overflow: hidden;
}

.steep-donut {
  width: 7.5rem;
  aspect-ratio: 1;
  margin: 1.2rem auto 0.9rem;
  border-radius: 999px;
  background: conic-gradient(
    var(--steep-rust) 0 calc(var(--article-share) * 1%),
    var(--steep-blue) calc(var(--article-share) * 1%) 100%
  );
  display: grid;
  place-items: center;
}

.steep-donut span {
  width: 4.4rem;
  aspect-ratio: 1;
  border-radius: inherit;
  background: var(--steep-apricot-wash);
  box-shadow: inset 0 0 0 1px rgba(93, 42, 26, 0.12);
}

.steep-legend {
  display: flex;
  flex-wrap: wrap;
  gap: 0.7rem;
  color: var(--steep-ash);
  font-size: 14px;
}

.steep-legend span {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
}

.steep-legend i {
  width: 8px;
  height: 8px;
  border-radius: 999px;
}

.steep-legend .is-rust {
  background: var(--steep-rust);
}

.steep-legend .is-blue {
  background: var(--steep-blue);
}

.steep-line-card svg {
  width: 100%;
  height: auto;
  margin-top: 1.2rem;
  overflow: visible;
}

.steep-line-card path {
  fill: none;
  stroke-width: 4;
  stroke-linecap: round;
}

.line-last {
  stroke: rgba(93, 42, 26, 0.72);
}

.line-now {
  stroke: var(--steep-blue);
}

.steep-line-card circle {
  fill: var(--steep-rust);
  stroke: var(--steep-pure-white);
  stroke-width: 3;
}

.steep-floating-avatar {
  position: absolute;
  right: 1rem;
  bottom: 1rem;
}

.steep-region-list {
  display: grid;
  gap: 12px;
  margin-top: 1.2rem;
}

.steep-ai-card {
  grid-column: span 2;
  display: grid;
  gap: 1rem;
}

.steep-ai-response {
  display: grid;
  gap: 1rem;
  padding: 1rem;
  border-radius: 16px;
  background: var(--steep-sky-wash);
}

.steep-ai-response-head {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  gap: 0.85rem;
  align-items: start;
}

.steep-ai-response h3 {
  margin: 0 0 0.45rem;
  color: var(--steep-ink);
  font-size: 22px;
  font-weight: 500;
  letter-spacing: -0.009em;
}

.steep-ai-response p {
  margin: 0;
  color: var(--steep-ash);
  font-size: 16px;
  line-height: 1.38;
}

.insight-metrics {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 0.7rem;
}

.insight-metric {
  display: grid;
  gap: 0.2rem;
  min-width: 0;
  padding: 0.8rem;
  border: 1px solid rgba(23, 25, 28, 0.08);
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.68);
}

.insight-metric strong {
  color: var(--steep-ink);
  font-size: 24px;
  font-weight: 480;
  line-height: 1;
}

.insight-metric span {
  color: var(--steep-graphite);
  font-size: 13px;
  font-weight: 450;
}

.insight-layout {
  display: grid;
  grid-template-columns: minmax(0, 1.1fr) minmax(12rem, 0.9fr);
  gap: 0.9rem;
  align-items: start;
}

.insight-discussions,
.insight-actions {
  display: grid;
  gap: 0.65rem;
}

.insight-discussion {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  align-items: center;
  gap: 0.7rem;
  width: 100%;
  min-width: 0;
  padding: 0.75rem;
  border: 1px solid rgba(23, 25, 28, 0.08);
  border-radius: 14px;
  background: var(--steep-pure-white);
  color: inherit;
  font: inherit;
  text-align: left;
  cursor: pointer;
  transition:
    border-color 180ms ease,
    transform 180ms ease,
    box-shadow 180ms ease;
}

.insight-discussion:hover {
  transform: translateY(-1px);
  border-color: rgba(23, 25, 28, 0.18);
  box-shadow: 0 8px 18px rgba(23, 25, 28, 0.08);
}

.discussion-score {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 2.35rem;
  height: 2.35rem;
  border-radius: 999px;
  background: var(--steep-apricot-wash);
  color: var(--steep-rust);
  font-size: 14px;
  font-weight: 500;
}

.discussion-body {
  display: grid;
  gap: 0.18rem;
  min-width: 0;
}

.discussion-body strong {
  overflow: hidden;
  color: var(--steep-ink);
  font-size: 15px;
  font-weight: 500;
  line-height: 1.25;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.discussion-body small {
  overflow: hidden;
  color: var(--steep-graphite);
  font-size: 12px;
  line-height: 1.3;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.insight-discussion > svg {
  color: var(--steep-graphite);
  font-size: 13px;
}

.insight-action {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  gap: 0.65rem;
  align-items: start;
  padding: 0.72rem;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.5);
}

.insight-action > svg {
  margin-top: 0.1rem;
  color: var(--steep-rust);
}

.insight-action span {
  display: grid;
  gap: 0.2rem;
  min-width: 0;
}

.insight-action strong {
  color: var(--steep-ink);
  font-size: 14px;
  font-weight: 500;
  line-height: 1.25;
}

.insight-action small {
  color: var(--steep-ash);
  font-size: 13px;
  line-height: 1.35;
}

.insight-keywords {
  display: flex;
  flex-wrap: wrap;
  gap: 0.45rem;
}

.insight-keywords span {
  display: inline-flex;
  align-items: center;
  min-height: 1.75rem;
  padding: 0.28rem 0.6rem;
  border: 1px solid rgba(23, 25, 28, 0.08);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.58);
  color: var(--steep-ash);
  font-size: 13px;
  font-weight: 450;
}

@media (max-width: 1080px) {
  .steep-home-panel,
  .steep-dashboard-shell {
    grid-template-columns: 1fr;
  }

  .steep-panel-copy .steep-display {
    max-width: 14ch;
  }
}

@media (max-width: 680px) {
  .steep-dashboard-grid {
    grid-template-columns: 1fr;
  }

  .steep-ai-card {
    grid-column: auto;
  }

  .insight-layout,
  .insight-metrics {
    grid-template-columns: 1fr;
  }

  .steep-sidebar-nav {
    display: none;
  }
}

@media (max-width: 480px) {
  .steep-ai-response-head,
  .insight-discussion {
    grid-template-columns: 1fr;
  }

  .discussion-body strong,
  .discussion-body small {
    white-space: normal;
  }

  .insight-discussion > svg {
    display: none;
  }
}
</style>
