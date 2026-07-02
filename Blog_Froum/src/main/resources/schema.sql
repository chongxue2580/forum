-- 用户表
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    email VARCHAR(100) NOT NULL UNIQUE COMMENT '邮箱',
    password VARCHAR(255) NOT NULL COMMENT '密码',
    nickname VARCHAR(100) COMMENT '昵称',
    bio VARCHAR(500) COMMENT '个人简介',
    avatar_url VARCHAR(255) COMMENT '头像URL',
    role VARCHAR(20) NOT NULL DEFAULT 'USER' COMMENT '角色：USER, ADMIN, SUPER_ADMIN',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE, DISABLED, LOCKED',
    last_login_time TIMESTAMP COMMENT '最后登录时间',
    login_count INT DEFAULT 0 COMMENT '登录次数',
    two_factor_enabled BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否启用两步验证',
    two_factor_secret VARCHAR(64) COMMENT 'TOTP密钥',
    oauth_provider VARCHAR(20) COMMENT '第三方登录提供方',
    oauth_subject VARCHAR(128) COMMENT '第三方账号唯一ID',
    oauth_email_verified BOOLEAN NOT NULL DEFAULT FALSE COMMENT '第三方邮箱是否已验证',
    ban_type VARCHAR(20) DEFAULT 'NONE' COMMENT '封禁类型：NONE, LOGIN, CONTENT',
    ban_reason VARCHAR(500) COMMENT '封禁理由',
    ban_expires_at TIMESTAMP COMMENT '封禁到期时间，空表示永久',
    banned_at TIMESTAMP COMMENT '封禁时间',
    banned_by BIGINT COMMENT '执行封禁的管理员ID',
    banned_by_email VARCHAR(100) COMMENT '执行封禁的管理员邮箱',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
);

ALTER TABLE users ADD COLUMN two_factor_enabled BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否启用两步验证';
ALTER TABLE users ADD COLUMN two_factor_secret VARCHAR(64) COMMENT 'TOTP密钥';
ALTER TABLE users ADD COLUMN oauth_provider VARCHAR(20) COMMENT '第三方登录提供方';
ALTER TABLE users ADD COLUMN oauth_subject VARCHAR(128) COMMENT '第三方账号唯一ID';
ALTER TABLE users ADD COLUMN oauth_email_verified BOOLEAN NOT NULL DEFAULT FALSE COMMENT '第三方邮箱是否已验证';
CREATE INDEX idx_users_oauth_provider_subject ON users (oauth_provider, oauth_subject);
ALTER TABLE users ADD COLUMN ban_type VARCHAR(20) DEFAULT 'NONE' COMMENT '封禁类型：NONE, LOGIN, CONTENT';
ALTER TABLE users ADD COLUMN ban_reason VARCHAR(500) COMMENT '封禁理由';
ALTER TABLE users ADD COLUMN ban_expires_at TIMESTAMP COMMENT '封禁到期时间，空表示永久';
ALTER TABLE users ADD COLUMN banned_at TIMESTAMP COMMENT '封禁时间';
ALTER TABLE users ADD COLUMN banned_by BIGINT COMMENT '执行封禁的管理员ID';
ALTER TABLE users ADD COLUMN banned_by_email VARCHAR(100) COMMENT '执行封禁的管理员邮箱';

-- 分类表
CREATE TABLE IF NOT EXISTS categories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE COMMENT '分类名称',
    description VARCHAR(500) COMMENT '分类描述',
    icon VARCHAR(100) COMMENT '分类图标',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING, APPROVED, REJECTED',
    article_count INT DEFAULT 0 COMMENT '文章数量',
    display_order INT DEFAULT 999 COMMENT '显示顺序',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
);

-- 标签表
CREATE TABLE IF NOT EXISTS tags (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE COMMENT '标签名称',
    description VARCHAR(200) COMMENT '标签描述',
    color VARCHAR(20) COMMENT '标签颜色',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING, APPROVED, REJECTED',
    usage_count INT DEFAULT 0 COMMENT '使用次数',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
);

-- 文章表
CREATE TABLE IF NOT EXISTS articles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(200) NOT NULL COMMENT '文章标题',
    summary VARCHAR(500) COMMENT '文章摘要',
    content LONGTEXT COMMENT '文章内容',
    cover_image TEXT COMMENT '封面图片',
    author_id BIGINT NOT NULL COMMENT '作者ID',
    category_id BIGINT COMMENT '分类ID',
    view_count INT DEFAULT 0 COMMENT '浏览量',
    like_count INT DEFAULT 0 COMMENT '点赞数',
    comment_count INT DEFAULT 0 COMMENT '评论数',
    edit_count INT DEFAULT 0 COMMENT '编辑次数',
    is_official BOOLEAN DEFAULT FALSE COMMENT '是否官方',
    is_pinned BOOLEAN DEFAULT FALSE COMMENT '是否置顶',
    is_featured BOOLEAN DEFAULT FALSE COMMENT '是否加精',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING, APPROVED, REJECTED, DRAFT',
    published_at TIMESTAMP COMMENT '发布时间',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (author_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE SET NULL
);

