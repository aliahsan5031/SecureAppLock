package com.example.applock.ui

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.example.applock.data.AppLockPreferences
import com.example.applock.data.LockType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class AppItem(
    val name: String,
    val packageName: String,
    val icon: android.graphics.drawable.Drawable,
    val isLocked: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppListScreen(prefs: AppLockPreferences) {
    val context = LocalContext.current
    val pm = context.packageManager
    var appList by remember { mutableStateOf<List<AppItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val lockedSet = prefs.getLockedPackages()
            val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
                .filter { (it.flags and ApplicationInfo.FLAG_SYSTEM) == 0 }
                .map { appInfo ->
                    AppItem(
                        name = appInfo.loadLabel(pm).toString(),
                        packageName = appInfo.packageName,
                        icon = appInfo.loadIcon(pm),
                        isLocked = lockedSet.contains(appInfo.packageName)
                    )
                }.sortedBy { it.name }
            appList = apps
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("App Locker Protection") },
                actions = {
                    IconButton(onClick = { showSettingsDialog = true }) {
                        Icon(imageVector = Icons.Default.Settings, contentDescription = "Security Settings")
                    }
                }
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(modifier = Modifier.padding(padding)) {
                items(appList, key = { it.packageName }) { app ->
                    var isLocked by remember { mutableStateOf(app.isLocked) }
                    ListItem(
                        leadingContent = {
                            Image(
                                bitmap = app.icon.toBitmap(96, 96).asImageBitmap(),
                                contentDescription = null,
                                modifier = Modifier.size(40.dp)
                            )
                        },
                        headlineContent = { Text(app.name) },
                        supportingContent = { Text(app.packageName, style = MaterialTheme.typography.bodySmall) },
                        trailingContent = {
                            Switch(
                                checked = isLocked,
                                onCheckedChange = { checked ->
                                    isLocked = checked
                                    prefs.toggleAppLock(app.packageName, checked)
                                }
                            )
                        }
                    )
                    HorizontalDivider()
                }
            }
        }
    }

    if (showSettingsDialog) {
        LockSettingsDialog(
            prefs = prefs,
            onDismiss = { showSettingsDialog = false }
        )
    }
}

@Composable
fun LockSettingsDialog(
    prefs: AppLockPreferences,
    onDismiss: () -> Unit
) {
    var selectedType by remember { mutableStateOf(prefs.getLockType()) }
    var biometricEnabled by remember { mutableStateOf(prefs.isBiometricEnabled()) }
    var newPin by remember { mutableStateOf(prefs.getPin()) }
    var newPassword by remember { mutableStateOf(prefs.getPassword()) }
    var passwordVisible by remember { mutableStateOf(false) }
    var recordedPattern by remember { mutableStateOf(prefs.getPattern()) }
    var patternStatusMessage by remember { mutableStateOf("Draw pattern to register") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Security & Lock Types") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                // Biometric toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Fingerprint, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text("Fingerprint / Face Unlock")
                    }
                    Switch(
                        checked = biometricEnabled,
                        onCheckedChange = { biometricEnabled = it }
                    )
                }

                HorizontalDivider()

                Text("Fallback Lock Type:", style = MaterialTheme.typography.titleSmall)

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    FilterChip(
                        selected = selectedType == LockType.PIN,
                        onClick = { selectedType = LockType.PIN },
                        label = { Text("PIN") }
                    )
                    FilterChip(
                        selected = selectedType == LockType.PASSWORD,
                        onClick = { selectedType = LockType.PASSWORD },
                        label = { Text("Password") }
                    )
                    FilterChip(
                        selected = selectedType == LockType.PATTERN,
                        onClick = { selectedType = LockType.PATTERN },
                        label = { Text("Pattern") }
                    )
                }

                HorizontalDivider()

                when (selectedType) {
                    LockType.PIN -> {
                        Text("Change PIN (4 to 6 digits):", style = MaterialTheme.typography.bodyMedium)
                        OutlinedTextField(
                            value = newPin,
                            onValueChange = { input ->
                                if (input.length <= 6 && input.all { it.isDigit() }) {
                                    newPin = input
                                }
                            },
                            label = { Text("PIN") },
                            singleLine = true
                        )
                    }
                    LockType.PASSWORD -> {
                        Text("Change Alphanumeric Password:", style = MaterialTheme.typography.bodyMedium)
                        OutlinedTextField(
                            value = newPassword,
                            onValueChange = { newPassword = it },
                            label = { Text("Alphanumeric Password") },
                            singleLine = true,
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                val image = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(imageVector = image, contentDescription = null)
                                }
                            }
                        )
                    }
                    LockType.PATTERN -> {
                        Text("Set Pattern:", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = patternStatusMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            PatternLockView(
                                expectedPattern = "",
                                isSettingMode = true,
                                onPatternDrawn = { pattern ->
                                    if (pattern.split(",").size >= 4) {
                                        recordedPattern = pattern
                                        patternStatusMessage = "Pattern recorded successfully!"
                                    } else {
                                        patternStatusMessage = "Connect at least 4 dots!"
                                    }
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    prefs.setBiometricEnabled(biometricEnabled)
                    prefs.setLockType(selectedType)
                    if (selectedType == LockType.PIN && newPin.length >= 4) {
                        prefs.setPin(newPin)
                    } else if (selectedType == LockType.PASSWORD && newPassword.isNotEmpty()) {
                        prefs.setPassword(newPassword)
                    } else if (selectedType == LockType.PATTERN && recordedPattern.isNotEmpty()) {
                        prefs.setPattern(recordedPattern)
                    }
                    onDismiss()
                }
            ) {
                Text("Save Changes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
