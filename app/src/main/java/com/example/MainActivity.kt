package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import com.example.data.local.AppDatabase
import com.example.data.repository.DocumentRepository
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.ScannerViewModel
import com.example.ui.viewmodel.ScannerViewModelFactory
import com.example.ui.views.ScannerAppMainContent

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 1. Initialize client-side Room Secure database & standard Repository
        val database = AppDatabase.getDatabase(applicationContext)
        val repository = DocumentRepository(database.documentDao(), applicationContext)

        // 2. Instantiate master view model with factory provider
        val viewModelFactory = ScannerViewModelFactory(repository)
        val viewModel = ViewModelProvider(this, viewModelFactory)[ScannerViewModel::class.java]

        setContent {
            MyApplicationTheme {
                ScannerAppMainContent(viewModel = viewModel)
            }
        }
    }
}

