package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.example.data.SentinelRepository
import com.example.ui.screens.DashboardScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    private lateinit var repository: SentinelRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize the local persistent configurations repository
        repository = SentinelRepository(this)
        
        enableEdgeToEdge()
        
        setContent {
            MyApplicationTheme {
                DashboardScreen(
                    repository = repository,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
