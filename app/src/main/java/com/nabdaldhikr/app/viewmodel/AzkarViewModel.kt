package com.nabdaldhikr.app.viewmodel

import android.app.Application
import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nabdaldhikr.app.data.model.AzkarCategory
import com.nabdaldhikr.app.data.repository.AzkarRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private val Context.azkarDataStore by preferencesDataStore(name = "azkar_progress")

class AzkarViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = AzkarRepository(application)
    private val dataStore = application.azkarDataStore

    private val COUNTERS_KEY = stringSetPreferencesKey("counters")
    private val FAVORITES_KEY = stringSetPreferencesKey("favorites")

    private val _categories = MutableStateFlow<List<AzkarCategory>>(emptyList())
    val categories: StateFlow<List<AzkarCategory>> = _categories.asStateFlow()

    // فهرس سريع O(1) بدل البحث الخطي O(N)
    private var categoryMap: Map<String, AzkarCategory> = emptyMap()

    // Key format: "categoryId_azkarId", value: current count
    private val _counters = MutableStateFlow<Map<String, Int>>(emptyMap())
    val counters: StateFlow<Map<String, Int>> = _counters.asStateFlow()

    private val _favorites = MutableStateFlow<Set<String>>(emptySet())
    val favorites: StateFlow<Set<String>> = _favorites.asStateFlow()

    // Debounce لمنع الكتابة المتكررة لـ DataStore عند كل نقرة
    private var persistJob: Job? = null
    private val PERSIST_DEBOUNCE_MS = 500L

    init {
        viewModelScope.launch {
            val loaded = repository.loadAzkar()
            _categories.value = loaded
            categoryMap = loaded.associateBy { it.id }

            val preferences = dataStore.data.first()

            _counters.value = preferences[COUNTERS_KEY]
                ?.mapNotNull { entry ->
                    val sepIdx = if (entry.contains(':')) entry.lastIndexOf(':') else entry.lastIndexOf('_')
                    if (sepIdx != -1) {
                        val key = entry.substring(0, sepIdx)
                        val count = entry.substring(sepIdx + 1).toIntOrNull() ?: 0
                        key to count
                    } else {
                        null
                    }
                }
                ?.toMap() ?: emptyMap()

            _favorites.value = preferences[FAVORITES_KEY]?.toSet() ?: emptySet()
        }
    }

    fun getCategory(id: String): AzkarCategory? = categoryMap[id]

    fun incrementCount(categoryId: String, azkarId: Int) {
        val key = "${categoryId}_$azkarId"
        val category = getCategory(categoryId) ?: return
        val azkar = category.azkarList.find { it.id == azkarId } ?: return

        val currentCount = _counters.value[key] ?: 0
        if (currentCount < azkar.repetition) {
            val newMap = _counters.value.toMutableMap()
            newMap[key] = currentCount + 1
            _counters.value = newMap
            debouncedPersist()
        }
    }

    fun resetCategory(categoryId: String) {
        val newMap = _counters.value.toMutableMap()
        val keysToRemove = newMap.keys.filter { it.startsWith("${categoryId}_") }
        keysToRemove.forEach { newMap.remove(it) }
        _counters.value = newMap
        persistNow()
    }

    fun getRemainingCount(categoryId: String, azkarId: Int, countersMap: Map<String, Int> = _counters.value): Int {
        val category = getCategory(categoryId) ?: return 0
        val azkar = category.azkarList.find { it.id == azkarId } ?: return 0
        val key = "${categoryId}_$azkarId"
        val currentCount = countersMap[key] ?: 0
        return maxOf(0, azkar.repetition - currentCount)
    }

    fun isCompleted(categoryId: String, azkarId: Int, countersMap: Map<String, Int> = _counters.value): Boolean {
        return getRemainingCount(categoryId, azkarId, countersMap) == 0
    }

    fun toggleFavorite(categoryId: String, azkarId: Int) {
        val key = "${categoryId}_$azkarId"
        val newFavorites = _favorites.value.toMutableSet()
        if (newFavorites.contains(key)) {
            newFavorites.remove(key)
        } else {
            newFavorites.add(key)
        }
        _favorites.value = newFavorites
        viewModelScope.launch {
            dataStore.edit { preferences ->
                preferences[FAVORITES_KEY] = _favorites.value
            }
        }
    }

    fun isFavorite(categoryId: String, azkarId: Int): Boolean {
        return _favorites.value.contains("${categoryId}_$azkarId")
    }

    fun getCategoryProgress(categoryId: String, countersMap: Map<String, Int> = _counters.value): Pair<Int, Int> {
        val category = getCategory(categoryId) ?: return Pair(0, 0)
        val total = category.azkarList.size
        var completed = 0
        category.azkarList.forEach { azkar ->
            val key = "${categoryId}_${azkar.id}"
            val currentCount = countersMap[key] ?: 0
            if (currentCount >= azkar.repetition) completed++
        }
        return Pair(completed, total)
    }

    // توحيد منطق الحفظ (DRY)
    private suspend fun saveCounters() {
        dataStore.edit { preferences ->
            preferences[COUNTERS_KEY] = _counters.value.map { (k, v) -> "$k:$v" }.toSet()
        }
    }

    private fun debouncedPersist() {
        persistJob?.cancel()
        persistJob = viewModelScope.launch {
            delay(PERSIST_DEBOUNCE_MS)
            saveCounters()
        }
    }

    private fun persistNow() {
        persistJob?.cancel()
        viewModelScope.launch { saveCounters() }
    }
}
