<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { marked } from 'marked'
import hljs from 'highlight.js'
import { Editor } from '@toast-ui/editor'
import '@toast-ui/editor/dist/toastui-editor.css'
import { articleService } from '../services/articleService'
import { useStore } from 'vuex'

// 配置 marked
marked.setOptions({
  highlight: function(code, lang) {
    if (lang && hljs.getLanguage(lang)) {
      return hljs.highlight(code, { language: lang }).value
    }
    return hljs.highlightAuto(code).value
  },
  breaks: true,
  gfm: true,
  pedantic: false,
  sanitize: false,
  smartLists: true,
  smartypants: true
})

const route = useRoute()
const router = useRouter()
const editor = ref(null)
const editorEl = ref(null)
const isEditMode = computed(() => route.params.id !== undefined)
const isSaving = ref(false)
const saveError = ref('')
const store = useStore()

// 文章数据
const article = ref({
  id: null,
  title: '',
  content: '',
  category: '',
  subcategory: '',
  tags: [],
  coverImage: '',
  isPublished: false,
  isDraft: true,
  summary: ''
})

// 编辑器配置
const editorConfig = ref({
  mode: 'markdown',
  fullscreen: false
})

// 分类和标签数据
const categories = ref([
  {
    id: 1,
    name: '前端开发',
    subcategories: [
      { id: 11, name: 'Vue.js' },
      { id: 12, name: 'React' },
      { id: 13, name: 'Angular' },
      { id: 14, name: '原生JavaScript' }
    ]
  },
  {
    id: 2,
    name: '后端开发',
    subcategories: [
      { id: 21, name: 'Node.js' },
      { id: 22, name: 'Python' },
      { id: 23, name: 'Java' },
      { id: 24, name: 'Go' }
    ]
  }
])

const popularTags = ref([
  { id: 1, name: 'JavaScript' },
  { id: 2, name: 'Vue3' },
  { id: 3, name: 'React' },
  { id: 4, name: 'TypeScript' },
  { id: 5, name: 'Node.js' },
  { id: 6, name: 'Python' },
  { id: 7, name: 'Java' },
  { id: 8, name: 'Go' }
])

// 计算属性
const renderedContent = computed(() => {
  return marked(article.value.content)
})

const selectedCategory = computed(() => {
  return categories.value.find(c => c.id === parseInt(article.value.category))
})

const subcategories = computed(() => {
  return selectedCategory.value?.subcategories || []
})

const wordCount = computed(() => {
  return article.value.content.length
})

// 获取路由参数中的文章ID
const articleId = computed(() => route.params.id);

// 表单验证
const validateForm = () => {
  if (!article.value.title || article.value.title.trim() === '') {
    saveError.value = '请输入文章标题';
    return false;
  }
  
  if (!article.value.content || article.value.content.trim() === '') {
    saveError.value = '请输入文章内容';
    return false;
  }
  
  if (!article.value.category) {
    saveError.value = '请选择文章分类';
    return false;
  }
  
  return true;
};

// 成功消息
const successMessage = ref('');
const showSuccessMessage = (message) => {
  successMessage.value = message;
  // 3秒后清除消息
  setTimeout(() => {
    successMessage.value = '';
  }, 3000);
};

// 方法
const togglePreview = () => {
  editorConfig.value.mode = editorConfig.value.mode === 'markdown' ? 'preview' : 'markdown'
}

const toggleFullscreen = () => {
  editorConfig.value.fullscreen = !editorConfig.value.fullscreen
}

const addTag = (tag) => {
  if (!article.value.tags.includes(tag)) {
    article.value.tags.push(tag)
  }
}

const removeTag = (tag) => {
  const index = article.value.tags.indexOf(tag)
  if (index > -1) {
    article.value.tags.splice(index, 1)
  }
}

