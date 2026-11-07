package com.example.claimit.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Industrial Fintech Slate & Neutral Palette
val DarkSlateBackground = Color(0xFF0F1216)
val DarkSlateSurface = Color(0xFF171B21)
val DarkSlateSurfaceElevated = Color(0xFF202630)
val DarkBorderOutline = Color(0xFF2C3440)
val DarkBorderHighlight = Color(0xFF3F4A5C)

val LightSlateBackground = Color(0xFFF6F8FA)
val LightSlateSurface = Color(0xFFFFFFFF)
val LightSlateSurfaceElevated = Color(0xFFF1F5F9)
val LightBorderOutline = Color(0xFFE2E8F0)
val LightBorderHighlight = Color(0xFFCBD5E1)

// Primary Editorial Accents
val AccentCyanPrimary = Color(0xFF00E5FF)
val AccentCyanDark = Color(0xFF00838F)
val TextPrimaryDark = Color(0xFFF0F6FC)
val TextSecondaryDark = Color(0xFF8B949E)
val TextPrimaryLight = Color(0xFF0F172A)
val TextSecondaryLight = Color(0xFF475569)

// Status & Indicator Accents
val StatusLostCrimson = Color(0xFFFF4D4D)
val StatusFoundEmerald = Color(0xFF00E676)
val StatusPendingAmber = Color(0xFFFFB300)
val TagAiIndigo = Color(0xFF7C4DFF)

val StatusLostCrimsonBg: Color
    @Composable
    get() = if (MaterialTheme.colorScheme.background == DarkSlateBackground) Color(0xFF2A1517) else Color(0xFFFFEBEE)

val StatusFoundEmeraldBg: Color
    @Composable
    get() = if (MaterialTheme.colorScheme.background == DarkSlateBackground) Color(0xFF0D281E) else Color(0xFFE8F5E9)

val StatusPendingAmberBg: Color
    @Composable
    get() = if (MaterialTheme.colorScheme.background == DarkSlateBackground) Color(0xFF2B220B) else Color(0xFFFFF8E1)

val TagAiIndigoBg: Color
    @Composable
    get() = if (MaterialTheme.colorScheme.background == DarkSlateBackground) Color(0xFF1E1B38) else Color(0xFFEDE7F6)

