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
            <span>分析最近的高价值技术讨论...</span>
            <button type="button" aria-label="发送">
              <font-awesome-icon :icon="['fas', 'arrow-right']" />
            </button>
          </div>
          <div class="steep-ai-response">
            <h3>今日内容结构稳定</h3>
            <p>
              文章沉淀占比 {{ articleShare }}%，问答协作占比 {{ questionShare }}%。建议优先维护高频标签和活跃分类。
            </p>
          </div>
        </article>
      </div>
    </div>
  </section>
</template>

<script setup>
import { computed } from 'vue'

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
  padding: 1rem;
  border-radius: 16px;
  background: var(--steep-sky-wash);
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

  .steep-sidebar-nav {
    display: none;
  }
}
</style>