const uploadCover = async (event) => {
  const file = event.target.files[0]
  if (file) {
    try {
      console.log('开始上传封面图片:', file)

      // 验证文件类型
      if (!file.type.startsWith('image/')) {
        saveError.value = '请选择图片文件'
        return
      }

      // 验证文件大小 (5MB)
      if (file.size > 5 * 1024 * 1024) {
        saveError.value = '图片文件大小不能超过5MB'
        return
      }

      const url = await articleService.uploadImage(file)
      article.value.coverImage = url
      console.log('封面图片上传成功:', url)

      // 显示成功消息
      showSuccessMessage('封面图片上传成功！')
    } catch (error) {
      console.error('封面图片上传失败:', error)
      saveError.value = `封面图片上传失败: ${error.message}`
    }
  }
}

// 初始化编辑器
onMounted(async () => {
  await nextTick()
  
  editor.value = new Editor({
    el: editorEl.value,
    height: '600px',
    initialEditType: 'markdown',
    previewStyle: 'vertical',
    hooks: {
      addImageBlobHook: async (blob, callback) => {
        try {
          const url = await articleService.uploadImage(blob)
          callback(url)
        } catch (error) {
          console.error('图片上传失败:', error)
          callback('图片上传失败')
        }
      }
    },
    toolbarItems: [
      ['heading', 'bold', 'italic', 'strike'],
      ['hr', 'quote'],
      ['ul', 'ol', 'task', 'indent', 'outdent'],
      ['table', 'image', 'link'],
      ['code', 'codeblock'],
      ['scrollSync']
    ],
    customHTMLRenderer: {
      codeBlock: (node) => {
        const html = hljs.highlightAuto(node.literal).value
        return {
          html: `<pre><code>${html}</code></pre>`,
          type: 'html'
        }
      }
    }
  })

  // 如果是编辑模式，加载文章数据
  if (isEditMode.value) {
    await loadArticle(route.params.id)
  }

  // 监听内容变化
  editor.value.on('change', () => {
    article.value.content = editor.value.getMarkdown()
  })
})

// 加载文章数据
const loadArticle = async (id) => {
  try {
    const data = await articleService.getArticle(id)
    article.value = data
    editor.value.setMarkdown(data.content)
  } catch (error) {
    console.error('加载文章失败:', error)
    alert('加载文章失败')
  }
}

// 保存文章
const saveArticle = async () => {
  if (!validateForm()) return;
  
  try {
    isSaving.value = true;
    saveError.value = '';
    
    // 获取编辑器的最新内容
    article.value.content = editor.value.getMarkdown();
    
    // 构建文章对象，确保符合后端API要求
    const articleData = {
      title: article.value.title?.trim() || '',
      content: article.value.content?.trim() || '',
      summary: article.value.summary?.trim() || article.value.title?.trim() || '', // 如果没有摘要，使用标题
      categoryId: article.value.category ? parseInt(article.value.category) : 1, // 确保是数字
      tags: Array.isArray(article.value.tags) ? article.value.tags.filter(tag => tag.trim()) : [],
      coverImage: article.value.coverImage || null,
      isDraft: Boolean(article.value.isDraft) // 确保是布尔值
    };

    // 验证必填字段
    if (!articleData.title) {
      throw new Error('文章标题不能为空');
    }
    if (!articleData.content) {
      throw new Error('文章内容不能为空');
    }
    if (articleData.title.length > 200) {
      throw new Error('文章标题长度不能超过200个字符');
    }
    if (articleData.summary && articleData.summary.length > 500) {
      throw new Error('文章摘要长度不能超过500个字符');
    }
    
    let savedArticle;
    
    if (isEditMode.value && articleId.value) {
      // 更新现有文章
      savedArticle = await store.dispatch('updateArticle', { 
        id: articleId.value, 
        article: articleData 
      });
      console.log('Article updated successfully:', savedArticle);
    } else {
      // 创建新文章
      savedArticle = await store.dispatch('createArticle', articleData);
      console.log('New article created successfully:', savedArticle);
    }
    
    // 显示成功消息
    showSuccessMessage(isEditMode.value ? '文章已更新成功!' : '文章已发布成功!');
    
    // 导航到文章详情页
    setTimeout(() => {
      router.push({
        name: 'ArticleDetail',
        params: { id: savedArticle.id }
      });
    }, 1500);
  } catch (error) {
    console.error('Error saving article:', error);
    saveError.value = error.message || '保存文章时出错，请稍后重试';
  } finally {
    isSaving.value = false;
  }
};

