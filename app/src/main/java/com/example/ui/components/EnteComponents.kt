package com.example.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SentimentSatisfiedAlt
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Visibility
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.MessageStatus
import com.example.ui.theme.KablammoFontFamily
import com.example.ui.theme.NewSchoolCyan
import com.example.ui.theme.NewSchoolLime
import com.example.ui.theme.NewSchoolOrange
import com.example.ui.theme.NewSchoolPink
import com.example.ui.theme.NewSchoolYellow
import com.example.ui.theme.PureBlack
import com.example.ui.theme.PureWhite
import com.example.viewmodel.ActiveCallState

@Composable
fun EnteAvatar(
    initials: String,
    colorHex: String = "#FFFFFF",
    size: Dp = 48.dp,
    showOnline: Boolean = false,
    borderColor: Color = PureWhite,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.size(size)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(PureBlack)
                .border(2.dp, borderColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initials,
                color = PureWhite,
                fontFamily = KablammoFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = (size.value * 0.36f).sp
            )
        }

        if (showOnline) {
            Box(
                modifier = Modifier
                    .size(size * 0.28f)
                    .clip(CircleShape)
                    .background(PureWhite)
                    .border(2.dp, PureBlack, CircleShape)
                    .align(Alignment.BottomEnd)
            )
        }
    }
}

@Composable
fun MessageStatusIcon(status: MessageStatus, modifier: Modifier = Modifier) {
    when (status) {
        MessageStatus.SENDING -> {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "Envoi",
                modifier = modifier.size(12.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
        MessageStatus.SENT -> {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Envoyé",
                modifier = modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
        MessageStatus.DELIVERED -> {
            Icon(
                imageVector = Icons.Default.DoneAll,
                contentDescription = "Distribué",
                modifier = modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
        MessageStatus.READ -> {
            Icon(
                imageVector = Icons.Default.DoneAll,
                contentDescription = "Lu",
                modifier = modifier.size(14.dp),
                tint = PureWhite
            )
        }
    }
}

@Composable
fun TypingIndicator(friendName: String, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "dots")
    val alpha1 by transition.animateFloat(
        initialValue = 0.2f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse), label = "d1"
    )
    val alpha2 by transition.animateFloat(
        initialValue = 0.2f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(600, delayMillis = 200), RepeatMode.Reverse), label = "d2"
    )
    val alpha3 by transition.animateFloat(
        initialValue = 0.2f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(600, delayMillis = 400), RepeatMode.Reverse), label = "d3"
    )

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier.padding(vertical = 4.dp, horizontal = 12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Text(
                text = "$friendName écrit",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(6.dp))
            Box(Modifier.size(6.dp).clip(CircleShape).background(MaterialTheme.colorScheme.onSurface.copy(alpha = alpha1)))
            Spacer(modifier = Modifier.width(3.dp))
            Box(Modifier.size(6.dp).clip(CircleShape).background(MaterialTheme.colorScheme.onSurface.copy(alpha = alpha2)))
            Spacer(modifier = Modifier.width(3.dp))
            Box(Modifier.size(6.dp).clip(CircleShape).background(MaterialTheme.colorScheme.onSurface.copy(alpha = alpha3)))
        }
    }
}

