package com.nabdaldhikr.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.nabdaldhikr.app.data.model.Azkar
import com.nabdaldhikr.app.ui.theme.*
import com.nabdaldhikr.app.viewmodel.AzkarViewModel

data class FavoriteItem(
    val categoryId: String,
    val azkar: Azkar,
    val categoryName: String,
    val indexInCategory: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    navController: NavController,
    azkarViewModel: AzkarViewModel
) {
    val favorites by azkarViewModel.favorites.collectAsState()
    val categories by azkarViewModel.categories.collectAsState()

    val favoriteItems = remember(favorites, categories) {
        if (favorites.isEmpty()) emptyList()
        else buildList {
            categories.forEach { category ->
                category.azkarList.forEachIndexed { index, azkar ->
                    val key = "${category.id}_${azkar.id}"
                    if (favorites.contains(key)) {
                        add(FavoriteItem(category.id, azkar, category.nameAr, index))
                    }
                }
            }
        }
    }

    Scaffold(
        containerColor = ScreenBg,
        topBar = {
            TopAppBar(
                title = { Text("المفضلة", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = HeaderBg,
                    titleContentColor = PrimaryGold,
                    navigationIconContentColor = TextPrimary
                )
            )
        }
    ) { padding ->
        if (favoriteItems.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Filled.Favorite,
                    contentDescription = null,
                    tint = TextSecondary.copy(alpha = 0.4f),
                    modifier = Modifier.size(72.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "لا توجد أذكار مفضلة بعد",
                    fontSize = 16.sp,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "أضف الأذكار إلى المفضلة من شاشة عرض الذكر",
                    fontSize = 14.sp,
                    color = TextSecondary
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(favoriteItems, key = { "${it.categoryId}_${it.azkar.id}" }) { item ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                navController.navigate("azkar_detail/${item.categoryId}/${item.indexInCategory}")
                            },
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = CardBg),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = item.categoryName,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryGold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = item.azkar.text,
                                fontSize = 16.sp,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis,
                                color = TextPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}
