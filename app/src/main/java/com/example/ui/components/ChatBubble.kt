package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ConversationEntity
import com.example.model.MessageEntity
import com.example.ui.theme.KablammoFontFamily
import com.example.ui.theme.NewSchoolCyan
import com.example.ui.theme.NewSchoolLime
import com.example.ui.theme.NewSchoolOrange
import com.example.ui.theme.NewSchoolPink
import com.example.ui.theme.NewSchoolPurple
import com.example.ui.theme.NewSchoolYellow
import com.example.ui.theme.PureBlack
import com.example.ui.theme.PureWhite
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * BubbleStyle: Available aesthetics for the chat bubbles.
 */
enum class BubbleStyle(val title: String, val subtitle: String) {
    COMIC("Bulle BD Pop", "Style New-School avec queue de bulle BD, ombre décalée et traits appuyés"),
    MODERN("Bulle Moderne Ronde", "Courbure fluide, micro-pointe organique et bordure discrète"),
    NEON("Bulle Cyber Néon", "Contours lumineux néon cyan & fuchsia, look futuriste sécurisé")
}

/**
 * Custom SpeechBubbleShape with directional pointer/tail.
 * - [isMe] : if true, tail is positioned on the bottom right; otherwise on bottom left.
 * - [hasTail] : if false (e.g. grouped message), draws a smooth rounded rectangle without tail.
 * - [style] : changes the geometry of the tail (comic pointed tail, modern rounded curve, or cyber angled bevel).
 */
class SpeechBubbleShape(
    val isMe: Boolean,
    val hasTail: Boolean = true,
    val style: BubbleStyle = BubbleStyle.COMIC,
    val cornerRadius: Dp = 16.dp,
    val tailWidth: Dp = 9.dp,
    val tailHeight: Dp = 10.dp
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val crPx = with(density) { cornerRadius.toPx() }
        val twPx = with(density) { tailWidth.toPx() }
        val thPx = with(density) { tailHeight.toPx() }

        val path = Path()

        if (!hasTail) {
            // Clustered message without tail
            val roundRect = RoundRect(
                rect = Rect(0f, 0f, size.width, size.height),
                topLeft = CornerRadius(crPx, crPx),
                topRight = CornerRadius(crPx, crPx),
                bottomLeft = CornerRadius(if (isMe) crPx else 6f, if (isMe) crPx else 6f),
                bottomRight = CornerRadius(if (isMe) 6f else crPx, if (isMe) 6f else crPx)
            )
            path.addRoundRect(roundRect)
            return Outline.Generic(path)
        }

        // Message with directional speech tail
        if (isMe) {
            // Sender tail on bottom right
            val bodyWidth = size.width - twPx
            val bodyHeight = size.height

            // Main body
            path.moveTo(crPx, 0f)
            path.lineTo(bodyWidth - crPx, 0f)
            path.quadraticTo(bodyWidth, 0f, bodyWidth, crPx)

            // Right side going down towards tail
            path.lineTo(bodyWidth, bodyHeight - thPx - 4f)

            when (style) {
                BubbleStyle.COMIC -> {
                    // Comic pointy speech tail
                    path.lineTo(size.width, bodyHeight)
                    path.lineTo(bodyWidth - (crPx * 0.7f), bodyHeight)
                }
                BubbleStyle.MODERN -> {
                    // Smooth curved micro-tail
                    path.quadraticTo(size.width, bodyHeight - 2f, size.width, bodyHeight)
                    path.quadraticTo(bodyWidth, bodyHeight, bodyWidth - crPx, bodyHeight)
                }
                BubbleStyle.NEON -> {
                    // Angular cyber tail
                    path.lineTo(size.width, bodyHeight - 2f)
                    path.lineTo(bodyWidth - crPx, bodyHeight)
                }
            }

            // Bottom side
            path.lineTo(crPx, bodyHeight)
            path.quadraticTo(0f, bodyHeight, 0f, bodyHeight - crPx)

            // Left side going up
            path.lineTo(0f, crPx)
            path.quadraticTo(0f, 0f, crPx, 0f)
            path.close()

        } else {
            // Partner tail on bottom left
            val bodyLeft = twPx
            val bodyWidth = size.width - twPx
            val bodyHeight = size.height

            // Start after tail on bottom left
            path.moveTo(bodyLeft + crPx, 0f)
            path.lineTo(size.width - crPx, 0f)
            path.quadraticTo(size.width, 0f, size.width, crPx)

            // Right side
            path.lineTo(size.width, bodyHeight - crPx)
            path.quadraticTo(size.width, bodyHeight, size.width - crPx, bodyHeight)

            // Bottom towards tail
            path.lineTo(bodyLeft + (crPx * 0.7f), bodyHeight)

            when (style) {
                BubbleStyle.COMIC -> {
                    // Comic pointy tail to bottom left
                    path.lineTo(0f, bodyHeight)
                    path.lineTo(bodyLeft, bodyHeight - thPx - 4f)
                }
                BubbleStyle.MODERN -> {
                    // Smooth curved micro-tail
                    path.quadraticTo(0f, bodyHeight, 0f, bodyHeight)
                    path.quadraticTo(bodyLeft, bodyHeight - 2f, bodyLeft, bodyHeight - thPx - 4f)
                }
                BubbleStyle.NEON -> {
                    // Cyber angled tail
                    path.lineTo(0f, bodyHeight - 2f)
                    path.lineTo(bodyLeft, bodyHeight - thPx - 4f)
                }
            }

            // Left side going up
            path.lineTo(bodyLeft, crPx)
            path.quadraticTo(bodyLeft, 0f, bodyLeft + crPx, 0f)
            path.close()
        }

        return Outline.Generic(path)
    }
}

