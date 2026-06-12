<template>
  <div class="tag-list">
    <div v-if="tags.length === 0" class="no-tags">
      <div class="empty-state">
        <font-awesome-icon :icon="['fas', 'tags']" class="empty-icon" />
        <p>暂无标签内容</p>
      </div>
    </div>
    
    <div v-else class="tag-cloud">
      <router-link 
        v-for="tag in tags" 
        :key="tag.id" 
        :to="`/tag/${tag.id}`"
        class="tag-item"
        :style="getTagStyle(tag.count)"
        :class="getTagClass(tag.count)"
      >
        <font-awesome-icon :icon="['fas', 'tag']" class="tag-icon" />
        <span class="tag-name">{{ tag.name }}</span>
        <span class="tag-count">{{ tag.count }}</span>
      </router-link>
    </div>
  </div>
</template>

<script>
export default {
  name: 'TagList',
  props: {
    tags: {
      type: Array,
      default: () => []
    }
  },
  methods: {
    getTagStyle(count) {
      // 根据文章数量调整标签大小
      if (!this.tags || this.tags.length === 0) return {};
      
      const maxCount = Math.max(...this.tags.map(t => t.count));
      const minCount = Math.min(...this.tags.map(t => t.count));
      const range = maxCount - minCount || 1;
      const fontSize = 0.75 + ((count - minCount) / range) * 0.5;
      const fontWeight = 400 + Math.floor((count - minCount) / range * 200);
      
      return {
        fontSize: `${fontSize}rem`,
        fontWeight: fontWeight
      };
    },
    getTagClass(count) {
      if (!this.tags || this.tags.length === 0) return 'tag-normal';
      
      const maxCount = Math.max(...this.tags.map(t => t.count));
      const minCount = Math.min(...this.tags.map(t => t.count));
      const range = maxCount - minCount || 1;
      const level = Math.floor(((count - minCount) / range) * 3);
      
      switch(level) {
        case 0: return 'tag-low';
        case 1: return 'tag-medium';
        case 2: 
        case 3: return 'tag-high';
        default: return 'tag-normal';
      }
    }
  }
}
</script>

<style scoped>
.tag-list {
  width: 100%;
}

.no-tags {
  padding: 1.5rem 0;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  padding: 2rem;
  background-color: #fff;
  border-radius: var(--radius);
  box-shadow: var(--shadow);
}

.empty-icon {
  font-size: 2.5rem;
  color: var(--text-light);
  margin-bottom: 1rem;
  opacity: 0.5;
}

.empty-state p {
  margin: 0.5rem 0;
  color: var(--text-color);
  font-size: 1rem;
}

.tag-cloud {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
  align-items: center;
  padding: 0.5rem;
}

.tag-item {
  background-color: var(--primary-light);
  color: var(--primary-color);
  padding: 0.5rem 1rem;
  border-radius: 20px;
  text-decoration: none;
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  transition: all 0.25s cubic-bezier(0.25, 0.46, 0.45, 0.94);
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
  position: relative;
  overflow: hidden;
}

.tag-item::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: linear-gradient(135deg, rgba(255,255,255,0.1), rgba(255,255,255,0));
  opacity: 0;
  transition: opacity 0.25s ease;
}

.tag-item:hover {
  transform: translateY(-3px);
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1);
}

.tag-item:hover::before {
  opacity: 1;
}

.tag-icon {
  font-size: 0.75em;
  opacity: 0.7;
}

.tag-name {
  line-height: 1.2;
}

.tag-count {
  background-color: rgba(255, 255, 255, 0.25);
  font-size: 0.75em;
  padding: 0.1rem 0.4rem;
  border-radius: 10px;
  transition: background-color 0.25s ease;
  min-width: 1.5rem;
  text-align: center;
}

.tag-item:hover .tag-count {
  background-color: rgba(255, 255, 255, 0.4);
}

/* 标签样式变体 */
.tag-low {
  background-color: rgba(var(--primary-rgb), 0.1);
  color: var(--primary-color);
}

.tag-medium {
  background-color: rgba(var(--primary-rgb), 0.2);
  color: var(--primary-dark);
}

.tag-high {
  background-color: rgba(var(--primary-rgb), 0.3);
  color: var(--primary-dark);
}

.tag-low:hover {
  background-color: rgba(var(--primary-rgb), 0.2);
}

.tag-medium:hover {
  background-color: rgba(var(--primary-rgb), 0.3);
}

.tag-high:hover {
  background-color: rgba(var(--primary-rgb), 0.4);
}

@media (max-width: 768px) {
  .tag-cloud {
    gap: 0.5rem;
  }
  
  .tag-item {
    padding: 0.35rem 0.75rem;
  }
}
</style> 