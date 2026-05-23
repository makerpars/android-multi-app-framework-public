package com.parsfilo.contentapp.core.model

/**
 * Namaz Suresi veya Duası modeli
 * namazsurelerivedualarsesli flavor'ında kullanılır
 */
data class Prayer(
    val sureID: Int,
    val sureAdiAR: String,
    val sureAdiEN: String,
    val sureAdiTR: String,
    val sureAdiDE: String = "",
    val ayetSayisi: Int,
    val sureMedya: String, // MP3 dosya adı (uppercase)
    val ayetler: List<PrayerVerse>
)

/**
 * Namaz/Dua içindeki ayet modeli
 */
data class PrayerVerse(
    val ayetID: Int,
    val ayetAR: String,    // Arapça metin
    val ayetLAT: String,   // Latin okunuş
    val ayetTR: String,    // Türkçe meal
    val ayetEN: String = "", // İngilizce çeviri (opsiyonel)
    val ayetDE: String = ""  // Almanca çeviri (opsiyonel)
)
