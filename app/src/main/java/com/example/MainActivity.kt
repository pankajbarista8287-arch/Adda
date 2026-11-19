package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.audio.VoiceAudioEngine
import com.example.data.AddaRepository
import com.example.data.RoomsAndAddaRepository
import com.example.localization.AppLang
import com.example.localization.EnglishStrings
import com.example.localization.HindiStrings
import com.example.localization.LanguageStore
import com.example.localization.LocalAddaStrings
import com.example.model.GiftAnimationEvent
import com.example.ui.components.AddaCosmicBackground
import com.example.ui.components.AddaSplashOverlay
import com.example.ui.components.GiftOverlayPlayer
import com.example.ui.screens.AddaGroupDetailScreen
import com.example.ui.screens.AddaGroupsScreen
import com.example.ui.screens.AdminDashboardScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.HubAndRankingsScreen
import com.example.ui.screens.ProfileEditorSheetContent
import com.example.ui.screens.SocialAndChatScreen
import com.example.ui.screens.VoiceRoomScreen
import com.example.ui.screens.WalletAndProfileScreen
import com.example.ui.theme.AddaTheme
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.RoyalGold
import com.example.ui.theme.RoyalPurpleAccent
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.AddaViewModel
import com.example.viewmodel.UiState
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { auth -> trySend(auth.currentUser) }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AddaTheme {
                AddaRootApp()
            }
        }
    }
}

