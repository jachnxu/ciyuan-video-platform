package com.ciyuan.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ciyuan.app.data.ApiException
import com.ciyuan.app.data.Repo
import com.ciyuan.app.data.Session
import com.ciyuan.app.data.User
import com.ciyuan.app.data.Video
import com.ciyuan.app.ui.Avatar
import com.ciyuan.app.ui.EmptyBox
import com.ciyuan.app.ui.LoadingBox
import com.ciyuan.app.ui.VideoCard
import com.ciyuan.app.ui.fmtCount
import com.ciyuan.app.ui.toast
import com.ciyuan.app.ui.theme.Ink
import com.ciyuan.app.ui.theme.Ink2
import com.ciyuan.app.ui.theme.Ink3
import com.ciyuan.app.ui.theme.Line
import com.ciyuan.app.ui.theme.Pink
import com.ciyuan.app.ui.theme.PinkSoft
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(
    onLogin: () -> Unit,
    onOpen: (Long) -> Unit,
    onList: (String) -> Unit,
    onUpload: () -> Unit,
    onSpace: (Long) -> Unit,
    onNotifications: () -> Unit
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var showServer by remember { mutableStateOf(false) }
    var serverInput by remember { mutableStateOf(Session.baseUrl) }

    LaunchedEffect(Session.token) {
        if (Session.loggedIn) {
            runCatching { Session.updateUser(Repo.me()) }
        }
    }

    if (!Session.loggedIn) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Avatar(null, 76.dp)
            Spacer(Modifier.height(14.dp))
            Text("还没有登录", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Ink)
            Spacer(Modifier.height(6.dp))
            Text("登录后可以投稿、发弹幕、收藏和关注 UP 主", fontSize = 12.5.sp, color = Ink3)
            Spacer(Modifier.height(18.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(22.dp))
                    .background(Pink)
                    .clickable { onLogin() }
                    .padding(horizontal = 40.dp, vertical = 11.dp)
            ) {
                Text("登录 / 注册", color = Color.White, fontSize = 14.sp)
            }
            Spacer(Modifier.height(16.dp))
            Text(
                "服务器：${Session.baseUrl}",
                fontSize = 11.sp,
                color = Ink3,
                modifier = Modifier.clickable {
                    serverInput = Session.baseUrl
                    showServer = true
                }
            )
        }
        if (showServer) {
            ServerDialog(
                value = serverInput,
                onChange = { serverInput = it },
                onDismiss = { showServer = false },
                onConfirm = {
                    Session.saveBase(serverInput)
                    showServer = false
                    toast(ctx, "已保存服务器地址")
                }
            )
        }
        return
    }

    val u = Session.user

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        // 头部
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    androidx.compose.ui.graphics.Brush.linearGradient(
                        listOf(Color(0xFFFFE3EC), Color(0xFFE8F4FF))
                    )
                )
                .padding(horizontal = 16.dp, vertical = 18.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Avatar(u, 64.dp)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        u?.displayName ?: "未登录",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Ink
                    )
                    Text(
                        u?.sign ?: "这个人很神秘，什么都没有写",
                        fontSize = 11.5.sp,
                        color = Ink2,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Lv${u?.level ?: 0} · 硬币 ${u?.coin ?: 0}",
                        fontSize = 10.5.sp,
                        color = Ink3
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                StatCell(fmtCount(u?.videoCount ?: 0), "投稿") { u?.let { onSpace(it.id) } }
                StatCell(fmtCount(u?.followers ?: 0), "粉丝") { u?.let { onSpace(it.id) } }
                StatCell(fmtCount(u?.following ?: 0), "关注") { u?.let { onSpace(it.id) } }
                StatCell(fmtCount(u?.totalViews ?: 0), "播放") { u?.let { onSpace(it.id) } }
            }
        }

        Spacer(Modifier.height(10.dp))
        Column(
            modifier = Modifier
                .padding(horizontal = 12.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White)
        ) {
            MenuRow(Icons.Filled.Upload, "投稿视频", "发布你的作品") { onUpload() }
            Divider()
            MenuRow(Icons.Filled.Person, "我的投稿", "查看已发布视频") { onList("my") }
            Divider()
            MenuRow(Icons.Filled.Star, "我的收藏", "收藏的视频都在这里") { onList("fav") }
            Divider()
            MenuRow(Icons.Filled.History, "观看历史", "最近看过的视频") { onList("history") }
            Divider()
            MenuRow(Icons.Filled.FavoriteBorder, "我赞过的", "点过赞的视频") { onList("like") }
            Divider()
            MenuRow(Icons.Filled.Notifications, "消息中心", "互动与系统通知") { onNotifications() }
        }

        Spacer(Modifier.height(10.dp))
        Column(
            modifier = Modifier
                .padding(horizontal = 12.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White)
        ) {
            MenuRow(Icons.Filled.Cloud, "服务器地址", Session.baseUrl) {
                serverInput = Session.baseUrl
                showServer = true
            }
            Divider()
            MenuRow(Icons.Filled.Logout, "退出登录", null, danger = true) {
                Session.clear()
                toast(ctx, "已退出登录")
            }
        }
        Spacer(Modifier.height(20.dp))
        Text(
            "次元站 Android 1.0.0",
            fontSize = 10.5.sp,
            color = Ink3,
            modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }

    if (showServer) {
        ServerDialog(
            value = serverInput,
            onChange = { serverInput = it },
            onDismiss = { showServer = false },
            onConfirm = {
                Session.saveBase(serverInput)
                showServer = false
                toast(ctx, "已保存：${Session.baseUrl}")
            }
        )
    }
}