// 保存为草稿
const saveAsDraft = async () => {
  try {
    article.value.isDraft = true
    await saveArticle()
    showSuccessMessage('草稿保存成功！')
  } catch (error) {
    saveError.value = `保存草稿失败: ${error.message}`
  }
}

// 发布文章
const publish = async () => {
  try {
    // 验证必填字段
    if (!article.value.title?.trim()) {
      saveError.value = '请输入文章标题'
      return
    }

    if (!article.value.content?.trim()) {
      saveError.value = '请输入文章内容'
      return
    }

    if (!article.value.category) {
      saveError.value = '请选择文章分类'
      return
    }

    article.value.isDraft = false
    await saveArticle()
    showSuccessMessage('文章发布成功！')

    // 发布成功后可以跳转到文章列表或详情页
    // router.push('/articles')
  } catch (error) {
    saveError.value = `发布文章失败: ${error.message}`
  }
}

// 图片拖拽上传处理
const handleDrop = async (e) => {
  e.preventDefault()
  
  const files = Array.from(e.dataTransfer.files)
  const imageFiles = files.filter(file => file.type.startsWith('image/'))
  
  for (const file of imageFiles) {
    try {
      const url = await articleService.uploadImage(file)
      const imageMarkdown = `![${file.name}](${url})\n`
      editor.value.insertText(imageMarkdown)
    } catch (error) {
      console.error('图片上传失败:', error)
    }
  }
}

// 组件销毁时清理编辑器
onUnmounted(() => {
  if (editor.value) {
    editor.value.destroy()
  }
})
</script>

