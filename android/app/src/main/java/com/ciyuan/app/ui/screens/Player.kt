package com.ciyuan.app.ui.screens

import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.ciyuan.app.data.ApiException
import com.ciyuan.app.data.Comment
import com.ciyuan.app.data.DanmakuItem
import com.ciyuan.app.data.Repo
import com.ciyuan.app.data.Session
import com.ciyuan.app.data.Video
import com.ciyuan.app.ui.Avatar
import com.ciyuan.app.ui.DanmakuView
import com.ciyuan.app.ui.EmptyBox
import com.ciyuan.app.ui.LoadingBox
import com.ciyuan.app.ui.dateOnly
import com.ciyuan.app.ui.fmtCount
import com.ciyuan.app.ui.timeAgo
import com.ciyuan.app.ui.toast
import com.ciyuan.app.ui.theme.Blue
import com.ciyuan.app.ui.theme.Ink
import com.ciyuan.app.ui.theme.Ink2
import com.ciyuan.app.ui.theme.Ink3
import com.ciyuan.app.ui.theme.Line
import com.ciyuan.app.ui.theme.Pink
import com.ciyuan.app.ui.theme.PinkSoft
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun PlayerScreen(
    videoId: Long,
    onBack: () -> Unit,
    onOpenVideo: (Long) -> Unit,
    onOpenUser: (Long) -> Unit,
    onLogin: () -> Unit
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val scroll = rememberScrollState()

    var video by remember { mutableStateOf<Video?>(null) }
    var related by remember { mutableStateOf<List<Video>>(emptyList()) }
    var danmaku by remember { mutableStateOf<List<DanmakuItem>>(emptyList()) }
    var comments by remember { mutableStateOf<List<Comment>>(emptyList()) }
    var commentPage by remember { mutableLongStateOf(1) }
    var commentPages by remember { mutableLongStateOf(1) }
    var dmOn by remember { mutableStateOf(true) }
    var dmText by remember { mutableStateOf("") }
    var commentText by remember { mutableStateOf("") }
    var replyTo by remember { mutableStateOf<Comment?>(null) }
    var loading by remember { mutableStateOf(true) }

    val exo = remember { ExoPlayer.Builder(ctx).build() }
    val dmView = remember {
        DanmakuView(ctx).apply {
            isClickable = false
            isFocusable = false
            timeProvider = { exo.currentPosition / 1000.0 }
            playingProvider = { exo.isPlaying }
        }
    }

    DisposableEffect(Unit) {
        onDispose { runCatching { exo.release() } }
    }

    LaunchedEffect(videoId) {
        loading = true
        try {
            val v = Repo.video(videoId)
            video = v
            launch { related = runCatching { Repo.related(videoId) }.getOrDefault(emptyList()) }
            launch { danmaku = runCatching { Repo.danmaku(videoId) }.getOrDefault(emptyList()) }
            val r = Repo.comments(videoId, 1)
            comments = r.items()
            commentPage = 1
            commentPages = r.pages
        } catch (e: ApiException) {
            toast(ctx, e.message)
        } finally {
            loading = false
        }
    }

    LaunchedEffect(video?.videoUrl) {
        val url = Session.mediaUrl(video?.videoUrl)
        if (!url.isNullOrBlank()) {
            runCatching {
                exo.setMediaItem(MediaItem.fromUri(url))
                exo.prepare()
                exo.playWhenReady = true
            }
        }
    }

    LaunchedEffect(danmaku) { dmView.items = danmaku }
    LaunchedEffect(dmOn) { dmView.danmakuEnabled = dmOn }

    LaunchedEffect(video?.id) {
        val id = video?.id ?: return@LaunchedEffect
        while (true) {
            delay(10000)
            if (Session.loggedIn && exo.isPlaying) {
                runCatching { Repo.reportHistory(id, (exo.currentPosition / 1000).toInt()) }
            }
        }
    }

    fun requireLogin(): Boolean {
        if (!Session.loggedIn) {
            toast(ctx, "请先登录")
            onLogin()
            return false
        }
        return true
    }

    fun act(kind: String) {
        if (!requireLogin()) return
        val v = video ?: return
        scope.launch {
            try {
                when (kind) {
                    "like" -> {
                        val r = Repo.like(v.id)
                        toast(ctx, r.msg)
                        video = v.copy(liked = r.active, likeCount = r.count)
                    }
                    "coin" -> {
                        val r = Repo.coin(v.id)
                        toast(ctx, r.msg)
                        video = v.copy(coined = true, coinCount = r.count)
                    }
                    else -> {
                        val r = Repo.fav(v.id)
                        toast(ctx, r.msg)
                        video = v.copy(faved = r.active, favCount = r.count)
                    }
                }
            } catch (e: ApiException) {
                toast(ctx, e.message)
            }
        }
    }

    fun sendDanmaku() {
        if (!requireLogin()) return
        val text = dmText.trim()
        if (text.isEmpty()) return
        scope.launch {
            try {
                val d = Repo.sendDanmaku(videoId, text, exo.currentPosition / 1000.0, "#ffffff")
                danmaku = danmaku + d
                dmText = ""
                video = video?.copy(danmakuCount = (video?.danmakuCount ?: 0) + 1)
                toast(ctx, "弹幕已发送")
            } catch (e: ApiException) {
                toast(ctx, e.message)
            }
        }
    }

    fun sendComment() {
        if (!requireLogin()) return
        val text = commentText.trim()
        if (text.isEmpty()) return
        val target = replyTo
        scope.launch {
            try {
                val c = Repo.addComment(videoId, text, target?.id, target?.user?.id)
                comments = listOf(c) + comments
                commentText = ""
                replyTo = null
                video = video?.copy(commentCount = (video?.commentCount ?: 0) + 1)
                toast(ctx, "评论成功")
            } catch (e: ApiException) {
                toast(ctx, e.message)
            }
        }
    }

    fun toggleFollow() {
        if (!requireLogin()) return
        val author = video?.author ?: return
        scope.launch {
            try {
                val r = Repo.follow(author.id)
                toast(ctx, r.msg)
                video = video?.copy(author = author.copy(followed = r.active, followers = r.count))
            } catch (e: ApiException) {
                toast(ctx, e.message)
            }
        }
    }

    fun loadMoreComments() {
        if (commentPage >= commentPages) return
        scope.launch {
            try {
                val r = Repo.comments(videoId, (commentPage + 1).toInt())
                comments = comments + r.items()
                commentPage = r.page
                commentPages = r.pages
            } catch (e: ApiException) {
                toast(ctx, e.message)
            }
        }
    }

    if (loading && video == null) {
        LoadingBox(Modifier.fillMaxSize())
        return
    }
    val v = video
    if (v == null) {
        EmptyBox("视频不存在或已下架")
        return
    }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(scroll)) {
        // 播放器
        Box(modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f).background(Color.Black)) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { c ->
                    FrameLayout(c).apply {
                        addView(
                            PlayerView(c).apply {
                                player = exo
                                useController = true
                                setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                            },
                            FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
                        )
                        addView(dmView, FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))
                    }
                }
            )
            Box(
                modifier = Modifier
                    .padding(10.dp)
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Color(0x66000000))
                    .clickable { onBack() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.ArrowBack, "返回", tint = Color.White, modifier = Modifier.size(20.dp))
            }
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(10.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (dmOn) Color(0x99000000) else Color(0x66000000))
                    .clickable { dmOn = !dmOn }
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text(
                    text = if (dmOn) "弹幕 开" else "弹幕 关",
                    color = if (dmOn) Color.White else Color(0x99FFFFFF),
                    fontSize = 11.sp
                )
            }
        }

        // 弹幕输入
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFFF1F2F3))
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                BasicTextField(
                    value = dmText,
                    onValueChange = { if (it.length <= 60) dmText = it },
                    singleLine = true,
                    textStyle = TextStyle(fontSize = 13.sp, color = Ink),
                    decorationBox = { inner ->
                        if (dmText.isEmpty()) Text("发个友善的弹幕见证当下", fontSize = 12.5.sp, color = Ink3)
                        inner()
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(18.dp))
                    .background(Pink)
                    .clickable { sendDanmaku() }
                    .padding(horizontal = 16.dp, vertical = 9.dp)
            ) {
                Text("发送", color = Color.White, fontSize = 12.5.sp)
            }
        }

        // 标题与数据
        Column(modifier = Modifier.padding(horizontal = 12.dp)) {
            Text(
                text = v.title ?: "",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Ink,
                lineHeight = 23.sp
            )
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${fmtCount(v.views)} 播放", fontSize = 11.5.sp, color = Ink3)
                Text("  ·  ${fmtCount(v.danmakuCount)} 弹幕", fontSize = 11.5.sp, color = Ink3)
                Text("  ·  ${dateOnly(v.createdAt)}", fontSize = 11.5.sp, color = Ink3)
                Spacer(Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(9.dp))
                        .background(PinkSoft)
                        .padding(horizontal = 6.dp, vertical = 1.dp)
                ) {
                    Text(v.categoryName ?: "", fontSize = 10.sp, color = Pink)
                }
            }
        }

        // 三连
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            ActionItem(Icons.Filled.ThumbUp, fmtCount(v.likeCount), v.liked) { act("like") }
            ActionItem(Icons.Filled.MonetizationOn, fmtCount(v.coinCount), v.coined) { act("coin") }
            ActionItem(Icons.Filled.Star, fmtCount(v.favCount), v.faved) { act("fav") }
            ActionItem(Icons.Filled.Share, "分享", false) {
                toast(ctx, "视频地址：${Session.baseUrl}/v/${v.id}")
            }
        }

        // UP 主
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color.White)
                .clickable { v.author?.let { onOpenUser(it.id) } }
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Avatar(v.author, 42.dp)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    v.author?.displayName ?: "",
                    fontSize = 13.5.sp,
                    color = Ink,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    "${fmtCount(v.author?.followers ?: 0)} 粉丝 · Lv${v.author?.level ?: 0}",
                    fontSize = 11.sp,
                    color = Ink3
                )
            }
            if (!v.mine) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (v.author?.followed == true) Color(0xFFF1F2F3) else Pink)
                        .clickable { toggleFollow() }
                        .padding(horizontal = 16.dp, vertical = 7.dp)
                ) {
                    Text(
                        text = if (v.author?.followed == true) "已关注" else "+ 关注",
                        color = if (v.author?.followed == true) Ink2 else Color.White,
                        fontSize = 12.5.sp
                    )
                }
            }
        }

        // 简介
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Text(
                text = v.description?.ifBlank { "这个 UP 主很懒，什么都没有写～" }
                    ?: "这个 UP 主很懒，什么都没有写～",
                fontSize = 12.5.sp,
                color = Ink2,
                lineHeight = 19.sp
            )
            val tags = (v.tags ?: "").split(",", "，", " ").filter { it.isNotBlank() }
            if (tags.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    tags.take(4).forEach { t ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFF1F2F3))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) { Text("#$t", fontSize = 10.5.sp, color = Ink2) }
                    }
                }
            }
        }

        // 评论
        Column(modifier = Modifier.padding(horizontal = 12.dp).padding(top = 6.dp)) {
            Text("评论 ${fmtCount(v.commentCount)}", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Ink)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Avatar(Session.user, 32.dp)
                Spacer(Modifier.width(8.dp))
                OutlinedTextField(
                    value = commentText,
                    onValueChange = { commentText = it },
                    modifier = Modifier.weight(1f),
                    placeholder = {
                        Text(
                            if (replyTo != null) "回复 @${replyTo?.user?.displayName}" else "发一条友善的评论吧～",
                            fontSize = 12.5.sp,
                            color = Ink3
                        )
                    },
                    maxLines = 3,
                    textStyle = TextStyle(fontSize = 13.sp),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Pink,
                        unfocusedBorderColor = Line,
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    ),
                    trailingIcon = {
                        Icon(
                            Icons.Filled.Send,
                            "发送",
                            tint = Pink,
                            modifier = Modifier.size(20.dp).clickable { sendComment() }
                        )
                    }
                )
            }
            if (replyTo != null) {
                Text(
                    "取消回复",
                    fontSize = 11.sp,
                    color = Blue,
                    modifier = Modifier.padding(start = 40.dp, top = 4.dp).clickable { replyTo = null }
                )
            }
            Spacer(Modifier.height(4.dp))
        }

        if (comments.isEmpty()) {
            EmptyBox("还没有评论，来抢沙发")
        } else {
            comments.forEach { c ->
                CommentRow(
                    comment = c,
                    onReply = { replyTo = c },
                    onLike = {
                        if (Session.loggedIn) {
                            scope.launch {
                                runCatching { Repo.likeComment(c.id) }.onSuccess { r ->
                                    comments = comments.map {
                                        if (it.id == c.id) it.copy(liked = r.active, likeCount = r.count) else it
                                    }
                                }
                            }
                        } else {
                            requireLogin()
                        }
                    }
                )
            }
            if (commentPage < commentPages) {
                Text(
                    "加载更多评论",
                    fontSize = 12.5.sp,
                    color = Pink,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { loadMoreComments() }
                        .padding(vertical = 10.dp),
                    textAlign = TextAlign.Center
                )
            }
        }

        Text(
            "相关推荐",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = Ink,
            modifier = Modifier.padding(start = 12.dp, top = 16.dp, bottom = 6.dp)
        )
        related.forEach { r ->
            RelatedRow(r) { onOpenVideo(r.id) }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun ActionItem(icon: ImageVector, label: String, active: Boolean, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Icon(icon, contentDescription = label, tint = if (active) Pink else Ink2, modifier = Modifier.size(22.dp))
        Spacer(Modifier.height(3.dp))
        Text(label, fontSize = 10.5.sp, color = if (active) Pink else Ink3)
    }
}