/**
 * ChatBubbleContainer:
 * The rich wrapper for any message content.
 * Draws comic drop shadows, matching speech bubble tails, double-tap heart animations, and long-press menus.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatBubbleContainer(
    isMe: Boolean,
    hasTail: Boolean,
    style: BubbleStyle = BubbleStyle.COMIC,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {},
    onDoubleTapHeart: () -> Unit = {},
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = remember(isMe, hasTail, style) {
        SpeechBubbleShape(isMe = isMe, hasTail = hasTail, style = style)
    }

    // Color palette based on style & sender
    val (bubbleBg, bubbleBorder, shadowColor) = remember(isMe, style) {
        when (style) {
            BubbleStyle.COMIC -> {
                val bg = if (isMe) PureWhite else Color(0xFF141414)
                val border = if (isMe) PureWhite else PureWhite.copy(alpha = 0.9f)
                val shadow = if (isMe) PureWhite.copy(alpha = 0.28f) else PureWhite.copy(alpha = 0.16f)
                Triple(bg, border, shadow)
            }
            BubbleStyle.MODERN -> {
                val bg = if (isMe) PureWhite else Color(0xFF1E1E1E)
                val border = if (isMe) Color(0xFFE5E5E5) else Color(0xFF333333)
                val shadow = Color.Transparent
                Triple(bg, border, shadow)
            }
            BubbleStyle.NEON -> {
                val bg = if (isMe) Color(0xFF111116) else Color(0xFF0F0F12)
                val border = if (isMe) NewSchoolCyan else NewSchoolPink
                val shadow = (if (isMe) NewSchoolCyan else NewSchoolPink).copy(alpha = 0.25f)
                Triple(bg, border, shadow)
            }
        }
    }

    // Double tap heart burst state
    var showHeartBurst by remember { mutableStateOf(false) }
    val heartScale = remember { Animatable(0f) }
    val heartAlpha = remember { Animatable(0f) }
    val coroutineScope = rememberCoroutineScope()

    Box(
        modifier = modifier
            .widthIn(min = 110.dp, max = 295.dp)
    ) {
        // Comic or Cyber Drop Shadow layer
        if (style == BubbleStyle.COMIC || style == BubbleStyle.NEON) {
            val offsetX = if (isMe) 2.5.dp else (-2.5).dp
            val offsetY = 2.5.dp
            Box(
                modifier = Modifier
                    .offset(x = offsetX, y = offsetY)
                    .matchParentSize()
                    .clip(shape)
                    .background(shadowColor)
            )
        }

        // Main Bubble Surface
        Surface(
            shape = shape,
            color = bubbleBg,
            border = androidx.compose.foundation.BorderStroke(
                width = if (style == BubbleStyle.NEON) 1.8.dp else 1.5.dp,
                color = bubbleBorder
            ),
            modifier = Modifier
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { onClick() },
                        onLongPress = { onLongClick() },
                        onDoubleTap = {
                            onDoubleTapHeart()
                            coroutineScope.launch {
                                showHeartBurst = true
                                heartScale.snapTo(0.2f)
                                heartAlpha.snapTo(1f)
                                heartScale.animateTo(
                                    targetValue = 1.3f,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessMedium
                                    )
                                )
                                delay(250)
                                heartAlpha.animateTo(0f, tween(180))
                                showHeartBurst = false
                            }
                        }
                    )
                }
        ) {
            Box {
                // Inner padding (accommodates tail offset)
                val startPad = if (!isMe && hasTail) 18.dp else 12.dp
                val endPad = if (isMe && hasTail) 18.dp else 12.dp

                Column(
                    modifier = Modifier.padding(
                        start = startPad,
                        end = endPad,
                        top = 9.dp,
                        bottom = 8.dp
                    )
                ) {
                    content()
                }

                // Heart Pop Animation on double tap
                if (showHeartBurst) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clip(shape)
                            .background(Color.Black.copy(alpha = 0.25f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = "Coup de cœur",
                            tint = NewSchoolPink,
                            modifier = Modifier
                                .size(50.dp)
                                .graphicsLayer {
                                    scaleX = heartScale.value
                                    scaleY = heartScale.value
                                    alpha = heartAlpha.value
                                }
                        )
                    }
                }
            }
        }
    }
}

/**
 * QuotedMessageBubble:
 * Rendered inside a chat bubble when replying to a message.
 */
