package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.SentimentSatisfiedAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.example.model.CustomStickerItem
import com.example.ui.theme.KablammoFontFamily
import com.example.ui.theme.NewSchoolCyan
import com.example.ui.theme.NewSchoolLime
import com.example.ui.theme.NewSchoolOrange
import com.example.ui.theme.NewSchoolPink
import com.example.ui.theme.PureBlack
import com.example.ui.theme.PureWhite

/**
 * MessageInput Composable:
 * Chat input bar styled with a high-contrast Black-and-White New-School aesthetic.
 * - Bold comic-book drop shadow layers for the input card and action buttons.
 * - Monochromatic styling with sharp Pure White / Deep Black contrasts.
 * - Interactive send button with instant visual feedback and audio record toggle.
 * - Quick emoji / sticker drawer in New-School style.
 */
@Composable
fun MessageInput(
    value: String,
    onValueChange: (String) -> Unit,
    onSendMessage: (String) -> Unit,
    modifier: Modifier = Modifier,
    onAttachClick: (() -> Unit)? = null,
    onMicClick: (() -> Unit)? = null,
    onEmojiInsert: ((String) -> Unit)? = null,
    placeholderText: String = "Message kris&her...",
    customStickers: List<CustomStickerItem> = emptyList(),
    onSendCustomSticker: ((CustomStickerItem) -> Unit)? = null,
    onOpenCustomStickerStudio: (() -> Unit)? = null
) {
    var showQuickStickers by remember { mutableStateOf(false) }

    Surface(
        color = PureBlack,
        border = androidx.compose.foundation.BorderStroke(1.5.dp, PureWhite.copy(alpha = 0.2f)),
        modifier = modifier
            .fillMaxWidth()
            .imePadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            // Quick New-School Sticker & Emoticon Drawer
            AnimatedVisibility(visible = showQuickStickers) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                ) {
                    // Custom Gallery Stickers Row (User created)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Button to open Custom Sticker Studio
                        if (onOpenCustomStickerStudio != null) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF1E1E1E),
                                border = androidx.compose.foundation.BorderStroke(1.5.dp, NewSchoolPink),
                                modifier = Modifier
                                    .clickable { onOpenCustomStickerStudio() }
                                    .testTag("open_sticker_studio_btn")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Créer",
                                        tint = NewSchoolPink,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "+ CRÉER",
                                        fontFamily = KablammoFontFamily,
                                        fontSize = 11.sp,
                                        color = PureWhite
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                        }

                        // Horizontal list of custom stickers
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            for (sticker in customStickers) {
                                val borderCol = try {
                                    Color(android.graphics.Color.parseColor(sticker.borderColorHex))
                                } catch (_: Exception) {
                                    NewSchoolPink
                                }

                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFF161616),
                                    border = androidx.compose.foundation.BorderStroke(1.5.dp, borderCol),
                                    modifier = Modifier
                                        .clickable {
                                            onSendCustomSticker?.invoke(sticker)
                                        }
                                        .testTag("sticker_chip_${sticker.id}")
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                                    ) {
                                        if (sticker.imageUri.startsWith("preset://")) {
                                            Text(
                                                text = sticker.emojiBadge,
                                                fontSize = 16.sp
                                            )
                                        } else {
                                            AsyncImage(
                                                model = sticker.imageUri,
                                                contentDescription = sticker.title,
                                                modifier = Modifier
                                                    .size(22.dp)
                                                    .clip(RoundedCornerShape(4.dp)),
                                                contentScale = ContentScale.Crop
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Text(
                                            text = sticker.title,
                                            fontFamily = KablammoFontFamily,
                                            fontSize = 10.5.sp,
                                            color = PureWhite
                                        )
                                        if (sticker.isVideo) {
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text(text = "🎬", fontSize = 10.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Built-in Quick Emojis Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val quickEmojis = listOf(
                            "⚡" to "ZAP",
                            "❤️" to "LOVE",
                            "🔒" to "SAFE",
                            "💀" to "DEAD",
                            "👑" to "KING",
                            "🔥" to "FIRE",
                            "💎" to "DIAMOND",
                            "🚀" to "GO",
                            "✨" to "SPARK"
                        )

                        for ((emoji, label) in quickEmojis) {
                            NewSchoolEmojiBadge(
                                emoji = emoji,
                                label = label,
                                borderColor = PureWhite,
                                isDarkBg = true,
                                onClick = {
                                    if (onEmojiInsert != null) {
                                        onEmojiInsert(" $emoji")
                                    } else {
                                        onValueChange("$value $emoji")
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // Main Input Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Sticker / Emoji Toggle Button with Comic Shadow
                Box(modifier = Modifier.size(42.dp)) {
                    Box(
                        modifier = Modifier
                            .offset(x = 1.5.dp, y = 1.5.dp)
                            .matchParentSize()
                            .clip(CircleShape)
                            .background(PureWhite.copy(alpha = 0.15f))
                    )
                    IconButton(
                        onClick = { showQuickStickers = !showQuickStickers },
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(Color(0xFF141414))
                            .border(1.5.dp, if (showQuickStickers) PureWhite else Color(0xFF333333), CircleShape)
                            .testTag("emoji_toggle_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.SentimentSatisfiedAlt,
                            contentDescription = "Autocollants & Emojis",
                            tint = if (showQuickStickers) PureWhite else Color(0xFFAAAAAA),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Attachment Button
                if (onAttachClick != null) {
                    Box(modifier = Modifier.size(42.dp)) {
                        Box(
                            modifier = Modifier
                                .offset(x = 1.5.dp, y = 1.5.dp)
                                .matchParentSize()
                            .clip(CircleShape)
                            .background(PureWhite.copy(alpha = 0.15f))
                        )
                        IconButton(
                            onClick = onAttachClick,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(Color(0xFF141414))
                                .border(1.5.dp, Color(0xFF333333), CircleShape)
                                .testTag("attach_file_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.AttachFile,
                                contentDescription = "Pièce jointe",
                                tint = PureWhite,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                }

                // Text Field with New-School Offset Layer
                Box(
                    modifier = Modifier
                        .weight(1f)
                ) {
                    // Comic drop shadow layer
                    Box(
                        modifier = Modifier
                            .offset(x = 2.dp, y = 2.dp)
                            .matchParentSize()
                            .clip(RoundedCornerShape(20.dp))
                            .background(PureWhite.copy(alpha = 0.15f))
                    )

                    OutlinedTextField(
                        value = value,
                        onValueChange = onValueChange,
                        placeholder = {
                            Text(
                                text = placeholderText,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF777777),
                                fontSize = 13.5.sp
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("chat_input_field"),
                        maxLines = 4,
                        shape = RoundedCornerShape(20.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF0F0F0F),
                            unfocusedContainerColor = Color(0xFF0F0F0F),
                            focusedTextColor = PureWhite,
                            unfocusedTextColor = PureWhite,
                            focusedBorderColor = PureWhite,
                            unfocusedBorderColor = Color(0xFF3A3A3A)
                        )
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Action Button: Send Button (if text present) OR Mic Button (if blank)
                val hasText = value.isNotBlank()

                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .testTag(if (hasText) "send_message_button" else "record_audio_button")
                ) {
                    // Comic hard drop shadow
                    Box(
                        modifier = Modifier
                            .offset(x = 2.5.dp, y = 2.5.dp)
                            .matchParentSize()
                            .clip(CircleShape)
                            .background(if (hasText) PureWhite.copy(alpha = 0.35f) else PureWhite.copy(alpha = 0.15f))
                    )

                    if (hasText) {
                        // Bold Pure White button with Pure Black arrow
                        IconButton(
                            onClick = {
                                val textToSend = value.trim()
                                if (textToSend.isNotEmpty()) {
                                    onSendMessage(textToSend)
                                }
                            },
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(PureWhite)
                                .border(2.dp, PureWhite, CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Envoyer le message",
                                tint = PureBlack,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    } else {
                        // Voice Note Mic button in Deep Black with Pure White border
                        IconButton(
                            onClick = { onMicClick?.invoke() },
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(Color(0xFF161616))
                                .border(2.dp, PureWhite, CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "Enregistrer note vocale",
                                tint = PureWhite,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
