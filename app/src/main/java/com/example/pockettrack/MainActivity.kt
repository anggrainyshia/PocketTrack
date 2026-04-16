package com.example.pockettrack

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.platform.LocalView
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import com.example.pockettrack.ui.AppNavigation
import com.example.pockettrack.ui.SplashScreen
import com.example.pockettrack.ui.theme.PocketTrackTheme
import com.example.pockettrack.viewmodel.AppViewModel

class MainActivity : ComponentActivity() {
    private val vm: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val themeMode  = vm.themeMode.observeAsState("System").value
            val systemDark = isSystemInDarkTheme()
            val isDark = when (themeMode) {
                "Dark"  -> true
                "Light" -> false
                else    -> systemDark   // "System"
            }

            val view = LocalView.current
            if (!view.isInEditMode) {
                SideEffect {
                    val window = (view.context as Activity).window
                    WindowCompat.getInsetsController(window, view).apply {
                        isAppearanceLightStatusBars     = !isDark
                        isAppearanceLightNavigationBars = !isDark
                    }
                }
            }

            PocketTrackTheme(darkTheme = isDark) {
                var showSplash by remember { mutableStateOf(true) }
                if (showSplash) {
                    SplashScreen(onFinished = { showSplash = false })
                } else {
                    AppNavigation(vm)
                }
            }
        }
    }
}
