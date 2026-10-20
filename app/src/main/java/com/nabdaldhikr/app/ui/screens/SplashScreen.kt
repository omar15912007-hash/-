package com.nabdaldhikr.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.nabdaldhikr.app.R
import com.nabdaldhikr.app.ui.theme.PrimaryGold
import com.nabdaldhikr.app.ui.theme.ScreenGradient
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(navController: NavController) {
    var visible by remember { mutableStateOf(false) }

    // حركة النبض الفاخرة للشعار
    val infiniteTransition = rememberInfiniteTransition(label = "PulseTransition")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseScale"
    )

    // حركة تنقل النقطة الرمادية
    val offsetY by infiniteTransition.animateFloat(
        initialValue = -10f,
        targetValue = 10f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "DotOffset"
    )

    LaunchedEffect(Unit) {
        visible = true
        delay(2400)
        navController.navigate("home") {
            popUpTo("splash") { inclusive = true }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ScreenGradient),
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(animationSpec = tween(900))
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Spacer(modifier = Modifier.weight(1f))

                // الشعار الرسمي بحجم 170dp مع تأثير النبض والظل المتوهج
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(170.dp)
                        .graphicsLayer {
                            scaleX = pulseScale
                            scaleY = pulseScale
                        }
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.app_logo),
                        contentDescription = "شعار نبض الذكر",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .shadow(24.dp, CircleShape, spotColor = PrimaryGold)
                    )
                }

                Spacer(modifier = Modifier.height(28.dp))

                // عنوان نبض الذكر ذهبي
                Text(
                    text = "نبض الذكر",
                    fontSize = 38.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryGold,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                // ليطمئن قلبك بذكر الله
                Text(
                    text = "ليطمئن قلبك بذكر الله",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFFE8D5A0)
                )

                Spacer(modifier = Modifier.height(44.dp))

                // مؤشر التحميل العصري: نقطة رمادية بحركة نطط
                Box(
                    modifier = Modifier
                        .graphicsLayer { translationY = offsetY * density }
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF808B86))
                )

                Spacer(modifier = Modifier.weight(1.2f))
            }
        }
    }
}
