package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.localization.AppLang
import com.example.localization.LocalAddaStrings
import com.example.model.AddaGroup
import com.example.model.Agency
import com.example.model.AppEvent
import com.example.model.Family
import com.example.model.GiftCatalogSeed
import com.example.model.PkBattle
import com.example.model.UserMission
import com.example.model.UserProfile
import com.example.model.VipPlan
import com.example.model.VoiceRoom
import com.example.ui.components.LevelBadge
import com.example.ui.components.VipBadge
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

@Composable
fun HubAndRankingsScreen(
    me: UserProfile?,
    lang: AppLang,
    users: List<UserProfile>,
    rooms: List<VoiceRoom>,
    addas: List<AddaGroup>,
    families: List<Family>,
    agencies: List<Agency>,
    pkBattles: List<PkBattle>,
    events: List<AppEvent>,
    missions: List<UserMission>,
    onCreateFamily: (name: String, motto: String) -> Unit,
    onJoinFamily: (Family) -> Unit,
    onCreateAgency: (name: String, desc: String, commission: Long) -> Unit,
    onApplyAsHost: (Agency) -> Unit,
    onClaimMission: (UserMission) -> Unit,
    onPurchaseVip: (VipPlan) -> Unit
) {
    val strings = LocalAddaStrings.current
    var hubTab by remember { mutableIntStateOf(0) }
    var rankPeriod by remember { mutableStateOf("DAILY") }
    var rankCategory by remember { mutableStateOf("USERS") }
    var showCreateFamily by remember { mutableStateOf(false) }
    var showCreateAgency by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(
            selectedTabIndex = hubTab,
            containerColor = ObsidianSurface,
            contentColor = RoyalGold
        ) {
            Tab(selected = hubTab == 0, onClick = { hubTab = 0 }, text = { Text("Rank") })
            Tab(selected = hubTab == 1, onClick = { hubTab = 1 }, text = { Text("Family") })
            Tab(selected = hubTab == 2, onClick = { hubTab = 2 }, text = { Text("Agency") })
            Tab(selected = hubTab == 3, onClick = { hubTab = 3 }, text = { Text("VIP/Event") })
        }

        when (hubTab) {
            // RANKINGS
            0 -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 96.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        Text(strings.rankings, style = MaterialTheme.typography.headlineSmall, color = RoyalGold)
                        Spacer(Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("DAILY", "WEEKLY", "MONTHLY").forEach { p ->
                                FilterChip(selected = rankPeriod == p, onClick = { rankPeriod = p }, label = { Text(p) })
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("USERS", "GIFTERS", "HOSTS", "ROOMS", "ADDA", "FAMILIES", "AGENCIES", "PK").forEach { cat ->
                                item {
                                    FilterChip(selected = rankCategory == cat, onClick = { rankCategory = cat }, label = { Text(cat) })
                                }
                            }
                        }
                    }

                    when (rankCategory) {
                        "ROOMS" -> itemsIndexed(rooms.sortedByDescending { it.totalGiftCoins }) { idx, r ->
                            RankRowCard(idx + 1, r.title, "Host: ${r.ownerName} • Lv.${r.level}", "🪙 ${r.totalGiftCoins}")
                        }
                        "ADDA" -> itemsIndexed(addas.sortedByDescending { it.xp }) { idx, a ->
                            RankRowCard(idx + 1, a.name, "${a.memberCount} Members • Lv.${a.level}", "${a.xp} XP")
                        }
                        "FAMILIES" -> itemsIndexed(families.sortedByDescending { it.totalGiftPoints + it.xp }) { idx, f ->
                            RankRowCard(idx + 1, f.name, "${f.memberCount} Members • Lv.${f.level}", "${f.totalGiftPoints} Pts")
                        }
                        "AGENCIES" -> itemsIndexed(agencies.sortedByDescending { it.totalDiamonds }) { idx, ag ->
                            RankRowCard(idx + 1, ag.name, "${ag.hostCount} Hosts", "💎 ${ag.totalDiamonds}")
                        }
                        "PK" -> itemsIndexed(pkBattles) { idx, pk ->
                            RankRowCard(idx + 1, "${pk.redTeamName} vs ${pk.blueTeamName}", "Winner: ${pk.winnerTeam}", "${pk.redScore} - ${pk.blueScore}")
                        }
                        "HOSTS" -> {
                            val hosts = users.filter { it.hostStatus == "APPROVED" }.ifEmpty { users }
                            itemsIndexed(hosts.sortedByDescending { it.giftReceiveLevel }) { idx, u ->
                                RankRowCard(idx + 1, u.username, "Host Lv.${u.giftReceiveLevel}", "${u.xp} XP")
                            }
                        }
                        "GIFTERS" -> itemsIndexed(users.sortedByDescending { it.giftSendLevel }) { idx, u ->
                            RankRowCard(idx + 1, u.username, "Send Lv.${u.giftSendLevel}", "${u.xp} XP")
                        }
                        else -> itemsIndexed(users.sortedByDescending { it.xp }) { idx, u ->
                            RankRowCard(idx + 1, u.username, "Lv.${u.level} • ${u.vipTier}", "${u.xp} XP")
                        }
                    }
                }
            }

            // FAMILIES
            1 -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 96.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(strings.families, style = MaterialTheme.typography.headlineSmall, color = RoyalGold)
                            Button(onClick = { showCreateFamily = true }, colors = ButtonDefaults.buttonColors(containerColor = RoyalPurpleAccent)) {
                                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(strings.createFamily)
                            }
                        }
                    }
                    items(families, key = { it.id }) { fam ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = RoyalPurpleDeep),
                            border = BorderStroke(1.dp, RoyalGold.copy(0.45f))
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.img_royal_crest),
                                    contentDescription = fam.name,
                                    modifier = Modifier.size(54.dp).clip(RoundedCornerShape(12.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(fam.name, style = MaterialTheme.typography.titleMedium, color = RoyalGold)
                                        LevelBadge(fam.level)
                                    }
                                    Text(fam.motto, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                                    Text("${fam.memberCount} Members • Owner: ${fam.ownerName}", style = MaterialTheme.typography.labelSmall, color = NeonCyan)
                                }
                                if (me?.familyId != fam.id) {
                                    OutlinedButton(onClick = { onJoinFamily(fam) }) { Text("Join") }
                                } else {
                                    Surface(color = EmeraldLive.copy(0.2f), shape = RoundedCornerShape(8.dp)) {
                                        Text("Joined", color = EmeraldLive, modifier = Modifier.padding(8.dp), style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // AGENCIES & HOSTS
            2 -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 96.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(strings.agencies, style = MaterialTheme.typography.headlineSmall, color = RoyalGold)
                            Button(onClick = { showCreateAgency = true }, colors = ButtonDefaults.buttonColors(containerColor = NeonPink)) {
                                Icon(Icons.Filled.Storefront, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(strings.createAgency)
                            }
                        }
                    }
                    items(agencies, key = { it.id }) { ag ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = RoyalPurpleDeep),
                            border = BorderStroke(1.dp, NeonCyan.copy(0.4f))
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(ag.name, style = MaterialTheme.typography.titleMedium, color = RoyalGold)
                                    Text("${ag.commissionPercent}% Share", style = MaterialTheme.typography.labelSmall, color = NeonCyan)
                                }
                                Text(ag.description, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Text("Hosts: ${ag.hostCount} • 💎 ${ag.totalDiamonds}", style = MaterialTheme.typography.labelSmall, color = TextPrimary)
                                    if (me?.agencyId != ag.id) {
                                        Button(onClick = { onApplyAsHost(ag) }) { Text(strings.registerHost) }
                                    } else {
                                        Text("Registered Host", color = EmeraldLive, style = MaterialTheme.typography.labelLarge)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // VIP, SVIP, EVENTS & MISSIONS
            3 -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 96.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text(strings.vipSvip, style = MaterialTheme.typography.headlineSmall, color = RoyalGold)
                    }
                    items(GiftCatalogSeed.vipPlans, key = { it.tier }) { plan ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = RoyalPurpleSurface),
                            border = BorderStroke(1.5.dp, if (plan.tier == "SVIP") NeonPink else RoyalGold)
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Icon(Icons.Filled.WorkspacePremium, contentDescription = null, tint = if (plan.tier == "SVIP") NeonPink else RoyalGold)
                                        Text(if (lang == AppLang.HI) plan.titleHi else plan.title, style = MaterialTheme.typography.titleLarge, color = RoyalGold)
                                    }
                                    VipBadge(plan.tier)
                                }
                                (if (lang == AppLang.HI) plan.perksHi else plan.perksEn).forEach { perk ->
                                    Text("✦ $perk", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                                }
                                Button(
                                    onClick = { onPurchaseVip(plan) },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (plan.tier == "SVIP") NeonPink else RoyalGold,
                                        contentColor = if (plan.tier == "SVIP") TextPrimary else ObsidianVoid
                                    )
                                ) {
                                    Text("Activate ${plan.tier} • 🪙 ${plan.priceCoins}")
                                }
                            }
                        }
                    }

                    item {
                        Text(strings.eventsMissions, style = MaterialTheme.typography.headlineSmall, color = NeonCyan)
                    }
                    items(missions, key = { it.id }) { m ->
                        Card(colors = CardDefaults.cardColors(containerColor = RoyalPurpleDeep)) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(m.title, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                                    Text("+${m.rewardXp} XP • ${m.progress}/${m.target}", style = MaterialTheme.typography.labelSmall, color = RoyalGold)
                                }
                                Button(
                                    onClick = { onClaimMission(m) },
                                    enabled = !m.claimed
                                ) {
                                    Text(if (m.claimed) "Claimed" else strings.claimReward)
                                }
                            }
                        }
                    }
                    items(events, key = { it.id }) { ev ->
                        Card(colors = CardDefaults.cardColors(containerColor = RoyalPurpleSurface)) {
                            Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Icon(Icons.Filled.Celebration, contentDescription = null, tint = RoyalGold)
                                Column {
                                    Text(if (lang == AppLang.HI) ev.titleHi else ev.title, style = MaterialTheme.typography.titleMedium, color = RoyalGold)
                                    Text(ev.description, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateFamily) {
        var name by remember { mutableStateOf("") }
        var motto by remember { mutableStateOf("Ek Parivaar, Ek Awaaz") }
        AlertDialog(
            onDismissRequest = { showCreateFamily = false },
            containerColor = ObsidianSurface,
            title = { Text(strings.createFamily, color = RoyalGold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Family Name") })
                    OutlinedTextField(value = motto, onValueChange = { motto = it }, label = { Text("Motto") })
                }
            },
            confirmButton = {
                Button(onClick = {
                    onCreateFamily(name.ifBlank { "Royal Family" }, motto)
                    showCreateFamily = false
                }) { Text(strings.createFamily) }
            },
            dismissButton = { TextButton(onClick = { showCreateFamily = false }) { Text(strings.cancel) } }
        )
    }

    if (showCreateAgency) {
        var name by remember { mutableStateOf("") }
        var desc by remember { mutableStateOf("Official ADDA Talent Agency") }
        AlertDialog(
            onDismissRequest = { showCreateAgency = false },
            containerColor = ObsidianSurface,
            title = { Text(strings.createAgency, color = RoyalGold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Agency Name") })
                    OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Description") })
                }
            },
            confirmButton = {
                Button(onClick = {
                    onCreateAgency(name.ifBlank { "Starlight Agency" }, desc, 15L)
                    showCreateAgency = false
                }) { Text(strings.createAgency) }
            },
            dismissButton = { TextButton(onClick = { showCreateAgency = false }) { Text(strings.cancel) } }
        )
    }
}

@Composable
private fun RankRowCard(rank: Int, title: String, subtitle: String, score: String) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (rank <= 3) RoyalPurpleSurface else RoyalPurpleDeep
        ),
        border = if (rank == 1) BorderStroke(1.dp, RoyalGold) else null
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "#$rank",
                    style = MaterialTheme.typography.titleLarge,
                    color = if (rank <= 3) RoyalGold else NeonCyan,
                    fontWeight = FontWeight.ExtraBold
                )
                Column {
                    Text(title, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    Text(subtitle, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                }
            }
            Text(score, style = MaterialTheme.typography.labelLarge, color = RoyalGold)
        }
    }
}
