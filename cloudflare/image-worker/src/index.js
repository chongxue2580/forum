const DEFAULT_CACHE_TTL = 31536000
const IMAGE_EXTENSIONS = new Set(['jpg', 'jpeg', 'png', 'gif', 'webp', 'avif', 'svg'])

export default {
  async fetch(request, env, ctx) {
    if (request.method !== 'GET' && request.method !== 'HEAD' && request.method !== 'OPTIONS') {
      return new Response('Method Not Allowed', { status: 405 })
    }

    if (request.method === 'OPTIONS') {
      return withCors(new Response(null, { status: 204 }), request, env)
    }

    const url = new URL(request.url)
    const objectKey = normalizeObjectKey(url.pathname)
    if (!objectKey) {
      return new Response('Not Found', { status: 404 })
    }

    const access = await authorizeRequest(request, env, objectKey)
    if (!access.allowed) {
      return withCors(new Response(access.reason || 'Forbidden', { status: 403 }), request, env)
    }

    const object = await env.FORUM_IMAGES.get(objectKey)
    if (!object) {
      return withCors(new Response('Not Found', { status: 404 }), request, env)
    }

    if (request.method !== 'HEAD' && shouldTransformImage(request, env, objectKey)) {
      const transformed = await transformImage(request, env, object)
      return withCors(transformed, request, env)
    }

    const headers = buildObjectHeaders(object, env)
    const response = new Response(request.method === 'HEAD' ? null : object.body, { headers })
    return withCors(response, request, env)
  }
}

async function authorizeRequest(request, env, objectKey) {
  if (isPublicPath(objectKey, env)) {
    return authorizePublicRequest(request, env)
  }

  if (await verifySignedUrl(request, env)) {
    return { allowed: true }
  }

  if (await verifyWithBackend(request, env, objectKey)) {
    return { allowed: true }
  }

  return { allowed: false, reason: 'Private image requires authorization' }
}

async function transformImage(request, env, object) {
  if (!env.IMAGES) {
    const headers = buildObjectHeaders(object, env)
    headers.set('x-image-transform', 'missing-images-binding')
    return new Response(request.method === 'HEAD' ? null : object.body, { headers })
  }

  const url = new URL(request.url)
  const input = object.body
  const transform = env.IMAGES.input(input).transform({
    width: clampNumber(url.searchParams.get('w'), 32, 2400),
    height: clampNumber(url.searchParams.get('h'), 32, 2400),
    fit: parseFit(url.searchParams.get('fit')),
    gravity: parseGravity(url.searchParams.get('gravity'))
  })

  const output = await transform.output({
    format: parseFormat(url.searchParams.get('format'), env),
    quality: clampNumber(url.searchParams.get('q') || env.DEFAULT_IMAGE_QUALITY, 40, 95) || 82
  })

  const response = output.response()
  const headers = new Headers(response.headers)
  headers.set('cache-control', `public, max-age=${Number(env.CACHE_TTL_SECONDS || DEFAULT_CACHE_TTL)}, immutable`)
  headers.set('x-image-transform', 'cloudflare-images')
  return new Response(request.method === 'HEAD' ? null : response.body, {
    status: response.status,
    statusText: response.statusText,
    headers
  })
}

function shouldTransformImage(request, env, objectKey) {
  if (String(env.ENABLE_IMAGE_TRANSFORM || '').toLowerCase() !== 'true') {
    return false
  }
  const url = new URL(request.url)
  const extension = objectKey.split('.').pop()?.toLowerCase() || ''
  return IMAGE_EXTENSIONS.has(extension)
    && (url.searchParams.has('w')
      || url.searchParams.has('h')
      || url.searchParams.has('q')
      || url.searchParams.has('format')
      || url.searchParams.has('fit'))
}

function authorizePublicRequest(request, env) {
  const referer = request.headers.get('referer') || ''
  const origin = request.headers.get('origin') || ''
  const allowedReferers = listFromEnv(env.ALLOWED_REFERERS)

  if (!referer && !origin) {
    return { allowed: String(env.ALLOW_EMPTY_REFERER || '').toLowerCase() === 'true', reason: 'Missing referer' }
  }

  const source = referer || origin
  const allowed = allowedReferers.some((allowedReferer) => source.startsWith(allowedReferer))
  return allowed ? { allowed: true } : { allowed: false, reason: 'Hotlink denied' }
}

function isPublicPath(objectKey, env) {
  const privatePrefixes = listFromEnv(env.PRIVATE_PREFIXES)
  if (privatePrefixes.some((prefix) => objectKey.startsWith(prefix))) {
    return false
  }

  const publicPrefixes = listFromEnv(env.PUBLIC_PREFIXES)
  if (publicPrefixes.length === 0) {
    return true
  }
  return publicPrefixes.some((prefix) => objectKey.startsWith(prefix))
}

