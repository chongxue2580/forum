-- 插入初始用户数据（密码已加密）
-- admin123 -> $2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDa (这个哈希是错误的，需要重新生成)
-- 使用正确的BCrypt哈希值
INSERT IGNORE INTO users (username, email, password, nickname, role, status, created_at, updated_at) VALUES
('admin', 'admin@example.com', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', '系统管理员', 'ADMIN', 'ACTIVE', NOW(), NOW()),
('testuser1', 'user1@example.com', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', '测试用户1', 'USER', 'ACTIVE', NOW(), NOW()),
('testuser2', 'user2@example.com', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', '测试用户2', 'USER', 'ACTIVE', NOW(), NOW()),
('testuser3', 'user3@example.com', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', '测试用户3', 'USER', 'ACTIVE', NOW(), NOW());

-- 插入分类数据
INSERT INTO categories (name, description, icon, status, display_order) VALUES
('技术分享', '分享各种技术经验和心得', '💻', 'APPROVED', 1),
('前端开发', '前端技术相关讨论', '🎨', 'APPROVED', 2),
('后端开发', '后端技术相关讨论', '⚙️', 'APPROVED', 3),
('数据库', '数据库相关技术', '🗄️', 'APPROVED', 4),
('算法与数据结构', '算法学习和讨论', '🧮', 'APPROVED', 5),
('职场经验', '职场心得和经验分享', '💼', 'APPROVED', 6),
('学习资源', '各种学习资料分享', '📚', 'APPROVED', 7);

-- 插入标签数据
INSERT INTO tags (name, description, color, status) VALUES
('Java', 'Java编程语言', '#FF6B35', 'APPROVED'),
('Spring Boot', 'Spring Boot框架', '#6DB33F', 'APPROVED'),
('React', 'React前端框架', '#61DAFB', 'APPROVED'),
('Vue', 'Vue.js前端框架', '#4FC08D', 'APPROVED'),
('MySQL', 'MySQL数据库', '#4479A1', 'APPROVED'),
('Redis', 'Redis缓存数据库', '#DC382D', 'APPROVED'),
('JavaScript', 'JavaScript编程语言', '#F7DF1E', 'APPROVED'),
('Python', 'Python编程语言', '#3776AB', 'APPROVED'),
('算法', '算法相关', '#FF4081', 'APPROVED'),
('面试', '面试相关', '#9C27B0', 'APPROVED'),
('Docker', 'Docker容器技术', '#2496ED', 'APPROVED'),
('Kubernetes', 'K8s容器编排', '#326CE5', 'APPROVED');

-- 插入文章数据
INSERT INTO articles (title, summary, content, author_id, category_id, status, view_count, like_count, comment_count, published_at) VALUES
('Spring Boot 入门指南', '详细介绍Spring Boot的基础知识和使用方法', 
'# Spring Boot 入门指南

Spring Boot是一个基于Spring框架的快速开发框架，它简化了Spring应用的配置和部署。

## 主要特性

1. **自动配置**: Spring Boot能够根据类路径中的依赖自动配置应用
2. **起步依赖**: 提供了一系列的starter依赖，简化了依赖管理
3. **内嵌服务器**: 内置了Tomcat、Jetty等服务器，无需外部部署
4. **生产就绪**: 提供了健康检查、指标监控等生产环境所需的功能

## 快速开始

```java
@SpringBootApplication
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

这就是一个最简单的Spring Boot应用！', 
2, 1, 'APPROVED', 150, 25, 8, NOW()),

('React Hooks 最佳实践', 'React Hooks的使用技巧和最佳实践', 
'# React Hooks 最佳实践

React Hooks是React 16.8引入的新特性，让我们能够在函数组件中使用状态和其他React特性。

## 常用Hooks

### useState
用于在函数组件中添加状态：

```javascript
const [count, setCount] = useState(0);
```

### useEffect
用于处理副作用：

```javascript
useEffect(() => {
  document.title = `Count: ${count}`;
}, [count]);
```

## 最佳实践

1. 只在最顶层调用Hook
2. 只在React函数中调用Hook
3. 使用ESLint插件确保Hook规则
4. 合理拆分useEffect

这些实践能帮助你更好地使用React Hooks！', 
3, 2, 'APPROVED', 120, 18, 5, NOW()),

('MySQL性能优化技巧', '分享MySQL数据库性能优化的实用技巧', 
'# MySQL性能优化技巧

数据库性能优化是后端开发中的重要技能，本文分享一些MySQL优化的实用技巧。

## 索引优化

### 1. 选择合适的索引类型
- 主键索引：唯一且非空
- 唯一索引：保证数据唯一性
- 普通索引：提高查询效率
- 复合索引：多列组合索引

### 2. 索引设计原则
- 最左前缀原则
- 避免过多索引
- 定期维护索引

## 查询优化

### 1. 避免全表扫描
```sql
-- 不好的查询
SELECT * FROM users WHERE name LIKE "%张%";

-- 好的查询
SELECT * FROM users WHERE name LIKE "张%";
```

### 2. 使用EXPLAIN分析查询
```sql
EXPLAIN SELECT * FROM users WHERE age > 18;
```

通过这些优化技巧，可以显著提升MySQL的性能！', 
2, 4, 'APPROVED', 200, 35, 12, NOW());

-- 插入文章标签关联
INSERT INTO article_tags (article_id, tag_id) VALUES
(1, 1), (1, 2),  -- Spring Boot文章关联Java和Spring Boot标签
(2, 3), (2, 7),  -- React文章关联React和JavaScript标签
(3, 5), (3, 1);  -- MySQL文章关联MySQL和Java标签

-- 插入问答数据
INSERT INTO questions (title, content, author_id, status, view_count, like_count, comment_count, follow_count) VALUES
('如何优化Java程序的性能？', 
'我的Java程序运行比较慢，主要是在处理大量数据时。请问有什么优化建议吗？

具体场景：
- 需要处理100万条数据
- 涉及数据库查询和计算
- 内存使用较高

希望大家能分享一些实用的优化技巧，谢谢！', 
3, 'APPROVED', 80, 12, 6, 4),

('React和Vue应该选择哪个？', 
'我是前端新手，想学习一个前端框架。看了很多资料，主要在React和Vue之间纠结。

我的情况：
- 有一定的JavaScript基础
- 希望学习曲线不要太陡峭
- 未来想找前端工作

请问大家有什么建议吗？两个框架各有什么优缺点？', 
4, 'APPROVED', 65, 8, 9, 3),

('Docker容器化部署的最佳实践？', 
'最近在学习Docker，想了解一下容器化部署的最佳实践。

主要想了解：
1. Dockerfile编写技巧
2. 镜像优化方法
3. 容器编排策略
4. 生产环境部署注意事项

有经验的朋友能分享一下吗？', 
2, 'APPROVED', 45, 6, 4, 2);

-- 插入问答标签关联
INSERT INTO question_tags (question_id, tag_id) VALUES
(1, 1), (1, 9),   -- Java性能问题关联Java和算法标签
(2, 3), (2, 4),   -- React vs Vue关联React和Vue标签
(3, 11), (3, 12); -- Docker问题关联Docker和Kubernetes标签

-- 插入评论数据
INSERT INTO comments (content, author_id, target_type, target_id) VALUES
('这篇文章写得很好，对Spring Boot初学者很有帮助！', 3, 'ARTICLE', 1),
('感谢分享，正好在学习Spring Boot', 4, 'ARTICLE', 1),
('文章很详细，收藏了！', 2, 'ARTICLE', 1),
('React Hooks确实很强大，这些最佳实践很实用', 2, 'ARTICLE', 2),
('useEffect的使用技巧很有用，谢谢分享', 4, 'ARTICLE', 2),
('MySQL优化确实很重要，这些技巧很实用', 3, 'ARTICLE', 3),
('索引优化那部分讲得很清楚', 4, 'ARTICLE', 3),

('Java性能优化可以从以下几个方面入手：\n1. 算法优化\n2. JVM调优\n3. 数据库优化\n4. 缓存使用', 2, 'QUESTION', 1),
('建议使用多线程处理，可以显著提升性能', 4, 'QUESTION', 1),
('对于新手，我推荐Vue，学习曲线更平缓一些', 2, 'QUESTION', 2),
('React生态更丰富，但Vue更容易上手', 3, 'QUESTION', 2),
('Docker最佳实践：\n1. 使用多阶段构建\n2. 最小化镜像大小\n3. 合理使用缓存', 3, 'QUESTION', 3);

-- 插入点赞数据
INSERT INTO likes (user_id, target_type, target_id) VALUES
(2, 'ARTICLE', 1), (3, 'ARTICLE', 1), (4, 'ARTICLE', 1),
(2, 'ARTICLE', 2), (3, 'ARTICLE', 2),
(2, 'ARTICLE', 3), (3, 'ARTICLE', 3), (4, 'ARTICLE', 3),
(2, 'QUESTION', 1), (4, 'QUESTION', 1),
(2, 'QUESTION', 2), (3, 'QUESTION', 2),
(3, 'QUESTION', 3), (4, 'QUESTION', 3),
(3, 'COMMENT', 1), (4, 'COMMENT', 1),
(2, 'COMMENT', 4), (3, 'COMMENT', 4);

-- 插入关注数据
INSERT INTO follows (follower_id, target_type, target_id) VALUES
(3, 'USER', 2), (4, 'USER', 2), (2, 'USER', 3),
(4, 'USER', 3), (2, 'USER', 4), (3, 'USER', 4),
(2, 'QUESTION', 1), (4, 'QUESTION', 1),
(2, 'QUESTION', 2), (3, 'QUESTION', 2),
(3, 'QUESTION', 3), (4, 'QUESTION', 3);

-- 插入通知数据
INSERT INTO notifications (user_id, from_user_id, type, title, content, target_id) VALUES
(2, 3, 'ARTICLE_COMMENT', '您的文章收到了新评论', 'testuser2 评论了您的文章：Spring Boot 入门指南', 1),
(2, 4, 'ARTICLE_COMMENT', '您的文章收到了新评论', 'testuser3 评论了您的文章：Spring Boot 入门指南', 1),
(3, 2, 'ARTICLE_COMMENT', '您的文章收到了新评论', 'testuser1 评论了您的文章：React Hooks 最佳实践', 2),
(2, 3, 'USER_FOLLOW', '您有新的粉丝', 'testuser2 关注了您', 2),
(2, 4, 'USER_FOLLOW', '您有新的粉丝', 'testuser3 关注了您', 2),
(3, 2, 'QUESTION_COMMENT', '您的问答收到了新评论', 'testuser1 评论了您的问答：如何优化Java程序的性能？', 1);

-- 插入操作日志数据
INSERT INTO operation_logs (user_id, operation_type, operation_desc, target_type, target_id, ip_address) VALUES
(1, 'USER_LOGIN', '管理员登录系统', 'USER', 1, '127.0.0.1'),
(2, 'ARTICLE_CREATE', '创建文章：Spring Boot 入门指南', 'ARTICLE', 1, '127.0.0.1'),
(3, 'ARTICLE_CREATE', '创建文章：React Hooks 最佳实践', 'ARTICLE', 2, '127.0.0.1'),
(2, 'ARTICLE_CREATE', '创建文章：MySQL性能优化技巧', 'ARTICLE', 3, '127.0.0.1'),
(3, 'QUESTION_CREATE', '创建问答：如何优化Java程序的性能？', 'QUESTION', 1, '127.0.0.1'),
(4, 'QUESTION_CREATE', '创建问答：React和Vue应该选择哪个？', 'QUESTION', 2, '127.0.0.1'),
(2, 'QUESTION_CREATE', '创建问答：Docker容器化部署的最佳实践？', 'QUESTION', 3, '127.0.0.1');

-- 插入系统设置默认值
INSERT IGNORE INTO system_settings (setting_key, setting_value, setting_group, value_type, description, is_sensitive) VALUES
('site.name', 'FroumX 技术社区', 'site', 'STRING', '站点名称', FALSE),
('site.description', '专注于前端开发、后端开发、移动开发、人工智能等技术交流的社区', 'site', 'STRING', '站点描述', FALSE),
('site.logo', '/logo.png', 'site', 'STRING', '站点Logo', FALSE),
('site.favicon', '/favicon.ico', 'site', 'STRING', '站点Favicon', FALSE),
('site.icp', '', 'site', 'STRING', 'ICP备案号', FALSE),
('site.allowRegistration', 'true', 'site', 'BOOLEAN', '允许注册', FALSE),
('site.requireEmailVerification', 'false', 'site', 'BOOLEAN', '邮箱验证', FALSE),
('site.defaultUserRole', 'USER', 'site', 'STRING', '默认用户角色', FALSE),
('content.articleReviewEnabled', 'true', 'content', 'BOOLEAN', '文章审核', FALSE),
('content.questionReviewEnabled', 'false', 'content', 'BOOLEAN', '问答审核', FALSE),
('content.commentReviewEnabled', 'false', 'content', 'BOOLEAN', '评论审核', FALSE),
('content.allowAnonymousView', 'true', 'content', 'BOOLEAN', '允许匿名访问', FALSE),
('content.allowGuestComment', 'false', 'content', 'BOOLEAN', '允许游客评论', FALSE),
('content.sensitiveWords', '', 'content', 'STRING', '敏感词', FALSE),
('content.maxUploadSize', '10', 'content', 'NUMBER', '最大上传大小', FALSE),
('content.allowedFileTypes', 'jpg,jpeg,png,gif,pdf,doc,docx,xls,xlsx,ppt,pptx,zip,rar', 'content', 'STRING', '允许的文件类型', FALSE),
('email.smtpServer', 'smtp-relay.brevo.com', 'email', 'STRING', 'SMTP服务器', FALSE),
('email.smtpPort', '587', 'email', 'NUMBER', 'SMTP端口', FALSE),
('email.smtpUsername', '', 'email', 'STRING', 'SMTP用户名', FALSE),
('email.smtpPassword', '', 'email', 'STRING', 'SMTP密码', TRUE),
('email.senderName', 'FroumX 社区', 'email', 'STRING', '发件人名称', FALSE),
('email.senderEmail', '', 'email', 'STRING', '发件人邮箱', FALSE),
('email.enableSsl', 'true', 'email', 'BOOLEAN', '启用SSL', FALSE),
('oauth.frontendCallbackUrl', '', 'oauth', 'STRING', '第三方登录前端回调地址', FALSE),
('oauth.githubClientId', '', 'oauth', 'STRING', 'GitHub Client ID', FALSE),
('oauth.githubClientSecret', '', 'oauth', 'STRING', 'GitHub Client Secret', TRUE),
('oauth.googleClientId', '', 'oauth', 'STRING', 'Google Client ID', FALSE),
('oauth.googleClientSecret', '', 'oauth', 'STRING', 'Google Client Secret', TRUE),
('security.jwtExpireDays', '7', 'security', 'NUMBER', 'JWT过期天数', FALSE),
('security.maxLoginAttempts', '5', 'security', 'NUMBER', '最大登录尝试次数', FALSE),
('security.lockoutMinutes', '30', 'security', 'NUMBER', '锁定时间', FALSE),
('security.enableRecaptcha', 'false', 'security', 'BOOLEAN', '启用reCAPTCHA', FALSE),
('security.recaptchaSiteKey', '', 'security', 'STRING', 'reCAPTCHA Site Key', FALSE),
('security.recaptchaSecretKey', '', 'security', 'STRING', 'reCAPTCHA Secret Key', TRUE),
('optimization.enableCache', 'false', 'optimization', 'BOOLEAN', '启用缓存', FALSE),
('optimization.cacheTtl', '3600', 'optimization', 'NUMBER', '缓存过期时间', FALSE),
('optimization.pageSize', '20', 'optimization', 'NUMBER', '默认分页大小', FALSE),
('optimization.maxSearchResults', '100', 'optimization', 'NUMBER', '最大搜索结果数', FALSE);
