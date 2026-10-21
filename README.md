# 📿 Nabd Al-Dhikr (نبض الذكر) - v1.0.0

<p align="center">
  <img src="app/src/main/res/drawable/app_logo.png" alt="Nabd Al-Dhikr Logo" width="128" height="128" />
</p>

<p align="center">
  <strong>A modern, ultra-lightweight, and authentic Islamic Android application.</strong><br>
  <em>تطبيق إسلامي حديث، فائق الخفة، ومحقق للأندرويد والكمبيوتر</em>
</p>

<p align="center">
  <a href="https://github.com/omar15912007-hash/-/releases/latest">
    <img src="https://img.shields.io/badge/Download-Latest%20APK%20(2.2%20MB)-0D5C3A?style=for-the-badge&logo=android&logoColor=white" alt="Download APK" />
  </a>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android%20%7C%20PC-3DDC84.svg?style=flat-square&logo=android" alt="Platform" />
  <img src="https://img.shields.io/badge/Language-Kotlin-7F52FF.svg?style=flat-square&logo=kotlin" alt="Language" />
  <img src="https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4.svg?style=flat-square&logo=jetpackcompose" alt="Compose" />
  <img src="https://img.shields.io/badge/APK%20Size-2.22%20MB-brightgreen.svg?style=flat-square" alt="Size" />
  <img src="https://img.shields.io/badge/License-Free%20%26%20Open-gold.svg?style=flat-square" alt="License" />
</p>

---

## 📖 Overview | نبذة عن التطبيق

**Nabd Al-Dhikr (نبض الذكر)** is built from the ground up using 100% native Android technologies (**Kotlin**, **Jetpack Compose Material 3**, and **Clean Architecture + MVVM**). Engineered for maximum battery efficiency, instant responsiveness (60fps), and an ultra-compact footprint (**only 2.22 MB**), it provides essential daily spiritual tools without advertisements or internet dependency.

> **نبذة بالعربية:**  
> تم بناء تطبيق **نبض الذكر** بأحدث تقنيات أندرويد الأصلية بنسبة 100%، وبحجم فائق الصغر (**2.22 ميجابايت فقط**) واستهلاك شبه منعدم للبطارية والذاكرة (RAM). يجمع التطبيق بين الأذكار اليومية المحققة، محدد القبلة الفلكي، والسبحة الذكية مع نظام تحديث تلقائي فوري.

---

## 📲 How to Download & Install (دليل التحميل والتثبيت بالعربية)

