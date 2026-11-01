package com.ciyuan.app.data

import com.google.gson.Gson
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

class ApiException(val code: Int = -1, message: String) : Exception(message)

interface ApiService {
    @POST("api/auth/login")
    suspend fun login(@Body body: LoginReq): ApiResp<LoginResult>

    @POST("api/auth/register")
    suspend fun register(@Body body: RegisterReq): ApiResp<LoginResult>

    @GET("api/auth/me")
    suspend fun me(): ApiResp<User>

    @GET("api/categories")
    suspend fun categories(): ApiResp<List<Category>>

    @GET("api/videos")
    suspend fun videos(
        @Query("page") page: Int,
        @Query("size") size: Int,
        @Query("categoryId") categoryId: Long? = null,
        @Query("keyword") keyword: String? = null,
        @Query("sort") sort: String = "new"
    ): ApiResp<PageResult<Video>>

    @GET("api/videos/{id}")
    suspend fun video(@Path("id") id: Long): ApiResp<Video>

    @GET("api/videos/{id}/related")
    suspend fun related(@Path("id") id: Long, @Query("limit") limit: Int = 8): ApiResp<List<Video>>

    @POST("api/videos/{id}/like")
    suspend fun like(@Path("id") id: Long): ApiResp<ToggleResult>

    @POST("api/videos/{id}/coin")
    suspend fun coin(@Path("id") id: Long): ApiResp<ToggleResult>

    @POST("api/videos/{id}/fav")
    suspend fun fav(@Path("id") id: Long): ApiResp<ToggleResult>

    @POST("api/videos/{id}/history")
    suspend fun history(@Path("id") id: Long, @Query("progress") progress: Int): ApiResp<Void>

    @GET("api/comments")
    suspend fun comments(
        @Query("videoId") videoId: Long,
        @Query("page") page: Int,
        @Query("size") size: Int = 10,
        @Query("sort") sort: String = "new"
    ): ApiResp<PageResult<Comment>>

    @GET("api/comments/{rootId}/children")
    suspend fun commentChildren(@Path("rootId") rootId: Long): ApiResp<List<Comment>>

    @POST("api/comments")
    suspend fun addComment(@Body body: CommentReq): ApiResp<Comment>

    @POST("api/comments/{id}/like")
    suspend fun likeComment(@Path("id") id: Long): ApiResp<ToggleResult>

    @GET("api/danmaku/{videoId}")
    suspend fun danmaku(@Path("videoId") videoId: Long): ApiResp<List<DanmakuItem>>

    @POST("api/danmaku")
    suspend fun sendDanmaku(@Body body: DanmakuReq): ApiResp<DanmakuItem>

    @GET("api/users/{id}")
    suspend fun user(@Path("id") id: Long): ApiResp<User>

    @GET("api/users/{id}/videos")
    suspend fun userVideos(
        @Path("id") id: Long,
        @Query("page") page: Int,
        @Query("size") size: Int = 12
    ): ApiResp<PageResult<Video>>

    @GET("api/users/{id}/followers")
    suspend fun followers(@Path("id") id: Long, @Query("page") page: Int): ApiResp<PageResult<User>>

    @GET("api/users/{id}/following")
    suspend fun following(@Path("id") id: Long, @Query("page") page: Int): ApiResp<PageResult<User>>

    @POST("api/users/{id}/follow")
    suspend fun follow(@Path("id") id: Long): ApiResp<ToggleResult>

    @GET("api/users/me/videos")
    suspend fun myVideos(@Query("page") page: Int, @Query("size") size: Int = 12): ApiResp<PageResult<Video>>

    @GET("api/users/me/relations")
    suspend fun relations(
        @Query("type") type: String,
        @Query("page") page: Int,
        @Query("size") size: Int = 12
    ): ApiResp<PageResult<Video>>

    @GET("api/search")
    suspend fun searchVideos(
        @Query("keyword") keyword: String,
        @Query("type") type: String = "video",
        @Query("page") page: Int,
        @Query("size") size: Int = 12
    ): ApiResp<PageResult<Video>>

    @GET("api/search")
    suspend fun searchUsers(
        @Query("keyword") keyword: String,
        @Query("type") type: String = "user",
        @Query("page") page: Int,
        @Query("size") size: Int = 12
    ): ApiResp<PageResult<User>>

    @GET("api/notifications")
    suspend fun notifications(@Query("page") page: Int, @Query("size") size: Int = 20): ApiResp<PageResult<Notif>>

    @GET("api/notifications/unread")
    suspend fun unread(): ApiResp<Map<String, Long>>

    @POST("api/notifications/read")
    suspend fun readAll(): ApiResp<Void>

    @Multipart
    @POST("api/videos")
    suspend fun publish(
        @Part file: MultipartBody.Part,
        @Part cover: MultipartBody.Part?,
        @Part("title") title: RequestBody,
        @Part("description") description: RequestBody,
        @Part("categoryId") categoryId: RequestBody,
        @Part("tags") tags: RequestBody
    ): ApiResp<Video>

