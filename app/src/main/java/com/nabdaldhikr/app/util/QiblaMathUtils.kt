package com.nabdaldhikr.app.util

import android.hardware.GeomagneticField
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * دقة رياضية بحتة لحساب اتجاه القبلة، والمسافة إلى مكة المكرمة، وتصحيح الشمال الحقيقي،
 * وتنعيم دوران الإبرة عبر أقصر مسار زاوي.
 */
object QiblaMathUtils {

    /** الإحداثيات الجغرافية الدقيقة للكعبة المشرفة في مكة المكرمة */
    const val KAABA_LATITUDE = 21.4224779
    const val KAABA_LONGITUDE = 39.8261666

    /** نصف قطر الكرة الأرضية التقريبي بالكيلومترات (Mean Earth Radius) */
    private const val EARTH_RADIUS_KM = 6371.0

    /**
     * حساب زاوية القبلة (Initial Bearing / Forward Azimuth) عبر الدائرة العظمى (Great-Circle):
     * θ = atan2(sin(Δλ) * cos(φ2), cos(φ1) * sin(φ2) - sin(φ1) * cos(φ2) * cos(Δλ))
     *
     * @param userLat خط عرض المستخدم بالدرجات العادية
     * @param userLon خط طول المستخدم بالدرجات العادية
     * @return زاوية اتجاه القبلة بالنسبة للشمال الحقيقي محصورة بين [0.0, 360.0)
     */
    fun calculateQiblaBearing(userLat: Double, userLon: Double): Float {
        val phi1 = Math.toRadians(userLat)
        val phi2 = Math.toRadians(KAABA_LATITUDE)
        val deltaLambda = Math.toRadians(KAABA_LONGITUDE - userLon)

        val y = sin(deltaLambda) * cos(phi2)
        val x = cos(phi1) * sin(phi2) - sin(phi1) * cos(phi2) * cos(deltaLambda)

        val initialBearingRad = atan2(y, x)
        val initialBearingDeg = Math.toDegrees(initialBearingRad)

        return normalize360(initialBearingDeg).toFloat()
    }

    /**
     * حساب المسافة السطحية المباشرة إلى الكعبة المشرفة بالكيلومترات باستخدام صيغة هافرسين (Haversine Formula):
     * a = sin²(Δφ/2) + cos(φ1) * cos(φ2) * sin²(Δλ/2)
     * c = 2 * asin(sqrt(a))
     * d = R * c
     *
     * @param userLat خط عرض المستخدم
     * @param userLon خط طول المستخدم
     * @return المسافة بالكيلومتر
     */
    fun calculateDistanceToKaabaKm(userLat: Double, userLon: Double): Double {
        val dLat = Math.toRadians(KAABA_LATITUDE - userLat)
        val dLon = Math.toRadians(KAABA_LONGITUDE - userLon)
        val lat1 = Math.toRadians(userLat)
        val lat2 = Math.toRadians(KAABA_LATITUDE)

        val a = sin(dLat / 2.0).pow(2.0) + cos(lat1) * cos(lat2) * sin(dLon / 2.0).pow(2.0)
        val c = 2.0 * asin(sqrt(a))

        return EARTH_RADIUS_KM * c
    }

    /**
     * تصحيح الانحراف المغناطيسي للأرض للوصول إلى الشمال الجغرافي الحقيقي (Geographic True North):
     * يعتمد على نموذج WMM عبر كلاس GeomagneticField الرسمي من أندرويد.
     *
     * @param magneticHeading زاوية البوصلة المغناطيسية المقروءة من الحساسات [0, 360)
     * @param latitude خط عرض المستخدم
     * @param longitude خط طول المستخدم
     * @param altitudeMeters الارتفاع عن سطح البحر بالأمتار (افتراضياً 0.0)
     * @param timeMillis التوقيت الحالي بالمللي ثانية
     * @return الزاوية المصححة للشمال الحقيقي [0, 360)
     */
    fun correctToTrueNorth(
        magneticHeading: Float,
        latitude: Double,
        longitude: Double,
        altitudeMeters: Double = 0.0,
        timeMillis: Long = System.currentTimeMillis()
    ): Float {
        return try {
            val geomagneticField = GeomagneticField(
                latitude.toFloat(),
                longitude.toFloat(),
                altitudeMeters.toFloat(),
                timeMillis
            )
            val declination = geomagneticField.declination // الانحراف بالدرجات (موجب شرقاً، سالب غرباً)
            normalize360((magneticHeading + declination).toDouble()).toFloat()
        } catch (_: Exception) {
            // في حال تعذر الحساب يظل الاتجاه المغناطيسي كما هو كخيار آمن
            normalize360(magneticHeading.toDouble()).toFloat()
        }
    }

