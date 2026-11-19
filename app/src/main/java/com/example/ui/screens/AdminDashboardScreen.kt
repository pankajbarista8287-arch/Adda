package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.model.AdminRegistry
import com.example.model.GiftItem
import com.example.model.RechargeOrder
import com.example.model.Report
import com.example.model.UserProfile
import com.example.ui.components.AddaCosmicBackground
import com.example.ui.theme.CrimsonRed
import com.example.ui.theme.EmeraldLive
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.RoyalGold
import com.example.ui.theme.RoyalPurpleAccent
import com.example.ui.theme.RoyalPurpleDeep
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun AdminDashboardScreen(
    adminRegistry: AdminRegistry?,
    users: List<UserProfile>,
    orders: List<RechargeOrder>,
    gifts: List<GiftItem>,
    reports: List<Report>,
    onBack: () -> Unit,
    onVerifyOrder: (RechargeOrder, Boolean) -> Unit,
    onModerateUser: (UserProfile, verifyToggle: Boolean, banToggle: Boolean) -> Unit,
    onUpdateGiftPrice: (GiftItem, Long) -> Unit,
    onCreateGift: (name: String, nameHi: String, category: String, priceCoins: Long, animationType: String, iconKey: String) -> Unit = { _, _, _, _, _, _ -> },
    onPublishAnnouncement: (title: String, body: String) -> Unit,
    onCreateEvent: (title: String, titleHi: String, desc: String, rewardCoins: Long) -> Unit,
    onResolveReport: (Report, String) -> Unit
) {
    BackHandler { onBack() }

    if (adminRegistry == null) {
        AddaCosmicBackground {
            Column(
                modifier = Modifier.fillMaxSize().systemBarsPadding().padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Unauthorized: Server-verified AKKI_ADMIN role required.", color = CrimsonRed)
                Spacer(Modifier.height(12.dp))
                Button(onClick = onBack) { Text("Back") }
            }
        }
        return
    }

    var adminTab by remember { mutableIntStateOf(0) }

    AddaCosmicBackground {
        Column(modifier = Modifier.fillMaxSize().systemBarsPadding()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                    Icon(Icons.Filled.AdminPanelSettings, contentDescription = null, tint = RoyalGold)
                    Text("  ${adminRegistry.adminCode} Dashboard", style = MaterialTheme.typography.titleLarge, color = RoyalGold)
                }
                Text(adminRegistry.role, style = MaterialTheme.typography.labelSmall, color = NeonCyan)
            }

            TabRow(selectedTabIndex = adminTab, containerColor = ObsidianSurface, contentColor = RoyalGold) {
                Tab(selected = adminTab == 0, onClick = { adminTab = 0 }, text = { Text("Payments (${orders.count { it.status == "PENDING" }})") })
                Tab(selected = adminTab == 1, onClick = { adminTab = 1 }, text = { Text("Users (${users.size})") })
                Tab(selected = adminTab == 2, onClick = { adminTab = 2 }, text = { Text("Gifts") })
                Tab(selected = adminTab == 3, onClick = { adminTab = 3 }, text = { Text("Alerts & Reports") })
            }

            when (adminTab) {
                0 -> LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(orders, key = { it.id }) { o ->
                        Card(colors = CardDefaults.cardColors(containerColor = RoyalPurpleDeep), border = BorderStroke(1.dp, RoyalGold.copy(0.4f))) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("${o.userName} • ₹${o.amountInr} ➔ ${o.coinsAmount} Coins", color = TextPrimary)
                                Text("UTR: ${o.utrReference} • Status: ${o.status}", style = MaterialTheme.typography.labelSmall, color = NeonCyan)
                                if (o.status == "PENDING") {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(
                                            onClick = { onVerifyOrder(o, true) },
                                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldLive)
                                        ) { Text("Verify & Credit") }
                                        OutlinedButton(onClick = { onVerifyOrder(o, false) }) { Text("Reject", color = CrimsonRed) }
                                    }
                                }
                            }
                        }
                    }
                }

                1 -> LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(users, key = { it.userId }) { u ->
                        Card(colors = CardDefaults.cardColors(containerColor = RoyalPurpleDeep)) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("${u.username} (ID: ${u.addaCode})", color = TextPrimary)
                                    Text("Lv.${u.level} • Verified: ${u.verified} • Banned: ${u.banned}", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    OutlinedButton(onClick = { onModerateUser(u, true, false) }) {
                                        Text(if (u.verified) "Unverify" else "Verify")
                                    }
                                    OutlinedButton(onClick = { onModerateUser(u, false, true) }) {
                                        Text(if (u.banned) "Unban" else "Ban", color = CrimsonRed)
                                    }
                                }
                            }
                        }
                    }
                }

                2 -> LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    item {
                        var newName by remember { mutableStateOf("") }
                        var newNameHi by remember { mutableStateOf("") }
                        var newPrice by remember { mutableStateOf("200000") }
                        var newCat by remember { mutableStateOf("ROYAL") }
                        var newAnim by remember { mutableStateOf("FULLSCREEN_ROYAL") }
                        var newIcon by remember { mutableStateOf("royal_tilak") }
                        val cats = listOf("ROYAL", "VIP", "LUXURY", "SPECIAL", "FESTIVAL", "PREMIUM", "COUPLE", "LOVE")
                        val anims = listOf("FULLSCREEN_ROYAL", "ORBIT_3D", "FLOAT_2D")
                        val icons = listOf("royal_tilak", "tilak", "dragon", "phoenix", "chariot", "galaxy", "crown", "palace", "rolls_royce", "yacht", "jet", "rose")

                        Card(
                            colors = CardDefaults.cardColors(containerColor = RoyalPurpleDeep),
                            border = BorderStroke(1.5.dp, RoyalGold.copy(0.65f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Add New Gift (Up to 10,00,000 Coins)", color = RoyalGold, style = MaterialTheme.typography.titleMedium)
                                OutlinedTextField(
                                    value = newName,
                                    onValueChange = { newName = it },
                                    label = { Text("Gift Name (English)") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = newNameHi,
                                    onValueChange = { newNameHi = it },
                                    label = { Text("Gift Name (Hindi)") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = newPrice,
                                    onValueChange = { newPrice = it.filter(Char::isDigit) },
                                    label = { Text("Coins Price (e.g. 200000 or 300000)") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Text("Category", color = NeonCyan, style = MaterialTheme.typography.labelSmall)
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    items(cats) { c ->
                                        FilterChip(selected = newCat == c, onClick = { newCat = c }, label = { Text(c) })
                                    }
                                }
                                Text("Animation Tier", color = NeonCyan, style = MaterialTheme.typography.labelSmall)
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    items(anims) { a ->
                                        FilterChip(selected = newAnim == a, onClick = { newAnim = a }, label = { Text(a) })
                                    }
                                }
                                Text("Icon Theme", color = NeonCyan, style = MaterialTheme.typography.labelSmall)
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    items(icons) { ik ->
                                        FilterChip(selected = newIcon == ik, onClick = { newIcon = ik }, label = { Text(ik) })
                                    }
                                }
                                Button(
                                    onClick = {
                                        val price = (newPrice.toLongOrNull() ?: 200000L).coerceIn(1L, 1_000_000L)
                                        if (newName.isNotBlank()) {
                                            onCreateGift(newName, newNameHi.ifBlank { newName }, newCat, price, newAnim, newIcon)
                                            newName = ""
                                            newNameHi = ""
                                        }
                                    },
                                    enabled = newName.isNotBlank(),
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldLive)
                                ) {
                                    Text("Create & Publish Gift")
                                }
                            }
                        }
                    }
                    items(gifts, key = { it.id }) { g ->
                        var priceText by remember(g.priceCoins) { mutableStateOf(g.priceCoins.toString()) }
                        Card(colors = CardDefaults.cardColors(containerColor = RoyalPurpleDeep)) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("${g.name} (${g.category})", color = RoyalGold)
                                    Text("${g.animationType} • ${com.example.model.formatIndianCoins(g.priceCoins)} Coins", style = MaterialTheme.typography.labelSmall, color = NeonCyan)
                                    OutlinedTextField(
                                        value = priceText,
                                        onValueChange = { priceText = it.filter(Char::isDigit) },
                                        label = { Text("Coins Price") },
                                        singleLine = true
                                    )
                                }
                                Button(onClick = {
                                    val p = priceText.toLongOrNull() ?: g.priceCoins
                                    onUpdateGiftPrice(g, p)
                                }) { Text("Save") }
                            }
                        }
                    }
                }

                3 -> LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    item {
                        var annTitle by remember { mutableStateOf("") }
                        var annBody by remember { mutableStateOf("") }
                        Card(colors = CardDefaults.cardColors(containerColor = RoyalPurpleDeep)) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Publish App Announcement", color = RoyalGold, style = MaterialTheme.typography.titleMedium)
                                OutlinedTextField(value = annTitle, onValueChange = { annTitle = it }, label = { Text("Title") }, modifier = Modifier.fillMaxWidth())
                                OutlinedTextField(value = annBody, onValueChange = { annBody = it }, label = { Text("Message") }, modifier = Modifier.fillMaxWidth())
                                Button(onClick = {
                                    if (annTitle.isNotBlank() && annBody.isNotBlank()) {
                                        onPublishAnnouncement(annTitle, annBody)
                                        annTitle = ""; annBody = ""
                                    }
                                }) { Text("Publish") }
                            }
                        }
                    }
                    item {
                        var evTitle by remember { mutableStateOf("") }
                        var evHi by remember { mutableStateOf("") }
                        var evDesc by remember { mutableStateOf("") }
                        Card(colors = CardDefaults.cardColors(containerColor = RoyalPurpleDeep)) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Create Festival / PK Event", color = NeonCyan, style = MaterialTheme.typography.titleMedium)
                                OutlinedTextField(value = evTitle, onValueChange = { evTitle = it }, label = { Text("Title (EN)") }, modifier = Modifier.fillMaxWidth())
                                OutlinedTextField(value = evHi, onValueChange = { evHi = it }, label = { Text("Title (HI)") }, modifier = Modifier.fillMaxWidth())
                                OutlinedTextField(value = evDesc, onValueChange = { evDesc = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth())
                                Button(
                                    onClick = {
                                        if (evTitle.isNotBlank()) {
                                            onCreateEvent(evTitle, evHi.ifBlank { evTitle }, evDesc.ifBlank { "Special ADDA Event" }, 100L)
                                            evTitle = ""; evHi = ""; evDesc = ""
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = RoyalPurpleAccent)
                                ) { Text("Launch Event") }
                            }
                        }
                    }
                    items(reports, key = { it.id }) { rep ->
                        Card(colors = CardDefaults.cardColors(containerColor = RoyalPurpleDeep)) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("${rep.targetType}: ${rep.targetId} (${rep.status})", color = CrimsonRed)
                                Text(rep.reason, color = TextPrimary)
                                if (rep.status == "OPEN") {
                                    Button(onClick = { onResolveReport(rep, "Reviewed by AKKI_ADMIN") }) { Text("Mark Resolved") }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
