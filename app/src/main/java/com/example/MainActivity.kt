package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.ui.screens.AgentHomeScreen
import com.example.ui.theme.ArenaPilotTheme

/** The 1.0.1 entry point deliberately never starts the legacy Battle automation. */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ArenaPilotTheme {
                AgentHomeScreen()
            }
        }
    }
}
