package com.example.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.SentimentSatisfiedAlt
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.model.CustomStickerItem
import com.example.ui.theme.KablammoFontFamily
import com.example.ui.theme.NewSchoolCyan
import com.example.ui.theme.NewSchoolLime
import com.example.ui.theme.NewSchoolOrange
import com.example.ui.theme.NewSchoolPink
import com.example.ui.theme.NewSchoolPurple
import com.example.ui.theme.NewSchoolYellow
import com.example.ui.theme.PureBlack
import com.example.ui.theme.PureWhite
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

@Composable
fun CustomStickerStudioDialog(
    onDismiss: () -> Unit,
    onSaveSticker: (CustomStickerItem) -> Unit,
    defaultAuthor: String = "Kris"
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var selectedMediaUri by remember { mutableStateOf<Uri?>(null) }
    var extractedVideoBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isVideoSource by remember { mutableStateOf(false) }
    var videoDurationMs by remember { mutableLongStateOf(0L) }
    var selectedFrameOffsetSec by remember { mutableIntStateOf(1) }
    var isProcessingMedia by remember { mutableStateOf(false) }

    var stickerTitle by remember { mutableStateOf("STICKER") }
    var selectedEmojiBadge by remember { mutableStateOf("⚡") }
    var selectedBorderColor by remember { mutableStateOf(NewSchoolPink) }
    var authorName by remember { mutableStateOf(defaultAuthor) }

    val presetBadges = listOf("⚡", "❤️", "🔥", "💀", "👑", "💎", "✨", "🚀", "😂", "👀")
    val presetColors = listOf(
        NewSchoolPink to "Rose",
        NewSchoolOrange to "Orange",
        NewSchoolYellow to "Jaune",
        NewSchoolLime to "Vert",
        NewSchoolCyan to "Cyan",
        NewSchoolPurple to "Violet",
        PureWhite to "Blanc"
    )

    // Image Picker from Gallery
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                selectedMediaUri = uri
                isVideoSource = false
                extractedVideoBitmap = null
                if (stickerTitle == "STICKER") stickerTitle = "PHOTO"
            }
        }
    )

    // Video Picker from Gallery
    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                selectedMediaUri = uri
                isVideoSource = true
                isProcessingMedia = true
                coroutineScope.launch {
                    val (bitmap, duration) = extractFrameFromVideo(context, uri, 1000L)
                    extractedVideoBitmap = bitmap
                    videoDurationMs = duration
                    isProcessingMedia = false
                    if (stickerTitle == "STICKER") stickerTitle = "CLIP"
                }
            }
        }
    )

    // Camera Photo Capture Launcher
    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview(),
        onResult = { bitmap ->
            if (bitmap != null) {
                coroutineScope.launch {
                    val savedUri = saveBitmapToCache(context, bitmap)
                    selectedMediaUri = savedUri
                    isVideoSource = false
                    extractedVideoBitmap = bitmap
                    if (stickerTitle == "STICKER") stickerTitle = "CAM"
                }
            }
        }
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(PureBlack)
                .testTag("custom_sticker_studio"),
            color = PureBlack
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                // Top Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(PureWhite),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "🎨",
                                fontSize = 20.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "STUDIO AUTOCOLLANTS",
                                fontFamily = KablammoFontFamily,
                                fontSize = 18.sp,
                                color = PureWhite
                            )
                            Text(
                                text = "Créez vos stickers & émoticônes galerie",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFAAAAAA),
                                fontSize = 11.5.sp
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_studio_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Fermer le studio",
                            tint = PureWhite
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Preview Card Hero in New-School Style
                    Text(
                        text = "APERÇU DE L'AUTOCOLLANT",
                        fontFamily = KablammoFontFamily,
                        fontSize = 13.sp,
                        color = PureWhite.copy(alpha = 0.8f),
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF0F0F0F))
                            .border(1.5.dp, Color(0xFF2E2E2E), RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        // Background Comic Grid lines
                        if (isProcessingMedia) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = PureWhite, modifier = Modifier.size(36.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Extraction du média...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = PureWhite
                                )
                            }
                        } else {
                            // Sticker Badge Layout
                            CustomStickerPreviewWidget(
                                uri = selectedMediaUri,
                                bitmap = extractedVideoBitmap,
                                title = stickerTitle,
                                emojiBadge = selectedEmojiBadge,
                                borderColor = selectedBorderColor,
                                isVideo = isVideoSource
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Step 1: Select Media Source from Gallery or Camera
                    Text(
                        text = "1. CHOISIR DANS LA GALERIE OU CAMÉRA",
                        fontFamily = KablammoFontFamily,
                        fontSize = 13.sp,
                        color = PureWhite
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Gallery Image Button
                        MediaSourceButton(
                            title = "Photo Galerie",
                            subtitle = "JPG, PNG",
                            icon = Icons.Default.Image,
                            isSelected = selectedMediaUri != null && !isVideoSource,
                            onClick = {
                                imagePickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            modifier = Modifier.weight(1f),
                            testTag = "pick_image_button"
                        )

                        // Gallery Video Button
                        MediaSourceButton(
                            title = "Vidéo Galerie",
                            subtitle = "MP4, GIF",
                            icon = Icons.Default.Movie,
                            isSelected = selectedMediaUri != null && isVideoSource,
                            onClick = {
                                videoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                                )
                            },
                            modifier = Modifier.weight(1f),
                            testTag = "pick_video_button"
                        )

                        // Camera Button
                        MediaSourceButton(
                            title = "Caméra",
                            subtitle = "En direct",
                            icon = Icons.Default.PhotoCamera,
                            isSelected = false,
                            onClick = {
                                takePictureLauncher.launch(null)
                            },
                            modifier = Modifier.weight(1f),
                            testTag = "camera_capture_button"
                        )
                    }

                    // If video source, show Frame Selector slider / buttons
                    if (isVideoSource && selectedMediaUri != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            color = Color(0xFF141414),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF333333)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Videocam,
                                            contentDescription = null,
                                            tint = NewSchoolCyan,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Extrait vidéo (Frame)",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = PureWhite
                                        )
                                    }
                                    Text(
                                        text = "${selectedFrameOffsetSec}s",
                                        fontFamily = KablammoFontFamily,
                                        fontSize = 12.sp,
                                        color = NewSchoolCyan
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.horizontalScroll(rememberScrollState())
                                ) {
                                    val frameOptions = listOf(0, 1, 2, 3, 5)
                                    for (sec in frameOptions) {
                                        val isFrameSel = selectedFrameOffsetSec == sec
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (isFrameSel) PureWhite else Color(0xFF222222),
                                            border = androidx.compose.foundation.BorderStroke(
                                                1.dp,
                                                if (isFrameSel) PureWhite else Color(0xFF444444)
                                            ),
                                            modifier = Modifier.clickable {
                                                selectedFrameOffsetSec = sec
                                                val uri = selectedMediaUri ?: return@clickable
                                                coroutineScope.launch {
                                                    isProcessingMedia = true
                                                    val (bmp, _) = extractFrameFromVideo(
                                                        context,
                                                        uri,
                                                        sec * 1000L
                                                    )
                                                    extractedVideoBitmap = bmp
                                                    isProcessingMedia = false
                                                }
                                            }
                                        ) {
                                            Text(
                                                text = "${sec}s",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isFrameSel) PureBlack else PureWhite,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Step 2: Name / Tag
                    Text(
                        text = "2. TEXTE & TAG DU STICKER",
                        fontFamily = KablammoFontFamily,
                        fontSize = 13.sp,
                        color = PureWhite
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = stickerTitle,
                        onValueChange = { if (it.length <= 16) stickerTitle = it.uppercase() },
                        placeholder = { Text("EX: LOVE, WOW, KRIS...", color = Color(0xFF777777)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("sticker_title_input"),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF141414),
                            unfocusedContainerColor = Color(0xFF141414),
                            focusedTextColor = PureWhite,
                            unfocusedTextColor = PureWhite,
                            focusedBorderColor = PureWhite,
                            unfocusedBorderColor = Color(0xFF333333)
                        )
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Step 3: Emoji Badge
                    Text(
                        text = "3. ÉMOTICÔNE / BADGE DU STICKER",
                        fontFamily = KablammoFontFamily,
                        fontSize = 13.sp,
                        color = PureWhite
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for (emoji in presetBadges) {
                            val isSel = selectedEmojiBadge == emoji
                            Surface(
                                shape = CircleShape,
                                color = if (isSel) PureWhite else Color(0xFF1A1A1A),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.5.dp,
                                    if (isSel) PureWhite else Color(0xFF444444)
                                ),
                                modifier = Modifier
                                    .size(42.dp)
                                    .clickable { selectedEmojiBadge = emoji }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(text = emoji, fontSize = 20.sp)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Step 4: Border Accent Color
                    Text(
                        text = "4. COULEUR DE CONTOUR NEW-SCHOOL",
                        fontFamily = KablammoFontFamily,
                        fontSize = 13.sp,
                        color = PureWhite
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for ((color, _) in presetColors) {
                            val isSel = selectedBorderColor == color
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(
                                        if (isSel) 3.dp else 1.dp,
                                        if (isSel) PureWhite else Color(0xFF222222),
                                        CircleShape
                                    )
                                    .clickable { selectedBorderColor = color },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSel) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = if (color == PureWhite) PureBlack else PureWhite,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Step 5: Author Switch
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "CRÉATEUR",
                            fontFamily = KablammoFontFamily,
                            fontSize = 13.sp,
                            color = PureWhite
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("Kris", "Her").forEach { name ->
                                val isSel = authorName == name
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSel) PureWhite else Color(0xFF1E1E1E),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (isSel) PureWhite else Color(0xFF444444)
                                    ),
                                    modifier = Modifier.clickable { authorName = name }
                                ) {
                                    Text(
                                        text = name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = if (isSel) PureBlack else PureWhite,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Bottom Action Buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF222222),
                            contentColor = PureWhite
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Annuler", fontSize = 13.sp)
                    }

                    Box(modifier = Modifier.weight(1.5f)) {
                        Box(
                            modifier = Modifier
                                .offset(x = 2.dp, y = 2.dp)
                                .matchParentSize()
                                .clip(RoundedCornerShape(12.dp))
                                .background(selectedBorderColor.copy(alpha = 0.5f))
                        )
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    val finalImageUri = if (extractedVideoBitmap != null) {
                                        saveBitmapToCache(context, extractedVideoBitmap!!).toString()
                                    } else if (selectedMediaUri != null) {
                                        selectedMediaUri.toString()
                                    } else {
                                        // Default fallback comic artwork
                                        "preset://$selectedEmojiBadge"
                                    }

                                    val hexColor = "#" + Integer.toHexString(selectedBorderColor.toArgb()).uppercase().takeLast(6)

                                    val newSticker = CustomStickerItem(
                                        id = "stk_${System.currentTimeMillis()}",
                                        title = stickerTitle.ifBlank { "STICKER" },
                                        author = authorName,
                                        imageUri = finalImageUri,
                                        emojiBadge = selectedEmojiBadge,
                                        borderColorHex = hexColor,
                                        isVideo = isVideoSource,
                                        frameTimestampMs = selectedFrameOffsetSec * 1000L,
                                        createdAt = System.currentTimeMillis()
                                    )

                                    onSaveSticker(newSticker)
                                    onDismiss()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PureWhite,
                                contentColor = PureBlack
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("save_custom_sticker_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = PureBlack,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "ENREGISTRER",
                                fontFamily = KablammoFontFamily,
                                fontSize = 13.sp,
                                color = PureBlack
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CustomStickerPreviewWidget(
    uri: Uri?,
    bitmap: Bitmap?,
    title: String,
    emojiBadge: String,
    borderColor: Color,
    isVideo: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(140.dp)
            .testTag("sticker_preview_badge"),
        contentAlignment = Alignment.Center
    ) {
        // Comic Hard Drop Shadow
        Box(
            modifier = Modifier
                .offset(x = 4.dp, y = 4.dp)
                .matchParentSize()
                .clip(RoundedCornerShape(18.dp))
                .background(borderColor.copy(alpha = 0.4f))
        )

        // Main Sticker Card
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = Color(0xFF141414),
            border = androidx.compose.foundation.BorderStroke(2.5.dp, borderColor),
            modifier = Modifier.matchParentSize()
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Sticker",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else if (uri != null) {
                    AsyncImage(
                        model = uri,
                        contentDescription = "Sticker",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    // Placeholder Comic Canvas
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF1F1F1F)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = emojiBadge, fontSize = 38.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "kris & her",
                                fontFamily = KablammoFontFamily,
                                fontSize = 10.sp,
                                color = PureWhite.copy(alpha = 0.7f)
                            )
                        }
                    }
                }

                // Top Badge: Emoji Sticker Tag
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(PureBlack)
                        .border(1.5.dp, borderColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = emojiBadge, fontSize = 14.sp)
                }

                // If Video based: Movie Indicator Icon
                if (isVideo) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(6.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(PureBlack.copy(alpha = 0.8f))
                            .border(1.dp, PureWhite.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Movie,
                                contentDescription = "Vidéo",
                                tint = PureWhite,
                                modifier = Modifier.size(10.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "CLIP",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = PureWhite
                            )
                        }
                    }
                }

                // Bottom Banner: Title Text
                if (title.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .background(PureBlack.copy(alpha = 0.85f))
                            .border(
                                androidx.compose.foundation.BorderStroke(1.dp, borderColor.copy(alpha = 0.5f))
                            )
                            .padding(vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = title,
                            fontFamily = KablammoFontFamily,
                            fontSize = 11.sp,
                            color = PureWhite,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MediaSourceButton(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String = ""
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) PureWhite.copy(alpha = 0.15f) else Color(0xFF141414),
        border = androidx.compose.foundation.BorderStroke(
            1.5.dp,
            if (isSelected) PureWhite else Color(0xFF333333)
        ),
        modifier = modifier
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) PureWhite else Color(0xFFAAAAAA),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 11.5.sp,
                color = PureWhite,
                textAlign = TextAlign.Center
            )
            Text(
                text = subtitle,
                fontSize = 9.5.sp,
                color = Color(0xFF888888),
                textAlign = TextAlign.Center
            )
        }
    }
}

