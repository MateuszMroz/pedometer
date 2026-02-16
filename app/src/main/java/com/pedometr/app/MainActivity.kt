package com.pedometr.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.pedometr.app.R
import com.pedometr.app.service.StepCounterService
import com.pedometr.app.ui.screens.HomeScreen
import com.pedometr.app.ui.theme.PedometrTheme
import com.pedometr.app.viewmodel.StepViewModel
import org.koin.androidx.compose.koinViewModel

class MainActivity : ComponentActivity() {

    companion object {
        private const val TAG = "MainActivity"
    }

    private var hasPermission by mutableStateOf(false)

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasPermission = isGranted
        if (isGranted) {
            startStepCounterService()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        checkAndRequestPermissions()
        
        setContent {
            PedometrTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (hasPermission) {
                        // ViewModel wstrzykiwany przez Koin
                        val viewModel = koinViewModel<StepViewModel>()
                        val uiState by viewModel.uiState.collectAsState()
                        
                        HomeScreen(
                            uiState = uiState,
                            onRefresh = { viewModel.refreshData() }
                        )
                    } else {
                        PermissionScreen(
                            onRequestPermission = {
                                requestActivityRecognitionPermission()
                            }
                        )
                    }
                }
            }
        }
    }

    private fun checkAndRequestPermissions() {
        hasPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACTIVITY_RECOGNITION
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

        if (hasPermission) {
            startStepCounterService()
        }
    }

    private fun requestActivityRecognitionPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            requestPermissionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
        } else {
            hasPermission = true
            startStepCounterService()
        }
    }

    private fun startStepCounterService() {
        Log.d(TAG, "Starting StepCounterService")
        try {
            val serviceIntent = Intent(this, StepCounterService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
                Log.d(TAG, "Foreground service started")
            } else {
                startService(serviceIntent)
                Log.d(TAG, "Service started")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error starting service", e)
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh nie jest już potrzebny tutaj - ViewModel używa Flow który automatycznie się odświeża
    }
}

@Composable
fun PermissionScreen(
    onRequestPermission: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.permission_required),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = stringResource(R.string.permission_rationale),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = onRequestPermission,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.grant_permission))
        }
    }
}

