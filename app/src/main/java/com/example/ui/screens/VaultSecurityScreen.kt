package com.example.ui.screens

import android.widget.Toast
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.KrisLogo
import com.example.ui.theme.KablammoFontFamily
import com.example.ui.theme.PureBlack
import com.example.ui.theme.PureWhite

@Composable
fun VaultSecurityScreen(
    isPinProtectionEnabled: Boolean,
    hasPin: Boolean,
    onSetPin: (String) -> Boolean,
    ephemeralTimerHours: Int,
    onTogglePinProtection: (Boolean) -> Unit,
    onLockVaultNow: () -> Unit,
    onSetEphemeralTimer: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    val showPinDialog = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    val pinInput = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("") }
    val pinConfirm = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("") }
    val pinDialogError = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }

    if (showPinDialog.value) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showPinDialog.value = false },
            title = { Text(if (hasPin) "Changer le code PIN" else "Choisir un code PIN") },
            text = {
                Column {
                    Text("Code à 4 chiffres, demandé à chaque ouverture de l'app. Il n'est pas récupérable : si vous l'oubliez, il faudra réinstaller l'application.")
                    Spacer(modifier = Modifier.height(10.dp))
                    androidx.compose.material3.OutlinedTextField(
                        value = pinInput.value,
                        onValueChange = { v -> pinInput.value = v.filter { it.isDigit() }.take(4) },
                        label = { Text("Nouveau code (4 chiffres)") },
                        singleLine = true,
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.NumberPassword
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    androidx.compose.material3.OutlinedTextField(
                        value = pinConfirm.value,
                        onValueChange = { v -> pinConfirm.value = v.filter { it.isDigit() }.take(4) },
                        label = { Text("Confirmer le code") },
                        singleLine = true,
                        isError = pinDialogError.value,
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.NumberPassword
                        )
                    )
                    if (pinDialogError.value) {
                        Text("Les deux codes doivent être identiques (4 chiffres).", color = Color(0xFFFF6B6B))
                    }
                }
            },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    if (pinInput.value.length == 4 && pinInput.value == pinConfirm.value && onSetPin(pinInput.value)) {
                        pinInput.value = ""
                        pinConfirm.value = ""
                        pinDialogError.value = false
                        showPinDialog.value = false
                    } else {
                        pinDialogError.value = true
                    }
                }) { Text("Enregistrer") }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = {
                    pinInput.value = ""
                    pinConfirm.value = ""
                    pinDialogError.value = false
                    showPinDialog.value = false
                }) { Text("Annuler") }
            }
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(PureBlack)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        item {
            // Header in Kablammo font
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                KrisLogo(
                    size = 44.dp,
                    shape = CircleShape,
                    borderColor = PureWhite
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "COFFRE & CLÉS",
                        fontFamily = KablammoFontFamily,
                        fontSize = 24.sp,
                        color = PureWhite
                    )
                    Text(
                        text = "VERROUILLAGE PAR CODE PIN",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFAAAAAA),
                        letterSpacing = 1.sp
                    )
                }
            }

            // Status Badge
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF111111),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, PureWhite),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = PureWhite,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "CONNEXION SÉCURISÉE",
                            fontFamily = KablammoFontFamily,
                            fontSize = 14.sp,
                            color = PureWhite
                        )
                        Text(
                            text = "Les messages sont transmis via une connexion sécurisée (Firebase).",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFAAAAAA)
                        )
                    }
                }
            }

            // PIN Lock
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F0F0F)),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF333333)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = PureWhite,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Code PIN du coffre",
                                    fontFamily = KablammoFontFamily,
                                    fontSize = 15.sp,
                                    color = PureWhite
                                )
                                Text(
                                    text = if (hasPin) "Code à 4 chiffres choisi par vous" else "Choisissez un code à 4 chiffres",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF888888)
                                )
                            }
                        }

                        Switch(
                            checked = isPinProtectionEnabled,
                            onCheckedChange = { checked ->
                                if (checked && !hasPin) {
                                    pinDialogError.value = false
                                    showPinDialog.value = true
                                } else {
                                    onTogglePinProtection(checked)
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = PureBlack,
                                checkedTrackColor = PureWhite
                            ),
                            modifier = Modifier.testTag("pin_protection_switch")
                        )
                    }

                    if (isPinProtectionEnabled) {
                        Spacer(modifier = Modifier.height(14.dp))
                        androidx.compose.material3.TextButton(
                            onClick = {
                                pinDialogError.value = false
                                showPinDialog.value = true
                            }
                        ) { Text("Changer le code PIN", color = PureWhite) }
                        Button(
                            onClick = onLockVaultNow,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PureWhite,
                                contentColor = PureBlack
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("lock_vault_now_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = PureBlack,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "VERROUILLER MAINTENANT",
                                fontFamily = KablammoFontFamily,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            // Ephemeral messages
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F0F0F)),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF333333)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = null,
                            tint = PureWhite,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Messages éphémères",
                                fontFamily = KablammoFontFamily,
                                fontSize = 15.sp,
                                color = PureWhite
                            )
                            Text(
                                text = "Délai d'autodestruction par défaut",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF888888)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val options = listOf(0 to "Désactivé", 24 to "24 heures", 168 to "7 jours")
                        for ((hours, label) in options) {
                            val isSelected = ephemeralTimerHours == hours
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) PureWhite else Color(0xFF141414),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.5.dp,
                                    if (isSelected) PureWhite else Color(0xFF333333)
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { onSetEphemeralTimer(hours) }
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) PureBlack else PureWhite,
                                    modifier = Modifier.padding(vertical = 10.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }

            // Local Room DB
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F0F0F)),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF333333)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 80.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Storage,
                        contentDescription = null,
                        tint = PureWhite,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Stockage des données",
                            fontFamily = KablammoFontFamily,
                            fontSize = 14.sp,
                            color = PureWhite
                        )
                        Text(
                            text = "Copie locale sur l'appareil + synchronisation via Firebase (Google)",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF888888)
                        )
                    }
                }
            }
        }
    }
}
