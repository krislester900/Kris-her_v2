package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.example.ui.components.queryDeviceContacts
import com.example.ui.components.DeviceContactItem
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import com.example.model.UserProfile
import com.example.ui.components.UserProfileAvatar
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ConversationEntity
import com.example.ui.components.EnteAvatar
import com.example.ui.components.KrisLogo
import com.example.ui.components.NewSchoolEmojiBadge
import com.example.ui.theme.KablammoFontFamily
import com.example.ui.theme.NewSchoolCyan
import com.example.ui.theme.NewSchoolLime
import com.example.ui.theme.NewSchoolPink
import com.example.ui.theme.NewSchoolYellow
import com.example.ui.theme.PureBlack
import com.example.ui.theme.PureWhite
import com.example.viewmodel.ChatFilter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ChatListScreen(
    conversations: List<ConversationEntity>,
    searchQuery: String,
    selectedFilter: ChatFilter,
    userProfile: UserProfile? = null,
    onSearchQueryChange: (String) -> Unit,
    onFilterSelect: (ChatFilter) -> Unit,
    onConversationClick: (Long) -> Unit,
    onTogglePin: (Long) -> Unit,
    onDeleteConversation: (Long) -> Unit,
    onAddNewFriend: (name: String, status: String, colorHex: String, initials: String) -> Unit,
    onSearchUsers: (suspend (String) -> List<UserProfile>)? = null,
    onSearchUsersByPhoneNumbers: (suspend (List<String>) -> List<UserProfile>)? = null,
    onStartDirectChat: ((name: String, status: String, colorHex: String, initials: String, phoneNumber: String, photoUrl: String, isUnknownContact: Boolean, isAccepted: Boolean, peerUid: String) -> Unit)? = null,
    onNavigateToProfile: () -> Unit = {},
    onOpenSecurityVault: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showNewFriendDialog by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(modifier = Modifier.fillMaxSize()) {
            // kris&her Header in Kablammo font
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    KrisLogo(
                        size = 40.dp,
                        shape = RoundedCornerShape(12.dp),
                        borderColor = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "kris&her",
                            fontFamily = KablammoFontFamily,
                            fontSize = 24.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.onBackground)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "MESSAGERIE PRIVÉE",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 9.sp
                            )
                        }
                    }
                }

                IconButton(
                    onClick = onOpenSecurityVault,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.5.dp, MaterialTheme.colorScheme.onBackground, CircleShape)
                        .testTag("vault_security_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = "Coffre de sécurité",
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .testTag("search_input"),
                placeholder = {
                    Text(
                        text = "Rechercher...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Recherche",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Effacer",
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedBorderColor = MaterialTheme.colorScheme.onBackground,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Filter Chips in Black & White with crisp comic borders
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(ChatFilter.values()) { filter ->
                    val isSelected = filter == selectedFilter
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.surface,
                        border = androidx.compose.foundation.BorderStroke(
                            1.5.dp,
                            MaterialTheme.colorScheme.onBackground
                        ),
                        modifier = Modifier
                            .clickable { onFilterSelect(filter) }
                            .testTag("filter_${filter.name.lowercase()}")
                    ) {
                        Text(
                            text = filter.label,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Phone number setup reminder banner if user has not yet entered their phone number
            if (userProfile != null && userProfile.phoneNumber.isBlank()) {
                Surface(
                    color = Color(0xFF141414),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.2.dp, NewSchoolCyan),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 2.dp)
                        .clickable { onNavigateToProfile() }
                        .testTag("setup_phone_reminder_banner")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(NewSchoolCyan.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Phone,
                                contentDescription = null,
                                tint = NewSchoolCyan,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Ajoutez votre numéro dans votre profil 📱",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = PureWhite
                            )
                            Text(
                                text = "Permet à vos amis de vous ajouter avec votre numéro",
                                fontSize = 11.sp,
                                color = Color(0xFFAAAAAA)
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = null,
                            tint = NewSchoolCyan,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Conversations List / Clean Empty State
            if (conversations.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 32.dp, vertical = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Logo k en noir police kablammo et le reste en blanc
                        KrisLogo(
                            size = 72.dp,
                            shape = CircleShape,
                            showShadow = true,
                            borderColor = MaterialTheme.colorScheme.onBackground
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        NewSchoolEmojiBadge(
                            emoji = "⚡",
                            label = "kris&her",
                            borderColor = NewSchoolPink,
                            isDarkBg = true
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "AUCUN CHAT",
                            fontFamily = KablammoFontFamily,
                            fontSize = 24.sp,
                            color = MaterialTheme.colorScheme.onBackground,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Votre messagerie privée est vide. Ajoutez un ami pour commencer à échanger des messages.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = { showNewFriendDialog = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.onBackground,
                                contentColor = MaterialTheme.colorScheme.background
                            ),
                            shape = RoundedCornerShape(14.dp),
                            border = androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.onBackground),
                            modifier = Modifier.testTag("empty_state_new_chat_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "NOUVELLE DISCUSSION",
                                fontFamily = KablammoFontFamily,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp)
                ) {
                    items(conversations, key = { it.id }) { conv ->
                        ConversationItem(
                            conversation = conv,
                            onClick = { onConversationClick(conv.id) },
                            onTogglePin = { onTogglePin(conv.id) },
                            onDelete = { onDeleteConversation(conv.id) }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(88.dp))
                    }
                }
            }
        }

        // New Chat Floating Action Button with New School colored outline!
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
        ) {
            FloatingActionButton(
                onClick = { showNewFriendDialog = true },
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .border(2.5.dp, NewSchoolCyan, RoundedCornerShape(18.dp))
                    .testTag("new_chat_fab")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Nouvelle discussion",
                    modifier = Modifier.size(30.dp),
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
        }
    }

    if (showNewFriendDialog) {
        NewFriendDialog(
            onDismiss = { showNewFriendDialog = false },
            onAdd = { name, status, color, initials ->
                onAddNewFriend(name, status, color, initials)
                showNewFriendDialog = false
            },
            onSearchUsers = onSearchUsers,
            onSearchUsersByPhoneNumbers = onSearchUsersByPhoneNumbers,
            onStartDirectChat = { name, status, color, initials, phone, pic, isUnknown, isAccepted, peerUid ->
                if (onStartDirectChat != null) {
                    onStartDirectChat(name, status, color, initials, phone, pic, isUnknown, isAccepted, peerUid)
                } else {
                    onAddNewFriend(name, status, color, initials)
                }
                showNewFriendDialog = false
            },
            onNavigateToProfile = onNavigateToProfile
        )
    }
}

@Composable
fun ConversationItem(
    conversation: ConversationEntity,
    onClick: () -> Unit,
    onTogglePin: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.5.dp,
            if (conversation.isPinned) PureWhite else MaterialTheme.colorScheme.outline
        ),
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick)
            .testTag("conversation_item_${conversation.id}")
    ) {
        val isMoi = conversation.title.equals("Moi", ignoreCase = true)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            if (isMoi) {
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF0F172A))
                        .border(2.dp, NewSchoolCyan, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("📁", fontSize = 24.sp)
                }
            } else {
                EnteAvatar(
                    initials = conversation.avatarInitials,
                    colorHex = "#FFFFFF",
                    size = 50.dp,
                    showOnline = conversation.isOnline,
                    borderColor = PureWhite
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (isMoi) "Moi (Bibliothèque)" else conversation.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (isMoi) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                color = Color(0xFF0F172A),
                                shape = RoundedCornerShape(6.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, NewSchoolCyan)
                            ) {
                                Text(
                                    text = "STOCKAGE",
                                    color = NewSchoolCyan,
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                )
                            }
                        } else if (conversation.isPinned) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.PushPin,
                                contentDescription = "Épinglé",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    Text(
                        text = formatTimestamp(conversation.lastMessageTimestamp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isMoi) {
                            conversation.lastMessage.ifEmpty { "Stockage personnel • Notes, photos & médias sauvegardés" }
                        } else {
                            conversation.lastMessage.ifEmpty { "Discussion sécurisée" }
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (conversation.unreadCount > 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (conversation.unreadCount > 0) FontWeight.Bold else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    if (conversation.unreadCount > 0) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.onSurface)
                                .padding(horizontal = 7.dp, vertical = 2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = conversation.unreadCount.toString(),
                                color = MaterialTheme.colorScheme.background,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NewFriendDialog(
    onDismiss: () -> Unit,
    onAdd: (name: String, status: String, colorHex: String, initials: String) -> Unit,
    onSearchUsers: (suspend (String) -> List<UserProfile>)? = null,
    onSearchUsersByPhoneNumbers: (suspend (List<String>) -> List<UserProfile>)? = null,
    onStartDirectChat: ((name: String, status: String, colorHex: String, initials: String, phoneNumber: String, photoUrl: String, isUnknownContact: Boolean, isAccepted: Boolean, peerUid: String) -> Unit)? = null,
    onNavigateToProfile: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var selectedTab by remember { mutableStateOf(0) } // 0: Scanner Contacts, 1: Entrer Numéro, 2: Par Nom / Pseudo

    // --- TAB 0: SCANNER LES CONTACTS TÉLÉPHONIQUES ---
    var hasContactPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
        )
    }
    var isScanningContacts by remember { mutableStateOf(false) }
    var deviceContacts by remember { mutableStateOf<List<DeviceContactItem>>(emptyList()) }
    var registeredAppUsers by remember { mutableStateOf<List<UserProfile>>(emptyList()) }
    var hasScannedContacts by remember { mutableStateOf(false) }

    val contactPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            hasContactPermission = granted
            if (granted) {
                coroutineScope.launch {
                    isScanningContacts = true
                    val contacts = queryDeviceContacts(context)
                    deviceContacts = contacts
                    val phones = contacts.map { it.phoneNumber }
                    val matched = onSearchUsersByPhoneNumbers?.invoke(phones) ?: emptyList()
                    registeredAppUsers = matched
                    isScanningContacts = false
                    hasScannedContacts = true
                }
            }
        }
    )

    fun startContactScan() {
        if (!hasContactPermission) {
            contactPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
            return
        }
        coroutineScope.launch {
            isScanningContacts = true
            val contacts = queryDeviceContacts(context)
            deviceContacts = contacts
            val phones = contacts.map { it.phoneNumber }
            val matched = onSearchUsersByPhoneNumbers?.invoke(phones) ?: emptyList()
            registeredAppUsers = matched
            isScanningContacts = false
            hasScannedContacts = true
        }
    }

    LaunchedEffect(hasContactPermission) {
        if (hasContactPermission && !hasScannedContacts) {
            startContactScan()
        }
    }

    // --- TAB 1: ENTRER UN NUMÉRO (SI PAS DANS LES CONTACTS) ---
    var phoneInput by remember { mutableStateOf("") }
    var phoneSearchResults by remember { mutableStateOf<List<UserProfile>?>(null) }
    var isPhoneSearching by remember { mutableStateOf(false) }

    fun executePhoneSearch() {
        if (phoneInput.isBlank()) return
        coroutineScope.launch {
            isPhoneSearching = true
            phoneSearchResults = onSearchUsers?.invoke(phoneInput) ?: emptyList()
            isPhoneSearching = false
        }
    }

    // --- TAB 2: PAR NOM / PSEUDO ---
    var nameInput by remember { mutableStateOf("") }
    var nameSearchResults by remember { mutableStateOf<List<UserProfile>?>(null) }
    var isNameSearching by remember { mutableStateOf(false) }

    fun executeNameSearch() {
        if (nameInput.isBlank()) return
        coroutineScope.launch {
            isNameSearching = true
            nameSearchResults = onSearchUsers?.invoke(nameInput) ?: emptyList()
            isNameSearching = false
        }
    }

    // Manual fallback custom contact form
    var customName by remember { mutableStateOf("") }
    var customStatus by remember { mutableStateOf("En ligne • kris&her") }
    var showManualMode by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("new_friend_dialog"),
        shape = RoundedCornerShape(22.dp),
        title = {
            Column {
                Text(
                    text = "NOUVELLE DISCUSSION",
                    fontFamily = KablammoFontFamily,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Reconnaissance par numéro kris&her",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // 3 Tabs: 0: Scanner Contacts, 1: Entrer Numéro, 2: Nom/Pseudo
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = when (selectedTab) {
                                0 -> NewSchoolLime
                                1 -> NewSchoolCyan
                                else -> NewSchoolPink
                            }
                        )
                    }
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0; showManualMode = false },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Contacts, contentDescription = null, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Contacts", fontSize = 11.sp, fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal)
                            }
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1; showManualMode = false },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Numéro", fontSize = 11.sp, fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal)
                            }
                        }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2; showManualMode = false },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Pseudo", fontSize = 11.sp, fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal)
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                when (selectedTab) {
                    0 -> {
                        // TAB 0: SCANNER LES CONTACTS TÉLÉPHONIQUES
                        if (!hasContactPermission) {
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF131A14)),
                                border = BorderStroke(1.2.dp, NewSchoolLime),
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Contacts,
                                        contentDescription = null,
                                        tint = NewSchoolLime,
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "PERMISSION CONTACTS REQUISE",
                                        fontFamily = KablammoFontFamily,
                                        fontSize = 13.sp,
                                        color = PureWhite,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Pour voir qui a déjà l'application, kris&her compare les numéros de votre carnet d'adresses avec ceux enregistrés dans la base et vous propose de leur écrire directement.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFFDDDDDD),
                                        fontSize = 11.5.sp,
                                        textAlign = TextAlign.Center,
                                        lineHeight = 16.sp
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Button(
                                        onClick = { contactPermissionLauncher.launch(Manifest.permission.READ_CONTACTS) },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = NewSchoolLime,
                                            contentColor = PureBlack
                                        ),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.testTag("request_contact_permission_btn")
                                    ) {
                                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("AUTORISER L'ACCÈS AUX CONTACTS", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    }
                                }
                            }
                        } else {
                            // Permission granted: scanning or list
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Scan des numéros dans la base :",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    TextButton(onClick = { startContactScan() }) {
                                        Text("🔄 Scanner", fontSize = 11.sp, color = NewSchoolLime)
                                    }
                                }

                                if (isScanningContacts) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        CircularProgressIndicator(color = NewSchoolLime, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text("Recherche des numéros dans la base...", fontSize = 12.sp, color = PureWhite)
                                    }
                                } else {
                                    LazyColumn(
                                        modifier = Modifier.fillMaxWidth().height(260.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        // 1. Contacts using kris&her detected in DB
                                        if (registeredAppUsers.isNotEmpty()) {
                                            item {
                                                Text(
                                                    text = "✨ CONTACTS AYANT L'APPLICATION (${registeredAppUsers.size})",
                                                    fontFamily = KablammoFontFamily,
                                                    fontSize = 11.5.sp,
                                                    color = NewSchoolLime
                                                )
                                            }
                                            items(registeredAppUsers) { user ->
                                                val initials = user.displayName.split(" ")
                                                    .filter { it.isNotBlank() }
                                                    .take(2)
                                                    .map { it.first().uppercase() }
                                                    .joinToString("")
                                                    .ifEmpty { "KH" }

                                                Card(
                                                    shape = RoundedCornerShape(12.dp),
                                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF141E14)),
                                                    border = BorderStroke(1.2.dp, NewSchoolLime),
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth().padding(8.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        UserProfileAvatar(
                                                            photoUrl = user.photoUrl,
                                                            displayName = user.displayName,
                                                            size = 38.dp,
                                                            borderColor = NewSchoolLime
                                                        )
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        Column(modifier = Modifier.weight(1f)) {
                                                            Text(user.displayName, fontWeight = FontWeight.Bold, color = PureWhite, fontSize = 13.sp)
                                                            Text("Dans vos contacts", fontSize = 10.5.sp, color = NewSchoolLime)
                                                            Text("Inscrit sur kris&her", fontSize = 9.5.sp, color = Color(0xFFAAAAAA))
                                                        }
                                                        Button(
                                                            onClick = {
                                                                if (onStartDirectChat != null) {
                                                                    onStartDirectChat(user.displayName, user.statusText, user.avatarColorHex, initials, user.phoneNumber, user.photoUrl, false, true, user.userId)
                                                                } else {
                                                                    onAdd(user.displayName, user.statusText, user.avatarColorHex, initials)
                                                                }
                                                                onDismiss()
                                                            },
                                                            colors = ButtonDefaults.buttonColors(containerColor = NewSchoolLime, contentColor = PureBlack),
                                                            shape = RoundedCornerShape(8.dp),
                                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                                            modifier = Modifier.testTag("chat_contact_${user.userId}")
                                                        ) {
                                                            Text("DISCUTER", fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        // 2. Other phone contacts
                                        item {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "AUTRES CONTACTS DE VOTRE TÉLÉPHONE",
                                                fontFamily = KablammoFontFamily,
                                                fontSize = 11.sp,
                                                color = Color(0xFFAAAAAA)
                                            )
                                        }

                                        val unregistered = deviceContacts.filter { dc ->
                                            registeredAppUsers.none { ru ->
                                                ru.phoneHash.isNotBlank() && ru.phoneHash == com.example.util.phoneHashOf(dc.phoneNumber)
                                            }
                                        }

                                        items(unregistered) { contact ->
                                            Surface(
                                                color = Color(0xFF141414),
                                                shape = RoundedCornerShape(10.dp),
                                                border = BorderStroke(1.dp, Color(0xFF262626)),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Box(
                                                        modifier = Modifier.size(34.dp).clip(CircleShape).background(Color(0xFF2A2A2A)),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(contact.initials, fontWeight = FontWeight.Bold, color = PureWhite, fontSize = 11.sp)
                                                    }
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(contact.name, fontWeight = FontWeight.SemiBold, color = PureWhite, fontSize = 12.5.sp)
                                                        Text(contact.phoneNumber, fontSize = 10.5.sp, color = Color(0xFF888888))
                                                    }
                                                    OutlinedButton(
                                                        onClick = {
                                                            if (onStartDirectChat != null) {
                                                                onStartDirectChat(contact.name, "Disponible • kris&her", "#FFFFFF", contact.initials, contact.phoneNumber, "", true, false, "")
                                                            } else {
                                                                onAdd(contact.name, "Disponible • kris&her", "#FFFFFF", contact.initials)
                                                            }
                                                            onDismiss()
                                                        },
                                                        border = BorderStroke(1.dp, PureWhite),
                                                        shape = RoundedCornerShape(8.dp),
                                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                                    ) {
                                                        Text("ÉCRIRE", fontSize = 10.sp, color = PureWhite)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    1 -> {
                        // TAB 1: ENTRER UN NUMÉRO DIRECTEMENT (POUR CONTACT NON DANS LE CARNET)
                        Text(
                            text = "Entrez le numéro pour trouver son profil dans la base et lui envoyer un premier message direct en boîte de réception :",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 16.sp
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = phoneInput,
                            onValueChange = {
                                phoneInput = it
                                if (it.length >= 4) {
                                    executePhoneSearch()
                                } else if (it.isBlank()) {
                                    phoneSearchResults = null
                                }
                            },
                            label = { Text("Numéro de téléphone") },
                            placeholder = { Text("Ex: +33 6 12 34 56 78") },
                            leadingIcon = {
                                Icon(Icons.Default.Phone, contentDescription = null, tint = NewSchoolCyan)
                            },
                            trailingIcon = {
                                if (isPhoneSearching) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = NewSchoolCyan)
                                } else if (phoneInput.isNotEmpty()) {
                                    IconButton(onClick = { phoneInput = ""; phoneSearchResults = null }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Effacer")
                                    }
                                }
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("phone_search_input"),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Button(
                            onClick = { executePhoneSearch() },
                            enabled = phoneInput.isNotBlank() && !isPhoneSearching,
                            colors = ButtonDefaults.buttonColors(containerColor = NewSchoolCyan, contentColor = PureBlack),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().testTag("search_phone_btn")
                        ) {
                            Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("SCANNER LA BASE POUR CE NUMÉRO", fontWeight = FontWeight.Bold, fontSize = 11.5.sp)
                        }

                        // Results for Tab 1
                        if (phoneSearchResults != null) {
                            Spacer(modifier = Modifier.height(12.dp))
                            if (phoneSearchResults!!.isNotEmpty()) {
                                Text(
                                    text = "PROFIL TROUVÉ DANS LA BASE ✨",
                                    fontFamily = KablammoFontFamily,
                                    fontSize = 11.sp,
                                    color = NewSchoolCyan
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                LazyColumn(
                                    modifier = Modifier.fillMaxWidth().height(160.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    items(phoneSearchResults!!) { profile ->
                                        val initials = profile.displayName.split(" ")
                                            .filter { it.isNotBlank() }
                                            .take(2)
                                            .map { it.first().uppercase() }
                                            .joinToString("")
                                            .ifEmpty { "KH" }

                                        Card(
                                            shape = RoundedCornerShape(12.dp),
                                            colors = CardDefaults.cardColors(containerColor = Color(0xFF101B24)),
                                            border = BorderStroke(1.2.dp, NewSchoolCyan),
                                            modifier = Modifier.fillMaxWidth().testTag("found_profile_${profile.userId}")
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth().padding(10.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                UserProfileAvatar(
                                                    photoUrl = profile.photoUrl,
                                                    displayName = profile.displayName,
                                                    size = 42.dp,
                                                    borderColor = PureWhite
                                                )
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Text(profile.displayName, fontWeight = FontWeight.Bold, fontSize = 13.5.sp, color = PureWhite)
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Icon(Icons.Default.Check, contentDescription = "Vérifié", tint = NewSchoolCyan, modifier = Modifier.size(13.dp))
                                                    }
                                                    Text("Numéro reconnu", fontSize = 11.sp, color = NewSchoolCyan)
                                                    Text("Message direct en inbox (pas d'abonnement)", fontSize = 9.5.sp, color = Color(0xFFAAAAAA))
                                                }
                                                Button(
                                                    onClick = {
                                                        if (onStartDirectChat != null) {
                                                            onStartDirectChat(profile.displayName, profile.statusText, profile.avatarColorHex, initials, profile.phoneNumber, profile.photoUrl, true, false, profile.userId)
                                                        } else {
                                                            onAdd(profile.displayName, profile.statusText, profile.avatarColorHex, initials)
                                                        }
                                                        onDismiss()
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = NewSchoolCyan, contentColor = PureBlack),
                                                    shape = RoundedCornerShape(8.dp),
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                                    modifier = Modifier.testTag("start_chat_with_${profile.userId}")
                                                ) {
                                                    Text("ÉCRIRE", fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                                }
                                            }
                                        }
                                    }
                                }
                            } else {
                                Card(
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF141414)),
                                    border = BorderStroke(1.dp, Color(0xFF333333)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            text = "Aucun compte kris&her trouvé pour ce numéro.",
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 12.sp,
                                            color = PureWhite
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Ce numéro n'est pas inscrit : vos messages ne seront pas délivrés tant qu'il n'a pas de compte.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color(0xFFAAAAAA),
                                            fontSize = 11.sp
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Button(
                                            onClick = {
                                                if (onStartDirectChat != null) {
                                                    onStartDirectChat(phoneInput, "Contact non reconnu", "#FFFFFF", "KH", phoneInput, "", true, false, "")
                                                } else {
                                                    onAdd(phoneInput, "Contact non reconnu", "#FFFFFF", "KH")
                                                }
                                                onDismiss()
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = PureWhite, contentColor = PureBlack),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text("💬 ENVOYER UN MESSAGE EN INBOX", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    2 -> {
                        // TAB 2: RECHERCHE PAR PSEUDO / NOM
                        Text(
                            text = "Recherchez par nom de profil ou pseudo dans la base pour démarrer une discussion :",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = nameInput,
                            onValueChange = {
                                nameInput = it
                                if (it.length >= 2) {
                                    executeNameSearch()
                                } else if (it.isBlank()) {
                                    nameSearchResults = null
                                }
                            },
                            label = { Text("Nom ou @pseudo") },
                            placeholder = { Text("Ex: Kris, Her, @alex...") },
                            leadingIcon = {
                                Icon(Icons.Default.Person, contentDescription = null, tint = NewSchoolPink)
                            },
                            trailingIcon = {
                                if (isNameSearching) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = NewSchoolPink)
                                } else if (nameInput.isNotEmpty()) {
                                    IconButton(onClick = { nameInput = ""; nameSearchResults = null }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Effacer")
                                    }
                                }
                            },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("name_search_input"),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Button(
                            onClick = { executeNameSearch() },
                            enabled = nameInput.isNotBlank() && !isNameSearching,
                            colors = ButtonDefaults.buttonColors(containerColor = NewSchoolPink, contentColor = PureBlack),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().testTag("search_name_btn")
                        ) {
                            Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("RECHERCHER PAR PSEUDO", fontWeight = FontWeight.Bold, fontSize = 11.5.sp)
                        }

                        if (nameSearchResults != null) {
                            Spacer(modifier = Modifier.height(12.dp))
                            if (nameSearchResults!!.isNotEmpty()) {
                                LazyColumn(
                                    modifier = Modifier.fillMaxWidth().height(160.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    items(nameSearchResults!!) { profile ->
                                        val initials = profile.displayName.split(" ")
                                            .filter { it.isNotBlank() }
                                            .take(2)
                                            .map { it.first().uppercase() }
                                            .joinToString("")
                                            .ifEmpty { "KH" }

                                        Card(
                                            shape = RoundedCornerShape(12.dp),
                                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1F1218)),
                                            border = BorderStroke(1.2.dp, NewSchoolPink),
                                            modifier = Modifier.fillMaxWidth().testTag("found_profile_name_${profile.userId}")
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth().padding(10.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                UserProfileAvatar(
                                                    photoUrl = profile.photoUrl,
                                                    displayName = profile.displayName,
                                                    size = 40.dp,
                                                    borderColor = PureWhite
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(profile.displayName, fontWeight = FontWeight.Bold, fontSize = 13.5.sp, color = PureWhite)
                                                    if (profile.phoneNumber.isNotBlank()) {
                                                        Text("📞 ${profile.phoneNumber}", fontSize = 10.5.sp, color = NewSchoolPink)
                                                    }
                                                }
                                                Button(
                                                    onClick = {
                                                        if (onStartDirectChat != null) {
                                                            onStartDirectChat(profile.displayName, profile.statusText, profile.avatarColorHex, initials, profile.phoneNumber, profile.photoUrl, false, true, profile.userId)
                                                        } else {
                                                            onAdd(profile.displayName, profile.statusText, profile.avatarColorHex, initials)
                                                        }
                                                        onDismiss()
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = NewSchoolPink, contentColor = PureBlack),
                                                    shape = RoundedCornerShape(8.dp),
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                                                ) {
                                                    Text("DISCUTER", fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Optional manual fallback
                if (showManualMode) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Surface(
                        color = Color(0xFF181818),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, Color(0xFF444444)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("CRÉATION MANUELLE", fontFamily = KablammoFontFamily, fontSize = 12.sp, color = PureWhite)
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = customName,
                                onValueChange = { customName = it },
                                label = { Text("Nom du contact") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().testTag("manual_contact_name_input"),
                                shape = RoundedCornerShape(10.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    if (customName.isNotBlank()) {
                                        onAdd(customName.trim(), customStatus.trim(), "#FFFFFF", "")
                                    }
                                },
                                enabled = customName.isNotBlank(),
                                colors = ButtonDefaults.buttonColors(containerColor = PureWhite, contentColor = PureBlack),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth().testTag("manual_contact_submit_btn")
                            ) {
                                Text("AJOUTER ET DÉMARRER", fontFamily = KablammoFontFamily, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (!showManualMode) {
                TextButton(onClick = { showManualMode = true }) {
                    Text("Créer manuellement", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.5.sp)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Fermer", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    )
}

private fun formatTimestamp(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    return when {
        diff < 1000 * 60 * 60 * 24 -> SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
        diff < 1000 * 60 * 60 * 48 -> "Hier"
        else -> SimpleDateFormat("dd/MM", Locale.getDefault()).format(Date(timestamp))
    }
}
