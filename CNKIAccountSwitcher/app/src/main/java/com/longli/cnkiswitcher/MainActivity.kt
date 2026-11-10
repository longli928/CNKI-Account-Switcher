package com.longli.cnkiswitcher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val Ink = Color(0xFF202536)
private val Muted = Color(0xFF858DA1)
private val Accent = Color(0xFF536DFE)
private val Bg = Color(0xFFF3F5FA)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { SwitcherScreen() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwitcherScreen() {
    val root = remember { RootManager() }
    val scope = rememberCoroutineScope()
    var accounts by remember { mutableStateOf(emptyList<Account>()) }
    var rootOk by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("正在检查 Root 权限…") }
    var renameTarget by remember { mutableStateOf<Account?>(null) }
    var deleteTarget by remember { mutableStateOf<Account?>(null) }
    var switchTarget by remember { mutableStateOf<Account?>(null) }
    var renameText by remember { mutableStateOf("") }

    fun refresh() {
        scope.launch {
            busy = true
            try {
                val pair = withContext(Dispatchers.IO) {
                    val ok = root.checkRoot()
                    ok to if (ok) root.loadAccounts() else emptyList()
                }
                rootOk = pair.first
                accounts = pair.second
                message = if (rootOk) "已读取 ${accounts.size} 个账号" else "未获得 Root 授权"
            } catch (e: Exception) {
                message = e.message ?: "读取失败"
            } finally { busy = false }
        }
    }

    // Cold start: request root immediately, then refresh the list once.
    LaunchedEffect(Unit) { refresh() }

    MaterialTheme(colorScheme = lightColorScheme(
        primary = Accent, background = Bg, surface = Color.White,
        onBackground = Ink, onSurface = Ink
    )) {
        Box(Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(Color(0xFFE9EEFF), Bg, Color(0xFFF7F8FC)))
        )) {
            Box(Modifier.offset(x = 225.dp, y = 75.dp).size(180.dp).blur(55.dp)
                .background(Color(0x665B83FF), CircleShape))
            Box(Modifier.offset(x = (-55).dp, y = 350.dp).size(155.dp).blur(55.dp)
                .background(Color(0x557ADCCB), CircleShape))

            Scaffold(
                containerColor = Color.Transparent,
                topBar = {
                    TopAppBar(
                        title = {
                            Column {
                                Text("知网账号切换器", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                                Text("CNKI Account Switcher", color = Muted, fontSize = 11.sp)
                            }
                        },
                        actions = {
                            IconButton(onClick = { refresh() }, enabled = !busy) {
                                Icon(Icons.Default.Refresh, "刷新")
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                    )
                }
            ) { padding ->
                Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 18.dp)) {
                    CurrentCard(accounts.firstOrNull { it.isCurrent }, rootOk)
                    Spacer(Modifier.height(18.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("账号列表", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Ink)
                        Spacer(Modifier.width(8.dp))
                        Surface(color = Color(0xFFE5EAFE), shape = CircleShape) {
                            Text("${accounts.size}", Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
                                color = Accent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Spacer(Modifier.weight(1f))
                        Text("刷新列表", Modifier.clickable { refresh() }, color = Accent, fontSize = 13.sp)
                    }
                    Spacer(Modifier.height(10.dp))
                    if (accounts.isEmpty()) {
                        EmptyCard(rootOk)
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(bottom = 36.dp)) {
                            items(accounts, key = { it.folder }) { a ->
                                AccountCard(a, { switchTarget = a }, {
                                    renameTarget = a; renameText = a.displayName
                                }, { deleteTarget = a })
                            }
                        }
                    }
                }
            }

            if (busy) Surface(
                Modifier.align(Alignment.BottomCenter).padding(bottom = 22.dp),
                color = Color(0xEFFFFFFF), shape = CircleShape, shadowElevation = 8.dp
            ) {
                Row(Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(15.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(9.dp))
                    Text(message, fontSize = 12.sp, color = Ink)
                }
            } else Text(message, Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp),
                color = Muted, fontSize = 11.sp)
        }

        switchTarget?.let { a ->
            AlertDialog(
                onDismissRequest = { switchTarget = null },
                title = { Text("切换账号") },
                text = { Text("切换到「${a.displayName}」？知网 APP 将被停止，替换 XML 后重新启动。") },
                confirmButton = {
                    TextButton(onClick = {
                        switchTarget = null
                        scope.launch {
                            busy = true
                            try {
                                withContext(Dispatchers.IO) { root.switchAccount(a.folder) }
                                message = "已启动知网，正在确认当前账号"
                                delay(1000)
                                refresh()
                            } catch (e: Exception) { message = e.message ?: "切换失败" }
                            finally { busy = false }
                        }
                    }) { Text("确认切换") }
                },
                dismissButton = { TextButton(onClick = { switchTarget = null }) { Text("取消") } }
            )
        }

        renameTarget?.let { a ->
            AlertDialog(
                onDismissRequest = { renameTarget = null },
                title = { Text("重命名账号") },
                text = {
                    OutlinedTextField(renameText, { renameText = it.take(60) },
                        singleLine = true, label = { Text("账号备注") })
                },
                confirmButton = {
                    TextButton(onClick = {
                        scope.launch {
                            try {
                                withContext(Dispatchers.IO) { root.renameAccount(a.folder, renameText) }
                                renameTarget = null; refresh()
                            } catch (e: Exception) { message = e.message ?: "重命名失败" }
                        }
                    }) { Text("保存") }
                },
                dismissButton = { TextButton(onClick = { renameTarget = null }) { Text("取消") } }
            )
        }

        deleteTarget?.let { a ->
            AlertDialog(
                onDismissRequest = { deleteTarget = null },
                title = { Text("删除账号快照？") },
                text = { Text("永久删除「${a.displayName}」的备份文件，不会注销知网账号。") },
                confirmButton = {
                    TextButton(onClick = {
                        scope.launch {
                            try {
                                withContext(Dispatchers.IO) { root.deleteAccount(a.folder) }
                                deleteTarget = null; refresh()
                            } catch (e: Exception) { message = e.message ?: "删除失败" }
                        }
                    }) { Text("删除", color = Color(0xFFD94B4B)) }
                },
                dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("取消") } }
            )
        }
    }
}