ALTER TABLE articles MODIFY COLUMN content LONGTEXT COMMENT '文章内容';
ALTER TABLE articles MODIFY COLUMN cover_image TEXT COMMENT '封面图片';
ALTER TABLE articles ADD COLUMN tags_string TEXT COMMENT '标签字符串';
ALTER TABLE articles MODIFY COLUMN tags_string TEXT COMMENT '标签字符串';
ALTER TABLE articles ADD COLUMN edit_count INT DEFAULT 0 COMMENT '编辑次数';

-- 问答表
CREATE TABLE IF NOT EXISTS questions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(200) NOT NULL COMMENT '问题标题',
    content LONGTEXT COMMENT '问题内容',
    author_id BIGINT NOT NULL COMMENT '作者ID',
    view_count INT DEFAULT 0 COMMENT '浏览量',
    like_count INT DEFAULT 0 COMMENT '点赞数',
    comment_count INT DEFAULT 0 COMMENT '评论数',
    follow_count INT DEFAULT 0 COMMENT '关注数',
    is_solved BOOLEAN DEFAULT FALSE COMMENT '是否已解决',
    is_pinned BOOLEAN DEFAULT FALSE COMMENT '是否置顶',
    is_featured BOOLEAN DEFAULT FALSE COMMENT '是否加精',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING, APPROVED, REJECTED',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (author_id) REFERENCES users(id) ON DELETE CASCADE
);

ALTER TABLE questions MODIFY COLUMN content LONGTEXT COMMENT '问题内容';

-- 文章标签关联表
CREATE TABLE IF NOT EXISTS article_tags (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    article_id BIGINT NOT NULL COMMENT '文章ID',
    tag_id BIGINT NOT NULL COMMENT '标签ID',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    FOREIGN KEY (article_id) REFERENCES articles(id) ON DELETE CASCADE,
    FOREIGN KEY (tag_id) REFERENCES tags(id) ON DELETE CASCADE,
    UNIQUE KEY uk_article_tag (article_id, tag_id)
);

-- 问答标签关联表
CREATE TABLE IF NOT EXISTS question_tags (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    question_id BIGINT NOT NULL COMMENT '问答ID',
    tag_id BIGINT NOT NULL COMMENT '标签ID',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    FOREIGN KEY (question_id) REFERENCES questions(id) ON DELETE CASCADE,
    FOREIGN KEY (tag_id) REFERENCES tags(id) ON DELETE CASCADE,
    UNIQUE KEY uk_question_tag (question_id, tag_id)
);

-- 评论表
CREATE TABLE IF NOT EXISTS comments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    content TEXT NOT NULL COMMENT '评论内容',
    author_id BIGINT NOT NULL COMMENT '评论者ID',
    target_type VARCHAR(20) NOT NULL COMMENT '目标类型：ARTICLE, QUESTION',
    target_id BIGINT NOT NULL COMMENT '目标ID',
    parent_id BIGINT COMMENT '父评论ID',
    like_count INT DEFAULT 0 COMMENT '点赞数',
    is_deleted BOOLEAN DEFAULT FALSE COMMENT '是否删除',
    is_best_answer BOOLEAN DEFAULT FALSE COMMENT '是否最佳答案',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (author_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (parent_id) REFERENCES comments(id) ON DELETE CASCADE
);

-- 点赞表
CREATE TABLE IF NOT EXISTS likes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    target_type VARCHAR(20) NOT NULL COMMENT '目标类型：ARTICLE, QUESTION, COMMENT',
    target_id BIGINT NOT NULL COMMENT '目标ID',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE KEY uk_user_target (user_id, target_type, target_id)
);

-- 关注表
CREATE TABLE IF NOT EXISTS follows (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    follower_id BIGINT NOT NULL COMMENT '关注者ID',
    target_type VARCHAR(20) NOT NULL COMMENT '目标类型：USER, QUESTION',
    target_id BIGINT NOT NULL COMMENT '目标ID',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    FOREIGN KEY (follower_id) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE KEY uk_follower_target (follower_id, target_type, target_id)
);

-- 通知表
CREATE TABLE IF NOT EXISTS notifications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '接收者ID',
    from_user_id BIGINT COMMENT '发送者ID',
    type VARCHAR(50) NOT NULL COMMENT '通知类型',
    title VARCHAR(200) NOT NULL COMMENT '通知标题',
    content TEXT COMMENT '通知内容',
    target_id BIGINT COMMENT '相关目标ID',
    is_read BOOLEAN DEFAULT FALSE COMMENT '是否已读',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (from_user_id) REFERENCES users(id) ON DELETE SET NULL
);

