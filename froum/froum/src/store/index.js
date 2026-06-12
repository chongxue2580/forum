import { createStore } from 'vuex'
import { articleService } from '../services/articleService'
import { categoryService } from '../services/categoryService'
import { tagService } from '../services/tagService'
import { userApi } from '../api/userApi'
import { likeApi } from '../api/likeApi'
import { followApi } from '../api/followApi'
import { parseComments } from '../utils/commentHelper'

const articleApi = articleService
const categoryApi = categoryService
const tagApi = tagService

export default createStore({
  state: {
    // 用户状态
    user: null,
    isAuthenticated: false,
    
    // 分类和标签
    categories: [],
    popularTags: [],
    
    // 文章相关
    articles: [],
    articleCount: 0,
    currentArticle: null,

    // 用户关注相关
    followers: [],
    following: [],

    // 点赞相关
    likedArticles: new Set(),
    likedQuestions: new Set(),
    likedComments: new Set(),

    // 关注相关
    followedUsers: new Set(),
    followedQuestions: new Set(),

    // UI状态
    loading: false,
    error: null
  },
  
  getters: {
    // 用户相关
    isAdmin: state => state.user?.role === 'admin',
    
    // 分类相关
    categoryById: state => id => state.categories.find(c => c.id === id),
    subcategoriesByParentId: state => parentId => {
      const category = state.categories.find(c => c.id === parentId)
      return category ? category.subcategories : []
    },
    
    // 文章相关
    articleById: state => id => state.articles.find(a => a.id === id),

    // 点赞相关
    isArticleLiked: state => articleId => state.likedArticles.has(articleId),
    isQuestionLiked: state => questionId => state.likedQuestions.has(questionId),
    isCommentLiked: state => commentId => state.likedComments.has(commentId),

    // 关注相关
    isFollowing: state => userId => state.following.some(user => user.id === userId),
    isUserFollowed: state => userId => state.followedUsers.has(userId),
    isQuestionFollowed: state => questionId => state.followedQuestions.has(questionId),

    // 加载状态
    isLoading: state => state.loading,
    hasError: state => !!state.error
  },
  
  mutations: {
    // 用户相关
    SET_USER(state, user) {
      state.user = user
      state.isAuthenticated = !!user
    },

    SET_AUTHENTICATED(state, isAuthenticated) {
      state.isAuthenticated = isAuthenticated
    },
    
    // 分类相关
    SET_CATEGORIES(state, categories) {
      state.categories = categories
    },
    ADD_CATEGORY(state, category) {
      state.categories.push(category)
    },
    UPDATE_CATEGORY(state, { id, category }) {
      const index = state.categories.findIndex(c => c.id === id)
      if (index !== -1) {
        state.categories[index] = { ...state.categories[index], ...category }
      }
    },
    REMOVE_CATEGORY(state, id) {
      state.categories = state.categories.filter(c => c.id !== id)
    },
    
    // 标签相关
    SET_POPULAR_TAGS(state, tags) {
      state.popularTags = tags
    },
    
    // 文章相关
    SET_ARTICLES(state, { data, total }) {
      console.log('SET_ARTICLES mutation called with:', { 
        dataLength: data?.length || 0, 
        total 
      });
      
      // 确保 data 是数组
      if (Array.isArray(data)) {
        state.articles = data;
      } else {
        console.warn('SET_ARTICLES received non-array data:', data);
        state.articles = [];
      }
      
      // 确保 total 是数字
      if (typeof total === 'number') {
        state.articleCount = total;
      } else if (state.articles && Array.isArray(state.articles)) {
        // 如果 total 不是数字，使用文章数组长度作为总数
        state.articleCount = state.articles.length;
        console.warn('SET_ARTICLES using articles length as count:', state.articleCount);
      } else {
        state.articleCount = 0;
      }
      
      console.log('SET_ARTICLES mutation completed:', { 
        articlesInStore: state.articles.length, 
        articleCount: state.articleCount 
      });
    },
    SET_CURRENT_ARTICLE(state, article) {
      state.currentArticle = article
    },
    ADD_ARTICLE(state, article) {
      state.articles.unshift(article)
      state.articleCount++
    },
    UPDATE_ARTICLE(state, updatedArticle) {
      const id = updatedArticle.id
      const index = state.articles.findIndex(a => a.id === id)
      if (index !== -1) {
        state.articles[index] = updatedArticle
      }
      if (state.currentArticle?.id === id) {
        state.currentArticle = updatedArticle
      }
    },
    REMOVE_ARTICLE(state, id) {
      state.articles = state.articles.filter(a => a.id !== id)
      state.articleCount--
      if (state.currentArticle?.id === id) {
        state.currentArticle = null
      }
    },
    
    // UI状态
    SET_LOADING(state, loading) {
      state.loading = loading
    },
    SET_ERROR(state, error) {
      state.error = error
    },
    
    // 关注相关
    SET_FOLLOWERS(state, followers) {
      state.followers = followers
    },
    SET_FOLLOWING(state, following) {
      state.following = following
    },
    ADD_FOLLOWING(state, user) {
      state.following.push(user)
    },
    REMOVE_FOLLOWING(state, userId) {
      state.following = state.following.filter(user => user.id !== userId)
    },

    // 点赞相关mutations
    SET_LIKED_ARTICLES(state, articleIds) {
      state.likedArticles = new Set(articleIds)
    },
    ADD_LIKED_ARTICLE(state, articleId) {
      state.likedArticles.add(articleId)
    },
    REMOVE_LIKED_ARTICLE(state, articleId) {
      state.likedArticles.delete(articleId)
    },
    SET_LIKED_QUESTIONS(state, questionIds) {
      state.likedQuestions = new Set(questionIds)
    },
    ADD_LIKED_QUESTION(state, questionId) {
      state.likedQuestions.add(questionId)
    },
    REMOVE_LIKED_QUESTION(state, questionId) {
      state.likedQuestions.delete(questionId)
    },
    SET_LIKED_COMMENTS(state, commentIds) {
      state.likedComments = new Set(commentIds)
    },
    ADD_LIKED_COMMENT(state, commentId) {
      state.likedComments.add(commentId)
    },
    REMOVE_LIKED_COMMENT(state, commentId) {
      state.likedComments.delete(commentId)
    },

    // 关注相关mutations
    SET_FOLLOWED_USERS(state, userIds) {
      state.followedUsers = new Set(userIds)
    },
    ADD_FOLLOWED_USER(state, userId) {
      state.followedUsers.add(userId)
    },
    REMOVE_FOLLOWED_USER(state, userId) {
      state.followedUsers.delete(userId)
    },
    SET_FOLLOWED_QUESTIONS(state, questionIds) {
      state.followedQuestions = new Set(questionIds)
    },
    ADD_FOLLOWED_QUESTION(state, questionId) {
      state.followedQuestions.add(questionId)
    },
    REMOVE_FOLLOWED_QUESTION(state, questionId) {
      state.followedQuestions.delete(questionId)
    }
  },
  
  actions: {
    // 分类相关
    async fetchCategories({ commit }) {
      try {
        console.log('Fetching categories...')
        commit('SET_LOADING', true)
        const response = await categoryApi.getCategories()
        console.log('Categories response:', response)

        // 处理后端返回的数据格式
        const categories = response.success ? response.data : response
        console.log('Categories processed:', categories)
        commit('SET_CATEGORIES', categories)
        return categories
      } catch (error) {
        console.error('Error fetching categories:', error)
        commit('SET_ERROR', '获取分类列表失败')
      } finally {
        commit('SET_LOADING', false)
      }
    },
    
    async createCategory({ commit }, category) {
      try {
        commit('SET_LOADING', true)
        const newCategory = await categoryApi.createCategory(category)
        commit('ADD_CATEGORY', newCategory)
        return newCategory
      } catch (error) {
        commit('SET_ERROR', '创建分类失败')
        throw error
      } finally {
        commit('SET_LOADING', false)
      }
    },
    
    // 标签相关
    async fetchPopularTags({ commit }) {
      try {
        console.log('Fetching popular tags...')
        commit('SET_LOADING', true)
        const response = await tagApi.getPopularTags()
        console.log('Popular tags response:', response)

        // 处理后端返回的数据格式
        const tags = response.success ? response.data : response
        console.log('Popular tags processed:', tags)
        commit('SET_POPULAR_TAGS', tags)
        return tags
      } catch (error) {
        console.error('Error fetching popular tags:', error)
        commit('SET_ERROR', '获取热门标签失败')
      } finally {
        commit('SET_LOADING', false)
      }
    },
    
    // 文章相关
    async fetchArticles({ commit }, params) {
      try {
        console.log('Fetching articles with params:', params)
        commit('SET_LOADING', true)

        // 调用API获取文章数据
        const response = await articleApi.getArticles(params)
        console.log('Articles response:', response)

        // 处理后端返回的数据格式
        let articlesData = []
        let totalCount = 0

        if (response.success) {
          articlesData = response.data || []
          totalCount = response.totalElements || response.total || 0
        } else if (response.data) {
          articlesData = response.data
          totalCount = response.total || 0
        }

        console.log('Articles processed:', {
          count: articlesData.length,
          total: totalCount
        })

        // 更新状态
        commit('SET_ARTICLES', {
          data: articlesData,
          total: totalCount
        })

        return {
          data: articlesData,
          total: totalCount,
          totalPages: response.totalPages || Math.ceil(totalCount / (params?.size || 10)),
          page: params?.page || 1
        }
      } catch (error) {
        console.error('Error fetching articles:', error)
        commit('SET_ERROR', '获取文章列表失败')
      } finally {
        commit('SET_LOADING', false)
      }
    },
    
    async fetchArticleById({ commit }, id) {
      try {
        commit('SET_LOADING', true)
        const article = await articleApi.getArticleById(id)
        
        // Ensure article.comments is an array using our helper function
        article.comments = parseComments(article.comments);
        
        commit('SET_CURRENT_ARTICLE', article)
        return article
      } catch (error) {
        commit('SET_ERROR', '获取文章详情失败')
        throw error
      } finally {
        commit('SET_LOADING', false)
      }
    },
    
    async createArticle({ commit }, article) {
      try {
        commit('SET_LOADING', true)
        console.log('Vuex: 创建文章', article)
        const newArticle = await articleApi.createArticle(article)
        console.log('Vuex: 文章创建成功', newArticle)
        commit('ADD_ARTICLE', newArticle)
        return newArticle
      } catch (error) {
        console.error('Vuex: 创建文章失败', error)
        commit('SET_ERROR', error.message || '创建文章失败')
        throw error
      } finally {
        commit('SET_LOADING', false)
      }
    },

    async updateArticle({ commit }, { id, article }) {
      try {
        commit('SET_LOADING', true)
        console.log('Vuex: 更新文章', { id, article })
        const updatedArticle = await articleApi.updateArticle(id, article)
        console.log('Vuex: 文章更新成功', updatedArticle)
        commit('UPDATE_ARTICLE', updatedArticle)
        return updatedArticle
      } catch (error) {
        console.error('Vuex: 更新文章失败', error)
        commit('SET_ERROR', error.message || '更新文章失败')
        throw error
      } finally {
        commit('SET_LOADING', false)
      }
    },
    
    // 获取热门文章
    async fetchHotArticles({ commit }, params) {
      try {
        commit('SET_LOADING', true);
        const response = await articleApi.getHotArticles(params);
        console.log('Hot articles response:', response);

        // 处理后端返回的数据格式
        const articlesData = response.success ? response.data : response.data || []

        return {
          data: articlesData,
          total: response.totalElements || response.total || articlesData.length
        };
      } catch (error) {
        console.error('Error fetching hot articles:', error);
        commit('SET_ERROR', '获取热门文章失败');
        throw error;
      } finally {
        commit('SET_LOADING', false);
      }
    },

    // 获取推荐文章
    async fetchRecommendedArticles({ commit }, params) {
      try {
        commit('SET_LOADING', true);
        const response = await articleApi.getRecommendedArticles(params);
        console.log('Recommended articles response:', response);

        // 处理后端返回的数据格式
        const articlesData = response.success ? response.data : response.data || []

        return {
          data: articlesData,
          total: response.totalElements || response.total || articlesData.length
        };
      } catch (error) {
        console.error('Error fetching recommended articles:', error);
        commit('SET_ERROR', '获取推荐文章失败');
        throw error;
      } finally {
        commit('SET_LOADING', false);
      }
    },
    
    // UI相关
    clearError({ commit }) {
      commit('SET_ERROR', null)
    },
    
    // 管理员登录
    async adminLogin({ commit }, { username, password }) {
      try {
        commit('SET_LOADING', true)
        commit('SET_ERROR', null)

        console.log('管理员登录尝试:', { username })

        // 调用真实的后端API
        const { adminLogin } = await import('../api/admin.js')
        const response = await adminLogin({ username, password })

        console.log('管理员登录响应:', response)

        // 检查响应格式
        if (response && (response.code === 200 || response.code === 1) && response.data) {
          const { token, user } = response.data

          console.log('管理员登录成功:', user)

          // 保存token和用户信息
          localStorage.setItem('token', token)
          localStorage.setItem('adminToken', token)
          localStorage.setItem('userInfo', JSON.stringify(user))
          localStorage.setItem('adminUser', JSON.stringify(user))

          commit('SET_USER', user)
          commit('SET_AUTHENTICATED', true)
          return { success: true }
        } else {
          // 登录失败
          const errorMessage = response?.message || response?.msg || '管理员登录失败'
          console.error('管理员登录失败:', errorMessage)
          throw new Error(errorMessage)
        }
      } catch (error) {
        console.error('管理员登录错误:', error)
        const errorMessage = error.response?.data?.message || error.message || '管理员登录失败'
        commit('SET_ERROR', errorMessage)
        throw new Error(errorMessage)
      } finally {
        commit('SET_LOADING', false)
      }
    },
    
    async login({ commit }, { username, password }) {
      try {
        commit('SET_LOADING', true)
        commit('SET_ERROR', null)

        console.log('尝试登录:', { username })

        // 使用真实API登录
        const response = await userApi.login({ username, password })
        console.log('登录响应:', response)

        // 检查响应格式 - 后端返回的格式是 { success: true, message: "登录成功", data: { token, user } }
        if (response && response.success === true && response.data) {
          const { token, user } = response.data

          console.log('普通用户登录成功:', user)

          // 保存真实的JWT token和用户信息
          localStorage.setItem('token', token)
          localStorage.setItem('userInfo', JSON.stringify(user))

          commit('SET_USER', user)
          commit('SET_AUTHENTICATED', true)
          return user
        } else {
          // 登录失败
          const errorMessage = response?.message || response?.msg || '登录失败'
          console.error('普通用户登录失败:', errorMessage)
          throw new Error(errorMessage)
        }
      } catch (error) {
        console.error('登录错误:', error)
        const errorMessage = error.response?.data?.message || error.message || '登录失败'
        commit('SET_ERROR', errorMessage)
        throw new Error(errorMessage)
      } finally {
        commit('SET_LOADING', false)
      }
    },

    async register({ commit }, { username, email, password }) {
      try {
        commit('SET_LOADING', true)
        commit('SET_ERROR', null)

        console.log('尝试注册:', { username, email })

        // 调用注册API
        const response = await userApi.register({
          username,
          email,
          password
        })
        console.log('注册响应:', response)

        if (response.success && response.data) {
          const user = response.data
          console.log('注册成功:', user)
          return user
        } else {
          throw new Error(response.message || '注册失败')
        }
      } catch (error) {
        console.error('注册错误:', error)
        const errorMessage = error.response?.data?.message || error.message || '注册失败'
        commit('SET_ERROR', errorMessage)
        throw new Error(errorMessage)
      } finally {
        commit('SET_LOADING', false)
      }
    },

    logout({ commit }) {
      localStorage.removeItem('token')
      localStorage.removeItem('userInfo')
      commit('SET_USER', null)
    },
    
    checkAuth({ commit }) {
      const token = localStorage.getItem('token')
      const userInfoJson = localStorage.getItem('userInfo')

      if (token && userInfoJson) {
        try {
          const userInfo = JSON.parse(userInfoJson)
          commit('SET_USER', userInfo)
          commit('SET_AUTHENTICATED', true)
        } catch (e) {
          console.error('解析用户信息失败:', e)
          localStorage.removeItem('token')
          localStorage.removeItem('userInfo')
          localStorage.removeItem('adminToken')
          localStorage.removeItem('adminUser')
          commit('SET_USER', null)
          commit('SET_AUTHENTICATED', false)
        }
      } else {
        commit('SET_USER', null)
        commit('SET_AUTHENTICATED', false)
      }
    },
    
    async fetchUserProfile({ commit }, { userId }) {
      try {
        commit('SET_LOADING', true);
        
        if (!userId) {
          throw new Error('无效的用户ID');
        }
        
        // 调用API获取用户资料
        const userProfile = await userApi.getUserProfile(userId);

        const userStats = await userApi.getUserStats(userId);

        // 提取实际的用户数据（处理API响应格式）
        const userData = userProfile.data || userProfile;
        const statsData = userStats.data || userStats;

        // 转换后端返回的用户对象为前端需要的格式
        const formattedProfile = {
          id: userData.id || userId,
          name: userData.nickname || userData.username || '',
          username: userData.username || '',
          avatar: userData.avatarUrl,
          bio: userData.bio || '',
          email: userData.email || '',
          website: userData.website || '',
          github: userData.github || '',
          joinTime: userData.createdAt || null,
          lastLogin: userData.lastLoginTime,
          stats: statsData,
          isFollowing: userData.isFollowing || false
        };
        
        return formattedProfile;
      } catch (error) {
        console.error('Error fetching user profile:', error);
        commit('SET_ERROR', error.message || '获取用户资料失败');
        throw error;
      } finally {
        commit('SET_LOADING', false);
      }
    },
    
    async updateUserProfile({ commit, state }, profileData) {
      try {
        commit('SET_LOADING', true)
        const userId = state.user?.id || profileData.id
        if (!userId) {
          throw new Error('无法确定当前用户ID')
        }

        const response = await userApi.updateUserProfile(userId, profileData)
        const updatedUser = response.data || response
        commit('SET_USER', updatedUser)
        localStorage.setItem('userInfo', JSON.stringify(updatedUser))
        
        return updatedUser
      } catch (error) {
        commit('SET_ERROR', error.message || '更新个人资料失败')
        throw error
      } finally {
        commit('SET_LOADING', false)
      }
    },
    
    async updatePassword({ commit }, { currentPassword, newPassword }) {
      try {
        commit('SET_LOADING', true)
        const userInfo = JSON.parse(localStorage.getItem('userInfo') || '{}')
        if (!userInfo.id) {
          throw new Error('无法确定当前用户ID')
        }

        return await userApi.updatePassword(userInfo.id, { currentPassword, newPassword })
      } catch (error) {
        commit('SET_ERROR', error.message || '更新密码失败')
        throw error
      } finally {
        commit('SET_LOADING', false)
      }
    },
    
    // 关注相关
    async fetchFollowers({ commit }, { userId }) {
      try {
        commit('SET_LOADING', true)

        // 调用真实API获取粉丝列表
        const followers = await userApi.getUserFollowers(userId, 0, 20)

        commit('SET_FOLLOWERS', followers)
        return followers
      } catch (error) {
        console.error('获取粉丝列表失败:', error)
        commit('SET_ERROR', '获取粉丝列表失败')
        throw error
      } finally {
        commit('SET_LOADING', false)
      }
    },
    
    async fetchFollowing({ commit }, { userId }) {
      try {
        commit('SET_LOADING', true)

        // 调用真实API获取关注列表
        const following = await userApi.getUserFollowing(userId, 0, 20)

        commit('SET_FOLLOWING', following)
        return following
      } catch (error) {
        console.error('获取关注列表失败:', error)
        commit('SET_ERROR', '获取关注列表失败')
        throw error
      } finally {
        commit('SET_LOADING', false)
      }
    },
    
    async toggleFollow({ commit }, { userId }) {
      try {
        commit('SET_LOADING', true);

        // 调用真实API切换关注状态
        const result = await userApi.toggleFollow(userId);

        if (result && result.success) {
          // 可以在这里触发其他相关的状态更新
          // 比如更新用户的关注数等
        }

        return result;
      } catch (error) {
        console.error('切换关注状态失败:', error);
        commit('SET_ERROR', '操作失败，请稍后重试');
        throw error;
      } finally {
        commit('SET_LOADING', false);
      }
    },



    // 获取用户统计信息
    async fetchUserStats({ commit }, { userId }) {
      try {
        const stats = await userApi.getUserStats(userId);
        return stats;
      } catch (error) {
        console.error('获取用户统计失败:', error);
        commit('SET_ERROR', error.message || '获取用户统计失败');
        throw error;
      }
    },

    // 获取用户问题列表
    async getUserQuestions({ commit }, { userId, page = 0, size = 10 }) {
      try {
        const response = await userApi.getUserQuestions(userId, page, size);
        // 处理API响应格式
        const questions = response.data || response;
        return questions;
      } catch (error) {
        console.error('获取用户问题失败:', error);
        throw error;
      }
    },

    // 获取用户回答列表
    async getUserAnswers({ commit }, { userId, page = 0, size = 10 }) {
      try {
        const response = await userApi.getUserAnswers(userId, page, size);
        // 处理API响应格式
        const answers = response.data || response;
        return answers;
      } catch (error) {
        console.error('获取用户回答失败:', error);
        throw error;
      }
    },

    // 获取用户文章列表
    async getUserArticles({ commit }, { userId, page = 0, size = 10 }) {
      try {
        const response = await userApi.getUserArticles(userId, page, size);
        // 处理API响应格式
        const articles = response.data || response;
        return articles;
      } catch (error) {
        console.error('获取用户文章失败:', error);
        throw error;
      }
    },

    // 检查关注状态
    async checkFollowStatus({ commit }, { targetUserId }) {
      try {
        const isFollowing = await userApi.checkFollowStatus(targetUserId);
        return isFollowing;
      } catch (error) {
        console.error('检查关注状态失败:', error);
        throw error;
      }
    },

    async updatePrivacySettings({ commit }, settings) {
      try {
        commit('SET_LOADING', true)
        throw new Error('当前后端暂未提供隐私设置接口')
      } catch (error) {
        commit('SET_ERROR', error.message || '更新隐私设置失败')
        throw error
      } finally {
        commit('SET_LOADING', false)
      }
    },
    
    // 文章点赞
    async likeArticle({ commit, state }, articleId) {
      try {
        commit('SET_LOADING', true)
        
        // 调用API
        await articleApi.likeArticle(articleId)
        
        // 如果成功，更新文章
        const articles = [...state.articles]
        const index = articles.findIndex(a => a.id === articleId)
        if (index !== -1) {
          articles[index] = {
            ...articles[index],
            likes: (articles[index].likes || 0) + 1
          }
          commit('SET_ARTICLES', { data: articles, total: state.articleCount })
        }
        
        // 如果当前文章也是正在查看的文章，也更新它
        if (state.currentArticle && state.currentArticle.id === articleId) {
          commit('SET_CURRENT_ARTICLE', {
            ...state.currentArticle,
            likes: (state.currentArticle.likes || 0) + 1
          })
        }
        
        return true
      } catch (error) {
        console.error('Error liking article:', error)
        commit('SET_ERROR', '点赞失败')
        throw error
      } finally {
        commit('SET_LOADING', false)
      }
    },
    // 点赞相关actions
    async toggleLike({ commit }, { targetType, targetId }) {
      try {
        commit('SET_LOADING', true)
        const result = await likeApi.toggleLike(targetType, targetId)

        if (result.success) {
          const mutationMap = {
            'ARTICLE': result.isLiked ? 'ADD_LIKED_ARTICLE' : 'REMOVE_LIKED_ARTICLE',
            'QUESTION': result.isLiked ? 'ADD_LIKED_QUESTION' : 'REMOVE_LIKED_QUESTION',
            'COMMENT': result.isLiked ? 'ADD_LIKED_COMMENT' : 'REMOVE_LIKED_COMMENT'
          }

          const mutation = mutationMap[targetType.toUpperCase()]
          if (mutation) {
            commit(mutation, targetId)
          }
        }

        return result
      } catch (error) {
        console.error('切换点赞状态失败:', error)
        commit('SET_ERROR', '操作失败，请稍后重试')
        throw error
      } finally {
        commit('SET_LOADING', false)
      }
    },

    async getLikeInfo({ commit }, { targetType, targetId }) {
      try {
        const info = await likeApi.getLikeInfo(targetType, targetId)
        return info
      } catch (error) {
        console.error('获取点赞信息失败:', error)
        throw error
      }
    },

    // 关注相关actions
    async toggleFollowTarget({ commit }, { targetType, targetId }) {
      try {
        commit('SET_LOADING', true)
        const result = await followApi.toggleFollow(targetType, targetId)

        if (result.success) {
          const mutationMap = {
            'USER': result.isFollowed ? 'ADD_FOLLOWED_USER' : 'REMOVE_FOLLOWED_USER',
            'QUESTION': result.isFollowed ? 'ADD_FOLLOWED_QUESTION' : 'REMOVE_FOLLOWED_QUESTION'
          }

          const mutation = mutationMap[targetType.toUpperCase()]
          if (mutation) {
            commit(mutation, targetId)
          }
        }

        return result
      } catch (error) {
        console.error('切换关注状态失败:', error)
        commit('SET_ERROR', '操作失败，请稍后重试')
        throw error
      } finally {
        commit('SET_LOADING', false)
      }
    },

    async getFollowInfo({ commit }, { targetType, targetId }) {
      try {
        const info = await followApi.getFollowInfo(targetType, targetId)
        return info
      } catch (error) {
        console.error('获取关注信息失败:', error)
        throw error
      }
    },

    // 获取当前用户的关注统计
    async fetchFollowStats({ commit }) {
      try {
        const stats = await followApi.getFollowStats()
        return stats
      } catch (error) {
        console.error('获取关注统计失败:', error)
        throw error
      }
    },

    // 初始化用户点赞和关注状态
    async initUserLikeAndFollowStatus({ commit }) {
      try {
        // 这里可以批量获取用户的点赞和关注状态
        // 暂时留空，后续可以根据需要实现
      } catch (error) {
        console.error('初始化用户状态失败:', error)
      }
    },
  }
})
