# 次元站 CiYuan

一个仿 B 站的视频社区平台，包含 **Web 主站 + Android 客户端 + 直播 + 大模型推荐**。

演示站点：<http://121.43.245.66>

---

## 功能特性

**视频**
- 投稿上传，自动探测编码并按需转码为 H.264 + AAC
- 无封面时自动抽帧生成
- 分区浏览、关键词搜索、多种排序（最新 / 播放 / 弹幕 / 投币 / 点赞）
- 播放历史、相关推荐

**互动**
- 弹幕（按时间轴渲染，支持自定义颜色）
- 两级评论树（根评论分页 + 子回复懒加载）
- 点赞 / 投币 / 收藏（三连，带硬币余额校验）
- 关注与粉丝、消息通知

**直播**
- 两种推流方式：OBS 直推 RTMP，或浏览器摄像头（WebSocket 推流）
- 服务端 ffmpeg 转码后推入本地 RTMP，由 nginx-rtmp 切 HLS 分发
- 直播间聊天、在线人数（心跳滑动窗口）、点赞

**大模型**
- 基于站内真实视频的 AI 推荐，先检索后推荐，**不会编造不存在的视频**
- 未配置 API Key 或调用失败时自动降级为热门推荐，接口不会空手

**管理后台**
- 站点统计、视频审核上下架、用户封禁与角色管理、分区管理

---

## 技术栈

| 层 | 技术 |
|---|---|
| 后端 | Java 17 · Spring Boot 3.2.5 · MyBatis-Plus 3.5.5 · MySQL 8.0 · JWT (jjwt) · WebSocket |
| Web | Vue 3 + Vite 构建的 SPA（`web/` 下为构建产物与手写静态页） |
| Android | Kotlin · Jetpack Compose · Retrofit · OkHttp · Coil · Media3 (ExoPlayer) |
| 直播 | nginx-rtmp-module · FFmpeg · HLS |
| 大模型 | DeepSeek（OpenAI 兼容协议） |

---

## 项目结构

```
.
├── server/                      # Spring Boot 后端
│   ├── pom.xml
│   ├── sql/schema.sql           # 建表脚本（16 张表）
│   └── src/main/java/com/ciyuan/
│       ├── common/              # ApiResponse、JWT、鉴权拦截器、异常处理
│       ├── config/              # Web、WebSocket、MyBatis-Plus、线程池
│       ├── controller/          # 9 个 REST 控制器
│       ├── service/             # 11 个业务服务（含转码、直播、AI）
│       ├── mapper/              # 16 个 MyBatis-Plus Mapper
│       ├── entity/ dto/ vo/     # 数据模型
│       └── resources/application.yml
│
├── android/                     # Android 客户端
│   └── app/src/main/java/com/ciyuan/app/
│       ├── data/                # Net（Retrofit）、Session、Models
│       └── ui/                  # 10 个 Compose 页面 + 自绘弹幕层
│
├── web/                         # Web 端
│   ├── index.html               # 主站入口（引用构建产物）
│   ├── ai/                      # 大模型推荐页（原生 HTML/JS）
│   └── live/                    # 直播页（原生 HTML/JS + hls.js）
│
└── deploy/                      # 部署配置
    ├── nginx-ciyuan.conf        # 站点 + 反向代理
    ├── nginx-rtmp.conf          # RTMP → HLS
    └── ciyuan.service           # systemd 单元
```

> 主站 SPA 的 Vue 源码未包含在本仓库中，`web/index.html` 引用的是构建产物。
> `web/ai` 与 `web/live` 为原生 JS 页面，源码完整。

---

## 快速开始

### 环境要求

- JDK 17、Maven 3.6+
- MySQL 8.0
- FFmpeg（转码与抽帧）
- Node.js 18+（仅 Android 构建或前端二次开发需要）
- Nginx（含 nginx-rtmp-module，仅直播需要）

### 1. 初始化数据库

```bash
mysql -uroot -p -e "CREATE DATABASE ciyuan DEFAULT CHARSET utf8mb4 COLLATE utf8mb4_unicode_ci;"
mysql -uroot -p ciyuan < server/sql/schema.sql
```

### 2. 启动后端

```bash
cd server
# 按需修改 src/main/resources/application.yml 中的存储路径、ffmpeg 路径等
export DB_PASSWORD='你的数据库密码'
export CIYUAN_JWT_SECRET="$(openssl rand -base64 48)"
mvn spring-boot:run
```

服务默认监听 `8080`，媒体文件通过 `/media/**` 对外暴露（需 Nginx 或自行配置静态映射）。

### 3. Web 前端

把 `web/` 目录部署到站点根目录，并将 `/api`、`/media`、`/ws` 反向代理到后端：

```nginx
location ^~ /api/ { proxy_pass http://127.0.0.1:8080; }
location ^~ /ws/  { proxy_pass http://127.0.0.1:8080; proxy_set_header Upgrade $http_upgrade;
                    proxy_set_header Connection "upgrade"; }
location ^~ /media/ { alias /path/to/storage/; }
```

完整配置见 `deploy/nginx-ciyuan.conf`。

### 4. Android

```bash
cd android
echo "sdk.dir=/path/to/android-sdk" > local.properties
./gradlew assembleDebug
```

App 内可修改服务器地址，默认指向演示站点（见 `data/Session.kt` 的 `DEFAULT_BASE`）。

---

## 配置

敏感配置全部通过环境变量注入，配置文件本身不含密钥：