    /**
     * حساب أقصر فارق زاوي بين زاويتين في النطاق [-180, 180]:
     * يمنع الدوران العكسي المزعج عند الانتقال بين 359° و 1°
     *
     * @param fromAngle الزاوية الحالية
     * @param toAngle الزاوية المستهدفة
     * @return الفارق الزاوي بالأقصر اتجاه (-180° إلى +180°)
     */
    fun calculateShortestAngleDelta(fromAngle: Float, toAngle: Float): Float {
        var diff = (toAngle - fromAngle) % 360f
        if (diff > 180f) diff -= 360f
        if (diff < -180f) diff += 360f
        return diff
    }

    /**
     * حساب زاوية تراكمية مستمرة (Continuous Angle) لضمان سلاسة حركة الرسوم المتحركة (60fps animation):
     * تمنع الإبرة من الدوران العكسي بمقدار 358 درجة عند عبور نقطة الصفر (360° ↔ 0°).
     *
     * @param previousContinuousAngle الزاوية السابقة المستمرة (قد تكون سالبة أو تتجاوز 360)
     * @param newTargetAngle الزاوية الجديدة في المجال [0, 360)
     * @return زاوية مستمرة جديدة تضمن أقصر مسار تحريك
     */
    fun calculateContinuousAngle(
        previousContinuousAngle: Float,
        newTargetAngle: Float
    ): Float {
        val currentNormalized = normalize360(previousContinuousAngle.toDouble()).toFloat()
        val delta = calculateShortestAngleDelta(currentNormalized, newTargetAngle)
        return previousContinuousAngle + delta
    }

    /**
     * حساب زاوية توجيه الإبرة بالنسبة لأعلى شاشة الهاتف (12 O'Clock):
     * NeedleAngle = QiblaBearing - DeviceHeading (True North)
     *
     * @param deviceHeadingTrueNorth اتجاه الهاتف بالنسبة للشمال الحقيقي [0, 360)
     * @param qiblaBearing اتجاه القبلة بالنسبة للشمال الحقيقي [0, 360)
     * @return زاوية الإبرة بالنسبة لأعلى الهاتف [0, 360)
     */
    fun calculateNeedleAngle(
        deviceHeadingTrueNorth: Float,
        qiblaBearing: Float
    ): Float {
        return normalize360((qiblaBearing - deviceHeadingTrueNorth).toDouble()).toFloat()
    }

    /**
     * مرشح تمرير منخفض زاوي (Low-Pass Filter) لتنعيم قراءات البوصلة الحركية
     * يحل مشكلة تشوه المتوسط الحسابي بالقرب من نقطة 0/360 بالاعتماد على المكونات المثلثية (Sin/Cos).
     *
     * @param currentAngle الزاوية الجديدة المقروءة
     * @param previousAngle الزاوية المنعمة السابقة
     * @param alpha معامل الترشيح (0.15f يعطي توازناً مثالياً بين سرعة الاستجابة ومنع الارتعاش)
     * @return زاوية منعمة ثابتة
     */
    fun lowPassFilter(
        currentAngle: Float,
        previousAngle: Float,
        alpha: Float = 0.15f
    ): Float {
        val currentRad = Math.toRadians(currentAngle.toDouble())
        val prevRad = Math.toRadians(previousAngle.toDouble())

        val sinVal = (1.0 - alpha) * sin(prevRad) + alpha * sin(currentRad)
        val cosVal = (1.0 - alpha) * cos(prevRad) + alpha * cos(currentRad)

        val smoothedDeg = Math.toDegrees(atan2(sinVal, cosVal))
        return normalize360(smoothedDeg).toFloat()
    }

    /**
     * التحقق مما إذا كان اتجاه الهاتف الحالي متطابقاً مع القبلة ضمن هامش سماحية محدد (±3 درجات).
     *
     * @param currentHeading اتجاه الهاتف
     * @param qiblaBearing اتجاه القبلة
     * @param toleranceDegrees هامش الخطأ المسموح به (افتراضياً 3.0 درجات)
     * @return true إذا كان الاتجاه نحو الكعبة بدقة
     */
    fun isAlignedWithQibla(
        currentHeading: Float,
        qiblaBearing: Float,
        toleranceDegrees: Float = 3.0f
    ): Boolean {
        val delta = abs(calculateShortestAngleDelta(currentHeading, qiblaBearing))
        return delta <= toleranceDegrees
    }

    /**
     * تطبيع أي قيمة بالدرجات لتكون دائماً محصورة في النطاق الرياضي القياسي [0.0, 360.0).
     */
    fun normalize360(degrees: Double): Double {
        return (degrees % 360.0 + 360.0) % 360.0
    }
}
