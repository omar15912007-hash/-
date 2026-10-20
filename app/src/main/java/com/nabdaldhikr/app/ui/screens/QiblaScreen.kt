package com.nabdaldhikr.app.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Paint
import android.graphics.SurfaceTexture
import android.graphics.Typeface
import android.hardware.GeomagneticField
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.HapticFeedbackConstants
import android.view.Surface
import android.view.TextureView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.nabdaldhikr.app.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import kotlin.math.*

// الإحداثيات الجغرافية الدقيقة لمركز الكعبة المشرفة (مكة المكرمة)
private const val KAABA_LAT = 21.422487
private const val KAABA_LON = 39.826206

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QiblaScreen(
    navController: NavController,
    vibrationEnabled: Boolean = true
) {
    val context = LocalContext.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()

    // حالة الموقع الجغرافي
    var userLat by remember { mutableDoubleStateOf(30.0444) } // افتراضي مبدئي (القاهرة) حتى يلتقط الـ GPS
    var userLon by remember { mutableDoubleStateOf(31.2357) }
    var userAlt by remember { mutableDoubleStateOf(0.0) }
    var accuracyMeters by remember { mutableFloatStateOf(0f) }
    var locationSource by remember { mutableStateOf("جاري تحديد الموقع الدقيق...") }
    var cityName by remember { mutableStateOf("تحديد الموقع...") }
    var isGpsEnabled by remember { mutableStateOf(true) }
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }

    // حالة وضع الكاميرا (AR مثل Qibla Finder)
    var isArMode by remember { mutableStateOf(false) }
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    // قراءات الحساسات (البوصلة والميل ثلاثي الأبعاد)
    var smoothAzimuth by remember { mutableFloatStateOf(0f) }
    var smoothPitch by remember { mutableFloatStateOf(0f) }
    var smoothRoll by remember { mutableFloatStateOf(0f) }
    var sensorAccuracy by remember { mutableIntStateOf(SensorManager.SENSOR_STATUS_ACCURACY_HIGH) }
    var hasSensor by remember { mutableStateOf(true) }

    // حساب زاوية القبلة والمسافة إلى الكعبة المشرفة
    val qiblaBearing = remember(userLat, userLon) {
        calculateQiblaBearing(userLat, userLon)
    }
    val distanceKm = remember(userLat, userLon) {
        calculateDistanceKm(userLat, userLon, KAABA_LAT, KAABA_LON)
    }

    // فرق الزاوية الدائري الأقصر (-180 إلى +180)
    // موجب = القبلة على اليمين، سالب = القبلة على اليسار
    val angleDelta = remember(smoothAzimuth, qiblaBearing) {
        ((qiblaBearing - smoothAzimuth + 540f) % 360f) - 180f
    }
    val absDelta = abs(angleDelta)
    val isAligned = absDelta <= 4.5f

    // اهتزاز عند التطابق مع اتجاه القبلة
    var wasAligned by remember { mutableStateOf(false) }
    LaunchedEffect(isAligned) {
        if (isAligned && !wasAligned && vibrationEnabled) {
            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        }
        wasAligned = isAligned
    }

    // جلب اسم المدينة عبر الإنترنت (Geocoder أو IP Fallback)
    fun resolvePlaceName(lat: Double, lon: Double) {
        scope.launch(Dispatchers.IO) {
            try {
                @Suppress("DEPRECATION")
                val geocoder = Geocoder(context, Locale("ar"))
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(lat, lon, 1)
                if (!addresses.isNullOrEmpty()) {
                    val addr = addresses[0]
                    val locality = addr.locality ?: addr.subAdminArea ?: addr.adminArea ?: ""
                    val country = addr.countryName ?: ""
                    val label = listOf(locality, country).filter { it.isNotBlank() }.joinToString("، ")
                    if (label.isNotBlank()) {
                        withContext(Dispatchers.Main) { cityName = label }
                        return@launch
                    }
                }
            } catch (_: Exception) {}
        }
    }

    // تحديد الموقع عبر الإنترنت في حال ضعف الـ GPS داخل المباني
    fun fetchNetworkIpLocationIfNeeded() {
        scope.launch(Dispatchers.IO) {
            try {
                val conn = (URL("https://ipwho.is/?lang=ar").openConnection() as HttpURLConnection).apply {
                    connectTimeout = 4000
                    readTimeout = 4000
                }
                if (conn.responseCode == 200) {
                    val body = conn.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(body)
                    if (json.optBoolean("success", false)) {
                        val lat = json.optDouble("latitude", Double.NaN)
                        val lon = json.optDouble("longitude", Double.NaN)
                        val city = json.optString("city", "")
                        val country = json.optString("country", "")
                        if (!lat.isNaN() && !lon.isNaN()) {
                            withContext(Dispatchers.Main) {
                                if (accuracyMeters == 0f) {
                                    userLat = lat
                                    userLon = lon
                                    locationSource = "تحديد عبر الشبكة"
                                    val label = listOf(city, country).filter { it.isNotBlank() }.joinToString("، ")
                                    if (label.isNotBlank()) cityName = label
                                }
                            }
                        }
                    }
                }
            } catch (_: Exception) {
                withContext(Dispatchers.Main) {
                    if (cityName == "تحديد الموقع...") {
                        cityName = String.format(Locale.US, "%.3f° N, %.3f° E", userLat, userLon)
                    }
                }
            }
        }
    }

    // تحديث الموقع من نظام أندرويد (GPS + Network)
    @SuppressLint("MissingPermission")
    fun refreshLocation() {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val gpsOn = lm.isProviderEnabled(LocationManager.GPS_PROVIDER)
        val netOn = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        isGpsEnabled = gpsOn || netOn

        if (!hasLocationPermission) {
            fetchNetworkIpLocationIfNeeded()
            return
        }

        val providers = listOf(
            LocationManager.GPS_PROVIDER,
            LocationManager.NETWORK_PROVIDER,
            LocationManager.PASSIVE_PROVIDER
        )
        var bestLoc: Location? = null
        for (p in providers) {
            try {
                val loc = lm.getLastKnownLocation(p) ?: continue
                if (bestLoc == null || loc.accuracy < bestLoc.accuracy || loc.time > bestLoc.time + 60_000) {
                    bestLoc = loc
                }
            } catch (_: Exception) {}
        }

        if (bestLoc != null) {
            userLat = bestLoc.latitude
            userLon = bestLoc.longitude
            userAlt = bestLoc.altitude
            accuracyMeters = bestLoc.accuracy
            locationSource = if (bestLoc.provider == LocationManager.GPS_PROVIDER) {
                "GPS دقيق (±${bestLoc.accuracy.roundToInt()} م)"
            } else {
                "موقع الشبكة (±${bestLoc.accuracy.roundToInt()} م)"
            }
            cityName = String.format(Locale.US, "%.4f°, %.4f°", userLat, userLon)
            resolvePlaceName(userLat, userLon)
        } else {
            fetchNetworkIpLocationIfNeeded()
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        hasLocationPermission = perms[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        refreshLocation()
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        if (granted) isArMode = true
    }

    // طلب صلاحية الموقع عند فتح الشاشة لأول مرة
    LaunchedEffect(Unit) {
        if (!hasLocationPermission) {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        } else {
            refreshLocation()
        }
    }

    // الاستماع المستمر لتحديثات الموقع (GPS + Network)
    DisposableEffect(hasLocationPermission) {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                userLat = location.latitude
                userLon = location.longitude
                userAlt = location.altitude
                accuracyMeters = location.accuracy
                locationSource = if (location.provider == LocationManager.GPS_PROVIDER) {
                    "GPS مباشر (±${location.accuracy.roundToInt()} م)"
                } else {
                    "الشبكة المباشرة (±${location.accuracy.roundToInt()} م)"
                }
                resolvePlaceName(location.latitude, location.longitude)
            }
            @Deprecated("Deprecated in Java")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
            override fun onProviderEnabled(provider: String) { isGpsEnabled = true }
            override fun onProviderDisabled(provider: String) {}
        }

        if (hasLocationPermission) {
            try {
                if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1500L, 1f, listener, Looper.getMainLooper())
                }
                if (lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                    lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 2000L, 2f, listener, Looper.getMainLooper())
                }
            } catch (_: SecurityException) {}
        }

        onDispose {
            try {
                lm.removeUpdates(listener)
            } catch (_: Exception) {}
        }
    }

    // الاستماع لحساسات الاتجاه والمجال المغناطيسي مع تصحيح الشمال الجغرافي الحقيقي (True North)
    // ودعم مسك الهاتف أفقياً (وضع البوصلة) أو عمودياً (وضع الكاميرا AR)
    DisposableEffect(userLat, userLon, userAlt, isArMode) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        val accelSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val magSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

        if (rotationSensor == null && (accelSensor == null || magSensor == null)) {
            hasSensor = false
        }

        val rotationMatrix = FloatArray(9)
        val remappedMatrix = FloatArray(9)
        val orientationAngles = FloatArray(3)
        val gravity = FloatArray(3)
        val geomagnetic = FloatArray(3)
        var hasGravity = false
        var hasMag = false

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                var matrixReady = false
                if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
                    SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                    matrixReady = true
                } else if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                    lowPassFilter(event.values, gravity, 0.15f)
                    hasGravity = true
                } else if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
                    lowPassFilter(event.values, geomagnetic, 0.15f)
                    hasMag = true
                }

                if (!matrixReady && hasGravity && hasMag) {
                    matrixReady = SensorManager.getRotationMatrix(rotationMatrix, null, gravity, geomagnetic)
                }

                if (matrixReady) {
                    // فحص زاوية ميل الهاتف: إذا كان في وضع الكاميرا أو مرفوعاً عمودياً نعيد توجيه المحاور (AXIS_X, AXIS_Z)
                    val zTilt = abs(rotationMatrix[8])
                    val useUprightRemap = isArMode || zTilt < 0.55f
                    if (useUprightRemap) {
                        SensorManager.remapCoordinateSystem(
                            rotationMatrix,
                            SensorManager.AXIS_X,
                            SensorManager.AXIS_Z,
                            remappedMatrix
                        )
                        SensorManager.getOrientation(remappedMatrix, orientationAngles)
                    } else {
                        SensorManager.getOrientation(rotationMatrix, orientationAngles)
                    }

                    val magAzimuthDeg = (Math.toDegrees(orientationAngles[0].toDouble()).toFloat() + 360f) % 360f
                    val pitchDeg = Math.toDegrees(orientationAngles[1].toDouble()).toFloat()
                    val rollDeg = Math.toDegrees(orientationAngles[2].toDouble()).toFloat()

                    // تصحيح الانحراف المغناطيسي للحصول على الشمال الجغرافي الحقيقي (True North)
                    val geoField = GeomagneticField(
                        userLat.toFloat(),
                        userLon.toFloat(),
                        userAlt.toFloat(),
                        System.currentTimeMillis()
                    )
                    val trueAzimuth = (magAzimuthDeg + geoField.declination + 360f) % 360f

                    // تنعيم دائري ذكي لمنع القفز عند الانتقال بين 359° و 0°
                    val diff = ((trueAzimuth - smoothAzimuth + 540f) % 360f) - 180f
                    smoothAzimuth = (smoothAzimuth + diff * 0.18f + 360f) % 360f
                    smoothPitch += (pitchDeg - smoothPitch) * 0.18f
                    smoothRoll += (rollDeg - smoothRoll) * 0.18f
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                if (sensor?.type == Sensor.TYPE_MAGNETIC_FIELD || sensor?.type == Sensor.TYPE_ROTATION_VECTOR) {
                    sensorAccuracy = accuracy
                }
            }
        }

        if (rotationSensor != null) {
            sensorManager.registerListener(listener, rotationSensor, SensorManager.SENSOR_DELAY_GAME)
        } else if (accelSensor != null && magSensor != null) {
            sensorManager.registerListener(listener, accelSensor, SensorManager.SENSOR_DELAY_GAME)
            sensorManager.registerListener(listener, magSensor, SensorManager.SENSOR_DELAY_GAME)
        }

        onDispose {
            sensorManager.unregisterListener(listener)
        }
    }

    // نبض الهالة الذهبية/الخضراء حول الكعبة
    val infiniteTransition = rememberInfiniteTransition(label = "QiblaPulse")
    val pulseRadiusFraction by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.22f,
        animationSpec = infiniteRepeatable(
            animation = tween(1300, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseRadius"
    )
    val arrowPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ArrowPhase"
    )

    val activeAccentColor by animateColorAsState(
        targetValue = if (isAligned) CompletedGreen else PrimaryGold,
        animationSpec = tween(350),
        label = "AccentColor"
    )

    // كائنات الرسم النصية المحفوظة في الذاكرة لتجنب أي تخصيص أثناء الرسم (Clean Code / Zero Allocation)
    val degreePaint = remember {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            textSize = 30f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
    }
    val cardinalPaint = remember {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.parseColor("#C9A24D")
            textSize = 38f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
    }

    Scaffold(
        containerColor = ScreenBg,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "محدد القبلة الذكي",
                            fontWeight = FontWeight.Bold,
                            fontSize = 19.sp,
                            color = PrimaryGold
                        )
                        Text(
                            text = cityName,
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
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
                    IconButton(onClick = { refreshLocation() }) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = "تحديث الموقع",
                            tint = PrimaryGold
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = HeaderBg)
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(ScreenGradient)
                .padding(padding)
        ) {
            // 1. خلفية الكاميرا المباشرة عند تفعيل وضع الواقع المعزز (AR مثل Google Qibla Finder)
            if (isArMode && hasCameraPermission) {
                CameraPreviewSurface(modifier = Modifier.fillMaxSize())
                // طبقة تظليل خفيفة لإبراز عناصر الواقع المعزز بوضوح
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.28f))
                )
            }

            // 2. طبقة الرسم التفاعلي (شريط الأفق العلوي + بوابة الكعبة العائمة + خط التوجيه + رادار Qibla Finder السفلي)
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val centerX = w / 2f
                val centerY = h * 0.44f

                // أ) شريط الأفق العلوي للدرجات (Horizon Compass Ribbon مثل Qibla Finder)
                drawHorizonRibbon(
                    width = w,
                    azimuth = smoothAzimuth,
                    qiblaBearing = qiblaBearing,
                    accentColor = activeAccentColor,
                    degreePaint = degreePaint
                )

                if (isArMode) {
                    // ب) وضع الكاميرا (AR): عدسة Qibla Finder التفاعلية مع بوابة الكعبة المتحركة ثلاثية الأبعاد
                    drawArQiblaFinderOverlay(
                        centerX = centerX,
                        centerY = centerY,
                        angleDelta = angleDelta,
                        pitch = smoothPitch,
                        isAligned = isAligned,
                        pulseFraction = pulseRadiusFraction,
                        arrowPhase = arrowPhase,
                        accentColor = activeAccentColor,
                        distanceKm = distanceKm
                    )
                } else {
                    // ج) وضع البوصلة الذكية ثلاثية الأبعاد: حلقة Qibla Finder مع مسار التوجيه وميزان الاستواء
                    drawLuxuryQiblaCompass(
                        centerX = centerX,
                        centerY = centerY,
                        azimuth = smoothAzimuth,
                        qiblaBearing = qiblaBearing,
                        angleDelta = angleDelta,
                        pitch = smoothPitch,
                        roll = smoothRoll,
                        isAligned = isAligned,
                        pulseFraction = pulseRadiusFraction,
                        arrowPhase = arrowPhase,
                        accentColor = activeAccentColor,
                        cardinalPaint = cardinalPaint,
                        degreePaint = degreePaint
                    )
                }

                // د) الرادار السفلي ثلاثي الأبعاد (مثل الدائرة السفلية في Google Qibla Finder)
                if (isArMode) {
                    drawBottomPerspectiveRadar(
                        centerX = centerX,
                        centerY = h * 0.77f,
                        radius = min(w, h) * 0.14f,
                        azimuth = smoothAzimuth,
                        qiblaBearing = qiblaBearing,
                        accentColor = activeAccentColor
                    )
                }
            }

            // 3. شريط التبديل العلوي بين (البوصلة الذكية) و(الواقع المعزز AR بالكاميرا)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 52.dp, start = 16.dp, end = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(HeaderBg.copy(alpha = 0.92f))
                        .border(1.dp, PrimaryGold.copy(alpha = 0.35f), RoundedCornerShape(50))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ModeChip(
                        title = "البوصلة الذكية",
                        icon = AppIcons.Explore,
                        selected = !isArMode,
                        onClick = { isArMode = false }
                    )
                    ModeChip(
                        title = "الكاميرا (AR)",
                        icon = AppIcons.CameraAlt,
                        selected = isArMode,
                        onClick = {
                            if (hasCameraPermission) {
                                isArMode = true
                            } else {
                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // شارة التوجيه المباشر (يمين / يسار / متطابق)
                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (isAligned) CompletedGreen.copy(alpha = 0.22f) else CardBg.copy(alpha = 0.88f),
                    border = BorderStroke(1.dp, activeAccentColor.copy(alpha = 0.65f))
                ) {
                    val guidanceText = when {
                        !hasSensor -> "جهازك لا يحتوي على حساس بوصلة مغناطيسي"
                        isAligned -> "أنت الآن باتجاه القبلة الصحيح تماماً"
                        angleDelta > 0 -> "استدر لليمين بمقدار ${absDelta.roundToInt()}° نحو الكعبة"
                        else -> "استدر لليسار بمقدار ${absDelta.roundToInt()}° نحو الكعبة"
                    }
                    Text(
                        text = guidanceText,
                        color = if (isAligned) CompletedGreen else TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 7.dp)
                    )
                }
            }

            // 4. اللوحة السفلية للمعلومات الدقيقة (المسافة للكعبة، زاوية القبلة، حالة الـ GPS، دقة البوصلة)
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (!isGpsEnabled || !hasLocationPermission) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (!hasLocationPermission) {
                                    locationPermissionLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.ACCESS_FINE_LOCATION,
                                            Manifest.permission.ACCESS_COARSE_LOCATION
                                        )
                                    )
                                } else {
                                    context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                                }
                            },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = CardBg.copy(alpha = 0.95f)),
                        border = BorderStroke(1.dp, PrimaryGold)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(AppIcons.MyLocation, contentDescription = null, tint = PrimaryGold, modifier = Modifier.size(20.dp))
                                Text(
                                    text = if (!hasLocationPermission) "اضغط لمنح صلاحية الموقع لأعلى دقة"
                                    else "الـ GPS مغلق — اضغط لتفعيله لدقة أعلى",
                                    fontSize = 12.sp,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Text(
                                text = "تفعيل",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryGold
                            )
                        }
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBg.copy(alpha = 0.92f)),
                    border = BorderStroke(1.dp, activeAccentColor.copy(alpha = 0.35f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            QiblaStatItem(
                                label = "زاوية القبلة",
                                value = "${qiblaBearing.roundToInt()}°",
                                valueColor = PrimaryGold
                            )
                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(36.dp)
                                    .background(ProgressTrack)
                            )
                            QiblaStatItem(
                                label = "اتجاهك الحالي",
                                value = "${smoothAzimuth.roundToInt()}°",
                                valueColor = activeAccentColor
                            )
                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(36.dp)
                                    .background(ProgressTrack)
                            )
                            QiblaStatItem(
                                label = "المسافة للكعبة",
                                value = "${String.format(Locale.US, "%,d", distanceKm.roundToInt())} كم",
                                valueColor = TextPrimary
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(color = ProgressTrack.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = locationSource,
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                            val accuracyLabel = when (sensorAccuracy) {
                                SensorManager.SENSOR_STATUS_ACCURACY_HIGH -> "دقة البوصلة: ممتازة"
                                SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM -> "دقة البوصلة: متوسطة"
                                else -> "حرّك الهاتف بشكل (∞) للمعايرة"
                            }
                            Text(
                                text = accuracyLabel,
                                fontSize = 11.sp,
                                color = if (sensorAccuracy == SensorManager.SENSOR_STATUS_ACCURACY_HIGH) CompletedGreen else PrimaryGold,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ModeChip(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) PrimaryGold else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = if (selected) ScreenBg else TextPrimary,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = title,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = if (selected) ScreenBg else TextPrimary
        )
    }
}

