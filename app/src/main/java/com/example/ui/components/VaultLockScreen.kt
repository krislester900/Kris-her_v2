package com.example.ui.components

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.KablammoFontFamily
import com.example.ui.theme.PureBlack
import com.example.ui.theme.PureWhite

@Composable
fun VaultLockScreen(
    enteredPin: String,
    hasError: Boolean,
    onDigitClick: (String) -> Unit,
    onDeleteClick: () -> Unit,
    onQuickUnlock: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = PureBlack
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp, vertical = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(modifier = Modifier.height(24.dp))
                KrisLogo(
                    size = 80.dp,
                    shape = CircleShape,
                    showShadow = true,
                    borderColor = PureWhite
                )
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "kris&her",
                    fontFamily = KablammoFontFamily,
                    fontSize = 32.sp,
                    color = PureWhite
                )
                Text(
                    text = "COFFRE PRIVÉ VERROUILLÉ",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp,
                    color = PureWhite
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Saisissez votre code PIN pour déverrouiller l'application.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFAAAAAA),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))

                // PIN indicator dots
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (i in 0 until 4) {
                        val isFilled = i < enteredPin.length
                        val dotColor = when {
                            hasError -> Color(0xFFEF4444)
                            isFilled -> PureWhite
                            else -> Color(0xFF404040)
                        }
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(if (isFilled) dotColor else Color.Transparent)
                                .border(2.dp, dotColor, CircleShape)
                        )
                    }
                }

                if (hasError) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Code incorrect. Réessayez.",
                        color = Color(0xFFEF4444),
                        style = MaterialTheme.typography.labelMedium
                    )
                } else {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Entrez votre code PIN",
                        color = Color(0xFF888888),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }

            // Keypad
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                val rows = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf("QUICK", "0", "DEL")
                )

                for (row in rows) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        for (key in row) {
                            when (key) {
                                "DEL" -> {
                                    Box(
                                        modifier = Modifier
                                            .size(72.dp)
                                            .clip(CircleShape)
                                            .clickable(onClick = onDeleteClick)
                                            .testTag("pin_delete_button"),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.Backspace,
                                            contentDescription = "Effacer",
                                            tint = PureWhite
                                        )
                                    }
                                }
                                "QUICK" -> {
                                    // Case vide : l'ancien « déverrouillage rapide » contournait le code PIN
                                    Box(modifier = Modifier.size(72.dp))
                                }
                                else -> {
                                    Box(
                                        modifier = Modifier
                                            .size(72.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF141414))
                                            .border(1.5.dp, Color(0xFF333333), CircleShape)
                                            .clickable { onDigitClick(key) }
                                            .testTag("pin_digit_$key"),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = key,
                                            fontSize = 26.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = PureWhite
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // (le bouton « déverrouillage rapide » qui contournait le code PIN a été supprimé)
            Spacer(modifier = Modifier.height(1.dp))
        }
    }
}
