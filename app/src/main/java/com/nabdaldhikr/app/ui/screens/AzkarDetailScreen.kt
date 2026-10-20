package com.nabdaldhikr.app.ui.screens

import android.content.Intent
import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.nabdaldhikr.app.ui.components.GoldDivider
import com.nabdaldhikr.app.ui.theme.*
import com.nabdaldhikr.app.viewmodel.AzkarViewModel
import com.nabdaldhikr.app.viewmodel.SettingsViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AzkarDetailScreen(
    categoryId: String,
    initialIndex: Int,
    navController: NavController,
    azkarViewModel: AzkarViewModel,
    settingsViewModel: SettingsViewModel
) {
    val category = azkarViewModel.getCategory(categoryId) ?: return
    val currentIndex = initialIndex.coerceIn(0, category.azkarList.lastIndex)
    val currentAzkar = category.azkarList[currentIndex]

    val counters by azkarViewModel.counters.collectAsState()
    val progress = azkarViewModel.getCategoryProgress(categoryId, counters)
    val remaining = azkarViewModel.getRemainingCount(categoryId, currentAzkar.id, counters)
    val isCompleted = remaining == 0
    val isFavorite = azkarViewModel.isFavorite(categoryId, currentAzkar.id)

    val vibrationEnabled by settingsViewModel.vibrationEnabled.collectAsState()
    val context = LocalContext.current
    val view = LocalView.current
    val coroutineScope = rememberCoroutineScope()

    // تأثير النبض عند النقر
    var buttonScale by remember { mutableFloatStateOf(1f) }
    var isNavigating by remember { mutableStateOf(false) }
    val animatedButtonScale by animateFloatAsState(
        targetValue = buttonScale,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "ButtonScale"
    )

    // دالة النقر لاحتساب التكرار والعودة عند الاكتمال
    val handleTap: () -> Unit = {
        if (!isNavigating && !isCompleted) {
            buttonScale = 1.12f
            azkarViewModel.incrementCount(categoryId, currentAzkar.id)
            if (vibrationEnabled) {
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            }
            if (remaining - 1 <= 0) {
                isNavigating = true
                coroutineScope.launch {
                    delay(350)
                    navController.popBackStack()
                }
            } else {
                coroutineScope.launch {
                    delay(100)
                    buttonScale = 1f
                }
            }
        } else if (isCompleted && !isNavigating) {
            isNavigating = true
            navController.popBackStack()
        }
    }

    Scaffold(
        containerColor = ScreenBg,
        topBar = {
            TopAppBar(
                // يمينًا سهم الرجوع ثم العداد "منجز / كلي" ذهبي
                title = { 
                    Text(
                        text = "${progress.first} / ${progress.second}", 
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (progress.first == progress.second && progress.second > 0) CompletedGreen else PrimaryGold
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack, 
                            contentDescription = "رجوع",
                            tint = TextPrimary
                        )
                    }
                },
                // ويسارًا بصريًا: أيقونة مشاركة ثم أيقونة قلب (تضيف/تشيل من المفضلة)
                actions = {
                    IconButton(onClick = {
                        val shareIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, "${currentAzkar.text}\n\n[المصدر: ${currentAzkar.source}]")
                            type = "text/plain"
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "مشاركة الذكر عبر"))
                    }) {
                        Icon(
                            imageVector = Icons.Filled.Share, 
                            contentDescription = "مشاركة",
                            tint = TextSecondary
                        )
                    }
                    IconButton(onClick = {
                        azkarViewModel.toggleFavorite(categoryId, currentAzkar.id)
                    }) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = "مفضلة",
                            tint = if (isFavorite) PrimaryGold else TextSecondary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = HeaderBg
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(ScreenGradient)
                .padding(padding)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    handleTap()
                }
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            
            // نص الذكر كاملًا 24sp أبيض بتوسيط بارتفاع سطر 1.9
            Text(
                text = currentAzkar.text,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                lineHeight = 45.6.sp,
                color = TextPrimary,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // فاصل أفقي رفيع بتدرج ذهبي يتلاشى عند الطرفين
            GoldDivider()
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // كارت #17452F باستدارة 16dp بداخله "الفضل:" ذهبي bold ثم نص الفضل أبيض (يظهر فقط إن وجد فضل)
            if (currentAzkar.virtue.isNotBlank()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CardBg),
                    border = BorderStroke(1.dp, PrimaryGold.copy(alpha = 0.2f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(CardGradient)
                            .padding(16.dp)
                    ) {
                        Column {
                            Text(
                                text = "الفضل:",
                                fontWeight = FontWeight.Bold,
                                color = PrimaryGold,
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = currentAzkar.virtue,
                                fontSize = 15.sp,
                                color = TextPrimary,
                                lineHeight = 24.sp
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            // سطر "المصدر: ..." ذهبي 12sp بتوسيط
            Text(
                text = "المصدر: ${currentAzkar.source}",
                fontSize = 12.sp,
                color = PrimaryGold,
                textAlign = TextAlign.Center
            )

            if (currentAzkar.hadithNumber.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "رقم الحديث: ${currentAzkar.hadithNumber}",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(36.dp))

            // زر دائري أخضر/ذهبي ~120dp مع تأثير النبض والحلقة التفاعلية
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(130.dp)
                    .graphicsLayer {
                        scaleX = animatedButtonScale
                        scaleY = animatedButtonScale
                    }
            ) {
                // حلقة التقدم التفاعلية
                val progressFraction = if (currentAzkar.repetition > 0) {
                    (currentAzkar.repetition - remaining).toFloat() / currentAzkar.repetition.toFloat()
                } else 1f

                Canvas(modifier = Modifier.size(130.dp)) {
                    val arcStroke = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                    drawArc(
                        color = ProgressTrack,
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = arcStroke
                    )
                    drawArc(
                        color = if (isCompleted) CompletedGreen else PrimaryGold,
                        startAngle = -90f,
                        sweepAngle = 360f * progressFraction,
                        useCenter = false,
                        style = arcStroke
                    )
                }

                Surface(
                    shape = CircleShape,
                    color = if (isCompleted) CompletedGreen else PrimaryGold,
                    modifier = Modifier
                        .size(116.dp)
                        .shadow(12.dp, CircleShape, spotColor = if (isCompleted) CompletedGreen else PrimaryGold)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (isCompleted) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = "مكتمل",
                                tint = ScreenBg,
                                modifier = Modifier.size(52.dp)
                            )
                        } else {
                            Text(
                                text = remaining.toString(),
                                fontSize = 44.sp,
                                fontWeight = FontWeight.Bold,
                                color = ScreenBg
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // تحته رمادي "انقر في أي مكان للمتابعة"
            Text(
                text = if (isCompleted) "تقبل الله طاعتك ✓" else "انقر في أي مكان لاحتساب الذكر",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = if (isCompleted) CompletedGreen else TextSecondary
            )
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
