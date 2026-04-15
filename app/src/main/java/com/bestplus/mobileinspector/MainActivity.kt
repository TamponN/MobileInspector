package com.bestplus.mobileinspector

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.bestplus.mobileinspector.ui.InspectorNavHost
import com.bestplus.mobileinspector.ui.theme.InspectorTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            InspectorTheme {
                InspectorNavHost()
            }
        }
    }
}
