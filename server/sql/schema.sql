-- 次元站 数据库结构
SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS users (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  username VARCHAR(32) NOT NULL,
  password VARCHAR(100) NOT NULL,
  nickname VARCHAR(32) NOT NULL,
  avatar VARCHAR(255) DEFAULT NULL,
  sign VARCHAR(120) DEFAULT NULL,
  gender TINYINT DEFAULT 0,
  coin INT DEFAULT 5,
  level INT DEFAULT 0,
  exp INT DEFAULT 0,
  role VARCHAR(16) DEFAULT 'USER',
  status TINYINT DEFAULT 0,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS categories (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  name VARCHAR(32) NOT NULL,
  slug VARCHAR(32) DEFAULT NULL,
  sort INT DEFAULT 99,
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS videos (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  user_id BIGINT UNSIGNED NOT NULL,
  category_id BIGINT UNSIGNED DEFAULT 1,
  title VARCHAR(120) NOT NULL,
  description TEXT,
  cover VARCHAR(255) DEFAULT NULL,
  video_url VARCHAR(255) DEFAULT NULL,
  duration INT DEFAULT 0,
  width INT DEFAULT 0,
  height INT DEFAULT 0,
  status TINYINT DEFAULT 1,
  views BIGINT DEFAULT 0,
  danmaku_count BIGINT DEFAULT 0,
  like_count BIGINT DEFAULT 0,
  coin_count BIGINT DEFAULT 0,
  fav_count BIGINT DEFAULT 0,
  comment_count BIGINT DEFAULT 0,
  tags VARCHAR(255) DEFAULT NULL,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_status_created (status, created_at),
  KEY idx_user (user_id),
  KEY idx_category (category_id),
  KEY idx_views (views)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS comments (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  video_id BIGINT UNSIGNED NOT NULL,
  user_id BIGINT UNSIGNED NOT NULL,
  root_id BIGINT UNSIGNED DEFAULT 0,
  parent_id BIGINT UNSIGNED DEFAULT 0,
  reply_user_id BIGINT UNSIGNED DEFAULT NULL,
  content VARCHAR(600) NOT NULL,
  like_count BIGINT DEFAULT 0,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_video_root (video_id, root_id),
  KEY idx_root (root_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS danmaku (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  video_id BIGINT UNSIGNED NOT NULL,
  user_id BIGINT UNSIGNED NOT NULL,
  content VARCHAR(80) NOT NULL,
  time_sec DOUBLE DEFAULT 0,
  color VARCHAR(16) DEFAULT '#ffffff',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_video_time (video_id, time_sec)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS video_likes (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  video_id BIGINT UNSIGNED NOT NULL,
  user_id BIGINT UNSIGNED NOT NULL,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_video_user (video_id, user_id),
  KEY idx_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS video_coins (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  video_id BIGINT UNSIGNED NOT NULL,
  user_id BIGINT UNSIGNED NOT NULL,
  amount INT DEFAULT 1,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_video_user (video_id, user_id),
  KEY idx_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS video_favs (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  video_id BIGINT UNSIGNED NOT NULL,
  user_id BIGINT UNSIGNED NOT NULL,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_video_user (video_id, user_id),
  KEY idx_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS comment_likes (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  comment_id BIGINT UNSIGNED NOT NULL,
  user_id BIGINT UNSIGNED NOT NULL,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_comment_user (comment_id, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS follows (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  follower_id BIGINT UNSIGNED NOT NULL,
  following_id BIGINT UNSIGNED NOT NULL,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_follow (follower_id, following_id),
  KEY idx_following (following_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS histories (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  video_id BIGINT UNSIGNED NOT NULL,
  user_id BIGINT UNSIGNED NOT NULL,
  progress INT DEFAULT 0,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_video (user_id, video_id),
  KEY idx_user_time (user_id, updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS notifications (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  user_id BIGINT UNSIGNED NOT NULL,
  from_user_id BIGINT UNSIGNED DEFAULT NULL,
  type VARCHAR(20) DEFAULT 'system',
  video_id BIGINT UNSIGNED DEFAULT NULL,
  content VARCHAR(255) DEFAULT NULL,
  is_read TINYINT DEFAULT 0,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_user_read (user_id, is_read)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO categories (id, name, slug, sort) VALUES
 (1,'动画','anime',1),
 (2,'番剧','bangumi',2),
 (3,'国创','guochuang',3),
 (4,'音乐','music',4),
 (5,'舞蹈','dance',5),
 (6,'游戏','game',6),
 (7,'知识','knowledge',7),
 (8,'科技','tech',8),
 (9,'运动','sports',9),
 (10,'汽车','car',10),
 (11,'生活','life',11),
 (12,'美食','food',12),
 (13,'动物圈','animal',13),
 (14,'影视','movie',14),
 (15,'鬼畜','kichiku',15)
ON DUPLICATE KEY UPDATE name = VALUES(name), sort = VALUES(sort);

-- ---------------------------------------------------------------
-- 直播模块（原 schema.sql 缺失，此处补全）
-- ---------------------------------------------------------------

CREATE TABLE IF NOT EXISTS live_rooms (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  user_id BIGINT UNSIGNED NOT NULL,
  title VARCHAR(120) NOT NULL,
  cover VARCHAR(255) DEFAULT NULL,
  category_id BIGINT UNSIGNED DEFAULT 0,
  stream_key VARCHAR(64) NOT NULL,
  status TINYINT DEFAULT 0 COMMENT '0未开播 1直播中 2已结束',
  viewers BIGINT DEFAULT 0,
  peak_viewers BIGINT DEFAULT 0,
  total_viewers BIGINT DEFAULT 0,
  like_count BIGINT DEFAULT 0,
  started_at DATETIME DEFAULT NULL,
  ended_at DATETIME DEFAULT NULL,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_stream_key (stream_key),
  UNIQUE KEY uk_user (user_id),
  KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS live_messages (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  room_id BIGINT UNSIGNED NOT NULL,
  user_id BIGINT UNSIGNED NOT NULL,
  content VARCHAR(200) NOT NULL,
  type TINYINT DEFAULT 0 COMMENT '0聊天 1弹幕 2系统 3进入',
  color VARCHAR(16) DEFAULT '#ffffff',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_room_id (room_id, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS live_viewers (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  room_id BIGINT UNSIGNED NOT NULL,
  user_id BIGINT UNSIGNED NOT NULL,
  last_seen DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_room_user (room_id, user_id),
  KEY idx_room_seen (room_id, last_seen)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS live_likes (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  room_id BIGINT UNSIGNED NOT NULL,
  user_id BIGINT UNSIGNED NOT NULL,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_room_user (room_id, user_id),
  KEY idx_room (room_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
