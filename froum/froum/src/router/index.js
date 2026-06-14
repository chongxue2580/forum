import { createRouter, createWebHistory } from 'vue-router'

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

router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  const userInfoJson = localStorage.getItem('userInfo')
  const isAuthenticated = Boolean(token && userInfoJson)
  let isAdmin = false

  if (isAuthenticated) {
    try {
      const userInfo = JSON.parse(userInfoJson)
      isAdmin = userInfo.role === 'ADMIN' || userInfo.role === 'SUPER_ADMIN' || userInfo.role === 'admin'
    } catch (error) {
      localStorage.removeItem('token')
      localStorage.removeItem('userInfo')
      localStorage.removeItem('adminToken')
      localStorage.removeItem('adminUser')
    }
  }

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
