package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.KablammoFontFamily
import com.example.ui.theme.NewSchoolCyan
import com.example.ui.theme.NewSchoolPink
import com.example.ui.theme.PureBlack
import com.example.ui.theme.PureWhite

/**
 * Interface de capture audio dédiée pour les messages vocaux de kris&her.
 * Propose : visualiseur d'amplitude en direct, chronomètre, pause/reprise, annulation et envoi.
 */
@Composable
fun AudioCapturePanel(
    isRecording: Boolean,
    isPaused: Boolean,
    durationSec: Int,
    amplitudes: List<Float>,
    onPauseResumeToggle: () -> Unit,
    onCancel: () -> Unit,
    onSend: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    AnimatedVisibility(
        visible = isRecording,
        enter = slideInVertically { it } + fadeIn(),
        exit = slideOutVertically { it } + fadeOut()
    ) {
        Surface(
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            color = PureBlack,
            border = androidx.compose.foundation.BorderStroke(2.dp, if (isPaused) PureWhite else NewSchoolCyan),
            shadowElevation = 12.dp,
            modifier = modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top status bar with live indicator & Kablammo duration
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isPaused) Color(0xFFFFB703) else NewSchoolPink.copy(alpha = pulseAlpha)
                                )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isPaused) "ENREGISTREMENT EN PAUSE" else "CAPTURE AUDIO ACTIVE",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = if (isPaused) Color(0xFFFFB703) else PureWhite
                        )
                    }

                    // Chronomètre
                    val m = durationSec / 60
                    val s = durationSec % 60
                    Text(
                        text = "%02d:%02d".format(m, s),
                        fontFamily = KablammoFontFamily,
                        fontSize = 18.sp,
                        color = PureWhite
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Waveform live audio amplitude visualizer
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF0F0F0F))
                        .border(1.dp, Color(0xFF2B2B2B), RoundedCornerShape(14.dp))
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxWidth().height(36.dp)) {
                        val barWidth = 3.5.dp.toPx()
                        val gap = 2.dp.toPx()
                        val totalSlots = (size.width / (barWidth + gap)).toInt().coerceIn(12, 35)

                        val displayAmps = if (amplitudes.isEmpty()) {
                            List(totalSlots) { 0.15f }
                        } else {
                            val list = amplitudes.takeLast(totalSlots)
                            if (list.size < totalSlots) {
                                List(totalSlots - list.size) { 0.12f } + list
                            } else list
                        }

                        for (i in displayAmps.indices) {
                            val amp = displayAmps[i].coerceIn(0.1f, 1f)
                            val barHeight = (size.height * amp).coerceAtLeast(4.dp.toPx())
                            val x = i * (barWidth + gap)
                            val y = (size.height - barHeight) / 2

                            val barColor = if (isPaused) {
                                Color(0xFF666666)
                            } else if (i >= displayAmps.size - 4) {
                                NewSchoolCyan
                            } else {
                                PureWhite
                            }

                            drawRoundRect(
                                color = barColor,
                                topLeft = Offset(x, y),
                                size = Size(barWidth, barHeight),
                                cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action Controls Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Supprimer / Annuler
                    IconButton(
                        onClick = onCancel,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1E1E1E))
                            .border(1.5.dp, Color(0xFF444444), CircleShape)
                            .testTag("cancel_audio_record")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Supprimer l'enregistrement",
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // Pause / Reprendre
                    IconButton(
                        onClick = onPauseResumeToggle,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1E1E1E))
                            .border(1.5.dp, PureWhite, CircleShape)
                            .testTag("pause_resume_audio_record")
                    ) {
                        Icon(
                            imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                            contentDescription = if (isPaused) "Reprendre" else "Mettre en pause",
                            tint = PureWhite,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // Envoyer le vocal
                    IconButton(
                        onClick = onSend,
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(PureWhite)
                            .border(2.dp, NewSchoolPink, CircleShape)
                            .testTag("send_audio_record")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Envoyer le message vocal",
                            tint = PureBlack,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Dialogue d'explication et demande de permission microphone
 */
@Composable
fun AudioPermissionDialog(
    onDismiss: () -> Unit,
    onGrantPermission: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(PureWhite),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = null,
                        tint = PureBlack,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Permission Microphone",
                    fontFamily = KablammoFontFamily,
                    fontSize = 17.sp,
                    color = PureWhite
                )
            }
        },
        text = {
            Text(
                text = "kris&her a besoin d'accéder au microphone de votre appareil pour enregistrer et envoyer vos notes vocales.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFAAAAAA),
                lineHeight = 20.sp
            )
        },
        confirmButton = {
            Button(
                onClick = onGrantPermission,
                colors = ButtonDefaults.buttonColors(containerColor = PureWhite, contentColor = PureBlack),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Autoriser le micro", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler", color = Color(0xFFAAAAAA))
            }
        },
        containerColor = Color(0xFF121212)
    )
}
