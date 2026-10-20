package com.nabdaldhikr.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nabdaldhikr.app.ui.navigation.AppNavigation
import com.nabdaldhikr.app.ui.theme.NabdAlDhikrTheme
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
            
            // فرض اتجاه النص من اليمين لليسار (عربي) وتطبيق مقياس الخط الشامل على كافة الشاشات
            CompositionLocalProvider(
                LocalLayoutDirection provides LayoutDirection.Rtl,
                LocalDensity provides scaledDensity
            ) {
                NabdAlDhikrTheme(isDarkTheme = isDarkMode) {
                    AppNavigation(settingsViewModel = settingsViewModel)
                }
            }
        }
    }
}
