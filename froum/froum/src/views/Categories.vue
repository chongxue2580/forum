<template>
  <div class="categories-container">
    <div class="categories-header">
      <div class="header-content">
        <h1>分类浏览</h1>
        <p class="header-description">
          根据不同的技术领域和主题分类浏览文章，找到您感兴趣的内容
        </p>
      </div>
      <div class="header-actions">
        <router-link to="/" class="btn btn-secondary">
          <font-awesome-icon :icon="['fas', 'home']" />
          返回首页
        </router-link>
      </div>
    </div>

    <div v-if="loading" class="loading-container">
      <div class="loading-spinner">
        <font-awesome-icon :icon="['fas', 'spinner']" spin />
      </div>
      <p>正在加载分类...</p>
    </div>
    
    <div v-else>
      <div class="search-section" v-if="categories.length > 0">
        <div class="search-input-container">
          <font-awesome-icon :icon="['fas', 'search']" class="search-icon" />
          <input 
            type="text" 
            class="search-input"
            v-model="searchQuery"
            placeholder="搜索分类..."
          />
          <button 
            v-if="searchQuery" 
            class="clear-search-button"
            @click="searchQuery = ''"
          >
            <font-awesome-icon :icon="['fas', 'times']" />
          </button>
        </div>
      </div>
      
      <category-list :categories="filteredCategories" />
      
      <div v-if="filteredCategories.length === 0 && categories.length > 0" class="no-results">
        <div class="empty-state">
          <font-awesome-icon :icon="['fas', 'search']" class="empty-icon" />
          <p>未找到匹配 "{{ searchQuery }}" 的分类</p>
          <button class="btn btn-secondary" @click="searchQuery = ''">
            清除搜索
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { defineComponent, ref, computed } from 'vue'
import { useStore } from 'vuex'
import CategoryList from '../components/CategoryList.vue'

export default defineComponent({
  name: 'CategoriesView',
  components: {
    CategoryList
  },
  setup() {
    const store = useStore()
    const loading = ref(true)
    const searchQuery = ref('')
    
    const categories = computed(() => store.state.categories)
    
    const filteredCategories = computed(() => {
      if (!searchQuery.value) {
        return categories.value
      }
      
      const query = searchQuery.value.toLowerCase()
      return categories.value.filter(category => 
        category.name.toLowerCase().includes(query) ||
        category.description?.toLowerCase().includes(query) ||
        category.subcategories?.some(sub => sub.name.toLowerCase().includes(query))
      )
    })
    
    const loadCategories = async () => {
      try {
        loading.value = true
        if (categories.value.length === 0) {
          await store.dispatch('fetchCategories')
        }
      } catch (error) {
        console.error('加载分类失败:', error)
      } finally {
        loading.value = false
      }
    }
    
    loadCategories()
    
    return {
      categories,
      filteredCategories,
      searchQuery,
      loading
    }
  }
})
</script>

<style scoped>
.categories-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 1rem;
}

.categories-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 2rem;
  padding-bottom: 1.5rem;
  border-bottom: 1px solid var(--border-color);
}

.header-content h1 {
  margin: 0 0 0.5rem 0;
  color: var(--text-color);
  font-size: 1.75rem;
}

.header-description {
  margin: 0;
  color: var(--text-light);
  font-size: 1rem;
}

.header-actions {
  display: flex;
  gap: 1rem;
}

.btn {
  padding: 0.625rem 1.25rem;
  border-radius: 20px;
  font-weight: 600;
  transition: all 0.2s ease;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  text-decoration: none;
}

.btn-secondary {
  background-color: #f5f5f5;
  color: var(--text-color);
  border: none;
  cursor: pointer;
}

.btn-secondary:hover {
  background-color: #e0e0e0;
}

.loading-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 3rem 0;
}

.loading-spinner {
  font-size: 2rem;
  color: var(--primary-color);
  margin-bottom: 1rem;
}

.search-section {
  margin-bottom: 1.5rem;
}

.search-input-container {
  position: relative;
  max-width: 500px;
}

.search-icon {
  position: absolute;
  left: 1rem;
  top: 50%;
  transform: translateY(-50%);
  color: var(--text-light);
}

.search-input {
  width: 100%;
  padding: 0.75rem 2.5rem;
  border-radius: 20px;
  border: 1px solid var(--border-color);
  font-size: 1rem;
  transition: all 0.2s ease;
}

.search-input:focus {
  outline: none;
  border-color: var(--primary-color);
  box-shadow: 0 0 0 2px rgba(var(--primary-rgb), 0.2);
}

.clear-search-button {
  position: absolute;
  right: 1rem;
  top: 50%;
  transform: translateY(-50%);
  background: none;
  border: none;
  cursor: pointer;
  color: var(--text-light);
  padding: 0.25rem;
  border-radius: 50%;
}

.clear-search-button:hover {
  background-color: #f0f0f0;
  color: var(--text-color);
}

.no-results {
  padding: 3rem 0;
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
  margin: 0.5rem 0 1.5rem;
  color: var(--text-color);
  font-size: 1rem;
}

@media (max-width: 768px) {
  .categories-header {
    flex-direction: column;
    align-items: flex-start;
  }
  
  .header-actions {
    margin-top: 1rem;
  }
}
</style> 