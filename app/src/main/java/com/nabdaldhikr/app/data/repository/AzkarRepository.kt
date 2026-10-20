package com.nabdaldhikr.app.data.repository

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.nabdaldhikr.app.data.model.AzkarCategory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AzkarRepository(private val context: Context) {
    @Volatile
    private var cachedCategories: List<AzkarCategory>? = null

    suspend fun loadAzkar(): List<AzkarCategory> {
        cachedCategories?.let { return it }

        return withContext(Dispatchers.IO) {
            try {
                val categories: List<AzkarCategory> = context.assets.open("azkar.json").bufferedReader().use { reader ->
                    val listType = object : TypeToken<List<AzkarCategory>>() {}.type
                    Gson().fromJson(reader, listType)
                }
                cachedCategories = categories
                categories
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        }
    }
}