| 变量 | 说明 | 必填 |
|---|---|---|
| `DB_HOST` / `DB_PORT` | 数据库地址，默认 `127.0.0.1:3306` | 否 |
| `DB_USER` | 数据库用户，默认 `ciyuan` | 否 |
| `DB_PASSWORD` | 数据库密码 | **是** |
| `CIYUAN_JWT_SECRET` | JWT 签名密钥，**务必改成随机长字符串** | **是** |
| `CIYUAN_AI_KEY` | DeepSeek API Key，不填则 AI 降级为热门推荐 | 否 |

参考 `.env.example`。使用 systemd 时可用 `EnvironmentFile=` 注入（见 `deploy/ciyuan.service`）。

---

## API 概览

统一响应：`{"code":0,"msg":"ok","data":...}`，`code` 非 0 表示失败（401 未登录 / 403 无权限 / 404 不存在）。
🔒 需登录（`Authorization: Bearer <token>`），👑 需管理员。

| 模块 | 主要接口 |
|---|---|
| 认证 | `POST /api/auth/register`、`POST /api/auth/login`、`GET /api/auth/me` |
| 视频 | `GET /api/videos`、`GET /api/videos/{id}`、`POST /api/videos` 🔒、`POST /api/videos/{id}/like|coin|fav` 🔒 |
| 评论 | `GET /api/comments`、`POST /api/comments` 🔒、`POST /api/comments/{id}/like` 🔒 |
| 弹幕 | `GET /api/danmaku/{videoId}`、`POST /api/danmaku` 🔒 |
| 用户 | `GET /api/users/{id}`、`POST /api/users/{id}/follow` 🔒、`GET /api/users/me/relations` 🔒 |
| 搜索 | `GET /api/search?keyword=&type=video|user` |
| 通知 | `GET /api/notifications` 🔒、`GET /api/notifications/unread` 🔒 |
| 直播 | `GET /api/live/rooms`、`POST /api/live/rooms` 🔒、`WS /ws/live/{streamKey}` |
| 大模型 | `GET /api/ai/status`、`POST /api/ai/chat` |
| 后台 | `GET /api/admin/stats` 👑、`POST /api/admin/videos/{id}/status` 👑 |

---

## 核心设计

### 视频转码流水线

```
上传 → 落盘 storage/video/{uuid}.ext → ffprobe 探测编码
     ├─ H.264 + AAC/MP3/无音轨 → 直接发布（status=1）
     └─ 其他编码 → status=0（转码中），丢进单线程池异步转码
                    → libx264 veryfast crf24、宽≤1280、AAC 128k、+faststart
                    → 成功后替换 URL、删除原文件、置 status=1
     └─ 无封面 → 取时长 1/3 处抽帧，缩放 854 宽生成 jpg
```

线程池刻意限制为 core=1 / max=2，避免小规格服务器被转码打满 CPU。

### 直播链路

```
方式 A（OBS）：rtmp://host:1935/live + streamKey
方式 B（浏览器）：getUserMedia → MediaRecorder → WebSocket 二进制帧
                → 服务端 ffmpeg（pipe:0 → libx264 zerolatency → rtmp://127.0.0.1:1935）
        ↓
nginx-rtmp 接收 → HLS 切片（2s / 列表 30s）→ /livehls/{streamKey}.m3u8
        ↓
观众用 hls.js 或 ExoPlayer 播放（deny play all，不开放 RTMP 拉流）
```

在线人数用「最近 60 秒内有心跳」的滑动窗口统计，峰值用 `GREATEST` 更新。

### 大模型推荐的防幻觉设计

模型不生成视频信息，只做「选择题」：

```
用户提问
  ↓
① 检索：按分区 + 关键词从库中取 ≤24 个候选（分区优先，关键词退让，不足则热门补齐）
  ↓
② 把候选清单（id/标题/分区/标签/时长/播放）塞进 prompt
  ↓
③ 模型只能输出 {"reply":"...","picks":[{"id":12,"reason":"..."}]}
  ↓
④ 服务端校验 picks 的 id 必须在候选集内，越界的直接丢弃
  ↓
⑤ 用 id 回查数据库拼装完整视频信息返回前端
```

任何环节失败（未配置 Key / 超时 / 401 / 402 / 429 / 输出非 JSON）都会降级为热门推荐，
保证接口始终有结果返回。

---

## 部署

```bash
# 后端
mvn -f server/pom.xml clean package -DskipTests
cp server/target/ciyuan-server.jar /opt/ciyuan/app.jar
cp deploy/ciyuan.service /etc/systemd/system/
systemctl daemon-reload && systemctl enable --now ciyuan

# 站点
cp -r web/* /var/www/ciyuan/
cp deploy/nginx-ciyuan.conf /etc/nginx/sites-available/ciyuan
ln -s /etc/nginx/sites-available/ciyuan /etc/nginx/sites-enabled/
cp deploy/nginx-rtmp.conf /etc/nginx/   # 并在 nginx.conf 中 include
nginx -t && systemctl reload nginx
```

需要放行的端口：`80`、`443`、`1935`（推流）。

---

## 说明

- 本项目为学习与演示用途，未实现内容审核、风控、限流等生产级能力，请勿直接用于公开运营。
- 数据库密码、JWT 密钥、大模型 Key 均已从仓库中移除，部署时请自行配置。
- 若用于生产，建议额外补充：接口限流、对象存储、转码队列、日志脱敏。
