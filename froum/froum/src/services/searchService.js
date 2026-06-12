import request from '../utils/request'

export const searchService = {
  async search(keyword, type = 'all', page = 1, pageSize = 10) {
    if (type === 'articles') {
      return this.searchArticles(keyword, page, pageSize)
    }
    if (type === 'questions') {
      return this.searchQuestions(keyword, page, pageSize)
    }
    return request.get('/search/all', {
      params: { keyword, page, pageSize }
    })
  },

  async searchArticles(keyword, page = 1, pageSize = 10) {
    return request.get('/search/articles', {
      params: { keyword, page, pageSize }
    })
  },

  async searchQuestions(keyword, page = 1, pageSize = 10) {
    return request.get('/search/questions', {
      params: { keyword, page, pageSize }
    })
  },

  async searchUsers() {
    throw new Error('当前后端暂未提供用户搜索接口')
  }
}
