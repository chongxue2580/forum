<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import '@/components/admin/AdminCommonStyles.css'
import {
  clearSystemCache,
  getSystemSettings,
  resetSystemSettings,
  testSystemEmailSettings,
  updateSystemSettings
} from '@/api/admin'

const formRef = ref(null)
const loading = ref(false)
const saving = ref(false)
const testingEmail = ref(false)
const clearingCache = ref(false)

const createDefaultSettings = () => ({
  site: {
    name: 'FroumX 技术社区',
    description: '专注于前端开发、后端开发、移动开发、人工智能等技术交流的社区',
    logo: '/logo.png',
    favicon: '/favicon.ico',
    icp: '',
    allowRegistration: true,
    requireEmailVerification: false,
    defaultUserRole: 'USER'
  },
  content: {
    articleReviewEnabled: true,
    questionReviewEnabled: false,
    commentReviewEnabled: false,
    allowAnonymousView: true,
    allowGuestComment: false,
    sensitiveWords: '',
    maxUploadSize: 10,
    allowedFileTypes: 'jpg,jpeg,png,gif,pdf,doc,docx,xls,xlsx,ppt,pptx,zip,rar'
  },
  email: {
    smtpServer: '',
    smtpPort: 465,
    smtpUsername: '',
    smtpPassword: '',
    senderName: 'FroumX 社区',
    senderEmail: '',
    enableSsl: true
  },
  security: {
    jwtExpireDays: 7,
    maxLoginAttempts: 5,
    lockoutMinutes: 30,
    enableRecaptcha: false,
    recaptchaSiteKey: '',
    recaptchaSecretKey: ''
  },
  optimization: {
    enableCache: false,
    cacheTtl: 3600,
    pageSize: 20,
    maxSearchResults: 100
  }
})

const systemSettings = reactive(createDefaultSettings())

const rules = {
  'site.name': [
    { required: true, message: '请输入站点名称', trigger: 'blur' },
    { min: 2, max: 50, message: '长度在 2 到 50 个字符', trigger: 'blur' }
  ],
  'site.description': [
    { required: true, message: '请输入站点描述', trigger: 'blur' },
    { min: 10, max: 200, message: '长度在 10 到 200 个字符', trigger: 'blur' }
  ],
  'email.smtpPort': [
    { required: true, message: '请输入SMTP端口', trigger: 'blur' },
    { type: 'number', message: '端口必须为数字值', trigger: 'blur' }
  ]
}

const unwrapData = (response) => response?.data || response || {}

const applySettings = (settings = {}) => {
  const defaults = createDefaultSettings()
  Object.keys(defaults).forEach(group => {
    systemSettings[group] = {
      ...defaults[group],
      ...(settings[group] || {})
    }
  })
}

const loadSettings = async () => {
  loading.value = true
  try {
    const response = await getSystemSettings()
    applySettings(unwrapData(response))
  } catch (error) {
    ElMessage.error(error.message || '加载系统设置失败')
  } finally {
    loading.value = false
  }
}

const saveSettings = async () => {
  if (!formRef.value) return

  try {
    await formRef.value.validate()
    saving.value = true
    const response = await updateSystemSettings(JSON.parse(JSON.stringify(systemSettings)))
    applySettings(unwrapData(response))
    ElMessage.success(response?.message || '系统设置已保存')
  } catch (error) {
    if (error !== false) {
      ElMessage.error(error.message || '保存系统设置失败')
    }
  } finally {
    saving.value = false
  }
}

const resetSettings = async () => {
  try {
    await ElMessageBox.confirm('确定要将系统设置重置为默认值吗？', '重置系统设置', {
      confirmButtonText: '重置',
      cancelButtonText: '取消',
      type: 'warning'
    })
    saving.value = true
    const response = await resetSystemSettings()
    applySettings(unwrapData(response))
    ElMessage.success(response?.message || '系统设置已重置')
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '重置系统设置失败')
    }
  } finally {
    saving.value = false
  }
}

