package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Castle
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.LocalFlorist
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.localization.AppLang
import com.example.localization.LocalAddaStrings
import com.example.model.GiftAnimationEvent
import com.example.model.GiftItem
import com.example.ui.theme.AmberGold
import com.example.ui.theme.CrimsonRed
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonPink
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianVoid
import com.example.ui.theme.RoyalGold
import com.example.ui.theme.RoyalPurpleAccent
import com.example.ui.theme.RoyalPurpleDeep
import com.example.ui.theme.RoyalPurpleGlow
import com.example.ui.theme.RoyalPurpleSurface
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun AddaCosmicBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val transition = rememberInfiniteTransition(label = "bg_glow")
    val pulse by transition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(4200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orb_pulse"
    )
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(ObsidianVoid, RoyalPurpleDeep, ObsidianVoid)
                )
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(RoyalPurpleAccent.copy(alpha = 0.25f), Color.Transparent),
                    center = Offset(size.width * 0.18f, size.height * 0.14f),
                    radius = size.width * 0.65f * pulse
                ),
                center = Offset(size.width * 0.18f, size.height * 0.14f),
                radius = size.width * 0.65f * pulse
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(NeonPink.copy(alpha = 0.14f), Color.Transparent),
                    center = Offset(size.width * 0.85f, size.height * 0.32f),
                    radius = size.width * 0.55f * pulse
                ),
                center = Offset(size.width * 0.85f, size.height * 0.32f),
                radius = size.width * 0.55f * pulse
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(NeonCyan.copy(alpha = 0.10f), Color.Transparent),
                    center = Offset(size.width * 0.5f, size.height * 0.88f),
                    radius = size.width * 0.7f
                ),
                center = Offset(size.width * 0.5f, size.height * 0.88f),
                radius = size.width * 0.7f
            )
        }
        content()
    }
}

@Composable
fun AddaBrandLogo(
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    showWordmark: Boolean = true
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .clip(RoundedCornerShape(14.dp))
                .background(
                    Brush.linearGradient(listOf(RoyalPurpleSurface, ObsidianSurface))
                )
                .border(
                    width = 1.5.dp,
                    brush = Brush.linearGradient(listOf(RoyalGold, NeonCyan, NeonPink)),
                    shape = RoundedCornerShape(14.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize().padding(7.dp)) {
                val w = this.size.width
                val h = this.size.height
                // Crown silhouette
                val crown = Path().apply {
                    moveTo(w * 0.18f, h * 0.45f)
                    lineTo(w * 0.12f, h * 0.18f)
                    lineTo(w * 0.36f, h * 0.32f)
                    lineTo(w * 0.50f, h * 0.10f)
                    lineTo(w * 0.64f, h * 0.32f)
                    lineTo(w * 0.88f, h * 0.18f)
                    lineTo(w * 0.82f, h * 0.45f)
                    close()
                }
                drawPath(
                    path = crown,
                    brush = Brush.horizontalGradient(listOf(RoyalGold, AmberGold))
                )
                // Neon voice equalizer bars
                val barHeights = listOf(0.28f, 0.42f, 0.35f, 0.44f, 0.28f)
                barHeights.forEachIndexed { idx, ratio ->
                    val x = w * (0.20f + idx * 0.15f)
                    val barH = h * ratio * 0.55f
                    drawLine(
                        brush = Brush.verticalGradient(listOf(NeonCyan, NeonPink)),
                        start = Offset(x, h * 0.90f),
                        end = Offset(x, h * 0.90f - barH),
                        strokeWidth = w * 0.08f
                    )
                }
            }
        }
        if (showWordmark) {
            Column {
                Text(
                    text = "ADDA",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = RoyalGold,
                    letterSpacing = 2.sp
                )
                Text(
                    text = "VOICE • GAMING",
                    style = MaterialTheme.typography.labelSmall,
                    color = NeonCyan
                )
            }
        }
    }
}

