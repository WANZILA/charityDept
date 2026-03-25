package com.example.charityDept.presentation.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val darkScheme = darkColorScheme(
    primary       = DeepGreen,
    onPrimary     = SoftOffWhite,

    secondary     = GoldenOrange,
    onSecondary   = Black,

    tertiary      = LimeGreen,
    onTertiary    = Black,

    background    = Black,
    onBackground  = SoftOffWhite,
    surface       = Black,
    onSurface     = SoftOffWhite,

    error         = ErrorRed,
    onError       = OnErrorRed
)

private val lightScheme = lightColorScheme(
    primary       = DeepGreen,
    onPrimary     = SoftOffWhite,

    secondary     = GoldenOrange,
    onSecondary   = Black,

    tertiary      = LimeGreen,
    onTertiary    = Black,

    background   = LimeGreen,
    surface      = SoftOffWhite,
    onBackground = Black,
    onSurface    = Black,
//    background   = SoftOffWhite,
//    surface      = SoftOffWhite,
//    onBackground = Black,
//    onSurface    = Black,

    error         = ErrorRed,
    onError       = OnErrorRed
)

@Composable
fun CharityDeptTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> darkScheme
        else -> lightScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography ,
        content = content
    )
}

