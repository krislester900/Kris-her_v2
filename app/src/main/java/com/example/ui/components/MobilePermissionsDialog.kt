package com.example.ui.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.ui.theme.KablammoFontFamily
import com.example.ui.theme.NewSchoolCyan
import com.example.ui.theme.NewSchoolLime
import com.example.ui.theme.NewSchoolOrange
import com.example.ui.theme.NewSchoolPink
import com.example.ui.theme.NewSchoolYellow
import com.example.ui.theme.PureBlack
import com.example.ui.theme.PureWhite

data class AppPermissionItem(
    val title: String,
    val subtitle: String,
    val permission: String?,
    val icon: ImageVector,
    val accentColor: Color,
    val isGranted: Boolean
)

@Composable
fun MobilePermissionsDialog(
    onDismiss: () -> Unit,
    onPermissionsUpdated: () -> Unit = {}
) {
    val context = LocalContext.current

    var hasAudio by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }
    var hasCamera by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    var hasContacts by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
        )
    }

    val multiplePermissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { permissions ->
            hasAudio = permissions[Manifest.permission.RECORD_AUDIO] ?: hasAudio
            hasCamera = permissions[Manifest.permission.CAMERA] ?: hasCamera
            hasContacts = permissions[Manifest.permission.READ_CONTACTS] ?: hasContacts
            onPermissionsUpdated()
        }
    )

    val singleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { _ ->
            hasAudio = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
            hasCamera = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
            hasContacts = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
            onPermissionsUpdated()
        }
    )

    fun refreshState() {
        hasAudio = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        hasCamera = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        hasContacts = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
    }

    LaunchedEffect(Unit) {
        refreshState()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
            .testTag("mobile_permissions_dialog"),
        containerColor = PureBlack,
        shape = RoundedCornerShape(24.dp),
        title = {
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
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = PureBlack,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "AUTORISATIONS MOBILES",
                            fontFamily = KablammoFontFamily,
                            fontSize = 17.sp,
                            color = PureWhite
                        )
                        Text(
                            text = "kris&her • Vie privée",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFAAAAAA),
                            fontSize = 11.sp
                        )
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Fermer",
                        tint = Color(0xFFAAAAAA)
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Pour exploiter pleinement la création de stickers, les fonctions vocales, la galerie et le partage de contacts :",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFCCCCCC),
                    fontSize = 12.5.sp,
                    lineHeight = 17.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // 1. Fonctions Vocales (Microphone)
                PermissionRowCard(
                    title = "Fonctions Vocales & Micro",
                    description = "Notes vocales New-School haute fidélité et appels vocaux",
                    icon = Icons.Default.Mic,
                    accentColor = NewSchoolYellow,
                    isGranted = hasAudio,
                    onRequest = {
                        singleLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                )

                Spacer(modifier = Modifier.height(10.dp))

                // 2. Appareil Photo & Vidéo
                PermissionRowCard(
                    title = "Appareil Photo & Vidéo",
                    description = "Capture live pour concevoir vos autocollants & appels vidéo",
                    icon = Icons.Default.PhotoCamera,
                    accentColor = NewSchoolCyan,
                    isGranted = hasCamera,
                    onRequest = {
                        singleLauncher.launch(Manifest.permission.CAMERA)
                    }
                )

                Spacer(modifier = Modifier.height(10.dp))

                // 3. Galerie & Médias (Photos/Vidéos)
                PermissionRowCard(
                    title = "Galerie Photos & Vidéos",
                    description = "Import d'images et vidéos pour le studio d'émoticônes & stickers personnalisés",
                    icon = Icons.Default.PhotoLibrary,
                    accentColor = NewSchoolPink,
                    isGranted = true, // Photo Picker est zero-permission par défaut sous Android
                    onRequest = {},
                    isPhotoPicker = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                // 4. Contacts
                PermissionRowCard(
                    title = "Fiches Contacts",
                    description = "Partage de correspondants et invitations",
                    icon = Icons.Default.Contacts,
                    accentColor = NewSchoolLime,
                    isGranted = hasContacts,
                    onRequest = {
                        singleLauncher.launch(Manifest.permission.READ_CONTACTS)
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                Surface(
                    color = Color(0xFF141414),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF262626)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = PureWhite,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Aucune donnée n'est transmise sans votre accord. Toutes vos interactions restent strictement confidentielles.",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFAAAAAA),
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                    }
                }
            }
        },
        confirmButton = {
            val allGranted = hasAudio && hasCamera && hasContacts
            if (!allGranted) {
                Box {
                    Box(
                        modifier = Modifier
                            .offset(x = 2.dp, y = 2.dp)
                            .matchParentSize()
                            .clip(RoundedCornerShape(14.dp))
                            .background(PureWhite.copy(alpha = 0.35f))
                    )
                    Button(
                        onClick = {
                            val toRequest = mutableListOf<String>()
                            if (!hasAudio) toRequest.add(Manifest.permission.RECORD_AUDIO)
                            if (!hasCamera) toRequest.add(Manifest.permission.CAMERA)
                            if (!hasContacts) toRequest.add(Manifest.permission.READ_CONTACTS)
                            multiplePermissionsLauncher.launch(toRequest.toTypedArray())
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PureWhite,
                            contentColor = PureBlack
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.testTag("grant_all_permissions_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.VerifiedUser,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = PureBlack
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "TOUT AUTORISER",
                            fontFamily = KablammoFontFamily,
                            fontSize = 13.sp,
                            color = PureBlack
                        )
                    }
                }
            } else {
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PureWhite,
                        contentColor = PureBlack
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "TERMINÉ",
                        fontFamily = KablammoFontFamily,
                        fontSize = 13.sp
                    )
                }
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = PureWhite
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF444444))
            ) {
                Text(
                    text = "Fermer",
                    fontSize = 13.sp,
                    color = Color(0xFFDDDDDD)
                )
            }
        }
    )
}

@Composable
private fun PermissionRowCard(
    title: String,
    description: String,
    icon: ImageVector,
    accentColor: Color,
    isGranted: Boolean,
    onRequest: () -> Unit,
    isPhotoPicker: Boolean = false
) {
    Surface(
        color = Color(0xFF111111),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isGranted) PureWhite.copy(alpha = 0.5f) else Color(0xFF2B2B2B)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF222222))
                    .border(1.5.dp, accentColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.5.sp,
                    color = PureWhite
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 11.sp,
                    color = Color(0xFFAAAAAA),
                    lineHeight = 14.sp
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            if (isGranted) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(PureWhite.copy(alpha = 0.12f))
                        .border(1.dp, PureWhite.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = PureWhite,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isPhotoPicker) "Inclus" else "Actif",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = PureWhite
                    )
                }
            } else {
                Button(
                    onClick = onRequest,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PureWhite,
                        contentColor = PureBlack
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text(
                        text = "Autoriser",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = PureBlack
                    )
                }
            }
        }
    }
}
