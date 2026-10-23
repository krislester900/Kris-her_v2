package com.example.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.asImageBitmap
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.model.UserProfile
import com.example.ui.theme.KablammoFontFamily
import com.example.ui.theme.NewSchoolCyan
import com.example.ui.theme.NewSchoolPink
import com.example.ui.theme.PureBlack
import com.example.ui.theme.PureWhite
import java.io.ByteArrayOutputStream

data class AvatarPreset(
    val id: String,
    val label: String,
    val iconEmoji: String,
    val borderColorHex: String,
    val defaultName: String? = null,
    val defaultStatus: String? = null
)

val NEW_SCHOOL_AVATAR_PRESETS = listOf(
    AvatarPreset(
        id = "preset:kris_noir",
        label = "Kris 🕶️",
        iconEmoji = "🕶️",
        borderColorHex = "#FFFFFF",
        defaultName = "Kris",
        defaultStatus = "Kris • En ligne • kris&her"
    ),
    AvatarPreset(
        id = "preset:her_cyber",
        label = "Her ⚡",
        iconEmoji = "⚡",
        borderColorHex = "#FF1493",
        defaultName = "Her",
        defaultStatus = "Her • En ligne • kris&her"
    ),
    AvatarPreset(
        id = "preset:duo_monochrome",
        label = "Duo 💎",
        iconEmoji = "💎",
        borderColorHex = "#00FFFF",
        defaultName = "kris&her",
        defaultStatus = "Canal privé"
    ),
    AvatarPreset(
        id = "preset:neko_punk",
        label = "Neko 🐱",
        iconEmoji = "🐱",
        borderColorHex = "#FFFFFF",
        defaultName = "Neko",
        defaultStatus = "Vibes New-School 🐾"
    ),
    AvatarPreset(
        id = "preset:arcade_8bit",
        label = "Arcade 👾",
        iconEmoji = "👾",
        borderColorHex = "#00FF66",
        defaultName = "Gamer",
        defaultStatus = "Player 1 Ready 🕹️"
    )
)

/**
 * High-contrast New-School Avatar component that seamlessly renders:
 * - Direct Base64 data URIs from photo picker
 * - Remote HTTP / Cloud URLs
 * - Built-in Comic Presets (Kris, Her, Duo, etc.)
 * - Fallback Initials Avatar
 */