    // ---------------- 直播 ----------------
    @GET("api/live/rooms")
    suspend fun liveRooms(
        @Query("page") page: Int,
        @Query("size") size: Int,
        @Query("status") status: Int?
    ): ApiResp<PageResult<LiveRoom>>

    @GET("api/live/rooms/{id}")
    suspend fun liveRoom(@Path("id") id: Long): ApiResp<LiveRoom>

    @GET("api/live/my")
    suspend fun myLiveRoom(): ApiResp<LiveRoom>

    @POST("api/live/rooms")
    suspend fun createLiveRoom(@Body body: LiveRoomReq): ApiResp<LiveRoom>

    @POST("api/live/rooms/{id}/start")
    suspend fun startLive(@Path("id") id: Long): ApiResp<LiveRoom>

    @POST("api/live/rooms/{id}/stop")
    suspend fun stopLive(@Path("id") id: Long): ApiResp<LiveRoom>

    @POST("api/live/rooms/{id}/like")
    suspend fun likeLive(@Path("id") id: Long): ApiResp<ToggleResult>

    @POST("api/live/rooms/{id}/heartbeat")
    suspend fun liveHeartbeat(@Path("id") id: Long): ApiResp<OnlineResult>

    @GET("api/live/rooms/{id}/online")
    suspend fun liveOnline(@Path("id") id: Long): ApiResp<OnlineResult>

    @GET("api/live/rooms/{id}/messages")
    suspend fun liveMessages(
        @Path("id") id: Long,
        @Query("afterId") afterId: Long,
        @Query("limit") limit: Int
    ): ApiResp<List<LiveMessage>>

    @POST("api/live/rooms/{id}/messages")
    suspend fun sendLiveMessage(@Path("id") id: Long, @Body body: LiveMessageReq): ApiResp<LiveMessage>
}

class ProgressBody(
    private val file: File,
    private val mediaType: String,
    private val onProgress: (Int) -> Unit
) : RequestBody() {
    override fun contentType() = mediaType.toMediaTypeOrNull()
    override fun contentLength(): Long = file.length()
    override fun writeTo(sink: okio.BufferedSink) {
        val total = file.length().coerceAtLeast(1)
        var written = 0L
        file.inputStream().use { input ->
            val buf = ByteArray(64 * 1024)
            while (true) {
                val read = input.read(buf)
                if (read == -1) break
                sink.write(buf, 0, read)
                written += read
                onProgress(((written * 100) / total).toInt())
            }
        }
    }
}

object Net {

    private val gson = Gson()

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.MINUTES)
            .retryOnConnectionFailure(true)
            .addInterceptor { chain ->
                val old = chain.request()
                val base = Session.baseUrl.trimEnd('/').toHttpUrlOrNull()
                val url = if (base != null) {
                    old.url.newBuilder()
                        .scheme(base.scheme)
                        .host(base.host)
                        .port(base.port)
                        .build()
                } else old.url
                val builder = old.newBuilder().url(url)
                Session.token?.let { if (it.isNotBlank()) builder.header("Authorization", "Bearer $it") }
                chain.proceed(builder.build())
            }
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            })
            .build()
    }

    val api: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl("http://127.0.0.1/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(ApiService::class.java)
    }
}

object Repo {

    private suspend fun <T> data(block: suspend () -> ApiResp<T>): T {
        val r = try {
            block()
        } catch (e: IOException) {
            throw ApiException(-2, "网络连接失败，请检查服务器地址（${Session.baseUrl}）")
        } catch (e: ApiException) {
            throw e
        } catch (e: Exception) {
            throw ApiException(-3, e.message ?: "请求失败")
        }
        if (r.code == 401) {
            Session.clear()
            throw ApiException(401, "登录已过期，请重新登录")
        }
        if (r.code != 0) throw ApiException(r.code, r.msg ?: "请求失败")
        return r.data ?: throw ApiException(-4, "服务器返回数据为空")
    }

    private suspend fun <T> ignore(block: suspend () -> ApiResp<T>) {
        val r = try {
            block()
        } catch (e: IOException) {
            throw ApiException(-2, "网络连接失败")
        }
        if (r.code == 401) {
            Session.clear()
            throw ApiException(401, "登录已过期，请重新登录")
        }
        if (r.code != 0) throw ApiException(r.code, r.msg ?: "操作失败")
    }

    suspend fun login(username: String, password: String): LoginResult =
        data { Net.api.login(LoginReq(username.trim(), password)) }

    suspend fun register(username: String, password: String, nickname: String?): LoginResult =
        data { Net.api.register(RegisterReq(username.trim(), password, nickname?.trim()?.ifBlank { null })) }

    suspend fun me(): User = data { Net.api.me() }

    suspend fun categories(): List<Category> = data { Net.api.categories() }

    suspend fun videos(
        page: Int,
        size: Int = 12,
        categoryId: Long? = null,
        keyword: String? = null,
        sort: String = "new"
    ): PageResult<Video> = data { Net.api.videos(page, size, categoryId, keyword, sort) }

    suspend fun video(id: Long): Video = data { Net.api.video(id) }

    suspend fun related(id: Long): List<Video> = data { Net.api.related(id) }

