package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.R
import com.example.localization.AppLang
import com.example.localization.LocalAddaStrings
import com.example.model.AddaGroup
import com.example.model.GiftItem
import com.example.model.RoomMember
import com.example.model.RoomMessage
import com.example.model.RoomSeat
import com.example.model.UserProfile
import com.example.ui.components.GiftPickerSheet
import com.example.ui.components.LevelBadge
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
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.UiState
import kotlin.math.roundToInt

@Composable
fun AddaGroupsScreen(
    groupsState: UiState<List<AddaGroup>>,
    onOpenGroup: (AddaGroup) -> Unit,
    onCreateGroup: (name: String, desc: String, rules: String, isPublic: Boolean, seats: Int, themeId: String) -> Unit
) {
    val strings = LocalAddaStrings.current
    var showCreate by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp, 14.dp, 16.dp, 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(strings.addaGroupsTitle, style = MaterialTheme.typography.headlineSmall, color = RoyalGold)
                        Text("अपना ADDA बनाएँ • नियम, को-एडमिन और लाइव वॉइस", style = MaterialTheme.typography.bodyMedium, color = NeonCyan)
                    }
                    Button(
                        onClick = { showCreate = true },
                        colors = ButtonDefaults.buttonColors(containerColor = RoyalPurpleAccent),
                        modifier = Modifier.testTag("create_adda_button")
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(strings.createAdda)
                    }
                }
            }

            when (groupsState) {
                is UiState.Loading -> item {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = NeonCyan)
                    }
                }
                is UiState.Error -> item { Text(groupsState.message, color = CrimsonRed) }
                is UiState.Success -> {
                    if (groupsState.data.isEmpty()) {
                        item {
                            Card(colors = CardDefaults.cardColors(containerColor = RoyalPurpleDeep)) {
                                Column(
                                    modifier = Modifier.padding(24.dp).fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(Icons.Filled.Groups, contentDescription = null, tint = RoyalGold, modifier = Modifier.size(44.dp))
                                    Spacer(Modifier.height(8.dp))
                                    Text("पहला ADDA ग्रुप बनाएँ और दोस्तों को आमंत्रित करें!", color = TextSecondary)
                                }
                            }
                        }
                    } else {
                        items(groupsState.data, key = { it.id }) { adda ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onOpenGroup(adda) }
                                    .testTag("adda_group_card_${adda.id}"),
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = RoyalPurpleDeep),
                                border = BorderStroke(1.dp, RoyalGold.copy(alpha = 0.45f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Image(
                                        painter = painterResource(id = R.drawable.img_royal_crest),
                                        contentDescription = adda.name,
                                        modifier = Modifier.size(56.dp).clip(RoundedCornerShape(14.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Text(adda.name, style = MaterialTheme.typography.titleMedium, color = RoyalGold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            LevelBadge(adda.level)
                                            Icon(
                                                imageVector = if (adda.publicAdda) Icons.Filled.Public else Icons.Filled.Lock,
                                                contentDescription = null,
                                                tint = NeonCyan,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                        Text(adda.description.ifBlank { adda.rules }, style = MaterialTheme.typography.bodyMedium, color = TextSecondary, maxLines = 2)
                                        Text("Owner: ${adda.ownerName} • ${adda.memberCount} Members • ${adda.seatCount} Mics", style = MaterialTheme.typography.labelSmall, color = NeonCyan)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { showCreate = true },
            containerColor = RoyalGold,
            contentColor = ObsidianVoid,
            modifier = Modifier.align(Alignment.BottomEnd).padding(18.dp)
        ) {
            Icon(Icons.Filled.Add, contentDescription = strings.createAdda)
        }
    }

    if (showCreate) {
        var name by remember { mutableStateOf("") }
        var desc by remember { mutableStateOf("") }
        var rules by remember { mutableStateOf("सम्मान बनाए रखें • स्पैम प्रतिबंधित") }
        var isPublic by remember { mutableStateOf(true) }
        var seats by remember { mutableFloatStateOf(8f) }

        AlertDialog(
            onDismissRequest = { showCreate = false },
            containerColor = ObsidianSurface,
            title = { Text(strings.createAdda, color = RoyalGold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("ADDA Name") }, singleLine = true, modifier = Modifier.fillMaxWidth().testTag("create_adda_name_input"))
                    OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Description") }, maxLines = 2, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = rules, onValueChange = { rules = it }, label = { Text(strings.addaRules) }, maxLines = 2, modifier = Modifier.fillMaxWidth())
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = isPublic, onClick = { isPublic = true }, label = { Text(strings.publicRoom) })
                        FilterChip(selected = !isPublic, onClick = { isPublic = false }, label = { Text(strings.privateRoom) })
                    }
                    Text("${strings.seatCount}: ${seats.roundToInt()}", color = NeonCyan)
                    Slider(value = seats, onValueChange = { seats = it }, valueRange = 8f..16f, steps = 7)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onCreateGroup(name.ifBlank { "Royal ADDA" }, desc, rules, isPublic, seats.roundToInt(), "ROYAL_STAGE")
                        showCreate = false
                    },
                    modifier = Modifier.testTag("confirm_create_adda_button")
                ) { Text(strings.createAdda) }
            },
            dismissButton = { TextButton(onClick = { showCreate = false }) { Text(strings.cancel) } }
        )
    }
}

