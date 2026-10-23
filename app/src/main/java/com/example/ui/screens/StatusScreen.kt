package com.example.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.StatusStory
import com.example.ui.components.EnteAvatar
import com.example.ui.components.NewSchoolEmojiBadge
import com.example.ui.components.getNewSchoolBorderColor
import com.example.ui.theme.KablammoFontFamily
import com.example.ui.theme.NewSchoolCyan
import com.example.ui.theme.NewSchoolLime
import com.example.ui.theme.NewSchoolPink
import com.example.ui.theme.NewSchoolYellow
import com.example.ui.theme.PureBlack
import com.example.ui.theme.PureWhite

@Composable
fun StatusScreen(
    stories: List<StatusStory>,
    onPostStatus: (caption: String, emoji: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var viewingStory by remember { mutableStateOf<StatusStory?>(null) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(PureBlack)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        item {
            // Header in Kablammo font
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "STATUTS",
                        fontFamily = KablammoFontFamily,
                        fontSize = 26.sp,
                        color = PureWhite
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = PureWhite,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "ÉPHÉMÈRE 24 HEURES • CHIFFRÉ",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFAAAAAA),
                            fontSize = 10.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // My Status Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF111111)),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, PureWhite),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showAddDialog = true }
                    .testTag("my_status_card")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(54.dp)) {
                        EnteAvatar(
                            initials = "MOI",
                            colorHex = "#FFFFFF",
                            size = 54.dp,
                            borderColor = NewSchoolPink
                        )
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(PureWhite)
                                .align(Alignment.BottomEnd),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Ajouter un statut",
                                tint = PureBlack,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column {
                        Text(
                            text = "Mon statut",
                            fontFamily = KablammoFontFamily,
                            fontSize = 16.sp,
                            color = PureWhite
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Touchez pour partager une note éphémère",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFAAAAAA)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "MISES À JOUR RÉCENTES",
                fontFamily = KablammoFontFamily,
                fontSize = 14.sp,
                color = PureWhite,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(10.dp))
        }

        if (stories.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        NewSchoolEmojiBadge(
                            emoji = "⚡",
                            label = "STATUT",
                            borderColor = NewSchoolCyan,
                            isDarkBg = true
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "AUCUN STATUT ACTUEL",
                            fontFamily = KablammoFontFamily,
                            fontSize = 16.sp,
                            color = PureWhite
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Publiez le premier statut éphémère pour vos contacts.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF888888),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(stories) { story ->
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF111111)),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF333333)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable { viewingStory = story }
                        .testTag("story_item_${story.id}")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Story Ring Avatar with New School colored outline
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .clip(CircleShape)
                                .border(2.5.dp, getNewSchoolBorderColor(story.id.hashCode()), CircleShape)
                                .padding(3.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            EnteAvatar(
                                initials = story.authorInitials,
                                colorHex = "#FFFFFF",
                                size = 46.dp,
                                borderColor = PureWhite
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = story.authorName,
                                fontFamily = KablammoFontFamily,
                                fontSize = 15.sp,
                                color = PureWhite
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${story.emojiTag} ${story.caption}",
                                style = MaterialTheme.typography.bodySmall,
                                color = PureWhite,
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = story.timeAgo,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF888888)
                            )
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }

    // Add status modal in Black & White with New School stickers
    if (showAddDialog) {
        var captionText by remember { mutableStateOf("") }
        var selectedEmoji by remember { mutableStateOf("⚡") }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            containerColor = PureBlack,
            title = {
                Text(
                    text = "NOUVEAU STATUT",
                    fontFamily = KablammoFontFamily,
                    fontSize = 20.sp,
                    color = PureWhite
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Ce statut sera visible par vos contacts et s'autodétruira après 24 heures.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFAAAAAA)
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = captionText,
                        onValueChange = { captionText = it },
                        placeholder = { Text("Que partagez-vous ?", color = Color(0xFF666666)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("status_caption_input"),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF141414),
                            unfocusedContainerColor = Color(0xFF141414),
                            focusedTextColor = PureWhite,
                            unfocusedTextColor = PureWhite,
                            focusedBorderColor = PureWhite,
                            unfocusedBorderColor = Color(0xFF333333)
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "STICKER NEW SCHOOL :",
                        fontFamily = KablammoFontFamily,
                        fontSize = 12.sp,
                        color = PureWhite
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        val stickers = listOf(
                            "⚡" to NewSchoolYellow,
                            "❤️" to NewSchoolPink,
                            "🔥" to NewSchoolCyan,
                            "💀" to NewSchoolLime,
                            "👑" to PureWhite
                        )
                        for ((e, col) in stickers) {
                            NewSchoolEmojiBadge(
                                emoji = e,
                                borderColor = if (selectedEmoji == e) PureWhite else col,
                                isDarkBg = true,
                                onClick = { selectedEmoji = e }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (captionText.isNotBlank()) {
                            onPostStatus(captionText.trim(), selectedEmoji)
                            showAddDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PureWhite,
                        contentColor = PureBlack
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("publish_status_button")
                ) {
                    Text(
                        text = "PUBLIER",
                        fontFamily = KablammoFontFamily,
                        fontSize = 12.sp
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Annuler", color = Color(0xFFAAAAAA))
                }
            }
        )
    }

    // Story viewer modal
    if (viewingStory != null) {
        val story = viewingStory!!
        Dialog(
            onDismissRequest = { viewingStory = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            val progress = remember { Animatable(0f) }

            LaunchedEffect(story.id) {
                progress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 5000, easing = LinearEasing)
                )
                viewingStory = null
            }

            Surface(
                modifier = Modifier.fillMaxSize(),
                color = PureBlack
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Spacer(modifier = Modifier.height(16.dp))
                        LinearProgressIndicator(
                            progress = { progress.value },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = PureWhite,
                            trackColor = Color(0xFF333333)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                EnteAvatar(
                                    initials = story.authorInitials,
                                    colorHex = "#FFFFFF",
                                    size = 40.dp,
                                    borderColor = PureWhite
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = story.authorName,
                                        fontFamily = KablammoFontFamily,
                                        fontSize = 16.sp,
                                        color = PureWhite
                                    )
                                    Text(
                                        text = "${story.timeAgo}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFFAAAAAA)
                                    )
                                }
                            }

                            IconButton(onClick = { viewingStory = null }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Fermer",
                                    tint = PureWhite
                                )
                            }
                        }
                    }

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = story.emojiTag,
                            fontSize = 72.sp
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = story.caption,
                            style = MaterialTheme.typography.headlineSmall,
                            color = PureWhite,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xFF141414),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF333333)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Répondre à ${story.authorName}...",
                                color = Color(0xFF888888),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Répondre",
                                tint = PureWhite,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
