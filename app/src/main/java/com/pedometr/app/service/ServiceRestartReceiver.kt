package com.pedometr.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

/**
 * Receiver do automatycznego restartu serwisu po restarcie urządzenia
 * lub gdy serwis zostanie zabity przez system
 */
class ServiceRestartReceiver : BroadcastReceiver() {
    
    companion object {
        private const val TAG = "ServiceRestartReceiver"
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "Received intent: ${intent.action}")
        
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            "com.pedometr.app.RESTART_SERVICE" -> {
                Log.d(TAG, "Starting StepCounterService")
                startStepCounterService(context)
            }
        }
    }
    
    private fun startStepCounterService(context: Context) {
        try {
            val serviceIntent = Intent(context, StepCounterService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
            Log.d(TAG, "Service start requested successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error starting service", e)
        }
    }
}

