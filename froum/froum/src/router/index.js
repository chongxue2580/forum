import { createRouter, createWebHistory } from 'vue-router'
import { userApi } from '../api/userApi'

const routes = [
  {
    path: '/',
    name: 'Home',
    component: () => import('../views/Home.vue')
  },
  {
    path: '/articles',
    name: 'Articles',
    component: () => import('../views/ArticlesPage.vue')
  },
  {
    path: '/article/new',
    name: 'NewArticle',
    component: () => import('../components/ArticleEditor.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/article/:id',
    name: 'ArticleDetail',
    component: () => import('../components/ArticleDetail.vue'),
    props: true
  },
  {
    path: '/article/:id/edit',
    name: 'EditArticle',
    component: () => import('../components/ArticleEditor.vue'),
    props: true,
    meta: { requiresAuth: true }
  },
  {
    path: '/questions',
    name: 'Questions',
    component: () => import('../views/Questions.vue')
  },
  {
    path: '/question/new',
    name: 'NewQuestion',
    component: () => import('../components/QuestionEditor.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/question/:id',
    name: 'QuestionDetail',
    component: () => import('../views/QuestionDetail.vue'),
    props: true
  },
  {
    path: '/question/:id/edit',
    name: 'EditQuestion',
    component: () => import('../components/QuestionEditor.vue'),
    props: route => ({ questionId: route.params.id }),
    meta: { requiresAuth: true }
  },
  {
    path: '/categories',
    name: 'Categories',
    component: () => import('../views/Categories.vue')
  },
  {
    path: '/category/:categoryId',
    name: 'CategoryDetail',
    component: () => import('../views/CategoryDetail.vue'),
    props: true
  },
  {
    path: '/tags',
    name: 'Tags',
    component: () => import('../views/Tags.vue')
  },
  {
    path: '/tag/:tagId',
    name: 'TagDetail',
    component: () => import('../views/TagDetail.vue'),
    props: true
  },
  {
    path: '/user/likes',
    name: 'UserLikes',
    component: () => import('../views/UserLikes.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/user/follows',
    name: 'UserFollows',
    component: () => import('../views/UserFollows.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/user/:id',
    name: 'UserProfile',
    component: () => import('../components/UserProfile.vue'),
    props: true
  },
  {
    path: '/following',
    name: 'Following',
    redirect: '/user/follows',
    meta: { requiresAuth: true }
  },
  {
    path: '/notifications',
    name: 'Notifications',
    component: () => import('../components/NotificationList.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/messages',
    name: 'Messages',
    component: () => import('../views/Messages.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/settings',
    name: 'Settings',
    component: () => import('../views/Settings.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/Login.vue')
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('../views/Register.vue')
  },
  {
    path: '/forgot-password',
    name: 'ForgotPassword',
    component: () => import('../views/ForgotPassword.vue')
  },
  {
    path: '/oauth/callback',
    name: 'OAuthCallback',
    component: () => import('../views/OAuthCallback.vue')
  },
  {
    path: '/admin',
    name: 'AdminRoot',
    redirect: '/admin/dashboard'
  },
  {
    path: '/admin/login',
    name: 'AdminLogin',
    component: () => import('../views/AdminLogin.vue')
  },
  {
    path: '/admin/dashboard',
    name: 'AdminDashboard',
    component: () => import('../views/AdminView.vue'),
    meta: { requiresAuth: true, requiresAdmin: true }
  },
  {
    path: '/404',
    name: 'NotFound',
    component: () => import('../views/errors/NotFound.vue')
  },
  {
    path: '/403',
    name: 'Forbidden',
    component: () => import('../views/errors/Forbidden.vue')
  },
  {
    path: '/500',
    name: 'ServerError',
    component: () => import('../views/errors/ServerError.vue')
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/404'
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

const isAdminRole = (role) => ['ADMIN', 'SUPER_ADMIN', 'admin'].includes(role)

const clearAuthState = () => {
  localStorage.removeItem('token')
  localStorage.removeItem('userInfo')
  localStorage.removeItem('adminToken')
  localStorage.removeItem('adminUser')
}

const readCachedUser = () => {
  const userInfoJson = localStorage.getItem('userInfo')
  if (!userInfoJson) {
    return null
  }

  try {
    return JSON.parse(userInfoJson)
  } catch (error) {
    clearAuthState()
    return null
  }
}

const validateCurrentUser = async () => {
  const token = localStorage.getItem('token')
  if (!token) {
    return null
  }

  try {
    const response = await userApi.getCurrentUser()
    const currentUser = response?.data || response
    if (!currentUser?.id) {
      throw new Error('登录状态无效')
    }

    localStorage.setItem('userInfo', JSON.stringify(currentUser))
    if (isAdminRole(currentUser.role) && localStorage.getItem('adminToken') === token) {
      localStorage.setItem('adminUser', JSON.stringify(currentUser))
    }
    return currentUser
  } catch (error) {
    clearAuthState()
    return null
  }
}

router.beforeEach(async (to, from, next) => {
  const token = localStorage.getItem('token')
  const shouldValidate = Boolean(token) && (
    to.meta.requiresAuth ||
    to.meta.requiresAdmin ||
    to.path === '/admin/login'
  )
  const userInfo = shouldValidate ? await validateCurrentUser() : readCachedUser()
  const isAuthenticated = Boolean(token && userInfo)
  const isAdmin = isAdminRole(userInfo?.role)

  if (to.meta.requiresAuth && !isAuthenticated) {
    next({
      path: to.meta.requiresAdmin ? '/admin/login' : '/login',
      query: { redirect: to.fullPath }
    })
    return
  }

  if (to.meta.requiresAdmin && !isAdmin) {
    next('/403')
    return
  }

  if (to.path === '/admin/login' && isAuthenticated && isAdmin) {
    next('/admin/dashboard')
    return
  }

  next()
})

export default router
