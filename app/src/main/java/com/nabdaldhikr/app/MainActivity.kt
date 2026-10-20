package com.nabdaldhikr.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.viewmodel.compose.viewModel
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
            val settingsViewModel: SettingsViewModel = viewModel()
            val isDarkMode by settingsViewModel.isDarkMode.collectAsState()
            val fontScale by settingsViewModel.fontScale.collectAsState()
            val currentDensity = LocalDensity.current
            val scaledDensity = remember(currentDensity, fontScale) {
                Density(density = currentDensity.density, fontScale = currentDensity.fontScale * fontScale)
            }

            var updateInfo by remember { mutableStateOf<UpdateInfo?>(null) }

            // Automatic In-App Update Check on App Launch
            LaunchedEffect(Unit) {
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
