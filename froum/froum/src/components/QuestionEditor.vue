<template>
  <div class="question-editor">
    <div class="editor-header">
      <h1>{{ isEditing ? '编辑问题' : '提问题' }}</h1>
      <p class="description">
        {{ isEditing ? '编辑您的问题，让社区帮助您解决问题' : '详细描述您的问题，让社区帮助您解决问题' }}
      </p>
      <div v-if="!isAuthenticated" class="login-notice">
        <el-alert
          title="请先登录"
          type="warning"
          description="您需要登录后才能发布问题"
          show-icon
          :closable="false">
        </el-alert>
      </div>
      <div v-else class="user-info">
        <span class="current-user">当前用户：{{ currentUser?.nickname || currentUser?.username || '未知用户' }}</span>
      </div>
    </div>
    
    <form class="editor-form" @submit.prevent="submitQuestion">
      <div class="form-group">
        <label for="title">问题标题</label>
        <input 
          type="text" 
          id="title" 
          v-model="question.title" 
          placeholder="请用一句话描述您的问题" 
          required
          :maxlength="200"
        />
        <div class="input-counter">
          {{ question.title.length }}/200
        </div>
      </div>
      
      <div class="form-group">
        <label for="content">问题详情</label>
        <div class="editor-toolbar">
          <button type="button" class="toolbar-btn" title="加粗" @click="wrapSelection('**', '**', '加粗文字')">
            <font-awesome-icon :icon="['fas', 'bold']" />
          </button>
          <button type="button" class="toolbar-btn" title="斜体" @click="wrapSelection('*', '*', '斜体文字')">
            <font-awesome-icon :icon="['fas', 'italic']" />
          </button>
          <button type="button" class="toolbar-btn" title="链接" @click="wrapSelection('[', '](https://)', '链接文字')">
            <font-awesome-icon :icon="['fas', 'link']" />
          </button>
          <button type="button" class="toolbar-btn" title="代码" @click="wrapSelection('`', '`', '代码')">
            <font-awesome-icon :icon="['fas', 'code']" />
          </button>
          <button type="button" class="toolbar-btn" title="图片" :disabled="isUploadingImage" @click="triggerImagePicker">
            <font-awesome-icon :icon="['fas', isUploadingImage ? 'spinner' : 'image']" :spin="isUploadingImage" />
          </button>
          <button type="button" class="toolbar-btn" title="无序列表" @click="insertLinePrefix('- ')">
            <font-awesome-icon :icon="['fas', 'list-ul']" />
          </button>
          <button type="button" class="toolbar-btn" title="有序列表" @click="insertLinePrefix('1. ')">
            <font-awesome-icon :icon="['fas', 'list-ol']" />
          </button>
          <button type="button" class="toolbar-btn" title="引用" @click="insertLinePrefix('> ')">
            <font-awesome-icon :icon="['fas', 'quote-left']" />
          </button>
          <input
            ref="imageInput"
            type="file"
            accept="image/*"
            class="hidden-file-input"
            @change="handleImageSelect"
          />
        </div>
        <textarea
          id="content"
          ref="contentRef"
          v-model="question.content"
          placeholder="请详细描述您的问题，可以包含代码示例、错误信息等"
          rows="12"
          required
        ></textarea>
        <p class="markdown-hint">支持 Markdown 格式</p>
      </div>
      
      <div class="form-group">
        <label for="tags">标签</label>
        <div class="tags-input-container">
          <div v-for="(tag, index) in question.tags" :key="index" class="tag-item">
            <span class="tag-text">{{ getTagName(tag) }}</span>
            <button type="button" class="tag-remove" @click="removeTag(index)">
              <font-awesome-icon :icon="['fas', 'times']" />
            </button>
          </div>
          <input 
            type="text" 
            id="tag-input" 
            v-model="tagInput" 
            placeholder="输入标签，按回车添加" 
            @keydown.enter.prevent="addTag"
          />
        </div>
        <div v-if="availableTags.length" class="tag-options">
          <button
            v-for="tag in availableTags.slice(0, 12)"
            :key="tag.id"
            type="button"
            class="tag-option"
            :class="{ selected: isTagSelected(tag) }"
            @click="addTag(tag)"
          >
            {{ tag.name }}
          </button>
        </div>
        <p class="tags-hint">最多选择5个已有标签</p>
      </div>
      
      <div class="preview-section" v-if="showPreview">
        <div class="preview-header">
          <h2>预览</h2>
          <button type="button" class="close-preview" @click="showPreview = false">
            <font-awesome-icon :icon="['fas', 'times']" />
          </button>
        </div>
        <div class="preview-content">
          <h1>{{ question.title || '问题标题' }}</h1>
          <div class="tags-preview">
            <span v-for="(tag, index) in question.tags" :key="index" class="tag-preview">
              {{ getTagName(tag) }}
            </span>
          </div>
          <div class="content-preview">
            {{ question.content || '问题详情' }}
          </div>
        </div>
      </div>
      
      <div class="form-actions">
        <button type="button" class="btn-preview" @click="togglePreview">
          <font-awesome-icon :icon="['fas', 'eye']" />
          {{ showPreview ? '关闭预览' : '预览' }}
        </button>
        <button type="submit" class="btn-submit" :disabled="isSubmitting">
          <font-awesome-icon :icon="['fas', 'spinner']" spin v-if="isSubmitting" />
          {{ isSubmitting ? '提交中...' : (isEditing ? '保存修改' : '发布问题') }}
        </button>
      </div>
    </form>
  </div>