@Composable
private fun QiblaStatItem(
    label: String,
    value: String,
    valueColor: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = valueColor
        )
        Text(
            text = label,
            fontSize = 11.sp,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * شريط البوصلة الأفقية العلوي (مثل Google Qibla Finder)
 */
private fun DrawScope.drawHorizonRibbon(
    width: Float,
    azimuth: Float,
    qiblaBearing: Float,
    accentColor: Color,
    degreePaint: Paint
) {
    val barHeight = 38.dp.toPx()
    drawRect(
        color = HeaderBg.copy(alpha = 0.85f),
        topLeft = Offset(0f, 0f),
        size = Size(width, barHeight)
    )
    val fovDeg = 90f
    val pxPerDeg = width / fovDeg
    val centerX = width / 2f

    // رسم تدريجات الدرجات كل 15 درجة
    val startDeg = (azimuth - fovDeg / 2f).toInt() - 15
    val endDeg = (azimuth + fovDeg / 2f).toInt() + 15
    for (deg in startDeg..endDeg) {
        if (deg % 15 == 0) {
            val normDeg = (deg % 360 + 360) % 360
            val diff = ((normDeg - azimuth + 540f) % 360f) - 180f
            if (abs(diff) <= fovDeg / 2f) {
                val x = centerX + diff * pxPerDeg
                val isCardinal = normDeg % 90 == 0
                drawLine(
                    color = if (isCardinal) PrimaryGold else TextSecondary.copy(alpha = 0.5f),
                    start = Offset(x, barHeight - if (isCardinal) 14.dp.toPx() else 8.dp.toPx()),
                    end = Offset(x, barHeight),
                    strokeWidth = if (isCardinal) 2.dp.toPx() else 1.dp.toPx()
                )
                val label = when (normDeg) {
                    0 -> "ش"
                    90 -> "ق"
                    180 -> "ج"
                    270 -> "غ"
                    else -> null
                }
                if (label != null) {
                    degreePaint.textSize = 12.dp.toPx()
                    degreePaint.color = android.graphics.Color.parseColor("#C9A24D")
                    drawContext.canvas.nativeCanvas.drawText(label, x, 17.dp.toPx(), degreePaint)
                }
            }
        }
    }

    // علامة موقع الكعبة على شريط الأفق
    val qiblaDiff = ((qiblaBearing - azimuth + 540f) % 360f) - 180f
    val clampedX = (centerX + qiblaDiff * pxPerDeg).coerceIn(14.dp.toPx(), width - 14.dp.toPx())
    drawCircle(
        color = accentColor,
        radius = 6.dp.toPx(),
        center = Offset(clampedX, barHeight - 7.dp.toPx())
    )

    // مؤشر المنتصف الثابت
    drawLine(
        color = accentColor,
        start = Offset(centerX, 0f),
        end = Offset(centerX, barHeight),
        strokeWidth = 2.dp.toPx()
    )
}

/**
 * رسم وضع البوصلة الذكية الفاخرة (تجمع بين أسلوب Google Qibla Finder والهوية الزمردية الذهبية)
 */
private fun DrawScope.drawLuxuryQiblaCompass(
    centerX: Float,
    centerY: Float,
    azimuth: Float,
    qiblaBearing: Float,
    angleDelta: Float,
    pitch: Float,
    roll: Float,
    isAligned: Boolean,
    pulseFraction: Float,
    arrowPhase: Float,
    accentColor: Color,
    cardinalPaint: Paint,
    degreePaint: Paint
) {
    val outerRadius = min(size.width, size.height) * 0.34f
    val center = Offset(centerX, centerY)

    // 1. هالة التوهج عند المحاذاة الصحيحة للقبلة
    if (isAligned) {
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    CompletedGreen.copy(alpha = 0.32f),
                    CompletedGreen.copy(alpha = 0.08f),
                    Color.Transparent
                ),
                center = center,
                radius = outerRadius * 1.32f * pulseFraction
            ),
            radius = outerRadius * 1.32f * pulseFraction,
            center = center
        )
    }

    // 2. قوس التوجيه الديناميكي على المحيط الخارجي (مثل Google Qibla Finder)
    // يوضح المسار الأقصر للدوران من اتجاه الهاتف الحالي (الأعلى -90°) إلى اتجاه القبلة
    val arcRadius = outerRadius + 16.dp.toPx()
    drawCircle(
        color = ProgressTrack.copy(alpha = 0.55f),
        radius = arcRadius,
        center = center,
        style = Stroke(width = 4.dp.toPx())
    )
    if (!isAligned) {
        drawArc(
            color = PrimaryGold,
            startAngle = -90f,
            sweepAngle = angleDelta,
            useCenter = false,
            topLeft = Offset(centerX - arcRadius, centerY - arcRadius),
            size = Size(arcRadius * 2f, arcRadius * 2f),
            style = Stroke(
                width = 5.dp.toPx(),
                cap = StrokeCap.Round,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(18f, 12f), phase = -arrowPhase * 30f)
            )
        )
    } else {
        drawCircle(
            color = CompletedGreen,
            radius = arcRadius,
            center = center,
            style = Stroke(width = 5.dp.toPx())
        )
    }

    // 3. قرص البوصلة الدوار
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFF194B34), CardBg, ScreenBg),
            center = center,
            radius = outerRadius
        ),
        radius = outerRadius,
        center = center
    )
    drawCircle(
        color = activeBorderColor(isAligned),
        radius = outerRadius,
        center = center,
        style = Stroke(width = 2.5.dp.toPx())
    )

    // تدوير تدريجات البوصلة والاتجاهات الأربعة عكس زاوية الهاتف
    rotate(degrees = -azimuth, pivot = center) {
        for (i in 0 until 72) {
            val angleDeg = i * 5f
            val angleRad = Math.toRadians((angleDeg - 90f).toDouble())
            val isMajor = i % 18 == 0
            val isMedium = i % 6 == 0
            val tickLen = when {
                isMajor -> 16.dp.toPx()
                isMedium -> 10.dp.toPx()
                else -> 5.dp.toPx()
            }
            val startR = outerRadius - 6.dp.toPx()
            val endR = startR - tickLen
            val sx = centerX + (startR * cos(angleRad)).toFloat()
            val sy = centerY + (startR * sin(angleRad)).toFloat()
            val ex = centerX + (endR * cos(angleRad)).toFloat()
            val ey = centerY + (endR * sin(angleRad)).toFloat()

            drawLine(
                color = when {
                    i == 0 -> CompletedGreen
                    isMajor -> PrimaryGold
                    isMedium -> TextPrimary.copy(alpha = 0.7f)
                    else -> TextSecondary.copy(alpha = 0.35f)
                },
                start = Offset(sx, sy),
                end = Offset(ex, ey),
                strokeWidth = if (isMajor) 2.5.dp.toPx() else 1.2.dp.toPx(),
                cap = StrokeCap.Round
            )
        }

        // كتابة الاتجاهات الأصلية (ش، ق، ج، غ)
        val labels = listOf(0f to "ش", 90f to "ق", 180f to "ج", 270f to "غ")
        val textRadius = outerRadius - 34.dp.toPx()
        cardinalPaint.textSize = 15.dp.toPx()
        for ((deg, lbl) in labels) {
            val rad = Math.toRadians((deg - 90f).toDouble())
            val tx = centerX + (textRadius * cos(rad)).toFloat()
            val ty = centerY + (textRadius * sin(rad)).toFloat() + 5.dp.toPx()
            cardinalPaint.color = if (deg == 0f) {
                android.graphics.Color.parseColor("#57B96C")
            } else {
                android.graphics.Color.parseColor("#C9A24D")
            }
            drawContext.canvas.nativeCanvas.drawText(lbl, tx, ty, cardinalPaint)
        }
    }

    // 4. إبرة القبلة الذهبية وبوابة الكعبة المشرفة (تتحرك بزاوية angleDelta)
    rotate(degrees = angleDelta, pivot = center) {
        // شعاع التوجيه من المركز إلى الكعبة
        drawLine(
            brush = Brush.verticalGradient(
                colors = listOf(accentColor, accentColor.copy(alpha = 0.1f)),
                startY = centerY - outerRadius * 0.82f,
                endY = centerY
            ),
            start = Offset(centerX, centerY),
            end = Offset(centerX, centerY - outerRadius * 0.78f),
            strokeWidth = 4.dp.toPx(),
            cap = StrokeCap.Round
        )

        // إبرة البوصلة المزدوجة الفاخرة
        val needleTop = centerY - outerRadius * 0.56f
        val needleBottom = centerY + outerRadius * 0.35f
        val needleHalfW = 10.dp.toPx()

        val northNeedle = Path().apply {
            moveTo(centerX, needleTop)
            lineTo(centerX + needleHalfW, centerY)
            lineTo(centerX - needleHalfW, centerY)
            close()
        }
        drawPath(northNeedle, color = accentColor)

        val southNeedle = Path().apply {
            moveTo(centerX, needleBottom)
            lineTo(centerX + needleHalfW * 0.75f, centerY)
            lineTo(centerX - needleHalfW * 0.75f, centerY)
            close()
        }
        drawPath(southNeedle, color = ProgressTrack)

        // أيقونة الكعبة المشرفة على طرف شعاع القبلة
        val kaabaCenter = Offset(centerX, centerY - outerRadius - 16.dp.toPx())
        drawKaabaIcon(
            center = kaabaCenter,
            boxSize = 32.dp.toPx(),
            accentColor = accentColor,
            isAligned = isAligned,
            pulseFraction = pulseFraction
        )
    }

    // 5. عدسة المحاذاة العلوية الثابتة (عند الساعة 12)
    val topTarget = Offset(centerX, centerY - outerRadius - 16.dp.toPx())
    drawCircle(
        color = accentColor,
        radius = 22.dp.toPx(),
        center = topTarget,
        style = Stroke(width = 2.dp.toPx())
    )

    // 6. ميزان استواء الهاتف ثلاثي الأبعاد في قلب البوصلة (Bubble Level)
    val bubbleMaxOffset = outerRadius * 0.22f
    val bubbleX = (centerX + (roll / 45f).coerceIn(-1f, 1f) * bubbleMaxOffset)
    val bubbleY = (centerY + (pitch / 45f).coerceIn(-1f, 1f) * bubbleMaxOffset)

    drawCircle(
        color = ScreenBg.copy(alpha = 0.85f),
        radius = bubbleMaxOffset + 8.dp.toPx(),
        center = center
    )
    drawCircle(
        color = PrimaryGold.copy(alpha = 0.35f),
        radius = bubbleMaxOffset + 8.dp.toPx(),
        center = center,
        style = Stroke(width = 1.5.dp.toPx())
    )
    drawCircle(
        color = accentColor.copy(alpha = 0.85f),
        radius = 7.dp.toPx(),
        center = Offset(bubbleX, bubbleY)
    )
    drawCircle(
        color = PrimaryGold,
        radius = 3.dp.toPx(),
        center = center
    )
}

