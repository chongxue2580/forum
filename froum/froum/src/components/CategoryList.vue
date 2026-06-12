<template>
  <div class="category-list">
    <div v-if="categories.length === 0" class="no-categories">
      <div class="empty-state">
        <font-awesome-icon :icon="['fas', 'folder-open']" class="empty-icon" />
        <p>暂无分类内容</p>
      </div>
    </div>
    
    <div v-else class="category-grid">
      <router-link 
        v-for="category in categories" 
        :key="category.id" 
        :to="`/category/${category.id}`"
        class="category-card"
      >
        <div class="category-icon">
          <font-awesome-icon :icon="getRandomIcon(category)" />
        </div>
        <div class="category-content">
          <h3>{{ category.name }}</h3>
          <p>{{ category.articleCount || 0 }}篇文章</p>
        </div>
        <div class="category-arrow">
          <font-awesome-icon :icon="['fas', 'chevron-right']" />
        </div>
      </router-link>
    </div>
  </div>
</template>

<script>
export default {
  name: 'CategoryList',
  props: {
    categories: {
      type: Array,
      default: () => []
    }
  },
  methods: {
    getRandomIcon(category) {
      // 为特定类别分配固定图标
      const categoryIcons = {
        '前端开发': ['fas', 'code'],
        '后端开发': ['fas', 'server'],
        '移动开发': ['fas', 'mobile-alt'],
        '人工智能': ['fas', 'robot'],
        '开发工具': ['fas', 'tools']
      };
      
      if (categoryIcons[category.name]) {
        return categoryIcons[category.name];
      }
      
      // 为其他类别分配基于ID的图标
      const icons = [
        'folder', 'code', 'laptop-code', 'server', 
        'database', 'mobile-alt', 'desktop', 'browser', 
        'cloud', 'network-wired', 'microchip', 'robot'
      ];
      
      const index = category.id % icons.length;
      return ['fas', icons[index]];
    }
  }
}
</script>

<style scoped>
.category-list {
  width: 100%;
}

.no-categories {
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

.category-grid {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.category-card {
  display: flex;
  align-items: center;
  padding: 1rem;
  background-color: #fff;
  border-radius: var(--radius);
  text-decoration: none;
  color: var(--text-color);
  box-shadow: var(--shadow);
  transition: transform 0.2s ease, box-shadow 0.2s ease;
  position: relative;
  overflow: hidden;
}

.category-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  width: 4px;
  height: 100%;
  background: linear-gradient(to bottom, var(--primary-color), var(--accent-color));
  opacity: 0;
  transition: opacity 0.2s ease;
}

.category-card:hover {
  transform: translateY(-3px) translateX(3px);
  box-shadow: var(--shadow-lg);
}

.category-card:hover::before {
  opacity: 1;
}

.category-icon {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  background: linear-gradient(135deg, var(--primary-light), #fff);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.25rem;
  color: var(--primary-color);
  margin-right: 1rem;
  transition: transform 0.3s ease, background 0.3s ease, color 0.3s ease;
  flex-shrink: 0;
}

.category-card:hover .category-icon {
  transform: scale(1.1);
  background: linear-gradient(135deg, var(--primary-color), var(--accent-color));
  color: white;
}

.category-content {
  flex: 1;
}

h3 {
  margin: 0;
  font-size: 1rem;
  font-weight: 600;
  margin-bottom: 0.25rem;
  transition: color 0.2s ease;
}

.category-card:hover h3 {
  color: var(--primary-color);
}

p {
  margin: 0;
  font-size: 0.875rem;
  color: var(--text-light);
}

.category-arrow {
  margin-left: 1rem;
  font-size: 0.875rem;
  color: var(--text-light);
  opacity: 0.5;
  transition: transform 0.3s ease, opacity 0.3s ease, color 0.3s ease;
}

.category-card:hover .category-arrow {
  transform: translateX(3px);
  opacity: 1;
  color: var(--primary-color);
}

@media (max-width: 480px) {
  .category-card {
    padding: 0.75rem;
  }
  
  .category-icon {
    width: 36px;
    height: 36px;
    font-size: 1rem;
    margin-right: 0.75rem;
  }
}
</style>