</template>

<script>
import { defineComponent, ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useStore } from 'vuex'
import { questionService } from '../services/questionService'
import { articleService } from '../services/articleService'
import { tagService } from '../services/tagService'
import { ElMessage } from 'element-plus'

export default defineComponent({
  name: 'QuestionEditor',
  props: {
    questionId: {
      type: [String, Number],
      default: null
    }
  },
  setup(props) {
    const router = useRouter()
    const route = useRoute()
    const store = useStore()

    // 检查用户登录状态
    const isAuthenticated = computed(() => store.state.isAuthenticated)
    const currentUser = computed(() => store.state.user)

    const isEditing = computed(() => !!props.questionId)
    const isSubmitting = ref(false)
    const showPreview = ref(false)
    const tagInput = ref('')
    const availableTags = ref([])
    
    const question = ref({
      title: '',
      content: '',
      tags: []
    })

    // 编辑器工具栏
    const contentRef = ref(null)
    const imageInput = ref(null)
    const isUploadingImage = ref(false)

    // 在光标处包裹/插入文本
    const applyEdit = (transform) => {
      const el = contentRef.value
      const value = question.value.content || ''
      const start = el ? el.selectionStart : value.length
      const end = el ? el.selectionEnd : value.length
      const selected = value.slice(start, end)
      const { text, cursorStart, cursorEnd } = transform(selected, value, start, end)
      question.value.content = text
      // 恢复光标
      requestAnimationFrame(() => {
        if (!el) return
        el.focus()
        el.setSelectionRange(cursorStart, cursorEnd)
      })
    }

    const wrapSelection = (before, after, placeholder) => {
      applyEdit((selected, value, start, end) => {
        const inner = selected || placeholder
        const text = value.slice(0, start) + before + inner + after + value.slice(end)
        const cursorStart = start + before.length
        return { text, cursorStart, cursorEnd: cursorStart + inner.length }
      })
    }

    const insertLinePrefix = (prefix) => {
      applyEdit((selected, value, start, end) => {
        const lineStart = value.lastIndexOf('\n', start - 1) + 1
        const text = value.slice(0, lineStart) + prefix + value.slice(lineStart)
        const pos = end + prefix.length
        return { text, cursorStart: pos, cursorEnd: pos }
      })
    }

    const insertText = (snippet) => {
      applyEdit((selected, value, start, end) => {
        const text = value.slice(0, start) + snippet + value.slice(end)
        const pos = start + snippet.length
        return { text, cursorStart: pos, cursorEnd: pos }
      })
    }

    const triggerImagePicker = () => {
      if (!isAuthenticated.value) {
        ElMessage.warning('请先登录后再上传图片')
        return
      }
      imageInput.value?.click()
    }

    const handleImageSelect = async (event) => {
      const file = event.target.files?.[0]
      if (!file) return
      isUploadingImage.value = true
      try {
        const url = await articleService.uploadImage(file)
        if (!url) throw new Error('图片上传未返回地址')
        insertText(`![${file.name}](${url})\n`)
        ElMessage.success('图片已插入')
      } catch (error) {
        ElMessage.error(error.message || '图片上传失败')
      } finally {
        isUploadingImage.value = false
        event.target.value = ''
      }
    }

    const unwrapList = (response) => {
      const data = response?.data ?? response
      if (Array.isArray(data)) return data
      if (Array.isArray(data?.content)) return data.content
      return []
    }

    const getTagName = (tag) => typeof tag === 'string' ? tag : tag?.name || ''

    const isTagSelected = (tag) => {
      const tagName = getTagName(tag).toLowerCase()
      return question.value.tags.some(selected => (
        selected.id && tag?.id
          ? String(selected.id) === String(tag.id)
          : getTagName(selected).toLowerCase() === tagName
      ))
    }

    const resolveTag = (tag) => {
      if (!tag) return null
      if (typeof tag === 'object' && tag.id) return tag

      const name = getTagName(tag).trim()
      if (!name) return null
      return availableTags.value.find(item => item.name.toLowerCase() === name.toLowerCase()) || null
    }

    const loadAvailableTags = async () => {
      try {
        const response = await tagService.getAllTags()
        availableTags.value = unwrapList(response)
      } catch (error) {
        ElMessage.error(error.message || '加载标签失败')
      }
    }
    
    // 如果是编辑问题，加载已有问题数据
    onMounted(async () => {
      await loadAvailableTags()

      if (isEditing.value) {
        try {
          const response = await questionService.getQuestionById(props.questionId)
          const questionData = response.data || response
          const selectedTags = Array.isArray(questionData.tags)
            ? questionData.tags.map(resolveTag).filter(Boolean)
            : []

          question.value = {
            title: questionData.title || '',
            content: questionData.content || '',
            tags: selectedTags
          }
        } catch (error) {
          ElMessage.error(error.message || '加载问题数据失败')
        }
      }
    })
    
    const addTag = (tagOption = null) => {
      const explicitTag = tagOption && (tagOption.id || tagOption.name) ? tagOption : null
      const tag = explicitTag || resolveTag(tagInput.value)
      if (!tag) {
        if (tagInput.value.trim()) {
          ElMessage.error('请选择已有标签')
        }
        return
      }
      
      // 检查是否已存在该标签
      if (isTagSelected(tag)) {
        tagInput.value = ''
        return
      }
      
      // 检查标签数量限制
      if (question.value.tags.length >= 5) {
        ElMessage.warning('最多选择5个标签')
        return
      }

      if (!tag.id) {
        ElMessage.error('请选择已有标签')
        return
      }
      
      question.value.tags.push(tag)
      tagInput.value = ''
    }
    
    const removeTag = (index) => {
      question.value.tags.splice(index, 1)
    }
    
    const togglePreview = () => {
      showPreview.value = !showPreview.value
      
      if (showPreview.value) {
        // 滚动到预览区域
        setTimeout(() => {
          const previewElement = document.querySelector('.preview-section')
          if (previewElement) {
            previewElement.scrollIntoView({ behavior: 'smooth' })
          }
        }, 100)
      }
    }
    
    const submitQuestion = async () => {
      // 检查用户登录状态
      if (!isAuthenticated.value) {
        ElMessage.error('请先登录后再发布问题')
        router.push('/login?redirect=' + encodeURIComponent(route.fullPath))
        return
      }

      // 表单验证
      if (!question.value.title.trim()) {
        ElMessage.error('请输入问题标题')
        return
      }

      if (!question.value.content.trim()) {
        ElMessage.error('请输入问题详情')
        return
      }

      isSubmitting.value = true

      try {
        const payload = {
          title: question.value.title.trim(),
          content: question.value.content.trim(),
          tagIds: question.value.tags.map(tag => Number(tag.id)).filter(Boolean)
        }

        if (isEditing.value) {
          // 编辑问题
          const response = await questionService.updateQuestion(props.questionId, payload)
          if (response.success) {
            ElMessage.success('问题修改成功！')
            router.push(`/question/${props.questionId}`)
          } else {
            throw new Error(response.message || '修改失败')
          }
        } else {
          // 创建新问题
          const response = await questionService.createQuestion(payload)

          if (response.success) {
            ElMessage.success('问题发布成功！')
            // 跳转到问题详情页面，使用真实的问题ID
            router.push(`/question/${response.data.id}`)
          } else {
            throw new Error(response.message || '发布失败')
          }
        }
      } catch (error) {
        ElMessage.error(error.message || '提交失败，请稍后重试')
      } finally {
        isSubmitting.value = false
      }
    }
    
    return {
      isEditing,
      isSubmitting,
      isAuthenticated,
      currentUser,
      question,
      availableTags,
      tagInput,
      showPreview,
      getTagName,
      isTagSelected,
      addTag,
      removeTag,
      togglePreview,
      submitQuestion,
      contentRef,
      imageInput,
      isUploadingImage,
      wrapSelection,
      insertLinePrefix,
      triggerImagePicker,
      handleImageSelect
    }
  }
})
</script>

