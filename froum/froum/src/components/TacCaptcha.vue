<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { captchaService } from '../services/captchaService'

const emit = defineEmits(['update:modelValue'])

const loading = ref(false)
const error = ref('')
const captchaResponse = ref(null)
const stageRef = ref(null)
const dragging = ref(false)
const dragStartX = ref(0)
const startLeft = ref(0)
const sliderLeft = ref(0)
const maxLeft = ref(0)
const completed = ref(false)

const captcha = computed(() => captchaResponse.value?.captcha || null)
const pieceWidthPercent = computed(() => {
  if (!captcha.value?.backgroundImageWidth || !captcha.value?.templateImageWidth) return 0
  return (captcha.value.templateImageWidth / captcha.value.backgroundImageWidth) * 100
})
const trackLabel = computed(() => {
  if (loading.value) return '加载验证码...'
  if (completed.value) return '已滑动，提交时校验'
  return '拖动滑块完成验证'
})

const resetDrag = () => {
  dragging.value = false
  sliderLeft.value = 0
  completed.value = false
  emit('update:modelValue', null)
}

const syncStageSize = async () => {
  await nextTick()
  const stage = stageRef.value
  if (!stage || !captcha.value) return
  const pieceWidth = stage.clientWidth * (captcha.value.templateImageWidth / captcha.value.backgroundImageWidth)
  maxLeft.value = Math.max(0, stage.clientWidth - pieceWidth)
}

const refresh = async () => {
  loading.value = true
  error.value = ''
  resetDrag()

  try {
    captchaResponse.value = await captchaService.generate()
    await syncStageSize()
  } catch (err) {
    error.value = err.message || '验证码加载失败'
    captchaResponse.value = null
  } finally {
    loading.value = false
  }
}

const clampLeft = (value) => Math.min(Math.max(value, 0), maxLeft.value)

const getClientX = (event) => event.touches?.[0]?.clientX ?? event.clientX

const startDrag = (event) => {
  if (!captcha.value || loading.value) return
  dragging.value = true
  completed.value = false
  dragStartX.value = getClientX(event)
  startLeft.value = sliderLeft.value
  emit('update:modelValue', null)
}

const moveDrag = (event) => {
  if (!dragging.value) return
  event.preventDefault()
  sliderLeft.value = clampLeft(startLeft.value + getClientX(event) - dragStartX.value)
}

const stopDrag = () => {
  if (!dragging.value) return
  dragging.value = false

  if (!captchaResponse.value?.id || maxLeft.value <= 0) {
    resetDrag()
    return
  }

  const stageWidth = stageRef.value?.clientWidth || captcha.value.backgroundImageWidth || 1
  const percentage = sliderLeft.value / stageWidth
  completed.value = sliderLeft.value > 8
  emit('update:modelValue', completed.value
    ? {
        captchaId: captchaResponse.value.id,
        captchaPercentage: Number(percentage.toFixed(6))
      }
    : null)
}

onMounted(() => {
  refresh()
  window.addEventListener('mousemove', moveDrag)
  window.addEventListener('mouseup', stopDrag)
  window.addEventListener('touchmove', moveDrag, { passive: false })
  window.addEventListener('touchend', stopDrag)
  window.addEventListener('resize', syncStageSize)
})

onBeforeUnmount(() => {
  window.removeEventListener('mousemove', moveDrag)
  window.removeEventListener('mouseup', stopDrag)
  window.removeEventListener('touchmove', moveDrag)
  window.removeEventListener('touchend', stopDrag)
  window.removeEventListener('resize', syncStageSize)
})

defineExpose({ refresh })
</script>

<template>
  <div class="tac-captcha">
    <div class="tac-header">
      <span>安全验证</span>
      <button type="button" class="tac-refresh" :disabled="loading" @click="refresh">
        <font-awesome-icon :icon="['fas', 'sync-alt']" :spin="loading" />
      </button>
    </div>

    <div v-if="error" class="tac-error">{{ error }}</div>

    <div
      v-if="captcha"
      ref="stageRef"
      class="tac-stage"
      :style="{ aspectRatio: `${captcha.backgroundImageWidth} / ${captcha.backgroundImageHeight}` }"
    >
      <img class="tac-bg" :src="captcha.backgroundImage" alt="验证码背景">
      <img
        class="tac-piece"
        :src="captcha.templateImage"
        alt="验证码滑块"
        :style="{ width: `${pieceWidthPercent}%`, transform: `translateX(${sliderLeft}px)` }"
      >
    </div>

    <div class="tac-track" :class="{ done: completed }">
      <div class="tac-track-fill" :style="{ width: `${sliderLeft + 42}px` }"></div>
      <button
        type="button"
        class="tac-handle"
        :style="{ transform: `translateX(${sliderLeft}px)` }"
        :disabled="loading || !captcha"
        @mousedown.prevent="startDrag"
        @touchstart.prevent="startDrag"
      >
        <font-awesome-icon :icon="['fas', completed ? 'check' : 'arrow-right']" />
      </button>
      <span class="tac-track-label">{{ trackLabel }}</span>
    </div>
  </div>
</template>

<style scoped>
.tac-captcha {
  margin: 0 0 1.25rem;
}

.tac-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 0.5rem;
  color: var(--text-color, #333);
  font-size: 0.9rem;
  font-weight: 500;
}

.tac-refresh {
  width: 32px;
  height: 32px;
  border: 1px solid var(--border-color, #d9d9d9);
  border-radius: 4px;
  background: #fff;
  color: var(--text-light, #666);
  cursor: pointer;
}

.tac-refresh:disabled {
  cursor: not-allowed;
  opacity: 0.6;
}

.tac-error {
  margin-bottom: 0.5rem;
  color: var(--error-color, #cf1322);
  font-size: 0.85rem;
}

.tac-stage {
  position: relative;
  width: 100%;
  overflow: hidden;
  border: 1px solid var(--border-color, #d9d9d9);
  border-radius: 6px;
  background: #f5f5f5;
  user-select: none;
}

.tac-bg,
.tac-piece {
  position: absolute;
  inset: 0;
  height: 100%;
  object-fit: fill;
  pointer-events: none;
}

.tac-bg {
  width: 100%;
}

.tac-piece {
  will-change: transform;
}

.tac-track {
  position: relative;
  height: 42px;
  margin-top: 0.625rem;
  overflow: hidden;
  border: 1px solid var(--border-color, #d9d9d9);
  border-radius: 6px;
  background: #f7f8fa;
}

.tac-track-fill {
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 42px;
  background: rgba(24, 144, 255, 0.16);
}

.tac-track.done .tac-track-fill {
  background: rgba(82, 196, 26, 0.16);
}

.tac-handle {
  position: absolute;
  left: 0;
  top: 0;
  width: 42px;
  height: 40px;
  border: 0;
  border-right: 1px solid var(--border-color, #d9d9d9);
  background: #fff;
  color: #1890ff;
  cursor: grab;
  z-index: 2;
}

.tac-handle:active {
  cursor: grabbing;
}

.tac-handle:disabled {
  cursor: not-allowed;
  color: #999;
}

.tac-track.done .tac-handle {
  color: #52c41a;
}

.tac-track-label {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--text-light, #666);
  font-size: 0.9rem;
  pointer-events: none;
}
</style>