/**
 * رسم واجهة الواقع المعزز (AR Qibla Finder) فوق الكاميرا المباشرة
 */
private fun DrawScope.drawArQiblaFinderOverlay(
    centerX: Float,
    centerY: Float,
    angleDelta: Float,
    pitch: Float,
    isAligned: Boolean,
    pulseFraction: Float,
    arrowPhase: Float,
    accentColor: Color,
    distanceKm: Double
) {
    val center = Offset(centerX, centerY)
    val reticleRadius = 56.dp.toPx()

    // حساب موقع بوابة الكعبة في الفضاء ثلاثي الأبعاد على الشاشة
    val horizontalFov = 65f
    val pxPerDegX = size.width / horizontalFov
    val rawKaabaX = centerX + angleDelta * pxPerDegX
    // إزاحة رأسية ناعمة حسب زاوية ميل الكاميرا
    val rawKaabaY = centerY + ((pitch + 75f).coerceIn(-25f, 25f) * 2.5.dp.toPx())

    val clampedKaabaX = rawKaabaX.coerceIn(48.dp.toPx(), size.width - 48.dp.toPx())
    val kaabaPos = Offset(clampedKaabaX, rawKaabaY)

    // 1. خط التوجيه المنقط والأسهم المتحركة من عدسة المنتصف إلى الكعبة (مثل Google Qibla Finder)
    if (!isAligned) {
        drawLine(
            color = PrimaryGold,
            start = center,
            end = kaabaPos,
            strokeWidth = 4.dp.toPx(),
            cap = StrokeCap.Round,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(22f, 14f), phase = -arrowPhase * 36f)
        )
    }

    // 2. عدسة التركيز الوسطى (Qibla Finder Target Ring)
    drawCircle(
        color = accentColor.copy(alpha = if (isAligned) 0.25f else 0.08f),
        radius = reticleRadius * (if (isAligned) pulseFraction else 1f),
        center = center
    )
    drawCircle(
        color = accentColor,
        radius = reticleRadius,
        center = center,
        style = Stroke(width = if (isAligned) 3.5.dp.toPx() else 2.dp.toPx())
    )

    // 3. بوابة الكعبة المشرفة العائمة بالواقع المعزز
    drawKaabaIcon(
        center = kaabaPos,
        boxSize = 48.dp.toPx(),
        accentColor = accentColor,
        isAligned = isAligned,
        pulseFraction = pulseFraction
    )
}

