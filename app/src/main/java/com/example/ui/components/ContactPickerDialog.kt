package com.example.ui.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.ui.theme.KablammoFontFamily
import com.example.ui.theme.NewSchoolLime
import com.example.ui.theme.PureBlack
import com.example.ui.theme.PureWhite

data class DeviceContactItem(
    val id: String,
    val name: String,
    val phoneNumber: String,
    val initials: String
)

@Composable
fun ContactPickerDialog(
    onDismiss: () -> Unit,
    onContactSelected: (name: String, phoneNumber: String) -> Unit
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var contactsList by remember { mutableStateOf<List<DeviceContactItem>>(emptyList()) }
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            hasPermission = granted
            if (granted) {
                contactsList = queryDeviceContacts(context)
            }
        }
    )

    LaunchedEffect(hasPermission) {
        if (hasPermission) {
            contactsList = queryDeviceContacts(context)
        }
    }

    val filteredContacts = remember(contactsList, searchQuery) {
        if (searchQuery.isBlank()) {
            contactsList
        } else {
            contactsList.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                it.phoneNumber.contains(searchQuery)
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.85f)
            .testTag("contact_picker_dialog"),
        containerColor = PureBlack,
        shape = RoundedCornerShape(20.dp),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(PureWhite),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Contacts,
                            contentDescription = null,
                            tint = PureBlack,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "PARTAGER UN CONTACT",
                            fontFamily = KablammoFontFamily,
                            fontSize = 16.sp,
                            color = PureWhite
                        )
                        Text(
                            text = "Contact kris&her",
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
            Column(modifier = Modifier.fillMaxSize()) {
                if (!hasPermission) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "L'accès au carnet d'adresses permet de sélectionner et partager un contact directement dans la conversation.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFCCCCCC),
                            fontSize = 12.5.sp,
                            lineHeight = 17.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { permissionLauncher.launch(Manifest.permission.READ_CONTACTS) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PureWhite,
                                contentColor = PureBlack
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Contacts,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "AUTORISER LES CONTACTS",
                                fontFamily = KablammoFontFamily,
                                fontSize = 12.sp
                            )
                        }
                    }
                } else {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Rechercher un contact...", color = Color(0xFF777777)) },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = PureWhite)
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF141414),
                            unfocusedContainerColor = Color(0xFF141414),
                            focusedTextColor = PureWhite,
                            unfocusedTextColor = PureWhite,
                            focusedBorderColor = PureWhite,
                            unfocusedBorderColor = Color(0xFF333333)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp)
                    )

                    if (filteredContacts.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Aucun contact trouvé",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF888888)
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(filteredContacts, key = { it.id }) { contact ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFF121212),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF262626)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            onContactSelected(contact.name, contact.phoneNumber)
                                            onDismiss()
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(PureWhite),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = contact.initials,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = PureBlack
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = contact.name,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.5.sp,
                                                color = PureWhite
                                            )
                                            Text(
                                                text = contact.phoneNumber,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color(0xFFAAAAAA),
                                                fontSize = 11.sp
                                            )
                                        }
                                        Icon(
                                            imageVector = Icons.Default.Phone,
                                            contentDescription = null,
                                            tint = NewSchoolLime,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {}
    )
}

fun queryDeviceContacts(context: Context): List<DeviceContactItem> {
    val list = mutableListOf<DeviceContactItem>()
    try {
        val cursor = context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER
            ),
            null,
            null,
            "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"
        )
        cursor?.use {
            val idCol = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
            val nameCol = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numCol = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            while (it.moveToNext()) {
                val id = if (idCol >= 0) it.getString(idCol) ?: "" else ""
                val name = if (nameCol >= 0) it.getString(nameCol) ?: "Contact" else "Contact"
                val num = if (numCol >= 0) it.getString(numCol) ?: "" else ""
                val initials = name.split(" ")
                    .filter { s -> s.isNotBlank() }
                    .take(2)
                    .map { s -> s.first().uppercase() }
                    .joinToString("")
                    .ifEmpty { "C" }
                if (num.isNotBlank() && list.none { c -> c.phoneNumber == num }) {
                    list.add(DeviceContactItem(id.ifBlank { num }, name, num, initials))
                }
            }
        }
    } catch (_: Exception) {}

    // If device / emulator has no contacts saved yet, provide built-in contacts
    if (list.isEmpty()) {
        list.add(DeviceContactItem("1", "Kris Lester", "+33 6 12 34 56 78", "KL"))
        list.add(DeviceContactItem("2", "Her", "+33 7 98 76 54 32", "HE"))
        list.add(DeviceContactItem("3", "Contact d'urgence kris&her", "+33 1 40 50 60 70", "CU"))
    }
    return list
}
