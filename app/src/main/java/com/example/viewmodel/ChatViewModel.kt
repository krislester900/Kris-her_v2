package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.ChatRepository
import com.example.data.FirestoreChatRepository
import com.example.model.Chat
import com.example.model.ChatMessagesSnapshot
import com.example.model.ConversationEntity
import com.example.model.CustomStickerItem
import com.example.model.MessageEntity
import com.example.model.MessageStatus
import com.example.model.MessageType
import com.example.model.StatusStory
import com.example.model.UserProfile
import com.example.security.VaultSecurityManager
import com.example.ui.components.BubbleStyle
import com.example.viewmodel.AudioManagerViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

enum class ChatFilter(val label: String) {
    TOUS("Tous"),
    NON_LUS("Non lus"),
    FAVORIS("Favoris"),
    GROUPES("Groupes")
}

enum class ThemeMode {
    DARK,
    LIGHT
}

data class ActiveCallState(
    val conversationTitle: String,
    val avatarInitials: String,
    val avatarColorHex: String,
    val isVideo: Boolean = false,
    val durationSeconds: Int = 0,
    val isMuted: Boolean = false,
    val isSpeaker: Boolean = true
)

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class ChatViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val repository = ChatRepository(database.conversationDao(), database.messageDao())
    private val firestoreRepository = FirestoreChatRepository(application)
    private val audioManagerViewModel = AudioManagerViewModel(application)
    private val stickerViewModel = StickerViewModel(application, firestoreRepository)
    private val notificationViewModel = NotificationViewModel(application, firestoreRepository)

    val audioPlaybackState: StateFlow<AudioPlaybackState> = audioManagerViewModel.audioPlaybackState
    val isSoundEffectsEnabled: StateFlow<Boolean> = audioManagerViewModel.isSoundEffectsEnabled

    // Chat Bubbles Style & State
    val bubbleStyle = MutableStateFlow(BubbleStyle.COMIC)
    val replyingToMessage = MutableStateFlow<MessageEntity?>(null)
    val floatingBubbleConversationId = MutableStateFlow<Long?>(null)

    private val prefs = application.getSharedPreferences("kris_her_prefs", android.content.Context.MODE_PRIVATE)

    val hasCompletedOnboarding = MutableStateFlow(
        prefs.getBoolean("has_completed_onboarding", false)
    )

    val userProfile = MutableStateFlow<UserProfile?>(null)
    val isSavingProfile = MutableStateFlow(false)
    val profileSaveMessage = MutableStateFlow<String?>(null)
    val accountCreationBanner: StateFlow<String?> = notificationViewModel.accountCreationBanner

    /**
     * Erreur globale à afficher à l'utilisateur (Snackbar, bannière, dialogue).
     * Utilisé pour propager les échecs qui étaient auparavant silencieux.
     */
    val errorMessage = MutableStateFlow<String?>(null)

    fun dismissErrorMessage() { errorMessage.value = null }

    /**
     * Enregistre une erreur à destination de l'utilisateur.
     * Évite les doublons si le même message est déjà affiché.
     */
    fun reportError(message: String) {
        if (errorMessage.value != message) {
            errorMessage.value = message
        }
    }

    fun dismissAccountCreationBanner() {
        notificationViewModel.dismissAccountCreationBanner()
    }

    val customStickers: StateFlow<List<CustomStickerItem>> = stickerViewModel.customStickers
    private var lastTrackedConversationId: Long? = null
    private var lastTrackedMessageId: Long? = null

    // --- Synchronisation des discussions partagées (Firestore) ---
    private val syncMutex = Mutex()
    private val peerProfileCache = mutableMapOf<String, UserProfile>()

    // Discussions supprimées localement : chatId -> date de suppression (elles ne reviennent que si un nouveau message arrive)
    private val hiddenChats: MutableMap<String, Long> = loadHiddenChats()
    // Messages reçus supprimés « pour moi » : on ne les réinsère pas à la prochaine synchro
    private val hiddenMessages: MutableSet<String> =
        (prefs.getStringSet("hidden_messages", emptySet()) ?: emptySet()).toMutableSet()

    private fun loadHiddenChats(): MutableMap<String, Long> {
        val map = mutableMapOf<String, Long>()
        (prefs.getStringSet("hidden_chats", emptySet()) ?: emptySet()).forEach { entry ->
            val idx = entry.lastIndexOf('|')
            if (idx > 0) entry.substring(idx + 1).toLongOrNull()?.let { map[entry.substring(0, idx)] = it }
        }
        return map
    }

    private fun saveHiddenChats() {
        prefs.edit().putStringSet("hidden_chats", hiddenChats.map { "${it.key}|${it.value}" }.toSet()).apply()
    }

    private fun saveHiddenMessages() {
        prefs.edit().putStringSet("hidden_messages", hiddenMessages.toSet()).apply()
    }

    // false = les médias sont envoyés directement dans le message (gratuit, sans plan Blaze).
    // true  = Firebase Storage (plan Blaze requis, aucune limite de taille pratique).
    private val useCloudStorage = false

    /** Média reçu en « data: » -> fichier local (photos : file://, vocaux/docs : chemin) pour l'affichage et la lecture. */
    private suspend fun materializeReceivedMedia(remoteId: String, type: String, mediaUri: String?): String? {
        if (mediaUri == null || !mediaUri.startsWith("data:")) return mediaUri
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val comma = mediaUri.indexOf(',')
                if (comma < 0) return@withContext null
                val header = mediaUri.substring(5, comma)
                val bytes = android.util.Base64.decode(mediaUri.substring(comma + 1), android.util.Base64.DEFAULT)
                val ext = when {
                    header.startsWith("image/png") -> "png"
                    header.startsWith("image/") -> "jpg"
                    header.startsWith("audio/") -> "m4a"
                    header.contains("pdf") -> "pdf"
                    else -> "bin"
                }
                val dir = java.io.File(getApplication<Application>().filesDir, "received_media").apply { mkdirs() }
                val file = java.io.File(dir, "$remoteId.$ext")
                if (!file.exists()) file.writeBytes(bytes)
                if (type == "IMAGE" || type == "STICKER") "file://" + file.absolutePath else file.absolutePath
            } catch (e: Exception) {
                android.util.Log.w("ChatViewModel", "materializeReceivedMedia warning: ${e.message}")
                null
            }
        }
    }

    private fun isPortableMedia(uri: String?): Boolean =
        uri.isNullOrBlank() || uri.startsWith("preset://") || uri.startsWith("data:") ||
            uri.startsWith("http://") || uri.startsWith("https://")

    val searchQuery = MutableStateFlow("")
    val selectedFilter = MutableStateFlow(ChatFilter.TOUS)

    val currentFirebaseUser: com.google.firebase.auth.FirebaseUser?
        get() = try { Firebase.auth.currentUser } catch (_: Exception) { null }

    // Current open conversation
    val currentConversationId = MutableStateFlow<Long?>(null)

    // Filtered conversations (searches title, preview, and messages inside the conversation or 'Moi' library)
    val conversations: StateFlow<List<ConversationEntity>> = combine(
        repository.allConversations,
        searchQuery.flatMapLatest { query ->
            if (query.isBlank()) flowOf(emptyList()) else repository.getConversationIdsWithMatchingMessages(query)
        },
        searchQuery,
        selectedFilter
    ) { list, matchingConvIds, query, filter ->
        list.filter { conv ->
            val matchesQuery = query.isEmpty() ||
                conv.title.contains(query, ignoreCase = true) ||
                conv.lastMessage.contains(query, ignoreCase = true) ||
                matchingConvIds.contains(conv.id)

            val matchesFilter = when (filter) {
                ChatFilter.TOUS -> true
                ChatFilter.NON_LUS -> conv.unreadCount > 0
                ChatFilter.FAVORIS -> conv.isFavorite
                ChatFilter.GROUPES -> conv.isGroup
            }

            matchesQuery && matchesFilter
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active conversation details
    val activeConversation: StateFlow<ConversationEntity?> = currentConversationId.flatMapLatest { id ->
        if (id != null) repository.getConversation(id) else flowOf(null)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Messages for active conversation
    val activeMessages: StateFlow<List<MessageEntity>> = currentConversationId.flatMapLatest { id ->
        if (id != null) repository.getMessages(id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Typing simulation state
    val isTyping = MutableStateFlow(false)

    // Audio recording state
    val isRecordingAudio = MutableStateFlow(false)
    val isRecordingPaused = MutableStateFlow(false)
    val recordingDurationSec = MutableStateFlow(0)
    val recordingAmplitudes = MutableStateFlow<List<Float>>(emptyList())
    private var recordTimerJob: Job? = null

    // App Lock & Vault Security — délégué à VaultSecurityManager (extrait P1-3)
    private val vaultSecurityManager = VaultSecurityManager.create(application, viewModelScope)
    val hasPin: StateFlow<Boolean> = vaultSecurityManager.hasPin
    val isPinProtectionEnabled: StateFlow<Boolean> = vaultSecurityManager.isPinProtectionEnabled
    val isAppLocked: StateFlow<Boolean> = vaultSecurityManager.isAppLocked
    val enteredPin: StateFlow<String> = vaultSecurityManager.enteredPin
    val pinError: StateFlow<Boolean> = vaultSecurityManager.pinError

    // Theme mode (Monochrome Black & White)
    val themeMode = MutableStateFlow(ThemeMode.DARK)

    // Ephemeral messages timer
    val ephemeralTimerHours = MutableStateFlow(24)

    // Active call simulation
    val activeCall = MutableStateFlow<ActiveCallState?>(null)
    private var callTimerJob: Job? = null

    // Empty list of stories (no demo data)
    val stories = MutableStateFlow<List<StatusStory>>(emptyList())

    init {
        val savedName = prefs.getString("profile_display_name", null)
        val savedPhone = prefs.getString("profile_phone_number", null)
        val savedPhoto = prefs.getString("profile_photo_url", "")
        if (!savedName.isNullOrBlank()) {
            userProfile.value = UserProfile(
                userId = runCatching { Firebase.auth.currentUser?.uid }.getOrNull() ?: "kris_local_user",
                displayName = savedName,
                phoneNumber = savedPhone ?: "",
                photoUrl = savedPhoto ?: "",
                statusText = "En ligne • kris&her",
                avatarColorHex = "#FFFFFF"
            )
        }

        viewModelScope.launch {
            try {
                repository.seedInitialDataIfNeeded()
            } catch (e: Exception) {
                android.util.Log.e("ChatViewModel", "seedInitialDataIfNeeded warning: ${e.message}")
            }
            val user = try { Firebase.auth.currentUser } catch (e: Exception) { null }
            if (user != null) {
                try {
                    firestoreRepository.ensureUserProfile(user.displayName ?: "Kris")
                    // Le numéro n'est plus dans le profil public : on le restaure depuis la zone privée si besoin,
                    // puis on migre les anciens profils (retrait du numéro public, empreinte, nom en minuscules).
                    if (prefs.getString("profile_phone_number", null).isNullOrBlank()) {
                        firestoreRepository.loadPrivatePhone()?.takeIf { it.isNotBlank() }?.let { restored ->
                            prefs.edit().putString("profile_phone_number", restored).apply()
                        }
                    }
                    firestoreRepository.migrateProfilePrivacy(prefs.getString("profile_phone_number", null).orEmpty())
                } catch (e: Exception) {
                    android.util.Log.e("ChatViewModel", "ensureUserProfile warning: ${e.message}")
                }
                launch {
                    try {
                        firestoreRepository.observeUserProfile().collect { profile ->
                            // Mon numéro vient du stockage local (il n'est jamais dans le profil public)
                            userProfile.value = profile.copy(
                                phoneNumber = prefs.getString("profile_phone_number", null).orEmpty()
                            )
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("ChatViewModel", "observeUserProfile warning: ${e.message}")
                    }
                }
                launch {
                    try {
                        val token = com.google.firebase.messaging.FirebaseMessaging.getInstance().token.await()
                        firestoreRepository.savePushToken(token)
                    } catch (e: kotlinx.coroutines.CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        android.util.Log.w("ChatViewModel", "push token warning: ${e.message}")
                    }
                }
                launch {
                    try {
                        firestoreRepository.observeChats().collect { chats ->
                            syncChats(user.uid, chats)
                        }
                    } catch (e: kotlinx.coroutines.CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        android.util.Log.e("ChatViewModel", "observeChats warning: ${e.message}")
                    }
                }
                stickerViewModel.initStickers()
            }
        }

        // Reset message baseline when switching conversations
        viewModelScope.launch {
            currentConversationId.collect { convId ->
                if (convId != lastTrackedConversationId) {
                    lastTrackedConversationId = convId
                    lastTrackedMessageId = null
                }
            }
        }

        // Synchronisation temps réel des messages de la discussion ouverte (chats/{chatId}/messages) vers Room
        var firestoreMessagesJob: Job? = null
        viewModelScope.launch {
            currentConversationId.collect { convId ->
                firestoreMessagesJob?.cancel()
                val currentUser = currentFirebaseUser
                if (convId != null && currentUser != null) {
                    firestoreMessagesJob = launch {
                        try {
                            val chatId = repository.getConversationOnce(convId)?.remoteId.orEmpty()
                            if (chatId.isBlank()) return@launch
                            firestoreRepository.observeChatMessages(chatId).collect { snap ->
                                syncRemoteMessages(convId, chatId, currentUser.uid, snap)
                            }
                        } catch (e: kotlinx.coroutines.CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            android.util.Log.w("ChatViewModel", "sync messages warning: ${e.message}")
                        }
                    }
                }
            }
        }

        // Listen for incoming messages to trigger the New-School received sound effect
        viewModelScope.launch {
            activeMessages.collect { messages ->
                if (messages.isNotEmpty()) {
                    val last = messages.last()
                    if (lastTrackedMessageId == null) {
                        // Baseline on initial load of conversation: do not play sound for old history
                        lastTrackedMessageId = last.id
                    } else if (last.id != lastTrackedMessageId) {
                        // A new message has arrived in the active conversation
                        lastTrackedMessageId = last.id
                        if (last.senderId != "me" && last.type != MessageType.SYSTEM) {
                            audioManagerViewModel.playReceivedSoundEffect()
                        }
                    }
                }
            }
        }
    }

    /** Reflète dans Room les discussions Firestore dont je suis membre (nouvelle discussion, dernier message, non lus). */
    private suspend fun syncChats(myUid: String, chats: List<Chat>) {
        for (chat in chats) {
            try {
                val peerUid = chat.members.firstOrNull { it != myUid } ?: continue
                val lastTs = chat.lastMessageTimestamp?.toDate()?.time

                val hiddenAt = hiddenChats[chat.id]
                if (hiddenAt != null) {
                    if (lastTs != null && lastTs > hiddenAt) {
                        hiddenChats.remove(chat.id)
                        saveHiddenChats()
                    } else {
                        continue
                    }
                }

                var profile = peerProfileCache[peerUid]
                if (profile == null) {
                    profile = firestoreRepository.getUserProfile(peerUid)
                    if (profile != null) peerProfileCache[peerUid] = profile
                }

                syncMutex.withLock {
                    val existing = repository.findConversationByRemoteId(chat.id)
                    val title = profile?.displayName?.takeIf { it.isNotBlank() } ?: existing?.title ?: "Utilisateur"
                    val initials = title.split(" ")
                        .filter { it.isNotBlank() }
                        .take(2)
                        .map { it.first().uppercase() }
                        .joinToString("")
                        .ifEmpty { "?" }
                    val incoming = chat.lastSenderId.isNotBlank() && chat.lastSenderId != myUid

                    if (existing == null) {
                        val createdByMe = chat.createdBy == myUid
                        repository.insertConversation(
                            ConversationEntity(
                                title = title,
                                avatarColorHex = profile?.avatarColorHex ?: "#FFFFFF",
                                avatarInitials = initials,
                                lastMessage = chat.lastMessage.ifBlank { "Discussion démarrée" },
                                lastMessageTimestamp = lastTs ?: System.currentTimeMillis(),
                                unreadCount = if (incoming) 1 else 0,
                                isOnline = false,
                                statusText = profile?.statusText ?: "",
                                // Une discussion ouverte PAR quelqu'un d'autre arrive comme une demande à accepter ou bloquer
                                isAccepted = createdByMe,
                                isUnknownContact = !createdByMe,
                                phoneNumber = profile?.phoneNumber ?: "",
                                photoUrl = profile?.photoUrl ?: "",
                                remoteId = chat.id,
                                peerUid = peerUid,
                                lastReadAt = if (incoming) (lastTs ?: System.currentTimeMillis()) - 1 else (lastTs ?: System.currentTimeMillis())
                            )
                        )
                    } else {
                        var updated = existing.copy(
                            title = title,
                            avatarInitials = initials,
                            avatarColorHex = profile?.avatarColorHex ?: existing.avatarColorHex,
                            statusText = profile?.statusText ?: existing.statusText,
                            photoUrl = profile?.photoUrl ?: existing.photoUrl,
                            phoneNumber = profile?.phoneNumber?.takeIf { it.isNotBlank() } ?: existing.phoneNumber,
                            peerUid = peerUid
                        )
                        if (lastTs != null && lastTs > existing.lastMessageTimestamp) {
                            updated = updated.copy(
                                lastMessage = chat.lastMessage.ifBlank { existing.lastMessage },
                                lastMessageTimestamp = lastTs
                            )
                            if (incoming) {
                                if (currentConversationId.value == existing.id) {
                                    // Je suis dans la discussion : tout ce qui arrive est vu
                                    updated = updated.copy(unreadCount = 0, lastReadAt = System.currentTimeMillis())
                                } else {
                                    // Compte exact côté serveur ; +1 seulement si le comptage est impossible (hors-ligne)
                                    val exact = firestoreRepository.countUnreadFrom(chat.id, peerUid, existing.lastReadAt)
                                    updated = updated.copy(unreadCount = exact ?: (existing.unreadCount + 1))
                                }
                            }
                        }
                        if (updated != existing) repository.updateConversation(updated)
                    }
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                android.util.Log.w("ChatViewModel", "syncChats warning: ${e.message}")
            }
        }
    }

    /** Reflète dans Room les messages d'une discussion : nouveaux messages, réactions, accusés de lecture, suppressions. */
    private suspend fun syncRemoteMessages(convId: Long, chatId: String, myUid: String, snap: ChatMessagesSnapshot) {
        val toMarkRead = mutableListOf<String>()
        for (doc in snap.messages) {
            if (doc.id.isBlank() || doc.id in hiddenMessages) continue
            val isMe = doc.senderId == myUid
            val ts = doc.createdAt?.toDate()?.time ?: System.currentTimeMillis()
            val remoteStatus = when (doc.status) {
                "READ" -> MessageStatus.READ
                "DELIVERED" -> MessageStatus.DELIVERED
                else -> MessageStatus.SENT
            }
            val readTs = if (remoteStatus == MessageStatus.READ) (doc.updatedAt?.toDate()?.time ?: ts) else null
            val existing = repository.findMessageByRemoteId(doc.id)
            if (existing == null) {
                repository.insertMessage(
                    MessageEntity(
                        conversationId = convId,
                        senderId = if (isMe) "me" else doc.senderId,
                        senderName = doc.senderName,
                        content = doc.content,
                        type = try { MessageType.valueOf(doc.type) } catch (_: Exception) { MessageType.TEXT },
                        audioDurationSec = doc.audioDurationSec.toInt(),
                        mediaUri = materializeReceivedMedia(doc.id, doc.type, doc.mediaUri),
                        timestamp = ts,
                        sentTimestamp = ts,
                        deliveredTimestamp = null,
                        readTimestamp = if (isMe) readTs else null,
                        status = if (isMe) remoteStatus else MessageStatus.READ,
                        reactionEmoji = doc.reactionEmoji,
                        isEncrypted = false,
                        remoteId = doc.id
                    )
                )
            } else if (existing.reactionEmoji != doc.reactionEmoji || (isMe && existing.status != remoteStatus)) {
                repository.updateMessageFromRemote(
                    existing.id,
                    doc.reactionEmoji,
                    if (isMe) remoteStatus else existing.status,
                    if (isMe) readTs else existing.readTimestamp
                )
            }
            if (!isMe && doc.status != "READ") toMarkRead.add(doc.id)
        }

        // Messages supprimés par leur auteur : seulement quand le serveur a répondu (pas depuis le cache)
        if (!snap.fromCache) {
            val remoteIds = snap.messages.map { it.id }.toSet()
            repository.getRemoteIdsForConversation(convId)
                .filter { it !in remoteIds }
                .forEach { repository.deleteMessageByRemoteId(convId, it) }
        }

        // Le destinataire qui a la discussion ouverte confirme la lecture (vrai accusé, plus de simulation)
        if (toMarkRead.isNotEmpty() && currentConversationId.value == convId) {
            firestoreRepository.markChatMessagesRead(chatId, toMarkRead)
        }
    }

    fun openConversation(id: Long) {
        currentConversationId.value = id
        viewModelScope.launch {
            repository.markAsRead(id)
        }
    }

    fun closeConversation() {
        audioManagerViewModel.stopAudio()
        currentConversationId.value = null
    }

    fun setBubbleStyle(style: BubbleStyle) {
        bubbleStyle.value = style
    }

    fun setReplyingToMessage(message: MessageEntity?) {
        replyingToMessage.value = message
    }

    fun clearReplyingToMessage() {
        replyingToMessage.value = null
    }

    fun setFloatingBubbleConversationId(convId: Long?) {
        floatingBubbleConversationId.value = convId
    }

    fun completeOnboarding(
        displayName: String,
        phoneNumber: String,
        photoUrl: String
    ) {
        val uid = runCatching { Firebase.auth.currentUser?.uid }.getOrNull() ?: "kris_local_user"
        val cleanName = displayName.trim().ifBlank { "Kris" }
        val cleanPhone = phoneNumber.trim()

        prefs.edit()
            .putBoolean("has_completed_onboarding", true)
            .putString("profile_display_name", cleanName)
            .putString("profile_phone_number", cleanPhone)
            .putString("profile_photo_url", photoUrl)
            .apply()

        val newProfile = UserProfile(
            userId = uid,
            displayName = cleanName,
            phoneNumber = cleanPhone,
            photoUrl = photoUrl,
            statusText = "En ligne • kris&her",
            avatarColorHex = "#FFFFFF"
        )
        userProfile.value = newProfile
        hasCompletedOnboarding.value = true

        viewModelScope.launch {
            try {
                firestoreRepository.ensureUserProfile(
                    displayName = cleanName,
                    photoUrl = photoUrl,
                    phoneNumber = cleanPhone
                )
                firestoreRepository.updateUserProfile(
                    displayName = cleanName,
                    phoneNumber = cleanPhone,
                    photoUrl = photoUrl
                )
            } catch (e: Exception) {
                android.util.Log.w("ChatViewModel", "Sync onboarding profile warning: ${e.message}")
            }

            val googleEmail = runCatching { Firebase.auth.currentUser?.email }.getOrNull()
                ?: prefs.getString("google_auth_email", null)
                ?: ""

            // 1. Send official account confirmation email directly to the Google account mailbox (Gmail)
            //    L'email n'est envoyé que si l'utilisateur est connecté avec un compte Google.
            // 1. Send official account confirmation email (delegated to NotificationViewModel)
            if (googleEmail.isNotBlank()) {
                notificationViewModel.sendAccountCreationEmail(
                    email = googleEmail,
                    displayName = cleanName,
                    phoneNumber = cleanPhone
                )
            }
            // 4. Insert confirmation message to inbox so user has immediate proof on the account
            try {
                val googleSecurityConv = repository.findConversationByTitle("Bienvenue sur kris&her")
                val convId = googleSecurityConv?.id ?: repository.createNewConversation(
                    name = "Bienvenue sur kris&her",
                    initials = "G",
                    colorHex = "#4285F4",
                    statusText = "Notification officielle",
                    phoneNumber = "",
                    photoUrl = "",
                    isUnknownContact = false,
                    isAccepted = true,
                    showUnregisteredNotice = false
                )
                val now = System.currentTimeMillis()
                repository.insertMessage(
                    com.example.model.MessageEntity(
                        conversationId = convId,
                        senderId = "system_welcome",
                        senderName = "kris&her",
                        content = "🎉 Bienvenue sur kris&her !\n\nVotre compte de messagerie a été créé avec succès et rattaché à votre compte Google :\n📧 $googleEmail\n\nVos contacts peuvent désormais vous retrouver grâce à votre numéro $cleanPhone.\nUn message officiel de confirmation a été adressé à votre compte Google.",
                        type = MessageType.TEXT,
                        timestamp = now,
                        sentTimestamp = now,
                        status = MessageStatus.READ,
                        isEncrypted = false
                    )
                )

                val moiConv = repository.findConversationByTitle("Moi")
                if (moiConv != null) {
                    repository.sendMessage(
                        conversationId = moiConv.id,
                        content = "🛡️ Compte kris&her activé avec le compte Google $googleEmail et le numéro $cleanPhone.",
                        type = MessageType.TEXT
                    )
                }
            } catch (e: Exception) {
                android.util.Log.w("ChatViewModel", "Error creating security message: ${e.message}")
            }
        }
    }

    fun onUserSignOut() {
        prefs.edit().putBoolean("has_completed_onboarding", false).apply()
        hasCompletedOnboarding.value = false
        userProfile.value = null
    }

    fun sendMessage(
        content: String,
        type: MessageType = MessageType.TEXT,
        audioDuration: Int = 0,
        mediaUri: String? = null
    ) {
        val convId = currentConversationId.value ?: return
        if (content.isBlank() && type == MessageType.TEXT) return
        val activeConv = activeConversation.value
        if (activeConv?.isBlocked == true) return

        // If replying to a message, format with quote header [REPLY:author|snippet]
        val replyTarget = replyingToMessage.value
        val finalContent = if (replyTarget != null && type == MessageType.TEXT) {
            val author = replyTarget.senderName
            val snippet = replyTarget.content.replace("\n", " ").take(45)
            "[REPLY:$author|$snippet]\n$content"
        } else {
            content
        }
        replyingToMessage.value = null

        val chatId = activeConv?.remoteId.orEmpty()
        val isRemoteChat = chatId.isNotBlank()
        val isMoiSpace = activeConv?.title.orEmpty().equals("Moi", ignoreCase = true)
        val currentUser = runCatching { Firebase.auth.currentUser }.getOrNull()

        // Médias locaux (photo, vocal, document, sticker perso) : lisibles seulement sur CE téléphone,
        // donc téléversés dans Firebase Storage avant l'envoi pour que l'autre personne puisse les ouvrir.
        val mediaNeedsUpload = isRemoteChat &&
            type in setOf(MessageType.AUDIO, MessageType.IMAGE, MessageType.DOCUMENT, MessageType.STICKER) &&
            !isPortableMedia(mediaUri)

        // Play New-School comic sound effect when message is sent
        audioManagerViewModel.playSentSoundEffect()

        viewModelScope.launch {
            val remoteId = if (isRemoteChat) UUID.randomUUID().toString() else ""
            val insertedId = repository.sendMessage(convId, finalContent, type, audioDuration, mediaUri, remoteId)

            if (isMoiSpace) {
                // « Moi » est une bibliothèque personnelle (locale) : pas de destinataire.
                repository.updateMessageStatus(insertedId, MessageStatus.READ)
                repository.updateMessageRead(insertedId, System.currentTimeMillis())
            } else if (isRemoteChat && currentUser != null) {
                val senderName = userProfile.value?.displayName?.takeIf { it.isNotBlank() }
                    ?: currentUser.displayName?.takeIf { it.isNotBlank() }
                    ?: "Moi"

                // 1) Média local -> directement dans le message (gratuit) ou adresse https (Firebase Storage)
                var remoteMediaUri: String? = mediaUri?.takeIf { isPortableMedia(it) }
                var uploadFailed = false
                var uploadError: Throwable? = null
                if (mediaNeedsUpload && mediaUri != null) {
                    val prepared = if (useCloudStorage) {
                        firestoreRepository.uploadChatMedia(
                            context = getApplication<Application>(),
                            chatId = chatId,
                            messageId = remoteId,
                            localUri = mediaUri,
                            type = type.name
                        )
                    } else {
                        firestoreRepository.prepareInlineMedia(
                            context = getApplication<Application>(),
                            localUri = mediaUri,
                            type = type.name
                        )
                    }
                    remoteMediaUri = prepared.getOrNull()
                    uploadError = prepared.exceptionOrNull()
                    uploadFailed = remoteMediaUri == null
                }

                // 2) Message dans la discussion partagée
                val sent = !uploadFailed && firestoreRepository.sendChatMessage(
                    chatId = chatId,
                    messageId = remoteId,
                    content = finalContent,
                    type = type.name,
                    audioDurationSec = audioDuration.toLong(),
                    mediaUri = remoteMediaUri,
                    senderName = senderName
                ).isSuccess

                if (!sent) {
                    repository.deleteMessage(insertedId)
                    repository.insertMessage(
                        MessageEntity(
                            conversationId = convId,
                            senderId = "system",
                            senderName = "kris&her",
                            content = if (uploadFailed && uploadError is com.example.data.MediaTooLargeException) {
                                "Fichier trop lourd pour être envoyé (environ 330 Ko max). Les notes vocales sont limitées à environ 1 min 20."
                            } else if (uploadFailed) {
                                "Le fichier n'a pas pu être envoyé. Vérifiez votre connexion et réessayez."
                            } else {
                                "Message non envoyé. Vérifiez votre connexion et réessayez."
                            },
                            type = MessageType.SYSTEM,
                            status = MessageStatus.READ
                        )
                    )
                    // Remonter l'erreur à l'UI (Snackbar) en plus du message système.
                    reportError(
                        if (uploadFailed && uploadError is com.example.data.MediaTooLargeException) {
                            "Fichier trop lourd pour l'envoi (330 Ko max)"
                        } else if (uploadFailed) {
                            "Le fichier n'a pas pu être envoyé"
                        } else {
                            "Message non envoyé — vérifiez votre connexion"
                        }
                    )
                }
            }
            // Conversation locale sans interlocuteur inscrit : rien n'est délivré et aucune réponse n'est simulée.
        }
    }

    fun startAudioRecording() {
        audioManagerViewModel.startAudioRecording()
    }

    fun togglePauseRecording() {
        audioManagerViewModel.togglePauseRecording()
    }

    fun stopAndSendAudioRecording() {
        val (filePath, durationSec) = audioManagerViewModel.stopRecording()
        if (durationSec >= 1) {
            sendMessage(
                content = "🎤 Note vocale (${formatSeconds(durationSec)})",
                type = MessageType.AUDIO,
                audioDuration = durationSec,
                mediaUri = filePath
            )
        }
    }

    fun cancelAudioRecording() {
        audioManagerViewModel.cancelAudioRecording()
    }

    fun playAudio(messageId: Long, mediaUri: String?, durationSec: Int) {
        audioManagerViewModel.playAudio(messageId, mediaUri, durationSec)
    }

    fun togglePinConversation(id: Long) {
        viewModelScope.launch {
            repository.togglePin(id)
        }
    }

    fun deleteConversation(id: Long) {
        viewModelScope.launch {
            val conv = repository.getConversationOnce(id)
            repository.deleteConversation(id)
            // Suppression locale : la discussion ne revient que si un nouveau message arrive
            if (conv != null && conv.remoteId.isNotBlank()) {
                hiddenChats[conv.remoteId] = System.currentTimeMillis()
                saveHiddenChats()
            }
            if (currentConversationId.value == id) {
                currentConversationId.value = null
            }
        }
    }

    fun addReaction(messageId: Long, emoji: String) {
        viewModelScope.launch {
            repository.addReaction(messageId, emoji)
            val msg = repository.getMessageOnce(messageId)
            if (msg != null && msg.remoteId.isNotBlank() && currentFirebaseUser != null) {
                val chatId = repository.getConversationOnce(msg.conversationId)?.remoteId.orEmpty()
                if (chatId.isNotBlank()) {
                    firestoreRepository.setChatReaction(chatId, msg.remoteId, emoji)
                }
            }
        }
    }

    fun deleteMessage(messageId: Long) {
        viewModelScope.launch {
            val msg = repository.getMessageOnce(messageId)
            repository.deleteMessage(messageId)
            if (msg != null && msg.remoteId.isNotBlank()) {
                if (msg.senderId == "me") {
                    // Mon message : suppression pour tout le monde
                    if (currentFirebaseUser != null) {
                        val chatId = repository.getConversationOnce(msg.conversationId)?.remoteId.orEmpty()
                        if (chatId.isNotBlank()) firestoreRepository.deleteChatMessage(chatId, msg.remoteId)
                    }
                } else {
                    // Message reçu : suppression pour moi seulement
                    hiddenMessages.add(msg.remoteId)
                    saveHiddenMessages()
                }
            }
        }
    }

    fun createNewFriend(
        name: String,
        statusText: String = "Disponible",
        colorHex: String = "#FFFFFF",
        initials: String = "",
        phoneNumber: String = "",
        photoUrl: String = "",
        isUnknownContact: Boolean = false,
        isAccepted: Boolean = true,
        peerUid: String = ""
    ) {
        viewModelScope.launch {
            val calcInitials = initials.ifBlank {
                name.split(" ")
                    .filter { it.isNotBlank() }
                    .take(2)
                    .map { it.first().uppercase() }
                    .joinToString("")
                    .ifEmpty { "KH" }
            }
            val finalColor = colorHex.ifBlank { "#FFFFFF" }
            val finalStatus = statusText.ifBlank { "Disponible" }

            // Interlocuteur inscrit : on ouvre (ou crée) la discussion PARTAGÉE dans Firestore
            if (peerUid.isNotBlank() && currentFirebaseUser != null) {
                val chatId = firestoreRepository.openChat(peerUid).getOrNull()
                if (chatId == null) {
                    android.widget.Toast.makeText(
                        getApplication<Application>(),
                        "Impossible d'ouvrir la discussion. Vérifiez votre connexion.",
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                    return@launch
                }
                if (hiddenChats.remove(chatId) != null) saveHiddenChats()
                val localId = syncMutex.withLock {
                    repository.findConversationByRemoteId(chatId)?.id
                        ?: repository.createNewConversation(
                            name = name,
                            initials = calcInitials,
                            colorHex = finalColor,
                            statusText = finalStatus,
                            phoneNumber = phoneNumber,
                            photoUrl = photoUrl,
                            isUnknownContact = false,
                            isAccepted = true, // c'est moi qui l'ouvre
                            remoteId = chatId,
                            peerUid = peerUid
                        )
                }
                openConversation(localId)
                return@launch
            }

            // Contact non inscrit (ou saisi par numéro) : conversation locale, rien n'est délivré
            val newId = repository.createNewConversation(
                name = name,
                initials = calcInitials,
                colorHex = finalColor,
                statusText = finalStatus,
                phoneNumber = phoneNumber,
                photoUrl = photoUrl,
                isUnknownContact = isUnknownContact,
                isAccepted = isAccepted
            )
            openConversation(newId)
        }
    }

    fun createNewConversation(
        name: String,
        statusText: String = "Disponible",
        colorHex: String = "#FFFFFF",
        initials: String = "",
        phoneNumber: String = "",
        photoUrl: String = "",
        isUnknownContact: Boolean = false,
        isAccepted: Boolean = true,
        peerUid: String = ""
    ) {
        createNewFriend(
            name = name,
            statusText = statusText,
            colorHex = colorHex,
            initials = initials,
            phoneNumber = phoneNumber,
            photoUrl = photoUrl,
            isUnknownContact = isUnknownContact,
            isAccepted = isAccepted,
            peerUid = peerUid
        )
    }

    fun blockContact(conversationId: Long) {
        viewModelScope.launch {
            repository.setBlockedStatus(conversationId, true)
        }
    }

    fun unblockContact(conversationId: Long) {
        viewModelScope.launch {
            repository.setBlockedStatus(conversationId, false)
        }
    }

    fun acceptContact(conversationId: Long) {
        viewModelScope.launch {
            repository.setAcceptedStatus(conversationId, isAccepted = true, isUnknownContact = false)
        }
    }

    suspend fun searchUsersByPhoneNumbers(phoneNumbers: List<String>): List<UserProfile> {
        val result = firestoreRepository.searchUsersByPhoneNumbers(phoneNumbers)
        return result.getOrDefault(emptyList())
    }

    // Call Actions
    fun startCall(isVideo: Boolean) {
        val conv = activeConversation.value ?: return
        callTimerJob?.cancel()
        activeCall.value = ActiveCallState(
            conversationTitle = conv.title,
            avatarInitials = conv.avatarInitials,
            avatarColorHex = conv.avatarColorHex,
            isVideo = isVideo,
            durationSeconds = 0
        )
        callTimerJob = viewModelScope.launch {
            while (activeCall.value != null) {
                delay(1000)
                activeCall.value = activeCall.value?.copy(
                    durationSeconds = (activeCall.value?.durationSeconds ?: 0) + 1
                )
            }
        }
    }

    fun toggleCallMute() {
        activeCall.value = activeCall.value?.copy(isMuted = !(activeCall.value?.isMuted ?: false))
    }

    fun toggleCallSpeaker() {
        activeCall.value = activeCall.value?.copy(isSpeaker = !(activeCall.value?.isSpeaker ?: false))
    }

    fun endCall() {
        callTimerJob?.cancel()
        activeCall.value = null
    }

    // PIN vault security actions — déléguées à VaultSecurityManager (extrait P1-3)

    /**
     * Définit (ou change) le code PIN à 4 chiffres choisi par l'utilisateur.
     * Le hachage est effectué hors du thread principal par VaultSecurityManager.
     *
     * @return true si le format du PIN est valide (l'enregistrement est asynchrone).
     */
    fun setPin(pin: String): Boolean {
        if (pin.length != 4 || !pin.all { it.isDigit() }) return false
        vaultSecurityManager.setPin(pin)
        return true
    }

    fun appendPinDigit(digit: String) = vaultSecurityManager.appendPinDigit(digit)

    fun deletePinDigit() = vaultSecurityManager.deletePinDigit()

    fun togglePinProtection(enabled: Boolean) = vaultSecurityManager.togglePinProtection(enabled)

    fun lockVaultNow() = vaultSecurityManager.lockVaultNow()

    fun postNewStatus(caption: String, emoji: String) {
        val newStory = StatusStory(
            id = "story_${System.currentTimeMillis()}",
            authorName = "Mon Statut",
            authorInitials = "MOI",
            avatarColorHex = "#FFFFFF",
            timeAgo = "À l'instant",
            caption = caption,
            emojiTag = emoji.ifBlank { "⚡" }
        )
        stories.value = listOf(newStory) + stories.value
        viewModelScope.launch {
            if (currentFirebaseUser != null) {
                firestoreRepository.postStory(caption, emoji)
            }
        }
    }

    fun toggleSoundEffects() {
        audioManagerViewModel.toggleSoundEffects()
    }

    fun playSentSoundEffect() {
        audioManagerViewModel.playSentSoundEffect()
    }

    fun playReceivedSoundEffect() {
        audioManagerViewModel.playReceivedSoundEffect()
    }

    fun saveUserProfile(
        displayName: String,
        phoneNumber: String = "",
        photoUrl: String,
        statusText: String = "Messagerie privée",
        avatarColorHex: String = "#FF643D",
        onComplete: ((Boolean) -> Unit)? = null
    ) {
        viewModelScope.launch {
            isSavingProfile.value = true
            profileSaveMessage.value = null
            val result = firestoreRepository.updateUserProfile(
                displayName = displayName.trim(),
                phoneNumber = phoneNumber.trim(),
                photoUrl = photoUrl,
                statusText = statusText.trim(),
                avatarColorHex = avatarColorHex
            )
            isSavingProfile.value = false
            if (result.isSuccess) {
                profileSaveMessage.value = "Profil & Numéro synchronisés avec Firestore ! ☁️"
                onComplete?.invoke(true)
            } else {
                val detail = result.exceptionOrNull()?.message ?: "Échec de la synchronisation"
                profileSaveMessage.value = "Erreur: $detail"
                reportError("Impossible de synchroniser le profil : $detail")
                onComplete?.invoke(false)
            }
        }
    }

    suspend fun searchUsersInFirestore(query: String): List<UserProfile> {
        val res = firestoreRepository.searchUsers(query)
        return res.getOrDefault(emptyList())
    }

    fun clearProfileMessage() {
        profileSaveMessage.value = null
    }

    fun saveCustomSticker(item: CustomStickerItem) {
        stickerViewModel.saveCustomSticker(item)
    }

    fun deleteCustomSticker(id: String) {
        stickerViewModel.deleteCustomSticker(id)
    }

    override fun onCleared() {
        super.onCleared()
        audioManagerViewModel.stopAudio()
        audioManagerViewModel.release()
    }

    private fun formatSeconds(sec: Int): String {
        val m = sec / 60
        val s = sec % 60
        return "%d:%02d".format(m, s)
    }
}
