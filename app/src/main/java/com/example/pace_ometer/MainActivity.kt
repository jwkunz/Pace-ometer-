package com.example.pace_ometer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.pace_ometer.ui.navigation.PaceometerNavHost
import com.example.pace_ometer.ui.theme.PaceometerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PaceometerTheme {
                PaceometerNavHost()
            }
        }
    }
}
