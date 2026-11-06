package com.example.applock.ui

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.example.applock.data.AppLockPreferences
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

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val lockedSet = prefs.getLockedPackages()
            val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
                .filter { (it.flags and ApplicationInfo.FLAG_SYSTEM) == 0 } // Filter to user apps
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
            TopAppBar(title = { Text("App Locker Protection") })
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
}
