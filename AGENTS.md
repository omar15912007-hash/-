# مشروع تطبيق «نبض الذكر» (NabdAlDhikr) — دليل الوكيل الذكي (AI Agent Guide)

## 1. المسار وبيئة التطوير والتشغيل (Zero Android Studio GUI Needed)
المستخدم لا يستخدم واجهة Android Studio. يتم التعديل والبناء مباشرة عبر الطرفية (PowerShell / Command Prompt):

- **مسار المشروع المعتمد على القرص:** `F:\omar\New folder`
- **مسار Java (JBR):** `F:\android studio\jbr`
- **مسار Android SDK:** `F:\android studio2`
- **حزمة التطبيق (Package Name):** `com.nabdaldhikr.app`
- **إصدارات SDK:** `minSdk = 26`, `targetSdk = 35`, `compileSdk = 37`
- **أمر البناء السريع لتوليد APK مضغوط على سطح المكتب:**
  ```powershell
  cmd /c "F:\omar\New folder\build_apk.bat"
  ```
  أو يدوياً عبر:
  ```powershell
  $env:JAVA_HOME = "F:\android studio\jbr"
  $env:PATH = "F:\android studio\jbr\bin;$env:PATH"
  .\gradlew.bat assembleRelease
  Copy-Item "app\build\outputs\apk\release\app-release.apk" "$env:USERPROFILE\Desktop\NabdAlDhikr.apk" -Force
  ```

---

## 2. الهيكل المعماري والتقني (Architecture & Stack)
- **لغة البرمجة:** 100% Kotlin مع بنية MVVM + Clean Architecture.
- **واجهة المستخدم:** Jetpack Compose (Material 3) بدعم كامل وأصيل لاتجاه اللغة العربية (RTL First).
- **التدفق التفاعلي:** Kotlin Coroutines & StateFlow.
- **إدارة التخزين والإعدادات:** Android Jetpack DataStore Preferences.
- **التذكيرات اليومية:** Android Jetpack WorkManager (`ReminderWorker.kt`).
- **تحديد الموقع:** Google Play Services Location (`FusedLocationProviderClient`) بدقة فلكية عالية (`PRIORITY_HIGH_ACCURACY`).

---

## 3. الأقسام والميزات المكتملة والمحققة (100% Complete)

### أ. موسوعة الأذكار الشاملة والمحققة (171 ذكراً — 28 قسماً)
- **الملف:** `app/src/main/assets/azkar.json`
- **المصدر:** حصن المسلم من أذكار الكتاب والسنة (فضيلة الشيخ سعيد بن علي بن وهف القحطاني رحمه الله).
- **المميزات:** تشكيل لغوي كامل، تخريج الحديث والمصدر، الفضل والأجر، عدد مرات التكرار، والترجمة الإنجليزية الدقيقة.
- **الأقسام الـ 28:**
  1. أذكار الصباح (30 ذكراً)
  2. أذكار المساء (30 ذكراً)
  3. أذكار النوم (12)
  4. أذكار الاستيقاظ (4)
  5. أذكار بعد الصلاة (9)
  6. أذكار الأذان (5)
  7. دخول المسجد (2) والخروج منه (1)
  8. دخول المنزل (2) والخروج منه (2)
  9. دخول الخلاء (1) والخروج منه (1)
  10. أذكار الطعام والشراب (7)
  11. أذكار السفر (6)
  12. أذكار اللباس (4)
  13. الهم والحزن (5)، الكرب (4)، الغضب (3)
  14. صلاة الاستخارة (1)
  15. المطر (4)، الريح (2)، الرعد (1)
  16. العطاس (3)، عيادة المريض (4)، دخول السوق (2)
  17. الأدعية القرآنية (12)، الرقية الشرعية (6)، الأذكار العامة (10).

### ب. محدد اتجاه القبلة الذكي فلكياً (Smart Qibla Finder)
- **الملفات:**
  1. `app/src/main/java/com/nabdaldhikr/app/util/QiblaMathUtils.kt`:
     - حساب زاوية القبلة الكروية (Great-Circle Forward Azimuth) بدقة كروية.
     - إحداثيات الكعبة المشرفة: خط عرض `21.4224779`، خط طول `39.8261666`.
     - حساب المسافة إلى مكة بصيغة هافرسين (Haversine).
     - تصحيح الشمال الحقيقي الجغرافي (True North) عبر `GeomagneticField`.
     - حساب الزاوية المستمرة (Continuous Angle) للقضاء نهائياً على ارتداد أو دوران الإبرة العكسي عبر $0^\circ \leftrightarrow 360^\circ$.
     - مرشح تمرير منخفض زاوي (Low-Pass Filter) للتنعيم ومنع الاهتزاز.
  2. `app/src/main/java/com/nabdaldhikr/app/sensor/CompassSensorManager.kt`:
     - الأولوية لـ `TYPE_ROTATION_VECTOR` مع بديل تلقائي (`ACCELEROMETER + MAGNETIC_FIELD`).
     - إعادة تعيين المحاور (`remapCoordinateSystem`) عند إمالة الهاتف رأسياً أو تركه مستوياً.
     - إدارة صارمة لدورة الحياة لإيقاف الحساسات فوراً عند الخروج لتوفير البطارية.
  3. `app/src/main/java/com/nabdaldhikr/app/viewmodel/QiblaViewModel.kt`:
     - إحضار الموقع الدقيق لمرة واحدة عبر `FusedLocationProviderClient`.
     - التعرف التلقائي على اسم المدينة والدولة بالعربية عبر `Geocoder`.
     - بث حالة `QiblaUiState` متكاملة تتضمن التطابق ($\pm 3^\circ$) وتنبيه المعايرة (رقم 8).
  4. `app/src/main/java/com/nabdaldhikr/app/ui/screens/QiblaCompassScreen.kt`:
     - قرص بوصلة إسلامي دوار 60fps مع الجهات الأصلية بالعربية (ش، ق، ج، غ) وتدريجات الدرجات.
     - إبرة فاخرة برأس يحمل مجسم الكعبة المشرفة وحزامها الذهبي.
     - نبضة اهتزاز (Haptic Feedback) وتوهج زمردي خفيف عند مطابقة القبلة.
     - شريط تنبيه للمعايرة، ومعالجة كاملة لجميع حالات الأخطاء والـ GPS.
  5. `app/src/main/java/com/nabdaldhikr/app/ui/screens/QiblaScreen.kt`:
     - شاشة الواقع المعزز (AR Camera) الإضافية المستوحاة من Google Qibla Finder.

