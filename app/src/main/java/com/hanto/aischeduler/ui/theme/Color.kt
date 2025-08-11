package com.hanto.aischeduler.ui.theme

import androidx.compose.ui.graphics.Color

// Light & Dark 공용
val PrimaryNavy = Color(0xFF1E3A8A)   // Light Mode 기본
val PrimaryBlueLight = Color(0xFF3B82F6)

val PrimaryBlueDark = Color(0xFF3B82F6) // Dark Mode 기본
val PrimaryBlueBright = Color(0xFF60A5FA)

// Light Mode
val LightBackground = Color(0xFFF9FAFB)
val LightSurface = Color(0xFFFFFFFF)
val LightTextPrimary = Color(0xFF111827)
val LightTextSecondary = Color(0xFF4B5563)

// Dark Mode
val DarkBackground = Color(0xFF0F172A)
val DarkSurface = Color(0xFF1E293B)
val DarkTextPrimary = Color(0xFFF1F5F9)
val DarkTextSecondary = Color(0xFF94A3B8)

// 기존 AppColors는 새로운 색상으로 매핑
object AppColors {
    val Primary = PrimaryNavy
    val PrimaryVariant = PrimaryBlueLight
    val Secondary = Color(0xFF10B981)
    val Warning = Color(0xFFF59E0B)
    val Background = LightBackground
    val Surface = LightSurface
    val OnSurface = LightTextPrimary
    val OnBackground = LightTextPrimary
    val TextSecondary = LightTextSecondary
    val Border = Color(0xFFE5E7EB)
}