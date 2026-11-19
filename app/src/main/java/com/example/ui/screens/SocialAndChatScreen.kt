package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.localization.LocalAddaStrings
import com.example.model.FriendRequest
import com.example.model.Friendship
import com.example.model.PrivateChat
import com.example.model.PrivateMessage
import com.example.model.UserNotification
import com.example.model.UserProfile
import com.example.ui.components.LevelBadge
import com.example.ui.components.UserAvatarWithFrame
import com.example.ui.components.VipBadge
import com.example.ui.theme.CrimsonRed
import com.example.ui.theme.EmeraldLive
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonPink
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianVoid
import com.example.ui.theme.RoyalGold
import com.example.ui.theme.RoyalPurpleAccent
import com.example.ui.theme.RoyalPurpleDeep
import com.example.ui.theme.RoyalPurpleSurface
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.UiState

@Composable
fun SocialAndChatScreen(
    me: UserProfile?,
    directoryState: UiState<List<UserProfile>>,
    requests: List<FriendRequest>,
    friendships: List<Friendship>,
    chats: List<PrivateChat>,
    activeChatId: String?,
    activeMessages: List<PrivateMessage>,
    notifications: List<UserNotification>,
    onSendFriendRequest: (UserProfile) -> Unit,
    onRespondRequest: (FriendRequest, Boolean) -> Unit,
    onOpenChat: (chatId: String, otherUid: String, otherName: String) -> Unit,
    onCloseChat: () -> Unit,
    onSendPrivateMessage: (otherUid: String, otherName: String, text: String, isVoice: Boolean, durationSec: Long) -> Unit,
    onReportUser: (targetUid: String, reason: String) -> Unit,
    onMarkNotificationRead: (String) -> Unit
) {
    val strings = LocalAddaStrings.current
    var selectedTab by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var activeOther by remember { mutableStateOf<Pair<String, String>?>(null) }
    var reportTarget by remember { mutableStateOf<UserProfile?>(null) }

    if (activeChatId != null && activeOther != null) {
        BackHandler {
            activeOther = null
            onCloseChat()
        }
        PrivateChatRoomView(
            myUid = me?.userId.orEmpty(),
            otherName = activeOther!!.second,
            messages = activeMessages,
            onBack = {
                activeOther = null
                onCloseChat()
            },
            onSendText = { txt ->
                onSendPrivateMessage(activeOther!!.first, activeOther!!.second, txt, false, 0L)
            },
            onSendVoice = { sec ->
                onSendPrivateMessage(activeOther!!.first, activeOther!!.second, "Voice", true, sec)
            }
        )
        return
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = ObsidianSurface,
            contentColor = RoyalGold
        ) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Chats (${chats.size})") })
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Discover (${requests.size})") })
            Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("${strings.notifications} (${notifications.count { !it.read }})") })
        }

        when (selectedTab) {
            0 -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 96.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (friendships.isNotEmpty()) {
                        item {
                            Text("Friends (${friendships.size})", style = MaterialTheme.typography.titleMedium, color = NeonCyan)
                        }
                        items(friendships, key = { it.id }) { fr ->
                            val idx = if (fr.userIds.firstOrNull() == me?.userId) 1 else 0
                            val otherUid = fr.userIds.getOrElse(idx) { "" }
                            val otherName = fr.userNames.getOrElse(idx) { "Friend" }
                            Card(
                                modifier = Modifier.fillMaxWidth().clickable {
                                    val pair = listOf(me?.userId.orEmpty(), otherUid).sorted()
                                    val chatId = "${pair[0]}_${pair[1]}"
                                    activeOther = otherUid to otherName
                                    onOpenChat(chatId, otherUid, otherName)
                                },
                                colors = CardDefaults.cardColors(containerColor = RoyalPurpleDeep)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    UserAvatarWithFrame("", otherName, size = 44.dp)
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(otherName, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                                        Text("Tap to message privately", style = MaterialTheme.typography.bodyMedium, color = NeonCyan)
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Text(strings.friendsAndChat, style = MaterialTheme.typography.titleMedium, color = RoyalGold)
                    }
                    if (chats.isEmpty()) {
                        item {
                            Text("No conversations yet — add friends in Discover!", color = TextSecondary)
                        }
                    } else {
                        items(chats, key = { it.id }) { chat ->
                            val idx = if (chat.participantIds.firstOrNull() == me?.userId) 1 else 0
                            val otherUid = chat.participantIds.getOrElse(idx) { "" }
                            val otherName = chat.participantNames.getOrElse(idx) { "ADDA User" }
                            Card(
                                modifier = Modifier.fillMaxWidth().clickable {
                                    activeOther = otherUid to otherName
                                    onOpenChat(chat.id, otherUid, otherName)
                                },
                                colors = CardDefaults.cardColors(containerColor = RoyalPurpleSurface)
                            ) {
                                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    UserAvatarWithFrame("", otherName, size = 46.dp)
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(otherName, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                                        Text(chat.lastMessage, style = MaterialTheme.typography.bodyMedium, color = TextSecondary, maxLines = 1)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            1 -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 96.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (requests.isNotEmpty()) {
                        item {
                            Text(strings.pendingRequests, style = MaterialTheme.typography.titleMedium, color = RoyalGold)
                        }
                        items(requests, key = { it.id }) { req ->
                            Card(colors = CardDefaults.cardColors(containerColor = RoyalPurpleSurface)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(req.fromName, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        IconButton(onClick = { onRespondRequest(req, true) }) {
                                            Icon(Icons.Filled.Check, contentDescription = strings.accept, tint = EmeraldLive)
                                        }
                                        IconButton(onClick = { onRespondRequest(req, false) }) {
                                            Icon(Icons.Filled.Close, contentDescription = strings.reject, tint = CrimsonRed)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            label = { Text(strings.searchUsers) },
                            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    if (directoryState is UiState.Success) {
                        val list = directoryState.data.filter {
                            it.userId != me?.userId &&
                                (searchQuery.isBlank() || it.username.contains(searchQuery, true) || it.addaCode.contains(searchQuery))
                        }
                        items(list, key = { it.userId }) { user ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = RoyalPurpleDeep),
                                border = BorderStroke(1.dp, RoyalPurpleAccent.copy(0.4f))
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    UserAvatarWithFrame(user.avatarUrl, user.username, user.vipTier, size = 48.dp, verified = user.verified)
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Text(user.username, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                                            LevelBadge(user.level)
                                            VipBadge(user.vipTier)
                                        }
                                        Text("ID: ${user.addaCode} • ${if (user.online) "Online" else "Offline"}", style = MaterialTheme.typography.labelSmall, color = if (user.online) EmeraldLive else TextMuted)
                                    }
                                    IconButton(onClick = { onSendFriendRequest(user) }) {
                                        Icon(Icons.Filled.PersonAdd, contentDescription = strings.addFriend, tint = NeonCyan)
                                    }
                                    IconButton(onClick = {
                                        val pair = listOf(me?.userId.orEmpty(), user.userId).sorted()
                                        activeOther = user.userId to user.username
                                        onOpenChat("${pair[0]}_${pair[1]}", user.userId, user.username)
                                    }) {
                                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Message", tint = RoyalGold)
                                    }
                                    IconButton(onClick = { reportTarget = user }) {
                                        Icon(Icons.Filled.Flag, contentDescription = strings.reportUser, tint = CrimsonRed)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            2 -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 96.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (notifications.isEmpty()) {
                        item { Text("No notifications yet.", color = TextSecondary) }
                    } else {
                        items(notifications, key = { it.id }) { n ->
                            Card(
                                modifier = Modifier.fillMaxWidth().clickable { onMarkNotificationRead(n.id) },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (n.read) RoyalPurpleDeep else RoyalPurpleSurface
                                ),
                                border = if (!n.read) BorderStroke(1.dp, NeonCyan) else null
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(n.title, style = MaterialTheme.typography.titleMedium, color = RoyalGold)
                                    Text(n.body, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    reportTarget?.let { target ->
        var reason by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { reportTarget = null },
            containerColor = ObsidianSurface,
            title = { Text("${strings.reportUser}: ${target.username}", color = CrimsonRed) },
            text = {
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Reason") },
                    maxLines = 3
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (reason.isNotBlank()) onReportUser(target.userId, reason)
                    reportTarget = null
                }) { Text(strings.confirm) }
            },
            dismissButton = { TextButton(onClick = { reportTarget = null }) { Text(strings.cancel) } }
        )
    }
}

@Composable
private fun PrivateChatRoomView(
    myUid: String,
    otherName: String,
    messages: List<PrivateMessage>,
    onBack: () -> Unit,
    onSendText: (String) -> Unit,
    onSendVoice: (Long) -> Unit
) {
    val strings = LocalAddaStrings.current
    var text by remember { mutableStateOf("") }
    var isRecording by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().background(ObsidianVoid).padding(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
            }
            Text(otherName, style = MaterialTheme.typography.titleLarge, color = RoyalGold)
        }
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth().padding(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(messages, key = { it.id }) { m ->
                val mine = m.senderId == myUid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (mine) RoyalPurpleAccent else RoyalPurpleDeep
                    ) {
                        Text(
                            text = if (m.msgType == "VOICE") "🎤 ${m.text}" else m.text,
                            color = TextPrimary,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                placeholder = { Text(strings.chatPlaceholder) },
                singleLine = true,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(24.dp)
            )
            IconButton(
                onClick = {
                    if (!isRecording) {
                        isRecording = true
                    } else {
                        isRecording = false
                        onSendVoice(5L)
                    }
                },
                modifier = Modifier.size(48.dp).clip(CircleShape).background(if (isRecording) CrimsonRed else RoyalPurpleSurface)
            ) {
                Icon(Icons.Filled.Mic, contentDescription = strings.voiceMessage, tint = TextPrimary)
            }
            IconButton(
                onClick = {
                    if (text.isNotBlank()) {
                        onSendText(text)
                        text = ""
                    }
                },
                modifier = Modifier.size(48.dp).clip(CircleShape).background(NeonCyan)
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = ObsidianVoid)
            }
        }
    }
}
