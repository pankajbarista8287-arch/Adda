package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Castle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.DirectionsBoat
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.LocalFlorist
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.localization.HindiStrings
import com.example.localization.LocalAddaStrings
import com.example.model.GiftAnimationEvent
import com.example.model.GiftItem
import com.example.model.formatIndianCoins
import com.example.ui.theme.CrimsonRed
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonPink
import com.example.ui.theme.ObsidianVoid
import com.example.ui.theme.RoyalGold
import com.example.ui.theme.RoyalPurpleAccent
import com.example.ui.theme.RoyalPurpleDeep
import com.example.ui.theme.RoyalPurpleSurface
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

fun giftIconForKey(iconKey: String): ImageVector = when (iconKey) {
    "rose" -> Icons.Filled.LocalFlorist
    "heart" -> Icons.Filled.Favorite
    "teddy" -> Icons.Filled.Pets
    "peacock" -> Icons.Filled.AutoAwesome
    "couple", "romantic_couple" -> Icons.Filled.Favorite
    "ring", "kohinoor" -> Icons.Filled.Diamond
    "dhol" -> Icons.Filled.MusicNote
    "bike" -> Icons.Filled.DirectionsBike
    "car", "rolls_royce", "chariot" -> Icons.Filled.DirectionsCar
    "yacht" -> Icons.Filled.DirectionsBoat
    "jet" -> Icons.Filled.Flight
    "crown" -> Icons.Filled.WorkspacePremium
    "palace", "dream_palace" -> Icons.Filled.Castle
    "tilak", "royal_tilak" -> Icons.Filled.LocalFireDepartment
    "phoenix" -> Icons.Filled.Whatshot
    "dragon" -> Icons.Filled.Star
    "galaxy" -> Icons.Filled.Public
    else -> Icons.Filled.CardGiftcard
}

fun giftEmojiForKey(iconKey: String): String = when (iconKey) {
    "rose" -> "🌹"
    "heart" -> "💖"
    "teddy" -> "🧸"
    "peacock" -> "🦚"
    "couple" -> "💑"
    "romantic_couple" -> "💞"
    "ring" -> "💍"
    "dhol" -> "🥁"
    "bike" -> "🏍️"
    "car" -> "🏎️"
    "rolls_royce" -> "🚘"
    "yacht" -> "🛥️"
    "jet" -> "✈️"
    "crown" -> "👑"
    "kohinoor" -> "💎"
    "palace" -> "🏰"
    "dream_palace" -> "🕌"
    "tilak" -> "🪔"
    "royal_tilak" -> "🔱"
    "chariot" -> "⚜️"
    "phoenix" -> "🦅"
    "dragon" -> "🐉"
    "galaxy" -> "🌌"
    else -> "🎁"
}

private enum class GiftVisualStyle {
    TILAK_MANDALA,
    VEHICLE_FLYIN,
    DRAGON_INFERNO,
    PALACE_COSMOS,
    ROMANTIC_BLOOM
}

private data class GiftParticle(
    val id: Int,
    val angleRad: Float,
    val speedFactor: Float,
    val radiusDp: Float,
    val delayFraction: Float,
    val burstGroup: Int,
    val kind: Int, // 0 = firework spark, 1 = spinning gold coin, 2 = petal/confetti
    val colorIndex: Int,
    val spinFreq: Float,
    val driftX: Float
)

private fun styleForGift(iconKey: String): GiftVisualStyle = when (iconKey) {
    "tilak", "royal_tilak", "peacock", "dhol" -> GiftVisualStyle.TILAK_MANDALA
    "jet", "car", "rolls_royce", "chariot", "yacht", "bike" -> GiftVisualStyle.VEHICLE_FLYIN
    "phoenix", "dragon" -> GiftVisualStyle.DRAGON_INFERNO
    "palace", "dream_palace", "crown", "kohinoor", "galaxy", "ring" -> GiftVisualStyle.PALACE_COSMOS
    else -> GiftVisualStyle.ROMANTIC_BLOOM
}

