package com.nabdaldhikr.app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.nabdaldhikr.app.BuildConfig
import com.nabdaldhikr.app.R
import com.nabdaldhikr.app.reminder.ReminderScheduler
import com.nabdaldhikr.app.ui.theme.*
import com.nabdaldhikr.app.updater.AppUpdateDialog
import com.nabdaldhikr.app.updater.AppUpdateManager
import com.nabdaldhikr.app.updater.UpdateInfo
import com.nabdaldhikr.app.updater.UpdateState
import com.nabdaldhikr.app.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch

private val TickFractions = floatArrayOf(0.25f, 0.5f, 0.75f)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: SettingsViewModel
) {
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val fontScale by viewModel.fontScale.collectAsState()
    val vibrationEnabled by viewModel.vibrationEnabled.collectAsState()
    val reminderEnabled by viewModel.reminderEnabled.collectAsState()

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var isCheckingUpdate by remember { mutableStateOf(false) }
    var updateDialogInfo by remember { mutableStateOf<UpdateInfo?>(null) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.setReminderEnabled(true)
            ReminderScheduler.schedule(context, true)
        }
    }

    val switchColors = SwitchDefaults.colors(
        checkedThumbColor = ScreenBg,
        checkedTrackColor = PrimaryGold,
        uncheckedThumbColor = TextSecondary,
        uncheckedTrackColor = ProgressTrack
    )

    val itemColors = ListItemDefaults.colors(
        containerColor = Color.Transparent,
        headlineColor = TextPrimary,
        supportingColor = TextSecondary,
        leadingIconColor = PrimaryGold
    )

    Scaffold(
        containerColor = ScreenBg,
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = HeaderBg,
                    contentColor = TextPrimary,
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        topBar = {
            TopAppBar(
                title = { Text("الإعدادات", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = HeaderBg,
                    titleContentColor = PrimaryGold,
                    navigationIconContentColor = TextPrimary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            
            // قسم المظهر
            Text(
                text = "المظهر",
                color = PrimaryGold,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            
            ListItem(
                colors = itemColors,
                headlineContent = { Text("الوضع الليلي") },
                supportingContent = { Text("استخدام الألوان الداكنة") },
                leadingContent = { Icon(AppIcons.DarkMode, contentDescription = null) },
                trailingContent = {
                    Switch(
                        checked = isDarkMode,
                        onCheckedChange = { viewModel.setDarkMode(it) },
                        colors = switchColors
                    )
                }
            )
            
            HorizontalDivider(color = ProgressTrack.copy(alpha = 0.4f))
            
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(AppIcons.FontDownload, contentDescription = null, tint = PrimaryGold, modifier = Modifier.padding(end = 16.dp))
                    Text("حجم الخط", fontSize = 16.sp, color = TextPrimary)
                }
                Spacer(modifier = Modifier.height(8.dp))
                val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
                Slider(
                    value = fontScale,
                    onValueChange = { viewModel.setFontScale(it) },
                    valueRange = 0.8f..1.6f,
                    steps = 3,
                    modifier = Modifier.padding(horizontal = 16.dp),
                    thumb = {
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .shadow(3.dp, CircleShape, spotColor = PrimaryGold)
                                .background(PrimaryGold, CircleShape)
                                .border(2.dp, CardBg, CircleShape)
                        )
                    },
                    track = { _ ->
                        val fraction = ((fontScale - 0.8f) / (1.6f - 0.8f)).coerceIn(0f, 1f)
                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                        ) {
                            val w = size.width
                            val h = size.height
                            val r = CornerRadius(h / 2f, h / 2f)

                            // مسار شريط التمرير الخلفي غير النشط (أخضر داكن)
                            drawRoundRect(
                                color = ProgressTrack,
                                size = size,
                                cornerRadius = r
                            )

                            // الجزء النشط الذهبي يبدأ من اليمين في RTL بدون أي انقلاب لوني عند القيمة الصغرى
                            val activeWidth = w * fraction
                            if (activeWidth > 0f) {
                                val activeLeft = if (isRtl) w - activeWidth else 0f
                                drawRoundRect(
                                    color = PrimaryGold,
                                    topLeft = Offset(activeLeft, 0f),
                                    size = Size(activeWidth, h),
                                    cornerRadius = r
                                )
                            }

                            // إزالة علامات التوقف (ticks) فوق الجزء النشط ورسمها فقط فوق الجزء غير النشط
                            for (sf in TickFractions) {
                                val isInActivePortion = sf <= fraction
                                if (!isInActivePortion) {
                                    val tickX = if (isRtl) w * (1f - sf) else w * sf
                                    drawCircle(
                                        color = PrimaryGold.copy(alpha = 0.45f),
                                        radius = 2.dp.toPx(),
                                        center = Offset(tickX, h / 2f)
                                    )
                                }
                            }
                        }
                    }
                )
            }
            
            HorizontalDivider(color = ProgressTrack.copy(alpha = 0.4f))
            
            // قسم التفاعل
            Text(
                text = "التفاعل",
                color = PrimaryGold,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            
            ListItem(
                colors = itemColors,
                headlineContent = { Text("الاهتزاز") },
                supportingContent = { Text("اهتزاز عند نقر العداد") },
                leadingContent = { Icon(AppIcons.Vibration, contentDescription = null) },
                trailingContent = {
                    Switch(
                        checked = vibrationEnabled,
                        onCheckedChange = { viewModel.setVibrationEnabled(it) },
                        colors = switchColors
                    )
                }
            )
            
            HorizontalDivider(color = ProgressTrack.copy(alpha = 0.4f))
            
            // قسم التذكيرات
            Text(
                text = "التذكيرات",
                color = PrimaryGold,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            
            ListItem(
                colors = itemColors,
                headlineContent = { Text("التذكير بالأذكار") },
                supportingContent = { Text("إشعار يومي لأذكار الصباح والمساء") },
                leadingContent = { Icon(Icons.Outlined.Notifications, contentDescription = null) },
                trailingContent = {
                    Switch(
                        checked = reminderEnabled,
                        onCheckedChange = { enabled ->
                            if (enabled) {
                                val hasPermission = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.POST_NOTIFICATIONS
                                ) == PackageManager.PERMISSION_GRANTED
                                if (hasPermission) {
                                    viewModel.setReminderEnabled(true)
                                    ReminderScheduler.schedule(context, true)
                                } else {
                                    // طلب الإذن
                                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                            } else {
                                viewModel.setReminderEnabled(false)
                                ReminderScheduler.schedule(context, false)
                            }
                        },
                        colors = switchColors
                    )
                }
            )
            
            HorizontalDivider(color = ProgressTrack.copy(alpha = 0.4f))
            
            // عن التطبيق والتحديثات
            Text(
                text = "عن التطبيق والتحديثات",
                color = PrimaryGold,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            
            ListItem(
                colors = itemColors,
                headlineContent = { Text("نبض الذكر", fontWeight = FontWeight.Bold, fontSize = 17.sp) },
                supportingContent = { 
                    Text("الإصدار v${BuildConfig.VERSION_NAME} (مُحدَّث)\nالمصدر المعتمد: كتاب حصن المسلم\n«أَلَا بِذِكْرِ اللَّهِ تَطْمَئِنُّ الْقُلُوبُ»") 
                },
                leadingContent = { 
                    Image(
                        painter = painterResource(id = R.drawable.app_logo),
                        contentDescription = "شعار نبض الذكر",
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .border(1.dp, PrimaryGold.copy(alpha = 0.5f), CircleShape)
                    )
                }
            )

            HorizontalDivider(color = ProgressTrack.copy(alpha = 0.4f))

            ListItem(
                colors = itemColors,
                modifier = Modifier.clickable(enabled = !isCheckingUpdate) {
                    isCheckingUpdate = true
                    coroutineScope.launch {
                        when (val state = AppUpdateManager.checkForUpdate()) {
                            is UpdateState.UpdateAvailable -> {
                                isCheckingUpdate = false
                                updateDialogInfo = state.info
                            }
                            is UpdateState.UpToDate -> {
                                isCheckingUpdate = false
                                snackbarHostState.showSnackbar("تطبيقك محدث إلى أحدث إصدار متاح بالفعل (v${BuildConfig.VERSION_NAME})")
                            }
                            is UpdateState.Error -> {
                                isCheckingUpdate = false
                                snackbarHostState.showSnackbar("تعذر التحقق: ${state.message}")
                            }
                            else -> {
                                isCheckingUpdate = false
                            }
                        }
                    }
                },
                headlineContent = { Text("التحقق من وجود تحديثات", fontWeight = FontWeight.SemiBold) },
                supportingContent = { 
                    Text(
                        if (isCheckingUpdate) "جاري فحص خوادم التحديث..." 
                        else "تحديث التطبيق وتثبيته تلقائياً من داخل البرنامج بنقرة واحدة"
                    ) 
                },
                leadingContent = { 
                    Icon(AppIcons.SystemUpdate, contentDescription = null, tint = PrimaryGold) 
                },
                trailingContent = {
                    if (isCheckingUpdate) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = PrimaryGold,
                            strokeWidth = 2.5.dp
                        )
                    } else {
                        OutlinedButton(
                            onClick = {
                                isCheckingUpdate = true
                                coroutineScope.launch {
                                    when (val state = AppUpdateManager.checkForUpdate()) {
                                        is UpdateState.UpdateAvailable -> {
                                            isCheckingUpdate = false
                                            updateDialogInfo = state.info
                                        }
                                        is UpdateState.UpToDate -> {
                                            isCheckingUpdate = false
                                            snackbarHostState.showSnackbar("تطبيقك محدث إلى أحدث إصدار متاح بالفعل (v${BuildConfig.VERSION_NAME})")
                                        }
                                        is UpdateState.Error -> {
                                            isCheckingUpdate = false
                                            snackbarHostState.showSnackbar("تعذر التحقق: ${state.message}")
                                        }
                                        else -> {
                                            isCheckingUpdate = false
                                        }
                                    }
                                }
                            },
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryGold),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text("فحص الآن", color = PrimaryGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            )
            
            Spacer(modifier = Modifier.height(32.dp))
        }

        // Render In-App Update Dialog if manual check finds an update
        updateDialogInfo?.let { info ->
            AppUpdateDialog(
                updateInfo = info,
                onDismiss = { updateDialogInfo = null }
            )
        }
    }
}
