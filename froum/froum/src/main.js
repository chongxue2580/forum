import './assets/main.css'

import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import store from './store'
import { marked } from 'marked'
import hljs from 'highlight.js'
import 'highlight.js/styles/github.css'

// Element Plus
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'

// 导入全局组件
import UserCard from './components/UserCard.vue'
import QuestionItem from './components/QuestionItem.vue'
import ArticleCard from './components/ArticleCard.vue'
import ArticleList from './components/ArticleList.vue'

import { FontAwesomeIcon } from './plugins/fontawesome'

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

// 创建 Vue 应用实例
const app = createApp(App)

// 注册 Element Plus
app.use(ElementPlus)

// 注册 Element Plus 图标
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

// 注册全局属性
app.config.globalProperties.$marked = marked

// 注册全局指令
app.directive('highlight', {
  mounted(el) {
    hljs.highlightElement(el)
  },
  updated(el) {
    hljs.highlightElement(el)
  }
})

// 注册全局组件
app.component('font-awesome-icon', FontAwesomeIcon)
app.component('user-card', UserCard)
app.component('question-item', QuestionItem)
app.component('article-card', ArticleCard)
app.component('article-list', ArticleList)

// 错误处理
app.config.errorHandler = (err, vm, info) => {
  console.error('Vue Error:', err)
  console.error('Error Info:', info)
  store.commit('SET_ERROR', '应用发生错误，请刷新页面重试')
}

// 使用插件
app.use(router)
app.use(store)

// 挂载应用
app.mount('#app')