private fun paletteForGift(gift: GiftItem): List<Color> = when (styleForGift(gift.iconKey)) {
    GiftVisualStyle.TILAK_MANDALA -> listOf(RoyalGold, Color(0xFFFF6D00), Color(0xFFFF1744), NeonPink)
    GiftVisualStyle.VEHICLE_FLYIN -> listOf(NeonCyan, RoyalGold, RoyalPurpleAccent, Color(0xFF00E676))
    GiftVisualStyle.DRAGON_INFERNO -> listOf(CrimsonRed, Color(0xFFFF9100), RoyalGold, NeonCyan)
    GiftVisualStyle.PALACE_COSMOS -> listOf(RoyalGold, Color(0xFFB388FF), NeonCyan, NeonPink)
    GiftVisualStyle.ROMANTIC_BLOOM -> listOf(NeonPink, RoyalGold, NeonCyan, Color(0xFFFF80AB))
}

private fun tierBadgeLabel(gift: GiftItem, totalCoins: Long, isHindi: Boolean): String? = when {
    totalCoins >= 500_000L -> if (isHindi) "सम्राट 5 लाख शोस्टॉपर" else "EMPEROR 5L SHOWSTOPPER"
    totalCoins >= 300_000L -> if (isHindi) "महाराजा 3 लाख मेगा शो" else "MAHARAJA 3L MEGA SHOW"
    totalCoins >= 200_000L -> if (isHindi) "रॉयल 2 लाख ग्रैंड एंट्री" else "ROYAL 2L GRAND ENTRY"
    totalCoins >= 100_000L -> if (isHindi) "शाही 1 लाख स्पेशल" else "SHAHI 1L SPECIAL"
    gift.animationType == "FULLSCREEN_ROYAL" -> if (isHindi) "रॉयल फुल-स्क्रीन" else "ROYAL CINEMA"
    gift.animationType == "ORBIT_3D" -> if (isHindi) "3D ऑर्बिट शो" else "3D ORBIT"
    else -> null
}