/**
 * الرادار السفلي المنظوري (مثل الدائرة السفلية في Google Qibla Finder)
 */
private fun DrawScope.drawBottomPerspectiveRadar(
    centerX: Float,
    centerY: Float,
    radius: Float,
    azimuth: Float,
    qiblaBearing: Float,
    accentColor: Color
) {
    val center = Offset(centerX, centerY)
    drawCircle(
        color = ScreenBg.copy(alpha = 0.78f),
        radius = radius,
        center = center
    )
    drawCircle(
        color = PrimaryGold.copy(alpha = 0.6f),
        radius = radius,
        center = center,
        style = Stroke(width = 2.dp.toPx())
    )

    // مخروط الرؤية الأمامي للكاميرا
    val fovPath = Path().apply {
        moveTo(centerX, centerY)
        lineTo(centerX - radius * 0.55f, centerY - radius * 0.82f)
        lineTo(centerX + radius * 0.55f, centerY - radius * 0.82f)
        close()
    }
    drawPath(fovPath, color = accentColor.copy(alpha = 0.22f))

    // نقطة موقع الكعبة على محيط الرادار
    val relAngleRad = Math.toRadians((qiblaBearing - azimuth - 90f).toDouble())
    val kx = centerX + (radius * 0.82f * cos(relAngleRad)).toFloat()
    val ky = centerY + (radius * 0.82f * sin(relAngleRad)).toFloat()
    drawLine(
        color = accentColor,
        start = center,
        end = Offset(kx, ky),
        strokeWidth = 2.5.dp.toPx(),
        cap = StrokeCap.Round
    )
    drawCircle(
        color = accentColor,
        radius = 5.dp.toPx(),
        center = Offset(kx, ky)
    )
    drawCircle(
        color = TextPrimary,
        radius = 3.5.dp.toPx(),
        center = center
    )
}