@Composable
fun SafetyNumberDialog(
    contactName: String,
    fingerprint: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = PureWhite,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Numéro de sécurité kris&her",
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = KablammoFontFamily
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Code de sécurité de la conversation avec $contactName (démonstration, sans vérification cryptographique) :",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Simulated QR visual block
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    modifier = Modifier.size(140.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.QrCode2,
                            contentDescription = "Code QR de vérification",
                            modifier = Modifier.size(110.dp),
                            tint = Color.Black
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Fingerprint code blocks
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, NewSchoolCyan),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = fingerprint,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp
                        ),
                        color = PureWhite,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(12.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = PureWhite,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Code de démonstration",
                        style = MaterialTheme.typography.labelSmall,
                        color = PureWhite
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = PureWhite, contentColor = PureBlack)
            ) {
                Text("Vérifier et Fermer", fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
fun ActiveCallDialog(
    callState: ActiveCallState,
    onToggleMute: () -> Unit,
    onToggleSpeaker: () -> Unit,
    onEndCall: () -> Unit
) {
    Dialog(
        onDismissRequest = onEndCall,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0F1116)),
            color = Color(0xFF0F1116)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top header
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Spacer(modifier = Modifier.height(32.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(PureWhite.copy(alpha = 0.15f))
                            .border(1.dp, PureWhite.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = PureWhite,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Appel kris&her",
                            color = PureWhite,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    EnteAvatar(
                        initials = callState.avatarInitials,
                        colorHex = callState.avatarColorHex,
                        size = 96.dp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = callState.conversationTitle,
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    val m = callState.durationSeconds / 60
                    val s = callState.durationSeconds % 60
                    Text(
                        text = if (callState.durationSeconds == 0) "Connexion sécurisée..." else "%02d:%02d".format(m, s),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF9CA3AF)
                    )
                }

                // Middle: Visual waveform animation or Video indicator
                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .clip(CircleShape)
                        .background(if (callState.isVideo) Color(0xFF1E212B) else PureWhite.copy(alpha = 0.1f))
                        .border(1.5.dp, NewSchoolCyan, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (callState.isVideo) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = null,
                                tint = PureWhite,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Flux vidéo", color = Color.White, fontSize = 11.sp)
                        }
                    } else {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = null,
                            tint = PureWhite,
                            modifier = Modifier.size(54.dp)
                        )
                    }
                }

                // Bottom Call Controls
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Mute
                        IconButton(
                            onClick = onToggleMute,
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(if (callState.isMuted) Color.White else Color(0xFF232733))
                        ) {
                            Icon(
                                imageVector = if (callState.isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                                contentDescription = "Couper le micro",
                                tint = if (callState.isMuted) Color.Black else Color.White
                            )
                        }

                        // End Call
                        IconButton(
                            onClick = onEndCall,
                            modifier = Modifier
                                .size(68.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFEF4444))
                                .testTag("end_call_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.CallEnd,
                                contentDescription = "Raccrocher",
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        // Speaker
                        IconButton(
                            onClick = onToggleSpeaker,
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(if (callState.isSpeaker) PureWhite else Color(0xFF232733))
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = "Haut-parleur",
                                tint = if (callState.isSpeaker) PureBlack else Color.White
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
fun AttachmentOptionsSheet(
    onOptionSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Partager en toute confidentialité",
                style = MaterialTheme.typography.titleMedium,
                fontFamily = KablammoFontFamily
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Les fichiers sont envoyés via une connexion sécurisée.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    AttachmentOptionItem(
                        icon = Icons.Default.Photo,
                        label = "Galerie",
                        tint = NewSchoolPink,
                        onClick = { onOptionSelected("PHOTO") }
                    )
                    AttachmentOptionItem(
                        icon = Icons.Default.CameraAlt,
                        label = "Caméra",
                        tint = NewSchoolCyan,
                        onClick = { onOptionSelected("CAMERA") }
                    )
                    AttachmentOptionItem(
                        icon = Icons.Default.SentimentSatisfiedAlt,
                        label = "Sticker",
                        tint = NewSchoolOrange,
                        onClick = { onOptionSelected("STICKER") }
                    )
                    AttachmentOptionItem(
                        icon = Icons.Default.Person,
                        label = "Contact",
                        tint = NewSchoolYellow,
                        onClick = { onOptionSelected("CONTACT") }
                    )
                    AttachmentOptionItem(
                        icon = Icons.Default.Security,
                        label = "Accès",
                        tint = NewSchoolLime,
                        onClick = { onOptionSelected("PERMISSIONS") }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler", color = MaterialTheme.colorScheme.primary)
            }
        }
    )
}

@Composable
private fun AttachmentOptionItem(
    icon: ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(PureBlack)
                .border(2.dp, tint, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = label, tint = PureWhite, modifier = Modifier.size(24.dp))
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun MessageInfoDialog(
    message: com.example.model.MessageEntity,
    recipientName: String,
    onDismiss: () -> Unit
) {
    val dateFormat = SimpleDateFormat("d MMMM 'à' HH:mm:ss", Locale.getDefault())

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = PureWhite,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Infos du message",
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = KablammoFontFamily
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Message preview
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = message.content,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Envoyé par ${message.senderName}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = "STATUT DE DISTRIBUTION",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Timeline Item: Lu / Vu
                ReceiptTimelineItem(
                    icon = Icons.Default.DoneAll,
                    iconTint = if (message.readTimestamp != null) PureWhite else MaterialTheme.colorScheme.outline,
                    title = if (message.readTimestamp != null) "Vu / Lu" else "Non encore lu",
                    subtitle = if (message.readTimestamp != null) {
                        "Lu par $recipientName le ${dateFormat.format(Date(message.readTimestamp))}"
                    } else {
                        "En attente de lecture par $recipientName"
                    },
                    isHighlighted = message.readTimestamp != null
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Timeline Item: Distribué
                ReceiptTimelineItem(
                    icon = Icons.Default.DoneAll,
                    iconTint = if (message.deliveredTimestamp != null) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.outline,
                    title = "Distribué",
                    subtitle = if (message.deliveredTimestamp != null) {
                        "Reçu sur l'appareil le ${dateFormat.format(Date(message.deliveredTimestamp))}"
                    } else {
                        "En cours de transmission..."
                    },
                    isHighlighted = false
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Timeline Item: Envoyé
                ReceiptTimelineItem(
                    icon = Icons.Default.Check,
                    iconTint = MaterialTheme.colorScheme.onSurfaceVariant,
                    title = "Envoyé",
                    subtitle = "Expédié le ${dateFormat.format(Date(message.sentTimestamp))}",
                    isHighlighted = false
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Sécurité
                ReceiptTimelineItem(
                    icon = Icons.Default.Lock,
                    iconTint = PureWhite,
                    title = "Messagerie privée",
                    subtitle = "Messages transmis via une connexion sécurisée (Firebase)",
                    isHighlighted = false
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = PureWhite, contentColor = PureBlack)
            ) {
                Text("Fermer", fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
private fun ReceiptTimelineItem(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    isHighlighted: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(if (isHighlighted) PureWhite.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isHighlighted) FontWeight.Bold else FontWeight.SemiBold,
                color = if (isHighlighted) PureWhite else MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp
            )
        }
    }
}
