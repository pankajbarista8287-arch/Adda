package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SportsKabaddi
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.R
import com.example.localization.AppLang
import com.example.localization.LocalAddaStrings
import com.example.model.GiftItem
import com.example.model.PkBattle
import com.example.model.RoomMember
import com.example.model.RoomMessage
import com.example.model.RoomSeat
import com.example.model.UserProfile
import com.example.model.VoiceRoom
import com.example.ui.components.GiftPickerSheet
import com.example.ui.components.LevelBadge
import com.example.ui.components.UserAvatarWithFrame
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

@Composable
fun VoiceRoomScreen(
    room: VoiceRoom,
    me: UserProfile?,
    coinsBalance: Long,
    seats: List<RoomSeat>,
    messages: List<RoomMessage>,
    members: List<RoomMember>,
    gifts: List<GiftItem>,
    activePk: PkBattle?,
    lang: AppLang,
    isMicCapturing: Boolean,
    speakingLevel: Int,
    speakerOn: Boolean,
    onExit: () -> Unit,
    onTakeSeat: (RoomSeat) -> Unit,
    onLeaveSeat: (RoomSeat) -> Unit,
    onModerateSeat: (seat: RoomSeat, toggleMute: Boolean, toggleLock: Boolean, kick: Boolean) -> Unit,
    onToggleMyMic: (mySeat: RoomSeat?, hasPerm: Boolean) -> Unit,
    onToggleSpeaker: () -> Unit,
    onSendChat: (String) -> Unit,
    onSendGift: (receiverId: String, receiverName: String, gift: GiftItem, qty: Int, pkId: String?, pkTeam: String?) -> Unit,
    onPreviewGift: (gift: GiftItem, receiverName: String) -> Unit = { _, _ -> },
    onStartPk: (redTeam: String, blueTeam: String) -> Unit,
    onFinishPk: (PkBattle) -> Unit,
    onUpdateRoomSettings: (announcement: String, themeId: String) -> Unit
) {
    BackHandler { onExit() }

    val strings = LocalAddaStrings.current
    val context = LocalContext.current
    val isOwnerOrMod = me != null && (room.ownerId == me.userId || me.userId in room.moderatorUids)
    val mySeat = seats.firstOrNull { it.occupantId == me?.userId && me.userId.isNotBlank() }

    var chatInput by remember { mutableStateOf("") }
    var showGiftSheet by remember { mutableStateOf(false) }
    var giftTarget by remember { mutableStateOf(room.ownerId to room.ownerName) }
    var selectedPkTeam by remember { mutableStateOf<String?>("RED") }
    var selectedSeatForMenu by remember { mutableStateOf<RoomSeat?>(null) }
    var showMembersDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showPkDialog by remember { mutableStateOf(false) }

    var hasAudioPerm by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }
    val micPermLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasAudioPerm = granted
        if (granted) onToggleMyMic(mySeat, true)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.img_room_royal_stage),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    when (room.themeId) {
                        "NEON_CLUB" -> Brush.verticalGradient(listOf(ObsidianVoid.copy(0.7f), RoyalPurpleDeep.copy(0.85f), ObsidianVoid.copy(0.92f)))
                        "DIWALI_GOLD" -> Brush.verticalGradient(listOf(ObsidianVoid.copy(0.65f), Color(0xFF2B1805).copy(0.75f), ObsidianVoid.copy(0.92f)))
                        else -> Brush.verticalGradient(listOf(ObsidianVoid.copy(0.62f), RoyalPurpleDeep.copy(0.78f), ObsidianVoid.copy(0.94f)))
                    }
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(12.dp)
        ) {
            // Top Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    IconButton(onClick = onExit, modifier = Modifier.testTag("exit_room_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = strings.close, tint = TextPrimary)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(room.title, style = MaterialTheme.typography.titleMedium, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            LevelBadge(room.level)
                        }
                        Text(
                            text = room.announcement.ifBlank { room.topic },
                            style = MaterialTheme.typography.bodyMedium,
                            color = NeonCyan,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Row {
                    IconButton(onClick = { showMembersDialog = true }) {
                        Icon(Icons.Filled.Group, contentDescription = strings.onlineMembers, tint = NeonCyan)
                    }
                    IconButton(onClick = {
                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, "Join my live ADDA Voice Room: ${room.title} (ID: ${room.id})")
                        }
                        context.startActivity(Intent.createChooser(sendIntent, strings.shareRoom))
                    }) {
                        Icon(Icons.Filled.Share, contentDescription = strings.shareRoom, tint = TextPrimary)
                    }
                    if (isOwnerOrMod) {
                        IconButton(onClick = { showSettingsDialog = true }) {
                            Icon(Icons.Filled.Settings, contentDescription = "Room Settings", tint = RoyalGold)
                        }
                    }
                }
            }

            // PK Battle Scoreboard
            if (activePk != null && activePk.status == "ACTIVE") {
                val total = (activePk.redScore + activePk.blueScore).coerceAtLeast(1L)
                val redRatio = (activePk.redScore.toFloat() / total.toFloat()).coerceIn(0.1f, 0.9f)
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    colors = CardDefaults.cardColors(containerColor = ObsidianSurface.copy(alpha = 0.9f)),
                    border = BorderStroke(1.dp, RoyalGold.copy(alpha = 0.7f))
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("🔥 ${activePk.redTeamName}: ${activePk.redScore}", style = MaterialTheme.typography.labelLarge, color = CrimsonRed)
                            Text("PK LIVE", style = MaterialTheme.typography.labelSmall, color = RoyalGold)
                            Text("${activePk.blueScore} :${activePk.blueTeamName} ⚡", style = MaterialTheme.typography.labelLarge, color = NeonCyan)
                        }
                        LinearProgressIndicator(
                            progress = { redRatio },
                            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                            color = CrimsonRed,
                            trackColor = NeonCyan
                        )
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                OutlinedButton(
                                    onClick = { selectedPkTeam = "RED"; showGiftSheet = true },
                                    border = BorderStroke(1.dp, CrimsonRed),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                                ) {
                                    Text("Gift Red", style = MaterialTheme.typography.labelSmall, color = CrimsonRed)
                                }
                                OutlinedButton(
                                    onClick = { selectedPkTeam = "BLUE"; showGiftSheet = true },
                                    border = BorderStroke(1.dp, NeonCyan),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                                ) {
                                    Text("Gift Blue", style = MaterialTheme.typography.labelSmall, color = NeonCyan)
                                }
                            }
                            if (isOwnerOrMod) {
                                TextButton(onClick = { onFinishPk(activePk) }) {
                                    Text("End PK", color = RoyalGold)
                                }
                            }
                        }
                    }
                }
            } else if (isOwnerOrMod) {
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.End) {
                    OutlinedButton(
                        onClick = { showPkDialog = true },
                        border = BorderStroke(1.dp, CrimsonRed)
                    ) {
                        Icon(Icons.Filled.SportsKabaddi, contentDescription = null, tint = CrimsonRed, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(strings.startPk, color = CrimsonRed)
                    }
                }
            }

            // 8..16 Microphone Seats Grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                modifier = Modifier.fillMaxWidth().height(if (seats.size > 12) 260.dp else 205.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(seats, key = { it.seatIndex }) { seat ->
                    val isMeOnSeat = seat.occupantId == me?.userId && seat.occupantId.isNotBlank()
                    val effectiveLevel = if (isMeOnSeat && isMicCapturing) speakingLevel else seat.speakingLevel.toInt()
                    SeatTile(
                        seat = seat,
                        effectiveSpeaking = effectiveLevel,
                        onClick = {
                            if (seat.occupantId.isBlank()) {
                                onTakeSeat(seat)
                            } else {
                                selectedSeatForMenu = seat
                            }
                        }
                    )
                }
            }

            Spacer(Modifier.height(6.dp))

            // Live Room Chat Feed
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(ObsidianVoid.copy(alpha = 0.45f))
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(messages, key = { it.id }) { msg ->
                    val isGift = msg.msgType == "GIFT"
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isGift) RoyalGold.copy(alpha = 0.16f) else RoyalPurpleDeep.copy(alpha = 0.55f)
                    ) {
                        Text(
                            text = if (isGift) "🎁 ${msg.text}" else "${msg.senderName}: ${msg.text}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isGift) RoyalGold else TextPrimary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Bottom Action Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                OutlinedTextField(
                    value = chatInput,
                    onValueChange = { chatInput = it },
                    placeholder = { Text(strings.chatPlaceholder, maxLines = 1) },
                    singleLine = true,
                    modifier = Modifier.weight(1f).testTag("room_chat_input"),
                    shape = RoundedCornerShape(24.dp)
                )
                IconButton(
                    onClick = {
                        if (chatInput.isNotBlank()) {
                            onSendChat(chatInput)
                            chatInput = ""
                        }
                    },
                    modifier = Modifier.testTag("room_send_chat_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = NeonCyan)
                }
                IconButton(
                    onClick = {
                        if (!hasAudioPerm) {
                            micPermLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        } else {
                            onToggleMyMic(mySeat, true)
                        }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(if (isMicCapturing) EmeraldLive else RoyalPurpleSurface)
                        .testTag("room_mic_toggle")
                ) {
                    Icon(
                        imageVector = if (isMicCapturing) Icons.Filled.Mic else Icons.Filled.MicOff,
                        contentDescription = if (isMicCapturing) strings.micOn else strings.micOff,
                        tint = TextPrimary
                    )
                }
                IconButton(
                    onClick = onToggleSpeaker,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(if (speakerOn) RoyalPurpleAccent else RoyalPurpleSurface)
                ) {
                    Icon(Icons.Filled.VolumeUp, contentDescription = "Speaker", tint = TextPrimary)
                }
                IconButton(
                    onClick = {
                        giftTarget = room.ownerId to room.ownerName
                        selectedPkTeam = null
                        showGiftSheet = true
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(NeonPink)
                        .testTag("room_gift_button")
                ) {
                    Icon(Icons.Filled.CardGiftcard, contentDescription = strings.sendGift, tint = TextPrimary)
                }
            }
        }
    }

    // Seat Options Dialog
    selectedSeatForMenu?.let { seat ->
        val isMine = seat.occupantId == me?.userId
        AlertDialog(
            onDismissRequest = { selectedSeatForMenu = null },
            containerColor = ObsidianSurface,
            title = { Text("Seat #${seat.seatIndex + 1} • ${seat.occupantName}", color = RoyalGold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            giftTarget = seat.occupantId to seat.occupantName
                            selectedSeatForMenu = null
                            showGiftSheet = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonPink)
                    ) {
                        Text(strings.sendGift)
                    }
                    if (isMine) {
                        OutlinedButton(
                            onClick = {
                                onLeaveSeat(seat)
                                selectedSeatForMenu = null
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(strings.leaveSeat)
                        }
                    }
                    if (isOwnerOrMod) {
                        OutlinedButton(
                            onClick = { onModerateSeat(seat, true, false, false); selectedSeatForMenu = null },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text(if (seat.muted) "Unmute Seat" else "Mute Seat") }
                        OutlinedButton(
                            onClick = { onModerateSeat(seat, false, true, false); selectedSeatForMenu = null },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text(if (seat.locked) "Unlock Seat" else "Lock Seat") }
                        if (!isMine) {
                            OutlinedButton(
                                onClick = { onModerateSeat(seat, false, false, true); selectedSeatForMenu = null },
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("Remove from Mic", color = CrimsonRed) }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedSeatForMenu = null }) { Text(strings.close) }
            }
        )
    }

    if (showMembersDialog) {
        AlertDialog(
            onDismissRequest = { showMembersDialog = false },
            containerColor = ObsidianSurface,
            title = { Text("${strings.onlineMembers} (${members.size})", color = RoyalGold) },
            text = {
                LazyColumn(modifier = Modifier.height(240.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(members, key = { it.userId }) { m ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("${m.username} (${m.role})", color = TextPrimary)
                            Text(m.vipTier, color = RoyalGold, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showMembersDialog = false }) { Text(strings.close) }
            }
        )
    }

    if (showSettingsDialog) {
        var newAnn by remember { mutableStateOf(room.announcement) }
        var newTheme by remember { mutableStateOf(room.themeId) }
        AlertDialog(
            onDismissRequest = { showSettingsDialog = false },
            containerColor = ObsidianSurface,
            title = { Text("Room Settings", color = RoyalGold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = newAnn, onValueChange = { newAnn = it }, label = { Text(strings.announcement) })
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("ROYAL_STAGE", "NEON_CLUB", "DIWALI_GOLD").forEach { th ->
                            OutlinedButton(onClick = { newTheme = th }) { Text(th.substringBefore("_")) }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    onUpdateRoomSettings(newAnn, newTheme)
                    showSettingsDialog = false
                }) { Text(strings.confirm) }
            },
            dismissButton = {
                TextButton(onClick = { showSettingsDialog = false }) { Text(strings.cancel) }
            }
        )
    }

    if (showPkDialog) {
        var redName by remember { mutableStateOf("Royal Lions") }
        var blueName by remember { mutableStateOf("Cyber Eagles") }
        AlertDialog(
            onDismissRequest = { showPkDialog = false },
            containerColor = ObsidianSurface,
            title = { Text(strings.startPk, color = RoyalGold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = redName, onValueChange = { redName = it }, label = { Text(strings.redTeam) })
                    OutlinedTextField(value = blueName, onValueChange = { blueName = it }, label = { Text(strings.blueTeam) })
                }
            },
            confirmButton = {
                Button(onClick = {
                    onStartPk(redName, blueName)
                    showPkDialog = false
                }) { Text(strings.startPk) }
            },
            dismissButton = {
                TextButton(onClick = { showPkDialog = false }) { Text(strings.cancel) }
            }
        )
    }

    if (showGiftSheet) {
        GiftPickerSheet(
            gifts = gifts,
            coinsBalance = coinsBalance,
            lang = lang,
            receiverLabel = giftTarget.second.ifBlank { room.ownerName },
            onDismiss = { showGiftSheet = false },
            onSendGift = { gift, qty ->
                onSendGift(
                    giftTarget.first,
                    giftTarget.second,
                    gift,
                    qty,
                    activePk?.id,
                    selectedPkTeam
                )
            },
            onPreviewGift = { gift ->
                onPreviewGift(gift, giftTarget.second.ifBlank { room.ownerName })
            }
        )
    }
}

