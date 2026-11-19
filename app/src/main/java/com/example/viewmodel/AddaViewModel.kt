package com.example.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.VoiceAudioEngine
import com.example.data.AddaRepository
import com.example.data.RoomsAndAddaRepository
import com.example.localization.AppLang
import com.example.localization.LanguageStore
import com.example.model.AddaGroup
import com.example.model.AdminRegistry
import com.example.model.Agency
import com.example.model.Announcement
import com.example.model.AppEvent
import com.example.model.Family
import com.example.model.FriendRequest
import com.example.model.Friendship
import com.example.model.GiftAnimationEvent
import com.example.model.GiftCatalogSeed
import com.example.model.GiftItem
import com.example.model.PkBattle
import com.example.model.PrivateChat
import com.example.model.PrivateMessage
import com.example.model.RechargeOrder
import com.example.model.RechargePackage
import com.example.model.Report
import com.example.model.RoomMember
import com.example.model.RoomMessage
import com.example.model.RoomSeat
import com.example.model.UserMission
import com.example.model.UserNotification
import com.example.model.UserPrivateAccount
import com.example.model.UserProfile
import com.example.model.VipPlan
import com.example.model.VoiceRoom
import com.example.model.Wallet
import com.example.model.WalletTransaction
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

@OptIn(ExperimentalCoroutinesApi::class)
class AddaViewModel(
    private val addaRepo: AddaRepository,
    private val roomsRepo: RoomsAndAddaRepository,
    private val languageStore: LanguageStore,
    val audioEngine: VoiceAudioEngine,
    val currentUserId: String
) : ViewModel() {

    private fun <T> Flow<T>.toUiState(label: String): StateFlow<UiState<T>> =
        this.map<T, UiState<T>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val root = firestoreError ?: error
                Log.w(TAG, "Error observing $label", root)
                emit(UiState.Error(root.message ?: "Failed to load $label"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = UiState.Loading
            )

    val language: StateFlow<AppLang> = languageStore.languageFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), AppLang.HI)

    val soundFxEnabled: StateFlow<Boolean> = languageStore.soundFxFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), true)

    val myProfile: StateFlow<UiState<UserProfile?>> =
        addaRepo.observeMyProfile(currentUserId).toUiState("profile")

    val myPrivateAccount: StateFlow<UiState<UserPrivateAccount?>> =
        addaRepo.observePrivateAccount(currentUserId).toUiState("private_account")

    val adminRegistry: StateFlow<AdminRegistry?> =
        addaRepo.observeAdminStatus(currentUserId)
            .catch { emit(null) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    val myWallet: StateFlow<UiState<Wallet?>> =
        addaRepo.observeWallet(currentUserId).toUiState("wallet")

    val gifts: StateFlow<List<GiftItem>> =
        addaRepo.observeGifts(currentUserId)
            .catch { emit(GiftCatalogSeed.defaultGifts) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), GiftCatalogSeed.defaultGifts)

    val directoryUsers: StateFlow<UiState<List<UserProfile>>> =
        addaRepo.observeAllUsersForDirectory(currentUserId).toUiState("directory")

    val notifications: StateFlow<List<UserNotification>> =
        addaRepo.observeNotifications(currentUserId)
            .catch { emit(emptyList()) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val missions: StateFlow<List<UserMission>> =
        addaRepo.observeMissions(currentUserId)
            .catch { emit(emptyList()) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val transactions: StateFlow<List<WalletTransaction>> =
        addaRepo.observeTransactions(currentUserId)
            .catch { emit(emptyList()) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val myRechargeOrders: StateFlow<List<RechargeOrder>> =
        addaRepo.observeMyRechargeOrders(currentUserId)
            .catch { emit(emptyList()) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val voiceRooms: StateFlow<UiState<List<VoiceRoom>>> =
        roomsRepo.observeVoiceRooms(currentUserId).toUiState("voice_rooms")

    val addaGroups: StateFlow<UiState<List<AddaGroup>>> =
        roomsRepo.observeAddaGroups(currentUserId).toUiState("adda_groups")

    val pkBattles: StateFlow<List<PkBattle>> =
        roomsRepo.observePkBattles(currentUserId)
            .catch { emit(emptyList()) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val families: StateFlow<List<Family>> =
        roomsRepo.observeFamilies(currentUserId)
            .catch { emit(emptyList()) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val agencies: StateFlow<List<Agency>> =
        roomsRepo.observeAgencies(currentUserId)
            .catch { emit(emptyList()) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val incomingRequests: StateFlow<List<FriendRequest>> =
        roomsRepo.observeIncomingRequests(currentUserId)
            .catch { emit(emptyList()) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val friendships: StateFlow<List<Friendship>> =
        roomsRepo.observeFriendships(currentUserId)
            .catch { emit(emptyList()) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val privateChats: StateFlow<List<PrivateChat>> =
        roomsRepo.observeChats(currentUserId)
            .catch { emit(emptyList()) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val events: StateFlow<List<AppEvent>> =
        roomsRepo.observeEvents(currentUserId)
            .catch { emit(emptyList()) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val announcements: StateFlow<List<Announcement>> =
        roomsRepo.observeAnnouncements(currentUserId)
            .catch { emit(emptyList()) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    // Admin streams (active only when verified as AKKI_ADMIN)
    val adminOrders: StateFlow<List<RechargeOrder>> = adminRegistry
        .flatMapLatest { reg ->
            if (reg != null) roomsRepo.observeAllRechargeOrdersForAdmin(currentUserId).catch { emit(emptyList()) }
            else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val adminReports: StateFlow<List<Report>> = adminRegistry
        .flatMapLatest { reg ->
            if (reg != null) roomsRepo.observeAllReportsForAdmin(currentUserId).catch { emit(emptyList()) }
            else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    // Active Voice Room / ADDA / Private Chat Focus
    private val _activeRoomId = MutableStateFlow<String?>(null)
    val activeRoomId: StateFlow<String?> = _activeRoomId.asStateFlow()

    private val _activeAddaId = MutableStateFlow<String?>(null)
    val activeAddaId: StateFlow<String?> = _activeAddaId.asStateFlow()

    private val _activeChatId = MutableStateFlow<String?>(null)
    val activeChatId: StateFlow<String?> = _activeChatId.asStateFlow()

    val activeRoomSeats: StateFlow<List<RoomSeat>> = _activeRoomId
        .flatMapLatest { id ->
            if (id.isNullOrBlank()) flowOf(emptyList())
            else roomsRepo.observeRoomSeats(id, currentUserId).catch { emit(emptyList()) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val activeRoomMessages: StateFlow<List<RoomMessage>> = _activeRoomId
        .flatMapLatest { id ->
            if (id.isNullOrBlank()) flowOf(emptyList())
            else roomsRepo.observeRoomMessages(id, currentUserId).catch { emit(emptyList()) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val activeRoomMembers: StateFlow<List<RoomMember>> = _activeRoomId
        .flatMapLatest { id ->
            if (id.isNullOrBlank()) flowOf(emptyList())
            else roomsRepo.observeRoomMembers(id, currentUserId).catch { emit(emptyList()) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val activeAddaSeats: StateFlow<List<RoomSeat>> = _activeAddaId
        .flatMapLatest { id ->
            if (id.isNullOrBlank()) flowOf(emptyList())
            else roomsRepo.observeAddaSeats(id, currentUserId).catch { emit(emptyList()) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val activeAddaMessages: StateFlow<List<RoomMessage>> = _activeAddaId
        .flatMapLatest { id ->
            if (id.isNullOrBlank()) flowOf(emptyList())
            else roomsRepo.observeAddaMessages(id, currentUserId).catch { emit(emptyList()) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val activeAddaMembers: StateFlow<List<RoomMember>> = _activeAddaId
        .flatMapLatest { id ->
            if (id.isNullOrBlank()) flowOf(emptyList())
            else roomsRepo.observeAddaMembers(id, currentUserId).catch { emit(emptyList()) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val activeChatMessages: StateFlow<List<PrivateMessage>> = _activeChatId
        .flatMapLatest { id ->
            if (id.isNullOrBlank()) flowOf(emptyList())
            else roomsRepo.observePrivateMessages(id, currentUserId).catch { emit(emptyList()) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    private val _giftOverlayEvents = MutableSharedFlow<GiftAnimationEvent>(extraBufferCapacity = 8)
    val giftOverlayEvents: SharedFlow<GiftAnimationEvent> = _giftOverlayEvents.asSharedFlow()

    private val _statusMessages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val statusMessages: SharedFlow<String> = _statusMessages.asSharedFlow()

    private val _needsProfileOnboarding = MutableStateFlow(false)
    val needsProfileOnboarding: StateFlow<Boolean> = _needsProfileOnboarding.asStateFlow()

    init {
        val user = Firebase.auth.currentUser
        if (user != null) {
            viewModelScope.launch {
                val res = addaRepo.ensureUserInitialized(
                    uid = user.uid,
                    displayName = user.displayName ?: "ADDA Star",
                    email = user.email ?: "",
                    photoUrl = user.photoUrl?.toString() ?: ""
                )
                if (res.getOrDefault(false)) {
                    _needsProfileOnboarding.value = true
                }
            }
        }
    }

    fun dismissProfileOnboarding() {
        _needsProfileOnboarding.value = false
    }

    private fun currentProfileOrNull(): UserProfile? =
        (myProfile.value as? UiState.Success)?.data

    fun toggleLanguage(lang: AppLang) {
        viewModelScope.launch {
            languageStore.setLanguage(lang)
            addaRepo.updatePrivateSettings(
                phone = (myPrivateAccount.value as? UiState.Success)?.data?.phone ?: "",
                preferredLang = lang.code,
                securityPinSet = (myPrivateAccount.value as? UiState.Success)?.data?.securityPinSet ?: false,
                privacyDiscoverable = (myPrivateAccount.value as? UiState.Success)?.data?.privacyDiscoverable ?: true
            )
        }
    }

    fun toggleSoundFx(enabled: Boolean) {
        viewModelScope.launch { languageStore.setSoundFx(enabled) }
    }

    fun enterVoiceRoom(roomId: String) {
        _activeRoomId.value = roomId
        currentProfileOrNull()?.let { prof ->
            viewModelScope.launch { roomsRepo.joinRoomMember(roomId, prof, isAdda = false) }
        }
    }

    fun exitVoiceRoom() {
        audioEngine.stopMicCapture()
        _activeRoomId.value = null
    }

    fun enterAddaGroup(addaId: String) {
        _activeAddaId.value = addaId
        currentProfileOrNull()?.let { prof ->
            viewModelScope.launch { roomsRepo.joinRoomMember(addaId, prof, isAdda = true) }
        }
    }

    fun exitAddaGroup() {
        audioEngine.stopMicCapture()
        _activeAddaId.value = null
    }

    fun openPrivateChat(chatId: String) {
        _activeChatId.value = chatId
    }

    fun closePrivateChat() {
        _activeChatId.value = null
    }

    fun saveProfile(username: String, bio: String, age: Long, gender: String, avatarUrl: String? = null) {
        viewModelScope.launch {
            val res = addaRepo.updateProfile(username, bio, age, gender, avatarUrl)
            _statusMessages.tryEmit(if (res.isSuccess) "Profile updated!" else (res.exceptionOrNull()?.message ?: "Update failed"))
        }
    }

    fun savePrivateAccount(phone: String, securityPinSet: Boolean, privacyDiscoverable: Boolean) {
        viewModelScope.launch {
            val res = addaRepo.updatePrivateSettings(phone, language.value.code, securityPinSet, privacyDiscoverable)
            _statusMessages.tryEmit(if (res.isSuccess) "Account settings saved" else (res.exceptionOrNull()?.message ?: "Failed"))
        }
    }

    fun createVoiceRoom(title: String, topic: String, roomType: String, pin: String, seats: Int, themeId: String, announcement: String) {
        val prof = currentProfileOrNull() ?: return
        viewModelScope.launch {
            val res = roomsRepo.createVoiceRoom(prof, title, topic, roomType, pin, seats, themeId, announcement)
            res.onSuccess { newId ->
                _activeRoomId.value = newId
                _statusMessages.tryEmit("Voice Room Live!")
            }.onFailure {
                _statusMessages.tryEmit(it.message ?: "Failed to create room")
            }
        }
    }

    fun createAddaGroup(name: String, description: String, rules: String, isPublic: Boolean, seats: Int, themeId: String) {
        val prof = currentProfileOrNull() ?: return
        viewModelScope.launch {
            val res = roomsRepo.createAddaGroup(prof, name, description, rules, isPublic, seats, themeId)
            res.onSuccess { newId ->
                _activeAddaId.value = newId
                _statusMessages.tryEmit("ADDA Group Created!")
            }.onFailure {
                _statusMessages.tryEmit(it.message ?: "Failed to create ADDA")
            }
        }
    }

    fun takeSeat(parentId: String, seat: RoomSeat, isAdda: Boolean) {
        val prof = currentProfileOrNull() ?: return
        if (seat.locked) {
            _statusMessages.tryEmit("Seat is locked by owner")
            return
        }
        viewModelScope.launch {
            val res = roomsRepo.updateSeat(
                parentId = parentId,
                seatIndex = seat.seatIndex.toInt(),
                occupantId = prof.userId,
                occupantName = prof.username,
                occupantVip = prof.vipTier,
                muted = false,
                locked = false,
                speaking = audioEngine.isCapturing.value,
                speakingLevel = audioEngine.speakingLevel.value,
                isAdda = isAdda
            )
            if (res.isFailure) _statusMessages.tryEmit(res.exceptionOrNull()?.message ?: "Failed to take seat")
        }
    }

    fun leaveSeat(parentId: String, seat: RoomSeat, isAdda: Boolean) {
        audioEngine.stopMicCapture()
        viewModelScope.launch {
            roomsRepo.updateSeat(
                parentId = parentId,
                seatIndex = seat.seatIndex.toInt(),
                occupantId = "",
                occupantName = "",
                occupantVip = "NONE",
                muted = false,
                locked = seat.locked,
                speaking = false,
                speakingLevel = 0,
                isAdda = isAdda
            )
        }
    }

    fun toggleSeatMuteOrLock(parentId: String, seat: RoomSeat, toggleMute: Boolean, toggleLock: Boolean, kickUser: Boolean, isAdda: Boolean) {
        viewModelScope.launch {
            roomsRepo.updateSeat(
                parentId = parentId,
                seatIndex = seat.seatIndex.toInt(),
                occupantId = if (kickUser) "" else seat.occupantId,
                occupantName = if (kickUser) "" else seat.occupantName,
                occupantVip = if (kickUser) "NONE" else seat.occupantVip,
                muted = if (toggleMute) !seat.muted else seat.muted,
                locked = if (toggleLock) !seat.locked else seat.locked,
                speaking = if (kickUser || (toggleMute && !seat.muted)) false else seat.speaking,
                speakingLevel = if (kickUser) 0 else seat.speakingLevel.toInt(),
                isAdda = isAdda
            )
        }
    }

    fun toggleMyMic(parentId: String, mySeat: RoomSeat?, hasAudioPermission: Boolean, isAdda: Boolean) {
        if (audioEngine.isCapturing.value) {
            audioEngine.stopMicCapture()
            if (mySeat != null) {
                viewModelScope.launch {
                    roomsRepo.updateSeat(parentId, mySeat.seatIndex.toInt(), mySeat.occupantId, mySeat.occupantName, mySeat.occupantVip, true, mySeat.locked, false, 0, isAdda)
                }
            }
        } else if (hasAudioPermission) {
            audioEngine.startMicCapture(viewModelScope)
            if (mySeat != null) {
                viewModelScope.launch {
                    roomsRepo.updateSeat(parentId, mySeat.seatIndex.toInt(), mySeat.occupantId, mySeat.occupantName, mySeat.occupantVip, false, mySeat.locked, true, 40, isAdda)
                }
            }
        }
    }

    fun sendRoomChat(parentId: String, text: String, isAdda: Boolean = false, isFamily: Boolean = false) {
        val prof = currentProfileOrNull() ?: return
        viewModelScope.launch {
            val res = roomsRepo.sendRoomMessage(parentId, prof, text, isAdda, isFamily)
            if (res.isFailure) _statusMessages.tryEmit(res.exceptionOrNull()?.message ?: "Message failed")
        }
    }

    fun sendGift(
        receiverId: String,
        receiverName: String,
        gift: GiftItem,
        quantity: Int,
        contextType: String,
        contextId: String,
        pkBattleId: String? = null,
        pkTeam: String? = null
    ) {
        val prof = currentProfileOrNull() ?: return
        viewModelScope.launch {
            val prevLevel = prof.level
            val res = addaRepo.sendGiftAtomic(prof, receiverId, receiverName, gift, quantity, contextType, contextId, pkBattleId, pkTeam)
            res.onSuccess {
                audioEngine.playGiftSound(gift.animationType, soundFxEnabled.value)
                _giftOverlayEvents.tryEmit(
                    GiftAnimationEvent(gift, prof.username, receiverName.ifBlank { "Stage" }, quantity)
                )
                val newXp = prof.xp + (gift.priceCoins * quantity)
                val newLevel = (1L + newXp / 500L).coerceIn(1L, 100L)
                if (newLevel > prevLevel) {
                    audioEngine.playLevelUpSound(soundFxEnabled.value)
                    _statusMessages.tryEmit("Level Up! Level $newLevel")
                }
            }.onFailure {
                _statusMessages.tryEmit(it.message ?: "Gift failed")
            }
        }
    }

    fun previewGiftAnimation(gift: GiftItem, receiverLabel: String = "Stage Preview") {
        val sender = currentProfileOrNull()?.username ?: "You"
        audioEngine.playGiftSound(gift.animationType, soundFxEnabled.value)
        _giftOverlayEvents.tryEmit(
            GiftAnimationEvent(gift, sender, receiverLabel.ifBlank { "Stage Preview" }, 1)
        )
    }

    fun startPkBattle(roomId: String, redTeam: String, blueTeam: String) {
        viewModelScope.launch {
            val res = roomsRepo.startPkBattle(roomId, redTeam, blueTeam)
            _statusMessages.tryEmit(if (res.isSuccess) "PK Battle Started!" else (res.exceptionOrNull()?.message ?: "PK failed"))
        }
    }

    fun finishPkBattle(battle: PkBattle) {
        viewModelScope.launch {
            val res = roomsRepo.finishPkBattle(battle)
            _statusMessages.tryEmit(if (res.isSuccess) "PK Battle Finished!" else (res.exceptionOrNull()?.message ?: "Error"))
        }
    }

    fun updateRoomSettings(roomId: String, announcement: String, themeId: String) {
        viewModelScope.launch {
            val res = roomsRepo.updateRoomSettings(roomId, announcement, themeId)
            _statusMessages.tryEmit(if (res.isSuccess) "Room updated!" else (res.exceptionOrNull()?.message ?: "Error"))
        }
    }

    fun moderateAddaMember(adda: AddaGroup, targetUid: String, action: String) {
        viewModelScope.launch {
            val res = roomsRepo.toggleAddaModeration(adda, targetUid, action)
            _statusMessages.tryEmit(if (res.isSuccess) "ADDA Updated ($action)" else (res.exceptionOrNull()?.message ?: "Permission denied"))
        }
    }

    fun submitRecharge(pkg: RechargePackage, utr: String, method: String) {
        val prof = currentProfileOrNull() ?: return
        viewModelScope.launch {
            val res = addaRepo.submitRechargeOrder(prof.username, pkg, utr, method)
            _statusMessages.tryEmit(
                if (res.isSuccess) "Order submitted for backend verification!"
                else (res.exceptionOrNull()?.message ?: "Order failed")
            )
        }
    }

    fun purchaseVip(plan: VipPlan) {
        val prof = currentProfileOrNull() ?: return
        viewModelScope.launch {
            val res = addaRepo.purchaseVipPlan(prof, plan)
            if (res.isSuccess) audioEngine.playLevelUpSound(soundFxEnabled.value)
            _statusMessages.tryEmit(if (res.isSuccess) "${plan.tier} Activated!" else (res.exceptionOrNull()?.message ?: "Purchase failed"))
        }
    }

    fun claimMission(mission: UserMission) {
        val prof = currentProfileOrNull() ?: return
        viewModelScope.launch {
            val res = addaRepo.markMissionCompletedAndClaim(prof, mission)
            if (res.isSuccess) audioEngine.playLevelUpSound(soundFxEnabled.value)
            _statusMessages.tryEmit(if (res.isSuccess) "Mission XP Claimed!" else (res.exceptionOrNull()?.message ?: "Claim failed"))
        }
    }

    fun sendFriendRequest(to: UserProfile) {
        val prof = currentProfileOrNull() ?: return
        viewModelScope.launch {
            val res = roomsRepo.sendFriendRequest(prof, to)
            _statusMessages.tryEmit(if (res.isSuccess) "Friend Request Sent!" else (res.exceptionOrNull()?.message ?: "Failed"))
        }
    }

    fun respondFriendRequest(req: FriendRequest, accept: Boolean) {
        viewModelScope.launch {
            val res = roomsRepo.respondFriendRequest(req, accept)
            _statusMessages.tryEmit(if (res.isSuccess) (if (accept) "Friend Added!" else "Request Declined") else "Failed")
        }
    }

    fun sendPrivateMessage(otherUid: String, otherName: String, text: String, isVoice: Boolean = false, durationSec: Long = 0L) {
        val prof = currentProfileOrNull() ?: return
        viewModelScope.launch {
            val res = roomsRepo.sendPrivateMessage(prof, otherUid, otherName, text, isVoice, durationSec)
            res.onSuccess { chatId -> _activeChatId.value = chatId }
                .onFailure { _statusMessages.tryEmit(it.message ?: "Message failed") }
        }
    }

    fun createFamily(name: String, motto: String) {
        val prof = currentProfileOrNull() ?: return
        viewModelScope.launch {
            val res = roomsRepo.createFamily(prof, name, motto)
            _statusMessages.tryEmit(if (res.isSuccess) "Family Created!" else (res.exceptionOrNull()?.message ?: "Failed"))
        }
    }

    fun joinFamily(family: Family) {
        val prof = currentProfileOrNull() ?: return
        viewModelScope.launch {
            val res = roomsRepo.joinFamily(family, prof)
            _statusMessages.tryEmit(if (res.isSuccess) "Joined ${family.name}!" else (res.exceptionOrNull()?.message ?: "Failed"))
        }
    }

    fun createAgency(name: String, desc: String, commission: Long) {
        val prof = currentProfileOrNull() ?: return
        viewModelScope.launch {
            val res = roomsRepo.createAgency(prof, name, desc, commission)
            _statusMessages.tryEmit(if (res.isSuccess) "Agency Registered!" else (res.exceptionOrNull()?.message ?: "Failed"))
        }
    }

    fun applyAsHost(agency: Agency) {
        val prof = currentProfileOrNull() ?: return
        viewModelScope.launch {
            val res = roomsRepo.applyAsHost(agency, prof)
            _statusMessages.tryEmit(if (res.isSuccess) "Host Registered in ${agency.name}!" else (res.exceptionOrNull()?.message ?: "Failed"))
        }
    }

    fun submitReport(targetId: String, targetType: String, reason: String) {
        viewModelScope.launch {
            val res = roomsRepo.submitReport(targetId, targetType, reason)
            _statusMessages.tryEmit(if (res.isSuccess) "Report submitted to AKKI_ADMIN" else (res.exceptionOrNull()?.message ?: "Failed"))
        }
    }

    fun markNotificationRead(notifId: String) {
        viewModelScope.launch { addaRepo.markNotificationRead(notifId) }
    }

    // Admin Dashboard actions
    fun adminVerifyOrder(order: RechargeOrder, approve: Boolean) {
        viewModelScope.launch {
            val res = roomsRepo.adminVerifyAndCreditRecharge(order, approve)
            _statusMessages.tryEmit(if (res.isSuccess) (if (approve) "Coins Credited!" else "Order Rejected") else (res.exceptionOrNull()?.message ?: "Admin action failed"))
        }
    }

    fun adminModerateUser(user: UserProfile, verifyToggle: Boolean, banToggle: Boolean) {
        viewModelScope.launch {
            val res = roomsRepo.adminToggleUserModeration(user, verifyToggle, banToggle)
            _statusMessages.tryEmit(if (res.isSuccess) "User updated" else (res.exceptionOrNull()?.message ?: "Failed"))
        }
    }

    fun adminSetGiftPrice(gift: GiftItem, newPrice: Long) {
        viewModelScope.launch {
            val res = roomsRepo.adminUpdateGiftPrice(gift, newPrice)
            _statusMessages.tryEmit(if (res.isSuccess) "Gift price updated!" else (res.exceptionOrNull()?.message ?: "Failed"))
        }
    }

    fun adminCreateGift(
        name: String,
        nameHi: String,
        category: String,
        priceCoins: Long,
        animationType: String,
        iconKey: String
    ) {
        viewModelScope.launch {
            val res = roomsRepo.adminCreateGift(name, nameHi, category, priceCoins, animationType, iconKey)
            _statusMessages.tryEmit(if (res.isSuccess) "New Gift Added!" else (res.exceptionOrNull()?.message ?: "Failed"))
        }
    }

    fun adminPublishAnnouncement(title: String, body: String) {
        viewModelScope.launch {
            val res = roomsRepo.adminPublishAnnouncement(title, body)
            _statusMessages.tryEmit(if (res.isSuccess) "Announcement Published!" else (res.exceptionOrNull()?.message ?: "Failed"))
        }
    }

    fun adminCreateEvent(title: String, titleHi: String, desc: String, rewardCoins: Long) {
        viewModelScope.launch {
            val res = roomsRepo.adminCreateEvent(title, titleHi, desc, rewardCoins)
            _statusMessages.tryEmit(if (res.isSuccess) "Event Created!" else (res.exceptionOrNull()?.message ?: "Failed"))
        }
    }

    fun adminResolveReport(report: Report, note: String) {
        viewModelScope.launch {
            val res = roomsRepo.adminResolveReport(report, note)
            _statusMessages.tryEmit(if (res.isSuccess) "Report Resolved" else (res.exceptionOrNull()?.message ?: "Failed"))
        }
    }

    suspend fun prepareSignOutCleanup() {
        audioEngine.stopMicCapture()
        addaRepo.setOfflineOnSignOut(currentUserId)
    }

    override fun onCleared() {
        super.onCleared()
        audioEngine.release()
    }

    private companion object {
        const val TAG = "AddaVM"
    }
}
