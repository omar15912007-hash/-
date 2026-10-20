package com.nabdaldhikr.app.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// =====================================================
// ألوان نظام التصميم المعتمد من المرجع
// =====================================================
val ScreenBg = Color(0xFF0A1F19)        // خلفية الشاشات
val HeaderBg = Color(0xFF12432E)        // خلفية AppBar والهيدر
val CardBg = Color(0xFF17452F)          // خلفية الكروت
val CardBgElevated = Color(0xFF1B4E36)  // خلفية الكروت المرتفعة
val PrimaryGold = Color(0xFFC9A24B)      // الذهبي الأساسي
val CompletedGreen = Color(0xFF4CAF50)   // أخضر الإنجاز
val TextPrimary = Color(0xFFFFFFFF)      // النص الأساسي
val TextSecondary = Color(0xFFB0B7B3)    // النص الثانوي
val ProgressTrack = Color(0xFF2E5B4A)    // مسار شريط التقدم الفارغ
val DialogBg = Color(0xFF2A2A2E)         // خلفية الـ Dialog
val CardBorder = Color(0xFF235841)       // إطار الكروت
val GoldGlow = Color(0x33C9A24B)         // توهج ذهبي خفيف

// تدرجات لونية عصرية
val ScreenGradient = Brush.verticalGradient(
    listOf(Color(0xFF071713), Color(0xFF0C241E), Color(0xFF071713))
)
val CardGradient = Brush.verticalGradient(
    listOf(Color(0xFF194C35), Color(0xFF143E2C))
)
val GoldGradient = Brush.horizontalGradient(
    listOf(Color(0xFFD4AF37), Color(0xFFC9A24B), Color(0xFFE8D5A0))
)
val CompletedGradient = Brush.horizontalGradient(
    listOf(Color(0xFF388E3C), Color(0xFF4CAF50), Color(0xFF66BB6A))
)