@Composable
fun GiftOverlayPlayer(
    event: GiftAnimationEvent?,
    onFinished: () -> Unit
) {
    if (event == null) return

    val lang = LocalAddaStrings.current
    val isHindi = lang.coins == HindiStrings.coins
    val haptic = LocalHapticFeedback.current

    val isFullScreen = event.gift.animationType == "FULLSCREEN_ROYAL"
    val isOrbit3d = event.gift.animationType == "ORBIT_3D"
    val totalCoins = event.gift.priceCoins * event.quantity
    val isMegaLakh = totalCoins >= 100_000L
    val style = remember(event.gift.iconKey) { styleForGift(event.gift.iconKey) }
    val palette = remember(event.gift.id, event.gift.iconKey) { paletteForGift(event.gift) }

    val durationMs = when {
        isMegaLakh -> 5000
        isFullScreen -> 4200
        isOrbit3d -> 2800
        else -> 2100
    }

    val progress = remember(event.timestampMillis) { Animatable(0f) }

    val particles = remember(event.timestampMillis) {
        val rng = Random(event.timestampMillis)
        val count = when {
            isMegaLakh -> 96
            isFullScreen -> 68
            isOrbit3d -> 36
            else -> 20
        }
        List(count) { i ->
            val group = when {
                i < count / 2 -> 0
                i < (count * 3) / 4 -> 1
                else -> 2
            }
            val kind = when {
                i % 5 == 0 -> 1 // spinning gold coin
                i % 3 == 0 -> 2 // floating petal / confetti
                else -> 0       // firework spark
            }
            GiftParticle(
                id = i,
                angleRad = (rng.nextFloat() * 2f * PI.toFloat()),
                speedFactor = 0.45f + rng.nextFloat() * 0.95f,
                radiusDp = 2.8f + rng.nextFloat() * 5.4f,
                delayFraction = when (group) {
                    0 -> 0.04f + rng.nextFloat() * 0.06f
                    1 -> 0.22f + rng.nextFloat() * 0.08f
                    else -> 0.44f + rng.nextFloat() * 0.08f
                },
                burstGroup = group,
                kind = kind,
                colorIndex = i % 4,
                spinFreq = 4f + rng.nextFloat() * 8f,
                driftX = (rng.nextFloat() - 0.5f) * 2f
            )
        }
    }

    val infinite = rememberInfiniteTransition(label = "gift_loop")
    val continuousRot by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(2400, easing = LinearEasing)),
        label = "rot"
    )
    val pulse by infinite.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.09f,
        animationSpec = infiniteRepeatable(tween(520), RepeatMode.Reverse),
        label = "pulse"
    )

    LaunchedEffect(event.timestampMillis) {
        if (isFullScreen || isMegaLakh) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
        progress.snapTo(0f)
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = durationMs, easing = LinearEasing)
        )
        onFinished()
    }

    val p = progress.value
    // Camera impact shake during opening burst on high-tier gifts
    val shakeIntensity = if ((isFullScreen || isMegaLakh) && p in 0.05f..0.24f) {
        val decay = 1f - ((p - 0.05f) / 0.19f)
        val amp = if (isMegaLakh) 18f else 10f
        sin(p * 180f) * amp * decay
    } else 0f

    val entryScale = when {
        p < 0.12f -> 0.35f + (p / 0.12f) * 0.80f
        p < 0.20f -> 1.15f - ((p - 0.12f) / 0.08f) * 0.15f
        p > 0.90f -> 1f - ((p - 0.90f) / 0.10f) * 0.35f
        else -> 1f
    }
    val overlayAlpha = when {
        p < 0.07f -> p / 0.07f
        p > 0.88f -> ((1f - p) / 0.12f).coerceIn(0f, 1f)
        else -> 1f
    }

    val coinCountFraction = (p / 0.48f).coerceIn(0f, 1f)
    val displayCoins = formatIndianCoins((totalCoins * coinCountFraction).toLong().coerceAtLeast(1L))
    val tierText = tierBadgeLabel(event.gift, totalCoins, isHindi)

    AnimatedVisibility(
        visible = true,
        enter = fadeIn(tween(150)) + scaleIn(initialScale = 0.78f),
        exit = fadeOut(tween(180)) + scaleOut()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    alpha = overlayAlpha
                    translationX = shakeIntensity
                    translationY = shakeIntensity * 0.55f
                }
                .background(
                    if (isFullScreen) {
                        Brush.radialGradient(
                            colors = listOf(
                                palette[0].copy(alpha = 0.22f),
                                ObsidianVoid.copy(alpha = if (isMegaLakh) 0.84f else 0.72f),
                                Color.Black.copy(alpha = if (isMegaLakh) 0.90f else 0.78f)
                            )
                        )
                    } else {
                        Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent))
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            // Full Physics Canvas Layer: Spotlight Rays, Shockwaves, Style Centerpiece, Fireworks, Coins & Petals
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cx = size.width / 2f
                val cy = size.height * if (isFullScreen) 0.46f else 0.48f
                val baseRadius = size.minDimension * if (isMegaLakh) 0.44f else 0.36f

                if (isFullScreen) {
                    drawStageSpotlightAndShockwaves(
                        cx = cx,
                        cy = cy,
                        p = p,
                        continuousRot = continuousRot,
                        baseRadius = baseRadius,
                        palette = palette,
                        isMega = isMegaLakh
                    )
                    drawStyleSpecificCinema(
                        style = style,
                        cx = cx,
                        cy = cy,
                        p = p,
                        continuousRot = continuousRot,
                        baseRadius = baseRadius,
                        palette = palette
                    )
                } else if (isOrbit3d) {
                    drawOrbitRings(cx = cx, cy = cy, continuousRot = continuousRot, baseRadius = baseRadius, palette = palette)
                }

                drawPhysicsParticles(
                    particles = particles,
                    p = p,
                    cx = cx,
                    cy = cy,
                    palette = palette,
                    isFullScreen = isFullScreen,
                    isMega = isMegaLakh
                )
            }

            // Vehicle fly-in horizontal sweep or floating bounce for center hero card
            val vehicleSweepX = if (style == GiftVisualStyle.VEHICLE_FLYIN && isFullScreen) {
                when {
                    p < 0.16f -> -460f * (1f - (p / 0.16f))
                    p > 0.85f -> 460f * ((p - 0.85f) / 0.15f)
                    else -> 0f
                }
            } else 0f

            val floatY = if (!isFullScreen && !isOrbit3d) {
                -110f * p + sin(p * 14f) * 14f
            } else 0f

            // Centerpiece Hero Stage Card
            Column(
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .graphicsLayer {
                        translationX = vehicleSweepX
                        translationY = floatY
                        scaleX = entryScale * if (isFullScreen) pulse else 1f
                        scaleY = entryScale * if (isFullScreen) pulse else 1f
                        if (isOrbit3d) {
                            rotationY = sin(Math.toRadians((continuousRot * 1.4).toDouble())).toFloat() * 26f
                            cameraDistance = 14f * density
                        } else if (isFullScreen && style == GiftVisualStyle.PALACE_COSMOS) {
                            rotationY = sin(Math.toRadians(continuousRot.toDouble())).toFloat() * 12f
                            cameraDistance = 16f * density
                        }
                    },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (tierText != null) {
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = palette[0].copy(alpha = 0.24f),
                        border = BorderStroke(1.5.dp, Brush.horizontalGradient(palette))
                    ) {
                        Text(
                            text = "✦ $tierText ✦",
                            style = MaterialTheme.typography.labelLarge,
                            color = RoyalGold,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                }

                Card(
                    modifier = Modifier.fillMaxWidth(if (isFullScreen) 0.92f else 0.78f),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = RoyalPurpleSurface.copy(alpha = if (isFullScreen) 0.92f else 0.94f)
                    ),
                    border = BorderStroke(
                        width = if (isMegaLakh) 3.dp else 2.dp,
                        brush = Brush.linearGradient(palette)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = if (isFullScreen) 22.dp else 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Glowing Emblem + Emoji + Vector Layered Badge
                        Box(
                            modifier = Modifier
                                .size(if (isMegaLakh) 112.dp else if (isFullScreen) 92.dp else 68.dp)
                                .background(
                                    Brush.radialGradient(
                                        listOf(
                                            palette[0].copy(alpha = 0.42f),
                                            palette[1].copy(alpha = 0.16f),
                                            Color.Transparent
                                        )
                                    ),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = giftEmojiForKey(event.gift.iconKey),
                                fontSize = if (isMegaLakh) 54.sp else if (isFullScreen) 44.sp else 32.sp
                            )
                            Icon(
                                imageVector = giftIconForKey(event.gift.iconKey),
                                contentDescription = event.gift.name,
                                tint = palette[0].copy(alpha = 0.35f),
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .size(if (isFullScreen) 30.dp else 22.dp)
                            )
                        }

                        Spacer(Modifier.height(8.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = if (isHindi) event.gift.nameHi else event.gift.name,
                                style = if (isFullScreen) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleLarge,
                                color = RoyalGold,
                                fontWeight = FontWeight.ExtraBold,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(999.dp),
                                color = NeonPink,
                                modifier = Modifier.scale(
                                    if (p < 0.25f) 1.25f else 1f
                                )
                            ) {
                                Text(
                                    text = "x${event.quantity}",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = Color.White,
                                    fontWeight = FontWeight.Black,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Spacer(Modifier.height(6.dp))

                        Text(
                            text = "${event.senderName}  ➔  ${event.receiverName}",
                            style = MaterialTheme.typography.titleMedium,
                            color = NeonCyan,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(Modifier.height(8.dp))

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = RoyalPurpleDeep.copy(alpha = 0.9f),
                            border = BorderStroke(1.dp, RoyalGold.copy(alpha = 0.65f))
                        ) {
                            Text(
                                text = "🪙 $displayCoins ${lang.coins}",
                                style = MaterialTheme.typography.titleMedium,
                                color = RoyalGold,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            // Skip button (48dp accessible touch target) for mega fullscreen gifts
            if (isFullScreen) {
                TextButton(
                    onClick = onFinished,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .statusBarsPadding()
                        .padding(12.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = ObsidianVoid.copy(alpha = 0.72f),
                        border = BorderStroke(1.dp, TextSecondary.copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.Close, contentDescription = "Skip", tint = TextPrimary, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(if (isHindi) "बंद करें" else "Skip", color = TextPrimary, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }
    }
}

private fun DrawScope.drawStageSpotlightAndShockwaves(
    cx: Float,
    cy: Float,
    p: Float,
    continuousRot: Float,
    baseRadius: Float,
    palette: List<Color>,
    isMega: Boolean
) {
    // 1. Rotating Radial Spotlight Beams
    val rayCount = if (isMega) 12 else 8
    rotate(degrees = continuousRot * 0.45f, pivot = Offset(cx, cy)) {
        for (i in 0 until rayCount) {
            val angleDeg = i * (360f / rayCount)
            val a1 = Math.toRadians((angleDeg - 6f).toDouble())
            val a2 = Math.toRadians((angleDeg + 6f).toDouble())
            val len = size.maxDimension * 0.75f
            val path = Path().apply {
                moveTo(cx, cy)
                lineTo(cx + (len * cos(a1)).toFloat(), cy + (len * sin(a1)).toFloat())
                lineTo(cx + (len * cos(a2)).toFloat(), cy + (len * sin(a2)).toFloat())
                close()
            }
            drawPath(
                path = path,
                brush = Brush.radialGradient(
                    colors = listOf(palette[i % palette.size].copy(alpha = 0.24f), Color.Transparent),
                    center = Offset(cx, cy),
                    radius = len
                )
            )
        }
    }

    // 2. Concentric Shockwave Rings on Arrival
    listOf(0f, 0.14f, 0.30f).forEachIndexed { idx, start ->
        val local = ((p - start) / 0.42f).coerceIn(0f, 1f)
        if (local > 0f && local < 1f) {
            val radius = size.maxDimension * 0.65f * local
            drawCircle(
                color = palette[idx % palette.size].copy(alpha = (1f - local) * 0.65f),
                radius = radius,
                center = Offset(cx, cy),
                style = Stroke(width = (8f * (1f - local)).dp.toPx())
            )
        }
    }

    // 3. Ambient Core Aura
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(palette[0].copy(alpha = 0.35f), palette[1].copy(alpha = 0.14f), Color.Transparent),
            center = Offset(cx, cy),
            radius = baseRadius * 1.4f
        ),
        radius = baseRadius * 1.4f,
        center = Offset(cx, cy)
    )
}

private fun DrawScope.drawStyleSpecificCinema(
    style: GiftVisualStyle,
    cx: Float,
    cy: Float,
    p: Float,
    continuousRot: Float,
    baseRadius: Float,
    palette: List<Color>
) {
    when (style) {
        GiftVisualStyle.TILAK_MANDALA -> {
            // Sacred 16-petal rotating mandala halo
            rotate(degrees = continuousRot * 0.8f, pivot = Offset(cx, cy)) {
                val petals = 16
                for (i in 0 until petals) {
                    val rad = Math.toRadians((i * (360.0 / petals)))
                    val px = cx + (baseRadius * 0.85f * cos(rad)).toFloat()
                    val py = cy + (baseRadius * 0.85f * sin(rad)).toFloat()
                    drawLine(
                        brush = Brush.linearGradient(
                            colors = listOf(RoyalGold.copy(alpha = 0.85f), palette[1].copy(alpha = 0.15f)),
                            start = Offset(cx, cy),
                            end = Offset(px, py)
                        ),
                        start = Offset(cx, cy),
                        end = Offset(px, py),
                        strokeWidth = 4.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                    drawCircle(
                        color = if (i % 2 == 0) RoyalGold else palette[2],
                        radius = 6.dp.toPx(),
                        center = Offset(px, py)
                    )
                }
            }
            drawCircle(
                color = RoyalGold.copy(alpha = 0.6f),
                radius = baseRadius * 0.72f,
                center = Offset(cx, cy),
                style = Stroke(width = 3.dp.toPx())
            )
        }

        GiftVisualStyle.VEHICLE_FLYIN -> {
            // Supersonic neon speed trails sweeping left-to-right
            for (lineIdx in 0 until 8) {
                val yOffset = cy + (lineIdx - 3.5f) * 38.dp.toPx()
                val streakStart = ((p * 2.4f + lineIdx * 0.13f) % 1.4f - 0.2f) * size.width
                val streakLen = size.width * (0.25f + (lineIdx % 3) * 0.1f)
                drawLine(
                    brush = Brush.horizontalGradient(
                        colors = listOf(Color.Transparent, palette[lineIdx % palette.size].copy(alpha = 0.85f), Color.White)
                    ),
                    start = Offset(streakStart - streakLen, yOffset),
                    end = Offset(streakStart, yOffset),
                    strokeWidth = (2.5f + (lineIdx % 2) * 2f).dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }

        GiftVisualStyle.DRAGON_INFERNO -> {
            // Twin sweeping fiery energy arcs & ember vortex
            for (ring in 0..2) {
                val r = baseRadius * (0.65f + ring * 0.22f)
                val sweepStart = (continuousRot * (if (ring % 2 == 0) 1.5f else -1.2f)) % 360f
                drawArc(
                    brush = Brush.sweepGradient(palette + palette.first()),
                    startAngle = sweepStart,
                    sweepAngle = 210f,
                    useCenter = false,
                    topLeft = Offset(cx - r, cy - r),
                    size = Size(r * 2f, r * 2f),
                    style = Stroke(width = (6f - ring).dp.toPx(), cap = StrokeCap.Round)
                )
            }
        }

        GiftVisualStyle.PALACE_COSMOS -> {
            // Vertical Aurora Light Pillars rising from bottom + orbiting star ring
            for (pillar in -2..2) {
                val px = cx + pillar * (size.width * 0.16f)
                val rise = (p * 1.8f).coerceIn(0f, 1f)
                drawLine(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.Transparent, palette[(pillar + 2) % palette.size].copy(alpha = 0.45f), RoyalGold.copy(alpha = 0.18f))
                    ),
                    start = Offset(px, size.height),
                    end = Offset(px, size.height * (1f - 0.88f * rise)),
                    strokeWidth = 18.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
            drawOrbitRings(cx, cy, continuousRot, baseRadius, palette)
        }

        GiftVisualStyle.ROMANTIC_BLOOM -> {
            drawOrbitRings(cx, cy, continuousRot, baseRadius, palette)
        }
    }
}

private fun DrawScope.drawOrbitRings(
    cx: Float,
    cy: Float,
    continuousRot: Float,
    baseRadius: Float,
    palette: List<Color>
) {
    val count = 16
    for (i in 0 until count) {
        val angle = Math.toRadians((i * (360.0 / count) + continuousRot).toDouble())
        val px = cx + (baseRadius * cos(angle)).toFloat()
        val py = cy + (baseRadius * 0.62f * sin(angle)).toFloat()
        drawCircle(
            color = palette[i % palette.size].copy(alpha = 0.85f),
            radius = if (i % 2 == 0) 6.dp.toPx() else 4.dp.toPx(),
            center = Offset(px, py)
        )
    }
}

private fun DrawScope.drawPhysicsParticles(
    particles: List<GiftParticle>,
    p: Float,
    cx: Float,
    cy: Float,
    palette: List<Color>,
    isFullScreen: Boolean,
    isMega: Boolean
) {
    val maxTravel = size.minDimension * if (isMega) 0.72f else if (isFullScreen) 0.56f else 0.34f
    val gravity = size.height * 0.28f

    particles.forEach { pt ->
        val life = ((p - pt.delayFraction) / (1f - pt.delayFraction).coerceAtLeast(0.25f)).coerceIn(0f, 1f)
        if (life <= 0f || life >= 1f) return@forEach

        val originX = when (pt.burstGroup) {
            1 -> size.width * 0.22f
            2 -> size.width * 0.78f
            else -> cx
        }
        val originY = when (pt.burstGroup) {
            1 -> size.height * 0.28f
            2 -> size.height * 0.32f
            else -> cy
        }

        val alpha = if (life > 0.65f) ((1f - life) / 0.35f).coerceIn(0f, 1f) else (life / 0.12f).coerceIn(0f, 1f)
        val color = palette[pt.colorIndex % palette.size].copy(alpha = alpha)

        when (pt.kind) {
            1 -> {
                // Spinning 3D Gold Coin Fountain / Rain
                val dist = maxTravel * pt.speedFactor * life
                val px = originX + (dist * 0.7f * cos(pt.angleRad)) + pt.driftX * 30f * sin(life * 8f)
                val py = originY - (dist * 0.55f * kotlin.math.abs(sin(pt.angleRad))) + gravity * life * life
                val spinWidth = (kotlin.math.abs(cos(life * pt.spinFreq * PI)).toFloat()).coerceAtLeast(0.18f)
                val r = (pt.radiusDp + 3.5f).dp.toPx()
                drawOval(
                    color = RoyalGold.copy(alpha = alpha),
                    topLeft = Offset(px - r * spinWidth, py - r),
                    size = Size(r * 2f * spinWidth, r * 2f)
                )
                drawOval(
                    color = Color.White.copy(alpha = alpha * 0.75f),
                    topLeft = Offset(px - r * spinWidth * 0.45f, py - r * 0.45f),
                    size = Size(r * 0.9f * spinWidth, r * 0.9f)
                )
            }
            2 -> {
                // Fluttering Petal / Neon Confetti Ribbon falling with sine sway
                val startX = cx + pt.driftX * size.width * 0.45f
                val startY = size.height * 0.06f + (pt.delayFraction * size.height * 0.25f)
                val px = startX + sin(life * pt.spinFreq) * 32.dp.toPx()
                val py = startY + size.height * 0.78f * life * pt.speedFactor
                val rw = (pt.radiusDp + 2f).dp.toPx()
                drawOval(
                    color = color,
                    topLeft = Offset(px - rw, py - rw * 0.55f),
                    size = Size(rw * 2f, rw * 1.1f)
                )
            }
            else -> {
                // Firework Spark with glowing velocity tail
                val dist = maxTravel * pt.speedFactor * life
                val tailDist = (dist - 22.dp.toPx()).coerceAtLeast(0f)
                val px = originX + (dist * cos(pt.angleRad))
                val py = originY + (dist * sin(pt.angleRad)) + (gravity * 0.5f * life * life)
                val tx = originX + (tailDist * cos(pt.angleRad))
                val ty = originY + (tailDist * sin(pt.angleRad)) + (gravity * 0.5f * life * life)
                drawLine(
                    color = color.copy(alpha = alpha * 0.65f),
                    start = Offset(tx, ty),
                    end = Offset(px, py),
                    strokeWidth = (pt.radiusDp * 0.7f).dp.toPx(),
                    cap = StrokeCap.Round
                )
                drawCircle(
                    color = Color.White.copy(alpha = alpha * 0.9f),
                    radius = (pt.radiusDp * 0.65f).dp.toPx(),
                    center = Offset(px, py)
                )
            }
        }
    }
}
