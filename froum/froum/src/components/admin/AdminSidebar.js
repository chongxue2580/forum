// AdminSidebar.js - 用于处理管理后台侧边栏的显示和隐藏，优化响应式适配

import { ref, nextTick } from 'vue';

// 创建一个全局状态来管理侧边栏的显示状态
const sidebarVisible = ref(true);
const sidebarCollapsed = ref(false);

// 响应式断点定义
const BREAKPOINTS = {
  mobile: 576,
  tablet: 768,
  desktop: 992,
  large: 1200
};

// 检查设备类型
const getDeviceType = () => {
  const width = window.innerWidth;
  if (width <= BREAKPOINTS.mobile) return 'mobile';
  if (width <= BREAKPOINTS.tablet) return 'tablet';
  if (width <= BREAKPOINTS.desktop) return 'desktop';
  return 'large';
};

// 检查是否是移动设备
const isMobileDevice = () => {
  return window.innerWidth <= BREAKPOINTS.tablet;
};

// 检查是否是小屏设备
const isSmallDevice = () => {
  return window.innerWidth <= BREAKPOINTS.mobile;
};

// 初始化侧边栏状态
const initSidebar = () => {
  const deviceType = getDeviceType();

  switch (deviceType) {
    case 'mobile':
      sidebarVisible.value = false;
      sidebarCollapsed.value = false;
      break;
    case 'tablet':
      sidebarVisible.value = false;
      sidebarCollapsed.value = false;
      break;
    case 'desktop':
      sidebarVisible.value = true;
      sidebarCollapsed.value = false;
      break;
    default:
      sidebarVisible.value = true;
      sidebarCollapsed.value = false;
  }
};

// 切换侧边栏显示状态
const toggleSidebar = () => {
  if (isMobileDevice()) {
    // 在移动设备上切换显示/隐藏
    sidebarVisible.value = !sidebarVisible.value;
  } else {
    // 在桌面设备上切换展开/收起
    if (sidebarVisible.value) {
      sidebarCollapsed.value = !sidebarCollapsed.value;
    } else {
      sidebarVisible.value = true;
      sidebarCollapsed.value = false;
    }
  }
};

// 强制隐藏侧边栏（用于移动设备点击遮罩层）
const hideSidebar = () => {
  if (isMobileDevice()) {
    sidebarVisible.value = false;
  }
};

// 防抖函数
const debounce = (func, wait) => {
  let timeout;
  return function executedFunction(...args) {
    const later = () => {
      clearTimeout(timeout);
      func(...args);
    };
    clearTimeout(timeout);
    timeout = setTimeout(later, wait);
  };
};

// 在窗口大小改变时更新侧边栏状态
const handleResize = debounce(() => {
  const deviceType = getDeviceType();

  switch (deviceType) {
    case 'mobile':
    case 'tablet':
      sidebarVisible.value = false;
      sidebarCollapsed.value = false;
      break;
    case 'desktop':
    case 'large':
      if (!sidebarVisible.value) {
        sidebarVisible.value = true;
      }
      // 保持当前的收起状态
      break;
  }
}, 150);

// 设置窗口大小改变事件监听器
const setupResizeListener = () => {
  window.addEventListener('resize', handleResize);
  // 初始化时也调用一次
  nextTick(() => {
    initSidebar();
  });
};

// 移除窗口大小改变事件监听器
const removeResizeListener = () => {
  window.removeEventListener('resize', handleResize);
};

// 获取侧边栏宽度
const getSidebarWidth = () => {
  if (!sidebarVisible.value) return 0;
  if (sidebarCollapsed.value) return 64; // 收起时的宽度
  return 250; // 展开时的宽度
};

// 获取内容区域的左边距
const getContentMarginLeft = () => {
  if (isMobileDevice()) return 0;
  return getSidebarWidth();
};

export {
  sidebarVisible,
  sidebarCollapsed,
  toggleSidebar,
  hideSidebar,
  initSidebar,
  setupResizeListener,
  removeResizeListener,
  getSidebarWidth,
  getContentMarginLeft,
  isMobileDevice,
  isSmallDevice,
  getDeviceType,
  BREAKPOINTS
};