@Composable
fun UserProfileAvatar(
    photoUrl: String,
    displayName: String,
    size: Dp = 96.dp,
    borderColor: Color = PureWhite,
    showEditBadge: Boolean = false,
    onEditClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val matchingPreset = NEW_SCHOOL_AVATAR_PRESETS.find { it.id == photoUrl }

    val decodedBitmap = remember(photoUrl) {
        if (photoUrl.startsWith("data:") || (!photoUrl.startsWith("http") && !photoUrl.startsWith("content:") && photoUrl.length > 50)) {
            decodeBase64ToBitmap(photoUrl, context)
        } else if (photoUrl.startsWith("file:") || photoUrl.startsWith("/")) {
            runCatching { BitmapFactory.decodeFile(photoUrl.removePrefix("file://")) }.getOrNull()
        } else {
            null
        }
    }

    Box(
        modifier = modifier
            .size(size)
            .testTag("profile_picture_preview")
    ) {
        // Comic drop shadow layer
        Box(
            modifier = Modifier
                .offset(x = 3.dp, y = 3.dp)
                .matchParentSize()
                .clip(CircleShape)
                .background(PureWhite.copy(alpha = 0.25f))
        )

        // Main avatar container
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(CircleShape)
                .background(Color(0xFF1E293B))
                .border(2.5.dp, borderColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            when {
                // Decoded Base64 Bitmap (custom picked photo)
                decodedBitmap != null -> {
                    Image(
                        bitmap = decodedBitmap.asImageBitmap(),
                        contentDescription = "Photo de profil de $displayName",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                    )
                }

                // If it's a remote URL or Content / File URI
                photoUrl.isNotBlank() && (photoUrl.startsWith("http") || photoUrl.startsWith("content:") || photoUrl.startsWith("file:")) -> {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(photoUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Photo de profil de $displayName",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                    )
                }

                // If it's a comic preset avatar
                matchingPreset != null -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = matchingPreset.iconEmoji,
                            fontSize = (size.value * 0.42f).sp
                        )
                        Text(
                            text = matchingPreset.label.split(" ").first(),
                            fontFamily = KablammoFontFamily,
                            fontSize = (size.value * 0.16f).sp,
                            color = PureWhite
                        )
                    }
                }

                // Default initials avatar
                else -> {
                    val initials = displayName.trim().let { name ->
                        if (name.isEmpty()) "KH"
                        else name.split(" ")
                            .filter { it.isNotBlank() }
                            .take(2)
                            .map { it.first().uppercase() }
                            .joinToString("")
                            .ifEmpty { "KH" }
                    }
                    Text(
                        text = initials,
                        fontFamily = KablammoFontFamily,
                        color = PureWhite,
                        fontSize = (size.value * 0.38f).sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Edit overlay button badge
        if (showEditBadge && onEditClick != null) {
            Box(
                modifier = Modifier
                    .size(size * 0.34f)
                    .clip(CircleShape)
                    .background(PureWhite)
                    .border(1.5.dp, PureBlack, CircleShape)
                    .clickable { onEditClick() }
                    .align(Alignment.BottomEnd)
                    .testTag("change_photo_overlay"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = "Changer la photo",
                    tint = PureBlack,
                    modifier = Modifier.size(size * 0.20f)
                )
            }
        }
    }
}

/**
 * User Profile component enabling Kris and Her to set and customize their display name
 * and profile picture, with direct persistence to Cloud Firestore.
 */
@Composable
fun UserProfileComponent(
    currentProfile: UserProfile?,
    isSaving: Boolean,
    saveMessage: String?,
    onSaveProfile: (displayName: String, phoneNumber: String, photoUrl: String, statusText: String, avatarColorHex: String) -> Unit,
    onClearMessage: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    var displayName by remember { mutableStateOf(currentProfile?.displayName ?: "Kris") }
    var phoneNumber by remember { mutableStateOf(currentProfile?.phoneNumber ?: "") }
    var photoUrl by remember { mutableStateOf(currentProfile?.photoUrl ?: "") }
    var statusText by remember { mutableStateOf(currentProfile?.statusText ?: "Messagerie privée") }
    var avatarColorHex by remember { mutableStateOf(currentProfile?.avatarColorHex ?: "#FFFFFF") }

    // Synchronize local edit fields when remote profile loads or changes from Firestore
    LaunchedEffect(currentProfile) {
        currentProfile?.let {
            if (it.displayName.isNotBlank()) displayName = it.displayName
            phoneNumber = it.phoneNumber
            photoUrl = it.photoUrl
            if (it.statusText.isNotBlank()) statusText = it.statusText
            if (it.avatarColorHex.isNotBlank()) avatarColorHex = it.avatarColorHex
        }
    }

    // Android Photo Picker launcher (Zero broad permissions required!)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            // Compress and convert to compact Base64 JPEG data URI
            val base64Data = uriToCompressedBase64(context, uri, maxSize = 256, quality = 80)
            if (base64Data != null) {
                photoUrl = base64Data
            }
        }
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0D0D0D)),
        border = BorderStroke(1.5.dp, PureWhite),
        modifier = modifier
            .fillMaxWidth()
            .testTag("user_profile_component_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header title
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = PureWhite,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "PROFIL KRIS & HER",
                        fontFamily = KablammoFontFamily,
                        fontSize = 16.sp,
                        color = PureWhite
                    )
                }

                // Cloud Firestore Status Badge
                Surface(
                    color = Color(0xFF1F1F1F),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, PureWhite.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF00FF66))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "FIRESTORE",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = PureWhite
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Live Profile Picture Preview with Camera Overlay
            UserProfileAvatar(
                photoUrl = photoUrl,
                displayName = displayName,
                size = 100.dp,
                borderColor = PureWhite,
                showEditBadge = true,
                onEditClick = {
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Photo action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PureWhite,
                        contentColor = PureBlack
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("pick_photo_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoLibrary,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Choisir une photo",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (photoUrl.isNotBlank()) {
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedButton(
                        onClick = { photoUrl = "" },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF4D4D)),
                        border = BorderStroke(1.dp, Color(0xFFFF4D4D)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("clear_photo_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Supprimer", fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Quick Persona & Avatar Presets Strip
            Text(
                text = "Identités rapides & Avatars New-School :",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFFAAAAAA),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(NEW_SCHOOL_AVATAR_PRESETS) { preset ->
                    val isSelected = photoUrl == preset.id || (photoUrl.isBlank() && displayName == preset.defaultName)
                    Surface(
                        color = if (isSelected) PureWhite else Color(0xFF1A1A1A),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) PureWhite else Color(0xFF444444)
                        ),
                        modifier = Modifier
                            .clickable {
                                photoUrl = preset.id
                                preset.defaultName?.let { displayName = it }
                                preset.defaultStatus?.let { statusText = it }
                                avatarColorHex = preset.borderColorHex
                            }
                            .testTag("avatar_preset_${preset.id.replace(":", "_")}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = preset.iconEmoji, fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = preset.label,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) PureBlack else PureWhite
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Display Name Input Field
            OutlinedTextField(
                value = displayName,
                onValueChange = { if (it.length <= 50) displayName = it },
                label = { Text("Nom d'affichage (Kris / Her)", color = Color(0xFF888888)) },
                placeholder = { Text("Entrez votre nom", color = Color(0xFF555555)) },
                singleLine = true,
                trailingIcon = {
                    if (displayName.isNotEmpty()) {
                        IconButton(onClick = { displayName = "" }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Effacer",
                                tint = Color(0xFF888888)
                            )
                        }
                    }
                },
                supportingText = {
                    Text(
                        text = "${displayName.length}/50 caractères",
                        color = Color(0xFF777777),
                        fontSize = 11.sp,
                        textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("display_name_input"),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFF141414),
                    unfocusedContainerColor = Color(0xFF141414),
                    focusedTextColor = PureWhite,
                    unfocusedTextColor = PureWhite,
                    focusedBorderColor = PureWhite,
                    unfocusedBorderColor = Color(0xFF333333)
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Phone Number Input Field (To allow friends to add by phone number)
            OutlinedTextField(
                value = phoneNumber,
                onValueChange = { if (it.length <= 32) phoneNumber = it },
                label = { Text("Mon numéro de téléphone (pour m'ajouter)", color = Color(0xFF888888)) },
                placeholder = { Text("Ex: +33 6 12 34 56 78 ou 06 12 34 56 78", color = Color(0xFF555555)) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = null,
                        tint = if (phoneNumber.isNotBlank()) Color(0xFF00E5FF) else Color(0xFF888888)
                    )
                },
                trailingIcon = {
                    if (phoneNumber.isNotEmpty()) {
                        IconButton(onClick = { phoneNumber = "" }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Effacer",
                                tint = Color(0xFF888888)
                            )
                        }
                    }
                },
                supportingText = {
                    Text(
                        text = "Permet à l'autre personne de vous ajouter en tapant votre numéro",
                        color = Color(0xFF00E5FF),
                        fontSize = 11.sp,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("phone_number_input"),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFF141414),
                    unfocusedContainerColor = Color(0xFF141414),
                    focusedTextColor = PureWhite,
                    unfocusedTextColor = PureWhite,
                    focusedBorderColor = Color(0xFF00E5FF),
                    unfocusedBorderColor = Color(0xFF333333)
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Status / Bio Input Field
            OutlinedTextField(
                value = statusText,
                onValueChange = { if (it.length <= 200) statusText = it },
                label = { Text("Statut & Phrase de sécurité", color = Color(0xFF888888)) },
                placeholder = { Text("Ex: Messagerie privée", color = Color(0xFF555555)) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("status_text_input"),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFF141414),
                    unfocusedContainerColor = Color(0xFF141414),
                    focusedTextColor = PureWhite,
                    unfocusedTextColor = PureWhite,
                    focusedBorderColor = PureWhite,
                    unfocusedBorderColor = Color(0xFF333333)
                )
            )

            // Feedback Message Banner
            if (!saveMessage.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    color = if (saveMessage.startsWith("Erreur")) Color(0xFF330000) else Color(0xFF002A10),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(
                        1.dp,
                        if (saveMessage.startsWith("Erreur")) Color(0xFFFF3333) else Color(0xFF00FF66)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("profile_save_message_card")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (saveMessage.startsWith("Erreur")) Icons.Default.Clear else Icons.Default.Check,
                                contentDescription = null,
                                tint = if (saveMessage.startsWith("Erreur")) Color(0xFFFF6666) else Color(0xFF00FF66),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = saveMessage,
                                color = PureWhite,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        IconButton(
                            onClick = onClearMessage,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Fermer",
                                tint = PureWhite.copy(alpha = 0.7f),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Save to Firestore Button
            Button(
                onClick = {
                    onSaveProfile(displayName, phoneNumber, photoUrl, statusText, avatarColorHex)
                },
                enabled = !isSaving && displayName.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PureWhite,
                    contentColor = PureBlack,
                    disabledContainerColor = Color(0xFF2B2B2B),
                    disabledContentColor = Color(0xFF777777)
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("save_profile_button")
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = PureBlack,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Synchronisation Firestore...",
                        fontFamily = KablammoFontFamily,
                        fontSize = 13.sp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.CloudUpload,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "ENREGISTRER DANS FIRESTORE",
                        fontFamily = KablammoFontFamily,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

/**
 * Utility function to compress and encode a picked image Uri into a lightweight Base64 JPEG data URI.
 */
fun uriToCompressedBase64(context: Context, uri: Uri, maxSize: Int = 256, quality: Int = 85): String? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val originalBitmap = BitmapFactory.decodeStream(inputStream)
        inputStream.close()
        if (originalBitmap == null) return null

        val width = originalBitmap.width
        val height = originalBitmap.height
        val scale = if (width > height) {
            maxSize.toFloat() / width
        } else {
            maxSize.toFloat() / height
        }

        val scaledBitmap = if (scale < 1.0f) {
            Bitmap.createScaledBitmap(
                originalBitmap,
                (width * scale).toInt().coerceAtLeast(1),
                (height * scale).toInt().coerceAtLeast(1),
                true
            )
        } else {
            originalBitmap
        }

        // Handle transparency gracefully: draw onto clean neutral background so it never turns black
        val rgbBitmap = Bitmap.createBitmap(scaledBitmap.width, scaledBitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(rgbBitmap)
        canvas.drawColor(android.graphics.Color.WHITE)
        canvas.drawBitmap(scaledBitmap, 0f, 0f, null)

        // Save local cache file so it never fails even if base64 or memory has constraints
        try {
            val localFile = java.io.File(context.filesDir, "profile_avatar.jpg")
            java.io.FileOutputStream(localFile).use { fos ->
                rgbBitmap.compress(Bitmap.CompressFormat.JPEG, quality, fos)
            }
        } catch (_: Exception) {}

        val outputStream = ByteArrayOutputStream()
        rgbBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
        val byteArray = outputStream.toByteArray()
        val base64 = Base64.encodeToString(byteArray, Base64.NO_WRAP)
        "data:image/jpeg;base64,$base64"
    } catch (e: Exception) {
        Log.e("UserProfileComponent", "Error converting image to base64: ${e.message}")
        null
    }
}

/**
 * Decodes a data URI or Base64 string into an Android Bitmap with local file fallback.
 */
fun decodeBase64ToBitmap(dataUriOrBase64: String, context: Context? = null): Bitmap? {
    if (dataUriOrBase64.isBlank()) return null
    try {
        val cleanBase64 = if (dataUriOrBase64.contains(",")) {
            dataUriOrBase64.substringAfter(",")
        } else {
            dataUriOrBase64
        }.trim()
        val decodedBytes = Base64.decode(cleanBase64, Base64.DEFAULT)
        val bmp = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        if (bmp != null) return bmp
    } catch (e: Exception) {
        Log.w("UserProfileComponent", "Could not decode base64 bitmap: ${e.message}")
    }

    // Fallback: check if local file exists
    if (context != null) {
        try {
            val localFile = java.io.File(context.filesDir, "profile_avatar.jpg")
            if (localFile.exists()) {
                val fileBmp = BitmapFactory.decodeFile(localFile.absolutePath)
                if (fileBmp != null) return fileBmp
            }
        } catch (_: Exception) {}
    }
    return null
}
