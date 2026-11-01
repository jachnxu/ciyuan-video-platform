package com.ciyuan.app

import com.ciyuan.app.data.ApiResp
import com.ciyuan.app.data.Category
import com.ciyuan.app.data.Comment
import com.ciyuan.app.data.DanmakuItem
import com.ciyuan.app.data.PageResult
import com.ciyuan.app.data.Video
import com.ciyuan.app.ui.fmtCount
import com.ciyuan.app.ui.fmtDuration
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 用真实接口响应做解析测试：验证 Kotlin 数据模型和后端 JSON 字段完全对齐。
 * 样本由部署在服务器上的站点导出（app/src/test/resources/*.json）。
 */
class ApiParsingTest {

    private val gson = Gson()

    private fun read(name: String): String =
        javaClass.classLoader!!.getResourceAsStream(name)!!.bufferedReader().use { it.readText() }

    @Test
    fun `视频列表可以正确解析`() {
        val type = object : TypeToken<ApiResp<PageResult<Video>>>() {}.type
        val resp: ApiResp<PageResult<Video>> = gson.fromJson(read("videos.json"), type)
        assertEquals(0, resp.code)
        val page = resp.data!!
        assertTrue("列表不应为空", page.items().isNotEmpty())
        val first = page.items().first()
        assertNotNull(first.id)
        assertNotNull("标题不能为空", first.title)
        assertNotNull("封面不能为空", first.cover)
        assertNotNull("必须带 UP 主信息", first.author)
        assertNotNull("UP 主昵称不能为空", first.author?.nickname)
        assertTrue("播放量应大于 0", first.views > 0)
    }

    @Test
    fun `视频详情字段完整`() {
        val type = object : TypeToken<ApiResp<Video>>() {}.type
        val resp: ApiResp<Video> = gson.fromJson(read("video.json"), type)
        val v = resp.data!!
        assertTrue(v.id > 0)
        assertNotNull(v.videoUrl)
        assertTrue("时长应为正数", v.duration > 0)
        assertEquals(1, v.status)
        assertNotNull(v.categoryName)
        assertTrue(v.views > 0)
    }

    @Test
    fun `评论树可以解析`() {
        val type = object : TypeToken<ApiResp<PageResult<Comment>>>() {}.type
        val resp: ApiResp<PageResult<Comment>> = gson.fromJson(read("comments.json"), type)
        val page = resp.data!!
        assertTrue(page.items().isNotEmpty())
        val c = page.items().first()
        assertNotNull(c.content)
        assertNotNull("评论必须带用户信息", c.user)
        assertNotNull(c.createdAt)
    }

    @Test
    fun `弹幕可以解析`() {
        val type = object : TypeToken<ApiResp<List<DanmakuItem>>>() {}.type
        val resp: ApiResp<List<DanmakuItem>> = gson.fromJson(read("danmaku.json"), type)
        val list = resp.data!!
        assertTrue(list.isNotEmpty())
        assertNotNull(list.first().text)
        assertTrue("弹幕时间轴必须有效", list.first().time >= 0.0)
    }

    @Test
    fun `分区可以解析`() {
        val type = object : TypeToken<ApiResp<List<Category>>>() {}.type
        val resp: ApiResp<List<Category>> = gson.fromJson(read("categories.json"), type)
        assertTrue(resp.data!!.size >= 10)
        assertNotNull(resp.data!!.first().name)
    }
}

class FormatTest {

    @Test
    fun `数字缩写`() {
        assertEquals("0", fmtCount(0))
        assertEquals("999", fmtCount(999))
        assertEquals("1万", fmtCount(10_000))
        assertEquals("1.2万", fmtCount(12_345))
        assertEquals("1亿", fmtCount(100_000_000))
    }

    @Test
    fun `时长格式化`() {
        assertEquals("00:00", fmtDuration(0))
        assertEquals("00:09", fmtDuration(9))
        assertEquals("01:05", fmtDuration(65))
        assertEquals("1:01:05", fmtDuration(3665))
    }
}
