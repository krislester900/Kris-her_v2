package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import com.example.model.CustomStickerItem
import com.example.ui.components.BubbleStyle
import com.example.ui.components.BubbleStyleSelectorDialog
import com.example.ui.components.ContactPickerDialog
import com.example.ui.components.CustomStickerStudioDialog
import com.example.ui.components.MobilePermissionsDialog
import com.example.ui.components.saveBitmapToCache
import kotlinx.coroutines.launch
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SentimentSatisfiedAlt
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.core.content.ContextCompat
import com.example.ui.components.KrisLogo
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.AudioPlaybackState
import com.example.model.ConversationEntity
import com.example.model.MessageEntity
import com.example.model.MessageStatus
import com.example.model.MessageType
import com.example.ui.components.AttachmentOptionsSheet
import com.example.ui.components.AudioCapturePanel
import com.example.ui.components.AudioPermissionDialog
import com.example.ui.components.EnteAvatar
import com.example.ui.components.MessageInfoDialog
import com.example.ui.components.MessageInput
import com.example.ui.components.MessageList
import com.example.ui.components.MessageStatusIcon
import com.example.ui.components.NewSchoolEmojiBadge
import com.example.ui.components.SafetyNumberDialog
import com.example.ui.components.TypingIndicator
import com.example.ui.components.getNewSchoolBorderColor
import com.example.ui.theme.KablammoFontFamily
import com.example.ui.theme.NewSchoolCyan
import com.example.ui.theme.NewSchoolLime
import com.example.ui.theme.NewSchoolOrange
import com.example.ui.theme.NewSchoolPink
import com.example.ui.theme.NewSchoolPurple
import com.example.ui.theme.NewSchoolYellow
import com.example.ui.theme.PureBlack
import com.example.ui.theme.PureWhite
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class ChatMediaFilter(val label: String, val icon: String) {
    ALL("Tout", "🔍"),
    MEDIA("Photos/Images", "📸"),
    AUDIO("Vocaux", "🎤"),
    TEXT("Notes/Texte", "📝")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatRoomScreen(
    conversation: ConversationEntity,
    messages: List<MessageEntity>,
    isTyping: Boolean,
    isRecordingAudio: Boolean,
    isRecordingPaused: Boolean = false,
    recordingDurationSec: Int = 0,
    recordingAmplitudes: List<Float> = emptyList(),
    playbackState: AudioPlaybackState,
    isSoundEffectsEnabled: Boolean = true,
    onToggleSoundEffects: () -> Unit = {},
    onBack: () -> Unit,
    onSendMessage: (String, MessageType, Int, String?) -> Unit,
    onStartAudioRecording: () -> Unit,
    onTogglePauseAudioRecording: () -> Unit = {},
    onStopAndSendAudioRecording: () -> Unit,
    onCancelAudioRecording: () -> Unit,
    onPlayAudio: (messageId: Long, filePath: String?, durationSec: Int) -> Unit,
    onStartCall: (isVideo: Boolean) -> Unit,
    onAddReaction: (Long, String) -> Unit,
    onDeleteMessage: (Long) -> Unit,
    customStickers: List<CustomStickerItem> = emptyList(),
    onSaveCustomSticker: (CustomStickerItem) -> Unit = {},
    bubbleStyle: BubbleStyle = BubbleStyle.COMIC,
    onSelectBubbleStyle: (BubbleStyle) -> Unit = {},
    replyingToMessage: MessageEntity? = null,
    onSetReplyingToMessage: (MessageEntity?) -> Unit = {},
    onMinimizeToFloatingBubble: () -> Unit = {},
    onBlockContact: (Long) -> Unit = {},
    onUnblockContact: (Long) -> Unit = {},
    onAcceptContact: (Long) -> Unit = {},
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var showPermissionDialog by remember { mutableStateOf(false) }
    var showMobilePermissionsDialog by remember { mutableStateOf(false) }
    var showCustomStickerStudio by remember { mutableStateOf(false) }
    var showContactPickerDialog by remember { mutableStateOf(false) }
    var showBubbleStyleDialog by remember { mutableStateOf(false) }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                onStartAudioRecording()
            } else {
                showMobilePermissionsDialog = true
            }
        }
    )

    val galleryPhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                onSendMessage("📸 Photo", MessageType.IMAGE, 0, uri.toString())
            }
        }
    )

    val cameraPhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview(),
        onResult = { bitmap ->
            if (bitmap != null) {
                coroutineScope.launch {
                    val uri = saveBitmapToCache(context, bitmap)
                    onSendMessage("📸 Photo prise en direct", MessageType.IMAGE, 0, uri.toString())
                }
            }
        }
    )

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                cameraPhotoLauncher.launch(null)
            } else {
                showMobilePermissionsDialog = true
            }
        }
    )

    val contactPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                showContactPickerDialog = true
            } else {
                showMobilePermissionsDialog = true
            }
        }
    )

    var inputText by remember { mutableStateOf("") }
    var showSafetyNumber by remember { mutableStateOf(false) }
    var showAttachmentSheet by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var selectedMessageForReaction by remember { mutableStateOf<MessageEntity?>(null) }
    var selectedMessageForInfo by remember { mutableStateOf<MessageEntity?>(null) }

    // In-Chat & Moi Library Search and Media Filter state
    var isSearchActive by remember { mutableStateOf(false) }
    var chatSearchQuery by remember { mutableStateOf("") }
    var selectedMediaFilter by remember { mutableStateOf(ChatMediaFilter.ALL) }

    val filteredMessages = remember(messages, isSearchActive, chatSearchQuery, selectedMediaFilter) {
        if (!isSearchActive && chatSearchQuery.isBlank() && selectedMediaFilter == ChatMediaFilter.ALL) {
            messages
        } else {
            messages.filter { msg ->
                val matchesType = when (selectedMediaFilter) {
                    ChatMediaFilter.ALL -> true
                    ChatMediaFilter.MEDIA -> msg.type == MessageType.IMAGE || !msg.mediaUri.isNullOrBlank()
                    ChatMediaFilter.AUDIO -> msg.type == MessageType.AUDIO
                    ChatMediaFilter.TEXT -> msg.type == MessageType.TEXT || msg.type == MessageType.SYSTEM
                }
                val matchesQuery = if (chatSearchQuery.isBlank()) {
                    true
                } else {
                    val q = chatSearchQuery.trim()
                    msg.content.contains(q, ignoreCase = true) ||
                    msg.senderName.contains(q, ignoreCase = true) ||
                    (msg.type == MessageType.IMAGE && ("photo".contains(q, ignoreCase = true) || "image".contains(q, ignoreCase = true))) ||
                    (msg.type == MessageType.AUDIO && ("vocal".contains(q, ignoreCase = true) || "audio".contains(q, ignoreCase = true) || "voix".contains(q, ignoreCase = true)))
                }
                matchesType && matchesQuery
            }
        }
    }

    val listState = rememberLazyListState()

    LaunchedEffect(filteredMessages.size, isTyping) {
        if (filteredMessages.isNotEmpty() && !isSearchActive) {
            listState.animateScrollToItem(filteredMessages.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PureBlack)
            .imePadding()
            .navigationBarsPadding()
    ) {
        val isMoi = conversation.title.equals("Moi", ignoreCase = true)

        // Black & White App Bar with Kablammo details
        TopAppBar(
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { showSafetyNumber = true }
                ) {
                    if (isMoi) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF0F172A))
                                .border(1.5.dp, NewSchoolCyan, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("📁", fontSize = 18.sp)
                        }
                    } else {
                        EnteAvatar(
                            initials = conversation.avatarInitials,
                            colorHex = "#FFFFFF",
                            size = 38.dp,
                            showOnline = conversation.isOnline,
                            borderColor = PureWhite
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = if (isMoi) "Moi (Bibliothèque)" else conversation.title,
                            fontFamily = KablammoFontFamily,
                            fontSize = 17.sp,
                            color = PureWhite,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = PureWhite,
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isMoi) "Stockage personnel • Ne répond pas" else conversation.statusText,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isMoi) NewSchoolCyan else Color(0xFFAAAAAA),
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            },
            navigationIcon = {
                IconButton(onClick = onBack, modifier = Modifier.testTag("chat_back_button")) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Retour",
                        tint = PureWhite
                    )
                }
            },
            actions = {
                IconButton(
                    onClick = {
                        isSearchActive = !isSearchActive
                        if (!isSearchActive) {
                            chatSearchQuery = ""
                            selectedMediaFilter = ChatMediaFilter.ALL
                        }
                    },
                    modifier = Modifier.testTag("chat_search_button")
                ) {
                    Icon(
                        imageVector = if (isSearchActive) Icons.Default.Close else Icons.Default.Search,
                        contentDescription = if (isSearchActive) "Fermer la recherche" else "Rechercher dans la discussion ou bibliothèque",
                        tint = if (isSearchActive) NewSchoolCyan else PureWhite
                    )
                }

                if (isMoi) {
                    IconButton(
                        onClick = {
                            galleryPhotoLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier.testTag("save_photo_moi_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = "Sauvegarder une photo",
                            tint = NewSchoolCyan
                        )
                    }
                } else {
                    IconButton(
                        onClick = { onStartCall(false) },
                        modifier = Modifier.testTag("audio_call_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = "Appel audio",
                            tint = PureWhite
                        )
                    }

                    IconButton(
                        onClick = { onStartCall(true) },
                        modifier = Modifier.testTag("video_call_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Videocam,
                            contentDescription = "Appel vidéo",
                            tint = PureWhite
                        )
                    }
                }

                IconButton(
                    onClick = { showSafetyNumber = true },
                    modifier = Modifier.testTag("safety_key_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = "Vérifier la clé",
                        tint = PureWhite
                    )
                }

                IconButton(
                    onClick = onToggleSoundEffects,
                    modifier = Modifier.testTag("sound_effects_toggle_button")
                ) {
                    Icon(
                        imageVector = if (isSoundEffectsEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                        contentDescription = if (isSoundEffectsEnabled) "Bruitages New-School activés" else "Bruitages New-School coupés",
                        tint = if (isSoundEffectsEnabled) PureWhite else Color(0xFF888888)
                    )
                }

                IconButton(
                    onClick = { showBubbleStyleDialog = true },
                    modifier = Modifier.testTag("bubble_style_toggle_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = "Style des bulles de discussion",
                        tint = PureWhite
                    )
                }

                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Menu",
                        tint = PureWhite
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = {
                            Text("💬 Style des bulles (${bubbleStyle.title})")
                        },
                        onClick = {
                            showMenu = false
                            showBubbleStyleDialog = true
                        }
                    )
                    DropdownMenuItem(
                        text = {
                            Text("🗨️ Minimiser en bulle flottante")
                        },
                        onClick = {
                            showMenu = false
                            onMinimizeToFloatingBubble()
                        }
                    )
                    DropdownMenuItem(
                        text = {
                            Text(if (isSoundEffectsEnabled) "🔊 Bruitages New-School (Activés)" else "🔇 Bruitages New-School (Coupés)")
                        },
                        onClick = {
                            showMenu = false
                            onToggleSoundEffects()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Numéro de sécurité") },
                        onClick = {
                            showMenu = false
                            showSafetyNumber = true
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Messages éphémères (24h)") },
                        onClick = {
                            showMenu = false
                            onSendMessage("⏱️ Les messages éphémères sont activés pour cette discussion.", MessageType.SYSTEM, 0, null)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("🎨 Studio Autocollants & Émoticônes") },
                        onClick = {
                            showMenu = false
                            showCustomStickerStudio = true
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("🛡️ Autorisations Mobiles") },
                        onClick = {
                            showMenu = false
                            showMobilePermissionsDialog = true
                        }
                    )
                    if (!isMoi) {
                        if (conversation.isUnknownContact && !conversation.isAccepted) {
                            DropdownMenuItem(
                                text = { Text("✅ Accepter ce contact", color = NewSchoolCyan) },
                                onClick = {
                                    showMenu = false
                                    onAcceptContact(conversation.id)
                                }
                            )
                        }
                        if (conversation.isBlocked) {
                            DropdownMenuItem(
                                text = { Text("🔓 Débloquer ce contact", color = PureWhite) },
                                onClick = {
                                    showMenu = false
                                    onUnblockContact(conversation.id)
                                }
                            )
                        } else {
                            DropdownMenuItem(
                                text = { Text("🚫 Bloquer ce contact", color = Color(0xFFF87171)) },
                                onClick = {
                                    showMenu = false
                                    onBlockContact(conversation.id)
                                }
                            )
                        }
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = PureBlack,
                titleContentColor = PureWhite
            )
        )

        // In-Chat Search & Media Filter Panel
        AnimatedVisibility(
            visible = isSearchActive,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Surface(
                color = Color(0xFF0F172A),
                border = BorderStroke(1.dp, NewSchoolCyan.copy(alpha = 0.6f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp)
                ) {
                    // Search Input field
                    OutlinedTextField(
                        value = chatSearchQuery,
                        onValueChange = { chatSearchQuery = it },
                        placeholder = {
                            Text(
                                text = if (isMoi) "Rechercher dans votre bibliothèque 'Moi'..." else "Rechercher un message, photo ou vocal...",
                                fontSize = 13.sp,
                                color = Color(0xFF94A3B8)
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = NewSchoolCyan,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        trailingIcon = {
                            if (chatSearchQuery.isNotEmpty()) {
                                IconButton(onClick = { chatSearchQuery = "" }) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Effacer la recherche",
                                        tint = PureWhite,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = PureWhite,
                            unfocusedTextColor = PureWhite,
                            focusedBorderColor = NewSchoolCyan,
                            unfocusedBorderColor = Color(0xFF334155),
                            cursorColor = NewSchoolCyan
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("chat_search_input")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Media Filter Chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ChatMediaFilter.values().forEach { filter ->
                            val isSelected = selectedMediaFilter == filter
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) NewSchoolCyan else Color(0xFF1E293B),
                                border = BorderStroke(1.dp, if (isSelected) NewSchoolCyan else Color(0xFF475569)),
                                modifier = Modifier
                                    .clickable { selectedMediaFilter = filter }
                                    .testTag("chat_filter_${filter.name.lowercase()}")
                            ) {
                                Text(
                                    text = "${filter.icon} ${filter.label}",
                                    color = if (isSelected) PureBlack else PureWhite,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (filteredMessages.isEmpty())
                                "Aucun élément trouvé"
                            else
                                "${filteredMessages.size} élément${if (filteredMessages.size > 1) "s" else ""} trouvé${if (filteredMessages.size > 1) "s" else ""}",
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8),
                            fontWeight = FontWeight.Medium
                        )
                        TextButton(
                            onClick = {
                                isSearchActive = false
                                chatSearchQuery = ""
                                selectedMediaFilter = ChatMediaFilter.ALL
                            },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Fermer la recherche",
                                fontSize = 11.sp,
                                color = NewSchoolCyan
                            )
                        }
                    }
                }
            }
        }

        // Security Banner
        Surface(
            color = Color(0xFF111111),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF222222)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = PureWhite,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "kris&her • Messagerie privée",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFAAAAAA),
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center
                )
            }
        }

        // Unrecognized Contact (Accept or Block on First Message) Banner
        if (conversation.isUnknownContact && !conversation.isAccepted && !conversation.isBlocked) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    .testTag("unknown_contact_banner"),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1B4B)),
                border = BorderStroke(1.2.dp, Color(0xFF818CF8)),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color(0xFFFBBF24),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "CONTACT INCONNU (NON DANS VOS CONTACTS)",
                            fontFamily = KablammoFontFamily,
                            color = PureWhite,
                            fontSize = 12.5.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Cette personne vous a envoyé un premier message ou ne figure pas dans votre carnet d'adresses. Vous pouvez accepter cette discussion ou bloquer ce contact.",
                        fontSize = 11.5.sp,
                        color = Color(0xFFE0E7FF),
                        lineHeight = 16.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onBlockContact(conversation.id) },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFF87171)),
                            border = BorderStroke(1.2.dp, Color(0xFFF87171)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .testTag("block_unknown_contact_button")
                        ) {
                            Icon(Icons.Default.Block, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("BLOQUER", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                        Button(
                            onClick = { onAcceptContact(conversation.id) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NewSchoolCyan,
                                contentColor = PureBlack
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .testTag("accept_unknown_contact_button")
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("ACCEPTER", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // Blocked Contact Banner
        if (conversation.isBlocked) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    .testTag("blocked_contact_banner"),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2A0D0D)),
                border = BorderStroke(1.2.dp, Color(0xFFEF4444)),
                shape = RoundedCornerShape(14.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(
                            imageVector = Icons.Default.Block,
                            contentDescription = null,
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Contact actuellement bloqué",
                                fontWeight = FontWeight.Bold,
                                color = PureWhite,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "Les messages entrants et sortants sont bloqués.",
                                color = Color(0xFFFCA5A5),
                                fontSize = 11.sp
                            )
                        }
                    }
                    OutlinedButton(
                        onClick = { onUnblockContact(conversation.id) },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = PureWhite),
                        border = BorderStroke(1.dp, PureWhite),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("unblock_contact_top_button")
                    ) {
                        Text("DÉBLOQUER", fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        if (isMoi) {
            Surface(
                color = Color(0xFF0F172A),
                border = androidx.compose.foundation.BorderStroke(1.2.dp, NewSchoolCyan),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("📁", fontSize = 22.sp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "ESPACE PERSONNEL • BIBLIOTHÈQUE MOI",
                                fontFamily = KablammoFontFamily,
                                fontSize = 12.5.sp,
                                color = NewSchoolCyan
                            )
                            Text(
                                text = "Envoyez-vous des messages, stockez des photos et images en toute sécurité. Cet endroit sert de bibliothèque personnelle et ne répond pas.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFE2E8F0),
                                fontSize = 11.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                isSearchActive = true
                                selectedMediaFilter = ChatMediaFilter.MEDIA
                            },
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, NewSchoolCyan),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.testTag("moi_filter_photos")
                        ) {
                            Text("📸 Photos & Images", fontSize = 10.5.sp, color = NewSchoolCyan)
                        }

                        OutlinedButton(
                            onClick = {
                                isSearchActive = true
                                selectedMediaFilter = ChatMediaFilter.AUDIO
                            },
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color(0xFF94A3B8)),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.testTag("moi_filter_audio")
                        ) {
                            Text("🎤 Mémos vocaux", fontSize = 10.5.sp, color = PureWhite)
                        }

                        OutlinedButton(
                            onClick = {
                                isSearchActive = true
                                chatSearchQuery = ""
                                selectedMediaFilter = ChatMediaFilter.ALL
                            },
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color(0xFF94A3B8)),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.testTag("moi_search_shortcut")
                        ) {
                            Icon(Icons.Default.Search, contentDescription = null, tint = PureWhite, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Rechercher", fontSize = 10.5.sp, color = PureWhite)
                        }
                    }
                }
            }
        }

        // Messages Area using New-School MessageList
        Box(modifier = Modifier.weight(1f)) {
            if (filteredMessages.isEmpty() && (isSearchActive || selectedMediaFilter != ChatMediaFilter.ALL || chatSearchQuery.isNotBlank())) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(44.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Aucun message ou média trouvé",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = PureWhite
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (chatSearchQuery.isNotBlank())
                            "Aucun élément ne correspond à \"$chatSearchQuery\" dans cette ${if (isMoi) "bibliothèque Moi" else "discussion"}."
                        else
                            "Aucun média correspondant au filtre sélectionné.",
                        fontSize = 12.sp,
                        color = Color(0xFFAAAAAA),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            chatSearchQuery = ""
                            selectedMediaFilter = ChatMediaFilter.ALL
                            isSearchActive = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PureWhite, contentColor = PureBlack),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Afficher tous les messages", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            } else {
                MessageList(
                    messages = filteredMessages,
                    partnerName = conversation.title,
                    partnerInitials = conversation.avatarInitials,
                    partnerAvatarColorHex = conversation.avatarColorHex,
                    isTyping = isTyping,
                    bubbleStyle = bubbleStyle,
                    playbackState = playbackState,
                    listState = listState,
                    onPlayAudio = onPlayAudio,
                    onMessageClick = { msg -> selectedMessageForReaction = msg },
                    onMessageLongClick = { msg -> selectedMessageForReaction = msg },
                    onReactionClick = { msg, emoji -> onAddReaction(msg.id, emoji) },
                    onInfoClick = { msg -> selectedMessageForInfo = msg },
                    modifier = Modifier.fillMaxSize()
                )
            }

            // New School Sticker Reaction & Bubble Action Popup Bar
            if (selectedMessageForReaction != null) {
                val targetMsg = selectedMessageForReaction!!
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = PureBlack,
                    border = androidx.compose.foundation.BorderStroke(2.dp, PureWhite),
                    shadowElevation = 8.dp,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val reactions = listOf(
                            "❤️" to NewSchoolPink,
                            "🔥" to NewSchoolOrange,
                            "💀" to NewSchoolPurple,
                            "⚡" to NewSchoolYellow,
                            "👑" to NewSchoolLime,
                            "💎" to NewSchoolCyan
                        )

                        for ((emoji, color) in reactions) {
                            NewSchoolEmojiBadge(
                                emoji = emoji,
                                borderColor = color,
                                isDarkBg = true,
                                onClick = {
                                    onAddReaction(targetMsg.id, emoji)
                                    selectedMessageForReaction = null
                                }
                            )
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        // Reply Button
                        IconButton(
                            onClick = {
                                onSetReplyingToMessage(targetMsg)
                                selectedMessageForReaction = null
                            },
                            modifier = Modifier.size(32.dp).testTag("reply_message_btn_${targetMsg.id}")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Reply,
                                contentDescription = "Répondre à ce message",
                                tint = NewSchoolCyan,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Copy Text Button
                        IconButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("kris&her", targetMsg.content)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Message copié dans le presse-papier", Toast.LENGTH_SHORT).show()
                                selectedMessageForReaction = null
                            },
                            modifier = Modifier.size(32.dp).testTag("copy_message_btn_${targetMsg.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copier le texte",
                                tint = PureWhite,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Info Button
                        IconButton(
                            onClick = {
                                selectedMessageForInfo = targetMsg
                                selectedMessageForReaction = null
                            },
                            modifier = Modifier.size(32.dp).testTag("message_info_btn_${targetMsg.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Infos du message (Statut vu)",
                                tint = PureWhite,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Delete Button
                        IconButton(
                            onClick = {
                                onDeleteMessage(targetMsg.id)
                                selectedMessageForReaction = null
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Supprimer",
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        IconButton(
                            onClick = { selectedMessageForReaction = null },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Fermer",
                                tint = Color(0xFFAAAAAA),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }

        // Bottom Input Area in Pure Black & White with Reply Quote Header
        Surface(
            color = PureBlack,
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF262626)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                // Reply Quote Banner above input
                if (replyingToMessage != null) {
                    Surface(
                        color = Color(0xFF141414),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2E2E2E)),
                        shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(3.dp)
                                    .height(28.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(NewSchoolCyan)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Réponse à ${replyingToMessage.senderName}",
                                    fontFamily = KablammoFontFamily,
                                    fontSize = 11.sp,
                                    color = NewSchoolCyan,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = replyingToMessage.content.take(50),
                                    fontSize = 12.sp,
                                    color = Color(0xFFAAAAAA),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            IconButton(
                                onClick = { onSetReplyingToMessage(null) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Annuler la réponse",
                                    tint = Color(0xFF888888),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                if (conversation.isBlocked) {
                    Surface(
                        color = Color(0xFF1E1111),
                        border = BorderStroke(1.dp, Color(0xFFEF4444)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                            .testTag("blocked_input_bottom_notice")
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Default.Block, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Ce contact est bloqué. Débloquez-le pour lui écrire.",
                                    color = Color(0xFFFCA5A5),
                                    fontSize = 12.sp
                                )
                            }
                            TextButton(onClick = { onUnblockContact(conversation.id) }) {
                                Text("DÉBLOQUER", color = PureWhite, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }
                    }
                } else if (isRecordingAudio) {
                    // Interface de capture audio dédiée avec visualiseur d'onde
                    AudioCapturePanel(
                        isRecording = isRecordingAudio,
                        isPaused = isRecordingPaused,
                        durationSec = recordingDurationSec,
                        amplitudes = recordingAmplitudes,
                        onPauseResumeToggle = onTogglePauseAudioRecording,
                        onCancel = onCancelAudioRecording,
                        onSend = onStopAndSendAudioRecording
                    )
                } else {
                    MessageInput(
                        value = inputText,
                        onValueChange = { inputText = it },
                        onSendMessage = { text ->
                            onSendMessage(text, MessageType.TEXT, 0, null)
                            inputText = ""
                        },
                        onAttachClick = { showAttachmentSheet = true },
                        onMicClick = {
                            val hasPermission = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.RECORD_AUDIO
                            ) == PackageManager.PERMISSION_GRANTED
                            if (hasPermission) {
                                onStartAudioRecording()
                            } else {
                                audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        },
                        onEmojiInsert = { emoji ->
                            inputText += emoji
                        },
                        customStickers = customStickers,
                        onSendCustomSticker = { sticker ->
                            val titleText = if (sticker.isVideo) "${sticker.title} 🎬" else sticker.title
                            onSendMessage(titleText, MessageType.STICKER, 0, sticker.imageUri)
                        },
                        onOpenCustomStickerStudio = { showCustomStickerStudio = true }
                    )
                }
            }
        }
    }

    if (showAttachmentSheet) {
        AttachmentOptionsSheet(
            onOptionSelected = { option ->
                showAttachmentSheet = false
                when (option) {
                    "PHOTO" -> {
                        galleryPhotoLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                    "CAMERA" -> {
                        val hasCamera = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.CAMERA
                        ) == PackageManager.PERMISSION_GRANTED
                        if (hasCamera) {
                            cameraPhotoLauncher.launch(null)
                        } else {
                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    }
                    "STICKER" -> {
                        showCustomStickerStudio = true
                    }
                    "CONTACT" -> {
                        val hasContact = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.READ_CONTACTS
                        ) == PackageManager.PERMISSION_GRANTED
                        if (hasContact) {
                            showContactPickerDialog = true
                        } else {
                            contactPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                        }
                    }
                    "PERMISSIONS" -> {
                        showMobilePermissionsDialog = true
                    }
                    "DOCUMENT" -> onSendMessage("📄 Document kris&her.pdf", MessageType.DOCUMENT, 0, null)
                    "LOCATION" -> onSendMessage("📍 Position", MessageType.TEXT, 0, null)
                }
            },
            onDismiss = { showAttachmentSheet = false }
        )
    }

    if (showSafetyNumber) {
        SafetyNumberDialog(
            contactName = conversation.title,
            fingerprint = conversation.safetyFingerprint,
            onDismiss = { showSafetyNumber = false }
        )
    }

    if (selectedMessageForInfo != null) {
        MessageInfoDialog(
            message = selectedMessageForInfo!!,
            recipientName = conversation.title,
            onDismiss = { selectedMessageForInfo = null }
        )
    }

    if (showPermissionDialog) {
        AudioPermissionDialog(
            onDismiss = { showPermissionDialog = false },
            onGrantPermission = {
                showPermissionDialog = false
                audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        )
    }

    if (showCustomStickerStudio) {
        CustomStickerStudioDialog(
            onDismiss = { showCustomStickerStudio = false },
            onSaveSticker = { newSticker ->
                onSaveCustomSticker(newSticker)
                val titleText = if (newSticker.isVideo) "${newSticker.title} 🎬" else newSticker.title
                onSendMessage(titleText, MessageType.STICKER, 0, newSticker.imageUri)
            },
            defaultAuthor = "Kris"
        )
    }

    if (showMobilePermissionsDialog) {
        MobilePermissionsDialog(
            onDismiss = { showMobilePermissionsDialog = false }
        )
    }

    if (showContactPickerDialog) {
        ContactPickerDialog(
            onDismiss = { showContactPickerDialog = false },
            onContactSelected = { name, phone ->
                onSendMessage("👤 Fiche Contact : $name • $phone", MessageType.TEXT, 0, null)
            }
        )
    }

    if (showBubbleStyleDialog) {
        BubbleStyleSelectorDialog(
            currentStyle = bubbleStyle,
            onSelectStyle = {
                onSelectBubbleStyle(it)
                showBubbleStyleDialog = false
            },
            onDismiss = { showBubbleStyleDialog = false }
        )
    }
}
}

@Composable
fun MessageBubble(
    message: MessageEntity,
    isMe: Boolean,
    playbackState: AudioPlaybackState,
    onPlayAudio: () -> Unit,
    onClick: () -> Unit,
    onInfoClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val bubbleColor = if (isMe) Color(0xFF1A1A1A) else Color(0xFF0F0F0F)
    val borderColor = if (isMe) PureWhite.copy(alpha = 0.6f) else Color(0xFF333333)
    val shape = if (isMe) {
        RoundedCornerShape(topStart = 16.dp, topEnd = 4.dp, bottomStart = 16.dp, bottomEnd = 16.dp)
    } else {
        RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp)
    }

    Surface(
        shape = shape,
        color = bubbleColor,
        border = androidx.compose.foundation.BorderStroke(1.5.dp, borderColor),
        modifier = modifier
            .widthIn(min = 120.dp, max = 290.dp)
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            when (message.type) {
                MessageType.AUDIO -> {
                    val isPlayingThis = playbackState.playingMessageId == message.id && playbackState.isPlaying
                    val currentPos = if (playbackState.playingMessageId == message.id) playbackState.currentPositionMs else 0
                    val totalDur = if (playbackState.playingMessageId == message.id && playbackState.totalDurationMs > 0) {
                        playbackState.totalDurationMs
                    } else {
                        message.audioDurationSec.coerceAtLeast(2) * 1000
                    }

                    AudioMessagePlayerContent(
                        durationSec = message.audioDurationSec.coerceAtLeast(2),
                        isPlaying = isPlayingThis,
                        currentPositionMs = currentPos,
                        totalDurationMs = totalDur,
                        onTogglePlay = onPlayAudio
                    )
                }
                MessageType.IMAGE -> {
                    Column {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF222222)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = PureWhite,
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "kris&her Photo",
                                    fontFamily = KablammoFontFamily,
                                    fontSize = 12.sp,
                                    color = PureWhite
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = message.content,
                            style = MaterialTheme.typography.bodyMedium,
                            color = PureWhite
                        )
                    }
                }
                else -> {
                    Text(
                        text = message.content,
                        style = MaterialTheme.typography.bodyMedium,
                        color = PureWhite
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Time and Vu Status
            Row(
                modifier = Modifier.align(Alignment.End),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(message.timestamp)),
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    color = Color(0xFFAAAAAA)
                )

                if (isMe) {
                    Spacer(modifier = Modifier.width(4.dp))
                    MessageStatusIcon(status = message.status)

                    if (message.status == MessageStatus.READ && message.readTimestamp != null) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(PureWhite.copy(alpha = 0.15f))
                                .border(1.dp, PureWhite.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                                .clickable { onInfoClick?.invoke() }
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                                .testTag("seen_badge_${message.id}")
                        ) {
                            Text(
                                text = "Vu ${SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(message.readTimestamp!!))}",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = PureWhite
                            )
                        }
                    } else if (message.status == MessageStatus.DELIVERED) {
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "Distribué",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 9.sp,
                            color = Color(0xFFAAAAAA)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AudioMessagePlayerContent(
    durationSec: Int,
    isPlaying: Boolean,
    currentPositionMs: Int,
    totalDurationMs: Int,
    onTogglePlay: () -> Unit,
    modifier: Modifier = Modifier
) {
    val progress = if (totalDurationMs > 0) {
        (currentPositionMs.toFloat() / totalDurationMs.toFloat()).coerceIn(0f, 1f)
    } else 0f

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onTogglePlay,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(PureWhite)
                .border(2.dp, NewSchoolCyan, CircleShape)
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isPlaying) "Pause" else "Lire",
                tint = PureBlack,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            // Interactive dynamic audio waveform progress canvas
            Canvas(modifier = Modifier.fillMaxWidth().height(24.dp)) {
                val barWidth = 3.5.dp.toPx()
                val gap = 2.5.dp.toPx()
                val totalBars = (size.width / (barWidth + gap)).toInt().coerceIn(10, 32)
                val heights = listOf(0.3f, 0.7f, 0.95f, 0.5f, 0.85f, 1f, 0.6f, 0.4f, 0.8f, 0.65f, 0.5f, 0.9f)

                for (i in 0 until totalBars) {
                    val hRatio = heights[i % heights.size]
                    val barHeight = size.height * hRatio
                    val x = i * (barWidth + gap)
                    val y = (size.height - barHeight) / 2
                    val barProgress = i.toFloat() / totalBars.toFloat()
                    val color = if (barProgress <= progress) PureWhite else Color(0xFF444444)

                    drawRoundRect(
                        color = color,
                        topLeft = Offset(x, y),
                        size = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                    )
                }
            }

            Spacer(modifier = Modifier.height(3.dp))

            // Real elapsed time & total duration
            val elapsedSec = currentPositionMs / 1000
            val totalSec = if (totalDurationMs > 0) totalDurationMs / 1000 else durationSec
            Text(
                text = if (isPlaying) "%d:%02d / %d:%02d 🔊".format(elapsedSec / 60, elapsedSec % 60, totalSec / 60, totalSec % 60)
                else "%d:%02d".format(durationSec / 60, durationSec % 60),
                style = MaterialTheme.typography.labelSmall,
                color = PureWhite,
                fontSize = 11.sp
            )
        }
    }
}