@Composable
fun SeatTile(
    seat: RoomSeat,
    effectiveSpeaking: Int,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(RoyalPurpleDeep.copy(alpha = 0.65f))
            .border(
                1.dp,
                when {
                    effectiveSpeaking > 15 -> NeonCyan
                    seat.occupantVip == "SVIP" -> NeonPink
                    seat.occupantVip == "VIP" -> RoyalGold
                    else -> Color.Transparent
                },
                RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onClick)
            .padding(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (seat.occupantId.isBlank()) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(RoyalPurpleSurface),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (seat.locked) Icons.Filled.Lock else Icons.Filled.Mic,
                    contentDescription = "Seat ${seat.seatIndex + 1}",
                    tint = if (seat.locked) RoyalGold else TextMuted,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.height(3.dp))
            Text("${seat.seatIndex + 1}", style = MaterialTheme.typography.labelSmall, color = TextMuted)
        } else {
            Box {
                UserAvatarWithFrame(
                    avatarUrl = "",
                    username = seat.occupantName,
                    vipTier = seat.occupantVip,
                    speakingLevel = effectiveSpeaking,
                    size = 44.dp
                )
                if (seat.muted) {
                    Icon(
                        Icons.Filled.MicOff,
                        contentDescription = "Muted",
                        tint = CrimsonRed,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(16.dp)
                            .background(ObsidianVoid, CircleShape)
                    )
                }
            }
            Spacer(Modifier.height(3.dp))
            Text(
                seat.occupantName,
                style = MaterialTheme.typography.labelSmall,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