<template>
  <div 
    class="article-editor"
    :class="{ 'fullscreen': editorConfig.fullscreen }"
    @drop="handleDrop"
    @dragover.prevent
  >
    <!-- 标题输入 -->
    <div class="editor-header">
      <div class="header-left">
        <h1 class="editor-title">{{ isEditMode ? '编辑文章' : '创建文章' }}</h1>
        <input
          v-model="article.title"
          type="text"
          placeholder="请输入文章标题..."
          class="title-input"
        />
      </div>

      <div class="header-right">
        <!-- 操作按钮 -->
        <div class="action-buttons">
          <button
            @click="saveAsDraft"
            :disabled="isSaving"
            class="btn btn-secondary"
          >
            <font-awesome-icon :icon="['fas', 'save']" />
            {{ isSaving ? '保存中...' : '保存草稿' }}
          </button>

          <button
            @click="publish"
            :disabled="isSaving"
            class="btn btn-primary"
          >
            <font-awesome-icon :icon="['fas', 'paper-plane']" />
            {{ isSaving ? '发布中...' : (isEditMode ? '更新文章' : '发布文章') }}
          </button>
        </div>
      </div>

      <!-- 成功消息 -->
      <div v-if="successMessage" class="success-message">
        <font-awesome-icon :icon="['fas', 'check-circle']" />
        <span>{{ successMessage }}</span>
      </div>

      <!-- 错误消息 -->
      <div v-if="saveError" class="error-message">
        <font-awesome-icon :icon="['fas', 'exclamation-circle']" />
        <span>{{ saveError }}</span>
      </div>
    </div>

    <!-- 编辑器主体 -->
    <div class="editor-main">
      <div class="editor-container">
        <div ref="editorEl"></div>
      </div>

      <!-- 侧边栏 -->
      <div class="editor-sidebar">
        <!-- 封面图片 -->
        <div class="cover-upload">
          <h3>文章封面</h3>
          <div 
            class="cover-preview"
            :class="{ 'has-image': article.coverImage }"
            @click="$refs.coverInput.click()"
          >
            <img v-if="article.coverImage" :src="article.coverImage" alt="封面图片">
            <div v-else class="upload-placeholder">
              <font-awesome-icon :icon="['fas', 'image']" />
              <span>点击上传封面图片</span>
            </div>
          </div>
          <input
            ref="coverInput"
            type="file"
            accept="image/*"
            class="cover-input"
            @change="uploadCover"
          >
        </div>

        <!-- 分类选择 -->
        <div class="category-select">
          <h3>文章分类</h3>
          <select v-model="article.category" required>
            <option value="">选择分类</option>
            <option 
              v-for="category in categories"
              :key="category.id"
              :value="category.id"
            >
              {{ category.name }}
            </option>
          </select>
          <select 
            v-if="selectedCategory"
            v-model="article.subcategory"
          >
            <option value="">选择子分类</option>
            <option
              v-for="sub in subcategories"
              :key="sub.id"
              :value="sub.id"
            >
              {{ sub.name }}
            </option>
          </select>
        </div>

        <!-- 标签选择 -->
        <div class="tag-select">
          <h3>文章标签</h3>
          <div class="selected-tags">
            <span 
              v-for="tag in article.tags"
              :key="tag"
              class="tag"
            >
              {{ tag }}
              <font-awesome-icon 
                :icon="['fas', 'times']"
                @click="removeTag(tag)"
              />
            </span>
          </div>
          <div class="popular-tags">
            <span>热门标签：</span>
            <span 
              v-for="tag in popularTags"
              :key="tag.id"
              class="tag-suggestion"
              @click="addTag(tag.name)"
            >
              {{ tag.name }}
            </span>
          </div>
        </div>

        <!-- 文章摘要 -->
        <div class="summary-input">
          <h3>文章摘要</h3>
          <textarea
            v-model="article.summary"
            placeholder="输入文章摘要（可选）..."
            rows="4"
          ></textarea>
          <p class="hint">如果不填写，将自动提取文章前200个字符作为摘要</p>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.article-editor {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: white;
}

.editor-header {
  padding: 20px;
  border-bottom: 1px solid #eee;
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  background: white;
  flex-wrap: wrap;
  gap: 20px;
}

.header-left {
  flex: 1;
  min-width: 300px;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 10px;
}

.editor-title {
  font-size: 1.5em;
  margin: 0 0 10px 0;
  color: #333;
}

.title-input {
  width: 100%;
  font-size: 1.2em;
  padding: 12px;
  border: 2px solid #e1e5e9;
  border-radius: 6px;
  outline: none;
  transition: border-color 0.3s;
}

.title-input:focus {
  border-color: #1890ff;
}

.action-buttons {
  display: flex;
  gap: 10px;
}

.btn {
  padding: 10px 20px;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  display: flex;
  align-items: center;
  gap: 8px;
  transition: all 0.3s;
  min-width: 120px;
  justify-content: center;
}

.btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.btn-primary {
  background: #1890ff;
  color: white;
}

.btn-primary:hover:not(:disabled) {
  background: #40a9ff;
  transform: translateY(-1px);
}

.btn-secondary {
  background: #f5f5f5;
  color: #666;
  border: 1px solid #d9d9d9;
}

.btn-secondary:hover:not(:disabled) {
  background: #e6f7ff;
  border-color: #1890ff;
  color: #1890ff;
}

.success-message {
  position: fixed;
  top: 20px;
  right: 20px;
  background: #52c41a;
  color: white;
  padding: 12px 20px;
  border-radius: 6px;
  display: flex;
  align-items: center;
  gap: 8px;
  z-index: 1000;
  animation: slideIn 0.3s ease;
}