/**
 * رسم مجسم الكعبة المشرفة بالحزام الذهبي وهالة النور
 */
private fun DrawScope.drawKaabaIcon(
    center: Offset,
    boxSize: Float,
    accentColor: Color,
    isAligned: Boolean,
    pulseFraction: Float
) {
    // هالة دائرية خلف الكعبة
    drawCircle(
        color = accentColor.copy(alpha = if (isAligned) 0.38f else 0.2f),
        radius = (boxSize * 0.78f) * (if (isAligned) pulseFraction else 1f),
        center = center
    )
    drawCircle(
        color = ScreenBg,
        radius = boxSize * 0.65f,
        center = center
    )
    drawCircle(
        color = accentColor,
        radius = boxSize * 0.65f,
        center = center,
        style = Stroke(width = 2.dp.toPx())
    )

    // مكعب الكعبة الأسود الفاخر
    val cubeW = boxSize * 0.62f
    val cubeH = boxSize * 0.68f
    val topLeft = Offset(center.x - cubeW / 2f, center.y - cubeH / 2f)
    drawRoundRect(
        color = Color(0xFF101413),
        topLeft = topLeft,
        size = Size(cubeW, cubeH),
        cornerRadius = CornerRadius(3.dp.toPx())
    )
    drawRoundRect(
        color = PrimaryGold.copy(alpha = 0.8f),
        topLeft = topLeft,
        size = Size(cubeW, cubeH),
        cornerRadius = CornerRadius(3.dp.toPx()),
        style = Stroke(width = 1.2.dp.toPx())
    )

    // الحزام الذهبي لكسوة الكعبة المشرفة
    val bandY = topLeft.y + cubeH * 0.26f
    drawLine(
        color = PrimaryGold,
        start = Offset(topLeft.x + 2.dp.toPx(), bandY),
        end = Offset(topLeft.x + cubeW - 2.dp.toPx(), bandY),
        strokeWidth = 3.dp.toPx()
    )

    // باب الكعبة الذهبي
    val doorW = cubeW * 0.24f
    val doorH = cubeH * 0.34f
    drawRoundRect(
        color = PrimaryGold,
        topLeft = Offset(center.x + cubeW * 0.05f, topLeft.y + cubeH * 0.48f),
        size = Size(doorW, doorH),
        cornerRadius = CornerRadius(1.5.dp.toPx())
    )
}

