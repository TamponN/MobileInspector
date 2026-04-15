package com.bestplus.mobileinspector.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme = lightColorScheme(
    primary = Blue700,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    secondary = Orange500,
    onSecondary = androidx.compose.ui.graphics.Color.White,
    surface = SurfaceLight,
    background = SurfaceLight,
)

private val DarkColorScheme = darkColorScheme(
    primary = Blue200,
    onPrimary = androidx.compose.ui.graphics.Color.Black,
    secondary = Orange200,
    onSecondary = androidx.compose.ui.graphics.Color.Black,
    surface = SurfaceDark,
    background = SurfaceDark,
)

@Composable
fun InspectorTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content,
    )
}