const testEmailSettings = async () => {
  try {
    const { value } = await ElMessageBox.prompt('请输入测试邮件收件人', '测试邮件设置', {
      confirmButtonText: '发送',
      cancelButtonText: '取消',
      inputValue: systemSettings.email.senderEmail,
      inputPattern: /^[^\s@]+@[^\s@]+\.[^\s@]+$/,
      inputErrorMessage: '请输入有效邮箱地址'
    })

    testingEmail.value = true
    const response = await testSystemEmailSettings({ recipient: value })
    ElMessage.success(response?.message || '测试邮件已发送')
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '测试邮件发送失败')
    }
  } finally {
    testingEmail.value = false
  }
}

const clearCache = async () => {
  try {
    await ElMessageBox.confirm('确定要清除后端缓存管理器中的缓存吗？', '清除系统缓存', {
      confirmButtonText: '清除',
      cancelButtonText: '取消',
      type: 'warning'
    })
    clearingCache.value = true
    const response = await clearSystemCache()
    ElMessage.success(response?.message || '系统缓存已清除')
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '清除系统缓存失败')
    }
  } finally {
    clearingCache.value = false
  }
}

onMounted(loadSettings)
</script>

<template>
  <div class="system-settings admin-page-container">
    <div class="admin-page-header">
      <h1 class="admin-page-title">
        <font-awesome-icon icon="cog" />
        系统设置
      </h1>
      <div class="header-actions">
        <el-button :loading="loading" @click="loadSettings">
          <font-awesome-icon icon="sync" /> 刷新
        </el-button>
        <el-button type="primary" :loading="saving" @click="saveSettings">
          <font-awesome-icon icon="save" /> 保存设置
        </el-button>
      </div>
    </div>

    <el-form
      ref="formRef"
      :model="systemSettings"
      :rules="rules"
      label-width="150px"
      v-loading="loading"
      class="settings-form admin-content-card"
    >
      <el-tabs>
        <el-tab-pane>
          <template #label>
            <span><font-awesome-icon icon="home" /> 基本设置</span>
          </template>

          <div class="settings-section">
            <h2>站点信息</h2>
            <el-form-item label="站点名称" prop="site.name">
              <el-input v-model="systemSettings.site.name" maxlength="50" show-word-limit />
            </el-form-item>

            <el-form-item label="站点描述" prop="site.description">
              <el-input v-model="systemSettings.site.description" type="textarea" :rows="3" maxlength="200" show-word-limit />
            </el-form-item>

            <el-form-item label="Logo URL">
              <div class="asset-field">
                <el-input v-model="systemSettings.site.logo" placeholder="/logo.png 或 https://..." />
                <img v-if="systemSettings.site.logo" :src="systemSettings.site.logo" alt="Logo" class="asset-preview" />
              </div>
            </el-form-item>

            <el-form-item label="Favicon URL">
              <div class="asset-field">
                <el-input v-model="systemSettings.site.favicon" placeholder="/favicon.ico 或 https://..." />
                <img v-if="systemSettings.site.favicon" :src="systemSettings.site.favicon" alt="Favicon" class="asset-preview small" />
              </div>
            </el-form-item>

            <el-form-item label="ICP备案号">
              <el-input v-model="systemSettings.site.icp" placeholder="例如：京ICP备..." />
            </el-form-item>
          </div>

          <div class="settings-section">
            <h2>注册设置</h2>
            <el-form-item label="允许注册">
              <el-switch v-model="systemSettings.site.allowRegistration" />
            </el-form-item>
            <el-form-item label="邮箱验证">
              <el-switch v-model="systemSettings.site.requireEmailVerification" />
            </el-form-item>
            <el-form-item label="默认用户角色">
              <el-select v-model="systemSettings.site.defaultUserRole">
                <el-option label="普通用户" value="USER" />
                <el-option label="管理员" value="ADMIN" />
                <el-option label="超级管理员" value="SUPER_ADMIN" />
              </el-select>
            </el-form-item>
          </div>
        </el-tab-pane>

        <el-tab-pane>
          <template #label>
            <span><font-awesome-icon icon="file-alt" /> 内容设置</span>
          </template>

          <div class="settings-section">
            <h2>审核设置</h2>
            <el-form-item label="文章审核">
              <el-switch v-model="systemSettings.content.articleReviewEnabled" />
            </el-form-item>
            <el-form-item label="问答审核">
              <el-switch v-model="systemSettings.content.questionReviewEnabled" />
            </el-form-item>
            <el-form-item label="评论审核">
              <el-switch v-model="systemSettings.content.commentReviewEnabled" />
            </el-form-item>
          </div>

          <div class="settings-section">
            <h2>访问和过滤</h2>
            <el-form-item label="允许匿名访问">
              <el-switch v-model="systemSettings.content.allowAnonymousView" />
            </el-form-item>
            <el-form-item label="允许游客评论">
              <el-switch v-model="systemSettings.content.allowGuestComment" />
            </el-form-item>
            <el-form-item label="敏感词">
              <el-input
                v-model="systemSettings.content.sensitiveWords"
                type="textarea"
                :rows="3"
                placeholder="多个敏感词请用英文逗号分隔"
              />
            </el-form-item>
          </div>

          <div class="settings-section">
            <h2>上传设置</h2>
            <el-form-item label="最大上传大小(MB)">
              <el-input-number v-model="systemSettings.content.maxUploadSize" :min="1" :max="100" />
            </el-form-item>
            <el-form-item label="允许的文件类型">
              <el-input v-model="systemSettings.content.allowedFileTypes" placeholder="jpg,png,pdf..." />
            </el-form-item>
          </div>
        </el-tab-pane>

        <el-tab-pane>
          <template #label>
            <span><font-awesome-icon icon="envelope" /> 邮件设置</span>
          </template>

          <div class="settings-section">
            <h2>SMTP 设置</h2>
            <el-form-item label="SMTP服务器">
              <el-input v-model="systemSettings.email.smtpServer" placeholder="smtp.example.com" />
            </el-form-item>
            <el-form-item label="SMTP端口" prop="email.smtpPort">
              <el-input-number v-model="systemSettings.email.smtpPort" :min="1" :max="65535" />
            </el-form-item>
            <el-form-item label="SMTP用户名">
              <el-input v-model="systemSettings.email.smtpUsername" autocomplete="off" />
            </el-form-item>
            <el-form-item label="SMTP密码">
              <el-input v-model="systemSettings.email.smtpPassword" type="password" show-password autocomplete="new-password" />
            </el-form-item>
            <el-form-item label="启用SSL/TLS">
              <el-switch v-model="systemSettings.email.enableSsl" />
            </el-form-item>
          </div>

          <div class="settings-section">
            <h2>发件人</h2>
            <el-form-item label="发件人名称">
              <el-input v-model="systemSettings.email.senderName" />
            </el-form-item>
            <el-form-item label="发件人邮箱">
              <el-input v-model="systemSettings.email.senderEmail" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="testingEmail" @click="testEmailSettings">
                <font-awesome-icon icon="paper-plane" /> 发送测试邮件
              </el-button>
            </el-form-item>
          </div>
        </el-tab-pane>

        <el-tab-pane>
          <template #label>
            <span><font-awesome-icon icon="shield-alt" /> 安全设置</span>
          </template>

          <div class="settings-section">
            <h2>登录保护</h2>
            <el-form-item label="JWT过期天数">
              <el-input-number v-model="systemSettings.security.jwtExpireDays" :min="1" :max="30" />
            </el-form-item>
            <el-form-item label="最大登录尝试次数">
              <el-input-number v-model="systemSettings.security.maxLoginAttempts" :min="1" :max="10" />
            </el-form-item>
            <el-form-item label="锁定时间(分钟)">
              <el-input-number v-model="systemSettings.security.lockoutMinutes" :min="5" :max="1440" />
            </el-form-item>
          </div>

          <div class="settings-section">
            <h2>reCAPTCHA</h2>
            <el-form-item label="启用reCAPTCHA">
              <el-switch v-model="systemSettings.security.enableRecaptcha" />
            </el-form-item>
            <el-form-item label="Site Key">
              <el-input v-model="systemSettings.security.recaptchaSiteKey" />
            </el-form-item>
            <el-form-item label="Secret Key">
              <el-input v-model="systemSettings.security.recaptchaSecretKey" type="password" show-password />
            </el-form-item>
          </div>
        </el-tab-pane>

        <el-tab-pane>
          <template #label>
            <span><font-awesome-icon icon="tachometer-alt" /> 优化设置</span>
          </template>

          <div class="settings-section">
            <h2>缓存设置</h2>
            <el-form-item label="启用缓存">
              <el-switch v-model="systemSettings.optimization.enableCache" />
            </el-form-item>
            <el-form-item label="缓存过期时间(秒)">
              <el-input-number v-model="systemSettings.optimization.cacheTtl" :min="60" :max="86400" />
            </el-form-item>
            <el-form-item>
              <el-button type="warning" :loading="clearingCache" @click="clearCache">
                <font-awesome-icon icon="trash-alt" /> 清除系统缓存
              </el-button>
            </el-form-item>
          </div>

          <div class="settings-section">
            <h2>分页和搜索</h2>
            <el-form-item label="默认分页大小">
              <el-input-number v-model="systemSettings.optimization.pageSize" :min="10" :max="100" />
            </el-form-item>
            <el-form-item label="最大搜索结果数">
              <el-input-number v-model="systemSettings.optimization.maxSearchResults" :min="20" :max="1000" />
            </el-form-item>
          </div>
        </el-tab-pane>
      </el-tabs>

      <div class="form-actions">
        <el-button type="primary" :loading="saving" @click="saveSettings">
          <font-awesome-icon icon="save" /> 保存设置
        </el-button>
        <el-button :loading="saving" @click="resetSettings">
          <font-awesome-icon icon="undo" /> 重置默认
        </el-button>
      </div>
    </el-form>
  </div>
