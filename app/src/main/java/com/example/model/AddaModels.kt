package com.example.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@Keep
@IgnoreExtraProperties
data class AdminRegistry(
    @DocumentId val id: String = "",
    val adminUid: String = "",
    val adminCode: String = "",
    val email: String = "",
    val role: String = "SUPER_ADMIN",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val addaCode: String = "",
    val username: String = "",
    val bio: String = "",
    val age: Long = 21L,
    val gender: String = "OTHER",
    val avatarUrl: String = "",
    val avatarPreset: String = "ROYAL_LOTUS",
    val level: Long = 1L,
    val xp: Long = 0L,
    val giftSendLevel: Long = 1L,
    val giftReceiveLevel: Long = 1L,
    val vipTier: String = "NONE",
    val frameStyle: String = "DEFAULT",
    val nameColorHex: String = "#F8F6FF",
    val familyId: String = "",
    val agencyId: String = "",
    val hostStatus: String = "NONE",
    val verified: Boolean = false,
    val banned: Boolean = false,
    val online: Boolean = true,
    val followersCount: Long = 0L,
    val followingCount: Long = 0L,
    val friendsCount: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class UserPrivateAccount(
    @DocumentId val id: String = "",
    val userId: String = "",
    val email: String = "",
    val phone: String = "",
    val preferredLang: String = "hi",
    val securityPinSet: Boolean = false,
    val twoFactorEnabled: Boolean = false,
    val privacyDiscoverable: Boolean = true,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class UserNotification(
    @DocumentId val id: String = "",
    val userId: String = "",
    val type: String = "SYSTEM",
    val title: String = "",
    val body: String = "",
    val read: Boolean = false,
    val targetId: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class UserMission(
    @DocumentId val id: String = "",
    val userId: String = "",
    val missionKey: String = "",
    val title: String = "",
    val progress: Long = 0L,
    val target: Long = 1L,
    val rewardCoins: Long = 0L,
    val rewardXp: Long = 0L,
    val completed: Boolean = false,
    val claimed: Boolean = false,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Wallet(
    @DocumentId val id: String = "",
    val userId: String = "",
    val coins: Long = 100L,
    val diamonds: Long = 0L,
    val totalSpentCoins: Long = 0L,
    val totalEarnedDiamonds: Long = 0L,
    val totalRechargedInr: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class WalletTransaction(
    @DocumentId val id: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val receiverId: String = "",
    val receiverName: String = "",
    val type: String = "GIFT_SEND",
    val amountCoins: Long = 0L,
    val amountDiamonds: Long = 0L,
    val giftId: String = "",
    val giftName: String = "",
    val quantity: Long = 1L,
    val contextId: String = "",
    val note: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class RechargeOrder(
    @DocumentId val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val packageId: String = "",
    val packageLabel: String = "",
    val amountInr: Long = 0L,
    val coinsAmount: Long = 0L,
    val paymentMethod: String = "UPI",
    val utrReference: String = "",
    val status: String = "PENDING",
    val verifiedBy: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class GiftItem(
    @DocumentId val id: String = "",
    val name: String = "",
    val nameHi: String = "",
    val category: String = "LOVE",
    val priceCoins: Long = 10L,
    val diamondValue: Long = 5L,
    val animationType: String = "FLOAT_2D",
    val iconKey: String = "rose",
    val active: Boolean = true,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class VoiceRoom(
    @DocumentId val id: String = "",
    val title: String = "",
    val topic: String = "",
    val ownerId: String = "",
    val ownerName: String = "",
    val roomType: String = "PUBLIC",
    val roomPin: String = "",
    val seatCount: Long = 8L,
    val themeId: String = "ROYAL_STAGE",
    val announcement: String = "",
    val level: Long = 1L,
    val xp: Long = 0L,
    val onlineCount: Long = 1L,
    val totalGiftCoins: Long = 0L,
    val pkActive: Boolean = false,
    val moderatorUids: List<String> = emptyList(),
    val active: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class RoomSeat(
    @DocumentId val id: String = "",
    val roomId: String = "",
    val addaId: String = "",
    val seatIndex: Long = 0L,
    val occupantId: String = "",
    val occupantName: String = "",
    val occupantVip: String = "NONE",
    val muted: Boolean = false,
    val locked: Boolean = false,
    val speaking: Boolean = false,
    val speakingLevel: Long = 0L,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class RoomMessage(
    @DocumentId val id: String = "",
    val roomId: String = "",
    val addaId: String = "",
    val familyId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val senderVip: String = "NONE",
    val msgType: String = "CHAT",
    val text: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class RoomMember(
    @DocumentId val id: String = "",
    val roomId: String = "",
    val addaId: String = "",
    val familyId: String = "",
    val userId: String = "",
    val username: String = "",
    val vipTier: String = "NONE",
    val role: String = "MEMBER",
    val muted: Boolean = false,
    @ServerTimestamp val joinedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class AddaGroup(
    @DocumentId val id: String = "",
    val name: String = "",
    val description: String = "",
    val rules: String = "",
    val avatarKey: String = "ROYAL_LOTUS",
    val ownerId: String = "",
    val ownerName: String = "",
    val publicAdda: Boolean = true,
    val themeId: String = "ROYAL_STAGE",
    val level: Long = 1L,
    val xp: Long = 0L,
    val memberCount: Long = 1L,
    val seatCount: Long = 8L,
    val coAdminUids: List<String> = emptyList(),
    val moderatorUids: List<String> = emptyList(),
    val mutedUids: List<String> = emptyList(),
    val bannedUids: List<String> = emptyList(),
    val active: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class PkBattle(
    @DocumentId val id: String = "",
    val roomId: String = "",
    val redTeamName: String = "Red Lions",
    val blueTeamName: String = "Blue Royals",
    val redCaptainId: String = "",
    val blueCaptainId: String = "",
    val redScore: Long = 0L,
    val blueScore: Long = 0L,
    val status: String = "ACTIVE",
    val winnerTeam: String = "NONE",
    val durationSec: Long = 300L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Family(
    @DocumentId val id: String = "",
    val name: String = "",
    val motto: String = "",
    val crestKey: String = "ROYAL_CREST",
    val ownerId: String = "",
    val ownerName: String = "",
    val adminUids: List<String> = emptyList(),
    val memberCount: Long = 1L,
    val level: Long = 1L,
    val xp: Long = 0L,
    val totalGiftPoints: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Agency(
    @DocumentId val id: String = "",
    val name: String = "",
    val description: String = "",
    val ownerId: String = "",
    val ownerName: String = "",
    val managerUids: List<String> = emptyList(),
    val hostCount: Long = 0L,
    val totalDiamonds: Long = 0L,
    val commissionPercent: Long = 15L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class AgencyHost(
    @DocumentId val id: String = "",
    val agencyId: String = "",
    val userId: String = "",
    val username: String = "",
    val status: String = "PENDING",
    val earnedDiamonds: Long = 0L,
    val liveMinutes: Long = 0L,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class FriendRequest(
    @DocumentId val id: String = "",
    val fromUid: String = "",
    val fromName: String = "",
    val toUid: String = "",
    val toName: String = "",
    val status: String = "PENDING",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Friendship(
    @DocumentId val id: String = "",
    val userIds: List<String> = emptyList(),
    val userNames: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class PrivateChat(
    @DocumentId val id: String = "",
    val participantIds: List<String> = emptyList(),
    val participantNames: List<String> = emptyList(),
    val lastMessage: String = "",
    val lastSenderId: String = "",
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class PrivateMessage(
    @DocumentId val id: String = "",
    val chatId: String = "",
    val participantIds: List<String> = emptyList(),
    val senderId: String = "",
    val senderName: String = "",
    val text: String = "",
    val msgType: String = "TEXT",
    val durationSec: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class AppEvent(
    @DocumentId val id: String = "",
    val title: String = "",
    val titleHi: String = "",
    val description: String = "",
    val eventType: String = "FESTIVAL",
    val rewardCoins: Long = 50L,
    val active: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Announcement(
    @DocumentId val id: String = "",
    val title: String = "",
    val body: String = "",
    val createdBy: String = "AKKI_ADMIN",
    val active: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Report(
    @DocumentId val id: String = "",
    val reporterId: String = "",
    val targetId: String = "",
    val targetType: String = "USER",
    val reason: String = "",
    val status: String = "OPEN",
    val adminNote: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

data class GiftAnimationEvent(
    val gift: GiftItem,
    val senderName: String,
    val receiverName: String,
    val quantity: Int,
    val timestampMillis: Long = System.currentTimeMillis()
)
