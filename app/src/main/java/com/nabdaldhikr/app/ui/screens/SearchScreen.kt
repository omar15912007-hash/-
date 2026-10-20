package com.nabdaldhikr.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
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

private data class SearchResult(
    val categoryId: String,
    val categoryName: String,
    val azkar: Azkar,
    val indexInCategory: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    navController: NavController,
    azkarViewModel: AzkarViewModel
) {
    val categories by azkarViewModel.categories.collectAsState()
    var query by remember { mutableStateOf("") }

    // تجريد التشكيل وتطبيع الهمزات للبحث العربي
    val tashkeelRegex = remember { Regex("[\u0610-\u061A\u064B-\u065F\u0670\u06D6-\u06DC\u06DF-\u06E4\u06E7\u06E8\u06EA-\u06ED]") }
    fun normalize(text: String): String {
        return tashkeelRegex.replace(text, "")
            .replace('إ', 'ا').replace('أ', 'ا').replace('آ', 'ا').replace('ٱ', 'ا')
            .replace('ة', 'ه')
    }

    val results = remember(categories, query) {
        if (query.isBlank()) {
            emptyList()
        } else {
            val q = normalize(query.trim())
            buildList {
                categories.forEach { category ->
                    category.azkarList.forEachIndexed { index, azkar ->
                        if (normalize(azkar.text).contains(q) || normalize(azkar.source).contains(q)) {
                            add(SearchResult(category.id, category.nameAr, azkar, index))
                        }
                    }
                }
            }
        }
    }

    Scaffold(
        containerColor = ScreenBg,
        topBar = {
            TopAppBar(
                title = {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        placeholder = { Text("ابحث عن ذكر...", color = TextSecondary) },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = PrimaryGold) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryGold,
                            unfocusedBorderColor = ProgressTrack,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            cursorColor = PrimaryGold
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = HeaderBg
                )
            )
        }
    ) { padding ->
        if (query.isBlank()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = null,
                    tint = TextSecondary.copy(alpha = 0.4f),
                    modifier = Modifier.size(72.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "ابحث في الأذكار",
                    fontSize = 16.sp,
                    color = TextPrimary
                )
            }
        } else if (results.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "لا توجد نتائج",
                    fontSize = 16.sp,
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
                items(results, key = { "${it.categoryId}_${it.azkar.id}" }) { result ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                navController.navigate("azkar_detail/${result.categoryId}/${result.indexInCategory}")
                            },
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = CardBg),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = result.categoryName,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryGold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = result.azkar.text,
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