package com.example.ui.screens

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SportsKabaddi
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.localization.LocalAddaStrings
import com.example.model.Announcement
import com.example.model.PkBattle
import com.example.model.UserProfile
import com.example.model.VoiceRoom
import com.example.ui.components.AddaBrandLogo
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
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.UiState
import kotlin.math.roundToInt

@Composable
fun HomeScreen(
    profile: UserProfile?,
    coinsBalance: Long,
    roomsState: UiState<List<VoiceRoom>>,
    pkBattles: List<PkBattle>,
    announcements: List<Announcement>,
    unreadNotifs: Int,
    isAdmin: Boolean,
    onOpenRoom: (VoiceRoom) -> Unit,
    onCreateRoom: (title: String, topic: String, type: String, pin: String, seats: Int, themeId: String, announcement: String) -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenRankings: () -> Unit,
    onOpenWallet: () -> Unit,
    onOpenAdminPanel: () -> Unit
) {
    val strings = LocalAddaStrings.current
    var searchQuery by remember { mutableStateOf("") }
    var filterType by remember { mutableStateOf("ALL") }
    var popularitySort by remember { mutableStateOf("POPULAR") }
    var showCreateDialog by remember { mutableStateOf(false) }
    var pinPromptRoom by remember { mutableStateOf<VoiceRoom?>(null) }
    var enteredPin by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 96.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Top Bar
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AddaBrandLogo(size = 40.dp)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            modifier = Modifier
                                .clickable { onOpenWallet() }
                                .testTag("header_coins_pill"),
                            shape = RoundedCornerShape(16.dp),
                            color = RoyalPurpleSurface,
                            border = BorderStroke(1.dp, RoyalGold.copy(alpha = 0.6f))
                        ) {
                            Text(
                                text = "🪙 $coinsBalance",
                                style = MaterialTheme.typography.labelLarge,
                                color = RoyalGold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                        if (isAdmin) {
                            IconButton(
                                onClick = onOpenAdminPanel,
                                modifier = Modifier.testTag("admin_panel_button")
                            ) {
                                Icon(
                                    Icons.Filled.AdminPanelSettings,
                                    contentDescription = strings.adminDashboard,
                                    tint = RoyalGold
                                )
                            }
                        }
                        IconButton(
                            onClick = onOpenNotifications,
                            modifier = Modifier.testTag("notifications_button")
                        ) {
                            BadgedBox(
                                badge = {
                                    if (unreadNotifs > 0) {
                                        Badge(containerColor = NeonPink) {
                                            Text("$unreadNotifs", color = TextPrimary)
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    Icons.Filled.Notifications,
                                    contentDescription = strings.notifications,
                                    tint = NeonCyan
                                )
                            }
                        }
                    }
                }
            }

            // Hero Stage Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, Brush.linearGradient(listOf(RoyalGold, NeonCyan, NeonPink)))
                ) {
                    Box(modifier = Modifier.height(168.dp).fillMaxWidth()) {
                        Image(
                            painter = painterResource(id = R.drawable.img_adda_hero),
                            contentDescription = "ADDA Live Stage",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(ObsidianVoid.copy(alpha = 0.88f), Color.Transparent)
                                    )
                                )
                        )
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(18.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "नमस्ते, ${profile?.username ?: "ADDA Star"}",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = RoyalGold
                                )
                                Text(
                                    text = strings.tagline,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextPrimary,
                                    maxLines = 2
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Button(
                                    onClick = { showCreateDialog = true },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = NeonPink,
                                        contentColor = TextPrimary
                                    ),
                                    modifier = Modifier.testTag("hero_create_room_button")
                                ) {
                                    Icon(Icons.Filled.Mic, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(strings.createRoom)
                                }
                                OutlinedButton(
                                    onClick = onOpenRankings,
                                    border = BorderStroke(1.dp, NeonCyan)
                                ) {
                                    Text(strings.rankings, color = NeonCyan)
                                }
                            }
                        }
                    }
                }
            }

            // Live Announcements
            if (announcements.isNotEmpty()) {
                item {
                    val latest = announcements.first()
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = RoyalPurpleSurface,
                        border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.45f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Filled.Campaign, contentDescription = null, tint = RoyalGold)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(latest.title, style = MaterialTheme.typography.labelLarge, color = RoyalGold)
                                Text(latest.body, style = MaterialTheme.typography.bodyMedium, color = TextSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }

            // PK Battle Spotlight
            val activePk = pkBattles.firstOrNull { it.status == "ACTIVE" }
            if (activePk != null) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, CrimsonRed.copy(alpha = 0.7f))
                    ) {
                        Box(modifier = Modifier.height(106.dp).fillMaxWidth()) {
                            Image(
                                painter = painterResource(id = R.drawable.img_pk_arena_banner),
                                contentDescription = strings.pkBattle,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            Box(modifier = Modifier.fillMaxSize().background(ObsidianVoid.copy(alpha = 0.55f)))
                            Row(
                                modifier = Modifier.fillMaxSize().padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Icon(Icons.Filled.SportsKabaddi, contentDescription = null, tint = RoyalGold)
                                        Text("LIVE ${strings.pkBattle}", style = MaterialTheme.typography.titleMedium, color = RoyalGold)
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        "${activePk.redTeamName} (${activePk.redScore}) VS (${activePk.blueScore}) ${activePk.blueTeamName}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Search Bar, Category Filters & Popularity Sorting
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(strings.liveVoiceRooms, style = MaterialTheme.typography.titleLarge, color = TextPrimary)

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = {
                            Text(
                                text = strings.searchRoomsPlaceholder,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.Search,
                                contentDescription = strings.searchRoomsPlaceholder,
                                tint = NeonCyan
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(
                                    onClick = { searchQuery = "" },
                                    modifier = Modifier.testTag("home_room_search_clear")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Close,
                                        contentDescription = strings.close,
                                        tint = TextSecondary
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = RoyalPurpleDeep,
                            unfocusedContainerColor = RoyalPurpleDeep.copy(alpha = 0.85f),
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = RoyalPurpleAccent.copy(alpha = 0.55f),
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            cursorColor = NeonCyan
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("home_room_search_input")
                    )

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val categoryFilters = listOf(
                            "ALL" to strings.catAll,
                            "PUBLIC" to strings.publicRoom,
                            "PRIVATE" to strings.privateRoom,
                            "PASSWORD" to strings.passwordRoom,
                            "PK" to strings.catPkLive,
                            "MUSIC" to strings.catMusic,
                            "GAMING" to strings.catGaming
                        )
                        items(categoryFilters) { (key, label) ->
                            FilterChip(
                                selected = filterType == key,
                                onClick = { filterType = key },
                                label = { Text(label) },
                                modifier = Modifier.testTag("room_category_chip_$key"),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = RoyalPurpleAccent,
                                    selectedLabelColor = TextPrimary
                                )
                            )
                        }
                    }

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val popularityOptions = listOf(
                            "POPULAR" to strings.sortPopular,
                            "TOP_GIFTS" to strings.sortTopGifts,
                            "HIGH_LEVEL" to strings.sortHighLevel,
                            "NEWEST" to strings.sortNewest
                        )
                        items(popularityOptions) { (key, label) ->
                            FilterChip(
                                selected = popularitySort == key,
                                onClick = { popularitySort = key },
                                leadingIcon = if (key == "POPULAR") {
                                    {
                                        Icon(
                                            imageVector = Icons.Filled.Whatshot,
                                            contentDescription = null,
                                            tint = if (popularitySort == key) RoyalGold else NeonPink,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                } else null,
                                label = { Text(label, style = MaterialTheme.typography.labelMedium) },
                                modifier = Modifier.testTag("room_popularity_chip_$key"),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = RoyalGold.copy(alpha = 0.22f),
                                    selectedLabelColor = RoyalGold
                                )
                            )
                        }
                    }
                }
            }

            // Rooms List
            when (roomsState) {
                is UiState.Loading -> {
                    item {
                        Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = NeonCyan)
                        }
                    }
                }
                is UiState.Error -> {
                    item {
                        Text(roomsState.message, color = CrimsonRed, modifier = Modifier.padding(12.dp))
                    }
                }
                is UiState.Success -> {
                    val query = searchQuery.trim()
                    val filtered = roomsState.data.filter { room ->
                        val matchesCategory = when (filterType) {
                            "ALL" -> true
                            "PUBLIC", "PRIVATE", "PASSWORD" -> room.roomType == filterType
                            "PK" -> room.pkActive
                            "MUSIC" -> listOf(room.title, room.topic, room.themeId).any {
                                it.contains("music", ignoreCase = true) ||
                                    it.contains("shayari", ignoreCase = true) ||
                                    it.contains("song", ignoreCase = true) ||
                                    it.contains("संगीत", ignoreCase = true) ||
                                    it.contains("शायरी", ignoreCase = true)
                            }
                            "GAMING" -> listOf(room.title, room.topic, room.themeId).any {
                                it.contains("game", ignoreCase = true) ||
                                    it.contains("gaming", ignoreCase = true) ||
                                    it.contains("ludo", ignoreCase = true) ||
                                    it.contains("pk", ignoreCase = true) ||
                                    it.contains("गेम", ignoreCase = true)
                            }
                            else -> true
                        }
                        val matchesQuery = query.isEmpty() || listOf(
                            room.title,
                            room.topic,
                            room.ownerName,
                            room.roomType,
                            room.themeId,
                            room.announcement
                        ).any { it.contains(query, ignoreCase = true) }
                        matchesCategory && matchesQuery
                    }
                    val list = when (popularitySort) {
                        "TOP_GIFTS" -> filtered.sortedByDescending { it.totalGiftCoins }
                        "HIGH_LEVEL" -> filtered.sortedWith(
                            compareByDescending<VoiceRoom> { it.level }.thenByDescending { it.xp }
                        )
                        "NEWEST" -> filtered.sortedByDescending { it.createdAt?.seconds ?: 0L }
                        else -> filtered.sortedByDescending { (it.onlineCount * 10L) + it.totalGiftCoins + it.level }
                    }
                    if (list.isEmpty()) {
                        val hasActiveFilters = query.isNotEmpty() || filterType != "ALL"
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = RoyalPurpleDeep)
                            ) {
                                Column(
                                    modifier = Modifier.padding(24.dp).fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(Icons.Filled.GraphicEq, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(40.dp))
                                    Text(
                                        text = if (hasActiveFilters) {
                                            strings.noMatchingRooms
                                        } else {
                                            "अभी कोई लाइव रूम नहीं है — पहला वॉइस रूम बनाएँ!"
                                        },
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = TextSecondary
                                    )
                                    if (hasActiveFilters) {
                                        OutlinedButton(
                                            onClick = {
                                                searchQuery = ""
                                                filterType = "ALL"
                                            },
                                            border = BorderStroke(1.dp, NeonCyan),
                                            modifier = Modifier.testTag("reset_room_filters_button")
                                        ) {
                                            Text(strings.resetFilters, color = NeonCyan)
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        items(list, key = { it.id }) { room ->
                            VoiceRoomLobbyCard(
                                room = room,
                                onClick = {
                                    if (room.roomType == "PASSWORD" && room.ownerId != profile?.userId) {
                                        pinPromptRoom = room
                                        enteredPin = ""
                                        pinError = false
                                    } else {
                                        onOpenRoom(room)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { showCreateDialog = true },
            containerColor = NeonPink,
            contentColor = TextPrimary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(18.dp)
                .testTag("fab_create_room")
        ) {
            Icon(Icons.Filled.Add, contentDescription = strings.createRoom)
        }
    }

    if (showCreateDialog) {
        CreateVoiceRoomDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { title, topic, type, pin, seats, themeId, ann ->
                onCreateRoom(title, topic, type, pin, seats, themeId, ann)
                showCreateDialog = false
            }
        )
    }

    pinPromptRoom?.let { target ->
        AlertDialog(
            onDismissRequest = { pinPromptRoom = null },
            containerColor = ObsidianSurface,
            title = { Text(strings.enterRoomPin, color = RoyalGold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = enteredPin,
                        onValueChange = { enteredPin = it; pinError = false },
                        label = { Text("PIN") },
                        singleLine = true
                    )
                    if (pinError) {
                        Text("Incorrect password!", color = CrimsonRed)
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (enteredPin == target.roomPin) {
                        val r = target
                        pinPromptRoom = null
                        onOpenRoom(r)
                    } else {
                        pinError = true
                    }
                }) {
                    Text(strings.joinRoom)
                }
            },
            dismissButton = {
                TextButton(onClick = { pinPromptRoom = null }) { Text(strings.cancel) }
            }
        )
    }
}

@Composable
private fun VoiceRoomLobbyCard(room: VoiceRoom, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("voice_room_card_${room.id}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = RoyalPurpleDeep),
        border = BorderStroke(
            1.dp,
            if (room.pkActive) CrimsonRed else RoyalPurpleAccent.copy(alpha = 0.6f)
        )
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(shape = RoundedCornerShape(8.dp), color = EmeraldLive.copy(alpha = 0.2f)) {
                        Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.GraphicEq, contentDescription = null, tint = EmeraldLive, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("LIVE", style = MaterialTheme.typography.labelSmall, color = EmeraldLive)
                        }
                    }
                    LevelBadge(room.level)
                    if (room.roomType == "PASSWORD") {
                        Icon(Icons.Filled.Lock, contentDescription = "Password", tint = RoyalGold, modifier = Modifier.size(16.dp))
                    }
                    if (room.pkActive) {
                        Surface(shape = RoundedCornerShape(6.dp), color = CrimsonRed) {
                            Text("PK", style = MaterialTheme.typography.labelSmall, color = TextPrimary, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Filled.Group, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(16.dp))
                    Text("${room.onlineCount} • ${room.seatCount} Mics", style = MaterialTheme.typography.labelSmall, color = NeonCyan)
                }
            }
            Text(room.title, style = MaterialTheme.typography.titleLarge, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (room.topic.isNotBlank()) {
                Text(room.topic, style = MaterialTheme.typography.bodyMedium, color = TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Host: ${room.ownerName}", style = MaterialTheme.typography.labelLarge, color = RoyalGold)
                Text("🪙 ${room.totalGiftCoins}", style = MaterialTheme.typography.labelSmall, color = NeonPink)
            }
        }
    }
}

@Composable
private fun CreateVoiceRoomDialog(
    onDismiss: () -> Unit,
    onCreate: (String, String, String, String, Int, String, String) -> Unit
) {
    val strings = LocalAddaStrings.current
    var title by remember { mutableStateOf("") }
    var topic by remember { mutableStateOf("Shayari, Music & Adda") }
    var roomType by remember { mutableStateOf("PUBLIC") }
    var pin by remember { mutableStateOf("") }
    var seatsSlider by remember { mutableFloatStateOf(8f) }
    var themeId by remember { mutableStateOf("ROYAL_STAGE") }
    var announcement by remember { mutableStateOf("Welcome to ADDA Live!") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ObsidianSurface,
        title = { Text(strings.createRoom, color = RoyalGold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(strings.roomTitle) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("create_room_title_input")
                )
                OutlinedTextField(
                    value = topic,
                    onValueChange = { topic = it },
                    label = { Text(strings.roomTopic) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("PUBLIC" to strings.publicRoom, "PRIVATE" to strings.privateRoom, "PASSWORD" to strings.passwordRoom).forEach { (k, l) ->
                        FilterChip(selected = roomType == k, onClick = { roomType = k }, label = { Text(l, style = MaterialTheme.typography.labelSmall) })
                    }
                }
                if (roomType == "PASSWORD") {
                    OutlinedTextField(
                        value = pin,
                        onValueChange = { pin = it.take(8) },
                        label = { Text("Room PIN") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Text("${strings.seatCount}: ${seatsSlider.roundToInt()}", style = MaterialTheme.typography.labelLarge, color = NeonCyan)
                Slider(
                    value = seatsSlider,
                    onValueChange = { seatsSlider = it },
                    valueRange = 8f..16f,
                    steps = 7
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("ROYAL_STAGE", "NEON_CLUB", "DIWALI_GOLD").forEach { th ->
                        FilterChip(selected = themeId == th, onClick = { themeId = th }, label = { Text(th.substringBefore("_"), style = MaterialTheme.typography.labelSmall) })
                    }
                }
                OutlinedTextField(
                    value = announcement,
                    onValueChange = { announcement = it },
                    label = { Text(strings.announcement) },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val safeTitle = title.trim().ifBlank { "ADDA Royal Lounge" }
                    onCreate(safeTitle, topic, roomType, pin, seatsSlider.roundToInt(), themeId, announcement)
                },
                modifier = Modifier.testTag("confirm_create_room_button"),
                colors = ButtonDefaults.buttonColors(containerColor = RoyalPurpleAccent)
            ) {
                Text(strings.createRoom)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(strings.cancel) }
        }
    )
}
