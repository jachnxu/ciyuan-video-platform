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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ciyuan.app.data.ApiException
import com.ciyuan.app.data.Repo
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
import com.ciyuan.app.ui.theme.Pink
import com.ciyuan.app.ui.theme.PinkSoft
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

private val hotWords = listOf("服务器", "弹幕", "vlog", "游戏实况", "音乐", "美食", "科技", "动漫")

@Composable
fun SearchScreen(onOpen: (Long) -> Unit, onUser: (Long) -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScopeCompat()
    var keyword by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("video") }
    var videos by remember { mutableStateOf<List<Video>>(emptyList()) }
    var users by remember { mutableStateOf<List<User>>(emptyList()) }
    var page by remember { mutableLongStateOf(1) }
    var pages by remember { mutableLongStateOf(1) }
    var loading by remember { mutableStateOf(false) }
    var searched by remember { mutableStateOf(false) }

    fun doSearch(kw: String = keyword, p: Int = 1, t: String = type) {
        if (kw.isBlank()) return
        scope.launch {
            loading = true
            try {
                if (t == "video") {
                    val r = Repo.searchVideos(kw.trim(), p)
                    videos = if (p == 1) r.items() else videos + r.items()
                    pages = r.pages
                } else {
                    val r = Repo.searchUsers(kw.trim(), p)
                    users = if (p == 1) r.items() else users + r.items()
                    pages = r.pages
                }
                page = p.toLong()
                searched = true
            } catch (e: ApiException) {
                toast(ctx, e.message)
            } finally {
                loading = false
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = keyword,
                onValueChange = { keyword = it },
                modifier = Modifier.weight(1f).height(44.dp).clip(RoundedCornerShape(22.dp)),
                placeholder = { Text("搜索视频、UP主", fontSize = 13.sp, color = Ink3) },
                leadingIcon = { Icon(Icons.Filled.Search, null, tint = Ink3, modifier = Modifier.size(18.dp)) },
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { doSearch() }),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFFF1F2F3),
                    unfocusedContainerColor = Color(0xFFF1F2F3),
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Pink)
                    .clickable { doSearch() }
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Text("搜索", color = Color.White, fontSize = 13.sp)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf("video" to "视频", "user" to "用户").forEach { (k, label) ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (type == k) Pink else Color.White)
                        .clickable {
                            type = k
                            if (searched) doSearch(keyword, 1, k)
                        }
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(label, fontSize = 12.5.sp, color = if (type == k) Color.White else Ink2)
                }
            }
        }

        if (!searched) {
            Column(modifier = Modifier.padding(16.dp)) {                Text("热门搜索", fontSize = 13.sp, color = Ink2)
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    hotWords.take(4).forEach { w ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color.White)
                                .clickable { keyword = w; doSearch(w) }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) { Text(w, fontSize = 12.sp, color = Ink2) }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    hotWords.drop(4).forEach { w ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color.White)
                                .clickable { keyword = w; doSearch(w) }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) { Text(w, fontSize = 12.sp, color = Ink2) }
                    }
                }
            }
        }

        if (searched && type == "video") {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(videos, key = { it.id }) { v ->
                    VideoCard(v, { onOpen(v.id) })
                }
                if (loading) item(span = { GridItemSpan(maxLineSpan) }) { LoadingBox() }
                if (!loading && videos.isEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) { EmptyBox("没有找到「$keyword」相关内容") }
                }
                if (page < pages) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Text(
                            "加载更多",
                            fontSize = 12.5.sp,
                            color = Pink,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { doSearch(keyword, (page + 1).toInt()) }
                                .padding(vertical = 12.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        } else if (searched) {
            LazyColumn(
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(users, key = { it.id }) { u ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.White)
                            .clickable { onUser(u.id) }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Avatar(u, 44.dp)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(u.displayName, fontSize = 14.sp, color = Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                "${fmtCount(u.followers)} 粉丝 · ${fmtCount(u.videoCount)} 投稿",
                                fontSize = 11.5.sp,
                                color = Ink3
                            )
                        }
                    }
                }
                if (!loading && users.isEmpty()) item { EmptyBox("没有找到相关用户") }
                if (loading) item { LoadingBox() }
            }
        }
    }
}

@Composable
private fun rememberCoroutineScopeCompat(): CoroutineScope {
    return androidx.compose.runtime.rememberCoroutineScope()
}