@Composable
fun AddaGroupDetailScreen(
    adda: AddaGroup,
    me: UserProfile?,
    coinsBalance: Long,
    seats: List<RoomSeat>,
    messages: List<RoomMessage>,
    members: List<RoomMember>,
    gifts: List<GiftItem>,
    lang: AppLang,
    isMicCapturing: Boolean,
    speakingLevel: Int,
    onExit: () -> Unit,
    onTakeSeat: (RoomSeat) -> Unit,
    onLeaveSeat: (RoomSeat) -> Unit,
    onToggleMyMic: (RoomSeat?, Boolean) -> Unit,
    onSendChat: (String) -> Unit,
    onSendGift: (String, String, GiftItem, Int) -> Unit,
    onPreviewGift: (GiftItem, String) -> Unit = { _, _ -> },
    onModerateMember: (targetUid: String, action: String) -> Unit
) {
    BackHandler { onExit() }

    val strings = LocalAddaStrings.current
    val context = LocalContext.current
    val isManager = me != null && (me.userId == adda.ownerId || me.userId in adda.coAdminUids || me.userId in adda.moderatorUids)
    val mySeat = seats.firstOrNull { it.occupantId == me?.userId && me.userId.isNotBlank() }

    var chatInput by remember { mutableStateOf("") }
    var showManageDialog by remember { mutableStateOf(false) }
    var showGiftSheet by remember { mutableStateOf(false) }
    var hasAudioPerm by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)
    }
    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasAudioPerm = granted
        if (granted) onToggleMyMic(mySeat, true)
    }

    Box(modifier = Modifier.fillMaxSize().background(ObsidianVoid)) {
        Column(modifier = Modifier.fillMaxSize().systemBarsPadding().padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    IconButton(onClick = onExit) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = strings.close, tint = TextPrimary)
                    }
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(adda.name, style = MaterialTheme.typography.titleLarge, color = RoyalGold)
                            LevelBadge(adda.level)
                        }
                        Text(adda.rules.ifBlank { adda.description }, style = MaterialTheme.typography.bodyMedium, color = NeonCyan, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                if (isManager) {
                    IconButton(onClick = { showManageDialog = true }) {
                        Icon(Icons.Filled.ManageAccounts, contentDescription = "Manage ADDA", tint = RoyalGold)
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                modifier = Modifier.fillMaxWidth().height(if (seats.size > 12) 250.dp else 195.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(seats, key = { it.seatIndex }) { seat ->
                    val isMine = seat.occupantId == me?.userId && seat.occupantId.isNotBlank()
                    val lvl = if (isMine && isMicCapturing) speakingLevel else seat.speakingLevel.toInt()
                    SeatTile(
                        seat = seat,
                        effectiveSpeaking = lvl,
                        onClick = {
                            if (seat.occupantId.isBlank()) onTakeSeat(seat)
                            else if (isMine) onLeaveSeat(seat)
                        }
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(RoyalPurpleDeep.copy(0.45f)).padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(messages, key = { it.id }) { msg ->
                    val isGift = msg.msgType == "GIFT"
                    Text(
                        text = if (isGift) "🎁 ${msg.text}" else "${msg.senderName}: ${msg.text}",
                        color = if (isGift) RoyalGold else TextPrimary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedTextField(
                    value = chatInput,
                    onValueChange = { chatInput = it },
                    placeholder = { Text(strings.chatPlaceholder) },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(24.dp)
                )
                IconButton(onClick = {
                    if (chatInput.isNotBlank()) {
                        onSendChat(chatInput)
                        chatInput = ""
                    }
                }) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = NeonCyan)
                }
                IconButton(
                    onClick = {
                        if (!hasAudioPerm) permLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        else onToggleMyMic(mySeat, true)
                    },
                    modifier = Modifier.size(48.dp).clip(CircleShape).background(if (isMicCapturing) EmeraldLive else RoyalPurpleSurface)
                ) {
                    Icon(if (isMicCapturing) Icons.Filled.Mic else Icons.Filled.MicOff, contentDescription = "Mic", tint = TextPrimary)
                }
                IconButton(
                    onClick = { showGiftSheet = true },
                    modifier = Modifier.size(48.dp).clip(CircleShape).background(NeonPink)
                ) {
                    Icon(Icons.Filled.CardGiftcard, contentDescription = strings.sendGift, tint = TextPrimary)
                }
            }
        }
    }

    if (showManageDialog) {
        AlertDialog(
            onDismissRequest = { showManageDialog = false },
            containerColor = ObsidianSurface,
            title = { Text("ADDA Member Management (${members.size})", color = RoyalGold) },
            text = {
                LazyColumn(modifier = Modifier.height(260.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(members, key = { it.userId }) { m ->
                        Column(
                            modifier = Modifier.fillMaxWidth().background(RoyalPurpleDeep, RoundedCornerShape(10.dp)).padding(8.dp)
                        ) {
                            Text("${m.username} (${m.role})", color = TextPrimary)
                            if (m.userId != adda.ownerId) {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    OutlinedButton(onClick = { onModerateMember(m.userId, "CO_ADMIN") }) { Text("Co-Admin", style = MaterialTheme.typography.labelSmall) }
                                    OutlinedButton(onClick = { onModerateMember(m.userId, "MUTE") }) { Text("Mute", style = MaterialTheme.typography.labelSmall) }
                                    OutlinedButton(onClick = { onModerateMember(m.userId, "KICK") }) { Text("Kick", style = MaterialTheme.typography.labelSmall, color = CrimsonRed) }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showManageDialog = false }) { Text(strings.close) } }
        )
    }

    if (showGiftSheet) {
        GiftPickerSheet(
            gifts = gifts,
            coinsBalance = coinsBalance,
            lang = lang,
            receiverLabel = adda.ownerName,
            onDismiss = { showGiftSheet = false },
            onSendGift = { gift, qty -> onSendGift(adda.ownerId, adda.ownerName, gift, qty) },
            onPreviewGift = { gift -> onPreviewGift(gift, adda.ownerName) }
        )
    }
}
