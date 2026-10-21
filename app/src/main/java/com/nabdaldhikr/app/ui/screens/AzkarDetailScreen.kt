package com.nabdaldhikr.app.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.view.HapticFeedbackConstants
import android.widget.Toast
import androidx.compose.animation.*
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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
    var currentIndex by remember {
        mutableIntStateOf(initialIndex.coerceIn(0, category.azkarList.lastIndex))
    }
    val currentAzkar = category.azkarList[currentIndex]

    val counters by azkarViewModel.counters.collectAsState()
    val remaining = azkarViewModel.getRemainingCount(categoryId, currentAzkar.id, counters)
    val isCompleted = remaining == 0
    val isFavorite = azkarViewModel.isFavorite(categoryId, currentAzkar.id)

    val vibrationEnabled by settingsViewModel.vibrationEnabled.collectAsState()
    val context = LocalContext.current
    val view = LocalView.current
    val coroutineScope = rememberCoroutineScope()

    // تحكم سريع بحجم الخط المباشر
    var fontScaleDelta by remember { mutableFloatStateOf(0f) }
    var showCompletionDialog by remember { mutableStateOf(false) }

    // تأثير النبض للزر
    var buttonScale by remember { mutableFloatStateOf(1f) }
    val animatedButtonScale by animateFloatAsState(
        targetValue = buttonScale,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "ButtonScale"
    )

    // دالة الانتقال للذكر التالي
    val moveToNext: () -> Unit = {
        if (currentIndex < category.azkarList.lastIndex) {
            currentIndex++
        } else {
            showCompletionDialog = true
        }
    }

    // دالة الانتقال للذكر السابق
    val moveToPrevious: () -> Unit = {
        if (currentIndex > 0) {
            currentIndex--
        }
    }

    // دالة النقر لاحتساب التكرار مع ميزة الانتقال التلقائي الذكي
    val handleTap: () -> Unit = {
        if (!isCompleted) {
            buttonScale = 1.15f
            azkarViewModel.incrementCount(categoryId, currentAzkar.id)
            if (vibrationEnabled) {
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            }

            if (remaining - 1 <= 0) {
                coroutineScope.launch {
                    delay(380)
                    buttonScale = 1f
                    moveToNext()
                }
            } else {
                coroutineScope.launch {
                    delay(120)
                    buttonScale = 1f
                }
            }
        } else {
            moveToNext()
        }
    }

    Scaffold(
        containerColor = ScreenBg,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "${currentIndex + 1} / ${category.azkarList.size}",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryGold
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
                actions = {
                    // زر تكبير الخط A+
                    IconButton(onClick = {
                        if (fontScaleDelta < 8f) fontScaleDelta += 2f
                    }) {
                        Text(
                            text = "A+",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (fontScaleDelta < 8f) PrimaryGold else TextSecondary.copy(alpha = 0.5f)
                        )
                    }

                    // زر تصغير الخط A-
                    IconButton(onClick = {
                        if (fontScaleDelta > -4f) fontScaleDelta -= 2f
                    }) {
                        Text(
                            text = "A-",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (fontScaleDelta > -4f) PrimaryGold else TextSecondary.copy(alpha = 0.5f)
                        )
                    }

                    // زر النسخ الفوري للحافظة
                    IconButton(onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText(
                            "ذكر نبض الذكر",
                            "${currentAzkar.text}\n\n[المصدر: ${currentAzkar.source}]"
                        )
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "تم نسخ الذكر بنجاح ✨", Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(
                            imageVector = AppIcons.ContentCopy,
                            contentDescription = "نسخ",
                            tint = TextSecondary
                        )
                    }

                    // زر المشاركة
                    IconButton(onClick = {
                        val shareIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(
                                Intent.EXTRA_TEXT,
                                "✨ من تطبيق نبض الذكر ✨\n\n${currentAzkar.text}\n\n📖 المصدر: ${currentAzkar.source}\n${if (currentAzkar.virtue.isNotBlank()) "🌟 الفضل: ${currentAzkar.virtue}" else ""}"
                            )
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

                    // زر المفضلة
                    IconButton(onClick = {
                        azkarViewModel.toggleFavorite(categoryId, currentAzkar.id)
                    }) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = "المفضلة",
                            tint = if (isFavorite) PrimaryGold else TextSecondary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = HeaderBg
                )
            )
        },
        bottomBar = {
            // شريط تنقل سفلي بين الأذكار
            Surface(
                color = HeaderBg,
                border = BorderStroke(1.dp, CardBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // الزر السابق
                    TextButton(
                        onClick = moveToPrevious,
                        enabled = currentIndex > 0,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = PrimaryGold,
                            disabledContentColor = TextSecondary.copy(alpha = 0.3f)
                        )
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("السابق", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }

                    // مؤشر التقدم
                    Text(
                        text = "${currentIndex + 1} من ${category.azkarList.size}",
                        fontSize = 13.sp,
                        color = TextSecondary
                    )

                    // الزر التالي
                    TextButton(
                        onClick = moveToNext,
                        colors = ButtonDefaults.textButtonColors(contentColor = PrimaryGold)
                    ) {
                        Text(
                            text = if (currentIndex < category.azkarList.lastIndex) "التالي" else "إنهاء",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                }
            }
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
                .padding(horizontal = 20.dp, vertical = 14.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(10.dp))

            // نص الذكر مع حجم الخط التفاعلي
            Text(
                text = currentAzkar.text,
                fontSize = (24 + fontScaleDelta).sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                lineHeight = (44 + fontScaleDelta * 1.5).sp,
                color = TextPrimary,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(20.dp))

            // فاصل ذهبي
            GoldDivider()

            Spacer(modifier = Modifier.height(20.dp))

            // فضل الذكر
            if (currentAzkar.virtue.isNotBlank()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CardBg),
                    border = BorderStroke(1.dp, PrimaryGold.copy(alpha = 0.25f)),
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
                                text = "الفضل والأجر:",
                                fontWeight = FontWeight.Bold,
                                color = PrimaryGold,
                                fontSize = 15.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = currentAzkar.virtue,
                                fontSize = (14 + (fontScaleDelta * 0.5f)).sp,
                                color = TextPrimary,
                                lineHeight = 23.sp
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(18.dp))
            }

            // مصدر الحديث
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

            Spacer(modifier = Modifier.height(28.dp))

            // زر احتساب التكرار الدائري التفاعلي
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(136.dp)
                    .graphicsLayer {
                        scaleX = animatedButtonScale
                        scaleY = animatedButtonScale
                    }
            ) {
                val progressFraction = if (currentAzkar.repetition > 0) {
                    (currentAzkar.repetition - remaining).toFloat() / currentAzkar.repetition.toFloat()
                } else 1f

                Canvas(modifier = Modifier.size(136.dp)) {
                    val arcStroke = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round)
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
                        .size(118.dp)
                        .shadow(14.dp, CircleShape, spotColor = if (isCompleted) CompletedGreen else PrimaryGold)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (isCompleted) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = "مكتمل",
                                tint = ScreenBg,
                                modifier = Modifier.size(54.dp)
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

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = if (isCompleted) "تقبل الله طاعتكم ✨ (انقر للمتابعة)" else "انقر في أي مكان لاحتساب التكرار",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = if (isCompleted) CompletedGreen else TextSecondary
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // نافذة الاحتفال بإتمام الورد
    if (showCompletionDialog) {
        Dialog(
            onDismissRequest = { showCompletionDialog = false },
            properties = DialogProperties(dismissOnClickOutside = false)
        ) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = CardBg),
                border = BorderStroke(1.dp, PrimaryGold),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(HeaderBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "🎉", fontSize = 32.sp)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "الحمد لله رب العالمين",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryGold,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "أتممت جميع أذكار «${category.nameAr}» بنجاح.\nتقبل الله طاعتكم ورزقكم بركتها.",
                        fontSize = 14.sp,
                        color = TextPrimary,
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                azkarViewModel.resetCategory(categoryId)
                                currentIndex = 0
                                showCompletionDialog = false
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, PrimaryGold)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, tint = PrimaryGold, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("إعادة", color = PrimaryGold, fontSize = 13.sp)
                        }

                        Button(
                            onClick = {
                                showCompletionDialog = false
                                navController.popBackStack()
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryGold, contentColor = Color.Black)
                        ) {
                            Text("تم", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}
