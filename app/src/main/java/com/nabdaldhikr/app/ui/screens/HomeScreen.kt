package com.nabdaldhikr.app.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ExitToApp
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.nabdaldhikr.app.R
import com.nabdaldhikr.app.ui.components.CategoryCard
import com.nabdaldhikr.app.ui.theme.*
import com.nabdaldhikr.app.viewmodel.AzkarViewModel

data class CategoryItem(val id: String, val nameAr: String, val nameEn: String, val icon: ImageVector)

val categoriesList = listOf(
    CategoryItem("morning_azkar", "أذكار الصباح", "Morning Azkar", AppIcons.WbSunny),
    CategoryItem("evening_azkar", "أذكار المساء", "Evening Azkar", AppIcons.Nightlight),
    CategoryItem("sleep_azkar", "أذكار النوم", "Sleep Azkar", AppIcons.Bedtime),
    CategoryItem("wakeup_azkar", "أذكار الاستيقاظ", "Wakeup Azkar", AppIcons.Alarm),
    CategoryItem("after_prayer", "أذكار بعد الصلاة", "After Prayer Azkar", AppIcons.Mosque),
    CategoryItem("adhan_azkar", "أذكار الأذان", "Adhan Azkar", AppIcons.VolumeUp),
    CategoryItem("mosque_enter", "دعاء دخول المسجد", "Enter Mosque Dua", AppIcons.DoorFront),
    CategoryItem("mosque_exit", "دعاء الخروج من المسجد", "Exit Mosque Dua", AppIcons.DoorBack),
    CategoryItem("home_enter", "دعاء دخول المنزل", "Enter Home Dua", Icons.Outlined.Home),
    CategoryItem("home_exit", "دعاء الخروج من المنزل", "Exit Home Dua", Icons.AutoMirrored.Outlined.ExitToApp),
    CategoryItem("restroom_enter", "دعاء دخول الخلاء", "Enter Restroom Dua", AppIcons.Wc),
    CategoryItem("restroom_exit", "دعاء الخروج من الخلاء", "Exit Restroom Dua", AppIcons.CleanHands),
    CategoryItem("food_azkar", "أذكار الطعام", "Food Azkar", AppIcons.Restaurant),
    CategoryItem("travel_azkar", "أذكار السفر", "Travel Azkar", AppIcons.Flight),
    CategoryItem("clothing_azkar", "دعاء لبس الثوب", "Clothing Dua", AppIcons.Checkroom),
    CategoryItem("anger_azkar", "أذكار الغضب", "Anger Azkar", AppIcons.SentimentDissatisfied),
    CategoryItem("worry_azkar", "دعاء الهم والحزن", "Worry Dua", AppIcons.SentimentNeutral),
    CategoryItem("distress_azkar", "دعاء الكرب", "Distress Dua", AppIcons.Healing),
    CategoryItem("istikhara", "دعاء الاستخارة", "Istikhara Dua", Icons.Outlined.Star),
    CategoryItem("rain_azkar", "دعاء المطر", "Rain Dua", AppIcons.WaterDrop),
    CategoryItem("wind_azkar", "دعاء الريح", "Wind Dua", AppIcons.Air),
    CategoryItem("thunder_azkar", "ذكر الرعد", "Thunder Dua", AppIcons.Thunderstorm),
    CategoryItem("sneezing_azkar", "دعاء العطاس", "Sneezing Dua", AppIcons.Sick),
    CategoryItem("quran_duas", "أدعية قرآنية", "Quran Duas", AppIcons.MenuBook),
    CategoryItem("ruqyah", "الرقية الشرعية", "Ruqyah", AppIcons.Shield),
    CategoryItem("general_azkar", "أذكار متنوعة", "General Azkar", AppIcons.AutoAwesome),
    CategoryItem("market_dua", "دعاء السوق", "Market Dua", AppIcons.Storefront),
    CategoryItem("sick_dua", "دعاء المريض", "Sick Dua", AppIcons.LocalHospital)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController, azkarViewModel: AzkarViewModel) {
    val counters by azkarViewModel.counters.collectAsState()

    // نبض خفيف للأزرار العائمة
    val infiniteTransition = rememberInfiniteTransition(label = "FabPulse")
    val fabScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "FabScale"
    )

    // حساب إجمالي الإنجاز
    val totalCompleted = remember(counters) {
        categoriesList.sumOf { azkarViewModel.getCategoryProgress(it.id, counters).first }
    }
    val totalCount = remember(counters) {
        categoriesList.sumOf { azkarViewModel.getCategoryProgress(it.id, counters).second }
    }

    Scaffold(
        containerColor = ScreenBg,
        topBar = {
            TopAppBar(
                title = { 
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.app_logo),
                            contentDescription = "شعار التطبيق",
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .border(1.dp, PrimaryGold.copy(alpha = 0.5f), CircleShape)
                        )
                        Text(
                            text = "نبض الذكر", 
                            fontWeight = FontWeight.Bold,
                            fontSize = 21.sp,
                            color = PrimaryGold
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { navController.navigate("search") }) {
                        Icon(imageVector = Icons.Filled.Search, contentDescription = "بحث", tint = TextPrimary)
                    }
                    IconButton(onClick = { navController.navigate("favorites") }) {
                        Icon(imageVector = Icons.Filled.Favorite, contentDescription = "المفضلة", tint = PrimaryGold)
                    }
                    IconButton(onClick = { navController.navigate("settings") }) {
                        Icon(imageVector = Icons.Filled.Settings, contentDescription = "الإعدادات", tint = TextSecondary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = HeaderBg
                )
            )
        },
        floatingActionButtonPosition = FabPosition.Center,
        floatingActionButton = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // على اليمين (بداية السطر في RTL): زر القبلة الذكية
                FloatingActionButton(
                    onClick = { navController.navigate("qibla") },
                    shape = RoundedCornerShape(24.dp),
                    containerColor = CardBg,
                    contentColor = PrimaryGold,
                    modifier = Modifier
                        .graphicsLayer {
                            scaleX = fabScale
                            scaleY = fabScale
                        }
                        .border(1.5.dp, PrimaryGold, RoundedCornerShape(24.dp))
                        .shadow(12.dp, RoundedCornerShape(24.dp), spotColor = PrimaryGold)
                ) {
                    // أيقونة بوصلة القبلة مع رمز الكعبة
                    Canvas(modifier = Modifier.size(28.dp)) {
                        val r = size.minDimension / 2.2f
                        drawCircle(
                            color = PrimaryGold,
                            radius = r,
                            style = Stroke(width = 2.dp.toPx())
                        )
                        // إبرة البوصلة الماسية باتجاه الأعلى
                        val needlePath = Path().apply {
                            moveTo(center.x, center.y - r * 0.72f)
                            lineTo(center.x + r * 0.24f, center.y)
                            lineTo(center.x, center.y + r * 0.55f)
                            lineTo(center.x - r * 0.24f, center.y)
                            close()
                        }
                        drawPath(needlePath, color = PrimaryGold.copy(alpha = 0.85f))
                        // مربع الكعبة في المنتصف
                        val kSize = 6.dp.toPx()
                        drawRoundRect(
                            color = ScreenBg,
                            topLeft = Offset(center.x - kSize / 2f, center.y - kSize / 2f),
                            size = Size(kSize, kSize),
                            cornerRadius = CornerRadius(1.5.dp.toPx())
                        )
                    }
                }

                // على الشمال (نهاية السطر في RTL): زر المسبحة
                FloatingActionButton(
                    onClick = { navController.navigate("tasbih") },
                    shape = RoundedCornerShape(24.dp),
                    containerColor = PrimaryGold,
                    contentColor = ScreenBg,
                    modifier = Modifier
                        .graphicsLayer {
                            scaleX = fabScale
                            scaleY = fabScale
                        }
                        .shadow(12.dp, RoundedCornerShape(24.dp), spotColor = PrimaryGold)
                ) {
                    // دائرة بنقطة داكنة
                    Canvas(modifier = Modifier.size(26.dp)) {
                        drawCircle(
                            color = ScreenBg,
                            radius = size.minDimension / 2.3f,
                            style = Stroke(width = 2.5.dp.toPx())
                        )
                        drawCircle(
                            color = ScreenBg,
                            radius = 3.5.dp.toPx()
                        )
                    }
                }
            }
        }
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxSize()
                .background(ScreenGradient)
                .padding(padding),
            contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 100.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // بانر إيماني وإحصائي متطور في أعلى الصفحة
            item(span = { GridItemSpan(2) }) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBg),
                    border = BorderStroke(1.dp, PrimaryGold.copy(alpha = 0.25f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(CardGradient)
                            .padding(18.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "﴿ أَلَا بِذِكْرِ اللَّهِ تَطْمَئِنُّ الْقُلُوبُ ﴾",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryGold
                                )
                                Text(
                                    text = "$totalCompleted / $totalCount",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (totalCompleted > 0) CompletedGreen else TextSecondary
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            // شريط تقدم الإنجاز العام
                            val generalProgress = if (totalCount > 0) totalCompleted.toFloat() / totalCount.toFloat() else 0f
                            LinearProgressIndicator(
                                progress = { generalProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = if (generalProgress >= 1f) CompletedGreen else PrimaryGold,
                                trackColor = ProgressTrack
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (totalCompleted == 0) "ابدأ وردك اليومي بنبض قلبك بالذكر"
                                       else if (totalCompleted < totalCount) "واصل ذكر الله، تقبل الله طاعتك"
                                       else "مبارك! أتممت أذكار اليوم كاملاً",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }

            items(categoriesList, key = { it.id }) { item ->
                val progress = azkarViewModel.getCategoryProgress(item.id, counters)
                CategoryCard(
                    item = item,
                    progress = progress,
                    onClick = { navController.navigate("azkar/${item.id}") }
                )
            }
        }
    }
}
