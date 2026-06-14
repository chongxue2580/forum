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
        console.error('图片上传失败:', error)
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
        console.error('加载标签失败:', error)
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
          console.error('加载问题数据失败:', error)
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
        console.error('提交问题失败:', error)
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
  max-width: 1200px;
  margin: 0 auto;
  padding: 2rem 1rem;
}

.editor-form {
  max-width: 850px;
  margin: 0 auto;
}

.editor-header {
  margin-bottom: 24px;
}

.editor-header h1 {
  font-size: 28px;
  font-weight: 600;
  margin-bottom: 8px;
  color: var(--text-color);
}

.description {
  color: var(--text-light);
  font-size: 16px;
}

.editor-form {
  background: white;
  border-radius: var(--radius);
  box-shadow: var(--shadow);
  padding: 30px;
}

.form-group {
  margin-bottom: 24px;
}

.form-group label {
  display: block;
  font-size: 16px;
  font-weight: 500;
  margin-bottom: 8px;
  color: var(--text-color);
}

.form-group input[type="text"] {
  width: 100%;
  padding: 12px;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-sm);
  font-size: 16px;
  transition: all 0.3s;
}

.form-group input[type="text"]:focus {
  border-color: var(--primary-color);
  box-shadow: 0 0 0 2px rgba(24, 144, 255, 0.2);
}

.input-counter {
  text-align: right;
  font-size: 12px;
  color: var(--text-lighter);
  margin-top: 4px;
}

.editor-toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  padding: 8px;
  background-color: var(--bg-gray);
  border: 1px solid var(--border-color);
  border-bottom: none;
  border-radius: var(--radius-sm) var(--radius-sm) 0 0;
}

.toolbar-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border: none;
  background: transparent;
  color: var(--text-light);
  cursor: pointer;
  border-radius: var(--radius-sm);
  transition: all 0.2s;
}

.toolbar-btn:hover {
  background-color: var(--bg-white);
  color: var(--primary-color);
}

.toolbar-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.hidden-file-input {
  display: none;
}

.form-group textarea {
  width: 100%;
  padding: 12px;
  border: 1px solid var(--border-color);
  border-radius: 0 0 var(--radius-sm) var(--radius-sm);
  font-size: 16px;
  font-family: inherit;
  resize: vertical;
  transition: all 0.3s;
}

.form-group textarea:focus {
  border-color: var(--primary-color);
  box-shadow: 0 0 0 2px rgba(24, 144, 255, 0.2);
}

.markdown-hint {
  font-size: 12px;
  color: var(--text-lighter);
  margin-top: 8px;
}

.tags-input-container {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  padding: 8px 12px;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-sm);
  transition: all 0.3s;
}

.tags-input-container:focus-within {
  border-color: var(--primary-color);
  box-shadow: 0 0 0 2px rgba(24, 144, 255, 0.2);
}

.tag-item {
  display: flex;
  align-items: center;
  background-color: var(--primary-light);
  color: var(--primary-color);
  padding: 4px 8px;
  border-radius: 16px;
  font-size: 14px;
}

.tag-text {
  margin-right: 4px;
}

.tag-remove {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 16px;
  height: 16px;
  background: none;
  border: none;
  color: var(--primary-color);
  cursor: pointer;
  opacity: 0.7;
  transition: opacity 0.2s;
}

.tag-remove:hover {
  opacity: 1;
}

#tag-input {
  flex: 1;
  min-width: 120px;
  border: none;
  outline: none;
  padding: 4px 0;
  font-size: 14px;
}

.tag-options {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 10px;
}

.tag-option {
  border: 1px solid var(--border-color);
  background: #fff;
  color: var(--text-light);
  border-radius: 16px;
  padding: 4px 10px;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
}

.tag-option:hover,
.tag-option.selected {
  border-color: var(--primary-color);
  background: var(--primary-light);
  color: var(--primary-color);
}

.tags-hint {
  font-size: 12px;
  color: var(--text-lighter);
  margin-top: 8px;
}

.preview-section {
  margin-top: 30px;
  margin-bottom: 30px;
  background: var(--bg-gray);
  border-radius: var(--radius);
  border: 1px solid var(--border-color);
  overflow: hidden;
}

.preview-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: white;
  border-bottom: 1px solid var(--border-color);
}

.preview-header h2 {
  font-size: 18px;
  font-weight: 500;
  margin: 0;
  color: var(--text-color);
}

.close-preview {
  background: none;
  border: none;
  color: var(--text-lighter);
  cursor: pointer;
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  transition: all 0.3s;
}

.close-preview:hover {
  background-color: var(--bg-gray);
  color: var(--text-color);
}

.preview-content {
  padding: 20px;
}

.preview-content h1 {
  font-size: 24px;
  font-weight: 600;
  margin-bottom: 16px;
  color: var(--text-color);
}

.tags-preview {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 20px;
}

.tag-preview {
  background-color: var(--primary-light);
  color: var(--primary-color);
  padding: 4px 8px;
  border-radius: 16px;
  font-size: 14px;
}

.content-preview {
  background: white;
  padding: 16px;
  border-radius: var(--radius-sm);
  border: 1px solid var(--border-color);
  min-height: 200px;
  white-space: pre-wrap;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 16px;
  margin-top: 30px;
}

.btn-preview {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 16px;
  background-color: white;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-sm);
  color: var(--text-color);
  font-size: 14px;
  cursor: pointer;
  transition: all 0.3s;
}

.btn-preview:hover {
  background-color: var(--bg-gray);
}

.btn-submit {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 24px;
  background-color: var(--primary-color);
  border: none;
  border-radius: var(--radius-sm);
  color: white;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s;
}

.btn-submit:hover {
  background-color: var(--primary-dark);
}

.btn-submit:disabled {
  background-color: #d9d9d9;
  cursor: not-allowed;
}

@media (max-width: 1024px) {
  .question-editor {
    padding: 1rem;
  }

  .editor-form {
    max-width: 100%;
  }
}

@media (max-width: 768px) {
  .question-editor {
    padding: 0.5rem;
  }

  .editor-form {
    padding: 1rem;
    max-width: 100%;
  }

  .form-actions {
    flex-direction: column;
  }

  .btn-preview, .btn-submit {
    width: 100%;
    justify-content: center;
  }
}

/* 登录提示和用户信息样式 */
.login-notice {
  margin-top: 16px;
}

.user-info {
  margin-top: 16px;
  padding: 12px;
  background-color: #f0f9ff;
  border-radius: 6px;
  border-left: 4px solid #3b82f6;
}

.current-user {
  color: #1e40af;
  font-weight: 500;
}
</style> 
