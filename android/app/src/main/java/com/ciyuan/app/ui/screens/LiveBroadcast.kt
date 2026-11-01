package com.ciyuan.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ciyuan.app.data.LiveRoom
import com.ciyuan.app.data.Repo
import com.ciyuan.app.data.Session
import com.ciyuan.app.ui.LoadingBox
import com.ciyuan.app.ui.toast
import com.ciyuan.app.ui.theme.Bg
import com.ciyuan.app.ui.theme.Ink
import com.ciyuan.app.ui.theme.Ink2
import com.ciyuan.app.ui.theme.Ink3
import com.ciyuan.app.ui.theme.Pink
import com.ciyuan.app.ui.theme.PinkSoft
import kotlinx.coroutines.launch

@Composable
fun LiveBroadcastScreen(
    onBack: () -> Unit,
    onLogin: () -> Unit,
    onOpenRoom: (Long) -> Unit
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current
    var room by remember { mutableStateOf<LiveRoom?>(null) }
    var loading by remember { mutableStateOf(true) }
    var title by remember { mutableStateOf("") }

    suspend fun load() {
        loading = true
        if (Session.loggedIn) {
            runCatching { Repo.myLiveRoom() }
                .onSuccess { it?.let { r -> room = r; title = r.title ?: "" } }
                .onFailure { toast(ctx, it.message ?: "加载失败") }
        }
        loading = false
    }

    LaunchedEffect(Unit) { load() }

    Column(Modifier.fillMaxSize().background(Bg)) {
        Row(
            Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 6.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.ArrowBack, contentDescription = "返回",
                modifier = Modifier.clickable(onClick = onBack).padding(8.dp), tint = Ink
            )
            Text("我的直播", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Ink)
        }

        if (!Session.loggedIn) {
            Column(Modifier.fillMaxWidth().padding(40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("登录后即可开播", color = Ink2, fontSize = 14.sp)
                Spacer(Modifier.height(14.dp))
                Text(
                    "去登录", color = Color.White, fontSize = 14.sp,
                    modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(Pink)
                        .clickable { onLogin() }.padding(horizontal = 24.dp, vertical = 10.dp)
                )
            }
            return@Column
        }

        if (loading) {
            LoadingBox()
            return@Column
        }

        val r = room
        if (r == null) {
            Column(Modifier.fillMaxWidth().padding(18.dp)) {
                Text("创建我的直播间", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Ink)
                Spacer(Modifier.height(14.dp))
                Text("直播标题", fontSize = 12.5.sp, color = Ink2)
                Spacer(Modifier.height(6.dp))
                BasicTextField(
                    value = title,
                    onValueChange = { title = it.take(120) },
                    singleLine = true,
                    textStyle = TextStyle(fontSize = 14.sp, color = Ink),
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                        .background(Color.White).padding(horizontal = 14.dp, vertical = 12.dp),
                    decorationBox = { inner ->
                        if (title.isEmpty()) Text("给直播间起个名字", color = Ink3, fontSize = 14.sp)
                        inner()
                    }
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    "创建直播间",
                    color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Pink)
                        .clickable {
                            val t = title.trim()
                            if (t.isEmpty()) {
                                toast(ctx, "请填写直播标题")
                                return@clickable
                            }
                            scope.launch {
                                runCatching { Repo.createLiveRoom(t) }
                                    .onSuccess { room = it; toast(ctx, "创建成功") }
                                    .onFailure { toast(ctx, it.message ?: "创建失败") }
                            }
                        }.padding(vertical = 12.dp)
                )
            }
            return@Column
        }

        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    r.title ?: "我的直播间",
                    fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Ink,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    if (r.status == 1) "● 直播中" else if (r.status == 2) "已结束" else "未开播",
                    color = if (r.status == 1) Color(0xFFFF4D6A) else Ink3,
                    fontSize = 12.sp, fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(Modifier.height(16.dp))

            Text("推流地址（OBS 服务器）", fontSize = 12.sp, color = Ink2)
            KeyBox("rtmp://121.43.245.66:1935/live") {
                clipboard.setText(AnnotatedString("rtmp://121.43.245.66:1935/live"))
                toast(ctx, "已复制推流地址")
            }
            Spacer(Modifier.height(12.dp))
            Text("串流密钥（Stream Key）", fontSize = 12.sp, color = Ink2)
            KeyBox(r.streamKey ?: "-") {
                clipboard.setText(AnnotatedString(r.streamKey ?: ""))
                toast(ctx, "已复制密钥")
            }
            Spacer(Modifier.height(18.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    if (r.status == 1) "结束直播" else "开始直播",
                    color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(Pink)
                        .clickable {
                            scope.launch {
                                runCatching {
                                    if (r.status == 1) Repo.stopLive(r.id) else Repo.startLive(r.id)
                                }.onSuccess {
                                    room = it
                                    toast(ctx, if (it.status == 1) "已开播" else "已结束")
                                }.onFailure { toast(ctx, it.message ?: "操作失败") }
                            }
                        }.padding(horizontal = 20.dp, vertical = 11.dp)
                )
                Text(
                    "查看直播间",
                    color = Pink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(PinkSoft)
                        .clickable { onOpenRoom(r.id) }
                        .padding(horizontal = 20.dp, vertical = 11.dp)
                )
            }

            Spacer(Modifier.height(20.dp))
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                    .background(Color.White).padding(14.dp)
            ) {
                Text("OBS 推流步骤", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Ink)
                Spacer(Modifier.height(8.dp))
                Text(
                    "1. OBS → 设置 → 推流 → 服务选「自定义」\n" +
                        "2. 服务器填上面的推流地址\n" +
                        "3. 串流密钥填 Stream Key\n" +
                        "4. 点「开始直播」后，在 OBS 点「开始推流」",
                    fontSize = 12.5.sp, color = Ink2, lineHeight = 21.sp
                )
            }
        }
    }
}

@Composable
private fun KeyBox(text: String, onCopy: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(top = 6.dp).clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF0F1115)).padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text, color = Color(0xFFE6E6E6), fontSize = 12.5.sp,
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            "复制", color = Pink, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.clickable(onClick = onCopy)
        )
    }
}
