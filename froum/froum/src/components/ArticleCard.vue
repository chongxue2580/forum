<template>
  <router-link :to="`/article/${article.id}`" class="article-card">
    <div class="article-card-inner">
      <!-- 文章封面图 -->
      <div v-if="article.coverImage" class="article-cover">
        <img :src="article.coverImage" :alt="article.title" />
      </div>
      
      <!-- 文章内容 -->
      <div class="article-content">
        <div class="article-meta">
          <div class="category-tag" v-if="categoryName">
            <font-awesome-icon :icon="['fas', 'folder']" />
            <span>{{ categoryName }}</span>
          </div>
          <div class="time">
            <font-awesome-icon :icon="['fas', 'clock']" />
            <span>{{ formatTime(article.createdAt || article.createTime) }}</span>
          </div>
        </div>
        
        <h2 class="article-title">{{ article.title }}</h2>
        
        <p class="article-summary">{{ article.summary || truncateContent }}</p>
        
        <div class="article-tags" v-if="article.tags && article.tags.length">
          <div class="tag" v-for="(tag, index) in article.tags" :key="index">
            <font-awesome-icon :icon="['fas', 'tag']" />
            <span>{{ tag }}</span>
          </div>
        </div>
        
        <div class="article-footer">
          <router-link :to="`/user/${article.author?.id}`" class="author" @click.stop>
            <div class="author-avatar" v-if="article.author?.avatarUrl || article.author?.avatar">
              <img :src="getAvatarPath(article.author.avatarUrl || article.author.avatar)" :alt="getAuthorName(article.author)" />
            </div>
            <div class="author-avatar" v-else>
              <span>{{ getAuthorInitials(getAuthorName(article.author)) }}</span>
            </div>
            <span class="author-name">{{ getAuthorName(article.author) || '匿名' }}</span>
          </router-link>
          
          <div class="article-stats">
            <div class="stat-item">
              <font-awesome-icon :icon="['fas', 'eye']" />
              <span>{{ article.viewCount || article.views || 0 }}</span>
            </div>
            <div class="stat-item">
              <font-awesome-icon :icon="['fas', 'thumbs-up']" />
              <span>{{ article.likeCount || article.likes || 0 }}</span>
            </div>
            <div class="stat-item">
              <font-awesome-icon :icon="['fas', 'comment']" />
              <span>{{ article.commentCount || getCommentsCount }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </router-link>
</template>

<script>
import { defineComponent, computed } from 'vue'
import { useStore } from 'vuex'
import { formatFriendlyTime } from '../utils/dateUtils'

export default defineComponent({
  name: 'ArticleCard',
  props: {
    article: {
      type: Object,
      required: true,
      default: () => ({
        id: 0,
        title: '加载中...',
        summary: '',
        content: '',
        author: { name: '未知作者', avatar: '' },
        createTime: '',
        views: 0,
        likes: 0,
        comments: [],
        tags: []
      })
    }
  },
  setup(props) {
    const store = useStore();
    
    // 从文章内容中生成摘要
    const truncateContent = computed(() => {
      if (props.article.content && !props.article.summary) {
        const content = props.article.content;
        // 移除Markdown标记并截取
        const plainText = content.replace(/[#*`_\[\]()]/g, '');
        return plainText.length > 120 ? plainText.substring(0, 120) + '...' : plainText;
      }
      return '';
    });
    
    // 获取类别名称
    const categoryName = computed(() => {
      const { category, categoryInfo, categoryName: articleCategoryName } = props.article;
      if (articleCategoryName) return articleCategoryName;
      if (categoryInfo?.name) return categoryInfo.name;
      if (typeof category === 'string' && category.trim()) return category.trim();
      
      // 如果类别是对象并且有name属性
      if (category && typeof category === 'object' && category.name) {
        return category.name;
      }
      
      // 如果类别是数字索引
      if (typeof category === 'number') {
        const categories = store.state.categories;
        if (categories && categories.length > category) {
          return categories[category].name;
        }
        
        // 使用默认类别名称
        const defaultCategories = ['前端开发', '后端开发', '移动开发', '人工智能', '开发工具'];
        return defaultCategories[category % defaultCategories.length] || '未分类';
      }
      
      return '未分类';
    });
    
    // 获取评论数量
    const getCommentsCount = computed(() => {
      if (!props.article.comments) return 0;
      
      if (Array.isArray(props.article.comments)) {
        return props.article.comments.length;
      }
      return props.article.comments || 0;
    });
    
    // 格式化时间
    const formatTime = (time) => formatFriendlyTime(time);
    
    // 获取作者名称
    const getAuthorName = (author) => {
      if (!author) return '匿名';
      // 优先使用 nickname，然后是 username，最后是 name
      return author.nickname || author.username || author.name || '匿名';
    };

    // 获取作者首字母
    const getAuthorInitials = (name) => {
      if (!name) return '匿';
      return name.charAt(0).toUpperCase();
    };
    
    // 确保头像路径正确
    const getAvatarPath = (avatar) => {
      if (!avatar) return '';
      if (avatar.startsWith('/images/')) return avatar;
      if (avatar.startsWith('/avatar')) {
        return `/images/avatars${avatar}`;
      }
      return avatar;
    };
    
    return {
      formatTime,
      getAuthorName,
      getAuthorInitials,
      getCommentsCount,
      categoryName,
      truncateContent,
      getAvatarPath
    };
  }
});
</script>

<style scoped>
.article-card {
  display: block;
  margin-bottom: 1.5rem;
  overflow: hidden;
  transition: var(--transition);
  color: var(--text-color);
  text-decoration: none;
}

.article-card-inner {
  display: flex;
  flex-direction: row;
  height: 100%;
  background-color: var(--bg-white);
  border-radius: var(--radius);
  overflow: hidden;
  box-shadow: var(--shadow-sm);
  border: 1px solid var(--border-color);
  transition: var(--transition);
}

.article-card:hover .article-card-inner {
  box-shadow: var(--shadow);
  transform: translateY(-4px);
  border-color: var(--primary-light);
}

.article-cover {
  width: 250px;
  height: auto;
  flex-shrink: 0;
  overflow: hidden;
  position: relative;
}

.article-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.5s ease;
}

.article-card:hover .article-cover img {
  transform: scale(1.05);
}

.article-content {
  padding: 1.25rem;
  display: flex;
  flex-direction: column;
  flex: 1;
}

.article-meta {
  display: flex;
  align-items: center;
  gap: 1rem;
  margin-bottom: 0.75rem;
  flex-wrap: wrap;
}

.category-tag, .time {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  color: var(--text-light);
  font-size: 0.8rem;
}

.article-title {
  font-size: 1.25rem;
  font-weight: 600;
  margin: 0 0 0.75rem 0;
  line-height: 1.3;
  color: var(--text-color);
}

.article-summary {
  color: var(--text-light);
  font-size: 0.95rem;
  line-height: 1.6;
  margin: 0 0 1rem 0;
}

.article-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
  margin-bottom: 1.25rem;
  align-items: center;
}

.tags-label {
  font-size: 0.8rem;
  color: var(--text-light);
}

.tag {
  display: inline-flex;
  align-items: center;
  gap: 0.25rem;
  background-color: var(--primary-light);
  color: var(--primary-color);
  padding: 0.25rem 0.5rem;
  border-radius: 50px;
  font-size: 0.75rem;
  transition: var(--transition);
}

.tag:hover {
  background-color: var(--primary-color);
  color: white;
}

.article-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: auto;
  padding-top: 1rem;
  border-top: 1px solid var(--border-color);
}

