package com.nabdaldhikr.app.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nabdaldhikr.app.ui.screens.CategoryItem
import com.nabdaldhikr.app.ui.theme.*

@Composable
fun CategoryCard(
    item: CategoryItem,
    progress: Pair<Int, Int>, // completed, total
    onClick: () -> Unit
) {
    val isCompleted = progress.second > 0 && progress.first == progress.second
    val progressFloat = if (progress.second > 0) progress.first.toFloat() / progress.second.toFloat() else 0f

    // شريط تقدم انسيابي متحرك
    val animatedProgress by animateFloatAsState(
        targetValue = progressFloat,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "ProgressBarAnimation"
    )

    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = CardBg
        ),
        border = BorderStroke(
            1.dp,
            if (isCompleted) CompletedGreen.copy(alpha = 0.6f) else CardBorder
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isCompleted) 4.dp else 2.dp
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(CardGradient)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // حاوية أيقونة أنيقة
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(
                            if (isCompleted) CompletedGreen.copy(alpha = 0.12f)
                            else PrimaryGold.copy(alpha = 0.08f)
                        )
                ) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.nameAr,
                        tint = if (isCompleted) CompletedGreen else PrimaryGold,
                        modifier = Modifier.size(34.dp)
                    )
                }

                // اسم الفئة أبيض bold مقاس 16sp
                Text(
                    text = item.nameAr,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    textAlign = TextAlign.Center,
                    color = TextPrimary,
                    maxLines = 2
                )

                Spacer(modifier = Modifier.height(8.dp))

                Column(modifier = Modifier.fillMaxWidth()) {
                    // شريط تقدم 4dp بعرض الكارت
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                    ) {
                        val trackColor = ProgressTrack
                        val fillColor = if (isCompleted) CompletedGreen else PrimaryGold
                        val strokeH = size.height
                        val cornerRadius = CornerRadius(strokeH / 2, strokeH / 2)

                        // مسار شريط التقدم الفارغ #2E5B4A
                        drawRoundRect(
                            color = trackColor,
                            size = size,
                            cornerRadius = cornerRadius
                        )

                        if (animatedProgress > 0f) {
                            val fillWidth = size.width * animatedProgress
                            drawRoundRect(
                                color = fillColor,
                                topLeft = Offset(x = size.width - fillWidth, y = 0f),
                                size = Size(width = fillWidth, height = strokeH),
                                cornerRadius = cornerRadius
                            )
                        } else {
                            // عند صفر نقطة ذهبية صغيرة عند الطرف اليمين فقط
                            drawCircle(
                                color = PrimaryGold,
                                radius = strokeH / 2,
                                center = Offset(x = size.width - (strokeH / 2), y = strokeH / 2)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // العداد بصيغة "منجز/كلي" بدون مسافات (0/15) بمحاذاة الطرف الشمال مقاس 12sp
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isCompleted) {
                            Text(
                                text = "مكتمل ✓",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = CompletedGreen
                            )
                        } else {
                            Spacer(modifier = Modifier.width(1.dp))
                        }

                        Text(
                            text = "${progress.first}/${progress.second}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isCompleted) CompletedGreen else TextSecondary,
                            modifier = Modifier.align(Alignment.CenterVertically)
                        )
                    }
                }
            }
        }
    }
}
