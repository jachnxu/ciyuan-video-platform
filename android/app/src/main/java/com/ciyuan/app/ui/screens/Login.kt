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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ciyuan.app.data.ApiException
import com.ciyuan.app.data.Repo
import com.ciyuan.app.data.Session
import com.ciyuan.app.ui.toast
import com.ciyuan.app.ui.theme.Ink
import com.ciyuan.app.ui.theme.Ink2
import com.ciyuan.app.ui.theme.Ink3
import com.ciyuan.app.ui.theme.Line
import com.ciyuan.app.ui.theme.Pink
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(onDone: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var isRegister by remember { mutableStateOf(false) }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var nickname by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var showServer by remember { mutableStateOf(false) }
    var serverInput by remember { mutableStateOf(Session.baseUrl) }

    fun submit() {
        if (username.isBlank() || password.isBlank()) {
            toast(ctx, "请填写账号和密码")
            return
        }
        if (isRegister && password.length < 6) {
            toast(ctx, "密码至少 6 位")
            return
        }
        scope.launch {
            loading = true
            try {
                val r = if (isRegister) {
                    Repo.register(username, password, nickname)
                } else {
                    Repo.login(username, password)
                }
                Session.saveAuth(r)
                toast(ctx, if (isRegister) "注册成功，欢迎加入" else "登录成功")
                onDone()
            } catch (e: ApiException) {
                toast(ctx, e.message)
            } finally {
                loading = false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(190.dp)
                .background(Brush.linearGradient(listOf(Color(0xFFFB7299), Color(0xFFFFB3C8)))),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("次元站", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                Spacer(Modifier.height(6.dp))
                Text(
                    if (isRegister) "加入我们，一起发弹幕" else "在这里，每个人都是创作者",
                    fontSize = 12.5.sp,
                    color = Color(0xE6FFFFFF)
                )
            }
        }

        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Column(
                    modifier = Modifier.clickable { isRegister = false }.padding(vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "登录",
                        fontSize = 16.sp,
                        fontWeight = if (!isRegister) FontWeight.Bold else FontWeight.Normal,
                        color = if (!isRegister) Ink else Ink3
                    )
                    Spacer(Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .width(20.dp)
                            .height(2.5.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(if (!isRegister) Pink else Color.Transparent)
                    )
                }
                Column(
                    modifier = Modifier.clickable { isRegister = true }.padding(vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "注册",
                        fontSize = 16.sp,
                        fontWeight = if (isRegister) FontWeight.Bold else FontWeight.Normal,
                        color = if (isRegister) Ink else Ink3
                    )
                    Spacer(Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .width(20.dp)
                            .height(2.5.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(if (isRegister) Pink else Color.Transparent)
                    )
                }
            }

            Spacer(Modifier.height(18.dp))

            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("用户名", fontSize = 12.sp) },
                placeholder = { Text("3-20 位字母、数字或下划线", fontSize = 12.sp, color = Ink3) },
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                colors = fieldColors()
            )
            Spacer(Modifier.height(12.dp))
            if (isRegister) {
                OutlinedTextField(
                    value = nickname,
                    onValueChange = { nickname = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("昵称（可选）", fontSize = 12.sp) },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = fieldColors()
                )
                Spacer(Modifier.height(12.dp))
            }
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("密码", fontSize = 12.sp) },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                shape = RoundedCornerShape(10.dp),
                colors = fieldColors()
            )

            Spacer(Modifier.height(22.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .clip(RoundedCornerShape(23.dp))
                    .background(if (loading) Color(0xFFF0A9C0) else Pink)
                    .clickable { if (!loading) submit() },
                contentAlignment = Alignment.Center
            ) {
                if (loading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    Text(
                        if (isRegister) "注 册" else "登 录",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    "服务器：${Session.baseUrl}",
                    fontSize = 11.sp,
                    color = Ink3,
                    modifier = Modifier.clickable { showServer = !showServer }
                )
            }
            if (showServer) {
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = serverInput,
                    onValueChange = { serverInput = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("服务器地址", fontSize = 12.sp) },
                    shape = RoundedCornerShape(10.dp),
                    colors = fieldColors()
                )
                Spacer(Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(Pink)
                        .clickable {
                            Session.saveBase(serverInput)
                            toast(ctx, "已保存：${Session.baseUrl}")
                        }
                        .padding(horizontal = 18.dp, vertical = 8.dp)
                ) { Text("保存地址", color = Color.White, fontSize = 12.sp) }
            }

            Spacer(Modifier.height(20.dp))
            Text(
                "演示账号：kuma / 123456　　管理员：admin / admin123",
                fontSize = 10.5.sp,
                color = Ink3
            )
        }
    }
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Pink,
    unfocusedBorderColor = Line,
    focusedLabelColor = Pink,
    unfocusedLabelColor = Ink3,
    focusedTextColor = Ink,
    unfocusedTextColor = Ink2,
    focusedContainerColor = Color.White,
    unfocusedContainerColor = Color.White
)
