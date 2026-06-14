<template>
  <div class="comment-item" :class="{ 'is-highlighted': highlighted }">
    <div class="comment-author">
      <div class="author-avatar" @click="navigateToUser">
        <img v-if="comment.author?.avatarUrl || comment.author?.avatar" :src="comment.author.avatarUrl || comment.author.avatar" :alt="getAuthorName(comment.author)" />
        <div v-else class="avatar-initials">{{ getAuthorInitials(getAuthorName(comment.author)) }}</div>
      </div>
      
      <div class="comment-content">
        <div class="comment-header">
          <div class="author-info">
            <router-link
              v-if="!noLink && comment.author?.id"
              :to="`/user/${comment.author.id}`"
              class="author-name"
            >
              {{ getAuthorName(comment.author) || '匿名用户' }}
            </router-link>
            <span v-else class="author-name">{{ getAuthorName(comment.author) || '匿名用户' }}</span>
            
            <span v-if="comment.author?.role === 'admin'" class="role-badge admin">管理员</span>
            <span v-if="comment.author?.role === 'moderator'" class="role-badge moderator">版主</span>
            <span v-if="comment.isAuthor" class="role-badge author">作者</span>
          </div>
          
          <div class="comment-meta">
            <div class="comment-time" :title="formatFullDate(comment.createdAt || comment.createTime)">
              <font-awesome-icon :icon="['fas', 'clock']" />
              <span>{{ formatTime(comment.createdAt || comment.createTime) }}</span>
            </div>
            
            <div v-if="showMenu" class="comment-menu">
              <button class="menu-btn" @click="toggleMenu">
                <font-awesome-icon :icon="['fas', 'ellipsis-h']" />
              </button>
              <div v-if="isMenuOpen" class="menu-dropdown">
                <button v-if="canEdit" class="menu-item" @click="handleEdit">
                  <font-awesome-icon :icon="['fas', 'edit']" />
                  <span>编辑</span>
                </button>
                <button v-if="canDelete" class="menu-item delete" @click="handleDelete">
                  <font-awesome-icon :icon="['fas', 'trash-alt']" />
                  <span>删除</span>
                </button>
                <button class="menu-item" @click="handleReport">
                  <font-awesome-icon :icon="['fas', 'flag']" />
                  <span>举报</span>
                </button>
              </div>
            </div>
          </div>
        </div>
        
        <div class="comment-body">
          <div v-if="isEditing" class="edit-form">
            <textarea v-model="editText" class="edit-textarea"></textarea>
            <div class="edit-actions">
              <button class="cancel-btn" @click="cancelEdit">取消</button>
              <button class="save-btn" @click="saveEdit">保存</button>
            </div>
          </div>
          <div v-else class="comment-text" v-html="formattedContent"></div>
        </div>
        
        <div class="comment-footer">
          <div class="comment-actions">
            <button 
              class="action-btn like-btn" 
              :class="{ 'is-active': comment.userLiked }"
              @click="handleLike"
            >
              <font-awesome-icon :icon="['fas', 'thumbs-up']" />
              <span>{{ comment.likes || 0 }}</span>
            </button>
            
            <button 
              v-if="showReply" 
              class="action-btn reply-btn"
              @click="handleReply"
            >
              <font-awesome-icon :icon="['fas', 'reply']" />
              <span>回复</span>
            </button>
          </div>
          
          <div v-if="comment.source" class="comment-source">
            <router-link :to="comment.source.url" class="source-link">
              <font-awesome-icon :icon="['fas', 'link']" />
              <span>查看原{{ comment.source.type === 'article' ? '文章' : '问题' }}</span>
            </router-link>
          </div>
        </div>
        
        <!-- 回复框 -->
        <div v-if="isReplying" class="reply-form">
          <textarea 
            v-model="replyText" 
            placeholder="写下你的回复..." 
            class="reply-textarea"
          ></textarea>
          <div class="reply-actions">
            <button class="cancel-btn" @click="cancelReply">取消</button>
            <button 
              class="submit-btn" 
              :disabled="!replyText.trim()" 
              @click="submitReply"
            >
              回复
            </button>
          </div>
        </div>
      </div>
    </div>
    
    <!-- 嵌套回复 -->
    <div v-if="comment.replies && comment.replies.length > 0" class="comment-replies">
      <div 
        v-for="reply in comment.replies" 
        :key="reply.id" 
        class="reply-item"
      >
        <comment-item 
          :comment="reply" 
          :show-reply="false" 
          :is-nested="true"
          @like="$emit('like', $event)"
          @reply="$emit('reply', $event)"
          @edit="$emit('edit', $event)"
          @delete="$emit('delete', $event)"
          @report="$emit('report', $event)"
        />
      </div>
    </div>
  </div>
