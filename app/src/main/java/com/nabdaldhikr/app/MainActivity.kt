package com.nabdaldhikr.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nabdaldhikr.app.reminder.ReminderScheduler
import com.nabdaldhikr.app.ui.navigation.AppNavigation
import com.nabdaldhikr.app.ui.theme.NabdAlDhikrTheme
import com.nabdaldhikr.app.updater.AppUpdateDialog
import com.nabdaldhikr.app.updater.AppUpdateManager
import com.nabdaldhikr.app.updater.UpdateInfo
import com.nabdaldhikr.app.updater.UpdateState
import com.nabdaldhikr.app.viewmodel.SettingsViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(
                android.graphics.Color.parseColor("#12432E")
            )
        )
        
        setContent {
            val context = LocalContext.current
            val settingsViewModel: SettingsViewModel = viewModel()
            val isDarkMode by settingsViewModel.isDarkMode.collectAsState()
            val fontScale by settingsViewModel.fontScale.collectAsState()

            val currentDensity = LocalDensity.current
            val scaledDensity = remember(currentDensity, fontScale) {
                Density(density = currentDensity.density, fontScale = currentDensity.fontScale * fontScale)
            }

            var updateInfo by remember { mutableStateOf<UpdateInfo?>(null) }

            // Notification permission request for Android 13+ (POST_NOTIFICATIONS)
            val notificationLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission()
            ) { isGranted ->
                if (isGranted) {
                    ReminderScheduler.schedule(context, true)
                }
            }

            LaunchedEffect(Unit) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val hasPerm = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED
                    if (!hasPerm) {
                        notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            }

            // Automatic In-App Update Check on App Launch (graceful background check)
            LaunchedEffect(Unit) {
                kotlinx.coroutines.delay(1500)
                val state = AppUpdateManager.checkForUpdate()
                if (state is UpdateState.UpdateAvailable) {
                    updateInfo = state.info
                }
            }
            
            // فرض اتجاه النص من اليمين لليسار (عربي) وتطبيق مقياس الخط الشامل على كافة الشاشات
            CompositionLocalProvider(
                LocalLayoutDirection provides LayoutDirection.Rtl,
                LocalDensity provides scaledDensity
            ) {
                NabdAlDhikrTheme(isDarkTheme = isDarkMode) {
                    AppNavigation(settingsViewModel = settingsViewModel)

                    // Render Update Dialog if update is found
                    updateInfo?.let { info ->
                        AppUpdateDialog(
                            updateInfo = info,
                            onDismiss = { updateInfo = null }
                        )
                    }
                }
            }
        }
    }
}
