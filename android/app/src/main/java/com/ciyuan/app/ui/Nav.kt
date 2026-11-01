package com.ciyuan.app.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ciyuan.app.data.Repo
import com.ciyuan.app.data.Session
import com.ciyuan.app.ui.screens.HomeScreen
import com.ciyuan.app.ui.screens.LiveBroadcastScreen
import com.ciyuan.app.ui.screens.LiveRoomScreen
import com.ciyuan.app.ui.screens.LiveScreen
import com.ciyuan.app.ui.screens.ListScreen
import com.ciyuan.app.ui.screens.LoginScreen
import com.ciyuan.app.ui.screens.NotificationsScreen
import com.ciyuan.app.ui.screens.PlayerScreen
import com.ciyuan.app.ui.screens.ProfileScreen
import com.ciyuan.app.ui.screens.SearchScreen
import com.ciyuan.app.ui.screens.SpaceScreen
import com.ciyuan.app.ui.screens.UploadScreen
import com.ciyuan.app.ui.theme.Ink3
import com.ciyuan.app.ui.theme.Pink
import com.ciyuan.app.ui.theme.PinkSoft

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab("home", "首页", Icons.Filled.Home),
    Tab("live", "直播", Icons.Filled.LiveTv),
    Tab("search", "搜索", Icons.Filled.Search),
    Tab("notifications", "消息", Icons.Filled.Notifications),
    Tab("profile", "我的", Icons.Filled.Person)
)

@Composable
fun AppRoot() {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route ?: "home"
    val showBar = tabs.any { it.route == route }
    var unread by remember { mutableLongStateOf(0L) }

    LaunchedEffect(Session.token, route) {
        if (Session.loggedIn) {
            unread = Repo.unread()
        } else {
            unread = 0
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (showBar) {
                NavigationBar(containerColor = Color.White, tonalElevation = 0.dp) {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = route == tab.route,
                            onClick = {
                                if (route != tab.route) {
                                    nav.navigate(tab.route) {
                                        popUpTo("home") { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = {
                                BadgedBox(badge = {
                                    if (tab.route == "notifications" && unread > 0) {
                                        Badge { Text(if (unread > 99) "99+" else "$unread", fontSize = 9.sp) }
                                    }
                                }) {
                                    Icon(tab.icon, contentDescription = tab.label)
                                }
                            },
                            label = { Text(tab.label, fontSize = 10.5.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Pink,
                                selectedTextColor = Pink,
                                indicatorColor = PinkSoft,
                                unselectedIconColor = Ink3,
                                unselectedTextColor = Ink3
                            )
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = "home",
            modifier = Modifier.padding(padding)
        ) {
            composable("home") {
                HomeScreen(
                    onOpen = { nav.navigate("player/$it") },
                    onSearch = { nav.navigate("search") },
                    onUpload = { nav.navigate("upload") }
                )
            }

            composable("live") {
                LiveScreen(
                    onOpenRoom = { nav.navigate("liveRoom/$it") },
                    onBroadcast = { nav.navigate("liveBroadcast") },
                    onLogin = { nav.navigate("login") }
                )
            }
            composable(
                "liveRoom/{id}",
                arguments = listOf(navArgument("id") { type = NavType.LongType })
            ) { back ->
                LiveRoomScreen(
                    roomId = back.arguments?.getLong("id") ?: 0L,
                    onBack = { nav.popBackStack() },
                    onLogin = { nav.navigate("login") }
                )
            }
            composable("liveBroadcast") {
                LiveBroadcastScreen(
                    onBack = { nav.popBackStack() },
                    onLogin = { nav.navigate("login") },
                    onOpenRoom = { nav.navigate("liveRoom/$it") }
                )
            }
            composable("search") {
                SearchScreen(
                    onOpen = { nav.navigate("player/$it") },
                    onUser = { nav.navigate("space/$it") }
                )
            }
            composable("notifications") {
                NotificationsScreen(
                    onOpenVideo = { nav.navigate("player/$it") },
                    onOpenUser = { nav.navigate("space/$it") },
                    onLogin = { nav.navigate("login") }
                )
            }
            composable("profile") {
                ProfileScreen(
                    onLogin = { nav.navigate("login") },
                    onOpen = { nav.navigate("player/$it") },
                    onList = { nav.navigate("list/$it") },
                    onUpload = { nav.navigate("upload") },
                    onSpace = { nav.navigate("space/$it") },
                    onNotifications = { nav.navigate("notifications") }
                )
            }
            composable(
                "player/{id}",
                arguments = listOf(navArgument("id") { type = NavType.LongType })
            ) { back ->
                PlayerScreen(
                    videoId = back.arguments?.getLong("id") ?: 0L,
                    onBack = { nav.popBackStack() },
                    onOpenVideo = { id ->
                        nav.navigate("player/$id") { popUpTo("player/{id}") { inclusive = true } }
                    },
                    onOpenUser = { nav.navigate("space/$it") },
                    onLogin = { nav.navigate("login") }
                )
            }
            composable(
                "space/{id}",
                arguments = listOf(navArgument("id") { type = NavType.LongType })
            ) { back ->
                SpaceScreen(
                    userId = back.arguments?.getLong("id") ?: 0L,
                    onBack = { nav.popBackStack() },
                    onOpen = { nav.navigate("player/$it") },
                    onLogin = { nav.navigate("login") }
                )
            }
            composable(
                "list/{type}",
                arguments = listOf(navArgument("type") { type = NavType.StringType })
            ) { back ->
                ListScreen(
                    type = back.arguments?.getString("type") ?: "history",
                    onBack = { nav.popBackStack() },
                    onOpen = { nav.navigate("player/$it") }
                )
            }
            composable("login") {
                LoginScreen(onDone = { nav.popBackStack() })
            }
            composable("upload") {
                UploadScreen(
                    onBack = { nav.popBackStack() },
                    onPublished = { id ->
                        nav.popBackStack()
                        nav.navigate("player/$id")
                    }
                )
            }
        }
    }
}