</template>

<style scoped>
.system-settings {
  color: var(--ad-text, #1d1d1f);
}

.header-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.settings-form {
  padding: 24px;
}

.settings-section {
  padding: 18px 0 8px;
  border-bottom: 1px solid var(--ad-hairline, rgba(64, 87, 140, 0.1));
}

.settings-section:last-child {
  border-bottom: none;
}

.settings-section h2 {
  margin: 0 0 16px;
  font-size: 0.95rem;
  font-weight: 700;
  color: var(--ad-text, #1d1d1f);
}

.asset-field {
  display: grid;
  grid-template-columns: minmax(240px, 1fr) 48px;
  gap: 12px;
  align-items: center;
  width: 100%;
}

.asset-preview {
  width: 48px;
  height: 48px;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  object-fit: contain;
  background: #fff;
}

.asset-preview.small {
  width: 32px;
  height: 32px;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding-top: 20px;
}

:deep(.el-tabs__header) {
  margin-bottom: 8px;
}

:deep(.el-form-item__label) {
  color: var(--ad-text-muted, #515154);
  font-weight: 600;
}

/* 表单组件玻璃化（与全站设计令牌一致） */
:deep(.el-input__wrapper),
:deep(.el-textarea__inner),
:deep(.el-select__wrapper),
:deep(.el-input-number) {
  border-radius: 12px;
  background: var(--ad-surface-muted, rgba(255, 255, 255, 0.62));
  box-shadow: inset 0 0 0 1px var(--ad-hairline-strong, rgba(64, 87, 140, 0.16));
}

:deep(.el-input__wrapper.is-focus),
:deep(.el-textarea__inner:focus),
:deep(.el-select__wrapper.is-focused) {
  background: #fff;
  box-shadow: inset 0 0 0 1px rgba(0, 122, 255, 0.45), 0 0 0 4px rgba(0, 122, 255, 0.12);
}

:deep(.el-switch.is-checked .el-switch__core) {
  background-color: var(--ad-accent, #007aff);
  border-color: var(--ad-accent, #007aff);
}

@media (max-width: 768px) {
  .header-actions,
  .form-actions {
    justify-content: flex-start;
  }

  .asset-field {
    grid-template-columns: 1fr;
  }
}
</style>
