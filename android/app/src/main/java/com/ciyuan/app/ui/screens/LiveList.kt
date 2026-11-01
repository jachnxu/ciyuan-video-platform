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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.ciyuan.app.data.LiveRoom
import com.ciyuan.app.data.Repo
import com.ciyuan.app.data.Session
import com.ciyuan.app.ui.Avatar
import com.ciyuan.app.ui.EmptyBox
import com.ciyuan.app.ui.LoadingBox
import com.ciyuan.app.ui.theme.Ink
import com.ciyuan.app.ui.theme.Ink2
import com.ciyuan.app.ui.theme.Ink3
import com.ciyuan.app.ui.theme.Pink
import com.ciyuan.app.ui.theme.PinkSoft

internal fun fmtCount(n: Long): String =
    if (n >= 10000) "%.1f万".format(n / 10000.0) else n.toString()

@Composable
fun LiveScreen(
    onOpenRoom: (Long) -> Unit,
    onBroadcast: () -> Unit,
    onLogin: () -> Unit
) {
    var rooms by remember { mutableStateOf<List<LiveRoom>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var onlyLive by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(onlyLive) {
        loading = true
        error = null
        runCatching { Repo.liveRooms(1, if (onlyLive) 1 else null) }
            .onSuccess { rooms = it.items() }
            .onFailure { error = it.message }
        loading = false
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("直播", fontSize = 19.sp, fontWeight = FontWeight.Bold, color = Ink)
            Spacer(Modifier.width(12.dp))
            FilterPill("全部", !onlyLive) { onlyLive = false }
            Spacer(Modifier.width(8.dp))
            FilterPill("直播中", onlyLive) { onlyLive = true }
            Spacer(Modifier.weight(1f))
            Row(
                Modifier.clip(RoundedCornerShape(18.dp)).background(Pink)
                    .clickable { if (Session.loggedIn) onBroadcast() else onLogin() }
                    .padding(horizontal = 13.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Videocam, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("开播", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        when {
            loading && rooms.isEmpty() -> LoadingBox()
            error != null && rooms.isEmpty() -> EmptyBox(error ?: "加载失败")
            rooms.isEmpty() -> EmptyBox("还没有直播间，点右上角「开播」做第一个主播吧")
            else -> LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(rooms, key = { it.id }) { r ->
                    LiveCard(r) { onOpenRoom(r.id) }
                }
            }
        }
    }
}

@Composable
private fun FilterPill(text: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        text,
        fontSize = 12.5.sp,
        color = if (selected) Color.White else Ink2,
        modifier = Modifier.clip(RoundedCornerShape(14.dp))
            .background(if (selected) Pink else Color.White)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 5.dp)
    )
}

@Composable
private fun LiveCard(r: LiveRoom, onClick: () -> Unit) {
    Column(
        Modifier.clip(RoundedCornerShape(12.dp)).background(Color.White).clickable(onClick = onClick)
    ) {
        Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f).background(PinkSoft)) {
            val cover = Session.mediaUrl(r.cover)
            if (!cover.isNullOrBlank()) {
                AsyncImage(
                    model = cover,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            if (r.status == 1) {
                Row(
                    Modifier.align(Alignment.TopStart).padding(6.dp)
                        .clip(RoundedCornerShape(8.dp)).background(Color(0xFFFF4D6A))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.size(5.dp).clip(RoundedCornerShape(3.dp)).background(Color.White))
                    Spacer(Modifier.width(4.dp))
                    Text("直播中", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            } else {
                Text(
                    if (r.status == 2) "已结束" else "未开播",
                    color = Color.White, fontSize = 10.sp,
                    modifier = Modifier.align(Alignment.TopStart).padding(6.dp)
                        .clip(RoundedCornerShape(8.dp)).background(Color(0x99000000))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
            Text(
                "👁 ${fmtCount(r.viewers)}",
                color = Color.White, fontSize = 10.5.sp,
                modifier = Modifier.align(Alignment.BottomEnd).padding(6.dp)
                    .clip(RoundedCornerShape(8.dp)).background(Color(0x99000000))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
        Text(
            r.title ?: "直播间",
            fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold, color = Ink,
            maxLines = 2, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
        )
        Row(
            Modifier.padding(horizontal = 8.dp).padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Avatar(r.author, 18.dp)
            Spacer(Modifier.width(5.dp))
            Text(
                r.author?.displayName ?: "主播",
                fontSize = 11.5.sp, color = Ink3,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
