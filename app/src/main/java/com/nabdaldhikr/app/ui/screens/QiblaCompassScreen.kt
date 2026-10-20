package com.nabdaldhikr.app.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nabdaldhikr.app.ui.theme.CardBg
import com.nabdaldhikr.app.ui.theme.CardBorder
import com.nabdaldhikr.app.ui.theme.CompletedGreen
import com.nabdaldhikr.app.ui.theme.HeaderBg
import com.nabdaldhikr.app.ui.theme.PrimaryGold
import com.nabdaldhikr.app.ui.theme.ScreenBg
import com.nabdaldhikr.app.ui.theme.ScreenGradient
import com.nabdaldhikr.app.ui.theme.TextPrimary
import com.nabdaldhikr.app.ui.theme.TextSecondary
import com.nabdaldhikr.app.viewmodel.QiblaUiState
import com.nabdaldhikr.app.viewmodel.QiblaViewModel
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

/**
 * شاشة محدد اتجاه القبلة الذكية (Qibla Compass Screen):
 * - مبنية بالكامل وفق Jetpack Compose (Material 3) مع دعم كامل للغة العربية (RTL).
 * - بوصلة إسلامية عصرية ثلاثية الأبعاد مع مؤشر الكعبة المشرفة وحسابات الشمال الحقيقي.
 * - تنعيم الحركة بمعدل 60 إطاراً في الثانية دون قفزات زاويّة.
 * - اهتزاز ذكي وتوهج زمردي فور تطابق الاتجاه مع القبلة (±3 درجات).
 * - معالجة كاملة لجميع حالات الأخطاء (نقص الصلاحيات، إيقاف GPS، حاجة البوصلة للمعايرة).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QiblaCompassScreen(
    onBackClick: () -> Unit,
    viewModel: QiblaViewModel = viewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val hapticFeedback = LocalHapticFeedback.current

    // إدارة دورة الحياة لإيقاف الحساسات عند مغادرة الشاشة
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> viewModel.onResume()
                Lifecycle.Event.ON_PAUSE -> viewModel.onPause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // مشغل طلب إذن الموقع في وقت التشغيل (Runtime Permissions)
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        viewModel.checkLocationPermissionAndFetch()
    }

    // اهتزاز ملموس (Haptic Vibration) يُطلق لمرة واحدة فقط عند التطابق الدقيق مع القبلة
    var wasAligned by remember { mutableStateOf(false) }
    LaunchedEffect(uiState.isAligned) {
        if (uiState.isAligned && !wasAligned) {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
            triggerPreciseVibration(context)
        }
        wasAligned = uiState.isAligned
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "محدد اتجاه القبلة",
                        color = TextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "رجوع",
                            tint = PrimaryGold
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.requestFreshLocation() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "تحديث الموقع",
                            tint = PrimaryGold
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = HeaderBg
                )
            )
        },
        containerColor = ScreenBg
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(ScreenGradient)
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                // 1. شريط تنبيه المعايرة (إذا كان الحساس غير دقيق)
                AnimatedVisibility(
                    visible = uiState.needsCalibration,
                    enter = slideInVertically() + fadeIn(),
                    exit = slideOutVertically() + fadeOut()
                ) {
                    CalibrationBanner()
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 2. معالجة حالات الأخطاء (صلاحية الموقع أو تعطيل الـ GPS)
                when {
                    !uiState.isLocationPermissionGranted -> {
                        LocationPermissionCard(
                            onRequestPermission = {
                                permissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                            }
                        )
                    }
                    !uiState.isGpsEnabled -> {
                        GpsDisabledCard(
                            onEnableGps = {
                                context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                            }
                        )
                    }
                    !uiState.isSensorAvailable -> {
                        SensorUnavailableCard()
                    }
                    else -> {
                        // 3. عرض البوصلة والبيانات
                        CompassMainContent(uiState = uiState)
                    }
                }
            }
        }
    }
}

/**
 * المحتوى الرئيسي للبوصلة وبطاقات المعلومات.
 */
