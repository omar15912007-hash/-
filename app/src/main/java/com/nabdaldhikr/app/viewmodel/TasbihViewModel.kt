package com.nabdaldhikr.app.viewmodel

import android.app.Application
import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class TasbihDhikr(
    val id: String,
    val text: String,
    val target: Int,
    val count: Int,
    val isCustom: Boolean = false
)

private val Context.tasbihDataStore by preferencesDataStore(name = "tasbih_v2")

class TasbihViewModel(application: Application) : AndroidViewModel(application) {
    private val dataStore = application.tasbihDataStore
    private val gson = Gson()

    private val TASBIH_DATA_KEY = stringPreferencesKey("tasbih_items_json")
    private val SELECTED_INDEX_KEY = stringPreferencesKey("selected_index")

    private var persistJob: Job? = null
    private val PERSIST_DEBOUNCE_MS = 500L

    private val defaultDhikrs = listOf(
        TasbihDhikr("b1", "سبحان الله", 33, 0),
        TasbihDhikr("b2", "الحمد لله", 33, 0),
        TasbihDhikr("b3", "الله أكبر", 33, 0),
        TasbihDhikr("b4", "لا إله إلا الله", 33, 0),
        TasbihDhikr("b5", "أستغفر الله", 33, 0),
        TasbihDhikr("b6", "سبحان الله وبحمده", 33, 0)
    )

    private val _dhikrList = MutableStateFlow<List<TasbihDhikr>>(defaultDhikrs)
    val dhikrList: StateFlow<List<TasbihDhikr>> = _dhikrList.asStateFlow()

    private val _selectedIndex = MutableStateFlow(0)
    val selectedIndex: StateFlow<Int> = _selectedIndex.asStateFlow()

    init {
        viewModelScope.launch {
            val prefs = dataStore.data.first()
            val json = prefs[TASBIH_DATA_KEY]
            if (!json.isNullOrBlank()) {
                try {
                    val listType = object : TypeToken<List<TasbihDhikr>>() {}.type
                    val loaded: List<TasbihDhikr> = gson.fromJson(json, listType)
                    if (loaded.isNotEmpty()) {
                        _dhikrList.value = loaded
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            val savedIdx = prefs[SELECTED_INDEX_KEY]?.toIntOrNull() ?: 0
            _selectedIndex.value = savedIdx.coerceIn(0, maxOf(0, _dhikrList.value.lastIndex))
        }
    }

    fun increment() {
        val list = _dhikrList.value.toMutableList()
        val idx = _selectedIndex.value.coerceIn(0, list.lastIndex)
        val item = list[idx]

        if (item.count < item.target) {
            list[idx] = item.copy(count = item.count + 1)
            _dhikrList.value = list
            debouncedPersist()
        }
    }

    fun reset() {
        val list = _dhikrList.value.toMutableList()
        val idx = _selectedIndex.value.coerceIn(0, list.lastIndex)
        list[idx] = list[idx].copy(count = 0)
        _dhikrList.value = list
        persistNow()
    }

    fun selectDhikr(index: Int) {
        if (index in _dhikrList.value.indices) {
            _selectedIndex.value = index
            viewModelScope.launch {
                dataStore.edit { prefs ->
                    prefs[SELECTED_INDEX_KEY] = index.toString()
                }
            }
        }
    }

    fun addCustomDhikr(text: String, target: Int) {
        val cleanText = text.trim()
        val validTarget = if (target > 0) target else 33
        if (cleanText.isBlank()) return

        val newDhikr = TasbihDhikr(
            id = "c_${System.currentTimeMillis()}",
            text = cleanText,
            target = validTarget,
            count = 0,
            isCustom = true
        )
        val updated = _dhikrList.value + newDhikr
        _dhikrList.value = updated
        _selectedIndex.value = updated.lastIndex
        persistNow()
    }

    // توحيد منطق الحفظ (DRY)
    private suspend fun saveState() {
        dataStore.edit { prefs ->
            prefs[TASBIH_DATA_KEY] = gson.toJson(_dhikrList.value)
            prefs[SELECTED_INDEX_KEY] = _selectedIndex.value.toString()
        }
    }

    private fun debouncedPersist() {
        persistJob?.cancel()
        persistJob = viewModelScope.launch {
            delay(PERSIST_DEBOUNCE_MS)
            saveState()
        }
    }

    private fun persistNow() {
        persistJob?.cancel()
        viewModelScope.launch { saveState() }
    }
}