<style scoped>
.question-editor {
  display: grid;
  gap: 1.25rem;
  max-width: 1100px;
  margin: 0 auto;
}

.editor-header {
  display: grid;
  gap: 0.8rem;
  padding: clamp(1.25rem, 3vw, 2rem);
  border: 1px solid var(--kumo-hairline-strong);
  border-radius: var(--kumo-radius-xl);
  background:
    linear-gradient(135deg, var(--kumo-bg-elevated), var(--kumo-bg-subtle));
  box-shadow: var(--kumo-shadow-md);
  backdrop-filter: var(--kumo-blur);
}

.editor-header h1 {
  margin: 0;
  color: var(--kumo-text-default);
  font-size: clamp(2rem, 6vw, 4rem);
  font-weight: 900;
  line-height: 1.05;
}

.description {
  max-width: 42rem;
  margin: 0;
  color: var(--kumo-text-muted);
  font-size: 1rem;
  line-height: 1.65;
}

.login-notice {
  margin-top: 0.4rem;
}

.user-info {
  width: fit-content;
  padding: 0.55rem 0.85rem;
  border: 1px solid var(--kumo-hairline);
  border-radius: 999px;
  background: var(--kumo-bg-brand-soft);
  color: var(--kumo-bg-brand-strong);
  font-weight: 760;
}

