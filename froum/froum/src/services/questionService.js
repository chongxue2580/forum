import request from '../utils/request'
import { parseComments } from '../utils/commentHelper'

const unwrapPage = (response) => {
  const page = response?.data || {}
  return {
    success: response?.success === true || response?.code === 200,
    message: response?.message,
    data: Array.isArray(page.content) ? page.content : Array.isArray(response?.data) ? response.data : [],
    total: page.totalElements ?? 0,
    totalPages: page.totalPages ?? 1,
    page: (page.number ?? 0) + 1
  }
}

const normalizeQuestion = (question) => {
  const author = question.author || {}
  const tags = Array.isArray(question.tags)
    ? question.tags.map(tag => typeof tag === 'string' ? tag : tag.name).filter(Boolean)
    : []

  const answers = parseComments(question.answers || question.comments || [])
  const bestAnswer = answers.find(answer => answer.isBestAnswer)

  return {
    ...question,
    summary: question.summary || question.content || '',
    answerCount: question.answerCount ?? question.commentCount ?? 0,
    voteCount: question.voteCount ?? question.likeCount ?? 0,
    solved: question.solved ?? question.isSolved ?? false,
    isSolved: question.isSolved ?? question.solved ?? false,
    bestAnswerId: question.bestAnswerId ?? bestAnswer?.id ?? null,
    createTime: question.createTime || question.createdAt,
    tags,
    author: {
      ...author,
      id: author.id,
      name: author.nickname || author.username || author.name || '匿名用户',
      avatar: author.avatarUrl || author.avatar || '/assets/default-avatar.svg'
    },
    answers
  }
}

const toQuestionPayload = (question) => ({
  title: question.title?.trim(),
  content: question.content?.trim(),
  tagIds: Array.isArray(question.tagIds)
    ? question.tagIds.map(Number).filter(Boolean)
    : Array.isArray(question.tags)
      ? question.tags.map(tag => Number(tag?.id ?? tag)).filter(Boolean)
      : []
})

export const questionService = {
  async getQuestions(params = {}) {
    const requestParams = {
      page: params.page || 1,
      pageSize: params.pageSize || params.size || 10
    }
    const keyword = params.keyword || params.search
    if (params.tagId) requestParams.tagId = params.tagId
    if (typeof params.solved === 'boolean') requestParams.solved = params.solved
    if (keyword) requestParams.keyword = keyword
    if (params.sort) requestParams.sort = params.sort

    const response = await request.get('/questions', {
      params: requestParams
    })
    const page = unwrapPage(response)
    page.data = page.data.map(normalizeQuestion)
    return page
  },

  async getPublishedQuestions(page = 0, size = 10) {
    return this.getQuestions({ page: page + 1, pageSize: size })
  },

  async getSolvedQuestions(params = {}) {
    const response = await request.get('/questions/solved', {
      params: {
        page: params.page || 1,
        pageSize: params.pageSize || params.size || 10
      }
    })
    const page = unwrapPage(response)
    page.data = page.data.map(normalizeQuestion)
    return page
  },

  async getUnsolvedQuestions(params = {}) {
    const response = await request.get('/questions/unsolved', {
      params: {
        page: params.page || 1,
        pageSize: params.pageSize || params.size || 10
      }
    })
    const page = unwrapPage(response)
    page.data = page.data.map(normalizeQuestion)
    return page
  },

  async getQuestionById(id) {
    const response = await request.get(`/questions/${id}`)
    if (response?.success && response.data) {
      return {
        ...response,
        data: normalizeQuestion(response.data)
      }
    }
    return response
  },

  async createQuestion(question) {
    return request.post('/questions', toQuestionPayload(question))
  },

  async updateQuestion(id, question) {
    return request.put(`/questions/${id}`, toQuestionPayload(question))
  },

  async deleteQuestion(id) {
    return request.delete(`/questions/${id}`)
  },

  async likeQuestion(questionId) {
    return request.post(`/likes/QUESTION/${questionId}`)
  },

  async unlikeQuestion(questionId) {
    return request.delete(`/likes/QUESTION/${questionId}`)
  },

  async getQuestionLikeInfo(questionId) {
    return request.get(`/likes/QUESTION/${questionId}/info`)
  },

  async getQuestionFollowInfo(questionId) {
    return request.get(`/follows/QUESTION/${questionId}/info`)
  },

  async getQuestionComments(id, params = {}) {
    const response = await request.get(`/comments/question/${id}`, {
      params: {
        page: params.page || 1,
        pageSize: params.pageSize || params.size || 20
      }
    })
    const page = unwrapPage(response)
    page.data = parseComments(page.data)
    return page
  },

  async addAnswer(questionId, content) {
    return this.addComment(questionId, { content })
  },

  async addComment(questionId, comment) {
    return request.post('/comments', {
      targetType: 'QUESTION',
      targetId: Number(questionId),
      content: comment.content || comment,
      parentId: comment.parentId || null
    })
  },

  async addAnswerComment(questionId, answerId, content) {
    return this.addComment(questionId, {
      content,
      parentId: Number(answerId)
    })
  },

  async deleteAnswer(answerId) {
    return request.delete(`/comments/${answerId}`)
  },

  async likeAnswer(answerId) {
    return request.post(`/likes/COMMENT/${answerId}`)
  },

  async unlikeAnswer(answerId) {
    return request.delete(`/likes/COMMENT/${answerId}`)
  },

  async getAnswerLikeInfo(answerId) {
    return request.get(`/likes/COMMENT/${answerId}/info`)
  },

  async followQuestion(questionId) {
    return request.post(`/follows/QUESTION/${questionId}`)
  },

  async unfollowQuestion(questionId) {
    return request.delete(`/follows/QUESTION/${questionId}`)
  },

  async setBestAnswer(questionId, answerId) {
    return request.post(`/questions/${questionId}/best-answer/${answerId}`)
  },

  async unsetBestAnswer(questionId) {
    return request.delete(`/questions/${questionId}/best-answer`)
  },

  async getQuestionsByTag(tagId, params = {}) {
    return this.getQuestions({ ...params, tagId })
  },

  async searchQuestions(keyword, params = {}) {
    const response = await request.get('/questions/search', {
      params: {
        keyword,
        page: params.page || 1,
        pageSize: params.pageSize || params.size || 10
      }
    })
    const page = unwrapPage(response)
    page.data = page.data.map(normalizeQuestion)
    return page
  },

  async getUserQuestions(userId, page = 0, size = 10) {
    const response = await request.get(`/questions/user/${userId}`, {
      params: { page, size }
    })
    const pageData = unwrapPage(response)
    pageData.data = pageData.data.map(normalizeQuestion)
    return pageData
  },

  async getQuestionsBySolvedStatus(solved, page = 0, size = 10) {
    return solved
      ? this.getSolvedQuestions({ page: page + 1, pageSize: size })
      : this.getUnsolvedQuestions({ page: page + 1, pageSize: size })
  }
}
