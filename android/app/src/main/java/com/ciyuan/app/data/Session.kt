package com.ciyuan.app.data

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.google.gson.Gson

/**
 * 全局登录态 + 本地配置（token / 服务器地址 / 缓存的用户信息）
 */
object Session {

    const val DEFAULT_BASE = "http://121.43.245.66"

    private lateinit var sp: SharedPreferences
    private val gson = Gson()

    var token: String? by mutableStateOf(null)
        private set

    var user: User? by mutableStateOf(null)
        private set

    var baseUrl: String by mutableStateOf(DEFAULT_BASE)
        private set

    val loggedIn: Boolean get() = !token.isNullOrBlank()

    fun init(ctx: Context) {
        sp = ctx.applicationContext.getSharedPreferences("ciyuan", Context.MODE_PRIVATE)
        token = sp.getString("token", null)
        baseUrl = sp.getString("base", DEFAULT_BASE) ?: DEFAULT_BASE
        user = sp.getString("user", null)?.let {
            runCatching { gson.fromJson(it, User::class.java) }.getOrNull()
        }
    }

    fun saveBase(url: String) {
        val clean = url.trim().trimEnd('/').ifBlank { DEFAULT_BASE }
        baseUrl = clean
        sp.edit().putString("base", clean).apply()
    }

    fun saveAuth(result: LoginResult) {
        token = result.token
        user = result.user
        sp.edit().putString("token", result.token).putString("user", gson.toJson(result.user)).apply()
    }

    fun updateUser(u: User?) {
        if (u == null) return
        user = u
        sp.edit().putString("user", gson.toJson(u)).apply()
    }

    fun clear() {
        token = null
        user = null
        sp.edit().remove("token").remove("user").apply()
    }

    fun mediaUrl(path: String?): String? {
        if (path.isNullOrBlank()) return null
        return if (path.startsWith("http")) path else baseUrl.trimEnd('/') + path
    }
}
