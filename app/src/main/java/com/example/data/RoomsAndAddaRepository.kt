package com.example.data

import com.example.model.AddaGroup
import com.example.model.Agency
import com.example.model.AgencyHost
import com.example.model.Announcement
import com.example.model.AppEvent
import com.example.model.Family
import com.example.model.FriendRequest
import com.example.model.Friendship
import com.example.model.GiftItem
import com.example.model.PkBattle
import com.example.model.PrivateChat
import com.example.model.PrivateMessage
import com.example.model.RechargeOrder
import com.example.model.Report
import com.example.model.RoomMember
import com.example.model.RoomMessage
import com.example.model.RoomSeat
import com.example.model.UserProfile
import com.example.model.VoiceRoom
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

class RoomsAndAddaRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    private fun requireUserId(): String =
        auth.currentUser?.uid ?: throw IllegalStateException("User must be signed in with Google.")

    private fun <T> Flow<T>.wrapError(op: OperationType, path: String, uid: String): Flow<T> =
        this.catch { error ->
            val fsErr = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (fsErr == null && error is CancellationException) throw error
            val toLog = fsErr ?: (error as? Exception)
            if (toLog != null && auth.currentUser?.uid == uid) {
                handleFirestoreError(toLog, op, path)
            }
            throw error
        }

    // --- VOICE ROOMS ---
    fun observeVoiceRooms(uid: String): Flow<List<VoiceRoom>> = flow {
        emitAll(
            db.collection("voice_rooms").whereEqualTo("active", true).snapshots()
                .map { snap ->
                    snap.toObjects(VoiceRoom::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.onlineCount + it.totalGiftCoins }
                }
                .wrapError(OperationType.LIST, "voice_rooms", uid)
        )
    }

    fun observeRoomSeats(roomId: String, uid: String): Flow<List<RoomSeat>> = flow {
        val path = "voice_rooms/$roomId/seats"
        emitAll(
            db.collection("voice_rooms").document(roomId).collection("seats")
                .whereEqualTo("roomId", roomId)
                .snapshots()
                .map { snap ->
                    snap.toObjects(RoomSeat::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.seatIndex }
                }
                .wrapError(OperationType.LIST, path, uid)
        )
    }

    fun observeRoomMessages(roomId: String, uid: String): Flow<List<RoomMessage>> = flow {
        val path = "voice_rooms/$roomId/messages"
        emitAll(
            db.collection("voice_rooms").document(roomId).collection("messages")
                .whereEqualTo("roomId", roomId)
                .snapshots()
                .map { snap ->
                    snap.toObjects(RoomMessage::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.createdAt?.seconds ?: Long.MAX_VALUE }
                }
                .wrapError(OperationType.LIST, path, uid)
        )
    }

    fun observeRoomMembers(roomId: String, uid: String): Flow<List<RoomMember>> = flow {
        val path = "voice_rooms/$roomId/members"
        emitAll(
            db.collection("voice_rooms").document(roomId).collection("members")
                .whereEqualTo("roomId", roomId)
                .snapshots()
                .map { snap ->
                    snap.toObjects(RoomMember::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .wrapError(OperationType.LIST, path, uid)
        )
    }

    suspend fun createVoiceRoom(
        owner: UserProfile,
        title: String,
        topic: String,
        roomType: String,
        pin: String,
        seatCount: Int,
        themeId: String,
        announcement: String
    ): Result<String> = try {
        val uid = requireUserId()
        val clampedSeats = seatCount.coerceIn(8, 16)
        val roomRef = db.collection("voice_rooms").document()
        val roomId = roomRef.id
        val batch = db.batch()
        batch.set(
            roomRef,
            mapOf(
                "title" to title.trim().take(60),
                "topic" to topic.trim().take(120),
                "ownerId" to uid,
                "ownerName" to owner.username.take(40),
                "roomType" to roomType,
                "roomPin" to if (roomType == "PASSWORD") pin.take(12) else "",
                "seatCount" to clampedSeats.toLong(),
                "themeId" to themeId,
                "announcement" to announcement.trim().take(250),
                "level" to 1L,
                "xp" to 0L,
                "onlineCount" to 1L,
                "totalGiftCoins" to 0L,
                "pkActive" to false,
                "moderatorUids" to emptyList<String>(),
                "active" to true,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )
        for (i in 0 until clampedSeats) {
            val seatRef = roomRef.collection("seats").document("seat_$i")
            batch.set(
                seatRef,
                mapOf(
                    "roomId" to roomId,
                    "seatIndex" to i.toLong(),
                    "occupantId" to if (i == 0) uid else "",
                    "occupantName" to if (i == 0) owner.username.take(40) else "",
                    "occupantVip" to if (i == 0) owner.vipTier else "NONE",
                    "muted" to false,
                    "locked" to false,
                    "speaking" to false,
                    "speakingLevel" to 0L,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
        }
        batch.set(
            roomRef.collection("members").document(uid),
            mapOf(
                "roomId" to roomId,
                "userId" to uid,
                "username" to owner.username.take(40),
                "vipTier" to owner.vipTier,
                "role" to "OWNER",
                "muted" to false,
                "joinedAt" to FieldValue.serverTimestamp()
            )
        )
        batch.commit().await()
        Result.success(roomId)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.CREATE, "voice_rooms")
        Result.failure(e)
    }

    suspend fun joinRoomMember(roomId: String, user: UserProfile, isAdda: Boolean = false): Result<Unit> = try {
        val uid = requireUserId()
        val col = if (isAdda) "adda_groups" else "voice_rooms"
        val idKey = if (isAdda) "addaId" else "roomId"
        val parentRef = db.collection(col).document(roomId)
        parentRef.collection("members").document(uid).set(
            mapOf(
                idKey to roomId,
                "userId" to uid,
                "username" to user.username.take(40),
                "vipTier" to user.vipTier,
                "role" to "MEMBER",
                "muted" to false,
                "joinedAt" to FieldValue.serverTimestamp()
            )
        ).await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.WRITE, "rooms/$roomId/members")
        Result.failure(e)
    }

    suspend fun updateSeat(
        parentId: String,
        seatIndex: Int,
        occupantId: String,
        occupantName: String,
        occupantVip: String,
        muted: Boolean,
        locked: Boolean,
        speaking: Boolean,
        speakingLevel: Int,
        isAdda: Boolean = false
    ): Result<Unit> = try {
        requireUserId()
        val col = if (isAdda) "adda_groups" else "voice_rooms"
        val idKey = if (isAdda) "addaId" else "roomId"
        db.collection(col).document(parentId)
            .collection("seats").document("seat_$seatIndex")
            .set(
                mapOf(
                    idKey to parentId,
                    "seatIndex" to seatIndex.toLong(),
                    "occupantId" to occupantId,
                    "occupantName" to occupantName.take(40),
                    "occupantVip" to occupantVip,
                    "muted" to muted,
                    "locked" to locked,
                    "speaking" to speaking,
                    "speakingLevel" to speakingLevel.coerceIn(0, 100).toLong(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.UPDATE, "seats/$parentId/seat_$seatIndex")
        Result.failure(e)
    }

    suspend fun sendRoomMessage(
        parentId: String,
        sender: UserProfile,
        text: String,
        isAdda: Boolean = false,
        isFamily: Boolean = false
    ): Result<Unit> = try {
        val uid = requireUserId()
        val clean = text.trim()
        require(clean.isNotEmpty() && clean.length <= 400)
        val col = when {
            isFamily -> "families"
            isAdda -> "adda_groups"
            else -> "voice_rooms"
        }
        val idKey = when {
            isFamily -> "familyId"
            isAdda -> "addaId"
            else -> "roomId"
        }
        db.collection(col).document(parentId).collection("messages").document().set(
            mapOf(
                idKey to parentId,
                "senderId" to uid,
                "senderName" to sender.username.take(40),
                "senderVip" to sender.vipTier,
                "msgType" to "CHAT",
                "text" to clean,
                "createdAt" to FieldValue.serverTimestamp()
            )
        ).await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.CREATE, "messages/$parentId")
        Result.failure(e)
    }

    suspend fun updateRoomSettings(roomId: String, announcement: String, themeId: String): Result<Unit> = try {
        requireUserId()
        db.collection("voice_rooms").document(roomId).update(
            mapOf(
                "announcement" to announcement.trim().take(250),
                "themeId" to themeId,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.UPDATE, "voice_rooms/$roomId")
        Result.failure(e)
    }

    // --- ADDA GROUPS ---
    fun observeAddaGroups(uid: String): Flow<List<AddaGroup>> = flow {
        val publicFlow = db.collection("adda_groups").whereEqualTo("publicAdda", true).snapshots()
            .map { it.toObjects(AddaGroup::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
        val mineFlow = db.collection("adda_groups").whereEqualTo("ownerId", uid).snapshots()
            .map { it.toObjects(AddaGroup::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
        emitAll(
            combine(publicFlow, mineFlow) { pub, mine ->
                (pub + mine).distinctBy { it.id }.filter { it.active }
                    .sortedByDescending { it.memberCount + it.xp }
            }.wrapError(OperationType.LIST, "adda_groups", uid)
        )
    }

    fun observeAddaSeats(addaId: String, uid: String): Flow<List<RoomSeat>> = flow {
        emitAll(
            db.collection("adda_groups").document(addaId).collection("seats")
                .whereEqualTo("addaId", addaId)
                .snapshots()
                .map { snap ->
                    snap.toObjects(RoomSeat::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.seatIndex }
                }
                .wrapError(OperationType.LIST, "adda_groups/$addaId/seats", uid)
        )
    }

    fun observeAddaMessages(addaId: String, uid: String): Flow<List<RoomMessage>> = flow {
        emitAll(
            db.collection("adda_groups").document(addaId).collection("messages")
                .whereEqualTo("addaId", addaId)
                .snapshots()
                .map { snap ->
                    snap.toObjects(RoomMessage::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.createdAt?.seconds ?: Long.MAX_VALUE }
                }
                .wrapError(OperationType.LIST, "adda_groups/$addaId/messages", uid)
        )
    }

    fun observeAddaMembers(addaId: String, uid: String): Flow<List<RoomMember>> = flow {
        emitAll(
            db.collection("adda_groups").document(addaId).collection("members")
                .whereEqualTo("addaId", addaId)
                .snapshots()
                .map { it.toObjects(RoomMember::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .wrapError(OperationType.LIST, "adda_groups/$addaId/members", uid)
        )
    }

    suspend fun createAddaGroup(
        owner: UserProfile,
        name: String,
        description: String,
        rules: String,
        publicAdda: Boolean,
        seatCount: Int,
        themeId: String
    ): Result<String> = try {
        val uid = requireUserId()
        val seats = seatCount.coerceIn(8, 16)
        val ref = db.collection("adda_groups").document()
        val addaId = ref.id
        val batch = db.batch()
        batch.set(
            ref,
            mapOf(
                "name" to name.trim().take(60),
                "description" to description.trim().take(250),
                "rules" to rules.trim().take(400),
                "avatarKey" to "ROYAL_LOTUS",
                "ownerId" to uid,
                "ownerName" to owner.username.take(40),
                "publicAdda" to publicAdda,
                "themeId" to themeId,
                "level" to 1L,
                "xp" to 0L,
                "memberCount" to 1L,
                "seatCount" to seats.toLong(),
                "coAdminUids" to emptyList<String>(),
                "moderatorUids" to emptyList<String>(),
                "mutedUids" to emptyList<String>(),
                "bannedUids" to emptyList<String>(),
                "active" to true,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )
        for (i in 0 until seats) {
            batch.set(
                ref.collection("seats").document("seat_$i"),
                mapOf(
                    "addaId" to addaId,
                    "seatIndex" to i.toLong(),
                    "occupantId" to if (i == 0) uid else "",
                    "occupantName" to if (i == 0) owner.username.take(40) else "",
                    "occupantVip" to if (i == 0) owner.vipTier else "NONE",
                    "muted" to false,
                    "locked" to false,
                    "speaking" to false,
                    "speakingLevel" to 0L,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
        }
        batch.set(
            ref.collection("members").document(uid),
            mapOf(
                "addaId" to addaId,
                "userId" to uid,
                "username" to owner.username.take(40),
                "vipTier" to owner.vipTier,
                "role" to "OWNER",
                "muted" to false,
                "joinedAt" to FieldValue.serverTimestamp()
            )
        )
        batch.commit().await()
        Result.success(addaId)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.CREATE, "adda_groups")
        Result.failure(e)
    }

    suspend fun toggleAddaModeration(
        adda: AddaGroup,
        targetUid: String,
        action: String // "CO_ADMIN", "MODERATOR", "MUTE", "KICK"
    ): Result<Unit> = try {
        requireUserId()
        val ref = db.collection("adda_groups").document(adda.id)
        when (action) {
            "CO_ADMIN" -> {
                val updated = (adda.coAdminUids + targetUid).distinct().take(10)
                ref.update(mapOf("coAdminUids" to updated, "updatedAt" to FieldValue.serverTimestamp())).await()
            }
            "MODERATOR" -> {
                val updated = (adda.moderatorUids + targetUid).distinct().take(20)
                ref.update(mapOf("moderatorUids" to updated, "updatedAt" to FieldValue.serverTimestamp())).await()
            }
            "MUTE" -> {
                val updated = if (targetUid in adda.mutedUids) adda.mutedUids - targetUid else (adda.mutedUids + targetUid).take(50)
                ref.update(mapOf("mutedUids" to updated, "updatedAt" to FieldValue.serverTimestamp())).await()
            }
            "KICK" -> {
                val updated = (adda.bannedUids + targetUid).distinct().take(50)
                ref.update(mapOf("bannedUids" to updated, "updatedAt" to FieldValue.serverTimestamp())).await()
                ref.collection("members").document(targetUid).delete().await()
            }
        }
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.UPDATE, "adda_groups/${adda.id}")
        Result.failure(e)
    }

    // --- PK BATTLES ---
    fun observePkBattles(uid: String): Flow<List<PkBattle>> = flow {
        emitAll(
            db.collection("pk_battles").whereIn("status", listOf("ACTIVE", "FINISHED")).snapshots()
                .map { snap ->
                    snap.toObjects(PkBattle::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .wrapError(OperationType.LIST, "pk_battles", uid)
        )
    }

    suspend fun startPkBattle(
        roomId: String,
        redTeam: String,
        blueTeam: String,
        durationSec: Long = 300L
    ): Result<String> = try {
        val uid = requireUserId()
        val ref = db.collection("pk_battles").document()
        ref.set(
            mapOf(
                "roomId" to roomId,
                "redTeamName" to redTeam.trim().take(40),
                "blueTeamName" to blueTeam.trim().take(40),
                "redCaptainId" to uid,
                "blueCaptainId" to uid,
                "redScore" to 0L,
                "blueScore" to 0L,
                "status" to "ACTIVE",
                "winnerTeam" to "NONE",
                "durationSec" to durationSec.coerceIn(60L, 1800L),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
        db.collection("voice_rooms").document(roomId).update(
            mapOf("pkActive" to true, "updatedAt" to FieldValue.serverTimestamp())
        ).await()
        Result.success(ref.id)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.CREATE, "pk_battles")
        Result.failure(e)
    }

    suspend fun finishPkBattle(battle: PkBattle): Result<Unit> = try {
        requireUserId()
        val winner = when {
            battle.redScore > battle.blueScore -> "RED"
            battle.blueScore > battle.redScore -> "BLUE"
            else -> "DRAW"
        }
        db.collection("pk_battles").document(battle.id).update(
            mapOf(
                "status" to "FINISHED",
                "winnerTeam" to winner,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
        db.collection("voice_rooms").document(battle.roomId).update(
            mapOf("pkActive" to false, "updatedAt" to FieldValue.serverTimestamp())
        ).await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.UPDATE, "pk_battles/${battle.id}")
        Result.failure(e)
    }

    // --- FAMILIES & AGENCIES ---
    fun observeFamilies(uid: String): Flow<List<Family>> = flow {
        emitAll(
            db.collection("families").whereGreaterThanOrEqualTo("memberCount", 1L).snapshots()
                .map { snap ->
                    snap.toObjects(Family::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.totalGiftPoints + it.xp }
                }
                .wrapError(OperationType.LIST, "families", uid)
        )
    }

    suspend fun createFamily(owner: UserProfile, name: String, motto: String): Result<Unit> = try {
        val uid = requireUserId()
        val ref = db.collection("families").document()
        val batch = db.batch()
        batch.set(
            ref,
            mapOf(
                "name" to name.trim().take(50),
                "motto" to motto.trim().take(150),
                "crestKey" to "ROYAL_CREST",
                "ownerId" to uid,
                "ownerName" to owner.username.take(40),
                "adminUids" to emptyList<String>(),
                "memberCount" to 1L,
                "level" to 1L,
                "xp" to 0L,
                "totalGiftPoints" to 0L,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )
        batch.set(
            ref.collection("members").document(uid),
            mapOf(
                "familyId" to ref.id,
                "userId" to uid,
                "username" to owner.username.take(40),
                "vipTier" to owner.vipTier,
                "role" to "OWNER",
                "muted" to false,
                "joinedAt" to FieldValue.serverTimestamp()
            )
        )
        batch.update(
            db.collection("users").document(uid),
            mapOf("familyId" to ref.id, "updatedAt" to FieldValue.serverTimestamp())
        )
        batch.commit().await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.CREATE, "families")
        Result.failure(e)
    }

    suspend fun joinFamily(family: Family, user: UserProfile): Result<Unit> = try {
        val uid = requireUserId()
        val ref = db.collection("families").document(family.id)
        val batch = db.batch()
        batch.set(
            ref.collection("members").document(uid),
            mapOf(
                "familyId" to family.id,
                "userId" to uid,
                "username" to user.username.take(40),
                "vipTier" to user.vipTier,
                "role" to "MEMBER",
                "muted" to false,
                "joinedAt" to FieldValue.serverTimestamp()
            )
        )
        batch.update(
            ref,
            mapOf("memberCount" to family.memberCount + 1L, "updatedAt" to FieldValue.serverTimestamp())
        )
        batch.update(
            db.collection("users").document(uid),
            mapOf("familyId" to family.id, "updatedAt" to FieldValue.serverTimestamp())
        )
        batch.commit().await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.WRITE, "families/${family.id}")
        Result.failure(e)
    }

    fun observeAgencies(uid: String): Flow<List<Agency>> = flow {
        emitAll(
            db.collection("agencies").whereGreaterThanOrEqualTo("hostCount", 0L).snapshots()
                .map { snap ->
                    snap.toObjects(Agency::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.totalDiamonds }
                }
                .wrapError(OperationType.LIST, "agencies", uid)
        )
    }

    suspend fun createAgency(owner: UserProfile, name: String, description: String, commission: Long): Result<Unit> = try {
        val uid = requireUserId()
        db.collection("agencies").document().set(
            mapOf(
                "name" to name.trim().take(50),
                "description" to description.trim().take(200),
                "ownerId" to uid,
                "ownerName" to owner.username.take(40),
                "managerUids" to emptyList<String>(),
                "hostCount" to 0L,
                "totalDiamonds" to 0L,
                "commissionPercent" to commission.coerceIn(5L, 50L),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.CREATE, "agencies")
        Result.failure(e)
    }

    suspend fun applyAsHost(agency: Agency, user: UserProfile): Result<Unit> = try {
        val uid = requireUserId()
        val ref = db.collection("agencies").document(agency.id)
        val batch = db.batch()
        batch.set(
            ref.collection("hosts").document(uid),
            mapOf(
                "agencyId" to agency.id,
                "userId" to uid,
                "username" to user.username.take(40),
                "status" to "APPROVED",
                "earnedDiamonds" to 0L,
                "liveMinutes" to 0L,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )
        batch.update(
            ref,
            mapOf("hostCount" to agency.hostCount + 1L, "updatedAt" to FieldValue.serverTimestamp())
        )
        batch.update(
            db.collection("users").document(uid),
            mapOf(
                "agencyId" to agency.id,
                "hostStatus" to "APPROVED",
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )
        batch.commit().await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.WRITE, "agencies/${agency.id}/hosts")
        Result.failure(e)
    }

    // --- FRIENDS, PRIVATE CHAT, EVENTS, ANNOUNCEMENTS, REPORTS ---
    fun observeIncomingRequests(uid: String): Flow<List<FriendRequest>> = flow {
        emitAll(
            db.collection("friend_requests").whereEqualTo("toUid", uid).snapshots()
                .map { snap ->
                    snap.toObjects(FriendRequest::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .filter { it.status == "PENDING" }
                }
                .wrapError(OperationType.LIST, "friend_requests", uid)
        )
    }

    fun observeFriendships(uid: String): Flow<List<Friendship>> = flow {
        emitAll(
            db.collection("friendships").whereArrayContains("userIds", uid).snapshots()
                .map { it.toObjects(Friendship::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .wrapError(OperationType.LIST, "friendships", uid)
        )
    }

    suspend fun sendFriendRequest(from: UserProfile, to: UserProfile): Result<Unit> = try {
        val uid = requireUserId()
        require(uid != to.userId)
        val batch = db.batch()
        batch.set(
            db.collection("friend_requests").document(),
            mapOf(
                "fromUid" to uid,
                "fromName" to from.username.take(40),
                "toUid" to to.userId,
                "toName" to to.username.take(40),
                "status" to "PENDING",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )
        batch.set(
            db.collection("users").document(to.userId).collection("notifications").document(),
            mapOf(
                "userId" to to.userId,
                "type" to "FRIEND_REQUEST",
                "title" to "New Friend Request",
                "body" to "${from.username} wants to be friends on ADDA",
                "read" to false,
                "targetId" to uid,
                "createdAt" to FieldValue.serverTimestamp()
            )
        )
        batch.commit().await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.CREATE, "friend_requests")
        Result.failure(e)
    }

    suspend fun respondFriendRequest(req: FriendRequest, accept: Boolean): Result<Unit> = try {
        requireUserId()
        val batch = db.batch()
        batch.update(
            db.collection("friend_requests").document(req.id),
            mapOf(
                "status" to if (accept) "ACCEPTED" else "REJECTED",
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )
        if (accept) {
            val sortedIds = listOf(req.fromUid, req.toUid).sorted()
            val pairId = "${sortedIds[0]}_${sortedIds[1]}"
            val friendshipRef = db.collection("friendships").document(pairId)
            if (!friendshipRef.get().await().exists()) {
                val names = if (sortedIds[0] == req.fromUid) {
                    listOf(req.fromName, req.toName)
                } else {
                    listOf(req.toName, req.fromName)
                }
                batch.set(
                    friendshipRef,
                    mapOf(
                        "userIds" to sortedIds,
                        "userNames" to names,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
            }
        }
        batch.commit().await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.UPDATE, "friend_requests/${req.id}")
        Result.failure(e)
    }

    fun observeChats(uid: String): Flow<List<PrivateChat>> = flow {
        emitAll(
            db.collection("chats").whereArrayContains("participantIds", uid).snapshots()
                .map { snap ->
                    snap.toObjects(PrivateChat::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.updatedAt?.seconds ?: 0L }
                }
                .wrapError(OperationType.LIST, "chats", uid)
        )
    }

    fun observePrivateMessages(chatId: String, uid: String): Flow<List<PrivateMessage>> = flow {
        emitAll(
            db.collection("chats").document(chatId).collection("messages")
                .whereArrayContains("participantIds", uid)
                .snapshots()
                .map { snap ->
                    snap.toObjects(PrivateMessage::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.createdAt?.seconds ?: Long.MAX_VALUE }
                }
                .wrapError(OperationType.LIST, "chats/$chatId/messages", uid)
        )
    }

    suspend fun sendPrivateMessage(
        sender: UserProfile,
        otherUid: String,
        otherName: String,
        text: String,
        isVoice: Boolean = false,
        durationSec: Long = 0L
    ): Result<String> = try {
        val uid = requireUserId()
        val sortedIds = listOf(uid, otherUid).sorted()
        val chatId = "${sortedIds[0]}_${sortedIds[1]}"
        val chatRef = db.collection("chats").document(chatId)
        val names = if (sortedIds[0] == uid) listOf(sender.username, otherName) else listOf(otherName, sender.username)
        val preview = if (isVoice) {
            "Voice Note (${durationSec.coerceIn(0L, 600L)}s)"
        } else {
            val trimmed = text.trim()
            require(trimmed.isNotEmpty()) { "Message cannot be empty" }
            trimmed.take(300)
        }
        val chatExists = chatRef.get().await().exists()
        val batch = db.batch()
        if (chatExists) {
            batch.update(
                chatRef,
                mapOf(
                    "lastMessage" to preview,
                    "lastSenderId" to uid,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
        } else {
            batch.set(
                chatRef,
                mapOf(
                    "participantIds" to sortedIds,
                    "participantNames" to names,
                    "lastMessage" to preview,
                    "lastSenderId" to uid,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
        }
        batch.set(
            chatRef.collection("messages").document(),
            mapOf(
                "chatId" to chatId,
                "participantIds" to sortedIds,
                "senderId" to uid,
                "senderName" to sender.username.take(40),
                "text" to preview,
                "msgType" to if (isVoice) "VOICE" else "TEXT",
                "durationSec" to durationSec.coerceIn(0L, 600L),
                "createdAt" to FieldValue.serverTimestamp()
            )
        )
        batch.commit().await()
        Result.success(chatId)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.WRITE, "chats")
        Result.failure(e)
    }

    fun observeEvents(uid: String): Flow<List<AppEvent>> = flow {
        emitAll(
            db.collection("events").whereEqualTo("active", true).snapshots()
                .map { it.toObjects(AppEvent::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .wrapError(OperationType.LIST, "events", uid)
        )
    }

    fun observeAnnouncements(uid: String): Flow<List<Announcement>> = flow {
        emitAll(
            db.collection("announcements").whereEqualTo("active", true).snapshots()
                .map { snap ->
                    snap.toObjects(Announcement::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .wrapError(OperationType.LIST, "announcements", uid)
        )
    }

    suspend fun submitReport(targetId: String, targetType: String, reason: String): Result<Unit> = try {
        val uid = requireUserId()
        db.collection("reports").document().set(
            mapOf(
                "reporterId" to uid,
                "targetId" to targetId,
                "targetType" to targetType,
                "reason" to reason.trim().take(300),
                "status" to "OPEN",
                "adminNote" to "",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.CREATE, "reports")
        Result.failure(e)
    }

    // --- ADMIN DASHBOARD OPERATIONS (AKKI_ADMIN verified on server rules) ---
    fun observeAllRechargeOrdersForAdmin(uid: String): Flow<List<RechargeOrder>> = flow {
        emitAll(
            db.collection("recharge_orders").snapshots()
                .map { snap ->
                    snap.toObjects(RechargeOrder::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .wrapError(OperationType.LIST, "recharge_orders", uid)
        )
    }

    fun observeAllReportsForAdmin(uid: String): Flow<List<Report>> = flow {
        emitAll(
            db.collection("reports").snapshots()
                .map { snap ->
                    snap.toObjects(Report::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .wrapError(OperationType.LIST, "reports", uid)
        )
    }

    suspend fun adminVerifyAndCreditRecharge(order: RechargeOrder, approve: Boolean): Result<Unit> = try {
        val adminUid = requireUserId()
        val orderRef = db.collection("recharge_orders").document(order.id)
        val batch = db.batch()
        batch.update(
            orderRef,
            mapOf(
                "status" to if (approve) "VERIFIED" else "REJECTED",
                "verifiedBy" to "AKKI_ADMIN ($adminUid)",
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )
        if (approve) {
            val walletRef = db.collection("wallets").document(order.userId)
            val snap = walletRef.get().await()
            val curCoins = snap.getLong("coins") ?: 0L
            val curInr = snap.getLong("totalRechargedInr") ?: 0L
            batch.update(
                walletRef,
                mapOf(
                    "coins" to curCoins + order.coinsAmount,
                    "totalRechargedInr" to curInr + order.amountInr,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.set(
                db.collection("transactions").document(),
                mapOf(
                    "senderId" to adminUid,
                    "senderName" to "AKKI_ADMIN",
                    "receiverId" to order.userId,
                    "receiverName" to order.userName,
                    "type" to "RECHARGE_CREDIT",
                    "amountCoins" to order.coinsAmount,
                    "amountDiamonds" to 0L,
                    "giftId" to order.packageId,
                    "giftName" to order.packageLabel,
                    "quantity" to 1L,
                    "contextId" to order.id,
                    "note" to "Verified INR ${order.amountInr} (${order.utrReference})",
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.set(
                db.collection("users").document(order.userId).collection("notifications").document(),
                mapOf(
                    "userId" to order.userId,
                    "type" to "RECHARGE",
                    "title" to "Coins Credited!",
                    "body" to "${order.coinsAmount} Coins added to your ADDA Wallet.",
                    "read" to false,
                    "targetId" to order.id,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
        }
        batch.commit().await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.UPDATE, "recharge_orders/${order.id}")
        Result.failure(e)
    }

    suspend fun adminToggleUserModeration(targetUser: UserProfile, verifyToggle: Boolean, banToggle: Boolean): Result<Unit> = try {
        requireUserId()
        db.collection("users").document(targetUser.userId).update(
            mapOf(
                "verified" to if (verifyToggle) !targetUser.verified else targetUser.verified,
                "banned" to if (banToggle) !targetUser.banned else targetUser.banned,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.UPDATE, "users/${targetUser.userId}")
        Result.failure(e)
    }

    suspend fun adminUpdateGiftPrice(gift: GiftItem, newPriceCoins: Long): Result<Unit> = try {
        requireUserId()
        val safePrice = newPriceCoins.coerceIn(1L, 1_000_000L)
        db.collection("gifts").document(gift.id).set(
            mapOf(
                "name" to gift.name,
                "nameHi" to gift.nameHi,
                "category" to gift.category,
                "priceCoins" to safePrice,
                "diamondValue" to (safePrice / 2L).coerceIn(1L, 1_000_000L),
                "animationType" to gift.animationType,
                "iconKey" to gift.iconKey,
                "active" to gift.active,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.UPDATE, "gifts/${gift.id}")
        Result.failure(e)
    }

    suspend fun adminCreateGift(
        name: String,
        nameHi: String,
        category: String,
        priceCoins: Long,
        animationType: String,
        iconKey: String
    ): Result<Unit> = try {
        requireUserId()
        val cleanName = name.trim().take(50).ifBlank { "Royal Gift" }
        val cleanNameHi = nameHi.trim().take(60).ifBlank { cleanName }
        val safePrice = priceCoins.coerceIn(1L, 1_000_000L)
        val safeDiamonds = ((safePrice * 2L) / 3L).coerceIn(1L, 1_000_000L)
        val docRef = db.collection("gifts").document()
        docRef.set(
            mapOf(
                "name" to cleanName,
                "nameHi" to cleanNameHi,
                "category" to category,
                "priceCoins" to safePrice,
                "diamondValue" to safeDiamonds,
                "animationType" to animationType,
                "iconKey" to iconKey.trim().take(40).ifBlank { "crown" },
                "active" to true,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.CREATE, "gifts")
        Result.failure(e)
    }

    suspend fun adminPublishAnnouncement(title: String, body: String): Result<Unit> = try {
        requireUserId()
        db.collection("announcements").document().set(
            mapOf(
                "title" to title.trim().take(80),
                "body" to body.trim().take(400),
                "createdBy" to "AKKI_ADMIN",
                "active" to true,
                "createdAt" to FieldValue.serverTimestamp()
            )
        ).await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.CREATE, "announcements")
        Result.failure(e)
    }

    suspend fun adminCreateEvent(title: String, titleHi: String, description: String, rewardCoins: Long): Result<Unit> = try {
        requireUserId()
        db.collection("events").document().set(
            mapOf(
                "title" to title.trim().take(80),
                "titleHi" to titleHi.trim().take(80),
                "description" to description.trim().take(250),
                "eventType" to "FESTIVAL",
                "rewardCoins" to rewardCoins.coerceAtLeast(0L),
                "active" to true,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.CREATE, "events")
        Result.failure(e)
    }

    suspend fun adminResolveReport(report: Report, note: String): Result<Unit> = try {
        requireUserId()
        db.collection("reports").document(report.id).update(
            mapOf(
                "status" to "RESOLVED",
                "adminNote" to note.trim().take(300),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.UPDATE, "reports/${report.id}")
        Result.failure(e)
    }
}
