package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.*
import com.example.data.AppDatabase
import com.example.data.StartupRepository
import com.example.ui.screens.MainAppScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.StartupIdeaViewModel
import com.example.viewmodel.StartupIdeaViewModelFactory

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Local Room DB & Repository setup
        val database = AppDatabase.getDatabase(applicationContext)
        val repository = StartupRepository(database.startupIdeaDao())

        setContent {
            // Check system-level dark theme initially
            val systemDark = isSystemInDarkTheme()
            var isDarkTheme by remember { mutableStateOf(systemDark) }

            // ViewModel instantiation using the builder factory
            val viewModel: StartupIdeaViewModel by viewModels {
                StartupIdeaViewModelFactory(application, repository)
            }

            MyApplicationTheme(darkTheme = isDarkTheme) {
                MainAppScreen(
                    viewModel = viewModel,
                    isDarkTheme = isDarkTheme,
                    onThemeToggle = { target -> isDarkTheme = target }
                )
            }
        }
    }
}
