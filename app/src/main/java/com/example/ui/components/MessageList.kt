package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import kotlinx.coroutines.launch
import androidx.compose.runtime.remember
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.example.audio.AudioPlaybackState
import com.example.model.MessageEntity
import com.example.model.MessageStatus
import com.example.model.MessageType
import com.example.ui.theme.KablammoFontFamily
import com.example.ui.theme.NewSchoolCyan
import com.example.ui.theme.NewSchoolLime
import com.example.ui.theme.NewSchoolPink
import com.example.ui.theme.PureBlack
import com.example.ui.theme.PureWhite
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * MessageList Composable:
 * Displays a list of chat messages with a distinctive New-School monochrome aesthetic.
 * - User messages (Moi) are styled with Pure White high-contrast comic bubbles and Pure Black text.
 * - Partner messages are styled with Deep Black comic bubbles, Pure White text, and crisp white borders.
 * - Includes drop-shadow comic layers, sticker reaction badges, audio player waveforms, and date headers.
 */
@Composable
fun MessageList(
    messages: List<MessageEntity>,
    partnerName: String,
    partnerInitials: String = "",
    partnerAvatarColorHex: String = "#FF643D",
    isTyping: Boolean = false,
    bubbleStyle: BubbleStyle = BubbleStyle.COMIC,
    playbackState: AudioPlaybackState = AudioPlaybackState(),
    listState: LazyListState = rememberLazyListState(),
    onPlayAudio: (messageId: Long, filePath: String?, durationSec: Int) -> Unit = { _, _, _ -> },
    onMessageClick: (MessageEntity) -> Unit = {},
    onMessageLongClick: (MessageEntity) -> Unit = {},
    onReactionClick: (MessageEntity, String) -> Unit = { _, _ -> },
    onInfoClick: ((MessageEntity) -> Unit)? = null,
    highlightMessageId: Long? = null,
    searchQuery: String = "",
    modifier: Modifier = Modifier
) {
    // Auto-scroll to latest message
    LaunchedEffect(messages.size, isTyping) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (messages.isEmpty()) {
            // New-School Empty State
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    NewSchoolEmojiBadge(
                        emoji = "💬",
                        label = "START",
                        borderColor = NewSchoolLime,
                        isDarkBg = true
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "DÉMARREZ LA DISCUSSION",
                        fontFamily = KablammoFontFamily,
                        fontSize = 20.sp,
                        color = PureWhite
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Messages transmis via une connexion sécurisée (Firebase).",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFAAAAAA),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                }

                itemsIndexed(messages, key = { _, msg -> msg.id }) { index, msg ->
                    // Show Date header if first message or date changed
                    val showDateHeader = shouldDisplayDateHeader(index, messages)
                    if (showDateHeader) {
                        DateSeparatorPill(timestamp = msg.timestamp)
                    }

                    if (msg.type == MessageType.SYSTEM) {
                        SystemMessagePill(content = msg.content)
                    } else {
                        val isMe = msg.senderId == "me"

                        // Grouping / clustering: only the last message of a consecutive sender sequence has the directional tail
                        val isNextFromSameSender = index < messages.size - 1 &&
                            messages[index + 1].senderId == msg.senderId &&
                            messages[index + 1].type != MessageType.SYSTEM
                        val hasTail = !isNextFromSameSender

                        val isHighlighted = msg.id == highlightMessageId ||
                            (searchQuery.isNotBlank() && msg.content.contains(searchQuery, ignoreCase = true))

                        AnimatedMessageEntrance(
                            isMe = isMe,
                            modifier = Modifier.animateItem()
                        ) {
                            NewSchoolMessageRow(
                                message = msg,
                                isMe = isMe,
                                hasTail = hasTail,
                                bubbleStyle = bubbleStyle,
                                partnerName = partnerName,
                                partnerInitials = partnerInitials,
                                partnerAvatarColorHex = partnerAvatarColorHex,
                                playbackState = playbackState,
                                onPlayAudio = { onPlayAudio(msg.id, msg.mediaUri, msg.audioDurationSec) },
                                onClick = { onMessageClick(msg) },
                                onLongClick = { onMessageLongClick(msg) },
                                onReactionClick = { emoji -> onReactionClick(msg, emoji) },
                                onInfoClick = if (onInfoClick != null) { { onInfoClick(msg) } } else null,
                                isHighlighted = isHighlighted
                            )
                        }
                    }
                }

                if (isTyping) {
                    item {
                        TypingIndicator(friendName = partnerName)
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NewSchoolMessageRow(
    message: MessageEntity,
    isMe: Boolean,
    hasTail: Boolean,
    bubbleStyle: BubbleStyle,
    partnerName: String,
    partnerInitials: String,
    partnerAvatarColorHex: String,
    playbackState: AudioPlaybackState,
    onPlayAudio: () -> Unit,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onReactionClick: (String) -> Unit,
    onInfoClick: (() -> Unit)?,
    isHighlighted: Boolean = false,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = if (hasTail) 4.dp else 2.dp),
        horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
    ) {
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Partner Avatar if partner message
            if (!isMe) {
                if (hasTail) {
                    EnteAvatar(
                        initials = partnerInitials.ifBlank { partnerName.take(2).uppercase() },
                        colorHex = partnerAvatarColorHex,
                        size = 34.dp,
                        borderColor = PureWhite,
                        modifier = Modifier.padding(end = 6.dp, bottom = 2.dp)
                    )
                } else {
                    // Spacer aligned with avatar width
                    Spacer(modifier = Modifier.width(40.dp))
                }
            }

            // Message Bubble with custom Speech Bubble Shape and tail
            NewSchoolMessageBubble(
                message = message,
                isMe = isMe,
                hasTail = hasTail,
                bubbleStyle = bubbleStyle,
                partnerName = partnerName,
                playbackState = playbackState,
                onPlayAudio = onPlayAudio,
                onClick = onClick,
                onLongClick = onLongClick,
                onReactionClick = onReactionClick,
                onInfoClick = onInfoClick,
                modifier = if (isHighlighted) {
                    Modifier
                        .border(2.5.dp, NewSchoolCyan, RoundedCornerShape(16.dp))
                        .background(NewSchoolCyan.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                        .padding(2.dp)
                } else Modifier
            )
        }

        // Reaction Sticker Badge below bubble
        if (message.reactionEmoji.isNotBlank()) {
            Box(
                modifier = Modifier.padding(
                    start = if (isMe) 0.dp else 42.dp,
                    end = if (isMe) 14.dp else 0.dp,
                    top = 3.dp
                )
            ) {
                NewSchoolEmojiBadge(
                    emoji = message.reactionEmoji,
                    borderColor = getNewSchoolBorderColor(message.id.toInt()),
                    isDarkBg = true,
                    onClick = { onReactionClick(message.reactionEmoji) }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NewSchoolMessageBubble(
    message: MessageEntity,
    isMe: Boolean,
    hasTail: Boolean,
    bubbleStyle: BubbleStyle,
    partnerName: String,
    playbackState: AudioPlaybackState,
    onPlayAudio: () -> Unit,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onReactionClick: (String) -> Unit,
    onInfoClick: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val bubbleText = when (bubbleStyle) {
        BubbleStyle.NEON -> if (isMe) PureWhite else PureWhite
        BubbleStyle.MODERN -> if (isMe) PureBlack else PureWhite
        BubbleStyle.COMIC -> if (isMe) PureBlack else PureWhite
    }

    ChatBubbleContainer(
        isMe = isMe,
        hasTail = hasTail,
        style = bubbleStyle,
        onClick = onClick,
        onLongClick = onLongClick,
        onDoubleTapHeart = { onReactionClick("❤️") },
        modifier = modifier.testTag("message_item_${message.id}")
    ) {
        // Partner header tag if not me and first/independent message
        if (!isMe && hasTail) {
            Text(
                text = partnerName,
                fontFamily = KablammoFontFamily,
                fontSize = 11.5.sp,
                color = if (bubbleStyle == BubbleStyle.NEON) NewSchoolPink else PureWhite.copy(alpha = 0.85f),
                modifier = Modifier.padding(bottom = 3.dp)
            )
        }

        when (message.type) {
            MessageType.AUDIO -> {
                val isPlayingThis = playbackState.playingMessageId == message.id && playbackState.isPlaying
                val currentPos = if (playbackState.playingMessageId == message.id) playbackState.currentPositionMs else 0
                val totalDur = if (playbackState.playingMessageId == message.id && playbackState.totalDurationMs > 0) {
                    playbackState.totalDurationMs
                } else {
                    message.audioDurationSec.coerceAtLeast(2) * 1000
                }

                NewSchoolAudioPlayer(
                    durationSec = message.audioDurationSec.coerceAtLeast(2),
                    isPlaying = isPlayingThis,
                    currentPositionMs = currentPos,
                    totalDurationMs = totalDur,
                    isMe = isMe,
                    onTogglePlay = onPlayAudio
                )
            }
            MessageType.IMAGE -> {
                Column {
                    if (!message.mediaUri.isNullOrBlank()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 140.dp, max = 260.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isMe) Color(0xFFF0F0F0) else Color(0xFF1E1E1E))
                                .border(1.5.dp, if (isMe) Color(0xFFCCCCCC) else Color(0xFF444444), RoundedCornerShape(12.dp))
                        ) {
                            AsyncImage(
                                model = message.mediaUri,
                                contentDescription = "Photo",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 140.dp, max = 260.dp)
                                    .clip(RoundedCornerShape(12.dp)),
                                contentScale = ContentScale.Crop
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isMe) Color(0xFFF0F0F0) else Color(0xFF222222))
                                .border(1.dp, if (isMe) Color(0xFFDDDDDD) else Color(0xFF444444), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Verrou",
                                    tint = bubbleText,
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Photo",
                                    fontFamily = KablammoFontFamily,
                                    fontSize = 11.sp,
                                    color = bubbleText
                                )
                            }
                        }
                    }
                    if (message.content.isNotBlank() && !message.content.startsWith("📸")) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = message.content,
                            style = MaterialTheme.typography.bodyMedium,
                            color = bubbleText
                        )
                    }
                }
            }
            MessageType.DOCUMENT -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isMe) Color(0xFFEEEEEE) else Color(0xFF222222))
                        .padding(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = bubbleText,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = message.content.ifBlank { "document.pdf" },
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = bubbleText
                        )
                        Text(
                            text = "Document kris&her",
                            fontSize = 10.sp,
                            color = if (isMe) PureBlack.copy(alpha = 0.6f) else Color(0xFFAAAAAA)
                        )
                    }
                }
            }
            MessageType.STICKER -> {
                val isVideoSticker = message.content.contains("🎬") || message.content.contains("[CLIP]")
                val stickerTitle = message.content.replace("🎬", "").replace("[CLIP]", "").trim()
                Column(
                    modifier = Modifier.padding(vertical = 2.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(150.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isMe) Color(0xFFF2F2F2) else Color(0xFF1E1E1E))
                            .border(
                                2.dp,
                                if (isMe) PureBlack else PureWhite,
                                RoundedCornerShape(16.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (message.mediaUri != null && !message.mediaUri.startsWith("preset://")) {
                            AsyncImage(
                                model = message.mediaUri,
                                contentDescription = "Autocollant galerie",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            val emojiChar = message.mediaUri?.removePrefix("preset://") ?: "⚡"
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(text = emojiChar, fontSize = 48.sp)
                            }
                        }

                        if (isVideoSticker) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(6.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(PureBlack.copy(alpha = 0.85f))
                                    .border(1.dp, PureWhite.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "🎬 VIDÉO",
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PureWhite
                                )
                            }
                        }

                        if (stickerTitle.isNotBlank()) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .fillMaxWidth()
                                    .background(PureBlack.copy(alpha = 0.85f))
                                    .padding(vertical = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = stickerTitle,
                                    fontFamily = KablammoFontFamily,
                                    fontSize = 11.5.sp,
                                    color = PureWhite,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
            else -> {
                if (message.content.startsWith("👤")) {
                    val clean = message.content.removePrefix("👤").trim()
                    val parts = clean.split("•")
                    val contactName = parts.firstOrNull()?.replace("Fiche Contact :", "")?.trim() ?: "Contact"
                    val contactDetail = if (parts.size > 1) parts[1].trim() else ""

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isMe) Color(0xFFF3F3F3) else Color(0xFF1E1E1E),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isMe) Color(0xFFCCCCCC) else Color(0xFF3A3A3A)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(if (isMe) PureBlack else PureWhite),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = contactName.take(2).uppercase().ifEmpty { "C" },
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = if (isMe) PureWhite else PureBlack
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = contactName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.5.sp,
                                    color = bubbleText
                                )
                                if (contactDetail.isNotBlank()) {
                                    Text(
                                        text = contactDetail,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontSize = 11.sp,
                                        color = if (isMe) PureBlack.copy(alpha = 0.7f) else Color(0xFFAAAAAA)
                                    )
                                }
                                Text(
                                    text = "Fiche contact",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isMe) PureBlack.copy(alpha = 0.5f) else Color(0xFF888888)
                                )
                            }
                        }
                    }
                } else {
                    // Check if message contains a reply quote [REPLY:author|snippet]
                    val (quoteAuthor, quoteSnippet, cleanText) = parseQuotedMessage(message.content)
                    if (quoteAuthor != null && quoteSnippet != null) {
                        QuotedMessageBubble(
                            authorName = quoteAuthor,
                            contentPreview = quoteSnippet,
                            isMe = isMe
                        )
                    }

                    Text(
                        text = cleanText,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 14.5.sp,
                            lineHeight = 19.sp,
                            fontWeight = FontWeight.Normal
                        ),
                        color = bubbleText
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Footer: Timestamp + Status Indicators
        Row(
            modifier = Modifier.align(Alignment.End),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(message.timestamp)),
                style = MaterialTheme.typography.labelSmall,
                fontSize = 10.sp,
                color = if (isMe) {
                    if (bubbleStyle == BubbleStyle.NEON) PureWhite.copy(alpha = 0.7f) else PureBlack.copy(alpha = 0.65f)
                } else {
                    Color(0xFFAAAAAA)
                }
            )

            if (isMe) {
                Spacer(modifier = Modifier.width(4.dp))
                val tickColor = when (message.status) {
                    MessageStatus.READ -> if (bubbleStyle == BubbleStyle.NEON) NewSchoolCyan else Color(0xFF00C853)
                    else -> if (bubbleStyle == BubbleStyle.NEON) PureWhite.copy(alpha = 0.65f) else PureBlack.copy(alpha = 0.6f)
                }

                NewSchoolMessageStatusTick(
                    status = message.status,
                    color = tickColor
                )

                if (message.status == MessageStatus.READ) {
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = if (message.readTimestamp != null) {
                            "Lu ${SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(message.readTimestamp))}"
                        } else {
                            "Lu"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = tickColor,
                        modifier = Modifier
                            .testTag("read_receipt_indicator_${message.id}")
                            .then(
                                if (onInfoClick != null) Modifier.clickable(onClick = onInfoClick) else Modifier
                            )
                    )
                }
            }
        }
    }
}