suspend fun extractFrameFromVideo(
    context: Context,
    videoUri: Uri,
    timeUs: Long
): Pair<Bitmap?, Long> = withContext(Dispatchers.IO) {
    var retriever: MediaMetadataRetriever? = null
    try {
        retriever = MediaMetadataRetriever()
        retriever.setDataSource(context, videoUri)
        val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
        val durationMs = durationStr?.toLongOrNull() ?: 3000L

        val frameTimeUs = (timeUs * 1000).coerceAtMost(durationMs * 1000)
        val frame = retriever.getFrameAtTime(frameTimeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
            ?: retriever.frameAtTime

        // Scale down to sticker resolution for fast, memory-safe rendering
        val scaled = frame?.let {
            val maxDim = 380
            val ratio = it.width.toFloat() / it.height.toFloat()
            val (w, h) = if (ratio > 1) {
                maxDim to (maxDim / ratio).toInt()
            } else {
                (maxDim * ratio).toInt() to maxDim
            }
            Bitmap.createScaledBitmap(it, w.coerceAtLeast(100), h.coerceAtLeast(100), true)
        }
        Pair(scaled, durationMs)
    } catch (e: Exception) {
        Pair(null, 0L)
    } finally {
        try {
            retriever?.release()
        } catch (_: Exception) {}
    }
}

suspend fun saveBitmapToCache(context: Context, bitmap: Bitmap): Uri = withContext(Dispatchers.IO) {
    val file = File(context.cacheDir, "sticker_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(4)}.jpg")
    FileOutputStream(file).use { out ->
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
    }
    Uri.fromFile(file)
}