@Composable
fun AddaRootApp() {
    val context = LocalContext.current
    val languageStore = remember(context) { LanguageStore(context.applicationContext) }
    val currentLang by languageStore.languageFlow.collectAsStateWithLifecycle(initialValue = AppLang.HI)
    val scope = rememberCoroutineScope()
    val strings = if (currentLang == AppLang.HI) HindiStrings else EnglishStrings

    var showSplash by remember { mutableStateOf(true) }
    var skipAutoSignIn by remember { mutableStateOf(false) }
    val currentUser by Firebase.auth.authStateFlow().collectAsStateWithLifecycle(initialValue = Firebase.auth.currentUser)

    CompositionLocalProvider(LocalAddaStrings provides strings) {
        Box(modifier = Modifier.fillMaxSize()) {
            val user = currentUser
            if (user == null) {
                AuthScreen(
                    currentLang = currentLang,
                    skipAutoSignIn = skipAutoSignIn,
                    onToggleLang = { lang -> scope.launch { languageStore.setLanguage(lang) } },
                    onAuthSuccess = { skipAutoSignIn = false }
                )
            } else {
                val uid = user.uid
                key(uid) {
                    val vm: AddaViewModel = viewModel(
                        key = uid,
                        factory = viewModelFactory {
                            initializer {
                                val app = checkNotNull(this[APPLICATION_KEY])
                                val databaseId = app.getString(R.string.firestore_database_id)
                                val db = FirebaseFirestore.getInstance(databaseId)
                                AddaViewModel(
                                    addaRepo = AddaRepository(db),
                                    roomsRepo = RoomsAndAddaRepository(db),
                                    languageStore = LanguageStore(app),
                                    audioEngine = VoiceAudioEngine(app),
                                    currentUserId = uid
                                )
                            }
                        }
                    )
                    MainAddaScaffold(
                        vm = vm,
                        currentLang = currentLang,
                        onSignedOut = { skipAutoSignIn = true }
                    )
                }
            }

            if (showSplash) {
                AddaSplashOverlay(onFinished = { showSplash = false })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainAddaScaffold(
    vm: AddaViewModel,
    currentLang: AppLang,
    onSignedOut: () -> Unit
) {
    val strings = LocalAddaStrings.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val profileState by vm.myProfile.collectAsStateWithLifecycle()
    val privateState by vm.myPrivateAccount.collectAsStateWithLifecycle()
    val walletState by vm.myWallet.collectAsStateWithLifecycle()
    val adminRegistry by vm.adminRegistry.collectAsStateWithLifecycle()
    val roomsState by vm.voiceRooms.collectAsStateWithLifecycle()
    val addasState by vm.addaGroups.collectAsStateWithLifecycle()
    val directoryState by vm.directoryUsers.collectAsStateWithLifecycle()

    val gifts by vm.gifts.collectAsStateWithLifecycle()
    val pkBattles by vm.pkBattles.collectAsStateWithLifecycle()
    val families by vm.families.collectAsStateWithLifecycle()
    val agencies by vm.agencies.collectAsStateWithLifecycle()
    val announcements by vm.announcements.collectAsStateWithLifecycle()
    val events by vm.events.collectAsStateWithLifecycle()
    val missions by vm.missions.collectAsStateWithLifecycle()
    val notifications by vm.notifications.collectAsStateWithLifecycle()
    val requests by vm.incomingRequests.collectAsStateWithLifecycle()
    val friendships by vm.friendships.collectAsStateWithLifecycle()
    val chats by vm.privateChats.collectAsStateWithLifecycle()
    val orders by vm.myRechargeOrders.collectAsStateWithLifecycle()
    val transactions by vm.transactions.collectAsStateWithLifecycle()
    val adminOrders by vm.adminOrders.collectAsStateWithLifecycle()
    val adminReports by vm.adminReports.collectAsStateWithLifecycle()
    val soundFxEnabled by vm.soundFxEnabled.collectAsStateWithLifecycle()

    val activeRoomId by vm.activeRoomId.collectAsStateWithLifecycle()
    val activeRoomSeats by vm.activeRoomSeats.collectAsStateWithLifecycle()
    val activeRoomMessages by vm.activeRoomMessages.collectAsStateWithLifecycle()
    val activeRoomMembers by vm.activeRoomMembers.collectAsStateWithLifecycle()

    val activeAddaId by vm.activeAddaId.collectAsStateWithLifecycle()
    val activeAddaSeats by vm.activeAddaSeats.collectAsStateWithLifecycle()
    val activeAddaMessages by vm.activeAddaMessages.collectAsStateWithLifecycle()
    val activeAddaMembers by vm.activeAddaMembers.collectAsStateWithLifecycle()

    val activeChatId by vm.activeChatId.collectAsStateWithLifecycle()
    val activeChatMessages by vm.activeChatMessages.collectAsStateWithLifecycle()

    val isMicCapturing by vm.audioEngine.isCapturing.collectAsStateWithLifecycle()
    val speakingLevel by vm.audioEngine.speakingLevel.collectAsStateWithLifecycle()
    val speakerOn by vm.audioEngine.speakerphoneEnabled.collectAsStateWithLifecycle()

    val me = (profileState as? UiState.Success)?.data
    val myPrivate = (privateState as? UiState.Success)?.data
    val wallet = (walletState as? UiState.Success)?.data
    val rooms = (roomsState as? UiState.Success)?.data.orEmpty()
    val addas = (addasState as? UiState.Success)?.data.orEmpty()
    val users = (directoryState as? UiState.Success)?.data.orEmpty()

    var selectedTab by remember { mutableIntStateOf(0) }
    var showAdminScreen by remember { mutableStateOf(false) }
    var activeGiftAnim by remember { mutableStateOf<GiftAnimationEvent?>(null) }
    val needsProfileOnboarding by vm.needsProfileOnboarding.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        vm.statusMessages.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }
    LaunchedEffect(Unit) {
        vm.giftOverlayEvents.collect { evt ->
            activeGiftAnim = evt
        }
    }

    val activeRoom = rooms.firstOrNull { it.id == activeRoomId }
    val activeAdda = addas.firstOrNull { it.id == activeAddaId }

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            showAdminScreen -> {
                AdminDashboardScreen(
                    adminRegistry = adminRegistry,
                    users = users,
                    orders = adminOrders,
                    gifts = gifts,
                    reports = adminReports,
                    onBack = { showAdminScreen = false },
                    onVerifyOrder = vm::adminVerifyOrder,
                    onModerateUser = vm::adminModerateUser,
                    onUpdateGiftPrice = vm::adminSetGiftPrice,
                    onCreateGift = vm::adminCreateGift,
                    onPublishAnnouncement = vm::adminPublishAnnouncement,
                    onCreateEvent = vm::adminCreateEvent,
                    onResolveReport = vm::adminResolveReport
                )
            }
            activeRoom != null -> {
                val roomPk = pkBattles.firstOrNull { it.roomId == activeRoom.id && it.status == "ACTIVE" }
                VoiceRoomScreen(
                    room = activeRoom,
                    me = me,
                    coinsBalance = wallet?.coins ?: 0L,
                    seats = activeRoomSeats,
                    messages = activeRoomMessages,
                    members = activeRoomMembers,
                    gifts = gifts,
                    activePk = roomPk,
                    lang = currentLang,
                    isMicCapturing = isMicCapturing,
                    speakingLevel = speakingLevel,
                    speakerOn = speakerOn,
                    onExit = vm::exitVoiceRoom,
                    onTakeSeat = { seat -> vm.takeSeat(activeRoom.id, seat, isAdda = false) },
                    onLeaveSeat = { seat -> vm.leaveSeat(activeRoom.id, seat, isAdda = false) },
                    onModerateSeat = { seat, mute, lock, kick -> vm.toggleSeatMuteOrLock(activeRoom.id, seat, mute, lock, kick, isAdda = false) },
                    onToggleMyMic = { mySeat, hasPerm -> vm.toggleMyMic(activeRoom.id, mySeat, hasPerm, isAdda = false) },
                    onToggleSpeaker = vm.audioEngine::toggleSpeakerphone,
                    onSendChat = { txt -> vm.sendRoomChat(activeRoom.id, txt, isAdda = false) },
                    onSendGift = { recId, recName, gift, qty, pkId, pkTeam ->
                        vm.sendGift(recId, recName, gift, qty, "ROOM", activeRoom.id, pkId, pkTeam)
                    },
                    onPreviewGift = vm::previewGiftAnimation,
                    onStartPk = { red, blue -> vm.startPkBattle(activeRoom.id, red, blue) },
                    onFinishPk = vm::finishPkBattle,
                    onUpdateRoomSettings = { ann, th -> vm.updateRoomSettings(activeRoom.id, ann, th) }
                )
            }
            activeAdda != null -> {
                AddaGroupDetailScreen(
                    adda = activeAdda,
                    me = me,
                    coinsBalance = wallet?.coins ?: 0L,
                    seats = activeAddaSeats,
                    messages = activeAddaMessages,
                    members = activeAddaMembers,
                    gifts = gifts,
                    lang = currentLang,
                    isMicCapturing = isMicCapturing,
                    speakingLevel = speakingLevel,
                    onExit = vm::exitAddaGroup,
                    onTakeSeat = { seat -> vm.takeSeat(activeAdda.id, seat, isAdda = true) },
                    onLeaveSeat = { seat -> vm.leaveSeat(activeAdda.id, seat, isAdda = true) },
                    onToggleMyMic = { mySeat, hasPerm -> vm.toggleMyMic(activeAdda.id, mySeat, hasPerm, isAdda = true) },
                    onSendChat = { txt -> vm.sendRoomChat(activeAdda.id, txt, isAdda = true) },
                    onSendGift = { recId, recName, gift, qty ->
                        vm.sendGift(recId, recName, gift, qty, "ADDA", activeAdda.id)
                    },
                    onPreviewGift = vm::previewGiftAnimation,
                    onModerateMember = { targetUid, action -> vm.moderateAddaMember(activeAdda, targetUid, action) }
                )
            }
            else -> {
                val navItems = listOf(
                    strings.navRooms to Icons.Filled.Mic,
                    strings.navAdda to Icons.Filled.Groups,
                    strings.navSocial to Icons.Filled.People,
                    strings.navHub to Icons.Filled.EmojiEvents,
                    strings.navWallet to Icons.Filled.AccountBalanceWallet
                )
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val useRail = maxWidth >= 600.dp
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        snackbarHost = { SnackbarHost(snackbarHostState) },
                        contentWindowInsets = WindowInsets.statusBars,
                        bottomBar = {
                            if (!useRail) {
                                NavigationBar(
                                    containerColor = ObsidianSurface,
                                    modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
                                ) {
                                    navItems.forEachIndexed { idx, (label, icon) ->
                                        NavigationBarItem(
                                            selected = selectedTab == idx,
                                            onClick = { selectedTab = idx },
                                            icon = { Icon(icon, contentDescription = label) },
                                            label = { Text(label, maxLines = 1) },
                                            modifier = Modifier.testTag("nav_tab_$idx"),
                                            colors = NavigationBarItemDefaults.colors(
                                                selectedIconColor = RoyalGold,
                                                selectedTextColor = RoyalGold,
                                                indicatorColor = RoyalPurpleAccent.copy(alpha = 0.35f),
                                                unselectedIconColor = TextSecondary,
                                                unselectedTextColor = TextSecondary
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    ) { innerPadding ->
                        AddaCosmicBackground(modifier = Modifier.padding(innerPadding)) {
                            Row(modifier = Modifier.fillMaxSize()) {
                                if (useRail) {
                                    NavigationRail(containerColor = ObsidianSurface) {
                                        navItems.forEachIndexed { idx, (label, icon) ->
                                            NavigationRailItem(
                                                selected = selectedTab == idx,
                                                onClick = { selectedTab = idx },
                                                icon = { Icon(icon, contentDescription = label) },
                                                label = { Text(label) }
                                            )
                                        }
                                    }
                                }
                                Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                                    when (selectedTab) {
                                        0 -> HomeScreen(
                                            profile = me,
                                            coinsBalance = wallet?.coins ?: 0L,
                                            roomsState = roomsState,
                                            pkBattles = pkBattles,
                                            announcements = announcements,
                                            unreadNotifs = notifications.count { !it.read },
                                            isAdmin = adminRegistry != null,
                                            onOpenRoom = { r -> vm.enterVoiceRoom(r.id) },
                                            onCreateRoom = vm::createVoiceRoom,
                                            onOpenNotifications = { selectedTab = 2 },
                                            onOpenRankings = { selectedTab = 3 },
                                            onOpenWallet = { selectedTab = 4 },
                                            onOpenAdminPanel = { showAdminScreen = true }
                                        )
                                        1 -> AddaGroupsScreen(
                                            groupsState = addasState,
                                            onOpenGroup = { g -> vm.enterAddaGroup(g.id) },
                                            onCreateGroup = vm::createAddaGroup
                                        )
                                        2 -> SocialAndChatScreen(
                                            me = me,
                                            directoryState = directoryState,
                                            requests = requests,
                                            friendships = friendships,
                                            chats = chats,
                                            activeChatId = activeChatId,
                                            activeMessages = activeChatMessages,
                                            notifications = notifications,
                                            onSendFriendRequest = vm::sendFriendRequest,
                                            onRespondRequest = vm::respondFriendRequest,
                                            onOpenChat = { chatId, _, _ -> vm.openPrivateChat(chatId) },
                                            onCloseChat = vm::closePrivateChat,
                                            onSendPrivateMessage = vm::sendPrivateMessage,
                                            onReportUser = { targetId, reason -> vm.submitReport(targetId, "USER", reason) },
                                            onMarkNotificationRead = vm::markNotificationRead
                                        )
                                        3 -> HubAndRankingsScreen(
                                            me = me,
                                            lang = currentLang,
                                            users = users,
                                            rooms = rooms,
                                            addas = addas,
                                            families = families,
                                            agencies = agencies,
                                            pkBattles = pkBattles,
                                            events = events,
                                            missions = missions,
                                            onCreateFamily = vm::createFamily,
                                            onJoinFamily = vm::joinFamily,
                                            onCreateAgency = vm::createAgency,
                                            onApplyAsHost = vm::applyAsHost,
                                            onClaimMission = vm::claimMission,
                                            onPurchaseVip = vm::purchaseVip
                                        )
                                        4 -> WalletAndProfileScreen(
                                            me = me,
                                            privateAccount = myPrivate,
                                            wallet = wallet,
                                            orders = orders,
                                            transactions = transactions,
                                            lang = currentLang,
                                            soundFxEnabled = soundFxEnabled,
                                            isAdmin = adminRegistry != null,
                                            onToggleLanguage = vm::toggleLanguage,
                                            onToggleSoundFx = vm::toggleSoundFx,
                                            onSaveProfile = { uname, bio, age, gender, avatarUri, phone ->
                                                vm.saveProfile(uname, bio, age, gender, avatarUri)
                                                vm.savePrivateAccount(phone, myPrivate?.securityPinSet ?: false, myPrivate?.privacyDiscoverable ?: true)
                                            },
                                            onToggleSecurity = { pinSet, disc ->
                                                vm.savePrivateAccount(myPrivate?.phone.orEmpty(), pinSet, disc)
                                            },
                                            onSubmitRechargeOrder = vm::submitRecharge,
                                            onOpenAdminPanel = { showAdminScreen = true },
                                            onLogout = {
                                                val credMgr = CredentialManager.create(context)
                                                onSignedOut()
                                                scope.launch {
                                                    withContext(NonCancellable) {
                                                        vm.prepareSignOutCleanup()
                                                        try {
                                                            credMgr.clearCredentialState(ClearCredentialStateRequest())
                                                        } catch (_: Exception) {
                                                        } finally {
                                                            Firebase.auth.signOut()
                                                        }
                                                    }
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (needsProfileOnboarding && me != null) {
            ModalBottomSheet(
                onDismissRequest = vm::dismissProfileOnboarding,
                containerColor = ObsidianSurface
            ) {
                ProfileEditorSheetContent(
                    profile = me,
                    currentPhone = myPrivate?.phone.orEmpty(),
                    onSave = { uname, bio, age, gender, avatarUri, phone ->
                        vm.saveProfile(uname, bio, age, gender, avatarUri)
                        vm.savePrivateAccount(phone, myPrivate?.securityPinSet ?: false, myPrivate?.privacyDiscoverable ?: true)
                        vm.dismissProfileOnboarding()
                    },
                    onClose = vm::dismissProfileOnboarding
                )
            }
        }

        GiftOverlayPlayer(
            event = activeGiftAnim,
            onFinished = { activeGiftAnim = null }
        )
    }
}
