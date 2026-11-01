package com.ciyuan.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Icon
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.ciyuan.app.data.LiveMessage
import com.ciyuan.app.data.LiveRoom
import com.ciyuan.app.data.Repo
import com.ciyuan.app.data.Session
import com.ciyuan.app.ui.Avatar
import com.ciyuan.app.ui.toast
import com.ciyuan.app.ui.theme.Bg
import com.ciyuan.app.ui.theme.Blue
import com.ciyuan.app.ui.theme.Ink
import com.ciyuan.app.ui.theme.Ink3
import com.ciyuan.app.ui.theme.Pink
import com.ciyuan.app.ui.theme.PinkSoft
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun LiveRoomScreen(
    roomId: Long,
    onBack: () -> Unit,
    onLogin: () -> Unit
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var room by remember { mutableStateOf<LiveRoom?>(null) }
    var messages by remember { mutableStateOf<List<LiveMessage>>(emptyList()) }
    var lastId by remember { mutableLongStateOf(0L) }
    var online by remember { mutableLongStateOf(0L) }
    var input by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    val exo = remember { ExoPlayer.Builder(ctx).build() }
    DisposableEffect(Unit) {
        onDispose { exo.release() }
    }

    fun playIfLive(r: LiveRoom) {
        if (r.status == 1 && !r.playUrl.isNullOrBlank()) {
            val url = Session.mediaUrl(r.playUrl)
            if (url != null) {
                exo.setMediaItem(MediaItem.fromUri(url))
                exo.prepare()
                exo.playWhenReady = true
            }
        }
    }

    LaunchedEffect(roomId) {
        runCatching { Repo.liveRoom(roomId) }
            .onSuccess { r -> room = r; online = r.viewers; playIfLive(r) }
            .onFailure { toast(ctx, it.message ?: "加载失败") }
    }

    // 消息轮询
    LaunchedEffect(roomId) {
        while (true) {
            runCatching { Repo.liveMessages(roomId, lastId) }
                .onSuccess { list ->
                    if (list.isNotEmpty()) {
                        lastId = maxOf(lastId, list.maxOf { it.id })
                        messages = (messages + list).takeLast(300)
                        try {
                            listState.animateScrollToItem((messages.size - 1).coerceAtLeast(0))
                        } catch (e: Exception) {
                        }
                    }
                }
            delay(3000)
        }
    }

    // 在线人数 + 未开播时自动检测开播
    LaunchedEffect(roomId) {
        while (true) {
            runCatching { Repo.liveOnline(roomId) }.onSuccess { online = it.viewers }
            val cur = room
            if (cur == null || cur.status != 1) {
                runCatching { Repo.liveRoom(roomId) }.onSuccess { nr ->
                    if (nr.status == 1 && cur?.status != 1) {
                        room = nr
                        playIfLive(nr)
                    } else {
                        room = nr
                    }
                }
            }
            delay(8000)
        }
    }

    // 心跳（登录用户）
    LaunchedEffect(roomId, Session.token) {
        if (Session.loggedIn) {
            while (true) {
                runCatching { Repo.liveHeartbeat(roomId) }.onSuccess { online = it.viewers }
                delay(15000)
            }
        }
    }

    val r = room

    Column(Modifier.fillMaxSize().background(Bg)) {
        Row(
            Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 6.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.ArrowBack, contentDescription = "返回",
                modifier = Modifier.clickable(onClick = onBack).padding(8.dp), tint = Ink
            )
            Text(
                r?.title ?: "直播间",
                fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Ink,
                maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f)
            )
        }

        Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f).background(Color.Black)) {
            AndroidView(
                factory = { c -> PlayerView(c).apply { player = exo; useController = true } },
                modifier = Modifier.fillMaxSize()
            )
            if (r != null && r.status != 1) {
                Box(
                    Modifier.fillMaxSize().background(Color(0xCC000000)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (r.status == 2) "本场直播已结束" else "主播暂未开播",
                        color = Color.White, fontSize = 14.sp
                    )
                }
            }
        }

        Row(
            Modifier.fillMaxWidth().background(Color.White).padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Avatar(r?.author, 34.dp)
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    r?.author?.displayName ?: "主播",
                    fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold, color = Ink
                )
                Text(
                    "在线 ${fmtCount(online)} · 累计 ${fmtCount(r?.totalViewers ?: 0)}",
                    fontSize = 11.5.sp, color = Ink3
                )
            }
            Row(
                Modifier.clip(RoundedCornerShape(16.dp)).background(PinkSoft).clickable {
                    if (!Session.loggedIn) {
                        onLogin()
                        return@clickable
                    }
                    scope.launch {
                        runCatching { Repo.likeLive(roomId) }
                            .onSuccess { t ->
                                room = room?.copy(likeCount = t.count, liked = t.active)
                                toast(ctx, if (t.active) "点赞成功" else "已取消")
                            }
                            .onFailure { toast(ctx, it.message ?: "操作失败") }
                    }
                }.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Favorite, contentDescription = null, tint = Pink, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(4.dp))
                Text(
                    fmtCount(r?.likeCount ?: 0),
                    color = Pink, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold
                )
            }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(messages, key = { it.id }) { m ->
                LiveMessageRow(m, r?.userId ?: 0L)
            }
        }

        Row(
            Modifier.fillMaxWidth().background(Color.White).padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BasicTextField(
                value = input,
                onValueChange = { input = it.take(200) },
                singleLine = true,
                textStyle = TextStyle(fontSize = 13.sp, color = Ink),
                modifier = Modifier.weight(1f).clip(RoundedCornerShape(18.dp))
                    .background(Bg).padding(horizontal = 14.dp, vertical = 10.dp),
                decorationBox = { inner ->
                    if (input.isEmpty()) Text("说点什么…", color = Ink3, fontSize = 13.sp)
                    inner()
                }
            )
            Spacer(Modifier.width(8.dp))
            Text(
                "发送",
                color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clip(RoundedCornerShape(18.dp)).background(Pink).clickable {
                    if (!Session.loggedIn) {
                        onLogin()
                        return@clickable
                    }
                    val text = input.trim()
                    if (text.isEmpty()) return@clickable
                    scope.launch {
                        runCatching { Repo.sendLiveMessage(roomId, text) }
                            .onSuccess { m ->
                                messages = messages + m
                                lastId = maxOf(lastId, m.id)
                                input = ""
                            }
                            .onFailure { toast(ctx, it.message ?: "发送失败") }
                    }
                }.padding(horizontal = 18.dp, vertical = 10.dp)
            )
        }
    }
}

@Composable
private fun LiveMessageRow(m: LiveMessage, hostId: Long) {
    val name = m.user?.displayName ?: "用户"
    val isHost = m.userId == hostId
    Text(
        buildAnnotatedString {
            if (m.type == 2) {
                withStyle(SpanStyle(color = Ink3)) { append(m.content ?: "") }
            } else {
                withStyle(
                    SpanStyle(
                        color = if (isHost) Pink else Blue,
                        fontWeight = FontWeight.SemiBold
                    )
                ) { append("$name：") }
                withStyle(SpanStyle(color = Ink)) { append(m.content ?: "") }
            }
        },
        fontSize = 13.sp
    )
}