@Composable
fun AddaSplashOverlay(onFinished: () -> Unit) {
    val strings = LocalAddaStrings.current
    var visible by remember { mutableStateOf(true) }
    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 1.15f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "splash_scale"
    )
    LaunchedEffect(Unit) {
        delay(1750)
        visible = false
        delay(250)
        onFinished()
    }
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(tween(300))
    ) {
        AddaCosmicBackground {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp)
                    .scale(scale),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_royal_crest),
                    contentDescription = "ADDA Royal Emblem",
                    modifier = Modifier
                        .size(150.dp)
                        .clip(CircleShape)
                        .border(2.dp, Brush.linearGradient(listOf(RoyalGold, NeonCyan, NeonPink)), CircleShape),
                    contentScale = ContentScale.Crop
                )
                Spacer(Modifier.height(24.dp))
                Text(
                    text = "ADDA",
                    style = MaterialTheme.typography.displaySmall.copy(fontSize = 46.sp),
                    color = RoyalGold,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 6.sp
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = strings.tagline,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = strings.splashSubtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = NeonCyan,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun VipBadge(vipTier: String, modifier: Modifier = Modifier) {
    if (vipTier != "VIP" && vipTier != "SVIP") return
    val isSvip = vipTier == "SVIP"
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = if (isSvip) NeonPink.copy(alpha = 0.22f) else RoyalGold.copy(alpha = 0.20f),
        border = BorderStroke(1.dp, if (isSvip) NeonPink else RoyalGold)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.WorkspacePremium,
                contentDescription = vipTier,
                tint = if (isSvip) NeonPink else RoyalGold,
                modifier = Modifier.size(12.dp)
            )
            Text(
                text = vipTier,
                style = MaterialTheme.typography.labelSmall,
                color = if (isSvip) NeonPink else RoyalGold,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun LevelBadge(level: Long, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = RoyalPurpleAccent.copy(alpha = 0.30f),
        border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.7f))
    ) {
        Text(
            text = "Lv.$level",
            style = MaterialTheme.typography.labelSmall,
            color = NeonCyan,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
fun UserAvatarWithFrame(
    avatarUrl: String,
    username: String,
    vipTier: String = "NONE",
    speakingLevel: Int = 0,
    size: Dp = 56.dp,
    verified: Boolean = false
) {
    val frameBrush = when (vipTier) {
        "SVIP" -> Brush.linearGradient(listOf(NeonPink, RoyalGold, NeonCyan))
        "VIP" -> Brush.linearGradient(listOf(RoyalGold, AmberGold))
        else -> Brush.linearGradient(listOf(RoyalPurpleGlow, NeonCyan))
    }
    Box(contentAlignment = Alignment.Center) {
        if (speakingLevel > 10) {
            Box(
                modifier = Modifier
                    .size(size + 10.dp)
                    .clip(CircleShape)
                    .border(2.dp, NeonCyan.copy(alpha = (speakingLevel / 100f).coerceIn(0.3f, 1f)), CircleShape)
            )
        }
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(RoyalPurpleSurface)
                .border(2.5.dp, frameBrush, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (avatarUrl.isNotBlank()) {
                AsyncImage(
                    model = avatarUrl,
                    contentDescription = username,
                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Text(
                    text = username.take(2).uppercase().ifBlank { "AD" },
                    style = MaterialTheme.typography.titleMedium,
                    color = RoyalGold,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        if (verified) {
            Icon(
                imageVector = Icons.Filled.Verified,
                contentDescription = "Verified",
                tint = NeonCyan,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(16.dp)
                    .background(ObsidianVoid, CircleShape)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GiftPickerSheet(
    gifts: List<GiftItem>,
    coinsBalance: Long,
    lang: AppLang,
    receiverLabel: String,
    onDismiss: () -> Unit,
    onSendGift: (GiftItem, Int) -> Unit,
    onPreviewGift: ((GiftItem) -> Unit)? = null
) {
    val strings = LocalAddaStrings.current
    val isHindi = lang == AppLang.HI
    val categories = listOf("ALL", "MEGA 1L-5L", "ROYAL", "VIP", "LUXURY", "SPECIAL", "FESTIVAL", "PREMIUM", "COUPLE", "LOVE")
    var selectedCategory by remember { mutableStateOf("ALL") }
    var selectedQty by remember { mutableIntStateOf(1) }
    var previewMode by remember { mutableStateOf(false) }

    val filtered = remember(gifts, selectedCategory) {
        val base = when (selectedCategory) {
            "ALL" -> gifts
            "MEGA 1L-5L" -> gifts.filter { it.priceCoins >= 100_000L }
            else -> gifts.filter { it.category == selectedCategory }
        }
        base.sortedBy { it.priceCoins }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = ObsidianSurface,
        contentColor = TextPrimary
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(strings.sendGift, style = MaterialTheme.typography.titleLarge, color = RoyalGold)
                    Text("➔ $receiverLabel", style = MaterialTheme.typography.bodyMedium, color = NeonCyan, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                if (onPreviewGift != null) {
                    FilterChip(
                        selected = previewMode,
                        onClick = { previewMode = !previewMode },
                        label = {
                            Text(
                                if (isHindi) (if (previewMode) "▶ प्रीव्यू चालू" else "▶ प्रीव्यू देखें")
                                else (if (previewMode) "▶ Preview ON" else "▶ Preview"),
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = NeonPink.copy(alpha = 0.25f),
                            selectedLabelColor = NeonPink
                        )
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = RoyalPurpleDeep,
                    border = BorderStroke(1.dp, RoyalGold.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = "🪙 ${com.example.model.formatIndianCoins(coinsBalance)}",
                        style = MaterialTheme.typography.titleMedium,
                        color = RoyalGold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
            if (previewMode) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = if (isHindi) "प्रीव्यू मोड चालू है: किसी भी 2 लाख / 3 लाख गिफ्ट पर टैप करके एनिमेशन देखें (कॉइन्स नहीं कटेंगे)।"
                    else "Preview Mode ON: Tap any 2L / 3L gift to test its full-screen animation free!",
                    style = MaterialTheme.typography.labelSmall,
                    color = NeonPink
                )
            }
            Spacer(Modifier.height(10.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(categories) { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = cat },
                        label = { Text(cat, style = MaterialTheme.typography.labelSmall) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = RoyalPurpleAccent,
                            selectedLabelColor = TextPrimary
                        )
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(1, 5, 10, 99).forEach { qty ->
                    FilterChip(
                        selected = selectedQty == qty,
                        onClick = { selectedQty = qty },
                        label = { Text("x$qty") }
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.height(310.dp),
                contentPadding = PaddingValues(bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filtered, key = { it.id }) { gift ->
                    val isMega = gift.priceCoins >= 100_000L
                    val isRoyal = gift.animationType == "FULLSCREEN_ROYAL"
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (previewMode && onPreviewGift != null) {
                                    onDismiss()
                                    onPreviewGift(gift)
                                } else {
                                    onSendGift(gift, selectedQty)
                                    onDismiss()
                                }
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isMega) Color(0xFF2D1446) else RoyalPurpleDeep
                        ),
                        border = BorderStroke(
                            if (isMega) 1.5.dp else 1.dp,
                            when {
                                isMega -> RoyalGold
                                isRoyal -> NeonPink.copy(alpha = 0.75f)
                                else -> RoyalPurpleAccent.copy(alpha = 0.5f)
                            }
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(8.dp).fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = when {
                                        gift.priceCoins >= 500_000L -> "5L"
                                        gift.priceCoins >= 300_000L -> "3L"
                                        gift.priceCoins >= 200_000L -> "2L"
                                        gift.priceCoins >= 100_000L -> "1L"
                                        isRoyal -> "🔥"
                                        gift.animationType == "ORBIT_3D" -> "3D"
                                        else -> "✨"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isMega) RoyalGold else NeonCyan,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                if (previewMode) {
                                    Text("▶", style = MaterialTheme.typography.labelSmall, color = NeonPink)
                                }
                            }
                            Text(
                                text = giftEmojiForKey(gift.iconKey),
                                fontSize = 28.sp
                            )
                            Spacer(Modifier.height(3.dp))
                            Text(
                                text = if (isHindi) gift.nameHi else gift.name,
                                style = MaterialTheme.typography.labelMedium,
                                color = TextPrimary,
                                fontWeight = if (isMega) FontWeight.Bold else FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "🪙 ${com.example.model.formatIndianCoins(gift.priceCoins * selectedQty)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = RoyalGold,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}

