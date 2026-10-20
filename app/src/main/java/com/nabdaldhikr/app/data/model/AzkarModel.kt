package com.nabdaldhikr.app.data.model

data class AzkarCategory(
    val id: String,
    val nameAr: String,
    val nameEn: String,
    val azkarList: List<Azkar>
)

data class Azkar(
    val id: Int,
    val text: String,
    val translation: String = "",
    val source: String,
    val sourceEn: String = "",
    val hadithNumber: String = "",
    val virtue: String = "",
    val virtueEn: String = "",
    val repetition: Int = 1
)