</template>

<script>
import { defineComponent, ref, computed } from 'vue';
import { useStore } from 'vuex';
import { marked } from 'marked';
import { formatDateTime, formatFriendlyTime } from '../utils/dateUtils';

export default defineComponent({
  name: 'CommentItem',
  components: {
    CommentItem: () => import('./CommentItem.vue')
  },
  props: {
    comment: {
      type: Object,
      required: true
    },
    highlighted: {
      type: Boolean,
      default: false
    },
    noLink: {
      type: Boolean,
      default: false
    },
    showReply: {
      type: Boolean,
      default: true
    },
    isNested: {
      type: Boolean,
      default: false
    }
  },
  setup(props, { emit }) {
    const store = useStore();
    const isMenuOpen = ref(false);
    const isEditing = ref(false);
    const isReplying = ref(false);
    const editText = ref('');
    const replyText = ref('');
    
    // 当前用户
    const currentUser = computed(() => store.state.user);
    
    // 是否显示菜单
    const showMenu = computed(() => {
      return canEdit.value || canDelete.value || true; // 举报总是可用
    });
    
    // 是否可以编辑评论
    const canEdit = computed(() => {
      if (!currentUser.value) return false;
      
      // 当前用户是评论作者
      const isAuthor = currentUser.value.id === props.comment.author?.id;
      
      // 当前用户是管理员
      const isAdmin = currentUser.value.role === 'admin';
      
      // 当前用户是版主
      const isModerator = currentUser.value.role === 'moderator';
      
      return isAuthor || isAdmin || isModerator;
    });
    
    // 是否可以删除评论
    const canDelete = computed(() => {
      if (!currentUser.value) return false;
      
      // 当前用户是评论作者
      const isAuthor = currentUser.value.id === props.comment.author?.id;
      
      // 当前用户是管理员
      const isAdmin = currentUser.value.role === 'admin';
      
      // 当前用户是版主
      const isModerator = currentUser.value.role === 'moderator';
      
      return isAuthor || isAdmin || isModerator;
    });
    
    // 格式化评论内容，支持简单的 Markdown
    const formattedContent = computed(() => {
      if (!props.comment.content) return '';
      
      try {
        // 使用 marked 解析 Markdown
        return marked.parse(props.comment.content);
      } catch (error) {
        console.error('Error parsing markdown:', error);
        return props.comment.content;
      }
    });
    
    // 获取作者名称
    const getAuthorName = (author) => {
      if (!author) return '匿名用户';
      // 优先使用 nickname，然后是 username，最后是 name
      return author.nickname || author.username || author.name || '匿名用户';
    };

    // 获取作者首字母
    const getAuthorInitials = (name) => {
      if (!name) return '?';
      return name.charAt(0).toUpperCase();
    };
    
    // 格式化时间为相对时间（如"3小时前"）
    const formatTime = (time) => {
      return formatFriendlyTime(time);
    };
    
    // 格式化为完整日期时间
    const formatFullDate = (time) => {
      return formatDateTime(time);
    };
    
    // 切换菜单显示状态
    const toggleMenu = () => {
      isMenuOpen.value = !isMenuOpen.value;
    };
    
    // 导航到用户主页
    const navigateToUser = () => {
      if (props.comment.author?.id && !props.noLink) {
        emit('navigate-to-user', props.comment.author);
      }
    };
    
    // 点赞评论
    const handleLike = () => {
      emit('like', props.comment);
    };
    
    // 回复评论
    const handleReply = () => {
      isReplying.value = true;
    };
    
    // 提交回复
    const submitReply = () => {
      if (!replyText.value.trim()) return;
      
      emit('reply', {
        parentId: props.comment.id,
        content: replyText.value.trim()
      });
      
      // 重置状态
      replyText.value = '';
      isReplying.value = false;
    };
    
    // 取消回复
    const cancelReply = () => {
      replyText.value = '';
      isReplying.value = false;
    };
    
    // 编辑评论
    const handleEdit = () => {
      editText.value = props.comment.content;
      isEditing.value = true;
      isMenuOpen.value = false;
    };
    
    // 保存编辑
    const saveEdit = () => {
      if (!editText.value.trim()) return;
      
      emit('edit', {
        id: props.comment.id,
        content: editText.value.trim()
      });
      
      // 重置状态
      isEditing.value = false;
    };
    
    // 取消编辑
    const cancelEdit = () => {
      editText.value = '';
      isEditing.value = false;
    };
    
    // 删除评论
    const handleDelete = () => {
      if (confirm('确定要删除这条评论吗？此操作不可撤销。')) {
        emit('delete', props.comment);
      }
      isMenuOpen.value = false;
    };
    
    // 举报评论
    const handleReport = () => {
      emit('report', props.comment);
      isMenuOpen.value = false;
    };
    
    return {
      isMenuOpen,
      isEditing,
      isReplying,
      editText,
      replyText,
      showMenu,
      canEdit,
      canDelete,
      formattedContent,
      getAuthorName,
      getAuthorInitials,
      formatTime,
      formatFullDate,
      toggleMenu,
      navigateToUser,
      handleLike,
      handleReply,
      submitReply,
      cancelReply,
      handleEdit,
      saveEdit,
      cancelEdit,
      handleDelete,
      handleReport
    };
  }
});
</script>