@Composable
private fun CommentRow(comment: Comment, onReply: () -> Unit, onLike: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp)) {
        Avatar(comment.user, 32.dp)
        Spacer(Modifier.width(9.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    comment.user?.displayName ?: "",
                    fontSize = 11.5.sp,
                    color = Ink2,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.width(6.dp))
                Text(timeAgo(comment.createdAt), fontSize = 10.sp, color = Ink3)
            }
            Text(
                text = comment.content ?: "",
                fontSize = 13.sp,
                color = Ink,
                lineHeight = 19.sp,
                modifier = Modifier.padding(top = 3.dp)
            )
            Row(modifier = Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { onLike() }) {
                    Icon(
                        Icons.Filled.ThumbUp,
                        null,
                        tint = if (comment.liked) Pink else Ink3,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(Modifier.width(3.dp))
                    Text(
                        if (comment.likeCount > 0) fmtCount(comment.likeCount) else "",
                        fontSize = 10.5.sp,
                        color = if (comment.liked) Pink else Ink3
                    )
                }
                Spacer(Modifier.width(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { onReply() }) {
                    Icon(Icons.Filled.ChatBubbleOutline, null, tint = Ink3, modifier = Modifier.size(13.dp))
                    Spacer(Modifier.width(3.dp))
                    Text("回复", fontSize = 10.5.sp, color = Ink3)
                }
            }
            val children = comment.children ?: emptyList()
            if (children.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .padding(top = 6.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFF7F8FA))
                        .padding(8.dp)
                ) {
                    children.take(3).forEach { ch ->
                        Row(modifier = Modifier.padding(vertical = 2.dp)) {
                            Text(
                                text = "${ch.user?.displayName ?: ""}：",
                                fontSize = 11.5.sp,
                                color = Ink2,
                                fontWeight = FontWeight.Medium
                            )
                            Text(ch.content ?: "", fontSize = 11.5.sp, color = Ink2, lineHeight = 17.sp)
                        }
                    }
                    if (comment.childCount > 3) {
                        Text("共 ${comment.childCount} 条回复", fontSize = 10.5.sp, color = Blue)
                    }
                }
            }
        }
    }
}

@Composable
private fun RelatedRow(video: Video, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .width(130.dp)
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFFEEF0F2))
        ) {
            AsyncImage(
                model = Session.mediaUrl(video.cover),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        Spacer(Modifier.width(9.dp))
        Column(Modifier.weight(1f)) {
            Text(
                video.title ?: "",
                fontSize = 12.5.sp,
                color = Ink,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 17.sp
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "${video.author?.displayName ?: ""} · ${fmtCount(video.views)} 播放",
                fontSize = 10.5.sp,
                color = Ink3
            )
        }
    }
}
