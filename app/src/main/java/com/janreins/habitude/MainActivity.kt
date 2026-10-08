package com.janreins.habitude

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.janreins.habitude.ui.navigation.HabitudeApp
import com.janreins.habitude.ui.theme.HabitudeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HabitudeTheme {
                HabitudeApp()
            }
        }
    }
}
