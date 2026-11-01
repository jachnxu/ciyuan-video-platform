package com.ciyuan.app.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ciyuan.app.data.ApiException
import com.ciyuan.app.data.Category
import com.ciyuan.app.data.Repo
import com.ciyuan.app.data.Session
import com.ciyuan.app.ui.Chip
import com.ciyuan.app.ui.toast
import com.ciyuan.app.ui.theme.Ink
import com.ciyuan.app.ui.theme.Ink2
import com.ciyuan.app.ui.theme.Ink3
import com.ciyuan.app.ui.theme.Line
import com.ciyuan.app.ui.theme.Pink
import com.ciyuan.app.ui.theme.PinkSoft
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun UploadScreen(onBack: () -> Unit, onPublished: (Long) -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    var categories by remember { mutableStateOf<List<Category>>(emptyList()) }
    var categoryId by remember { mutableLongStateOf(1L) }
    var title by remember { mutableStateOf("") }
    var tags by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var pickedName by remember { mutableStateOf<String?>(null) }
    var pickedSize by remember { mutableStateOf(0L) }
    var pickedUri by remember { mutableStateOf<Uri?>(null) }
    var progress by remember { mutableIntStateOf(0) }
    var uploading by remember { mutableStateOf(false) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            pickedUri = uri
            val name = uri.lastPathSegment?.substringAfterLast('/') ?: "video"
            pickedName = name
            scope.launch {
                pickedSize = withContext(Dispatchers.IO) {
                    runCatching {
                        ctx.contentResolver.openInputStream(uri)?.use { it.available().toLong() } ?: 0L
                    }.getOrDefault(0L)
                }
            }
            if (title.isBlank()) title = name.substringBeforeLast('.').take(50)
        }
    }

    LaunchedEffect(Unit) {
        val cats = runCatching { Repo.categories() }.getOrDefault(emptyList())
        categories = cats
        if (cats.isNotEmpty()) categoryId = cats.first().id
    }

    fun doUpload() {
        val uri = pickedUri ?: return toast(ctx, "请先选择视频")
        if (title.isBlank()) return toast(ctx, "请填写标题")
        if (!Session.loggedIn) return toast(ctx, "请先登录")
        scope.launch {
            uploading = true
            progress = 0
            try {
                val file = withContext(Dispatchers.IO) {
                    val f = File(ctx.cacheDir, "upload_${System.currentTimeMillis()}.mp4")
                    ctx.contentResolver.openInputStream(uri)?.use { input ->
                        f.outputStream().use { output -> input.copyTo(output, 256 * 1024) }
                    }
                    f
                }
                val v = Repo.publish(
                    file = file,
                    cover = null,
                    title = title.trim(),
                    description = desc,
                    categoryId = categoryId,
                    tags = tags,
                    onProgress = { p -> progress = p }
                )
                withContext(Dispatchers.IO) { file.delete() }
                toast(ctx, "投稿成功，正在处理")
                uploading = false
                onPublished(v.id)
            } catch (e: ApiException) {
                toast(ctx, e.message)
                uploading = false
            } catch (e: Exception) {
                toast(ctx, "上传失败：${e.message}")
                uploading = false
            }
        }
    }

    Column(Modifier.fillMaxSize()) {
        TopBar("投稿视频", onBack)
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(14.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (pickedUri == null) Color(0xFFF7F8FA) else PinkSoft)
                    .clickable { if (!uploading) picker.launch("video/*") },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        if (pickedUri == null) Icons.Filled.CloudUpload else Icons.Filled.Movie,
                        null,
                        tint = Pink,
                        modifier = Modifier.size(34.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        pickedName ?: "点击选择手机里的视频",
                        fontSize = 13.sp,
                        color = Ink2,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                    if (pickedSize > 0) {
                        Text(
                            String.format("%.1f MB", pickedSize / 1024.0 / 1024.0),
                            fontSize = 11.sp,
                            color = Ink3
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = title,
                onValueChange = { if (it.length <= 80) title = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("标题", fontSize = 12.sp) },
                placeholder = { Text("好的标题能获得更多推荐", fontSize = 12.sp, color = Ink3) },
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                colors = uploadFieldColors()
            )

            Spacer(Modifier.height(14.dp))
            Text("分区", fontSize = 12.5.sp, color = Ink2)
            Spacer(Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                categories.chunked(5).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        row.forEach { c ->
                            Chip(c.name ?: "", categoryId == c.id) { categoryId = c.id }
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))
            OutlinedTextField(
                value = tags,
                onValueChange = { tags = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("标签", fontSize = 12.sp) },
                placeholder = { Text("用逗号分隔，例如：日常,vlog", fontSize = 12.sp, color = Ink3) },
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                colors = uploadFieldColors()
            )

            Spacer(Modifier.height(14.dp))
            OutlinedTextField(
                value = desc,
                onValueChange = { if (it.length <= 2000) desc = it },
                modifier = Modifier.fillMaxWidth().height(110.dp),
                label = { Text("简介", fontSize = 12.sp) },
                placeholder = { Text("介绍一下你的视频内容吧～", fontSize = 12.sp, color = Ink3) },
                shape = RoundedCornerShape(10.dp),
                colors = uploadFieldColors()
            )

            Spacer(Modifier.height(18.dp))
            if (uploading) {
                Column {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFFF1F2F3))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progress / 100f)
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Pink)
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            color = Pink,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("上传中 $progress%", fontSize = 11.5.sp, color = Ink3)
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .clip(RoundedCornerShape(23.dp))
                        .background(Pink)
                        .clickable { doUpload() },
                    contentAlignment = Alignment.Center
                ) {
                    Text("立即投稿", color = Color.White, fontSize = 14.5.sp, fontWeight = FontWeight.Medium)
                }
            }
            Spacer(Modifier.height(30.dp))
        }
    }
}

@Composable
private fun uploadFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Pink,
    unfocusedBorderColor = Line,
    focusedLabelColor = Pink,
    unfocusedLabelColor = Ink3,
    focusedTextColor = Ink,
    unfocusedTextColor = Ink2,
    focusedContainerColor = Color.White,
    unfocusedContainerColor = Color.White
)
