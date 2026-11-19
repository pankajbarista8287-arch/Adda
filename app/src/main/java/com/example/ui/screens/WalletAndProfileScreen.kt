package com.example.ui.screens

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.localization.AppLang
import com.example.localization.LocalAddaStrings
import com.example.model.GiftCatalogSeed
import com.example.model.RechargeOrder
import com.example.model.RechargePackage
import com.example.model.UserPrivateAccount
import com.example.model.UserProfile
import com.example.model.Wallet
import com.example.model.WalletTransaction
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
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletAndProfileScreen(
    me: UserProfile?,
    privateAccount: UserPrivateAccount?,
    wallet: Wallet?,
    orders: List<RechargeOrder>,
    transactions: List<WalletTransaction>,
    lang: AppLang,
    soundFxEnabled: Boolean,
    isAdmin: Boolean,
    onToggleLanguage: (AppLang) -> Unit,
    onToggleSoundFx: (Boolean) -> Unit,
    onSaveProfile: (username: String, bio: String, age: Long, gender: String, avatarUrl: String?, phone: String) -> Unit,
    onToggleSecurity: (pinSet: Boolean, discoverable: Boolean) -> Unit,
    onSubmitRechargeOrder: (pkg: RechargePackage, utr: String, method: String) -> Unit,
    onOpenAdminPanel: () -> Unit,
    onLogout: () -> Unit
) {
    val strings = LocalAddaStrings.current
    val context = LocalContext.current
    var showEditProfile by remember { mutableStateOf(false) }
    var selectedPkg by remember { mutableStateOf<RechargePackage?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // User Profile Hero
        if (me != null) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = RoyalPurpleSurface),
                    border = BorderStroke(1.dp, RoyalGold.copy(alpha = 0.55f)),
                    shape = RoundedCornerShape(22.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                UserAvatarWithFrame(me.avatarUrl, me.username, me.vipTier, size = 62.dp, verified = me.verified)
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(me.username, style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                                        VipBadge(me.vipTier)
                                    }
                                    Text("ADDA ID: ${me.addaCode}", style = MaterialTheme.typography.labelSmall, color = NeonCyan)
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        LevelBadge(me.level)
                                        Text("Send Lv.${me.giftSendLevel} • Recv Lv.${me.giftReceiveLevel}", style = MaterialTheme.typography.labelSmall, color = RoyalGold)
                                    }
                                }
                            }
                            OutlinedButton(onClick = { showEditProfile = true }, modifier = Modifier.testTag("edit_profile_button")) {
                                Icon(Icons.Filled.Edit, contentDescription = "Edit", modifier = Modifier.size(16.dp))
                            }
                        }
                        if (me.bio.isNotBlank()) {
                            Text(me.bio, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                        }
                        val progress = ((me.xp % 500L).toFloat() / 500f).coerceIn(0f, 1f)
                        Column {
                            Text("XP: ${me.xp} (Next Lv.${(me.level + 1).coerceAtMost(100)})", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            Spacer(Modifier.height(4.dp))
                            LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth(), color = RoyalGold)
                        }
                    }
                }
            }
        }

        // Digital Wallet Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = RoyalPurpleDeep),
                border = BorderStroke(1.dp, NeonCyan.copy(0.5f)),
                shape = RoundedCornerShape(22.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(strings.walletTitle, style = MaterialTheme.typography.titleLarge, color = RoyalGold)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Surface(
                            modifier = Modifier.weight(1f),
                            color = ObsidianSurface,
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, RoyalGold.copy(0.5f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(strings.coins, style = MaterialTheme.typography.labelLarge, color = TextSecondary)
                                Text("🪙 ${wallet?.coins ?: 0L}", style = MaterialTheme.typography.headlineSmall, color = RoyalGold, fontWeight = FontWeight.Bold)
                            }
                        }
                        Surface(
                            modifier = Modifier.weight(1f),
                            color = ObsidianSurface,
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, NeonCyan.copy(0.5f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(strings.diamonds, style = MaterialTheme.typography.labelLarge, color = TextSecondary)
                                Text("💎 ${wallet?.diamonds ?: 0L}", style = MaterialTheme.typography.headlineSmall, color = NeonCyan, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Text(strings.orderPendingNote, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                }
            }
        }

        // Indian Rupee Recharge Packages
        item {
            Text(strings.buyCoinsInr, style = MaterialTheme.typography.titleLarge, color = TextPrimary)
            Spacer(Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                GiftCatalogSeed.rechargePackages.chunked(2).forEach { rowPkgs ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowPkgs.forEach { pkg ->
                            val isMegaPack = pkg.coinsAmount >= 100_000L
                            Card(
                                modifier = Modifier.weight(1f),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isMegaPack) RoyalPurpleDeep else RoyalPurpleSurface
                                ),
                                border = BorderStroke(
                                    if (isMegaPack) 1.5.dp else 1.dp,
                                    if (isMegaPack) RoyalGold else RoyalGold.copy(0.4f)
                                ),
                                onClick = { selectedPkg = pkg }
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(if (lang == AppLang.HI) pkg.labelHi else pkg.label, style = MaterialTheme.typography.labelLarge, color = TextSecondary, maxLines = 1)
                                    Text("🪙 ${com.example.model.formatIndianCoins(pkg.coinsAmount)}", style = MaterialTheme.typography.titleMedium, color = RoyalGold, fontWeight = FontWeight.Bold)
                                    Text("₹${com.example.model.formatIndianCoins(pkg.amountInr)} (${pkg.bonusLabel})", style = MaterialTheme.typography.labelSmall, color = NeonCyan)
                                }
                            }
                        }
                        if (rowPkgs.size == 1) {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        // Settings & Language & Security & Admin
        item {
            Card(colors = CardDefaults.cardColors(containerColor = RoyalPurpleDeep)) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Filled.Translate, contentDescription = null, tint = NeonCyan)
                            Text(strings.languageLabel, color = TextPrimary)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedButton(onClick = { onToggleLanguage(AppLang.HI) }) { Text("हिन्दी", color = if (lang == AppLang.HI) RoyalGold else TextSecondary) }
                            OutlinedButton(onClick = { onToggleLanguage(AppLang.EN) }) { Text("English", color = if (lang == AppLang.EN) RoyalGold else TextSecondary) }
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Filled.VolumeUp, contentDescription = null, tint = NeonPink)
                            Text("Gift & Level Sound FX", color = TextPrimary)
                        }
                        Switch(checked = soundFxEnabled, onCheckedChange = onToggleSoundFx)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Filled.Security, contentDescription = null, tint = EmeraldLive)
                            Text("Account Security Lock", color = TextPrimary)
                        }
                        Switch(
                            checked = privateAccount?.securityPinSet ?: false,
                            onCheckedChange = { checked ->
                                onToggleSecurity(checked, privateAccount?.privacyDiscoverable ?: true)
                            }
                        )
                    }

                    Surface(
                        color = ObsidianSurface,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, NeonCyan.copy(0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(
                                text = "${strings.googleAccountLinked}: ${privateAccount?.email?.ifBlank { "Google Account" } ?: "Google Account"}",
                                style = MaterialTheme.typography.labelLarge,
                                color = TextPrimary
                            )
                            if (me != null) {
                                Text(
                                    text = "Google UID: ${me.userId.take(14)}...",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = NeonCyan
                                )
                            }
                            Text(
                                text = strings.googleSecurityBadge,
                                style = MaterialTheme.typography.labelSmall,
                                color = EmeraldLive
                            )
                        }
                    }

                    if (isAdmin) {
                        Button(
                            onClick = onOpenAdminPanel,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = RoyalGold, contentColor = ObsidianVoid)
                        ) {
                            Icon(Icons.Filled.AdminPanelSettings, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(strings.adminDashboard, fontWeight = FontWeight.Bold)
                        }
                    }

                    OutlinedButton(
                        onClick = onLogout,
                        modifier = Modifier.fillMaxWidth().testTag("logout_button"),
                        border = BorderStroke(1.dp, CrimsonRed)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = strings.logout, tint = CrimsonRed)
                        Spacer(Modifier.width(8.dp))
                        Text(strings.logout, color = CrimsonRed)
                    }
                }
            }
        }

        // Recharge History
        if (orders.isNotEmpty()) {
            item { Text(strings.rechargeHistory, style = MaterialTheme.typography.titleMedium, color = NeonCyan) }
            items(orders, key = { it.id }) { o ->
                Card(colors = CardDefaults.cardColors(containerColor = RoyalPurpleDeep)) {
                    Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("₹${o.amountInr} • ${o.coinsAmount} Coins", color = TextPrimary)
                            Text("UTR: ${o.utrReference}", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        }
                        Text(
                            o.status,
                            color = when (o.status) {
                                "VERIFIED" -> EmeraldLive
                                "REJECTED" -> CrimsonRed
                                else -> RoyalGold
                            },
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }

        // Transaction History
        if (transactions.isNotEmpty()) {
            item { Text(strings.transactionHistory, style = MaterialTheme.typography.titleMedium, color = RoyalGold) }
            items(transactions.take(25), key = { it.id }) { tx ->
                Card(colors = CardDefaults.cardColors(containerColor = ObsidianSurface)) {
                    Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(tx.note.ifBlank { tx.type }, color = TextPrimary, style = MaterialTheme.typography.bodyMedium)
                            Text("${tx.senderName} ➔ ${tx.receiverName}", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        }
                        Text("🪙 ${tx.amountCoins}", color = RoyalGold, style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }

    if (showEditProfile && me != null) {
        ModalBottomSheet(
            onDismissRequest = { showEditProfile = false },
            containerColor = ObsidianSurface
        ) {
            ProfileEditorSheetContent(
                profile = me,
                currentPhone = privateAccount?.phone.orEmpty(),
                onSave = onSaveProfile,
                onClose = { showEditProfile = false }
            )
        }
    }

    selectedPkg?.let { pkg ->
        var utrRef by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { selectedPkg = null },
            containerColor = ObsidianSurface,
            title = { Text("${strings.payViaUpi} • ₹${com.example.model.formatIndianCoins(pkg.amountInr)}", color = RoyalGold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Package: ${com.example.model.formatIndianCoins(pkg.coinsAmount)} Coins", color = NeonCyan)
                    OutlinedButton(
                        onClick = {
                            val upiUri = Uri.parse(
                                "upi://pay?pa=adda.payments@okicici&pn=ADDA%20India&tn=${Uri.encode(pkg.id)}&am=${pkg.amountInr}&cu=INR"
                            )
                            val intent = Intent(Intent.ACTION_VIEW, upiUri)
                            try {
                                context.startActivity(Intent.createChooser(intent, strings.payViaUpi))
                            } catch (_: Exception) {
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.Payment, contentDescription = null, tint = NeonCyan)
                        Spacer(Modifier.width(6.dp))
                        Text("Open UPI App (₹${pkg.amountInr})", color = NeonCyan)
                    }
                    OutlinedTextField(
                        value = utrRef,
                        onValueChange = { utrRef = it },
                        label = { Text(strings.utrHint) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(strings.orderPendingNote, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (utrRef.trim().length >= 4) {
                            onSubmitRechargeOrder(pkg, utrRef, "UPI")
                            selectedPkg = null
                        }
                    },
                    enabled = utrRef.trim().length >= 4
                ) {
                    Text(strings.submitOrder)
                }
            },
            dismissButton = { TextButton(onClick = { selectedPkg = null }) { Text(strings.cancel) } }
        )
    }
}