.editor-form {
  display: grid;
  gap: 1.25rem;
  padding: clamp(1rem, 3vw, 1.8rem);
  border: 1px solid var(--kumo-hairline);
  border-radius: var(--kumo-radius-xl);
  background: var(--kumo-bg-elevated);
  box-shadow: var(--kumo-shadow-sm);
  backdrop-filter: var(--kumo-blur);
}

.form-group {
  display: grid;
  gap: 0.6rem;
}

.form-group label {
  color: var(--kumo-text-default);
  font-weight: 840;
}

.form-group input[type='text'],
.form-group textarea,
#tag-input {
  color: var(--kumo-text-default);
  font: inherit;
}

.form-group input[type='text'],
.form-group textarea {
  width: 100%;
  padding: 0.8rem 0.95rem;
  border: 1px solid var(--kumo-hairline);
  background: var(--kumo-bg-base);
  transition:
    border-color var(--kumo-transition),
    box-shadow var(--kumo-transition);
}

.form-group input[type='text'] {
  min-height: 3rem;
  border-radius: var(--kumo-radius-md);
  font-size: 1.05rem;
  font-weight: 720;
}

.form-group input[type='text']:focus,
.form-group textarea:focus,
.tags-input-container:focus-within {
  border-color: var(--kumo-bg-brand);
  box-shadow: 0 0 0 4px var(--kumo-focus-ring);
  outline: none;
}

.input-counter,
.markdown-hint,
.tags-hint {
  color: var(--kumo-text-subtle);
  font-size: 0.82rem;
  font-weight: 690;
}

.input-counter {
  text-align: right;
}

.editor-toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 0.35rem;
  padding: 0.5rem;
  border: 1px solid var(--kumo-hairline);
  border-bottom: 0;
  border-radius: var(--kumo-radius-md) var(--kumo-radius-md) 0 0;
  background: var(--kumo-bg-subtle);
}

.toolbar-btn,
.close-preview {
  display: inline-grid;
  place-items: center;
  width: 2.35rem;
  height: 2.35rem;
  border: 1px solid var(--kumo-hairline);
  border-radius: var(--kumo-radius-sm);
  background: var(--kumo-bg-elevated);
  color: var(--kumo-text-muted);
  cursor: pointer;
  transition:
    transform var(--kumo-transition),
    border-color var(--kumo-transition),
    color var(--kumo-transition),
    background-color var(--kumo-transition);
}

.toolbar-btn:hover,
.close-preview:hover {
  transform: translateY(-2px);
  border-color: var(--kumo-bg-brand);
  background: var(--kumo-bg-brand-soft);
  color: var(--kumo-bg-brand-strong);
}