@Composable
private fun CompassMainContent(uiState: QiblaUiState) {
    // تنعيم زاوية الإبرة وزاوية قرص البوصلة بمعدل 60 إطاراً في الثانية دون ارتداد
    val animatedNeedleAngle by animateFloatAsState(
        targetValue = uiState.continuousNeedleAngle,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "NeedleRotation"
    )

    val animatedCompassDialAngle by animateFloatAsState(
        targetValue = -uiState.continuousCompassAngle,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "CompassDialRotation"
    )

    // وميض وتوهج زمردي خفيف عند محاذاة القبلة
    val infiniteTransition = rememberInfiniteTransition(label = "AlignmentPulse")
    val pulseGlowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "GlowAlpha"
    )

    val borderGlowColor by animateColorAsState(
        targetValue = if (uiState.isAligned) CompletedGreen.copy(alpha = pulseGlowAlpha) else CardBorder,
        animationSpec = tween(300),
        label = "BorderColor"
    )

    Spacer(modifier = Modifier.height(12.dp))

    // شارة حالة التطابق (Alignment Badge)
    AlignmentBadge(isAligned = uiState.isAligned)

    Spacer(modifier = Modifier.height(20.dp))

    // قرص البوصلة المتكامل مع مؤشر الكعبة المشرفة
    Box(
        modifier = Modifier
            .size(310.dp)
            .shadow(
                elevation = if (uiState.isAligned) 20.dp else 8.dp,
                shape = CircleShape,
                ambientColor = if (uiState.isAligned) CompletedGreen else Color.Black,
                spotColor = if (uiState.isAligned) CompletedGreen else PrimaryGold
            )
            .clip(CircleShape)
            .border(
                width = if (uiState.isAligned) 3.5.dp else 2.dp,
                color = borderGlowColor,
                shape = CircleShape
            )
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF143B2B),
                        Color(0xFF0D281E),
                        Color(0xFF071812)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = size.minDimension / 2f

            // رسم خلفية قرص البوصلة الدائري المتحرك (تدور عكس اتجاه الهاتف لتظل ثابتة مع الشمال الحقيقي)
            rotate(degrees = animatedCompassDialAngle, pivot = center) {
                drawCompassDialTicks(center = center, radius = radius)
            }

            // رسم إبرة الكعبة المشرفة (تشير بدقة إلى اتجاه مكة المكرمة)
            rotate(degrees = animatedNeedleAngle, pivot = center) {
                drawKaabaNeedle(center = center, radius = radius, isAligned = uiState.isAligned)
            }

            // محور الارتكاز الذهبي المركزي
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFFFF1B8), PrimaryGold, Color(0xFF8A6C1C)),
                    center = center,
                    radius = 16f
                ),
                radius = 12f,
                center = center
            )
            drawCircle(
                color = if (uiState.isAligned) CompletedGreen else Color(0xFF071812),
                radius = 5f,
                center = center
            )
        }

        // مؤشر التحميل عند جلب الموقع لأول مرة
        if (uiState.isLoading) {
            CircularProgressIndicator(
                color = PrimaryGold,
                modifier = Modifier.size(48.dp),
                strokeWidth = 3.dp
            )
        }
    }

    Spacer(modifier = Modifier.height(28.dp))

    // بطاقة معلومات الزوايا والمسافة والموقع
    QiblaDetailsCard(uiState = uiState)
}

/**
 * شارة توضح دقة الاتجاه نحو الكعبة.
 */