@Composable
fun QuotedMessageBubble(
    authorName: String,
    contentPreview: String,
    isMe: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (isMe) PureBlack.copy(alpha = 0.08f) else PureWhite.copy(alpha = 0.1f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isMe) PureBlack.copy(alpha = 0.2f) else PureWhite.copy(alpha = 0.2f)
        ),
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Accent bar
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(28.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(if (isMe) PureBlack else NewSchoolCyan)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = authorName,
                    fontFamily = KablammoFontFamily,
                    fontSize = 11.sp,
                    color = if (isMe) PureBlack else NewSchoolCyan,
                    maxLines = 1,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = contentPreview,
                    fontSize = 11.5.sp,
                    color = if (isMe) PureBlack.copy(alpha = 0.75f) else PureWhite.copy(alpha = 0.75f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * BubbleStyleSelectorDialog:
 * Allows user to switch between COMIC, MODERN, and NEON speech bubble designs.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BubbleStyleSelectorDialog(
    currentStyle: BubbleStyle,
    onSelectStyle: (BubbleStyle) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(22.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Palette,
                    contentDescription = null,
                    tint = PureWhite,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "BULLES DE DISCUSSION",
                        fontFamily = KablammoFontFamily,
                        fontSize = 18.sp,
                        color = PureWhite
                    )
                    Text(
                        text = "Personnalisation graphique kris&her",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFAAAAAA)
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                BubbleStyle.values().forEach { styleOption ->
                    val isSelected = currentStyle == styleOption

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isSelected) Color(0xFF222222) else Color(0xFF141414),
                        border = androidx.compose.foundation.BorderStroke(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) NewSchoolCyan else Color(0xFF333333)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .combinedClickable(onClick = { onSelectStyle(styleOption) })
                            .testTag("bubble_style_${styleOption.name.lowercase()}")
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = styleOption.title,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = if (isSelected) NewSchoolCyan else PureWhite
                                    )
                                }
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Sélectionné",
                                        tint = NewSchoolCyan,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = styleOption.subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFAAAAAA),
                                fontSize = 11.sp
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // Mini preview of the style
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(PureBlack)
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Friend mini bubble
                                ChatBubbleContainer(
                                    isMe = false,
                                    hasTail = true,
                                    style = styleOption,
                                    modifier = Modifier.widthIn(max = 110.dp)
                                ) {
                                    Text(
                                        text = "Salut Kris ! ⚡",
                                        fontSize = 10.sp,
                                        color = if (styleOption == BubbleStyle.NEON) NewSchoolPink else PureWhite
                                    )
                                }

                                // Me mini bubble
                                ChatBubbleContainer(
                                    isMe = true,
                                    hasTail = true,
                                    style = styleOption,
                                    modifier = Modifier.widthIn(max = 110.dp)
                                ) {
                                    Text(
                                        text = "Reçu 5/5 💬",
                                        fontSize = 10.sp,
                                        color = if (styleOption == BubbleStyle.NEON) NewSchoolCyan else PureBlack
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = PureWhite,
                    contentColor = PureBlack
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Appliquer le style", fontWeight = FontWeight.Bold)
            }
        }
    )
}

/**
 * FloatingChatHead:
 * Draggable floating conversation bubble overlay.
 * Users can tap to instantly expand the chat, or drag around the screen.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FloatingChatHead(
    conversation: ConversationEntity,
    onClick: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var offsetX by remember { mutableFloatStateOf(60f) }
    var offsetY by remember { mutableFloatStateOf(400f) }
    var isDragging by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { isDragging = true },
                    onDragEnd = { isDragging = false },
                    onDragCancel = { isDragging = false },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        offsetX += dragAmount.x
                        offsetY += dragAmount.y
                    }
                )
            }
            .testTag("floating_chat_head")
    ) {
        // Comic shadow under bubble head
        Box(
            modifier = Modifier
                .offset(x = 3.dp, y = 3.dp)
                .size(54.dp)
                .clip(CircleShape)
                .background(PureWhite.copy(alpha = 0.25f))
        )

        // Main head avatar
        Surface(
            shape = CircleShape,
            color = PureBlack,
            border = androidx.compose.foundation.BorderStroke(2.dp, PureWhite),
            modifier = Modifier
                .size(54.dp)
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onDismiss
                )
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = conversation.avatarInitials.ifBlank { conversation.title.take(2).uppercase() },
                    fontFamily = KablammoFontFamily,
                    fontSize = 16.sp,
                    color = PureWhite
                )

                // Unread or active badge
                if (conversation.unreadCount > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(NewSchoolPink)
                            .border(1.dp, PureBlack, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = conversation.unreadCount.toString(),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = PureWhite
                        )
                    }
                }

                // Mini close icon button on long-press affordance
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(PureBlack)
                        .border(1.dp, PureWhite, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Chat,
                        contentDescription = "Bulle active",
                        tint = NewSchoolCyan,
                        modifier = Modifier.size(10.dp)
                    )
                }
            }
        }
    }
}