@Composable
private fun Divider() {
    Box(modifier = Modifier.fillMaxWidth().height(0.6.dp).background(Line))
}

@Composable
private fun StatCell(value: String, label: String, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { onClick() }.padding(6.dp)
    ) {
        Text(value, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Ink)
        Text(label, fontSize = 10.5.sp, color = Ink3)
    }
}

@Composable
private fun MenuRow(icon: ImageVector, title: String, sub: String?, danger: Boolean = false, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = if (danger) Color(0xFFFF4D4F) else Pink, modifier = Modifier.size(19.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 13.5.sp, color = if (danger) Color(0xFFFF4D4F) else Ink)
            if (sub != null) {
                Text(sub, fontSize = 10.5.sp, color = Ink3, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Icon(Icons.Filled.ChevronRight, null, tint = Color(0xFFC9CCD0), modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun ServerDialog(
    value: String,
    onChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("服务器地址", fontSize = 16.sp) },
        text = {
            Column {
                OutlinedTextField(
                    value = value,
                    onValueChange = onChange,
                    singleLine = true,
                    placeholder = { Text("http://121.43.245.66", fontSize = 12.sp) },
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp)
                )
                Spacer(Modifier.height(6.dp))
                Text("修改后重新登录即可切换站点", fontSize = 10.5.sp, color = Ink3)
            }
        },
        confirmButton = { TextButton(onClick = onConfirm) { Text("保存") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

// ------------------------------------------------------------------ 我的列表

private val listMeta = mapOf(
    "my" to "我的投稿",
    "fav" to "我的收藏",
    "history" to "观看历史",
    "like" to "我赞过的"
)

@Composable
fun ListScreen(type: String, onBack: () -> Unit, onOpen: (Long) -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var videos by remember { mutableStateOf<List<Video>>(emptyList()) }
    var page by remember { mutableLongStateOf(1) }
    var pages by remember { mutableLongStateOf(1) }
    var loading by remember { mutableStateOf(true) }

    suspend fun load(p: Int) {
        loading = true
        try {
            val r = if (type == "my") Repo.myVideos(p) else Repo.relations(type, p)
            videos = if (p == 1) r.items() else videos + r.items()
            page = r.page
            pages = r.pages
        } catch (e: ApiException) {
            toast(ctx, e.message)
        } finally {
            loading = false
        }
    }

    LaunchedEffect(type) { load(1) }

    Column(Modifier.fillMaxSize()) {
        TopBar(listMeta[type] ?: "列表", onBack)
        if (loading && videos.isEmpty()) {
            LoadingBox()
        } else if (videos.isEmpty()) {
            EmptyBox("这里还什么都没有")
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(videos, key = { it.id }) { v -> VideoCard(v, { onOpen(v.id) }) }
                if (page < pages) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Text(
                            "加载更多",
                            fontSize = 12.5.sp,
                            color = Pink,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { scope.launch { load((page + 1).toInt()) } }
                                .padding(vertical = 12.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

// ------------------------------------------------------------------ 他人空间

@Composable
fun SpaceScreen(userId: Long, onBack: () -> Unit, onOpen: (Long) -> Unit, onLogin: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var user by remember { mutableStateOf<User?>(null) }
    var videos by remember { mutableStateOf<List<Video>>(emptyList()) }
    var page by remember { mutableLongStateOf(1) }
    var pages by remember { mutableLongStateOf(1) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(userId) {
        loading = true
        try {
            user = Repo.user(userId)
            val r = Repo.userVideos(userId, 1)
            videos = r.items()
            page = r.page
            pages = r.pages
        } catch (e: ApiException) {
            toast(ctx, e.message)
        } finally {
            loading = false
        }
    }

    Column(Modifier.fillMaxSize()) {
        TopBar(user?.displayName ?: "个人空间", onBack)
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Avatar(user, 60.dp)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(user?.displayName ?: "", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Ink)
                    Text(
                        user?.sign ?: "这个人很神秘，什么都没有写",
                        fontSize = 11.5.sp,
                        color = Ink3,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        "${fmtCount(user?.followers ?: 0)} 粉丝 · ${fmtCount(user?.videoCount ?: 0)} 投稿 · ${fmtCount(user?.totalViews ?: 0)} 播放",
                        fontSize = 10.5.sp,
                        color = Ink3
                    )
                }
                if (Session.user?.id != userId) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (user?.followed == true) Color(0xFFF1F2F3) else Pink)
                            .clickable {
                                if (!Session.loggedIn) {
                                    onLogin()
                                } else {
                                    scope.launch {
                                        runCatching { Repo.follow(userId) }.onSuccess { r ->
                                            toast(ctx, r.msg)
                                            user = user?.copy(followed = r.active, followers = r.count)
                                        }
                                    }
                                }
                            }
                            .padding(horizontal = 16.dp, vertical = 7.dp)
                    ) {
                        Text(
                            if (user?.followed == true) "已关注" else "+ 关注",
                            color = if (user?.followed == true) Ink2 else Color.White,
                            fontSize = 12.5.sp
                        )
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                "TA 的投稿",
                fontSize = 13.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = Ink,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
            )
            if (loading && videos.isEmpty()) {
                LoadingBox()
            } else if (videos.isEmpty()) {
                EmptyBox("TA 还没有投稿")
            } else {
                Column(modifier = Modifier.padding(horizontal = 12.dp)) {
                    videos.chunked(2).forEach { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            rowItems.forEach { v ->
                                Box(Modifier.weight(1f)) { VideoCard(v, { onOpen(v.id) }) }
                            }
                            if (rowItems.size == 1) Spacer(Modifier.weight(1f))
                        }
                        Spacer(Modifier.height(14.dp))
                    }
                    if (page < pages) {
                        Text(
                            "加载更多",
                            fontSize = 12.5.sp,
                            color = Pink,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    scope.launch {
                                        runCatching { Repo.userVideos(userId, (page + 1).toInt()) }
                                            .onSuccess { r ->
                                                videos = videos + r.items()
                                                page = r.page
                                                pages = r.pages
                                            }
                                    }
                                }
                                .padding(vertical = 12.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
fun TopBar(title: String, onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = 6.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(36.dp).clip(RoundedCornerShape(18.dp)).clickable { onBack() },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.ArrowBack, "返回", tint = Ink, modifier = Modifier.size(20.dp))
        }
        Text(
            title,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = Ink,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 4.dp)
        )
    }
}
