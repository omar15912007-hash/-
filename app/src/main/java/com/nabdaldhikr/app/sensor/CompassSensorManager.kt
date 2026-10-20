package com.nabdaldhikr.app.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.nabdaldhikr.app.util.QiblaMathUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs

/**
 * بيانات قراءة مستشعر البوصلة مع حالة الدقة والترشيح.
 */
data class CompassSensorData(
    val rawHeading: Float = 0f,
    val filteredHeading: Float = 0f,
    val accuracy: Int = SensorManager.SENSOR_STATUS_ACCURACY_HIGH,
    val isAvailable: Boolean = true
)

/**
 * مدير مستشعرات العتاد (Hardware Sensors) لقراءة اتجاه البوصلة بدقة متقدمة:
 * - الأولوية القصوى لمستشعر دوران المتجه (ROTATION_VECTOR).
 * - بديل تلقائي سلس عبر مستشعري التسارع والمجال المغناطيسي (ACCELEROMETER + MAGNETIC_FIELD).
 * - إعادة تعيين محاور الإحداثيات (remapCoordinateSystem) بناءً على زاوية إمالة الهاتف.
 * - إدارة دورة حياة صارمة تمنع أي استهلاك للبطارية أثناء توقف الشاشة.
 */
class CompassSensorManager(context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    // المستشعرات
    private val rotationVectorSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
    private val accelerometerSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val magneticSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

    // التدفق التفاعلي للحالة
    private val _compassData = MutableStateFlow(
        CompassSensorData(isAvailable = rotationVectorSensor != null || (accelerometerSensor != null && magneticSensor != null))
    )
    val compassData: StateFlow<CompassSensorData> = _compassData.asStateFlow()

    // مصفوفات العمليات الرياضية (تجنب الحجز المتكرر للذاكرة في recomposition أو OnSensorChanged)
    private val rotationMatrix = FloatArray(9)
    private val remappedMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)

    // مصفوفات القراءات للبديل (Fallback)
    private val lastAccelerometer = FloatArray(3)
    private val lastMagnetometer = FloatArray(3)
    private var isAccelerometerSet = false
    private var isMagnetometerSet = false

    private var isListening = false
    private var currentFilteredHeading = 0f
    private var currentAccuracy = SensorManager.SENSOR_STATUS_ACCURACY_HIGH

    /**
     * بدء الاستماع لقراءات الحساسات بأعلى كفاءة.
     */
    fun startListening() {
        if (isListening) return

        var registered = false
        if (rotationVectorSensor != null) {
            registered = sensorManager.registerListener(
                this,
                rotationVectorSensor,
                SensorManager.SENSOR_DELAY_GAME
            )
        }

        // إذا لم يتوفر مستشعر Rotation Vector أو فشل تسجيله، نستخدم البديل
        if (!registered && accelerometerSensor != null && magneticSensor != null) {
            val accelRegistered = sensorManager.registerListener(
                this,
                accelerometerSensor,
                SensorManager.SENSOR_DELAY_GAME
            )
            val magRegistered = sensorManager.registerListener(
                this,
                magneticSensor,
                SensorManager.SENSOR_DELAY_GAME
            )
            registered = accelRegistered && magRegistered
        }

        isListening = registered
        if (!registered) {
            _compassData.value = _compassData.value.copy(isAvailable = false)
        }
    }

    /**
     * إيقاف الاستماع فوراً لمنع استهلاك البطارية أو دوران المعالج في الخلفية.
     */
    fun stopListening() {
        if (!isListening) return
        sensorManager.unregisterListener(this)
        isListening = false
        isAccelerometerSet = false
        isMagnetometerSet = false
    }

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_ROTATION_VECTOR -> {
                SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                processRotationMatrix(rotationMatrix)
            }
            Sensor.TYPE_ACCELEROMETER -> {
                System.arraycopy(event.values, 0, lastAccelerometer, 0, 3)
                isAccelerometerSet = true
                if (isMagnetometerSet) {
                    computeOrientationFromAccelAndMag()
                }
            }
            Sensor.TYPE_MAGNETIC_FIELD -> {
                System.arraycopy(event.values, 0, lastMagnetometer, 0, 3)
                isMagnetometerSet = true
                if (isAccelerometerSet) {
                    computeOrientationFromAccelAndMag()
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        currentAccuracy = accuracy
        _compassData.value = _compassData.value.copy(accuracy = accuracy)
    }

    /**
     * حساب مصفوفة الدوران عند استخدام مستشعري التسارع والمغناطيسية معاً كبديل.
     */
    private fun computeOrientationFromAccelAndMag() {
        val success = SensorManager.getRotationMatrix(
            rotationMatrix,
            null,
            lastAccelerometer,
            lastMagnetometer
        )
        if (success) {
            processRotationMatrix(rotationMatrix)
        }
    }

    /**
     * معالجة مصفوفة الدوران وإعادة تعيين محاور الإحداثيات وفقاً لوضعية حمل الهاتف:
     * - مستوٍ على طاولة (Flat): المحاور القياسية (X, Y).
     * - مائل أو عمودي في يد المستخدم (Tilted / Upright): إعادة التعيين إلى (X, Z).
     */
    private fun processRotationMatrix(matrix: FloatArray) {
        // حساب زوايا الاتجاه المبدئية لمعرفة زاوية الميل (Pitch)
        SensorManager.getOrientation(matrix, orientationAngles)
        val pitch = Math.toDegrees(orientationAngles[1].toDouble())

        val headingRad: Float = if (abs(pitch) > 45.0) {
            // الهاتف محمول بزاوية مائلة أو عمودية نحو وجه المستخدم
            SensorManager.remapCoordinateSystem(
                matrix,
                SensorManager.AXIS_X,
                SensorManager.AXIS_Z,
                remappedMatrix
            )
            SensorManager.getOrientation(remappedMatrix, orientationAngles)
            orientationAngles[0]
        } else {
            // الهاتف مستوٍ أفقياً
            orientationAngles[0]
        }

        // تحويل الراديان إلى درجات وتطبيعها [0, 360)
        val rawDegrees = Math.toDegrees(headingRad.toDouble())
        val normalizedRaw = QiblaMathUtils.normalize360(rawDegrees).toFloat()

        // تطبيق مرشح التمرير المنخفض (Low-Pass Filter) لتنعيم الحركة
        currentFilteredHeading = if (currentFilteredHeading == 0f) {
            normalizedRaw
        } else {
            QiblaMathUtils.lowPassFilter(
                currentAngle = normalizedRaw,
                previousAngle = currentFilteredHeading,
                alpha = 0.15f
            )
        }

        _compassData.value = CompassSensorData(
            rawHeading = normalizedRaw,
            filteredHeading = currentFilteredHeading,
            accuracy = currentAccuracy,
            isAvailable = true
        )
    }
}