.toolbar-btn:disabled {
  cursor: not-allowed;
  opacity: 0.5;
  transform: none;
}

.hidden-file-input {
  display: none;
}

.form-group textarea {
  min-height: 18rem;
  border-radius: 0 0 var(--kumo-radius-md) var(--kumo-radius-md);
  font-size: 1rem;
  line-height: 1.65;
  resize: vertical;
}

.tags-input-container {
  display: flex;
  flex-wrap: wrap;
  gap: 0.55rem;
  padding: 0.6rem;
  border: 1px solid var(--kumo-hairline);
  border-radius: var(--kumo-radius-md);
  background: var(--kumo-bg-base);
  transition:
    border-color var(--kumo-transition),
    box-shadow var(--kumo-transition);
}

.tag-item,
.tag-option,
.tag-preview {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  padding: 0.34rem 0.62rem;
  border: 1px solid var(--kumo-hairline);
  border-radius: 999px;
  background: var(--kumo-bg-subtle);
  color: var(--kumo-text-muted);
  font-size: 0.84rem;
  font-weight: 760;
}

.tag-item {
  background: var(--kumo-bg-brand-soft);
  color: var(--kumo-bg-brand-strong);
}

.tag-remove {
  display: inline-grid;
  place-items: center;
  width: 1.2rem;
  height: 1.2rem;
  border: 0;
  border-radius: 50%;
  background: transparent;
  color: inherit;
  cursor: pointer;
}

.tag-remove:hover {
  color: var(--kumo-status-danger);
}

#tag-input {
  flex: 1;
  min-width: 10rem;
  border: 0;
  background: transparent;
  outline: none;
}

.tag-options,
.tags-preview {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
}

.tag-option {
  cursor: pointer;
  transition:
    border-color var(--kumo-transition),
    background-color var(--kumo-transition),
    color var(--kumo-transition);
}

.tag-option:hover,
.tag-option.selected {
  border-color: var(--kumo-bg-brand);
  background: var(--kumo-bg-brand-soft);
  color: var(--kumo-bg-brand-strong);
}

.preview-section {
  overflow: hidden;
  border: 1px solid var(--kumo-hairline);
  border-radius: var(--kumo-radius-lg);
  background: var(--kumo-bg-base);
}

.preview-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  padding: 0.9rem 1rem;
  border-bottom: 1px solid var(--kumo-hairline);
  background: var(--kumo-bg-elevated);
}

.preview-header h2,
.preview-content h1 {
  margin: 0;
  color: var(--kumo-text-default);
  font-weight: 840;
}

.preview-header h2 {
  font-size: 1.05rem;
}

.preview-content {
  display: grid;
  gap: 1rem;
  padding: 1rem;
}

.preview-content h1 {
  font-size: 1.45rem;
}

.content-preview {
  min-height: 12rem;
  padding: 1rem;
  border: 1px solid var(--kumo-hairline);
  border-radius: var(--kumo-radius-md);
  background: var(--kumo-bg-elevated);
  color: var(--kumo-text-default);
  line-height: 1.65;
  white-space: pre-wrap;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 0.75rem;
}

.btn-preview,
.btn-submit {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 0.55rem;
  min-height: 2.75rem;
  padding: 0.68rem 1.05rem;
  border: 1px solid var(--kumo-hairline);
  border-radius: 999px;
  background: var(--kumo-bg-elevated);
  color: var(--kumo-text-default);
  font-weight: 780;
  cursor: pointer;
  transition:
    transform var(--kumo-transition),
    border-color var(--kumo-transition),
    color var(--kumo-transition),
    background-color var(--kumo-transition),
    box-shadow var(--kumo-transition);
}

.btn-preview:hover,
.btn-submit:hover:not(:disabled) {
  transform: translateY(-2px);
  border-color: var(--kumo-hairline-strong);
  box-shadow: var(--kumo-shadow-sm);
}

.btn-submit {
  border-color: transparent;
  background: linear-gradient(135deg, var(--kumo-bg-brand), var(--kumo-bg-brand-strong));
  color: var(--kumo-text-inverse);
}

.btn-submit:disabled {
  background: var(--kumo-bg-recessed);
  color: var(--kumo-text-subtle);
  cursor: not-allowed;
  transform: none;
}

@media (max-width: 768px) {
  .form-actions {
    align-items: stretch;
    flex-direction: column;
  }

  .btn-preview,
  .btn-submit {
    width: 100%;
  }
}
</style>