-- 举报表
CREATE TABLE IF NOT EXISTS reports (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    reporter_id BIGINT NOT NULL COMMENT '举报人ID',
    target_type VARCHAR(20) NOT NULL COMMENT '目标类型：ARTICLE, QUESTION, COMMENT, USER',
    target_id BIGINT NOT NULL COMMENT '目标ID',
    target_title VARCHAR(255) COMMENT '目标标题或摘要',
    target_owner_id BIGINT COMMENT '目标作者或所属用户ID',
    reason VARCHAR(120) NOT NULL COMMENT '举报原因',
    description TEXT COMMENT '举报补充说明',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING, RESOLVED, REJECTED',
    handler_id BIGINT COMMENT '处理管理员ID',
    handler_note TEXT COMMENT '处理备注',
    handled_at TIMESTAMP COMMENT '处理时间',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (reporter_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (target_owner_id) REFERENCES users(id) ON DELETE SET NULL,
    FOREIGN KEY (handler_id) REFERENCES users(id) ON DELETE SET NULL
);

-- 操作日志表
CREATE TABLE IF NOT EXISTS operation_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT COMMENT '操作用户ID',
    operation_type VARCHAR(50) NOT NULL COMMENT '操作类型',
    operation_action VARCHAR(50) COMMENT '操作动作',
    operation_desc VARCHAR(500) NOT NULL COMMENT '操作描述',
    target_type VARCHAR(50) COMMENT '目标类型',
    target_id BIGINT COMMENT '目标ID',
    target_name VARCHAR(200) COMMENT '目标名称',
    ip_address VARCHAR(50) COMMENT 'IP地址',
    user_agent VARCHAR(500) COMMENT '用户代理',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL
);

-- 系统设置表
CREATE TABLE IF NOT EXISTS system_settings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    setting_key VARCHAR(120) NOT NULL UNIQUE COMMENT '设置键',
    setting_value TEXT COMMENT '设置值',
    setting_group VARCHAR(50) NOT NULL COMMENT '设置分组',
    value_type VARCHAR(20) NOT NULL DEFAULT 'STRING' COMMENT '值类型：STRING, BOOLEAN, NUMBER',
    description VARCHAR(255) COMMENT '设置描述',
    is_sensitive BOOLEAN DEFAULT FALSE COMMENT '是否敏感字段',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
);

ALTER TABLE operation_logs ADD COLUMN operation_action VARCHAR(50) COMMENT '操作动作';
ALTER TABLE operation_logs ADD COLUMN target_name VARCHAR(200) COMMENT '目标名称';

-- 迁移旧 user_follows 表数据到统一 follows 表；旧表不存在时由 continue-on-error 忽略。
INSERT IGNORE INTO follows (follower_id, target_type, target_id, created_at)
SELECT follower_id, 'USER', following_id, created_at
FROM user_follows;

DROP TABLE IF EXISTS user_follows;

-- 创建索引
CREATE INDEX idx_articles_author ON articles(author_id);
CREATE INDEX idx_articles_category ON articles(category_id);
CREATE INDEX idx_articles_status ON articles(status);
CREATE INDEX idx_articles_created ON articles(created_at);

CREATE INDEX idx_questions_author ON questions(author_id);
CREATE INDEX idx_questions_status ON questions(status);
CREATE INDEX idx_questions_created ON questions(created_at);

CREATE INDEX idx_comments_target ON comments(target_type, target_id);
CREATE INDEX idx_comments_author ON comments(author_id);
CREATE INDEX idx_comments_created ON comments(created_at);

CREATE INDEX idx_likes_user ON likes(user_id);
CREATE INDEX idx_likes_target ON likes(target_type, target_id);

CREATE INDEX idx_follows_follower ON follows(follower_id);
CREATE INDEX idx_follows_target ON follows(target_type, target_id);

CREATE INDEX idx_notifications_user ON notifications(user_id);
CREATE INDEX idx_notifications_read ON notifications(is_read);

CREATE INDEX idx_reports_reporter ON reports(reporter_id);
CREATE INDEX idx_reports_target ON reports(target_type, target_id);
CREATE INDEX idx_reports_status ON reports(status);
CREATE INDEX idx_reports_created ON reports(created_at);
CREATE INDEX idx_reports_pending_target ON reports(reporter_id, target_type, target_id, status);

CREATE INDEX idx_operation_logs_user ON operation_logs(user_id);
CREATE INDEX idx_operation_logs_type ON operation_logs(operation_type);
CREATE INDEX idx_operation_logs_action ON operation_logs(operation_action);
CREATE INDEX idx_operation_logs_created ON operation_logs(created_at);

CREATE INDEX idx_system_settings_group ON system_settings(setting_group);