@Composable
private fun AlignmentBadge(isAligned: Boolean) {
    val bgColor by animateColorAsState(
        targetValue = if (isAligned) CompletedGreen.copy(alpha = 0.18f) else Color(0x22FFFFFF),
        animationSpec = tween(300),
        label = "BadgeBg"
    )
    val textColor by animateColorAsState(
        targetValue = if (isAligned) CompletedGreen else TextSecondary,
        animationSpec = tween(300),
        label = "BadgeText"
    )

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bgColor)
            .border(
                width = 1.dp,
                color = if (isAligned) CompletedGreen.copy(alpha = 0.6f) else CardBorder,
                shape = RoundedCornerShape(20.dp)
            )
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = if (isAligned) Icons.Default.CheckCircle else Icons.Default.Info,
            contentDescription = null,
            tint = textColor,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = if (isAligned) "وجهة القبلة صحيحة ومضبوطة" else "وجّه الهاتف حتى يتحول المؤشر للأخضر",
            color = textColor,
            fontSize = 14.sp,
            fontWeight = if (isAligned) FontWeight.Bold else FontWeight.Medium
        )
    }
}

/**
 * رسم تدريجات قرص البوصلة والحروف الدالة على الاتجاهات الأربعة بالعربية:
 * - ش (شمال / 0°)
 * - ق (شرق / 90°)
 * - ج (جنوب / 180°)
 * - غ (غرب / 270°)
 */
private fun DrawScope.drawCompassDialTicks(center: Offset, radius: Float) {
    val textPaint = Paint().apply {
        isAntiAlias = true
        textSize = 34f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }

    val degreePaint = Paint().apply {
        isAntiAlias = true
        textSize = 20f
        color = android.graphics.Color.argb(160, 176, 183, 179)
        textAlign = Paint.Align.CENTER
    }

    // رسم الدائرة الداخلية الخفيفة
    drawCircle(
        color = Color(0x1AFFFFFF),
        radius = radius * 0.76f,
        center = center,
        style = Stroke(width = 1.5f)
    )

    // رسم علامات الـ 360 درجة
    for (angle in 0 until 360 step 2) {
        val angleRad = Math.toRadians(angle.toDouble())
        val isMajor = angle % 30 == 0
        val isMedium = angle % 10 == 0 && !isMajor

        val tickLength = when {
            isMajor -> radius * 0.11f
            isMedium -> radius * 0.07f
            else -> radius * 0.035f
        }

        val strokeWidth = when {
            isMajor -> 3f
            isMedium -> 2f
            else -> 1f
        }

        val tickColor = when {
            angle == 0 -> PrimaryGold
            isMajor -> Color(0xFFDCD6C8)
            isMedium -> Color(0x99B0B7B3)
            else -> Color(0x33B0B7B3)
        }

        val outerRadius = radius * 0.94f
        val innerRadius = outerRadius - tickLength

        val startX = (center.x + innerRadius * sin(angleRad)).toFloat()
        val startY = (center.y - innerRadius * cos(angleRad)).toFloat()
        val endX = (center.x + outerRadius * sin(angleRad)).toFloat()
        val endY = (center.y - outerRadius * cos(angleRad)).toFloat()

        drawLine(
            color = tickColor,
            start = Offset(startX, startY),
            end = Offset(endX, endY),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )

        // رسم أرقام الدرجات عند العلامات الرئيسية (كل 30 درجة عدا الاتجاهات الأصلية)
        if (isMajor && angle !in listOf(0, 90, 180, 270)) {
            val labelRadius = radius * 0.69f
            val labelX = (center.x + labelRadius * sin(angleRad)).toFloat()
            val labelY = (center.y - labelRadius * cos(angleRad) + 8f).toFloat()
            drawContext.canvas.nativeCanvas.drawText("$angle°", labelX, labelY, degreePaint)
        }
    }

    // رسم الحروف العربية للجهات الأصلية الأربعة
    val labelRadius = radius * 0.66f

    // 1. الشمال (ش) - 0°
    textPaint.color = android.graphics.Color.parseColor("#C9A24B")
    drawContext.canvas.nativeCanvas.drawText("ش", center.x, center.y - labelRadius + 12f, textPaint)

    // 2. الشرق (ق) - 90°
    textPaint.color = android.graphics.Color.WHITE
    drawContext.canvas.nativeCanvas.drawText("ق", center.x + labelRadius, center.y + 12f, textPaint)

    // 3. الجنوب (ج) - 180°
    textPaint.color = android.graphics.Color.WHITE
    drawContext.canvas.nativeCanvas.drawText("ج", center.x, center.y + labelRadius + 12f, textPaint)

    // 4. الغرب (غ) - 270°
    textPaint.color = android.graphics.Color.WHITE
    drawContext.canvas.nativeCanvas.drawText("غ", center.x - labelRadius, center.y + 12f, textPaint)
}

