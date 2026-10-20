# قواعد R8/ProGuard لتطبيق نبض الذكر

# Gson يستخدم انعكاس JSON على نماذج البيانات
-keep class com.nabdaldhikr.app.data.model.** { *; }

# TasbihDhikr يُسلسَل ويُعاد بناؤه عبر Gson
-keep class com.nabdaldhikr.app.viewmodel.TasbihDhikr { *; }

# WorkManager يكتشف العمال عبر الانعكاس
-keep class com.nabdaldhikr.app.reminder.** { *; }