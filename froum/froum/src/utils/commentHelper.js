/**
 * 解析评论数据
 * 用于处理可能是JSON字符串的评论数据
 * @param {Array|String|Number} comments - 评论数据
 * @returns {Array} 解析后的评论数组
 */
import { resolveAvatarUrl } from './avatar'
import { formatFriendlyTime } from './dateUtils'

export function parseComments(comments) {
  if (typeof comments === 'string') {
    try {
      comments = JSON.parse(comments);
    } catch (e) {
      return [];
    }
  }

  if (!Array.isArray(comments) || !comments.length) return [];

  const normalizeComment = (comment) => {
    const user = comment.user || comment.author || {};
    const nestedComments = Array.isArray(comment.replies)
      ? comment.replies
      : Array.isArray(comment.children)
        ? comment.children
        : Array.isArray(comment.comments)
          ? comment.comments
          : [];
    const normalizedChildren = parseComments(nestedComments);

    return {
      ...comment,
      author: {
        ...user,
        id: user.id,
        name: user.nickname || user.username || user.name || '匿名用户',
        avatar: user.avatarUrl || user.avatar || null
      },
      createTime: comment.createTime || comment.createdAt,
      likes: comment.likes ?? comment.likeCount ?? 0,
      voteCount: comment.voteCount ?? comment.likeCount ?? comment.likes ?? 0,
      userVote: comment.userVote || (comment.liked || comment.isLiked ? 'up' : null),
      isLiked: Boolean(comment.isLiked ?? comment.liked),
      isBestAnswer: Boolean(comment.isBestAnswer ?? comment.bestAnswer),
      children: normalizedChildren,
      comments: normalizedChildren,
      replies: normalizedChildren
    };
  };

  comments = comments.map(normalizeComment);

  // 创建ID到评论的映射，便于查找
  const commentMap = {};
  comments.forEach(comment => {
    comment.children = comment.children || [];
    comment.comments = comment.comments || comment.children;
    comment.replies = comment.replies || comment.children;
    commentMap[comment.id] = comment;
  });

  // 根评论列表
  const rootComments = [];

  // 将评论组织成树形结构
  comments.forEach(comment => {
    // 如果是根评论（没有父评论）
    if (!comment.parentId) {
      rootComments.push(comment);
    } else {
      // 找到父评论并添加到子评论列表
      const parentComment = commentMap[comment.parentId];
      if (parentComment) {
        parentComment.children.push(comment);
        parentComment.comments = parentComment.children;
        parentComment.replies = parentComment.children;
      } else {
        // 如果找不到父评论，则作为根评论处理
        rootComments.push(comment);
      }
    }
  });

  return rootComments;
}

/**
 * 获取评论数量
 * @param {Array|String|Number} comments - 评论数据
 * @returns {Number} 评论数量
 */
export function getCommentsCount(comments) {
  // 如果是数组，返回长度
  if (Array.isArray(comments)) {
    return comments.length;
  }
  
  // 如果是字符串，尝试解析为JSON
  if (typeof comments === 'string') {
    try {
      const parsedComments = JSON.parse(comments);
      if (Array.isArray(parsedComments)) {
        return parsedComments.length;
      }
      return 0;
    } catch (e) {
      // 解析失败，忽略错误
      return 0;
    }
  }
  
  // 如果是数字，直接返回
  if (typeof comments === 'number') {
    return comments;
  }
  
  // 其他情况返回0
  return 0;
}

/**
 * 格式化日期
 * @param {String} dateString - 日期字符串
 * @returns {String} 格式化后的日期
 */
export function formatDate(dateString) {
  return formatFriendlyTime(dateString);
}

/**
 * 获取用户名称的首字母
 * @param {String} name - 用户名称
 * @returns {String} 首字母
 */
export function getInitials(name) {
  if (!name) return '?';
  return name.substring(0, 1).toUpperCase();
}

/**
 * 截断文本
 * @param {String} text - 原文本
 * @param {Number} maxLength - 最大长度
 * @returns {String} 截断后的文本
 */
export function truncateText(text, maxLength = 100) {
  if (!text) return '';
  if (text.length <= maxLength) return text;
  return text.substring(0, maxLength) + '...';
}

/**
 * 确保头像路径正确
 * @param {String} avatar - 头像路径
 * @returns {String} 处理后的头像路径
 */
export function getAvatarPath(avatar) {
  return resolveAvatarUrl(avatar);
}

/**
 * 展平树形评论结构
 * @param {Array} comments - 树形结构的评论列表
 * @returns {Array} - 展平后的评论列表
 */
export function flattenComments(comments) {
  if (!comments || !comments.length) return [];

  const result = [];
  const visited = new Set();
  const flatten = (commentList, level = 0) => {
    commentList.forEach(comment => {
      if (!comment || visited.has(comment.id)) return;
      visited.add(comment.id);
      comment.level = level;
      result.push(comment);

      const nested = Array.isArray(comment.children) && comment.children.length
        ? comment.children
        : Array.isArray(comment.replies) && comment.replies.length
          ? comment.replies
          : Array.isArray(comment.comments) && comment.comments.length
            ? comment.comments
            : [];
      if (nested.length) flatten(nested, level + 1);
    });
  };

  flatten(comments);
  return result;
}

/**
 * 获取评论的总数（包括所有子评论）
 * @param {Array} comments - 评论列表
 * @returns {Number} - 总评论数
 */
export function getTotalCommentCount(comments) {
  if (!comments || !comments.length) return 0;

  return flattenComments(comments).length;
}

/**
 * 根据ID查找评论
 * @param {Array} comments - 评论列表
 * @param {Number} commentId - 要查找的评论ID
 * @returns {Object|null} - 找到的评论或null
 */
export function findCommentById(comments, commentId) {
  if (!comments || !comments.length) return null;

  // 展平评论列表以便于查找
  const flatComments = flattenComments(comments);
  return flatComments.find(comment => Number(comment.id) === Number(commentId)) || null;
} 
