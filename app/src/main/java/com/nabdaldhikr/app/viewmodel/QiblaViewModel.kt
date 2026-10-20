package com.nabdaldhikr.app.viewmodel

import android.Manifest
import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.SensorManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.nabdaldhikr.app.sensor.CompassSensorManager
import com.nabdaldhikr.app.util.QiblaMathUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * حالة واجهة المستخدم لشاشة القبلة (Qibla UI State).
 */
data class QiblaUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val userCityName: String = "",
    val distanceToMeccaKm: Double = 0.0,
    val currentHeading: Float = 0f,
    val qiblaBearing: Float = 0f,
    val needleAngle: Float = 0f,
    val continuousNeedleAngle: Float = 0f,
    val continuousCompassAngle: Float = 0f,
    val isAligned: Boolean = false,
    val needsCalibration: Boolean = false,
    val isLocationPermissionGranted: Boolean = false,
    val isGpsEnabled: Boolean = true,
    val isSensorAvailable: Boolean = true
)

/**
 * نموذج العرض (ViewModel) لمحدد اتجاه القبلة:
 * - يدير إحضار الموقع بدقة عالية عبر FusedLocationProviderClient.
 * - يترجم الإحداثيات إلى اسم المدينة باللغة العربية عبر Geocoder.
 * - يربط قراءات البوصلة بالرياضيات الفلكية ويعرض حالة محاذاة القبلة (±3 درجات).
 * - يوقف عمل الحساسات فوراً عند مغادرة الشاشة حفاظاً على موارد الجهاز.
 */
class QiblaViewModel(application: Application) : AndroidViewModel(application) {

    private val compassSensorManager = CompassSensorManager(application)
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(application)
    private val locationManager = application.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    private val _uiState = MutableStateFlow(QiblaUiState())
    val uiState: StateFlow<QiblaUiState> = _uiState.asStateFlow()

    private var userLatitude: Double? = null
    private var userLongitude: Double? = null
    private var userAltitude: Double = 0.0
    private var calculatedQiblaBearing: Float = 0f

    init {
        observeCompassSensors()
        checkLocationPermissionAndFetch()
    }

    /**
     * ربط التدفق التفاعلي لمستشعر البوصلة وتطبيق التصحيح الرياضي المستمر.
     */
    private fun observeCompassSensors() {
        viewModelScope.launch {
            compassSensorManager.compassData.collect { sensorData ->
                if (!sensorData.isAvailable) {
                    _uiState.update { it.copy(isSensorAvailable = false) }
                    return@collect
                }

                val rawMagnetic = sensorData.filteredHeading
                val lat = userLatitude
                val lon = userLongitude

                // تصحيح الشمال الحقيقي إذا توفر الموقع
                val trueHeading = if (lat != null && lon != null) {
                    QiblaMathUtils.correctToTrueNorth(
                        magneticHeading = rawMagnetic,
                        latitude = lat,
                        longitude = lon,
                        altitudeMeters = userAltitude
                    )
                } else {
                    rawMagnetic
                }

                val qibla = calculatedQiblaBearing
                val rawNeedle = QiblaMathUtils.calculateNeedleAngle(trueHeading, qibla)

                // حساب الزوايا التراكمية المستمرة لمنع القفز عبر حاجز 0/360 في الرسوم المتحركة
                val continuousNeedle = QiblaMathUtils.calculateContinuousAngle(
                    previousContinuousAngle = _uiState.value.continuousNeedleAngle,
                    newTargetAngle = rawNeedle
                )
                val continuousCompass = QiblaMathUtils.calculateContinuousAngle(
                    previousContinuousAngle = _uiState.value.continuousCompassAngle,
                    newTargetAngle = trueHeading
                )

                val aligned = lat != null && lon != null && QiblaMathUtils.isAlignedWithQibla(
                    currentHeading = trueHeading,
                    qiblaBearing = qibla,
                    toleranceDegrees = 3.0f
                )

                val needsCalib = sensorData.accuracy == SensorManager.SENSOR_STATUS_ACCURACY_LOW ||
                        sensorData.accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE

                _uiState.update { current ->
                    current.copy(
                        currentHeading = trueHeading,
                        needleAngle = rawNeedle,
                        continuousNeedleAngle = continuousNeedle,
                        continuousCompassAngle = continuousCompass,
                        isAligned = aligned,
                        needsCalibration = needsCalib,
                        isSensorAvailable = true
                    )
                }
            }
        }
    }

    /**
     * التحقق من الصلاحيات والـ GPS وطلب تحديث الموقع.
     */
    fun checkLocationPermissionAndFetch() {
        val fineGranted = ContextCompat.checkSelfPermission(
            getApplication(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarseGranted = ContextCompat.checkSelfPermission(
            getApplication(),
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val hasPermission = fineGranted || coarseGranted
        val gpsEnabled = isGpsProviderEnabled()

        _uiState.update {
            it.copy(
                isLocationPermissionGranted = hasPermission,
                isGpsEnabled = gpsEnabled
            )
        }

        if (hasPermission && gpsEnabled) {
            requestFreshLocation()
        } else if (!hasPermission) {
            _uiState.update { it.copy(isLoading = false) }
        } else if (!gpsEnabled) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    errorMessage = "خدمة الموقع (GPS) معطلة، يرجى تفعيلها لتحديد اتجاه القبلة بدقة"
                )
            }
        }
    }

