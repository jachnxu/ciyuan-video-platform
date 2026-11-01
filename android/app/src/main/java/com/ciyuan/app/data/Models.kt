package com.ciyuan.app.data

data class ApiResp<T>(
    val code: Int = -1,
    val msg: String? = null,
    val data: T? = null
)

data class PageResult<T>(
    val list: List<T>? = null,
    val total: Long = 0,
    val page: Long = 1,
    val size: Long = 20,
    val pages: Long = 1
) {
    fun items(): List<T> = list ?: emptyList()
}

data class User(
    val id: Long = 0,
    val username: String? = null,
    val nickname: String? = null,
    val avatar: String? = null,
    val sign: String? = null,
    val gender: Int = 0,
    val level: Int = 0,
    val coin: Int = 0,
    val role: String? = null,
    val createdAt: String? = null,
    val followers: Long = 0,
    val following: Long = 0,
    val videoCount: Long = 0,
    val totalViews: Long = 0,
    val followed: Boolean? = false
) {
    val isAdmin: Boolean get() = role == "ADMIN"
    val displayName: String get() = nickname ?: username ?: "用户$id"
}

data class Video(
    val id: Long = 0,
    val title: String? = null,
    val description: String? = null,
    val cover: String? = null,
    val videoUrl: String? = null,
    val duration: Int = 0,
    val width: Int = 0,
    val height: Int = 0,
    val status: Int = 1,
    val views: Long = 0,
    val danmakuCount: Long = 0,
    val likeCount: Long = 0,
    val coinCount: Long = 0,
    val favCount: Long = 0,
    val commentCount: Long = 0,
    val tags: String? = null,
    val createdAt: String? = null,
    val categoryId: Long = 0,
    val categoryName: String? = null,
    val author: User? = null,
    val liked: Boolean = false,
    val coined: Boolean = false,
    val faved: Boolean = false,
    val mine: Boolean = false
)

data class Comment(
    val id: Long = 0,
    val content: String? = null,
    val createdAt: String? = null,
    val likeCount: Long = 0,
    val liked: Boolean = false,
    val user: User? = null,
    val replyUser: User? = null,
    val children: List<Comment>? = null,
    val childCount: Int = 0
)

data class DanmakuItem(
    val id: Long = 0,
    val text: String? = null,
    val time: Double = 0.0,
    val color: String? = "#ffffff",
    val userId: Long = 0
)

data class Category(
    val id: Long = 0,
    val name: String? = null,
    val slug: String? = null,
    val sort: Int = 0
)

data class LoginResult(
    val token: String? = null,
    val user: User? = null
)

data class ToggleResult(
    val active: Boolean = false,
    val count: Long = 0,
    val msg: String? = null
)

data class Notif(
    val id: Long = 0,
    val type: String? = null,
    val content: String? = null,
    val isRead: Int = 0,
    val createdAt: String? = null,
    val fromUser: User? = null,
    val videoId: Long? = null,
    val videoTitle: String? = null,
    val videoCover: String? = null
)

data class LoginReq(val username: String, val password: String)
data class RegisterReq(val username: String, val password: String, val nickname: String?)
data class CommentReq(val videoId: Long, val content: String, val parentId: Long?, val replyUserId: Long?)
data class DanmakuReq(val videoId: Long, val content: String, val time: Double, val color: String)
data class ProfileReq(val nickname: String?, val sign: String?)


data class LiveRoom(
    val id: Long = 0,
    val userId: Long = 0,
    val title: String? = null,
    val cover: String? = null,
    val categoryId: Long = 0,
    val categoryName: String? = null,
    val status: Int = 0,
    val viewers: Long = 0,
    val peakViewers: Long = 0,
    val totalViewers: Long = 0,
    val likeCount: Long = 0,
    val streamKey: String? = null,
    val pushUrl: String? = null,
    val playUrl: String? = null,
    val startedAt: String? = null,
    val endedAt: String? = null,
    val createdAt: String? = null,
    val author: User? = null,
    val mine: Boolean = false,
    val liked: Boolean = false
)

data class LiveMessage(
    val id: Long = 0,
    val roomId: Long = 0,
    val userId: Long = 0,
    val content: String? = null,
    val type: Int = 0,
    val color: String? = "#ffffff",
    val createdAt: String? = null,
    val user: User? = null
)

data class LiveRoomReq(val title: String, val cover: String?, val categoryId: Long)
data class LiveMessageReq(val content: String, val type: Int, val color: String)
data class OnlineResult(val viewers: Long = 0)
