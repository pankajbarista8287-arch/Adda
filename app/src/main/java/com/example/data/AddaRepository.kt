package com.example.data

import com.example.model.AdminRegistry
import com.example.model.GiftCatalogSeed
import com.example.model.GiftItem
import com.example.model.RechargeOrder
import com.example.model.RechargePackage
import com.example.model.UserMission
import com.example.model.UserNotification
import com.example.model.UserPrivateAccount
import com.example.model.UserProfile
import com.example.model.VipPlan
import com.example.model.Wallet
import com.example.model.WalletTransaction
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlin.math.abs

class AddaRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    private fun requireUserId(): String =
        auth.currentUser?.uid ?: throw IllegalStateException("User must be signed in with Google.")

    private fun <T> Flow<T>.wrapFirestoreErrors(op: OperationType, path: String, uid: String): Flow<T> =
        this.catch { error ->
            val fsError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (fsError == null && error is CancellationException) throw error
            val toLog = fsError ?: (error as? Exception)
            if (toLog != null && auth.currentUser?.uid == uid) {
                handleFirestoreError(toLog, op, path)
            }
            throw error
        }

    suspend fun ensureUserInitialized(
        uid: String,
        displayName: String,
        email: String,
        photoUrl: String
    ): Result<Boolean> = try {
        val currentUid = requireUserId()
        check(currentUid == uid)
        val userRef = db.collection("users").document(uid)
        val existingUser = userRef.get().await()
        val isNewUser = !existingUser.exists()
        if (isNewUser) {
            val numericCode = (1000000 + abs(uid.hashCode() % 8999999)).toString()
            val safeName = displayName.trim().ifBlank { "ADDA User" }.take(30)
            userRef.set(
                mapOf(
                    "userId" to uid,
                    "addaCode" to numericCode,
                    "username" to safeName,
                    "bio" to "ADDA Voice & Gaming Lover",
                    "age" to 22L,
                    "gender" to "OTHER",
                    "avatarUrl" to photoUrl.take(500),
                    "avatarPreset" to "ROYAL_LOTUS",
                    "level" to 1L,
                    "xp" to 0L,
                    "giftSendLevel" to 1L,
                    "giftReceiveLevel" to 1L,
                    "vipTier" to "NONE",
                    "frameStyle" to "DEFAULT",
                    "nameColorHex" to "#F8F6FF",
                    "familyId" to "",
                    "agencyId" to "",
                    "hostStatus" to "NONE",
                    "verified" to false,
                    "banned" to false,
                    "online" to true,
                    "followersCount" to 0L,
                    "followingCount" to 0L,
                    "friendsCount" to 0L,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } else {
            userRef.update(
                mapOf(
                    "online" to true,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }

        val privRef = userRef.collection("private").document("account")
        if (!privRef.get().await().exists()) {
            privRef.set(
                mapOf(
                    "userId" to uid,
                    "email" to email.take(150),
                    "phone" to "",
                    "preferredLang" to "hi",
                    "securityPinSet" to false,
                    "twoFactorEnabled" to false,
                    "privacyDiscoverable" to true,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }

        val walletRef = db.collection("wallets").document(uid)
        if (!walletRef.get().await().exists()) {
            walletRef.set(
                mapOf(
                    "userId" to uid,
                    "coins" to 100L,
                    "diamonds" to 0L,
                    "totalSpentCoins" to 0L,
                    "totalEarnedDiamonds" to 0L,
                    "totalRechargedInr" to 0L,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }

        seedDefaultMissionsIfMissing(uid)

        // Bootstrap server-verified admin entry only for authorized admin email
        val userEmail = auth.currentUser?.email.orEmpty()
        val verified = auth.currentUser?.isEmailVerified == true
        if (userEmail == "pankajbarista8287@gmail.com" && verified) {
            val adminRef = db.collection("admins").document(uid)
            if (!adminRef.get().await().exists()) {
                adminRef.set(
                    mapOf(
                        "adminUid" to uid,
                        "adminCode" to "AKKI_ADMIN",
                        "email" to userEmail,
                        "role" to "SUPER_ADMIN",
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            seedDefaultGiftsIfAdmin()
        }

        Result.success(isNewUser)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.WRITE, "users/$uid")
        Result.failure(e)
    }

    private suspend fun seedDefaultMissionsIfMissing(uid: String) {
        val missionsCol = db.collection("users").document(uid).collection("missions")
        val dailyRef = missionsCol.document("daily_login")
        if (!dailyRef.get().await().exists()) {
            val defaults = listOf(
                Triple("daily_login", "Daily Login Reward", 1L) to (50L to 40L),
                Triple("send_gift", "Send 1 Animated Gift", 1L) to (30L to 60L),
                Triple("join_voice", "Join a Voice Room", 1L) to (25L to 50L),
                Triple("play_pk", "Support a PK Battle", 1L) to (40L to 80L)
            )
            defaults.forEach { (meta, rewards) ->
                missionsCol.document(meta.first).set(
                    mapOf(
                        "userId" to uid,
                        "missionKey" to meta.first.uppercase(),
                        "title" to meta.second,
                        "progress" to if (meta.first == "daily_login") 1L else 0L,
                        "target" to meta.third,
                        "rewardCoins" to rewards.first,
                        "rewardXp" to rewards.second,
                        "completed" to (meta.first == "daily_login"),
                        "claimed" to false,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
        }
    }

    private suspend fun seedDefaultGiftsIfAdmin() {
        val existingIds = db.collection("gifts").get().await().documents.map { it.id }.toSet()
        GiftCatalogSeed.defaultGifts
            .filter { it.id !in existingIds }
            .forEach { gift ->
                db.collection("gifts").document(gift.id).set(
                    mapOf(
                        "name" to gift.name,
                        "nameHi" to gift.nameHi,
                        "category" to gift.category,
                        "priceCoins" to gift.priceCoins,
                        "diamondValue" to gift.diamondValue,
                        "animationType" to gift.animationType,
                        "iconKey" to gift.iconKey,
                        "active" to true,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
    }

    fun observeMyProfile(uid: String): Flow<UserProfile?> = flow {
        val path = "users/$uid"
        emitAll(
            db.collection("users").document(uid).snapshots()
                .map { it.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .wrapFirestoreErrors(OperationType.GET, path, uid)
        )
    }

    fun observePrivateAccount(uid: String): Flow<UserPrivateAccount?> = flow {
        val path = "users/$uid/private/account"
        emitAll(
            db.collection("users").document(uid).collection("private").document("account").snapshots()
                .map { it.toObject(UserPrivateAccount::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .wrapFirestoreErrors(OperationType.GET, path, uid)
        )
    }

    fun observeAdminStatus(uid: String): Flow<AdminRegistry?> = flow {
        val path = "admins/$uid"
        emitAll(
            db.collection("admins").document(uid).snapshots()
                .map { it.toObject(AdminRegistry::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .catch { emit(null) }
        )
    }

    fun observeWallet(uid: String): Flow<Wallet?> = flow {
        val path = "wallets/$uid"
        emitAll(
            db.collection("wallets").document(uid).snapshots()
                .map { it.toObject(Wallet::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .wrapFirestoreErrors(OperationType.GET, path, uid)
        )
    }

    fun observeGifts(uid: String): Flow<List<GiftItem>> = flow {
        emitAll(
            db.collection("gifts").whereEqualTo("active", true).snapshots()
                .map { snap ->
                    val list = snap.toObjects(GiftItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    if (list.isEmpty()) {
                        GiftCatalogSeed.defaultGifts.sortedBy { it.priceCoins }
                    } else {
                        val existingIds = list.map { it.id }.toSet()
                        val extras = GiftCatalogSeed.defaultGifts.filter { it.id !in existingIds }
                        (list + extras).sortedBy { it.priceCoins }
                    }
                }
                .wrapFirestoreErrors(OperationType.LIST, "gifts", uid)
        )
    }

    fun observeAllUsersForDirectory(uid: String): Flow<List<UserProfile>> = flow {
        emitAll(
            db.collection("users").whereEqualTo("banned", false).snapshots()
                .map { snap ->
                    snap.toObjects(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.xp }
                }
                .wrapFirestoreErrors(OperationType.LIST, "users", uid)
        )
    }

    fun observeNotifications(uid: String): Flow<List<UserNotification>> = flow {
        val path = "users/$uid/notifications"
        emitAll(
            db.collection("users").document(uid).collection("notifications").snapshots()
                .map { snap ->
                    snap.toObjects(UserNotification::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .wrapFirestoreErrors(OperationType.LIST, path, uid)
        )
    }

    fun observeMissions(uid: String): Flow<List<UserMission>> = flow {
        val path = "users/$uid/missions"
        emitAll(
            db.collection("users").document(uid).collection("missions").snapshots()
                .map { snap ->
                    snap.toObjects(UserMission::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .wrapFirestoreErrors(OperationType.LIST, path, uid)
        )
    }

    fun observeTransactions(uid: String): Flow<List<WalletTransaction>> = flow {
        val sentFlow = db.collection("transactions").whereEqualTo("senderId", uid).snapshots()
            .map { it.toObjects(WalletTransaction::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
        val recvFlow = db.collection("transactions").whereEqualTo("receiverId", uid).snapshots()
            .map { it.toObjects(WalletTransaction::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
        emitAll(
            combine(sentFlow, recvFlow) { sent, recv ->
                (sent + recv).distinctBy { it.id }
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }.wrapFirestoreErrors(OperationType.LIST, "transactions", uid)
        )
    }

    fun observeMyRechargeOrders(uid: String): Flow<List<RechargeOrder>> = flow {
        emitAll(
            db.collection("recharge_orders").whereEqualTo("userId", uid).snapshots()
                .map { snap ->
                    snap.toObjects(RechargeOrder::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .wrapFirestoreErrors(OperationType.LIST, "recharge_orders", uid)
        )
    }

    suspend fun updateProfile(
        username: String,
        bio: String,
        age: Long,
        gender: String,
        avatarUrl: String? = null
    ): Result<Unit> = try {
        val uid = requireUserId()
        val cleanName = username.trim()
        require(cleanName.length in 2..30) { "Username must be 2 to 30 characters" }
        require(age in 13..100) { "Age must be between 13 and 100" }
        val updates = mutableMapOf<String, Any>(
            "username" to cleanName,
            "bio" to bio.trim().take(200),
            "age" to age,
            "gender" to gender,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (!avatarUrl.isNullOrBlank()) {
            updates["avatarUrl"] = avatarUrl.take(500)
        }
        db.collection("users").document(uid).update(updates).await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.UPDATE, "users/${auth.currentUser?.uid}")
        Result.failure(e)
    }

    suspend fun updatePrivateSettings(
        phone: String,
        preferredLang: String,
        securityPinSet: Boolean,
        privacyDiscoverable: Boolean
    ): Result<Unit> = try {
        val uid = requireUserId()
        db.collection("users").document(uid).collection("private").document("account")
            .update(
                mapOf(
                    "phone" to phone.trim().take(20),
                    "preferredLang" to preferredLang,
                    "securityPinSet" to securityPinSet,
                    "privacyDiscoverable" to privacyDiscoverable,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.UPDATE, "users/${auth.currentUser?.uid}/private/account")
        Result.failure(e)
    }

    suspend fun submitRechargeOrder(
        userName: String,
        pkg: RechargePackage,
        utrReference: String,
        paymentMethod: String = "UPI"
    ): Result<Unit> = try {
        val uid = requireUserId()
        val cleanUtr = utrReference.trim()
        require(cleanUtr.length in 4..64) { "Enter valid UPI UTR / Payment reference (4-64 chars)" }
        val docRef = db.collection("recharge_orders").document()
        docRef.set(
            mapOf(
                "userId" to uid,
                "userName" to userName.take(60),
                "packageId" to pkg.id,
                "packageLabel" to pkg.label,
                "amountInr" to pkg.amountInr,
                "coinsAmount" to pkg.coinsAmount,
                "paymentMethod" to paymentMethod,
                "utrReference" to cleanUtr,
                "status" to "PENDING",
                "verifiedBy" to "",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.CREATE, "recharge_orders")
        Result.failure(e)
    }

    suspend fun sendGiftAtomic(
        senderProfile: UserProfile,
        receiverId: String,
        receiverName: String,
        gift: GiftItem,
        quantity: Int,
        contextType: String, // "ROOM" or "ADDA"
        contextId: String,
        pkBattleId: String? = null,
        pkTeam: String? = null // "RED" or "BLUE"
    ): Result<Unit> = try {
        val uid = requireUserId()
        require(uid == senderProfile.userId)
        require(quantity in 1..99)
        val totalCost = gift.priceCoins * quantity
        val totalDiamonds = gift.diamondValue * quantity

        val senderWalletRef = db.collection("wallets").document(uid)
        val senderWalletSnap = senderWalletRef.get().await()
        val currentCoins = senderWalletSnap.getLong("coins") ?: 0L
        if (currentCoins < totalCost) {
            return Result.failure(IllegalStateException("Insufficient Coins! Need $totalCost Coins."))
        }

        val batch = db.batch()
        batch.update(
            senderWalletRef,
            mapOf(
                "coins" to currentCoins - totalCost,
                "totalSpentCoins" to (senderWalletSnap.getLong("totalSpentCoins") ?: 0L) + totalCost,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )

        if (receiverId.isNotBlank() && receiverId != uid) {
            val receiverWalletRef = db.collection("wallets").document(receiverId)
            val receiverSnap = receiverWalletRef.get().await()
            if (receiverSnap.exists()) {
                val currDia = receiverSnap.getLong("diamonds") ?: 0L
                val currEarned = receiverSnap.getLong("totalEarnedDiamonds") ?: 0L
                batch.update(
                    receiverWalletRef,
                    mapOf(
                        "diamonds" to currDia + totalDiamonds,
                        "totalEarnedDiamonds" to currEarned + totalDiamonds,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
        }

        val txRef = db.collection("transactions").document()
        batch.set(
            txRef,
            mapOf(
                "senderId" to uid,
                "senderName" to senderProfile.username.take(60),
                "receiverId" to receiverId.ifBlank { uid },
                "receiverName" to receiverName.ifBlank { senderProfile.username }.take(60),
                "type" to "GIFT_SEND",
                "amountCoins" to totalCost,
                "amountDiamonds" to totalDiamonds,
                "giftId" to gift.id,
                "giftName" to gift.name,
                "quantity" to quantity.toLong(),
                "contextId" to contextId,
                "note" to "$contextType Gift: ${gift.name} x$quantity",
                "createdAt" to FieldValue.serverTimestamp()
            )
        )

        // XP & Level progression (1..100)
        val newXp = senderProfile.xp + totalCost
        val newLevel = (1L + (newXp / 500L)).coerceIn(1L, 100L)
        val newSendLvl = (1L + ((senderProfile.giftSendLevel * 300L + totalCost) / 300L)).coerceIn(1L, 100L)
        batch.update(
            db.collection("users").document(uid),
            mapOf(
                "xp" to newXp,
                "level" to newLevel,
                "giftSendLevel" to newSendLvl,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )

        if (contextId.isNotBlank()) {
            val colName = if (contextType == "ADDA") "adda_groups" else "voice_rooms"
            val roomRef = db.collection(colName).document(contextId)
            val msgRef = roomRef.collection("messages").document()
            val msgData = mutableMapOf<String, Any>(
                "senderId" to uid,
                "senderName" to senderProfile.username.take(40),
                "senderVip" to senderProfile.vipTier,
                "msgType" to "GIFT",
                "text" to "${senderProfile.username} sent ${gift.name} x$quantity to ${receiverName.ifBlank { "Stage" }}!",
                "createdAt" to FieldValue.serverTimestamp()
            )
            if (contextType == "ADDA") msgData["addaId"] = contextId else msgData["roomId"] = contextId
            batch.set(msgRef, msgData)

            if (contextType == "ROOM") {
                val roomSnap = roomRef.get().await()
                if (roomSnap.exists()) {
                    val curRoomCoins = roomSnap.getLong("totalGiftCoins") ?: 0L
                    val curRoomXp = roomSnap.getLong("xp") ?: 0L
                    val nextRoomXp = curRoomXp + totalCost
                    batch.update(
                        roomRef,
                        mapOf(
                            "totalGiftCoins" to curRoomCoins + totalCost,
                            "xp" to nextRoomXp,
                            "level" to (1L + nextRoomXp / 1000L).coerceIn(1L, 100L),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
            } else {
                val addaSnap = roomRef.get().await()
                if (addaSnap.exists()) {
                    val nextXp = (addaSnap.getLong("xp") ?: 0L) + totalCost
                    batch.update(
                        roomRef,
                        mapOf(
                            "xp" to nextXp,
                            "level" to (1L + nextXp / 1000L).coerceIn(1L, 100L),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
            }
        }

        if (!pkBattleId.isNullOrBlank() && pkTeam in listOf("RED", "BLUE")) {
            val pkRef = db.collection("pk_battles").document(pkBattleId)
            val pkSnap = pkRef.get().await()
            if (pkSnap.exists() && pkSnap.getString("status") == "ACTIVE") {
                val field = if (pkTeam == "RED") "redScore" else "blueScore"
                val currentScore = pkSnap.getLong(field) ?: 0L
                batch.update(
                    pkRef,
                    mapOf(
                        field to currentScore + totalCost,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
        }

        if (receiverId.isNotBlank() && receiverId != uid) {
            val notifRef = db.collection("users").document(receiverId).collection("notifications").document()
            batch.set(
                notifRef,
                mapOf(
                    "userId" to receiverId,
                    "type" to "GIFT_RECEIVED",
                    "title" to "Gift Received!",
                    "body" to "${senderProfile.username} sent you ${gift.name} x$quantity (+$totalDiamonds Diamonds)",
                    "read" to false,
                    "targetId" to contextId,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
        }

        batch.commit().await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.WRITE, "transactions")
        Result.failure(e)
    }

    suspend fun purchaseVipPlan(profile: UserProfile, plan: VipPlan): Result<Unit> = try {
        val uid = requireUserId()
        val walletRef = db.collection("wallets").document(uid)
        val walletSnap = walletRef.get().await()
        val coins = walletSnap.getLong("coins") ?: 0L
        if (coins < plan.priceCoins) {
            return Result.failure(IllegalStateException("Insufficient Coins for ${plan.tier}"))
        }
        val batch = db.batch()
        batch.update(
            walletRef,
            mapOf(
                "coins" to coins - plan.priceCoins,
                "totalSpentCoins" to (walletSnap.getLong("totalSpentCoins") ?: 0L) + plan.priceCoins,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )
        batch.update(
            db.collection("users").document(uid),
            mapOf(
                "vipTier" to plan.tier,
                "frameStyle" to if (plan.tier == "SVIP") "SVIP_PHOENIX" else "GOLD",
                "nameColorHex" to if (plan.tier == "SVIP") "#FF2A85" else "#FFD700",
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )
        batch.set(
            db.collection("transactions").document(),
            mapOf(
                "senderId" to uid,
                "senderName" to profile.username,
                "receiverId" to uid,
                "receiverName" to "ADDA VIP Store",
                "type" to "VIP_PURCHASE",
                "amountCoins" to plan.priceCoins,
                "amountDiamonds" to 0L,
                "giftId" to plan.tier,
                "giftName" to plan.title,
                "quantity" to 1L,
                "contextId" to "VIP_STORE",
                "note" to "Activated ${plan.tier} (${plan.durationDays} days)",
                "createdAt" to FieldValue.serverTimestamp()
            )
        )
        batch.commit().await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.WRITE, "users/${profile.userId}")
        Result.failure(e)
    }

    suspend fun markMissionCompletedAndClaim(profile: UserProfile, mission: UserMission): Result<Unit> = try {
        val uid = requireUserId()
        val ref = db.collection("users").document(uid).collection("missions").document(mission.id)
        ref.update(
            mapOf(
                "progress" to mission.target,
                "completed" to true,
                "claimed" to true,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
        val newXp = profile.xp + mission.rewardXp
        db.collection("users").document(uid).update(
            mapOf(
                "xp" to newXp,
                "level" to (1L + newXp / 500L).coerceIn(1L, 100L),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.UPDATE, "users/${auth.currentUser?.uid}/missions/${mission.id}")
        Result.failure(e)
    }

    suspend fun markNotificationRead(notifId: String): Result<Unit> = try {
        val uid = requireUserId()
        db.collection("users").document(uid).collection("notifications").document(notifId)
            .update(mapOf("read" to true))
            .await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.UPDATE, "users/${auth.currentUser?.uid}/notifications/$notifId")
        Result.failure(e)
    }

    suspend fun setOfflineOnSignOut(uid: String) {
        try {
            db.collection("users").document(uid)
                .update(mapOf("online" to false, "updatedAt" to FieldValue.serverTimestamp()))
                .await()
        } catch (_: Exception) {
        }
    }
}
