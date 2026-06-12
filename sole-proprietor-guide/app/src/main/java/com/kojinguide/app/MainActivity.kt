package com.kojinguide.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.kojinguide.app.ui.KojinGuideApp
import com.kojinguide.app.ui.theme.KojinGuideTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            KojinGuideTheme {
                KojinGuideApp()
            }
        }
    }
}
