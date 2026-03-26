package com.alvaronolasco.creditcardtracker.ui.theme

import androidx.compose.ui.graphics.Color

// New Design System Colors
val ForestGreen = Color(0xFF1E2C22)
val MintGreen = Color(0xFFD8ECE4)
val SoftLime = Color(0xFFB6D491)
val SoftGray = Color(0xFFF2F2F2)
val OffWhite = Color(0xFFF5F6F4)
val TextDark = Color(0xFF1A1A1A)
val TextGray = Color(0xFF757575)

// Functional Colors
val ErrorRed = Color(0xFFE57373)
val SuccessGreen = Color(0xFF81C784)

val ErrorLight = ErrorRed
val OnErrorLight = Color.White
val ErrorDark = ErrorRed
val OnErrorDark = Color.White

// Mapping to Material Colors (Legacy/Compat)
val PrimaryLight = ForestGreen
val OnPrimaryLight = Color.White
val PrimaryContainerLight = MintGreen
val OnPrimaryContainerLight = ForestGreen
val SecondaryLight = SoftLime
val OnSecondaryLight = ForestGreen
val SecondaryContainerLight = SoftLime.copy(alpha = 0.2f)
val OnSecondaryContainerLight = ForestGreen

val PrimaryDark = ForestGreen
val OnPrimaryDark = Color.White
val PrimaryContainerDark = ForestGreen.copy(alpha = 0.8f)
val OnPrimaryContainerDark = Color.White

val SecondaryDark = SoftLime
val OnSecondaryDark = ForestGreen
val SecondaryContainerDark = SoftLime.copy(alpha = 0.2f)
val OnSecondaryContainerDark = ForestGreen

// Neutral / Surface
val BackgroundLight = OffWhite
val OnBackgroundLight = TextDark
val SurfaceLight = Color.White
val OnSurfaceLight = TextDark
val SurfaceVariantLight = SoftGray
val OnSurfaceVariantLight = TextGray
val OutlineLight = Color(0xFFE0E0E0)

val BackgroundDark = Color(0xFF141A16)
val OnBackgroundDark = Color(0xFFE1E1E1)
val SurfaceDark = Color(0xFF1B241E)
val OnSurfaceDark = Color(0xFFE1E1E1)
val SurfaceVariantDark = Color(0xFF242F28)
val OnSurfaceVariantDark = Color(0xFFB0B0B0)
val OutlineDark = Color(0xFF2C3A31)

// Card Gradient Colors — Linear Gradients (top to bottom)

// Red Card Gradient
val CardRedLight = Color(0xFFFF4242)
val CardRedMid = Color(0xFF852424)
val CardRedDark = Color(0xFF531818)

// Yellow Card Gradient
val CardYellowLight = Color(0xFFFFFF42)
val CardYellowMid = Color(0xFF857D24)
val CardYellowDark = Color(0xFF535118)

// Blue Card Gradient
val CardBlueLight = Color(0xFF4265FF)
val CardBlueMid = Color(0xFF243D85)
val CardBlueDark = Color(0xFF181E53)

// Green Card Gradient
val CardGreenLight = Color(0xFF42FF45)
val CardGreenMid = Color(0xFF298524)
val CardGreenDark = Color(0xFF1F5318)

// Purple Card Gradient
val CardPurpleLight = Color(0xFFD342FF)
val CardPurpleMid = Color(0xFF682485)
val CardPurpleDark = Color(0xFF491853)

// Legacy card colors (kept for backward compatibility)
val CardBlue_Legacy = Color(0xFF34495E)
val CardGreen_Legacy = ForestGreen
val CardRed_Legacy = Color(0xFF7F4444)
val CardYellow_Legacy = Color(0xFF9E8E5E)
val CardPurple_Legacy = Color(0xFF5D4E75)
val CardOrange_Legacy = Color(0xFF8E6A4E)
val CardDark_Legacy = Color(0xFF1A1A1B)

// Card picker colors — use gradient-compatible values so CardGradients shows correct gradient
val CardBlue = CardBlueLight
val CardGreen = CardGreenLight
val CardRed = CardRedLight
val CardYellow = CardYellowLight
val CardPurple = CardPurpleLight
val CardOrange = Color(0xFFFF8C42)
val CardDark = Color(0xFF2C2C3A)