private fun activeBorderColor(isAligned: Boolean): Color =
    if (isAligned) CompletedGreen else PrimaryGold.copy(alpha = 0.55f)

/**
 * معاينة الكاميرا الخلفية المباشرة باستخدام Camera2 API بدون أي مكتبات خارجية ثقيلة
 */
@SuppressLint("MissingPermission")
@Composable
private fun CameraPreviewSurface(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    DisposableEffect(Unit) {
        onDispose { }
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            TextureView(ctx).apply {
                surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                    private var cameraDevice: CameraDevice? = null
                    private var captureSession: CameraCaptureSession? = null

                    override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
                        openRearCamera(ctx, surface) { cam, session ->
                            cameraDevice = cam
                            captureSession = session
                        }
                    }

                    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {}

                    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                        try {
                            captureSession?.close()
                            cameraDevice?.close()
                        } catch (_: Exception) {}
                        return true
                    }

                    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
                }
            }
        }
    )
}

@SuppressLint("MissingPermission")
private fun openRearCamera(
    context: Context,
    surfaceTexture: SurfaceTexture,
    onOpened: (CameraDevice?, CameraCaptureSession?) -> Unit
) {
    try {
        val manager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val cameraId = manager.cameraIdList.firstOrNull { id ->
            val chars = manager.getCameraCharacteristics(id)
            chars.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK
        } ?: manager.cameraIdList.firstOrNull() ?: return

        surfaceTexture.setDefaultBufferSize(1280, 720)
        val previewSurface = Surface(surfaceTexture)
        val mainHandler = Handler(Looper.getMainLooper())

        manager.openCamera(cameraId, object : CameraDevice.StateCallback() {
            override fun onOpened(camera: CameraDevice) {
                try {
                    val requestBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply {
                        addTarget(previewSurface)
                        set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
                    }
                    @Suppress("DEPRECATION")
                    camera.createCaptureSession(
                        listOf(previewSurface),
                        object : CameraCaptureSession.StateCallback() {
                            override fun onConfigured(session: CameraCaptureSession) {
                                try {
                                    session.setRepeatingRequest(requestBuilder.build(), null, mainHandler)
                                    onOpened(camera, session)
                                } catch (_: Exception) {}
                            }
                            override fun onConfigureFailed(session: CameraCaptureSession) {}
                        },
                        mainHandler
                    )
                } catch (_: Exception) {
                    camera.close()
                }
            }

            override fun onDisconnected(camera: CameraDevice) {
                camera.close()
            }

            override fun onError(camera: CameraDevice, error: Int) {
                camera.close()
            }
        }, mainHandler)
    } catch (_: Exception) {}
}

/**
 * معادلة الدائرة العظمى (Great-Circle Bearing) لحساب زاوية القبلة بدقة فلكية من أي نقطة على الأرض
 */
private fun calculateQiblaBearing(lat: Double, lon: Double): Float {
    val phi1 = Math.toRadians(lat)
    val phi2 = Math.toRadians(KAABA_LAT)
    val deltaLon = Math.toRadians(KAABA_LON - lon)

    val y = sin(deltaLon) * cos(phi2)
    val x = cos(phi1) * sin(phi2) - sin(phi1) * cos(phi2) * cos(deltaLon)
    val theta = Math.toDegrees(atan2(y, x))
    return ((theta + 360.0) % 360.0).toFloat()
}

/**
 * حساب المسافة بالكيلومتر بين موقع المستخدم والكعبة المشرفة بمعادلة Haversine
 */
private fun calculateDistanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val earthRadiusKm = 6371.0
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = sin(dLat / 2).pow(2.0) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2.0)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    return earthRadiusKm * c
}

private fun lowPassFilter(input: FloatArray, output: FloatArray, alpha: Float) {
    for (i in input.indices) {
        output[i] = output[i] + alpha * (input[i] - output[i])
    }
}
