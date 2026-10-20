package com.nabdaldhikr.app.ui.screens

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.nabdaldhikr.app.ui.theme.*
import com.nabdaldhikr.app.viewmodel.SettingsViewModel
import com.nabdaldhikr.app.viewmodel.TasbihViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasbihScreen(
    navController: NavController,
    viewModel: TasbihViewModel,
    settingsViewModel: SettingsViewModel
) {
    val dhikrList by viewModel.dhikrList.collectAsState()
    val selectedIndex by viewModel.selectedIndex.collectAsState()
    val vibrationEnabled by settingsViewModel.vibrationEnabled.collectAsState()

    val currentDhikr = if (dhikrList.isNotEmpty()) {
        dhikrList[selectedIndex.coerceIn(0, maxOf(0, dhikrList.lastIndex))]
    } else {
        com.nabdaldhikr.app.viewmodel.TasbihDhikr("default", "سبحان الله", 33, 0)
    }
    
    val totalCount = dhikrList.sumOf { it.count }
    val view = LocalView.current
    val coroutineScope = rememberCoroutineScope()

    var showCustomDialog by remember { mutableStateOf(false) }
    var dhikrText by remember { mutableStateOf("") }
    var dhikrTarget by remember { mutableStateOf("33") }

    // انضغاط نابض للقرص عند التسبيح
    var dialScale by remember { mutableFloatStateOf(1f) }
    val animatedDialScale by animateFloatAsState(
        targetValue = dialScale,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "TasbihDialScale"
    )

    val handleIncrement = {
        dialScale = 1.05f
        viewModel.increment()
        if (vibrationEnabled) {
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        }
        coroutineScope.launch {
            delay(100)
            dialScale = 1f
        }
    }

    Scaffold(
        containerColor = ScreenBg,
        topBar = {
            TopAppBar(
                // يمينًا سهم رجوع ثم "المسبحة الإلكترونية" أبيض bold
                title = { 
                    Text(
                        text = "المسبحة الإلكترونية", 
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
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
                // ويسارًا بصريًا: أيقونة + عند أقصى اليسار ثم أيقونة Refresh
                actions = {
                    IconButton(onClick = { viewModel.reset() }) {
                        Icon(
                            imageVector = Icons.Filled.Refresh, 
                            contentDescription = "تصفير الذكر الحالي",
                            tint = TextSecondary
                        )
                    }
                    IconButton(onClick = { showCustomDialog = true }) {
                        Icon(
                            imageVector = Icons.Filled.Add, 
                            contentDescription = "إضافة ذكر مخصص",
                            tint = PrimaryGold
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
                    handleIncrement()
                },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(0.8f))

            // الذكر الحالي ذهبي 28sp بتوسيط في الثلث العلوي
            Text(
                text = currentDhikr.text,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = PrimaryGold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp)
            )

            Spacer(modifier = Modifier.height(36.dp))

            // دائرة ~230dp بخلفية #17452F بداخلها العداد أبيض 64sp bold وتحته "/ 33" ذهبي 20sp
            // وحول الدائرة حلقة تقدم ذهبية سماكة ~8dp تبدأ من الساعة 12 وتتملى clockwise بنسبة العداد/الهدف
            val progressRatio = if (currentDhikr.target > 0) {
                (currentDhikr.count.toFloat() / currentDhikr.target.toFloat()).coerceIn(0f, 1f)
            } else 0f

            val animatedProgressRatio by animateFloatAsState(
                targetValue = progressRatio,
                animationSpec = tween(300, easing = FastOutSlowInEasing),
                label = "TasbihArcAnimation"
            )

            Box(
                modifier = Modifier
                    .size(234.dp)
                    .graphicsLayer {
                        scaleX = animatedDialScale
                        scaleY = animatedDialScale
                    },
                contentAlignment = Alignment.Center
            ) {
                // حلقة التقدم الدائرية حول الدائرة
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeW = 8.dp.toPx()
                    val arcStroke = Stroke(width = strokeW, cap = StrokeCap.Round)
                    val arcRadius = (size.minDimension - strokeW) / 2f
                    val arcTopLeft = androidx.compose.ui.geometry.Offset(
                        (size.width - arcRadius * 2) / 2f,
                        (size.height - arcRadius * 2) / 2f
                    )
                    val arcSize = androidx.compose.ui.geometry.Size(arcRadius * 2, arcRadius * 2)

                    // مسار الحلقة الفارغ
                    drawArc(
                        color = ProgressTrack,
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = arcTopLeft,
                        size = arcSize,
                        style = arcStroke
                    )

                    // التعبئة الذهبية تبدأ من الساعة 12 (-90 درجة) في اتجاه عقارب الساعة
                    if (animatedProgressRatio > 0f) {
                        drawArc(
                            color = if (progressRatio >= 1f) CompletedGreen else PrimaryGold,
                            startAngle = -90f,
                            sweepAngle = animatedProgressRatio * 360f,
                            useCenter = false,
                            topLeft = arcTopLeft,
                            size = arcSize,
                            style = arcStroke
                        )
                    }
                }

                // الدائرة الداخلية بخلفية #17452F وتدرج زمردي فاخر
                Surface(
                    shape = CircleShape,
                    color = CardBg,
                    border = BorderStroke(1.dp, PrimaryGold.copy(alpha = 0.25f)),
                    modifier = Modifier
                        .size(208.dp)
                        .shadow(16.dp, CircleShape, spotColor = PrimaryGold)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(CardGradient),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            // العداد أبيض 64sp bold
                            Text(
                                text = currentDhikr.count.toString(),
                                fontSize = 64.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            // تحته "/ 33" ذهبي 20sp
                            Text(
                                text = "/ ${currentDhikr.target}",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (progressRatio >= 1f) CompletedGreen else PrimaryGold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // سطر "المجموع الكلي: X" بتوسيط
            Text(
                text = "المجموع الكلي: $totalCount",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(16.dp))

            // صف Chips أفقي قابل للسكرول باستدارة 12dp
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(dhikrList, key = { _, item -> item.id }) { index, item ->
                    val isSelected = selectedIndex == index
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) PrimaryGold else Color.Transparent,
                        border = if (isSelected) null else BorderStroke(1.dp, ProgressTrack),
                        modifier = Modifier.clickable { viewModel.selectDhikr(index) }
                    ) {
                        Text(
                            text = item.text,
                            color = if (isSelected) ScreenBg else TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Dialog "ذكر مخصص": خلفية #2A2A2E باستدارة 24dp
    if (showCustomDialog) {
        AlertDialog(
            onDismissRequest = { showCustomDialog = false },
            containerColor = DialogBg,
            shape = RoundedCornerShape(24.dp),
            title = {
                Text(
                    text = "ذكر مخصص",
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // حقل بإطار رفيع hint "نص الذكر" باستدارة 8dp
                    OutlinedTextField(
                        value = dhikrText,
                        onValueChange = { dhikrText = it },
                        placeholder = { Text("نص الذكر", color = TextSecondary) },
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryGold,
                            unfocusedBorderColor = ProgressTrack,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    )

                    // حقل رقمي بإطار رفيع بعنوان على الحدود "عدد العدات" بقيمة افتراضية 33
                    OutlinedTextField(
                        value = dhikrTarget,
                        onValueChange = { dhikrTarget = it.filter { c -> c.isDigit() } },
                        label = { Text("عدد العدات", color = TextSecondary) },
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryGold,
                            unfocusedBorderColor = ProgressTrack,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                // أسفل اليسار زرا نص ذهبي: "إضافة" و"إلغاء"
                TextButton(onClick = {
                    val countValue = dhikrTarget.toIntOrNull() ?: 33
                    if (dhikrText.isNotBlank()) {
                        viewModel.addCustomDhikr(dhikrText, countValue)
                        showCustomDialog = false
                        dhikrText = ""
                        dhikrTarget = "33"
                    }
                }) {
                    Text("إضافة", color = PrimaryGold, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomDialog = false }) {
                    Text("إلغاء", color = PrimaryGold)
                }
            }
        )
    }
}