.author {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  color: var(--text-color);
  cursor: pointer;
  transition: var(--transition);
}

.author:hover {
  color: var(--primary-color);
}

.author:hover .author-avatar {
  border-color: var(--primary-color);
  transform: scale(1.05);
}

.author-avatar {
  width: 2rem;
  height: 2rem;
  border-radius: 50%;
  overflow: hidden;
  background-color: var(--primary-color);
  display: flex;
  justify-content: center;
  align-items: center;
  color: white;
  font-weight: 600;
  transition: var(--transition);
  border: 2px solid transparent;
}

.author-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.author-name {
  font-size: 0.9rem;
  color: var(--text-color);
  font-weight: 500;
}

.article-stats {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 0.25rem;
  color: var(--text-lighter);
  font-size: 0.8rem;
}

/* Featured article style (can be applied with a class) */
.article-card.featured .article-card-inner {
  border-left: 3px solid var(--primary-color);
}

/* Responsive design */
@media (max-width: 768px) {
  .article-card-inner {
    flex-direction: column;
  }
  
  .article-cover {
    width: 100%;
    height: 180px;
  }
}

@media (max-width: 576px) {
  .article-footer {
    flex-direction: column;
    align-items: flex-start;
    gap: 0.75rem;
  }
  
  .article-stats {
    width: 100%;
    justify-content: flex-start;
  }
}
</style> 