### أ. خطوات التثبيت على الهاتف المحمول (Android):
1. **تحميل التطبيق:** اضغط على الرابط المباشر:  
   👉 **[تحميل ملف NabdAlDhikr-v1.0.0.apk (اضغط هنا)](https://github.com/omar15912007-hash/-/releases/download/v1.0.0/NabdAlDhikr-v1.0.0.apk)**
2. **فتح الملف:** افتح الملف من الإشعارات أو من مجلد التنزيلات (Downloads).
3. **منح الإذن والتثبيت:** إذا طُلب منك السماح بتثبيت التطبيقات من مصادر غير معروفة، فعّل الإذن واضغط **تثبيت (Install)**.
4. **التحديث التلقائي:** مستقبلاً، سيقوم التطبيق بتحديث نفسه بضغطة زر داخلية دون الحاجة لإعادة التحميل اليدوي.

---

### ب. خطوات تشغيل التطبيق على الكمبيوتر (PC / Windows / Mac في 4 خطوات):

لتشغيل التطبيق على شاشة الكمبيوتر الكبيرة والتحكم بالماوس ولوحة المفاتيح:

1. **تحميل ملف التطبيق:**  
   حمّل ملف **[`NabdAlDhikr-v1.0.0.apk`](https://github.com/omar15912007-hash/-/releases/download/v1.0.0/NabdAlDhikr-v1.0.0.apk)** على جهاز الكمبيوتر.
2. **تنزيل أي محاكي أندرويد خفيف:**  
   إذا لم يكن لديك محاكي، حمّل أي محاكي خفيف ومجاني مثل [LDPlayer](https://www.ldplayer.net/) أو [BlueStacks](https://www.bluestacks.com/) أو [NoxPlayer](https://www.bignox.com/) وثبّته.
3. **سحب وإفلات الملف (Drag & Drop):**  
   اسحب ملف الـ APK الذي حمّلته وأفلته داخل نافذة المحاكي المفتوحة، وسيتم تثبيته تلقائياً خلال ثانيتين فقط.
4. **فتح التطبيق:**  
   اضغط على أيقونة **نبض الذكر** من واجهة المحاكي وابدأ في قراءة الأذكار واستخدام السبحة بالماوس بكل سهولة وسلاسة!

---

## ✨ Key Features | المميزات الرئيسية

### 1. Complete & Verified Adhkar Encyclopedia | موسوعة الأذكار الشاملة والمحققة
- **171 Adhkar across 28 categories** directly sourced from *Hisn al-Muslim (حصن المسلم)*.
- Full vocalized Arabic (Tashkeel), English translation, virtue references, Hadith sourcing, and interactive repetition counters.
- Covers Morning & Evening Adhkar, Sleep & Waking, Prayer & Adhan, Travel, Protection, and Quranic Supplications.
> **عربياً:** 171 ذكراً مشكولاً ومحققاً من حصن المسلم عبر 28 قسماً مع تخريج الحديث وفضل الذكر وترجمة إنجليزية كاملة.

### 2. Astronomical Qibla Finder | محدد القبلة الذكي فلكياً
- High-accuracy Great-Circle forward azimuth calculation towards the Holy Kaaba (`21.4225° N, 39.8262° E`).
- Magnetic declination correction to Geographic True North via `GeomagneticField`.
- 60fps angular low-pass filter preventing 0°/360° needle bounce.
- Haptic pulse feedback and emerald glow upon accurate alignment ($\pm 3^\circ$).
> **عربياً:** حساب فلكي كروي دقيق لزاوية القبلة والمسافة لمكة، مع بوصلة 60 إطاراً في الثانية واهتزاز تفاعلي عند مطابقة القبلة.

### 3. Smart Digital Tasbih | السبحة الإلكترونية الذكية
- Digital counter with customizable targets (33, 100, or unlimited).
- Responsive haptic feedback on each bead tap and milestone completions.
- Fluid rolling number animations.
> **عربياً:** عداد تسبيح ذكي مع اهتزازات تفاعلية، أهداف ورد مخصصة، وانتقالات رقمية انسيابية.

### 4. Fast Search & Offline Favorites | البحث الفوري والمفضلة
- Instant bilingual search supporting Arabic text with and without diacritics (Tashkeel).
- Local offline favorites stored via Jetpack DataStore Preferences.
> **عربياً:** محرك بحث ذكي يتجاهل علامات التشكيل لتسهيل الوصول للأذكار، مع ميزة حفظ الأذكار المفضلة بدون إنترنت.

### 5. Scheduled Daily Reminders | التنبيهات المجدولة
- Battery-friendly background notifications scheduled using Android Jetpack `WorkManager`.
> **عربياً:** تذكيرات يومية ذكية بأذكار الصباح والمساء بأقل استهلاك ممكن للبطارية.

---

## 🛠️ Tech Stack & Architecture | الهيكلية التقنية

- **Language:** 100% Kotlin
- **Architecture:** Clean Architecture + MVVM + Repository Pattern
- **UI Toolkit:** Jetpack Compose (Material 3) — RTL-First Arabic layout
- **Async & Reactive:** Kotlin Coroutines & StateFlow
- **Local Persistence:** Android Jetpack DataStore Preferences
- **Sensors:** Hardware `TYPE_ROTATION_VECTOR` + Fused Location API
- **Background Tasks:** Android Jetpack WorkManager
- **CI/CD:** Automated GitHub Actions pipeline (`.github/workflows/android-release.yml`)

---

## 🚀 Building from Source | البناء من الكود المصدري

```bash
# Clone the repository
git clone https://github.com/omar15912007-hash/-.git
cd -

# Build the optimized release APK
./gradlew assembleRelease

# The output APK will be located at:
# app/build/outputs/apk/release/app-release.apk
```

---

## 📄 License & Credits
Developed with ❤️ as a continuous charity (*Sadaqah Jariyah*). Distributed freely for the benefit of the Muslim Ummah worldwide.  
*«أَلَا بِذِكْرِ اللَّهِ تَطْمَئِنُّ الْقُلُوبُ»*