async function verifyWithBackend(request, env, objectKey) {
  if (!env.AUTH_API_URL) {
    return false
  }

  const auth = request.headers.get('authorization') || ''
  const cookie = request.headers.get('cookie') || ''
  if (!auth && !cookie) {
    return false
  }

  const verifyUrl = new URL(env.AUTH_API_URL)
  verifyUrl.searchParams.set('key', objectKey)

  const response = await fetch(verifyUrl, {
    headers: {
      authorization: auth,
      cookie
    }
  })

  return response.ok
}

async function verifySignedUrl(request, env) {
  if (!env.IMAGE_SIGNING_SECRET) {
    return false
  }

  const url = new URL(request.url)
  const expires = Number(url.searchParams.get('expires') || 0)
  const signature = url.searchParams.get('sig') || ''
  if (!expires || !signature || Date.now() > expires * 1000) {
    return false
  }

  const path = normalizeObjectKey(url.pathname)
  const payload = `${path}.${expires}`
  const expected = await hmacSha256Hex(env.IMAGE_SIGNING_SECRET, payload)
  return timingSafeEqual(expected, signature)
}

function clampNumber(value, min, max) {
  if (value == null || value === '') {
    return undefined
  }
  const number = Number(value)
  if (!Number.isFinite(number)) {
    return undefined
  }
  return Math.max(min, Math.min(max, Math.round(number)))
}

function parseFit(value) {
  const allowed = new Set(['scale-down', 'contain', 'cover', 'crop', 'pad'])
  return allowed.has(value) ? value : 'scale-down'
}

function parseGravity(value) {
  const allowed = new Set(['auto', 'left', 'right', 'top', 'bottom', 'center'])
  return allowed.has(value) ? value : 'center'
}

function parseFormat(value, env) {
  const fallback = env.DEFAULT_IMAGE_FORMAT || 'image/webp'
  const formatMap = {
    avif: 'image/avif',
    webp: 'image/webp',
    jpeg: 'image/jpeg',
    jpg: 'image/jpeg',
    png: 'image/png'
  }
  return formatMap[value] || formatMap[fallback] || fallback
}

function buildObjectHeaders(object, env) {
  const headers = new Headers()
  object.writeHttpMetadata(headers)
  headers.set('etag', object.httpEtag)
  headers.set('cache-control', `public, max-age=${Number(env.CACHE_TTL_SECONDS || DEFAULT_CACHE_TTL)}, immutable`)
  headers.set('x-content-type-options', 'nosniff')

  if (!headers.get('content-type')) {
    headers.set('content-type', getContentType(object.key || ''))
  }

  return headers
}

function normalizeObjectKey(pathname) {
  try {
    return decodeURIComponent(pathname).replace(/^\/+/, '').replace(/\/+/g, '/')
  } catch {
    return pathname.replace(/^\/+/, '').replace(/\/+/g, '/')
  }
}

function getContentType(objectKey) {
  const extension = objectKey.split('.').pop()?.toLowerCase() || ''
  if (!IMAGE_EXTENSIONS.has(extension)) {
    return 'application/octet-stream'
  }
  if (extension === 'jpg' || extension === 'jpeg') return 'image/jpeg'
  if (extension === 'svg') return 'image/svg+xml'
  return `image/${extension}`
}

function listFromEnv(value) {
  return String(value || '')
    .split(',')
    .map((item) => item.trim())
    .filter(Boolean)
}

async function hmacSha256Hex(secret, payload) {
  const key = await crypto.subtle.importKey(
    'raw',
    new TextEncoder().encode(secret),
    { name: 'HMAC', hash: 'SHA-256' },
    false,
    ['sign']
  )
  const signature = await crypto.subtle.sign('HMAC', key, new TextEncoder().encode(payload))
  return [...new Uint8Array(signature)].map((byte) => byte.toString(16).padStart(2, '0')).join('')
}

function timingSafeEqual(left, right) {
  if (left.length !== right.length) return false
  let diff = 0
  for (let index = 0; index < left.length; index += 1) {
    diff |= left.charCodeAt(index) ^ right.charCodeAt(index)
  }
  return diff === 0
}

function withCors(response, request, env) {
  const headers = new Headers(response.headers)
  const origin = request.headers.get('origin') || ''
  const allowedReferers = listFromEnv(env.ALLOWED_REFERERS)
  if (allowedReferers.some((allowed) => origin.startsWith(allowed))) {
    headers.set('access-control-allow-origin', origin)
    headers.set('vary', 'Origin')
  }
  headers.set('access-control-allow-methods', 'GET,HEAD,OPTIONS')
  headers.set('access-control-allow-headers', 'Authorization,Content-Type')
  return new Response(response.body, {
    status: response.status,
    statusText: response.statusText,
    headers
  })
}
