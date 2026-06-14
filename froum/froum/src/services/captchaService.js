import request from '../utils/request'

export const captchaService = {
  async generate() {
    const response = await request.get('/captcha/generate')
    return response.data
  }
}
