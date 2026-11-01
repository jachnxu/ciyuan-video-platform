package com.ciyuan.app.ui

import android.content.Context
import android.widget.Toast
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.ciyuan.app.data.Session
import com.ciyuan.app.data.User
import com.ciyuan.app.data.Video
import com.ciyuan.app.ui.theme.Ink
import com.ciyuan.app.ui.theme.Ink2
import com.ciyuan.app.ui.theme.Ink3
import com.ciyuan.app.ui.theme.Pink
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun fmtCount(n: Long): String = when {
    n >= 100_000_000 -> trimZero(n / 100_000_000.0) + "亿"
    n >= 10_000 -> trimZero(n / 10_000.0) + "万"
    else -> n.toString()
}

private fun trimZero(d: Double): String {
    val s = String.format(Locale.US, "%.1f", d)
    return if (s.endsWith(".0")) s.dropLast(2) else s
}

fun fmtDuration(sec: Int): String {
    val s = sec.coerceAtLeast(0)
    val h = s / 3600
    val m = (s % 3600) / 60
    val ss = s % 60
    return if (h > 0) String.format(Locale.US, "%d:%02d:%02d", h, m, ss)
    else String.format(Locale.US, "%02d:%02d", m, ss)
}

fun timeAgo(raw: String?): String {
    if (raw.isNullOrBlank()) return ""
    return try {
        val fmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.CHINA)
        val t = fmt.parse(raw.take(19))?.time ?: return raw.take(10)
        val diff = System.currentTimeMillis() - t
        val min = diff / 60000
        when {
            min < 1 -> "刚刚"
            min < 60 -> "$min 分钟前"
            min < 1440 -> "${min / 60} 小时前"
            min < 43200 -> "${min / 1440} 天前"
            else -> raw.take(10)
        }
    } catch (e: Exception) {
        raw.take(10)
    }
}

fun dateOnly(raw: String?): String = raw?.replace("T", " ")?.take(16) ?: ""

private val avatarPalette = listOf(
    Color(0xFFFB7299), Color(0xFF7C5CFF), Color(0xFF00A1D6), Color(0xFFFF9F43),
    Color(0xFF2ECC71), Color(0xFFE056FD), Color(0xFFF368E0), Color(0xFF00CEC9)
)

fun colorOf(seed: String?): Color {
    var h = 0
    (seed ?: "a").forEach { h = (h * 31 + it.code) % 9973 }
    return avatarPalette[h % avatarPalette.size]
}

fun toast(ctx: Context, msg: String?) {
    if (!msg.isNullOrBlank()) Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show()
}

@Composable
fun Avatar(user: User?, size: Dp = 36.dp) {
    val name = user?.nickname ?: user?.username ?: "?"
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(if (user?.avatar.isNullOrBlank()) colorOf(user?.id?.toString() ?: name) else Color(0xFFEEEEEE)),
        contentAlignment = Alignment.Center
    ) {
        if (!user?.avatar.isNullOrBlank()) {
            AsyncImage(
                model = Session.mediaUrl(user?.avatar),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Text(
                text = name.take(1).uppercase(),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = (size.value * 0.42f).sp
            )
        }
    }
}

@Composable
fun VideoCard(video: Video, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier.clickable { onClick() }) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFFEEF0F2))
        ) {
            val cover = Session.mediaUrl(video.cover)
            if (cover != null) {
                AsyncImage(
                    model = cover,
                    contentDescription = video.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    Icons.Filled.PlayArrow,
                    contentDescription = null,
                    tint = Color(0xFFC2C6CA),
                    modifier = Modifier.align(Alignment.Center).size(28.dp)
                )
            }
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .background(Color(0x66000000))
                    .padding(horizontal = 6.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(11.dp)
                )
                Text(
                    text = fmtCount(video.views),
                    color = Color.White,
                    fontSize = 10.sp
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "弹 ${fmtCount(video.danmakuCount)}",
                    color = Color.White,
                    fontSize = 10.sp
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = fmtDuration(video.duration),
                    color = Color.White,
                    fontSize = 10.sp
                )
            }
            if (video.status == 0) {
                Text(
                    text = "转码中",
                    color = Color.White,
                    fontSize = 10.sp,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .background(Pink, RoundedCornerShape(4.dp))
                        .padding(horizontal = 5.dp, vertical = 1.dp)
                )
            }
        }
        Text(
            text = video.title ?: "",
            fontSize = 13.sp,
            color = Ink,
            lineHeight = 18.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 6.dp, start = 1.dp, end = 1.dp)
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 5.dp, start = 1.dp)
        ) {
            Avatar(video.author, 16.dp)
            Spacer(Modifier.width(4.dp))
            Text(
                text = video.author?.displayName ?: "未知用户",
                fontSize = 10.5.sp,
                color = Ink3,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
            Text(text = " · " + fmtCount(video.views) + " 播放", fontSize = 10.5.sp, color = Ink3, maxLines = 1)
        }
    }
}

@Composable
fun EmptyBox(text: String, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth().padding(vertical = 60.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Filled.PlayArrow,
                contentDescription = null,
                tint = Color(0xFFD5D8DB),
                modifier = Modifier.size(40.dp)
            )
            Spacer(Modifier.height(8.dp))
            Text(text = text, color = Ink3, fontSize = 13.sp)
        }
    }
}

@Composable
fun LoadingBox(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth().padding(vertical = 40.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = Pink, strokeWidth = 2.5.dp, modifier = Modifier.size(26.dp))
    }
}

@Composable
fun SectionTitle(text: String, action: (@Composable () -> Unit)? = null) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = text, style = MaterialTheme.typography.titleMedium)
        action?.invoke()
    }
}

@Composable
fun Chip(text: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(15.dp))
            .background(if (selected) Pink else Color.White)
            .clickable { onClick() }
            .padding(horizontal = 13.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            color = if (selected) Color.White else Ink2,
            fontSize = 12.5.sp,
            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal
        )
    }
}