private fun parseQuotedMessage(rawContent: String): Triple<String?, String?, String> {
    if (rawContent.startsWith("[REPLY:") && rawContent.contains("]\n")) {
        val closingIndex = rawContent.indexOf("]\n")
        val header = rawContent.substring(7, closingIndex)
        val body = rawContent.substring(closingIndex + 2)
        val parts = header.split("|", limit = 2)
        val author = parts.getOrNull(0) ?: "Auteur"
        val snippet = parts.getOrNull(1) ?: ""
        return Triple(author, snippet, body)
    }
    return Triple(null, null, rawContent)
}

@Composable
fun NewSchoolAudioPlayer(
    durationSec: Int,
    isPlaying: Boolean,
    currentPositionMs: Int,
    totalDurationMs: Int,
    isMe: Boolean,
    onTogglePlay: () -> Unit,
    modifier: Modifier = Modifier
) {
    val buttonBg = if (isMe) PureBlack else PureWhite
    val buttonIconTint = if (isMe) PureWhite else PureBlack
    val waveformColor = if (isMe) PureBlack else PureWhite
    val textColor = if (isMe) PureBlack else PureWhite

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
                .size(38.dp)
                .clip(CircleShape)
                .background(buttonBg)
                .testTag("play_audio_btn")
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isPlaying) "Pause note vocale" else "Lire note vocale",
                tint = buttonIconTint,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Dynamic Waveform Canvas
        Box(
            modifier = Modifier
                .weight(1f)
                .height(32.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val barCount = 20
                val barWidth = 3.dp.toPx()
                val gap = (size.width - (barCount * barWidth)) / (barCount - 1).coerceAtLeast(1)

                val heights = listOf(
                    0.25f, 0.5f, 0.75f, 0.4f, 0.9f, 0.6f, 0.3f, 0.8f, 0.95f, 0.5f,
                    0.35f, 0.7f, 0.85f, 0.45f, 0.65f, 0.9f, 0.3f, 0.55f, 0.8f, 0.4f
                )

                for (i in 0 until barCount) {
                    val x = i * (barWidth + gap)
                    val hFraction = heights[i % heights.size]
                    val barHeight = size.height * hFraction
                    val y = (size.height - barHeight) / 2f
                    val isPastProgress = (i.toFloat() / barCount.toFloat()) <= progress

                    val barAlpha = if (isPastProgress) 1f else 0.35f
                    drawRoundRect(
                        color = waveformColor.copy(alpha = barAlpha),
                        topLeft = Offset(x, y),
                        size = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Duration text
        val currentSec = (currentPositionMs / 1000).coerceAtLeast(0)
        val displayDuration = if (isPlaying && currentPositionMs > 0) currentSec else durationSec
        val m = displayDuration / 60
        val s = displayDuration % 60
        Text(
            text = "%d:%02d".format(m, s),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = textColor,
            fontSize = 11.sp
        )
    }
}

@Composable
fun NewSchoolMessageStatusTick(
    status: MessageStatus,
    color: Color = PureBlack,
    modifier: Modifier = Modifier
) {
    when (status) {
        MessageStatus.SENDING -> {
            Icon(
                imageVector = Icons.Default.Schedule,
                contentDescription = "Envoi...",
                tint = color.copy(alpha = 0.5f),
                modifier = modifier.size(13.dp)
            )
        }
        MessageStatus.SENT -> {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Envoyé",
                tint = color.copy(alpha = 0.7f),
                modifier = modifier.size(13.dp)
            )
        }
        MessageStatus.DELIVERED -> {
            Icon(
                imageVector = Icons.Default.DoneAll,
                contentDescription = "Distribué",
                tint = color.copy(alpha = 0.7f),
                modifier = modifier.size(14.dp)
            )
        }
        MessageStatus.READ -> {
            Icon(
                imageVector = Icons.Default.DoneAll,
                contentDescription = "Lu / Vu",
                tint = color,
                modifier = modifier.size(14.dp)
            )
        }
    }
}

@Composable
private fun DateSeparatorPill(timestamp: Long) {
    val dateText = formatMessageDate(timestamp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF161616),
            border = androidx.compose.foundation.BorderStroke(1.dp, PureWhite.copy(alpha = 0.3f))
        ) {
            Text(
                text = dateText,
                fontFamily = KablammoFontFamily,
                fontSize = 11.sp,
                color = PureWhite,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun SystemMessagePill(content: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = Color(0xFF111111),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF333333))
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = PureWhite,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = content,
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 11.sp,
                    color = Color(0xFFAAAAAA),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

private fun shouldDisplayDateHeader(index: Int, messages: List<MessageEntity>): Boolean {
    if (index == 0) return true
    val prevMsg = messages[index - 1]
    val currentMsg = messages[index]

    val cal1 = Calendar.getInstance().apply { timeInMillis = prevMsg.timestamp }
    val cal2 = Calendar.getInstance().apply { timeInMillis = currentMsg.timestamp }

    return cal1.get(Calendar.YEAR) != cal2.get(Calendar.YEAR) ||
           cal1.get(Calendar.DAY_OF_YEAR) != cal2.get(Calendar.DAY_OF_YEAR)
}

private fun formatMessageDate(timestamp: Long): String {
    val now = Calendar.getInstance()
    val msgCal = Calendar.getInstance().apply { timeInMillis = timestamp }

    return when {
        now.get(Calendar.YEAR) == msgCal.get(Calendar.YEAR) &&
        now.get(Calendar.DAY_OF_YEAR) == msgCal.get(Calendar.DAY_OF_YEAR) -> "Aujourd'hui"

        now.get(Calendar.YEAR) == msgCal.get(Calendar.YEAR) &&
        now.get(Calendar.DAY_OF_YEAR) - msgCal.get(Calendar.DAY_OF_YEAR) == 1 -> "Hier"

        else -> SimpleDateFormat("d MMMM yyyy", Locale.getDefault()).format(Date(timestamp))
    }
}

/**
 * AnimatedMessageEntrance:
 * Provides a fluid, ultra-smooth slide-in entry animation for newly appearing message bubbles.
 * - Sender messages ("Moi") glide in fluidly from the right side (+80dp) into place.
 * - Recipient messages ("Partner") glide in fluidly from the left side (-80dp) into place.
 * - Employs a custom high-end cubic-bezier deceleration curve and gentle physical landing.
 */
@Composable
private fun AnimatedMessageEntrance(
    isMe: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current
    val slideDistancePx = with(density) { 80.dp.toPx() }
    val initialOffsetX = if (isMe) slideDistancePx else -slideDistancePx
    val initialOffsetY = with(density) { 14.dp.toPx() }

    val alphaAnim = remember { Animatable(0f) }
    val scaleAnim = remember { Animatable(0.93f) }
    val offsetXAnim = remember { Animatable(initialOffsetX) }
    val offsetYAnim = remember { Animatable(initialOffsetY) }

    LaunchedEffect(Unit) {
        // High-end smooth deceleration curve for a fluid, natural slide-in
        val fluidEasing = CubicBezierEasing(0.08f, 0.85f, 0.15f, 1.0f)

        launch {
            alphaAnim.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing)
            )
        }
        launch {
            scaleAnim.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = 0.82f,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
        }
        launch {
            offsetXAnim.animateTo(
                targetValue = 0f,
                animationSpec = tween(
                    durationMillis = 340,
                    easing = fluidEasing
                )
            )
        }
        launch {
            offsetYAnim.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = 0.85f,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
        }
    }

    Box(
        modifier = modifier.graphicsLayer {
            alpha = alphaAnim.value
            scaleX = scaleAnim.value
            scaleY = scaleAnim.value
            translationX = offsetXAnim.value
            translationY = offsetYAnim.value
        }
    ) {
        content()
    }
}