@Composable
private fun CurrentCard(account: Account?, rootOk: Boolean) {
    Surface(Modifier.fillMaxWidth(), RoundedCornerShape(26.dp),
        color = Color(0xDFFFFFFF), shadowElevation = 10.dp) {
        Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(52.dp).clip(RoundedCornerShape(18.dp))
                .background(Brush.linearGradient(listOf(Color(0xFF7894FF), Accent))),
                contentAlignment = Alignment.Center) {
                Icon(Icons.Default.AccountCircle, null, tint = Color.White, modifier = Modifier.size(30.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text("当前账号", color = Muted, fontSize = 12.sp)
                Text(account?.displayName ?: if (rootOk) "未匹配到已保存账号" else "等待 Root 授权",
                    color = Ink, fontSize = 17.sp, fontWeight = FontWeight.Bold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(if (account != null) "当前 XML 与此账号快照一致" else "通过 XML 指纹识别当前账号",
                    color = Muted, fontSize = 11.sp)
            }
            Surface(color = if (account != null) Color(0xFFE6F7EF) else Color(0xFFF0F1F5),
                shape = CircleShape) {
                Text(if (account != null) "已匹配" else "未知",
                    Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    color = if (account != null) Color(0xFF239766) else Muted, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun AccountCard(a: Account, onSwitch: () -> Unit, onRename: () -> Unit, onDelete: () -> Unit) {
    val scale by animateFloatAsState(1f, spring(), label = "touchFeedback")
    Surface(Modifier.fillMaxWidth().scale(scale), RoundedCornerShape(22.dp),
        color = Color(0xDFFFFFFF), shadowElevation = if (a.isCurrent) 5.dp else 2.dp) {
        Row(Modifier.padding(start = 16.dp, end = 8.dp, top = 13.dp, bottom = 13.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(42.dp).clip(RoundedCornerShape(15.dp))
                .background(if (a.isCurrent) Color(0xFFE8EDFF) else Color(0xFFF0F2F8)),
                contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Person, null, tint = if (a.isCurrent) Accent else Muted)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(a.displayName, fontWeight = FontWeight.SemiBold, color = Ink, fontSize = 15.sp,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(a.folder, color = Muted, fontSize = 11.sp,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (a.isCurrent) Text("当前使用", color = Color(0xFF239766), fontSize = 10.sp)
            }
            TextButton(onClick = onSwitch) { Text(if (a.isCurrent) "重启" else "切换") }
            var expanded by remember { mutableStateOf(false) }
            Box {
                IconButton(onClick = { expanded = true }) { Icon(Icons.Default.MoreVert, "更多", tint = Muted) }
                DropdownMenu(expanded, { expanded = false }) {
                    DropdownMenuItem(text = { Text("重命名") },
                        leadingIcon = { Icon(Icons.Default.Edit, null) },
                        onClick = { expanded = false; onRename() })
                    DropdownMenuItem(text = { Text("删除快照", color = Color(0xFFD94B4B)) },
                        leadingIcon = { Icon(Icons.Default.Delete, null, tint = Color(0xFFD94B4B)) },
                        onClick = { expanded = false; onDelete() })
                }
            }
        }
    }
}

@Composable
private fun EmptyCard(rootOk: Boolean) {
    Surface(shape = RoundedCornerShape(24.dp), color = Color(0xDFFFFFFF)) {
        Column(Modifier.fillMaxWidth().padding(vertical = 34.dp, horizontal = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.FolderOpen, null, tint = Color(0xFF9AA4BC), modifier = Modifier.size(42.dp))
            Spacer(Modifier.height(12.dp))
            Text(if (rootOk) "还没有账号快照" else "等待 Root 授权",
                fontWeight = FontWeight.Bold, color = Ink)
            Spacer(Modifier.height(5.dp))
            Text(if (rootOk) "登录知网后，LSPosed 模块会自动备份账号。" else "请在 Magisk 弹窗中授予 Root 权限。",
                color = Muted, fontSize = 12.sp)
        }
    }
}