<style scoped>
.comment-item {
  padding: 1.5rem;
  border-bottom: 1px solid var(--border-color);
  transition: var(--transition);
}

.comment-item.is-highlighted {
  background-color: rgba(var(--primary-rgb), 0.05);
}

.comment-item:last-child {
  border-bottom: none;
}

.comment-author {
  display: flex;
  gap: 1rem;
}

.author-avatar {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  overflow: hidden;
  flex-shrink: 0;
  cursor: pointer;
  background-color: var(--primary-lighter);
  display: flex;
  align-items: center;
  justify-content: center;
}

.author-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-initials {
  font-size: 1.25rem;
  font-weight: 600;
  color: var(--primary-color);
}

.comment-content {
  flex: 1;
  min-width: 0;
}

.comment-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 0.75rem;
}

.author-info {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  flex-wrap: wrap;
}

.author-name {
  font-weight: 600;
  color: var(--text-color);
  text-decoration: none;
  transition: var(--transition);
}

.author-name:hover {
  color: var(--primary-color);
}

.role-badge {
  display: inline-flex;
  align-items: center;
  padding: 0.15rem 0.5rem;
  border-radius: 50px;
  font-size: 0.7rem;
  font-weight: 500;
}

.role-badge.admin {
  background-color: rgba(var(--primary-rgb), 0.1);
  color: var(--primary-color);
}

.role-badge.moderator {
  background-color: rgba(var(--success-rgb), 0.1);
  color: var(--success-color);
}

.role-badge.author {
  background-color: rgba(var(--warning-rgb), 0.1);
  color: var(--warning-color);
}

.comment-meta {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.comment-time {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  color: var(--text-lighter);
  font-size: 0.8rem;
}

.comment-menu {
  position: relative;
}

.menu-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: none;
  border: none;
  color: var(--text-lighter);
  cursor: pointer;
  transition: var(--transition);
}

.menu-btn:hover {
  background-color: var(--bg-gray);
  color: var(--text-color);
}

.menu-dropdown {
  position: absolute;
  top: 100%;
  right: 0;
  background-color: var(--bg-white);
  border-radius: var(--radius);
  box-shadow: var(--shadow);
  width: 120px;
  z-index: 10;
  overflow: hidden;
  border: 1px solid var(--border-color);
}

