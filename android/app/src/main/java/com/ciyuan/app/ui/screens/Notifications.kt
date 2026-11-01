package com.ciyuan.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ciyuan.app.data.ApiException
import com.ciyuan.app.data.Notif
import com.ciyuan.app.data.Repo
import com.ciyuan.app.data.Session
import com.ciyuan.app.ui.Avatar
import com.ciyuan.app.ui.EmptyBox
import com.ciyuan.app.ui.LoadingBox
import com.ciyuan.app.ui.timeAgo
import com.ciyuan.app.ui.toast
import com.ciyuan.app.ui.theme.Blue
import com.ciyuan.app.ui.theme.Ink
import com.ciyuan.app.ui.theme.Ink2
import com.ciyuan.app.ui.theme.Ink3
import com.ciyuan.app.ui.theme.Pink
import com.ciyuan.app.ui.theme.PinkSoft
import kotlinx.coroutines.launch

@Composable
fun NotificationsScreen(
    onOpenVideo: (Long) -> Unit,
    onOpenUser: (Long) -> Unit,
    onLogin: () -> Unit
) {
    val ctx = LocalContext.current
    var list by remember { mutableStateOf<List<Notif>>(emptyList()) }
    var page by remember { mutableLongStateOf(1) }
    var pages by remember { mutableLongStateOf(1) }
    var loading by remember { mutableStateOf(true) }
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    suspend fun load(p: Int) {
        loading = true
        try {
            val r = Repo.notifications(p)
            list = if (p == 1) r.items() else list + r.items()
            page = r.page
            pages = r.pages
        } catch (e: ApiException) {
            toast(ctx, e.message)
        } finally {
            loading = false
        }
    }

    LaunchedEffect(Session.token) {
        if (Session.loggedIn) {
            load(1)
            runCatching { Repo.readAll() }
        } else {
            loading = false
        }
    }

    if (!Session.loggedIn) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Filled.Notifications, null, tint = Color(0xFFD5D8DB), modifier = Modifier.size(46.dp))
            Spacer(Modifier.height(10.dp))
            Text("登录后查看消息", fontSize = 14.sp, color = Ink3)
            Spacer(Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Pink)
                    .clickable { onLogin() }
                    .padding(horizontal = 30.dp, vertical = 9.dp)
            ) { Text("去登录", color = Color.White, fontSize = 13.sp) }
        }
        return
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("消息中心", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Ink)
            Spacer(Modifier.weight(1f))
            Text("${list.size} 条", fontSize = 11.5.sp, color = Ink3)
        }

        if (loading && list.isEmpty()) {
            LoadingBox()
        } else if (list.isEmpty()) {
            EmptyBox("暂时没有新消息")
        } else {
            LazyColumn(Modifier.fillMaxSize()) {
                items(list, key = { it.id }) { n ->
                    NotifRow(n) {
                        if (n.videoId != null && n.videoId > 0) onOpenVideo(n.videoId)
                        else n.fromUser?.let { onOpenUser(it.id) }
                    }
                }
                if (page < pages) {
                    item {
                        Text(
                            "加载更多",
                            fontSize = 12.5.sp,
                            color = Pink,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { scope.launch { load((page + 1).toInt()) } }
                                .padding(vertical = 14.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
                item { Spacer(Modifier.height(20.dp)) }
            }
        }
    }
}

@Composable
private fun NotifRow(n: Notif, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(contentAlignment = Alignment.BottomEnd) {
            Avatar(n.fromUser, 40.dp)
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(
                        when (n.type) {
                            "like" -> Pink
                            "comment" -> Blue
                            "follow" -> Color(0xFF52C41A)
                            else -> Color(0xFFB0B5BA)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    when (n.type) {
                        "like" -> Icons.Filled.Favorite
                        "follow" -> Icons.Filled.PersonAdd
                        else -> Icons.Filled.Notifications
                    },
                    null,
                    tint = Color.White,
                    modifier = Modifier.size(9.dp)
                )
            }
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    n.fromUser?.displayName ?: "系统通知",
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = Ink
                )
                if (n.isRead == 0) {
                    Spacer(Modifier.width(6.dp))
                    Box(Modifier.size(6.dp).clip(CircleShape).background(Pink))
                }
            }
            Text(
                n.content ?: "",
                fontSize = 12.sp,
                color = Ink2,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp)
            )
            if (!n.videoTitle.isNullOrBlank()) {
                Text(
                    "《${n.videoTitle}》",
                    fontSize = 11.sp,
                    color = Blue,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            Text(timeAgo(n.createdAt), fontSize = 10.sp, color = Ink3, modifier = Modifier.padding(top = 2.dp))
        }
    }
}
