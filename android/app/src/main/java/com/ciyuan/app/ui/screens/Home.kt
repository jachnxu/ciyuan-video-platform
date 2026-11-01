package com.ciyuan.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ciyuan.app.data.ApiException
import com.ciyuan.app.data.Category
import com.ciyuan.app.data.Repo
import com.ciyuan.app.data.Video
import com.ciyuan.app.ui.Chip
import com.ciyuan.app.ui.EmptyBox
import com.ciyuan.app.ui.LoadingBox
import com.ciyuan.app.ui.VideoCard
import com.ciyuan.app.ui.toast
import com.ciyuan.app.ui.theme.Ink2
import com.ciyuan.app.ui.theme.Ink3
import com.ciyuan.app.ui.theme.Pink
import com.ciyuan.app.ui.theme.PinkSoft
import kotlinx.coroutines.launch

private val sortOptions = listOf(
    "new" to "最新",
    "hot" to "最多播放",
    "danmaku" to "最多弹幕",
    "like" to "最多点赞"
)

@Composable
fun HomeScreen(
    onOpen: (Long) -> Unit,
    onSearch: () -> Unit,
    onUpload: () -> Unit
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var categories by remember { mutableStateOf<List<Category>>(emptyList()) }
    var selectedCat by remember { mutableLongStateOf(0L) }
    var sort by remember { mutableStateOf("new") }
    var videos by remember { mutableStateOf<List<Video>>(emptyList()) }
    var page by remember { mutableLongStateOf(0L) }
    var pages by remember { mutableLongStateOf(1L) }
    var loading by remember { mutableStateOf(false) }
    var firstLoad by remember { mutableStateOf(true) }
    val gridState = rememberLazyGridState()

    suspend fun load(reset: Boolean) {
        if (loading) return
        loading = true
        try {
            val target = if (reset) 1L else page + 1
            val r = Repo.videos(
                page = target.toInt(),
                size = 12,
                categoryId = if (selectedCat == 0L) null else selectedCat,
                sort = sort
            )
            videos = if (reset) r.items() else videos + r.items()
            page = target
            pages = r.pages
        } catch (e: ApiException) {
            toast(ctx, e.message)
        } finally {
            loading = false
            firstLoad = false
        }
    }

    LaunchedEffect(Unit) {
        runCatching { categories = Repo.categories() }
    }
    LaunchedEffect(selectedCat, sort) {
        load(true)
    }
    LaunchedEffect(gridState) {
        snapshotFlow { gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 }
            .collect { last ->
                if (last >= videos.size - 2 && page < pages && !loading) load(false)
            }
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        state = gridState,
        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 6.dp, bottom = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "次元站",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Pink
                    )
                    Spacer(Modifier.width(10.dp))
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .height(34.dp)
                            .clip(RoundedCornerShape(17.dp))
                            .background(Color(0xFFF1F2F3))
                            .clickable { onSearch() }
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Search, null, tint = Ink3, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("搜索视频、UP主", fontSize = 12.5.sp, color = Ink3)
                    }
                    IconButton(onClick = onUpload, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Filled.Add, "投稿", tint = Pink, modifier = Modifier.size(24.dp))
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    Chip("全部", selectedCat == 0L) { selectedCat = 0L }
                    categories.forEach { c ->
                        Chip(c.name ?: "", selectedCat == c.id) { selectedCat = c.id }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    sortOptions.forEach { (key, label) ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (sort == key) PinkSoft else Color.Transparent)
                                .clickable { sort = key }
                                .padding(horizontal = 9.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = label,
                                fontSize = 12.5.sp,
                                color = if (sort == key) Pink else Ink2,
                                fontWeight = if (sort == key) FontWeight.SemiBold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }

        items(videos, key = { it.id }) { v ->
            VideoCard(video = v, onClick = { onOpen(v.id) })
        }

        if (loading && videos.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) { LoadingBox() }
        }
        if (!loading && videos.isEmpty() && !firstLoad) {
            item(span = { GridItemSpan(maxLineSpan) }) { EmptyBox("这个分区还没有视频，下拉刷新试试") }
        }
        if (firstLoad) {
            item(span = { GridItemSpan(maxLineSpan) }) { LoadingBox() }
        }
        if (!firstLoad && videos.isNotEmpty() && page >= pages) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    text = "— 已经到底啦 —",
                    fontSize = 11.5.sp,
                    color = Ink3,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}