/**
 * رسم إبرة الكعبة المشرفة الفاخرة:
 * - سهم ذهبي متدرج يشير نحو القبلة.
 * - مجسم رمز الكعبة المشرفة في قمة السهم مع الحزام الذهبي.
 * - ذيل معدني أنيق في الطرف المقابل.
 */
private fun DrawScope.drawKaabaNeedle(center: Offset, radius: Float, isAligned: Boolean) {
    val needleLength = radius * 0.78f
    val needleBaseWidth = 18f
    val tailLength = radius * 0.28f

    val accentColor = if (isAligned) CompletedGreen else PrimaryGold

    // 1. السهم العلوي (المتجه للقبلة)
    val upperNeedlePath = Path().apply {
        moveTo(center.x, center.y - needleLength) // رأس السهم
        lineTo(center.x + needleBaseWidth, center.y)
        lineTo(center.x, center.y - 12f)
        close()
    }

    val upperNeedleLeftPath = Path().apply {
        moveTo(center.x, center.y - needleLength)
        lineTo(center.x - needleBaseWidth, center.y)
        lineTo(center.x, center.y - 12f)
        close()
    }

    // تدرج لوني لإضفاء بعد ثلاثي الأبعاد على السهم
    drawPath(
        path = upperNeedlePath,
        brush = Brush.linearGradient(
            colors = listOf(accentColor, Color(0xFFFFF1B8), accentColor),
            start = Offset(center.x, center.y - needleLength),
            end = Offset(center.x + needleBaseWidth, center.y)
        )
    )

    drawPath(
        path = upperNeedleLeftPath,
        brush = Brush.linearGradient(
            colors = listOf(accentColor, Color(0xFF7A5F1C), accentColor),
            start = Offset(center.x, center.y - needleLength),
            end = Offset(center.x - needleBaseWidth, center.y)
        )
    )

    // 2. الذيل السفلي المتوازن
    val tailPath = Path().apply {
        moveTo(center.x, center.y + tailLength)
        lineTo(center.x + (needleBaseWidth * 0.6f), center.y)
        lineTo(center.x - (needleBaseWidth * 0.6f), center.y)
        close()
    }

    drawPath(
        path = tailPath,
        brush = Brush.verticalGradient(
            colors = listOf(Color(0xFF555555), Color(0xFF222222))
        )
    )

    // 3. مجسم الكعبة المشرفة المصغر عند طرف السهم
    val kaabaBoxSize = 26f
    val kaabaCenterY = center.y - needleLength + (kaabaBoxSize / 2f) + 6f
    val kaabaRect = androidx.compose.ui.geometry.Rect(
        center.x - (kaabaBoxSize / 2f),
        kaabaCenterY - (kaabaBoxSize / 2f),
        center.x + (kaabaBoxSize / 2f),
        kaabaCenterY + (kaabaBoxSize / 2f)
    )

    // جسم الكعبة (أسود نقي مع إطار ذهبي)
    drawRoundRect(
        color = Color(0xFF111111),
        topLeft = Offset(kaabaRect.left, kaabaRect.top),
        size = Size(kaabaBoxSize, kaabaBoxSize),
        cornerRadius = CornerRadius(4f, 4f),
        style = Fill
    )

    drawRoundRect(
        color = accentColor,
        topLeft = Offset(kaabaRect.left, kaabaRect.top),
        size = Size(kaabaBoxSize, kaabaBoxSize),
        cornerRadius = CornerRadius(4f, 4f),
        style = Stroke(width = 2f)
    )

    // حزام الكعبة الذهبي الأفقي (Kiswa Belt)
    drawLine(
        color = PrimaryGold,
        start = Offset(kaabaRect.left + 2f, kaabaCenterY - 4f),
        end = Offset(kaabaRect.right - 2f, kaabaCenterY - 4f),
        strokeWidth = 2.5f
    )
}