### ج. السبحة الإلكترونية الذكية (Tasbih)
- **الملفات:** `TasbihScreen.kt`, `TasbihViewModel.kt`
- عدّاد رقمي ذكي بأهداف مخصصة (33، 100، مفتوح)، اهتزاز تفاعلي عند كل تسبيحة، واهتزاز طويل عند اكتمال الورد مع تأثيرات بصرية جذابة.

### د. البحث والمفضلة والتخصيص
- **البحث الفوري (`SearchScreen.kt`):** فلترة فورية باللغتين العربية والإنجليزية مع إزالة علامات التشكيل لتسهيل البحث.
- **المفضلة (`FavoritesScreen.kt`):** حفظ الأذكار المفضلة محلياً في DataStore.
- **الإعدادات (`SettingsScreen.kt`):** التحكم بحجم الخط عبر سلايدر يدعم RTL وتطبيقه مركزياً عبر `LocalDensity`، تفعيل/تعطيل الاهتزاز، وضبط مواعيد إشعارات الصباح والمساء.

---

## 4. هندسة تحسين وتقليص حجم التطبيق (Ultra-Compact APK)
تم تقليص حجم التطبيق من **`25.6 MB`** إلى **`2.22 MB` فقط** (نسبة انخفاض تفوق **91%**):
1. **استبدال المكتبات الضخمة:** تم استبدال مكتبة `material-icons-extended` بملف مخصص خفيف جداً (`AppIcons.kt` بحجم 4KB فقط) يرسم الأيقونات عبر مسارات فيكتور مباشرة.
2. **ضغط R8 وتنقية الموارد:** تفعيل `isMinifyEnabled = true` و `isShrinkResources = true` في `buildTypes.release` مع توقيع الـ Debug لضمان التثبيت الفوري دون الحاجة لمفاتيح خارجية.
3. **تنقية المعماريات واللغات:** قصر ملف الـ APK على المعماريات الفعلية للهواتف الحديثة (`arm64-v8a`, `armeabi-v7a`) واللغتين العربية والإنجليزية (`ar`, `en`).

---

## 5. خريطة الملفات الرئيسية (Source Map)
```
app/src/main/java/com/nabdaldhikr/app/
├── MainActivity.kt                      # نقطة الدخول وتهيئة LocalDensity والتذكيرات
├── data/
│   ├── model/AzkarModel.kt             # نماذج بيانات الأذكار
│   └── repository/AzkarRepository.kt   # قراءة azkar.json من الأصول
├── reminder/
│   ├── ReminderScheduler.kt            # جدولة تذكيرات الصباح والمساء
│   └── ReminderWorker.kt               # تنفيذ الإشعار وتوليد التنبيه
├── sensor/
│   └── CompassSensorManager.kt         # إدارة مستشعرات البوصلة وإعادة تعيين المحاور
├── util/
│   └── QiblaMathUtils.kt               # الرياضيات الفلكية وحسابات زاوية القبلة والشمال الحقيقي
├── viewmodel/
│   ├── AzkarViewModel.kt               # إدارة حالة الأذكار والمفضلة والتقدم
│   ├── QiblaViewModel.kt               # إدارة حالة القبلة والموقع والـ Geocoder
│   ├── SettingsViewModel.kt            # حفظ الإعدادات في DataStore
│   └── TasbihViewModel.kt              # إدارة عداد السبحة والأهداف
├── ui/
│   ├── navigation/AppNavigation.kt     # خريطة الانتقال بين شاشات التطبيق
│   ├── theme/                          # الهوية البصرية (AppIcons, Color, Theme, Type)
│   ├── components/                     # المكونات المشتركة (CategoryCard, GoldDivider)
│   └── screens/
│       ├── SplashScreen.kt             # شاشة البداية المتحركة
│       ├── HomeScreen.kt               # الشاشة الرئيسية مع زري القبلة والسبحة العائمين
│       ├── AzkarListScreen.kt          # قائمة أذكار القسم مع التقدم ونسبة الإنجاز
│       ├── AzkarDetailScreen.kt        # قراءة الذكر مع التكرار والترجمة والفضل
│       ├── QiblaCompassScreen.kt       # البوصلة الإسلامية الذكية الفلكية
│       ├── QiblaScreen.kt              # شاشة القبلة بكاميرا الواقع المعزز (AR)
│       ├── TasbihScreen.kt             # السبحة الإلكترونية
│       ├── FavoritesScreen.kt          # شاشة المفضلة
│       ├── SearchScreen.kt             # شاشة البحث الذكي
│       └── SettingsScreen.kt           # شاشة الإعدادات
```