.error-message {
  position: fixed;
  top: 20px;
  right: 20px;
  background: #ff4d4f;
  color: white;
  padding: 12px 20px;
  border-radius: 6px;
  display: flex;
  align-items: center;
  gap: 8px;
  z-index: 1000;
  animation: slideIn 0.3s ease;
}

@keyframes slideIn {
  from {
    transform: translateX(100%);
    opacity: 0;
  }
  to {
    transform: translateX(0);
    opacity: 1;
  }
}

.success-message,
.error-message {
  padding: 0.75rem 1rem;
  border-radius: var(--radius);
  margin-bottom: 1rem;
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.success-message {
  background-color: rgba(var(--success-rgb), 0.1);
  border: 1px solid var(--success-color);
  color: var(--success-color);
}

.error-message {
  background-color: rgba(var(--error-rgb), 0.1);
  border: 1px solid var(--error-color);
  color: var(--error-color);
}

.editor-main {
  flex: 1;
  display: flex;
  overflow: hidden;
}

.editor-container {
  flex: 1;
  overflow: hidden;
}

.editor-sidebar {
  width: 300px;
  border-left: 1px solid #eee;
  padding: 20px;
  overflow-y: auto;
}

.cover-upload {
  margin-bottom: 20px;
}

.cover-upload h3 {
  margin-bottom: 10px;
  font-size: 1em;
  color: #333;
}

.cover-preview {
  width: 100%;
  height: 150px;
  border: 2px dashed #ddd;
  border-radius: 8px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.3s;
}

.cover-preview:hover {
  border-color: #1890ff;
}

.cover-preview.has-image {
  border-style: solid;
}

.cover-preview img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.upload-placeholder {
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #999;
}

.cover-input {
  display: none;
}

.category-select,
.tag-select {
  margin-bottom: 20px;
}

.category-select h3,
.tag-select h3 {
  margin-bottom: 10px;
  font-size: 1em;
  color: #333;
}

.category-select select {
  width: 100%;
  padding: 8px;
  border: 1px solid #ddd;
  border-radius: 4px;
  margin-bottom: 10px;
}

.selected-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 10px;
}

.tag {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 4px 8px;
  background: #f5f5f5;
  border-radius: 16px;
  font-size: 0.9em;
  color: #666;
}

.tag i {
  cursor: pointer;
}

.tag i:hover {
  color: #ff4d4f;
}

.popular-tags {
  font-size: 0.9em;
  color: #666;
}

.tag-suggestion {
  display: inline-block;
  margin: 4px;
  padding: 2px 8px;
  background: #f5f5f5;
  border-radius: 12px;
  cursor: pointer;
}

.tag-suggestion:hover {
  background: #e6f7ff;
  color: #1890ff;
}

.summary-input {
  margin-top: 20px;
}

.summary-input textarea {
  width: 100%;
  padding: 8px;
  border: 1px solid #ddd;
  border-radius: 4px;
  resize: vertical;
  margin-top: 10px;
}

.hint {
  font-size: 0.9em;
  color: #999;
  margin-top: 5px;
}

/* Toast UI Editor 自定义样式 */
:deep(.toastui-editor-defaultUI) {
  border: none;
  border-radius: 4px;
}

:deep(.toastui-editor-toolbar) {
  border-bottom: 1px solid #eee;
  padding: 8px;
}

:deep(.toastui-editor-main) {
  background: white;
}

:deep(.toastui-editor-md-container) {
  background: #fafafa;
}

:deep(.toastui-editor-preview) {
  padding: 20px;
}

:deep(.toastui-editor-preview pre) {
  background: #f6f8fa;
  border-radius: 6px;
  padding: 16px;
}

:deep(.toastui-editor-preview code) {
  background: #f6f8fa;
  padding: 0.2em 0.4em;
  border-radius: 3px;
  font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', 'Consolas', monospace;
}
</style> 