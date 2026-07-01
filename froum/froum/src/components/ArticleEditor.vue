<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { marked } from 'marked'
import hljs from 'highlight.js'
import { Editor } from '@toast-ui/editor'
import '@toast-ui/editor/dist/toastui-editor.css'
import { articleService } from '../services/articleService'
import { categoryService } from '../services/categoryService'
import { tagService } from '../services/tagService'
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
  categoryId: '',
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
const categories = ref([])
const popularTags = ref([])

// 计算属性
const renderedContent = computed(() => {
  return marked(article.value.content)
})

const selectedCategory = computed(() => {
  return categories.value.find(c => c.id === parseInt(article.value.categoryId || article.value.category))
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
  
  if (!article.value.categoryId && !article.value.category) {
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

const unwrapList = (response) => {
  const data = response?.data ?? response
  if (Array.isArray(data)) return data
  if (Array.isArray(data?.content)) return data.content
  return []
}

const normalizeTagNames = (tags) => {
  if (!Array.isArray(tags)) return []
  return tags
    .map(tag => typeof tag === 'string' ? tag : tag?.name)
    .map(tag => tag?.trim())
    .filter(Boolean)
}

const buildArticleSummary = () => {
  const explicitSummary = article.value.summary?.trim()
  if (explicitSummary) return explicitSummary

  const plainContent = (article.value.content || '')
    .replace(/```[\s\S]*?```/g, ' ')
    .replace(/!\[[^\]]*]\([^)]*\)/g, ' ')
    .replace(/\[[^\]]*]\([^)]*\)/g, ' ')
    .replace(/[#>*_`~\-]+/g, ' ')
    .replace(/\s+/g, ' ')
    .trim()

  return (plainContent || article.value.title?.trim() || '').slice(0, 200)
}

const loadEditorOptions = async () => {
  try {
    const [categoryResponse, tagResponse] = await Promise.all([
      categoryService.getAllCategories(),
      tagService.getPopularTags(20)
    ])

    categories.value = unwrapList(categoryResponse)
    popularTags.value = unwrapList(tagResponse)
  } catch (error) {
    saveError.value = error.message || '加载分类或标签失败'
  }
}

const uploadCover = async (event) => {
  const file = event.target.files[0]
  if (file) {
    try {
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

      // 显示成功消息
      showSuccessMessage('封面图片上传成功！')
    } catch (error) {
      saveError.value = `封面图片上传失败: ${error.message}`
    }
  }
}

// 初始化编辑器
onMounted(async () => {
  await nextTick()
  await loadEditorOptions()
  
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
          saveError.value = error.message || '图片上传失败'
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
    const response = await articleService.getArticleById(id)
    const data = response.data || response
    article.value = {
      ...article.value,
      ...data,
      categoryId: data.categoryId || data.categoryInfo?.id || '',
      category: data.categoryId || data.categoryInfo?.id || '',
      tags: normalizeTagNames(data.tags),
      coverImage: data.coverImage || '',
      summary: data.summary || '',
      isDraft: data.isDraft ?? data.status === 'DRAFT'
    }
    editor.value.setMarkdown(data.content || '')
  } catch (error) {
    saveError.value = error.message || '加载文章失败'
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
      summary: buildArticleSummary(),
      categoryId: article.value.categoryId || article.value.category ? Number(article.value.categoryId || article.value.category) : null,
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
    } else {
      // 创建新文章
      savedArticle = await store.dispatch('createArticle', articleData);
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

    if (!article.value.categoryId && !article.value.category) {
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
      saveError.value = error.message || '图片上传失败'
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
          <select v-model="article.categoryId" required>
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
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
  min-height: calc(100vh - 4rem);
  gap: 1rem;
}

.article-editor.fullscreen {
  position: fixed;
  inset: 0;
  z-index: 1200;
  padding: 1rem;
  background: var(--kumo-bg-base);
}

.editor-header {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: start;
  gap: 1rem;
  padding: clamp(1rem, 2.4vw, 1.5rem);
  border: 1px solid var(--kumo-hairline);
  border-radius: var(--kumo-radius-xl);
  background:
    linear-gradient(135deg, var(--kumo-bg-elevated), var(--kumo-bg-subtle));
  box-shadow: var(--kumo-shadow-sm);
  backdrop-filter: var(--kumo-blur);
}

.header-left {
  display: grid;
  gap: 0.75rem;
  min-width: 0;
}

.header-right {
  display: flex;
  align-items: center;
}

.editor-title {
  margin: 0;
  color: var(--kumo-text-default);
  font-size: clamp(1.25rem, 3vw, 2rem);
  font-weight: 900;
  line-height: 1.1;
}

.title-input {
  width: 100%;
  min-height: 3.4rem;
  padding: 0.8rem 1rem;
  border: 1px solid var(--kumo-hairline);
  border-radius: var(--kumo-radius-lg);
  background: var(--kumo-bg-base);
  color: var(--kumo-text-default);
  font-size: 1.15rem;
  font-weight: 760;
  outline: none;
  transition:
    border-color var(--kumo-transition),
    box-shadow var(--kumo-transition);
}

.title-input:focus {
  border-color: var(--kumo-bg-brand);
  box-shadow: 0 0 0 4px var(--kumo-focus-ring);
}

.action-buttons {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 0.65rem;
}

.btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 0.55rem;
  min-height: 2.75rem;
  min-width: 8rem;
  padding: 0.68rem 1rem;
  border: 1px solid var(--kumo-hairline);
  border-radius: 999px;
  background: var(--kumo-bg-elevated);
  color: var(--kumo-text-default);
  font-size: 0.92rem;
  font-weight: 780;
  cursor: pointer;
  transition:
    transform var(--kumo-transition),
    border-color var(--kumo-transition),
    background-color var(--kumo-transition),
    color var(--kumo-transition),
    box-shadow var(--kumo-transition);
}

.btn:hover:not(:disabled) {
  transform: translateY(-2px);
  border-color: var(--kumo-hairline-strong);
  box-shadow: var(--kumo-shadow-sm);
}

.btn:disabled {
  cursor: not-allowed;
  opacity: 0.62;
}

.btn-primary {
  border-color: transparent;
  background: linear-gradient(135deg, var(--kumo-bg-brand), var(--kumo-bg-brand-strong));
  color: var(--kumo-text-inverse);
}

.success-message,
.error-message {
  position: fixed;
  top: 1.2rem;
  right: 1.2rem;
  z-index: 1300;
  display: inline-flex;
  align-items: center;
  gap: 0.6rem;
  min-width: min(22rem, calc(100vw - 2rem));
  padding: 0.85rem 1rem;
  border: 1px solid var(--kumo-hairline);
  border-radius: var(--kumo-radius-lg);
  box-shadow: var(--kumo-shadow-md);
  animation: slide-in 260ms ease both;
}

.success-message {
  border-color: var(--kumo-status-success);
  background: var(--kumo-status-success-tint);
  color: var(--kumo-status-success);
}

.error-message {
  border-color: var(--kumo-status-danger);
  background: var(--kumo-status-danger-tint);
  color: var(--kumo-status-danger);
}

.editor-main {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(18rem, 21rem);
  min-height: 0;
  gap: 1rem;
}

.editor-container {
  min-width: 0;
  overflow: hidden;
  border: 1px solid var(--kumo-hairline);
  border-radius: var(--kumo-radius-xl);
  background: var(--kumo-bg-elevated);
  box-shadow: var(--kumo-shadow-sm);
}

.editor-sidebar {
  display: grid;
  align-content: start;
  gap: 1rem;
  min-height: 0;
  padding: 1rem;
  overflow-y: auto;
  border: 1px solid var(--kumo-hairline);
  border-radius: var(--kumo-radius-xl);
  background: var(--kumo-bg-elevated);
  box-shadow: var(--kumo-shadow-sm);
  backdrop-filter: var(--kumo-blur);
}

.cover-upload,
.category-select,
.tag-select,
.summary-input {
  display: grid;
  gap: 0.7rem;
}

.cover-upload h3,
.category-select h3,
.tag-select h3,
.summary-input h3 {
  margin: 0;
  color: var(--kumo-text-default);
  font-size: 1rem;
  font-weight: 840;
}

.cover-preview {
  display: grid;
  place-items: center;
  width: 100%;
  aspect-ratio: 16 / 9;
  overflow: hidden;
  border: 1px dashed var(--kumo-hairline-strong);
  border-radius: var(--kumo-radius-lg);
  background: var(--kumo-bg-base);
  cursor: pointer;
  transition:
    transform var(--kumo-transition),
    border-color var(--kumo-transition),
    box-shadow var(--kumo-transition);
}

.cover-preview:hover {
  transform: translateY(-2px);
  border-color: var(--kumo-bg-brand);
  box-shadow: var(--kumo-shadow-sm);
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
  display: grid;
  place-items: center;
  gap: 0.55rem;
  color: var(--kumo-text-muted);
  font-weight: 760;
  text-align: center;
}

.upload-placeholder svg {
  color: var(--kumo-bg-brand);
  font-size: 2rem;
}

.cover-input {
  display: none;
}

.category-select select,
.summary-input textarea {
  width: 100%;
  padding: 0.7rem 0.85rem;
  border: 1px solid var(--kumo-hairline);
  border-radius: var(--kumo-radius-md);
  background: var(--kumo-bg-base);
  color: var(--kumo-text-default);
  font: inherit;
  transition:
    border-color var(--kumo-transition),
    box-shadow var(--kumo-transition);
}

.category-select select:focus,
.summary-input textarea:focus {
  border-color: var(--kumo-bg-brand);
  box-shadow: 0 0 0 4px var(--kumo-focus-ring);
  outline: none;
}

.selected-tags,
.popular-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 0.45rem;
}

.popular-tags {
  color: var(--kumo-text-muted);
  font-size: 0.88rem;
}

.tag,
.tag-suggestion {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  padding: 0.34rem 0.62rem;
  border: 1px solid var(--kumo-hairline);
  border-radius: 999px;
  background: var(--kumo-bg-base);
  color: var(--kumo-text-muted);
  font-size: 0.82rem;
  font-weight: 760;
}

.tag svg,
.tag-suggestion {
  cursor: pointer;
}

.tag svg:hover {
  color: var(--kumo-status-danger);
}

.tag-suggestion:hover {
  border-color: var(--kumo-bg-brand);
  background: var(--kumo-bg-brand-soft);
  color: var(--kumo-bg-brand-strong);
}

.summary-input textarea {
  min-height: 7rem;
  resize: vertical;
}

.hint {
  margin: 0;
  color: var(--kumo-text-subtle);
  font-size: 0.85rem;
  line-height: 1.5;
}

:deep(.toastui-editor-defaultUI) {
  overflow: hidden;
  border: 0;
  border-radius: var(--kumo-radius-xl);
  background: var(--kumo-bg-elevated);
}

:deep(.toastui-editor-toolbar) {
  border-bottom: 1px solid var(--kumo-hairline);
  background: var(--kumo-bg-subtle);
}

:deep(.toastui-editor-defaultUI-toolbar button) {
  border-radius: var(--kumo-radius-sm);
}

:deep(.toastui-editor-main),
:deep(.toastui-editor-md-container),
:deep(.toastui-editor-ww-container),
:deep(.toastui-editor-md-preview),
:deep(.toastui-editor-contents) {
  background: var(--kumo-bg-elevated);
  color: var(--kumo-text-default);
}

:deep(.toastui-editor-md-container),
:deep(.toastui-editor-md-preview) {
  background: var(--kumo-bg-base);
}

:deep(.toastui-editor-preview) {
  padding: 1.25rem;
}

:deep(.toastui-editor-preview pre),
:deep(.toastui-editor-preview code) {
  background: var(--kumo-bg-recessed);
}

:deep(.toastui-editor-preview pre) {
  padding: 1rem;
  border: 1px solid var(--kumo-hairline);
  border-radius: var(--kumo-radius-md);
}

:deep(.toastui-editor-preview code) {
  padding: 0.18rem 0.38rem;
  border-radius: var(--kumo-radius-sm);
  font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', 'Consolas', monospace;
}

@keyframes slide-in {
  from {
    opacity: 0;
    transform: translateY(-0.6rem);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@media (max-width: 960px) {
  .editor-header,
  .editor-main {
    grid-template-columns: 1fr;
  }

  .header-right,
  .action-buttons {
    width: 100%;
  }

  .action-buttons {
    justify-content: flex-start;
  }
}
</style>
