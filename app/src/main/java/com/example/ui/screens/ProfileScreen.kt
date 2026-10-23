package com.example.ui.screens

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SentimentSatisfiedAlt
import com.example.ui.components.BubbleStyle
import com.example.ui.components.BubbleStyleSelectorDialog
import com.example.ui.theme.NewSchoolCyan
import com.example.ui.theme.NewSchoolPink
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import com.example.model.UserProfile
import com.example.ui.components.EnteAvatar
import com.example.ui.components.KrisLogo
import com.example.ui.components.UserProfileAvatar
import com.example.ui.components.UserProfileComponent
import com.example.ui.screens.signOutUser
import com.example.ui.theme.KablammoFontFamily
import com.example.ui.theme.NewSchoolCyan
import com.example.ui.theme.NewSchoolPink
import com.example.ui.theme.PureBlack
import com.example.ui.theme.PureWhite
import com.example.viewmodel.ThemeMode
import com.google.firebase.Firebase
import com.google.firebase.auth.auth

@Composable
fun ProfileScreen(
    currentTheme: ThemeMode,
    onSelectTheme: (ThemeMode) -> Unit,
    userProfile: UserProfile? = null,
    isSavingProfile: Boolean = false,
    profileSaveMessage: String? = null,
    onSaveProfile: (displayName: String, phoneNumber: String, photoUrl: String, statusText: String, avatarColorHex: String) -> Unit = { _, _, _, _, _ -> },
    onClearProfileMessage: () -> Unit = {},
    isSoundEffectsEnabled: Boolean = true,
    onToggleSoundEffects: () -> Unit = {},
    onTestSentSound: () -> Unit = {},
    onTestReceivedSound: () -> Unit = {},
    onOpenPermissions: () -> Unit = {},
    onOpenStickerStudio: () -> Unit = {},
    currentBubbleStyle: BubbleStyle = BubbleStyle.COMIC,
    onSelectBubbleStyle: (BubbleStyle) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }
    val currentUser = try { Firebase.auth.currentUser } catch (e: Exception) { null }

    var notificationsEnabled by remember { mutableStateOf(true) }
    var showBubbleStyleDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(PureBlack)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        item {
            // Profile Header
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                UserProfileAvatar(
                    photoUrl = userProfile?.photoUrl ?: "",
                    displayName = userProfile?.displayName?.ifBlank { currentUser?.displayName ?: "Kris" } ?: (currentUser?.displayName ?: "Kris"),
                    size = 80.dp,
                    borderColor = PureWhite
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = userProfile?.displayName?.ifBlank { "kris&her" } ?: "kris&her",
                    fontFamily = KablammoFontFamily,
                    fontSize = 24.sp,
                    color = PureWhite
                )
                Text(
                    text = userProfile?.statusText ?: "En ligne • kris&her",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFAAAAAA)
                )

                val phone = userProfile?.phoneNumber ?: ""
                if (phone.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        color = Color(0xFF141414),
                        shape = RoundedCornerShape(20.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00E5FF))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Phone,
                                contentDescription = null,
                                tint = Color(0xFF00E5FF),
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = phone,
                                color = PureWhite,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "💡 Aucun numéro configuré (entrez-le ci-dessous pour être trouvable)",
                        color = Color(0xFFFFAA00),
                        fontSize = 11.sp
                    )
                }
            }

            // User Profile Component with Firestore Storage
            UserProfileComponent(
                currentProfile = userProfile,
                isSaving = isSavingProfile,
                saveMessage = profileSaveMessage,
                onSaveProfile = onSaveProfile,
                onClearMessage = onClearProfileMessage,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Google Account & Cloud Firebase Status Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F0F0F)),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF333333)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CloudDone,
                            contentDescription = null,
                            tint = PureWhite,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "COMPTE GOOGLE & FIREBASE",
                            fontFamily = KablammoFontFamily,
                            fontSize = 14.sp,
                            color = PureWhite
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = currentUser?.displayName ?: "Utilisateur kris&her",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = PureWhite
                    )
                    Text(
                        text = currentUser?.email ?: "Connecté à Firebase Firestore",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFAAAAAA)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            signOutUser(
                                context = context,
                                credentialManager = credentialManager,
                                onSignOutComplete = {},
                                scope = coroutineScope
                            )
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PureWhite.copy(alpha = 0.12f),
                            contentColor = PureWhite
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("signout_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = "Déconnexion",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Se déconnecter", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Theme Selection (Monochrome Noir ou Blanc)
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F0F0F)),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF333333)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.DarkMode,
                            contentDescription = null,
                            tint = PureWhite,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Thème monochrome",
                            fontFamily = KablammoFontFamily,
                            fontSize = 15.sp,
                            color = PureWhite
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val themes = listOf(
                            ThemeMode.DARK to "NOIR PUR",
                            ThemeMode.LIGHT to "BLANC PUR"
                        )
                        for ((t, label) in themes) {
                            val isSelected = currentTheme == t
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) PureWhite else Color(0xFF1A1A1A),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.5.dp,
                                    if (isSelected) PureWhite else Color(0xFF333333)
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { onSelectTheme(t) }
                                    .testTag("theme_btn_${t.name.lowercase()}")
                            ) {
                                Text(
                                    text = label,
                                    fontFamily = KablammoFontFamily,
                                    fontSize = 12.sp,
                                    color = if (isSelected) PureBlack else PureWhite,
                                    modifier = Modifier.padding(vertical = 10.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }

            // Bulles de discussion (Chat Bubbles) Customization Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F0F0F)),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF333333)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.ChatBubble,
                                contentDescription = null,
                                tint = NewSchoolCyan,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Bulles de discussion",
                                    fontFamily = KablammoFontFamily,
                                    fontSize = 15.sp,
                                    color = PureWhite
                                )
                                Text(
                                    text = "Forme des bulles, queue BD & contours",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF888888)
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF1E1E1E),
                            border = androidx.compose.foundation.BorderStroke(1.dp, NewSchoolCyan)
                        ) {
                            Text(
                                text = currentBubbleStyle.title,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = NewSchoolCyan,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = { showBubbleStyleDialog = true },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = PureWhite),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, PureWhite),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("change_bubble_style_btn")
                    ) {
                        Text(
                            text = "Personnaliser les bulles de discussion",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            // Notifications
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F0F0F)),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF333333)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = null,
                            tint = PureWhite,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Notifications",
                                fontFamily = KablammoFontFamily,
                                fontSize = 15.sp,
                                color = PureWhite
                            )
                            Text(
                                text = "Alertes privées discrètes",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF888888)
                            )
                        }
                    }

                    Switch(
                        checked = notificationsEnabled,
                        onCheckedChange = { notificationsEnabled = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = PureBlack,
                            checkedTrackColor = PureWhite
                        )
                    )
                }
            }

            // New-School Sound Effects Card (MediaPlayer)
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F0F0F)),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, if (isSoundEffectsEnabled) PureWhite else Color(0xFF333333)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = null,
                                tint = PureWhite,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "BRUITAGES NEW-SCHOOL",
                                    fontFamily = KablammoFontFamily,
                                    fontSize = 14.sp,
                                    color = PureWhite
                                )
                                Text(
                                    text = "Sons d'envoi et réception (MediaPlayer)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF888888)
                                )
                            }
                        }

                        Switch(
                            checked = isSoundEffectsEnabled,
                            onCheckedChange = { onToggleSoundEffects() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = PureBlack,
                                checkedTrackColor = PureWhite
                            ),
                            modifier = Modifier.testTag("sound_effects_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Tester les effets sonores :",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFAAAAAA)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onTestSentSound,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF1E1E1E),
                                contentColor = PureWhite
                            ),
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, PureWhite.copy(alpha = 0.4f)),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("test_sent_sound_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Envoi 🚀", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = onTestReceivedSound,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF1E1E1E),
                                contentColor = PureWhite
                            ),
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, PureWhite.copy(alpha = 0.4f)),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("test_received_sound_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Réception 🔔", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Mobile Permissions Management Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F0F0F)),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, PureWhite.copy(alpha = 0.6f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = PureWhite,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "AUTORISATIONS MOBILES",
                                    fontFamily = KablammoFontFamily,
                                    fontSize = 14.sp,
                                    color = PureWhite
                                )
                                Text(
                                    text = "Micro, Caméra, Médias Galerie & Contacts",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF888888)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = onOpenPermissions,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PureWhite,
                            contentColor = PureBlack
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("open_permissions_manager_button")
                    ) {
                        Text(
                            text = "GÉRER LES AUTORISATIONS",
                            fontFamily = KablammoFontFamily,
                            fontSize = 12.5.sp,
                            color = PureBlack
                        )
                    }
                }
            }

            // Custom Sticker & Emoticon Studio Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F0F0F)),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, NewSchoolPink),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.SentimentSatisfiedAlt,
                                contentDescription = null,
                                tint = NewSchoolPink,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "STUDIO AUTOCOLLANTS",
                                    fontFamily = KablammoFontFamily,
                                    fontSize = 14.sp,
                                    color = PureWhite
                                )
                                Text(
                                    text = "Créez depuis images & vidéos de votre galerie",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF888888)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = onOpenStickerStudio,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1E1E1E),
                            contentColor = PureWhite
                        ),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, NewSchoolPink),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("open_profile_sticker_studio_button")
                    ) {
                        Text(
                            text = "🎨 OUVRIR LE STUDIO",
                            fontFamily = KablammoFontFamily,
                            fontSize = 12.5.sp,
                            color = PureWhite
                        )
                    }
                }
            }

            // About kris&her
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F0F0F)),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF333333)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 80.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = PureWhite,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "kris&her v1.0",
                            fontFamily = KablammoFontFamily,
                            fontSize = 15.sp,
                            color = PureWhite
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Messagerie privée conçue sur mesure. Police Kablamo, design monochrome noir & blanc et stickers new-school.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFAAAAAA)
                    )
                }
            }
        }
    }

    if (showBubbleStyleDialog) {
        BubbleStyleSelectorDialog(
            currentStyle = currentBubbleStyle,
            onSelectStyle = {
                onSelectBubbleStyle(it)
                showBubbleStyleDialog = false
            },
            onDismiss = { showBubbleStyleDialog = false }
        )
    }
}