    suspend fun like(id: Long): ToggleResult = data { Net.api.like(id) }
    suspend fun coin(id: Long): ToggleResult = data { Net.api.coin(id) }
    suspend fun fav(id: Long): ToggleResult = data { Net.api.fav(id) }

    suspend fun reportHistory(id: Long, progress: Int) {
        ignore { Net.api.history(id, progress) }
    }

    suspend fun comments(videoId: Long, page: Int, sort: String = "new"): PageResult<Comment> =
        data { Net.api.comments(videoId, page, 10, sort) }

    suspend fun commentChildren(rootId: Long): List<Comment> = data { Net.api.commentChildren(rootId) }

    suspend fun addComment(videoId: Long, content: String, parentId: Long? = null, replyUserId: Long? = null): Comment =
        data { Net.api.addComment(CommentReq(videoId, content, parentId, replyUserId)) }

    suspend fun likeComment(id: Long): ToggleResult = data { Net.api.likeComment(id) }

    suspend fun danmaku(videoId: Long): List<DanmakuItem> = data { Net.api.danmaku(videoId) }

    suspend fun sendDanmaku(videoId: Long, text: String, time: Double, color: String): DanmakuItem =
        data { Net.api.sendDanmaku(DanmakuReq(videoId, text, time, color)) }

    suspend fun user(id: Long): User = data { Net.api.user(id) }

    suspend fun userVideos(id: Long, page: Int): PageResult<Video> = data { Net.api.userVideos(id, page) }

    suspend fun followers(id: Long, page: Int): PageResult<User> = data { Net.api.followers(id, page) }

    suspend fun following(id: Long, page: Int): PageResult<User> = data { Net.api.following(id, page) }

    suspend fun follow(id: Long): ToggleResult = data { Net.api.follow(id) }

    suspend fun myVideos(page: Int): PageResult<Video> = data { Net.api.myVideos(page) }

    suspend fun relations(type: String, page: Int): PageResult<Video> = data { Net.api.relations(type, page) }

    suspend fun searchVideos(keyword: String, page: Int): PageResult<Video> =
        data { Net.api.searchVideos(keyword, "video", page) }

    suspend fun searchUsers(keyword: String, page: Int): PageResult<User> =
        data { Net.api.searchUsers(keyword, "user", page) }

    suspend fun notifications(page: Int): PageResult<Notif> = data { Net.api.notifications(page) }

    suspend fun unread(): Long = runCatching { data { Net.api.unread() }["count"] ?: 0L }.getOrDefault(0L)

    suspend fun readAll() {
        ignore { Net.api.readAll() }
    }

    suspend fun publish(
        file: File,
        cover: File?,
        title: String,
        description: String,
        categoryId: Long,
        tags: String,
        onProgress: (Int) -> Unit
    ): Video {
        val text = "text/plain".toMediaTypeOrNull()
        val videoPart = MultipartBody.Part.createFormData(
            "file", file.name, ProgressBody(file, "video/*", onProgress)
        )
        val coverPart = cover?.let {
            MultipartBody.Part.createFormData("cover", it.name, it.asRequestBody("image/*".toMediaTypeOrNull()))
        }
        return data {
            Net.api.publish(
                videoPart,
                coverPart,
                title.toRequestBody(text),
                description.toRequestBody(text),
                categoryId.toString().toRequestBody(text),
                tags.toRequestBody(text)
            )
        }
    }

    // ---------------- 直播 ----------------
    suspend fun liveRooms(page: Int, status: Int? = null): PageResult<LiveRoom> =
        data { Net.api.liveRooms(page, 24, status) }

    suspend fun liveRoom(id: Long): LiveRoom = data { Net.api.liveRoom(id) }

    suspend fun myLiveRoom(): LiveRoom? {
        val r = try {
            Net.api.myLiveRoom()
        } catch (e: IOException) {
            throw ApiException(-2, "网络连接失败")
        }
        if (r.code == 401) {
            Session.clear()
            throw ApiException(401, "登录已过期，请重新登录")
        }
        if (r.code != 0) throw ApiException(r.code, r.msg ?: "请求失败")
        return r.data
    }

    suspend fun createLiveRoom(title: String): LiveRoom =
        data { Net.api.createLiveRoom(LiveRoomReq(title, null, 0)) }

    suspend fun startLive(id: Long): LiveRoom = data { Net.api.startLive(id) }
    suspend fun stopLive(id: Long): LiveRoom = data { Net.api.stopLive(id) }
    suspend fun likeLive(id: Long): ToggleResult = data { Net.api.likeLive(id) }
    suspend fun liveHeartbeat(id: Long): OnlineResult = data { Net.api.liveHeartbeat(id) }
    suspend fun liveOnline(id: Long): OnlineResult = data { Net.api.liveOnline(id) }
    suspend fun liveMessages(id: Long, afterId: Long): List<LiveMessage> =
        data { Net.api.liveMessages(id, afterId, 50) }
    suspend fun sendLiveMessage(id: Long, content: String): LiveMessage =
        data { Net.api.sendLiveMessage(id, LiveMessageReq(content, 0, "#ffffff")) }
}