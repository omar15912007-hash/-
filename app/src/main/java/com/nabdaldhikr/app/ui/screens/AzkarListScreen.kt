package com.nabdaldhikr.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.nabdaldhikr.app.ui.theme.*
import com.nabdaldhikr.app.viewmodel.AzkarViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AzkarListScreen(
    categoryId: String,
    navController: NavController,
    azkarViewModel: AzkarViewModel
) {
    val category = azkarViewModel.getCategory(categoryId)
    val counters by azkarViewModel.counters.collectAsState()
    
    var showResetDialog by remember { mutableStateOf(false) }

    if (category == null) {
        return
    }

    val progress = azkarViewModel.getCategoryProgress(categoryId, counters)

    Scaffold(
        containerColor = ScreenBg,
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = category.nameAr, 
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    ) 
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
                    // العداد بصيغة "منجز / كلي" بمسافات (0 / 15) ذهبي
                    Text(
                        text = "${progress.first} / ${progress.second}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = if (progress.first == progress.second && progress.second > 0) CompletedGreen else PrimaryGold,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    // أيقونة تصفير عند أقصى اليسار
                    IconButton(onClick = { showResetDialog = true }) {
                        Icon(
                            imageVector = Icons.Filled.Refresh, 
                            contentDescription = "إعادة ضبط",
                            tint = TextSecondary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = HeaderBg
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(ScreenGradient)
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            itemsIndexed(category.azkarList, key = { _, azkar -> azkar.id }) { index, azkar ->
                val remaining = azkarViewModel.getRemainingCount(categoryId, azkar.id, counters)
                val isCompleted = remaining == 0
                
                // كروت بعرض كامل باستدارة 24dp وخلفية #17452F وتدرج زمردي فاخر
                Card(
                    onClick = {
                        // ضغط الكارت يفتح تفاصيل الذكر
                        navController.navigate("azkar_detail/$categoryId/$index")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = CardBg
                    ),
                    border = BorderStroke(
                        1.dp,
                        if (isCompleted) CompletedGreen.copy(alpha = 0.5f) else CardBorder
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = if (isCompleted) 4.dp else 2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(CardGradient)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                // نص الذكر بالتشكيل أبيض 18sp محاذاة يمين بحد أقصى 3 أسطر ثم "..."
                                Text(
                                    text = azkar.text,
                                    fontSize = 18.sp,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis,
                                    color = TextPrimary,
                                    lineHeight = 28.sp
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                // تحته المصدر ذهبي 12sp يمينًا
                                Text(
                                    text = azkar.source,
                                    fontSize = 12.sp,
                                    color = PrimaryGold
                                )
                            }
                            
                            Spacer(modifier = Modifier.width(16.dp))
                            
                            // على يسار الكارت شارة دائرية ذهبية ~56dp بداخلها رقم التكرار المتبقي داكن bold
                            // وعند اكتمال الذكر: الشارة تتحول دائرة خضراء #4CAF50 بعلامة صح داكنة
                            Surface(
                                shape = CircleShape,
                                color = if (isCompleted) CompletedGreen else PrimaryGold,
                                modifier = Modifier
                                    .size(56.dp)
                                    .shadow(6.dp, CircleShape, spotColor = if (isCompleted) CompletedGreen else PrimaryGold)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    if (isCompleted) {
                                        Icon(
                                            imageVector = Icons.Filled.Check,
                                            contentDescription = "مكتمل",
                                            tint = ScreenBg,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    } else {
                                        Text(
                                            text = remaining.toString(),
                                            fontWeight = FontWeight.Bold,
                                            color = ScreenBg,
                                            fontSize = 20.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showResetDialog) {
            Dialog(
                onDismissRequest = { showResetDialog = false },
                properties = DialogProperties(
                    dismissOnBackPress = true,
                    dismissOnClickOutside = true,
                    usePlatformDefaultWidth = false
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.6f))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { showResetDialog = false },
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth(0.88f)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { /* يمنع تمرير النقر للخلفية */ },
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = CardBg),
                        border = BorderStroke(1.dp, PrimaryGold.copy(alpha = 0.25f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .background(CardGradient)
                                .padding(24.dp)
                        ) {
                            Text(
                                text = "إعادة ضبط الأذكار",
                                color = PrimaryGold,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            Text(
                                text = "هل أنت متأكد من تصفير جميع العدادات لهذه الفئة؟",
                                color = Color(0xFFD1D5DB),
                                fontSize = 15.sp,
                                lineHeight = 22.sp
                            )
                            
                            Spacer(modifier = Modifier.height(24.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // زر إلغاء محايد (يظهر أولاً من اليمين في اتجاه RTL)
                                TextButton(
                                    onClick = { showResetDialog = false },
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        text = "إلغاء",
                                        color = Color(0xFF9CA3AF),
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                
                                Spacer(modifier = Modifier.width(8.dp))
                                
                                // زر تصفير تدميري (يظهر يساراً في اتجاه RTL)
                                Button(
                                    onClick = {
                                        azkarViewModel.resetCategory(categoryId)
                                        showResetDialog = false
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFFB3261E).copy(alpha = 0.22f),
                                        contentColor = Color(0xFFFF6B6B)
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, Color(0xFFFF6B6B).copy(alpha = 0.45f))
                                ) {
                                    Text(
                                        text = "تصفير",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = Color(0xFFFF6B6B)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
