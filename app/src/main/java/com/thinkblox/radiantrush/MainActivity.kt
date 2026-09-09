package com.thinkblox.radiantrush

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.thinkblox.radiantrush.ui.RadiantRushApp
import com.thinkblox.radiantrush.ui.theme.RadiantRushTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RadiantRushTheme {
                RadiantRushApp()
            }
        }
    }
}