    /**
     * جلب الموقع الجغرافي الدقيق لمرة واحدة بأعلى دقة ممكنة (PRIORITY_HIGH_ACCURACY).
     */
    @SuppressLint("MissingPermission")
    fun requestFreshLocation() {
        val hasPermission = ContextCompat.checkSelfPermission(
            getApplication(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
            getApplication(),
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) return

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        val cancellationTokenSource = CancellationTokenSource()

        fusedLocationClient.getCurrentLocation(
            Priority.PRIORITY_HIGH_ACCURACY,
            cancellationTokenSource.token
        ).addOnSuccessListener { location: Location? ->
            if (location != null) {
                applyLocationData(location)
            } else {
                // إذا تعذر الحصول على قراءة فورية نستخدم آخر موقع معروف كبديل
                fusedLocationClient.lastLocation.addOnSuccessListener { lastLoc: Location? ->
                    if (lastLoc != null) {
                        applyLocationData(lastLoc)
                    } else {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = "تعذر تحديد الموقع بدقة، يرجى التأكد من التواجد في مكان مفتوح"
                            )
                        }
                    }
                }.addOnFailureListener {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "تعذر تحديد الموقع الجغرافي"
                        )
                    }
                }
            }
        }.addOnFailureListener { e ->
            _uiState.update {
                it.copy(
                    isLoading = false,
                    errorMessage = e.localizedMessage ?: "حدث خطأ أثناء جلب الموقع"
                )
            }
        }
    }

    /**
     * معالجة إحداثيات المستخدم وحساب زاوية القبلة والمسافة واسم المدينة.
     */
    private fun applyLocationData(location: Location) {
        val lat = location.latitude
        val lon = location.longitude
        userLatitude = lat
        userLongitude = lon
        userAltitude = location.altitude

        val bearing = QiblaMathUtils.calculateQiblaBearing(lat, lon)
        val distance = QiblaMathUtils.calculateDistanceToKaabaKm(lat, lon)
        calculatedQiblaBearing = bearing

        _uiState.update {
            it.copy(
                isLoading = false,
                errorMessage = null,
                distanceToMeccaKm = distance,
                qiblaBearing = bearing
            )
        }

        // إحضار اسم المدينة في خيط خلفي (IO Dispatcher) باللغة العربية
        reverseGeocode(lat, lon)
    }

    /**
     * تحويل الإحداثيات إلى اسم المدينة والدولة بالعربية عبر Geocoder.
     */
    private fun reverseGeocode(latitude: Double, longitude: Double) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val geocoder = Geocoder(getApplication(), Locale.forLanguageTag("ar"))
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    geocoder.getFromLocation(latitude, longitude, 1) { addresses ->
                        val address = addresses.firstOrNull()
                        val city = address?.locality ?: address?.subAdminArea ?: address?.adminArea ?: ""
                        val country = address?.countryName ?: ""
                        val fullAddress = if (city.isNotBlank() && country.isNotBlank()) {
                            "$city، $country"
                        } else {
                            country.ifBlank { city }
                        }
                        if (fullAddress.isNotBlank()) {
                            _uiState.update { it.copy(userCityName = fullAddress) }
                        }
                    }
                } else {
                    @Suppress("DEPRECATION")
                    val addresses = geocoder.getFromLocation(latitude, longitude, 1)
                    val address = addresses?.firstOrNull()
                    val city = address?.locality ?: address?.subAdminArea ?: address?.adminArea ?: ""
                    val country = address?.countryName ?: ""
                    val fullAddress = if (city.isNotBlank() && country.isNotBlank()) {
                        "$city، $country"
                    } else {
                        country.ifBlank { city }
                    }
                    if (fullAddress.isNotBlank()) {
                        _uiState.update { it.copy(userCityName = fullAddress) }
                    }
                }
            } catch (_: Exception) {
                // إذا لم يتوفر اتصال بالإنترنت للـ Geocoder، تظل القبلة تعمل بدقة عبر الإحداثيات
            }
        }
    }

    private fun isGpsProviderEnabled(): Boolean {
        return try {
            locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                    locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        } catch (_: Exception) {
            false
        }
    }

    /**
     * استئناف عمل الحساسات عند دخول الشاشة.
     */
    fun onResume() {
        compassSensorManager.startListening()
        checkLocationPermissionAndFetch()
    }

    /**
     * إيقاف الحساسات فوراً عند الخروج لحفظ بطارية الجهاز.
     */
    fun onPause() {
        compassSensorManager.stopListening()
    }

    override fun onCleared() {
        super.onCleared()
        compassSensorManager.stopListening()
    }
}
