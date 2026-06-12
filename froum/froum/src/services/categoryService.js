import request from '../utils/request'

export const categoryService = {
  async getAllCategories() {
    return request.get('/categories')
  },

  async getCategories() {
    return this.getAllCategories()
  },

  async getCategoryById(id) {
    return request.get(`/categories/${id}`)
  },

  async createCategory(category) {
    return request.post('/categories', category)
  },

  async updateCategory(id, category) {
    return request.put(`/categories/${id}`, category)
  },

  async deleteCategory(id) {
    return request.delete(`/categories/${id}`)
  },

  async getArticlesByCategory(categoryId, page = 0, size = 10) {
    return request.get(`/categories/${categoryId}/articles`, {
      params: { page, size }
    })
  },

  async getChildCategories(parentId) {
    return request.get(`/categories/${parentId}/children`)
  },

  async getSubcategories(categoryId) {
    return request.get(`/categories/${categoryId}/subcategories`)
  },

  async createSubcategory(categoryId, subcategory) {
    return request.post(`/categories/${categoryId}/subcategories`, subcategory)
  },

  async updateSubcategory(categoryId, subcategoryId, subcategory) {
    return request.put(`/categories/${categoryId}/subcategories/${subcategoryId}`, subcategory)
  },

  async deleteSubcategory(categoryId, subcategoryId) {
    return request.delete(`/categories/${categoryId}/subcategories/${subcategoryId}`)
  }
}
