import './assets/main.css'
import './assets/fontawesome.css'

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

// FontAwesome setup
import { library } from '@fortawesome/fontawesome-svg-core'
import { FontAwesomeIcon } from '@fortawesome/vue-fontawesome'
import {
  faSearch, faHome, faThLarge, faTags, faQuestionCircle,
  faSignInAlt, faEdit, faUser, faCog, faSignOutAlt,
  faExclamationTriangle, faTimes, faFolder, faTag, faCalendar,
  faEye, faThumbsUp, faComment, faShieldAlt, faIdBadge,
  faEnvelope, faMapMarkerAlt, faLink, faSpinner, faLock,
  faEyeSlash, faUpload, faBell, faUserPlus, faChevronDown,
  faChevronUp, faHeart, faFileAlt, faArrowRight, faPhone,
  faCalendarAlt, faFlag, faShare, faPlus, faCheckCircle, faClock,
  faUserMinus, faReply, faTrashAlt, faEllipsisH, faSyncAlt,
  faCommentDots, faCommentAlt, faSync, faList, faUserCheck,
  faPaperPlane, faCommentSlash, faExclamationCircle, faUserSlash,
  faUsers, faBars, faInfoCircle, faChartLine, faTachometerAlt,
  faTrash, faSave, faUndo, faRedo, faImage, faCode, faBold,
  faItalic, faHeading, faQuoteRight, faListUl, faListOl,
  faTasks, faChevronLeft, faChevronRight, faCheck, faUserShield,
  faUserFriends, faDownload, faFilter, faTable, faThumbtack, faStar,
  faTimesCircle, faChartBar, faClipboardList
} from '@fortawesome/free-solid-svg-icons'

import {
  faGithub, faTwitter, faWeixin, faFacebook, faLinkedin, faWeibo, faGoogle
} from '@fortawesome/free-brands-svg-icons'

import {
  faHeart as farHeart,
  faComment as farComment,
  faUser as farUser,
  faBell as farBell,
  faEye as farEye
} from '@fortawesome/free-regular-svg-icons'

// Register Font Awesome icons
library.add(
  faSearch, faHome, faThLarge, faTags, faQuestionCircle,
  faSignInAlt, faEdit, faUser, faCog, faSignOutAlt,
  faExclamationTriangle, faTimes, faFolder, faTag, faCalendar,
  faEye, faThumbsUp, faComment, faShieldAlt, faIdBadge,
  faEnvelope, faMapMarkerAlt, faLink, faSpinner, faLock,
  faEyeSlash, faUpload, faBell, faGithub, faUserPlus,
  faChevronDown, faChevronUp, faHeart, faFileAlt, faArrowRight,
  faTwitter, faWeixin, faPhone, faCalendarAlt, faFlag, faShare,
  faPlus, faCheckCircle, faClock, faUserMinus, faReply,
  faTrashAlt, faEllipsisH, faSyncAlt, faCommentDots, faCommentAlt,
  faSync, faList, faUserCheck, faPaperPlane, faCommentSlash,
  faExclamationCircle, faUserSlash, faUsers, faBars, faInfoCircle,
  faChartLine, faTachometerAlt, faTrash, faSave, faUndo, faRedo,
  faImage, faCode, faBold, faItalic, faHeading, faQuoteRight,
  faListUl, faListOl, faTasks, faChevronLeft, faChevronRight,
  faCheck, faUserShield, faUserFriends, faFacebook, faLinkedin, faWeibo, faGoogle,
  faDownload, faFilter, faTable, faThumbtack, faStar, faTimesCircle,
  faChartBar, faClipboardList,
  // Regular icons
  farHeart, farComment, farUser, farBell, farEye
)

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