.menu-item {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  width: 100%;
  padding: 0.75rem 1rem;
  text-align: left;
  background: none;
  border: none;
  cursor: pointer;
  transition: var(--transition);
  color: var(--text-color);
}

.menu-item:hover {
  background-color: var(--bg-gray);
}

.menu-item.delete {
  color: var(--error-color);
}

.menu-item.delete:hover {
  background-color: rgba(var(--error-rgb), 0.05);
}

.comment-body {
  margin-bottom: 1rem;
}

.comment-text {
  color: var(--text-color);
  line-height: 1.6;
  overflow-wrap: break-word;
  word-break: break-word;
}

.comment-text p:first-child {
  margin-top: 0;
}

.comment-text p:last-child {
  margin-bottom: 0;
}

.edit-form, .reply-form {
  margin-top: 1rem;
}

.edit-textarea, .reply-textarea {
  width: 100%;
  min-height: 100px;
  padding: 0.75rem;
  border: 1px solid var(--border-color);
  border-radius: var(--radius);
  margin-bottom: 0.75rem;
  font-family: inherit;
  resize: vertical;
  transition: var(--transition);
}

.edit-textarea:focus, .reply-textarea:focus {
  outline: none;
  border-color: var(--primary-color);
  box-shadow: 0 0 0 2px rgba(var(--primary-rgb), 0.1);
}

.edit-actions, .reply-actions {
  display: flex;
  justify-content: flex-end;
  gap: 0.75rem;
}

.cancel-btn {
  padding: 0.5rem 1rem;
  border: 1px solid var(--border-color);
  border-radius: var(--radius);
  background-color: var(--bg-white);
  color: var(--text-color);
  cursor: pointer;
  transition: var(--transition);
}

.cancel-btn:hover {
  background-color: var(--bg-gray);
}

.save-btn, .submit-btn {
  padding: 0.5rem 1rem;
  border: none;
  border-radius: var(--radius);
  background-color: var(--primary-color);
  color: white;
  cursor: pointer;
  transition: var(--transition);
}

.save-btn:hover, .submit-btn:hover {
  background-color: var(--primary-dark);
}

.submit-btn:disabled {
  background-color: var(--text-lighter);
  cursor: not-allowed;
}

.comment-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.comment-actions {
  display: flex;
  gap: 1rem;
}

.action-btn {
  display: flex;
  align-items: center;
  gap: 0.35rem;
  background: none;
  border: none;
  color: var(--text-lighter);
  font-size: 0.9rem;
  cursor: pointer;
  padding: 0.25rem 0.5rem;
  border-radius: var(--radius-sm);
  transition: var(--transition);
}

.action-btn:hover {
  background-color: var(--bg-gray);
  color: var(--text-color);
}

.like-btn.is-active {
  color: var(--primary-color);
}

.comment-source {
  font-size: 0.85rem;
}

.source-link {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  color: var(--primary-color);
  text-decoration: none;
  transition: var(--transition);
}

.source-link:hover {
  text-decoration: underline;
}

.comment-replies {
  margin-top: 1rem;
  margin-left: 3.5rem;
  border-left: 2px solid var(--border-color);
  padding-left: 1rem;
}

.reply-item {
  margin-bottom: 0.5rem;
}

.reply-item:last-child {
  margin-bottom: 0;
}

.reply-item .comment-item {
  padding: 1rem;
  border-radius: var(--radius);
  background-color: var(--bg-gray);
  border: none;
}

@media (max-width: 768px) {
  .comment-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 0.5rem;
  }
  
  .comment-meta {
    width: 100%;
    justify-content: space-between;
  }
  
  .comment-footer {
    flex-direction: column;
    align-items: flex-start;
    gap: 0.75rem;
  }
  
  .comment-source {
    width: 100%;
  }
  
  .comment-replies {
    margin-left: 1.5rem;
  }
}

@media (max-width: 576px) {
  .comment-item {
    padding: 1rem;
  }
  
  .author-avatar {
    width: 40px;
    height: 40px;
  }
  
  .avatar-initials {
    font-size: 1rem;
  }
  
  .comment-actions {
    width: 100%;
    justify-content: space-between;
  }
}
</style> 
