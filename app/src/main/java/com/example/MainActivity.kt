package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DonutLarge
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.ActiveCallDialog
import com.example.ui.components.BubbleStyle
import com.example.ui.components.CustomStickerStudioDialog
import com.example.ui.components.FloatingChatHead
import com.example.ui.components.MobilePermissionsDialog
import com.example.ui.components.VaultLockScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.ChatListScreen
import com.example.ui.screens.ChatRoomScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.StatusScreen
import com.example.ui.screens.VaultSecurityScreen
import com.example.ui.theme.KablammoFontFamily
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.NewSchoolPink
import com.example.ui.theme.PureBlack
import com.example.ui.theme.PureWhite
import com.example.viewmodel.ChatViewModel
import com.example.viewmodel.ThemeMode
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth

enum class MainTab(val title: String, val icon: ImageVector) {
    CHATS("Discussions", Icons.AutoMirrored.Filled.Chat),
    STATUS("Statuts", Icons.Default.DonutLarge),
    VAULT("Coffre", Icons.Default.Security),
    PROFILE("Profil", Icons.Default.Person)
}

class MainActivity : ComponentActivity() {
    private val viewModel: ChatViewModel by viewModels()

    // Android 13+ : les notifications doivent être autorisées explicitement
    private val notificationPermissionLauncher =
        registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.RequestPermission()) { }

    override fun onStart() {
        super.onStart()
        com.example.util.NotificationHelper.appInForeground = true
    }

    override fun onStop() {
        com.example.util.NotificationHelper.appInForeground = false
        super.onStop()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (android.os.Build.VERSION.SDK_INT >= 33 &&
            androidx.core.content.ContextCompat.checkSelfPermission(
                this, android.Manifest.permission.POST_NOTIFICATIONS
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
        try {
            com.google.firebase.FirebaseApp.initializeApp(this)
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "FirebaseApp init: ${e.message}")
        }
        enableEdgeToEdge()
        setContent {
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
            val hasCompletedOnboarding by viewModel.hasCompletedOnboarding.collectAsStateWithLifecycle()
            val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()

            MyApplicationTheme(
                darkTheme = themeMode != ThemeMode.LIGHT
            ) {
                var currentUser by remember {
                    mutableStateOf(
                        try { Firebase.auth.currentUser } catch (e: Exception) { null }
                    )
                }

                DisposableEffect(Unit) {
                    val auth = try { Firebase.auth } catch (e: Exception) { null }
                    val listener = FirebaseAuth.AuthStateListener { fbAuth ->
                        currentUser = try { fbAuth.currentUser } catch (e: Exception) { null }
                    }
                    try {
                        auth?.addAuthStateListener(listener)
                    } catch (e: Exception) {
                        android.util.Log.e("MainActivity", "addAuthStateListener error", e)
                    }
                    onDispose {
                        try {
                            auth?.removeAuthStateListener(listener)
                        } catch (_: Exception) {}
                    }
                }

                if (!hasCompletedOnboarding) {
                    AuthScreen(
                        userProfile = userProfile,
                        onCompleteOnboarding = { name, phone, pic ->
                            viewModel.completeOnboarding(name, phone, pic)
                        },
                        onAuthSuccess = {
                            currentUser = try { Firebase.auth.currentUser } catch (e: Exception) { null }
                        }
                    )
                } else {
                    KrisAndHerApp(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun KrisAndHerApp(viewModel: ChatViewModel) {
    val isAppLocked by viewModel.isAppLocked.collectAsStateWithLifecycle()
    val enteredPin by viewModel.enteredPin.collectAsStateWithLifecycle()
    val pinError by viewModel.pinError.collectAsStateWithLifecycle()
    val activeCall by viewModel.activeCall.collectAsStateWithLifecycle()
    val currentConvId by viewModel.currentConversationId.collectAsStateWithLifecycle()
    val activeConv by viewModel.activeConversation.collectAsStateWithLifecycle()
    val activeMessages by viewModel.activeMessages.collectAsStateWithLifecycle()
    val isTyping by viewModel.isTyping.collectAsStateWithLifecycle()
    val isRecordingAudio by viewModel.isRecordingAudio.collectAsStateWithLifecycle()
    val isRecordingPaused by viewModel.isRecordingPaused.collectAsStateWithLifecycle()
    val recordingDurationSec by viewModel.recordingDurationSec.collectAsStateWithLifecycle()
    val recordingAmplitudes by viewModel.recordingAmplitudes.collectAsStateWithLifecycle()
    val audioPlaybackState by viewModel.audioPlaybackState.collectAsStateWithLifecycle()
    val conversations by viewModel.conversations.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedFilter by viewModel.selectedFilter.collectAsStateWithLifecycle()
    val isPinProtectionEnabled by viewModel.isPinProtectionEnabled.collectAsStateWithLifecycle()
    val hasPin by viewModel.hasPin.collectAsStateWithLifecycle()
    val ephemeralTimerHours by viewModel.ephemeralTimerHours.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val stories by viewModel.stories.collectAsStateWithLifecycle()
    val isSoundEffectsEnabled by viewModel.isSoundEffectsEnabled.collectAsStateWithLifecycle()
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val isSavingProfile by viewModel.isSavingProfile.collectAsStateWithLifecycle()
    val profileSaveMessage by viewModel.profileSaveMessage.collectAsStateWithLifecycle()
    val customStickers by viewModel.customStickers.collectAsStateWithLifecycle()
    val bubbleStyle by viewModel.bubbleStyle.collectAsStateWithLifecycle()
    val replyingToMessage by viewModel.replyingToMessage.collectAsStateWithLifecycle()
    val floatingBubbleConversationId by viewModel.floatingBubbleConversationId.collectAsStateWithLifecycle()
    val accountCreationBanner by viewModel.accountCreationBanner.collectAsStateWithLifecycle()

    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(MainTab.CHATS) }
    var showGlobalPermissionsDialog by remember { mutableStateOf(false) }
    var showGlobalStickerStudio by remember { mutableStateOf(false) }

    BackHandler(enabled = currentConvId != null) {
        viewModel.closeConversation()
    }

    BackHandler(enabled = currentConvId == null && selectedTab != MainTab.CHATS) {
        selectedTab = MainTab.CHATS
    }

    // If vault is locked, show PIN keypad overlay
    if (isAppLocked) {
        VaultLockScreen(
            enteredPin = enteredPin,
            hasError = pinError,
            onDigitClick = { digit -> viewModel.appendPinDigit(digit) },
            onDeleteClick = { viewModel.deletePinDigit() }
        )
        return
    }

    // Main App Scaffold
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (currentConvId == null) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline)
                ) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        tonalElevation = 0.dp,
                        modifier = Modifier.navigationBarsPadding()
                    ) {
                        val totalUnread = conversations.sumOf { it.unreadCount }

                        MainTab.values().forEach { tab ->
                            val isSelected = selectedTab == tab
                            NavigationBarItem(
                                selected = isSelected,
                                onClick = { selectedTab = tab },
                                icon = {
                                    if (tab == MainTab.CHATS && totalUnread > 0) {
                                        BadgedBox(
                                            badge = {
                                                Badge(
                                                    containerColor = PureWhite,
                                                    contentColor = PureBlack
                                                ) {
                                                    Text(totalUnread.toString(), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        ) {
                                            Icon(
                                                imageVector = tab.icon,
                                                contentDescription = tab.title,
                                                tint = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    } else {
                                        Icon(
                                            imageVector = tab.icon,
                                            contentDescription = tab.title,
                                            tint = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                },
                                label = {
                                    Text(
                                        text = tab.title,
                                        fontFamily = KablammoFontFamily,
                                        fontSize = 11.sp,
                                        color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    indicatorColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
                                ),
                                modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (currentConvId != null && activeConv != null) {
                // Chat Room with pertinent voice playback and New School stickers
                ChatRoomScreen(
                    conversation = activeConv!!,
                    messages = activeMessages,
                    isTyping = isTyping,
                    isRecordingAudio = isRecordingAudio,
                    isRecordingPaused = isRecordingPaused,
                    recordingDurationSec = recordingDurationSec,
                    recordingAmplitudes = recordingAmplitudes,
                    playbackState = audioPlaybackState,
                    isSoundEffectsEnabled = isSoundEffectsEnabled,
                    onToggleSoundEffects = { viewModel.toggleSoundEffects() },
                    onBack = { viewModel.closeConversation() },
                    onSendMessage = { text, type, duration, mediaUri ->
                        viewModel.sendMessage(text, type, duration, mediaUri)
                    },
                    onStartAudioRecording = { viewModel.startAudioRecording() },
                    onTogglePauseAudioRecording = { viewModel.togglePauseRecording() },
                    onStopAndSendAudioRecording = { viewModel.stopAndSendAudioRecording() },
                    onCancelAudioRecording = { viewModel.cancelAudioRecording() },
                    onPlayAudio = { msgId, mediaUri, duration ->
                        viewModel.playAudio(msgId, mediaUri, duration)
                    },
                    onStartCall = { isVideo -> viewModel.startCall(isVideo) },
                    onAddReaction = { msgId, emoji -> viewModel.addReaction(msgId, emoji) },
                    onDeleteMessage = { msgId -> viewModel.deleteMessage(msgId) },
                    customStickers = customStickers,
                    onSaveCustomSticker = { sticker -> viewModel.saveCustomSticker(sticker) },
                    bubbleStyle = bubbleStyle,
                    onSelectBubbleStyle = { viewModel.setBubbleStyle(it) },
                    replyingToMessage = replyingToMessage,
                    onSetReplyingToMessage = { viewModel.setReplyingToMessage(it) },
                    onMinimizeToFloatingBubble = {
                        viewModel.setFloatingBubbleConversationId(activeConv!!.id)
                        viewModel.closeConversation()
                    },
                    onBlockContact = { id -> viewModel.blockContact(id) },
                    onUnblockContact = { id -> viewModel.unblockContact(id) },
                    onAcceptContact = { id -> viewModel.acceptContact(id) }
                )
            } else {
                // Tab Screens
                when (selectedTab) {
                    MainTab.CHATS -> {
                        ChatListScreen(
                            conversations = conversations,
                            searchQuery = searchQuery,
                            selectedFilter = selectedFilter,
                            userProfile = userProfile,
                            onSearchQueryChange = { viewModel.searchQuery.value = it },
                            onFilterSelect = { viewModel.selectedFilter.value = it },
                            onConversationClick = { id -> viewModel.openConversation(id) },
                            onTogglePin = { id -> viewModel.togglePinConversation(id) },
                            onDeleteConversation = { id -> viewModel.deleteConversation(id) },
                            onAddNewFriend = { name, status, color, initials -> viewModel.createNewFriend(name, status, color, initials) },
                            onSearchUsers = { query -> viewModel.searchUsersInFirestore(query) },
                            onSearchUsersByPhoneNumbers = { phoneNumbers -> viewModel.searchUsersByPhoneNumbers(phoneNumbers) },
                            onStartDirectChat = { name, status, color, initials, phone, pic, isUnknown, isAccepted, peerUid ->
                                viewModel.createNewConversation(name, status, color, initials, phone, pic, isUnknown, isAccepted, peerUid)
                            },
                            onNavigateToProfile = { selectedTab = MainTab.PROFILE },
                            onOpenSecurityVault = { selectedTab = MainTab.VAULT }
                        )
                    }
                    MainTab.STATUS -> {
                        StatusScreen(
                            stories = stories,
                            onPostStatus = { caption, emoji -> viewModel.postNewStatus(caption, emoji) }
                        )
                    }
                    MainTab.VAULT -> {
                        VaultSecurityScreen(
                            isPinProtectionEnabled = isPinProtectionEnabled,
                            hasPin = hasPin,
                            onSetPin = { pin -> viewModel.setPin(pin) },
                            ephemeralTimerHours = ephemeralTimerHours,
                            onTogglePinProtection = { enabled -> viewModel.togglePinProtection(enabled) },
                            onLockVaultNow = { viewModel.lockVaultNow() },
                            onSetEphemeralTimer = { hours -> viewModel.ephemeralTimerHours.value = hours }
                        )
                    }
                    MainTab.PROFILE -> {
                        ProfileScreen(
                            currentTheme = themeMode,
                            onSelectTheme = { viewModel.themeMode.value = it },
                            userProfile = userProfile,
                            isSavingProfile = isSavingProfile,
                            profileSaveMessage = profileSaveMessage,
                            onSaveProfile = { name, phone, pic, status, color ->
                                viewModel.saveUserProfile(
                                    displayName = name,
                                    phoneNumber = phone,
                                    photoUrl = pic,
                                    statusText = status,
                                    avatarColorHex = color
                                )
                            },
                            onClearProfileMessage = { viewModel.clearProfileMessage() },
                            isSoundEffectsEnabled = isSoundEffectsEnabled,
                            onToggleSoundEffects = { viewModel.toggleSoundEffects() },
                            onTestSentSound = { viewModel.playSentSoundEffect() },
                            onTestReceivedSound = { viewModel.playReceivedSoundEffect() },
                            onOpenPermissions = { showGlobalPermissionsDialog = true },
                            onOpenStickerStudio = { showGlobalStickerStudio = true },
                            currentBubbleStyle = bubbleStyle,
                            onSelectBubbleStyle = { viewModel.setBubbleStyle(it) }
                        )
                    }
                }
            }
        }
    }

    // Floating Chat Bubble Head Overlay when a chat is minimized
    if (currentConvId == null && floatingBubbleConversationId != null) {
        val floatingConv = conversations.firstOrNull { it.id == floatingBubbleConversationId }
        if (floatingConv != null) {
            FloatingChatHead(
                conversation = floatingConv,
                onClick = {
                    viewModel.openConversation(floatingConv.id)
                    viewModel.setFloatingBubbleConversationId(null)
                },
                onDismiss = {
                    viewModel.setFloatingBubbleConversationId(null)
                }
            )
        }
    }

    // Mobile Permissions Dialog
    if (showGlobalPermissionsDialog) {
        MobilePermissionsDialog(
            onDismiss = { showGlobalPermissionsDialog = false }
        )
    }

    // Custom Sticker Studio Dialog
    if (showGlobalStickerStudio) {
        CustomStickerStudioDialog(
            onDismiss = { showGlobalStickerStudio = false },
            onSaveSticker = { sticker -> viewModel.saveCustomSticker(sticker) },
            defaultAuthor = userProfile?.displayName ?: "Kris"
        )
    }

    // Ongoing encrypted call simulation overlay
    if (activeCall != null) {
        ActiveCallDialog(
            callState = activeCall!!,
            onToggleMute = { viewModel.toggleCallMute() },
            onToggleSpeaker = { viewModel.toggleCallSpeaker() },
            onEndCall = { viewModel.endCall() }
        )
    }

    // Confirmation dialog for account creation & email message sent to Google account
    if (accountCreationBanner != null) {
        val bannerMsg = accountCreationBanner ?: ""
        AlertDialog(
            onDismissRequest = { viewModel.dismissAccountCreationBanner() },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color(0xFF00F0FF),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Compte créé avec succès",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = PureWhite
                    )
                }
            },
            text = {
                Column {
                    Text(
                        text = bannerMsg,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        color = PureWhite.copy(alpha = 0.9f)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Un message officiel de confirmation a été envoyé directement dans votre boîte mail Google (Gmail) pour certifier votre inscription. Vos contacts peuvent désormais vous retrouver avec votre numéro.",
                        fontSize = 12.5.sp,
                        color = PureWhite.copy(alpha = 0.65f),
                        lineHeight = 17.sp
                    )
                }
            },
            containerColor = PureBlack,
            shape = RoundedCornerShape(16.dp),
            confirmButton = {
                Button(
                    onClick = { viewModel.dismissAccountCreationBanner() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PureWhite,
                        contentColor = PureBlack
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Accéder à mes discussions", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        viewModel.dismissAccountCreationBanner()
                        try {
                            val emailIntent = android.content.Intent(android.content.Intent.ACTION_MAIN).apply {
                                addCategory(android.content.Intent.CATEGORY_APP_EMAIL)
                                flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            context.startActivity(emailIntent)
                        } catch (_: Exception) {}
                    },
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFFAAAAAA)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Ouvrir boîte mail")
                }
            }
        )
    }
}