/**
 * بطاقة عرض بيانات القبلة (المدينة، الزوايا، المسافة).
 */
@Composable
private fun QiblaDetailsCard(uiState: QiblaUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // موقع المستخدم
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = PrimaryGold,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (uiState.userCityName.isNotBlank()) uiState.userCityName else "جاري تحديد الموقع...",
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // شبكة المقاييس الرياضية
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MetricItem(
                    title = "زاوية القبلة",
                    value = "${String.format(Locale.US, "%.1f", uiState.qiblaBearing)}°"
                )
                MetricItem(
                    title = "الاتجاه الحالي",
                    value = "${String.format(Locale.US, "%.1f", uiState.currentHeading)}°"
                )
                MetricItem(
                    title = "المسافة لمكة",
                    value = if (uiState.distanceToMeccaKm > 0) {
                        "${String.format(Locale.US, "%,d", uiState.distanceToMeccaKm.toInt())} كم"
                    } else {
                        "--"
                    }
                )
            }
        }
    }
}

@Composable
private fun MetricItem(title: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = title,
            color = TextSecondary,
            fontSize = 12.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            color = PrimaryGold,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * شريط تنبيه معايرة الحساس (يظهر عند ضعف دقة البوصلة).
 */
@Composable
private fun CalibrationBanner() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF4A3410)),
        border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryGold)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = PrimaryGold,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = "دقة البوصلة منخفضة",
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "حرّك الهاتف على شكل رقم 8 في الهواء لمعايرة الحساسات بدقة.",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }
        }
    }
}

/**
 * بطاقة المطالبة بصلاحية الموقع.
 */
@Composable
private fun LocationPermissionCard(onRequestPermission: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = null,
                tint = PrimaryGold,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "صلاحية الموقع الجغرافي مطلوبة",
                color = TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "لحساب المسافة إلى الكعبة المشرفة وتوجيه البوصلة نحو القبلة بدقة متناهية، يحتاج التطبيق إلى إذن الوصول لموقعك.",
                color = TextSecondary,
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(18.dp))
            Button(
                onClick = onRequestPermission,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGold),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(text = "منح إذن الموقع", color = Color(0xFF0A1F19), fontWeight = FontWeight.Bold)
            }
        }
    }
}

/**
 * بطاقة التنبيه عند إيقاف الـ GPS.
 */
@Composable
private fun GpsDisabledCard(onEnableGps: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = PrimaryGold,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "خدمة الموقع (GPS) غير مفعلة",
                color = TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "يرجى تشغيل الـ GPS من إعدادات الهاتف حتى يتمكن التطبيق من حساب زاوية القبلة وفق موقعك الحالي.",
                color = TextSecondary,
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(18.dp))
            Button(
                onClick = onEnableGps,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGold),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(text = "فتح إعدادات الموقع", color = Color(0xFF0A1F19), fontWeight = FontWeight.Bold)
            }
        }
    }
}

/**
 * بطاقة في حال عدم توفر مستشعرات البوصلة في العتاد.
 */
@Composable
private fun SensorUnavailableCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = Color(0xFFE57373),
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "مستشعر البوصلة غير مدعوم في هذا الجهاز",
                color = TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "للأسف، لا يحتوي هذا الهاتف على حساس دوران أو بوصلة رقمية (Magnetometer) لازمة لتحديد الاتجاه تلقائياً.",
                color = TextSecondary,
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * إطلاق نبضة اهتزازية دقيقة عبر الـ Vibrator عند محاذاة القبلة.
 */
private fun triggerPreciseVibration(context: Context) {
    try {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }

        if (vibrator?.hasVibrator() == true) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(
                    VibrationEffect.createOneShot(80, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(80)
            }
        }
    } catch (_: Exception) {
        // حماية ضد أي استثناءات في إدارة الأجهزة
    }
}
