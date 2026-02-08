package com.example.expensetracker

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {

    private val viewModel: TransactionViewModel by viewModels { TransactionViewModel.Factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        enableEdgeToEdge()

        val permissions = arrayOf(
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.READ_SMS
        )

        val permissionsToRequest = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsToRequest.toTypedArray(), 101)
        }
        setContent {
            com.example.expensetracker.ui.theme.ExpenseTrackerTheme {
                val isSetupComplete by viewModel.isSetupComplete.collectAsState()
                if (isSetupComplete == null) {
                    Box(Modifier.fillMaxSize()) // Or a specific SplashScreen composable
                } else {
                    // Decide where to start
                    val startDest = if (isSetupComplete == true) Routs.Home else Routs.Setup

                    AppNavigation(
                        viewModel = viewModel,
                        startDestination = startDest
                    )
                }
            }
        }
    }
}