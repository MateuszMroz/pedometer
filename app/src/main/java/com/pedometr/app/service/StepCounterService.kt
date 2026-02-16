package com.pedometr.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.pedometr.app.MainActivity
import com.pedometr.app.R
import com.pedometr.app.data.StepRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import java.time.LocalDate

class StepCounterService : Service(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var stepSensor: Sensor? = null
    private lateinit var notificationManager: NotificationManager
    
    // Dependency Injection przez Koin (lazy injection)
    private val repository: StepRepository by inject()
    
    private val serviceScope = CoroutineScope(Dispatchers.Default + Job())
    
    private var initialStepCount = -1
    private var currentDaySteps = 0
    private var lastSavedDate: LocalDate? = null
    
    private val sharedPrefs by lazy {
        getSharedPreferences("pedometr_prefs", Context.MODE_PRIVATE)
    }

    companion object {
        private const val TAG = "StepCounterService"
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "step_counter_channel"
        private const val PREF_LAST_SENSOR_VALUE = "last_sensor_value"
        private const val PREF_LAST_DATE = "last_date"
        private const val PREF_TODAY_STEPS = "today_steps"
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service onCreate()")
        
        // Repository jest już wstrzyknięty przez Koin
        
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        createNotificationChannel()
        loadSavedState()
        
        // Uruchom okresowe czyszczenie starych danych
        startPeriodicCleanup()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Service onStartCommand()")
        
        // KLUCZOWE: Najpierw utwórz powiadomienie, potem startForeground
        val notification = createNotification(currentDaySteps)
        
        // Uruchom jako Foreground Service z odpowiednim typem dla Android 14+
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                // Android 14+ wymaga specyfikacji typu foreground service
                ServiceCompat.startForeground(
                    this,
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH
                )
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10-13
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH
                )
            } else {
                // Android 8-9
                startForeground(NOTIFICATION_ID, notification)
            }
            Log.d(TAG, "Foreground service started successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error starting foreground service", e)
            // Spróbuj bez typu dla starszych urządzeń
            startForeground(NOTIFICATION_ID, notification)
        }
        
        // Zarejestruj listener sensora
        stepSensor?.let {
            val registered = sensorManager.registerListener(
                this,
                it,
                SensorManager.SENSOR_DELAY_NORMAL
            )
            Log.d(TAG, "Sensor listener registered: $registered")
        } ?: Log.e(TAG, "Step sensor not available!")
        
        // START_STICKY = system zrestartuje serwis jeśli zostanie zabity
        return START_STICKY
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            if (it.sensor.type == Sensor.TYPE_STEP_COUNTER) {
                val steps = it.values[0].toInt()
                Log.d(TAG, "Sensor changed: $steps steps")
                handleStepCount(steps)
            }
        }
    }

    private fun handleStepCount(sensorStepCount: Int) {
        val today = LocalDate.now()
        
        // Sprawdź czy to nowy dzień
        if (lastSavedDate != today) {
            // Nowy dzień - zresetuj licznik
            initialStepCount = sensorStepCount
            currentDaySteps = 0
            lastSavedDate = today
            saveState()
        } else {
            // Ten sam dzień - oblicz różnicę
            if (initialStepCount == -1) {
                initialStepCount = sensorStepCount
            }
            currentDaySteps = sensorStepCount - initialStepCount
            
            // Zabezpieczenie przed ujemnymi wartościami (restart urządzenia)
            if (currentDaySteps < 0) {
                initialStepCount = sensorStepCount
                currentDaySteps = 0
            }
        }
        
        // Zapisz do bazy danych
        serviceScope.launch {
            repository.updateStepsForToday(currentDaySteps)
            saveState()
        }
        
        // Aktualizuj powiadomienie
        updateNotification(currentDaySteps)
    }

    private fun loadSavedState() {
        val savedDate = sharedPrefs.getString(PREF_LAST_DATE, null)
        val today = LocalDate.now().toString()
        
        if (savedDate == today) {
            // Ten sam dzień - przywróć stan
            initialStepCount = sharedPrefs.getInt(PREF_LAST_SENSOR_VALUE, -1)
            currentDaySteps = sharedPrefs.getInt(PREF_TODAY_STEPS, 0)
            lastSavedDate = LocalDate.parse(savedDate)
        } else {
            // Nowy dzień - zresetuj
            initialStepCount = -1
            currentDaySteps = 0
            lastSavedDate = LocalDate.now()
        }
    }

    private fun saveState() {
        sharedPrefs.edit().apply {
            putInt(PREF_LAST_SENSOR_VALUE, initialStepCount)
            putInt(PREF_TODAY_STEPS, currentDaySteps)
            putString(PREF_LAST_DATE, LocalDate.now().toString())
            apply()
        }
    }

    private fun startPeriodicCleanup() {
        serviceScope.launch {
            while (isActive) {
                delay(24 * 60 * 60 * 1000L) // Raz dziennie
                repository.cleanOldData()
            }
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.notification_channel_description)
            setShowBadge(false)
        }
        notificationManager.createNotificationChannel(channel)
    }

    private fun createNotification(steps: Int): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_text, steps))
            .setSmallIcon(android.R.drawable.ic_menu_directions)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    private fun updateNotification(steps: Int) {
        notificationManager.notify(NOTIFICATION_ID, createNotification(steps))
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Nie potrzebujemy obsługi zmian dokładności
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Service onDestroy()")
        
        sensorManager.unregisterListener(this)
        
        // Zapisz stan przed zniszczeniem
        serviceScope.launch {
            try {
                repository.updateStepsForToday(currentDaySteps)
                saveState()
                Log.d(TAG, "State saved on destroy: $currentDaySteps steps")
            } catch (e: Exception) {
                Log.e(TAG, "Error saving state on destroy", e)
            }
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        Log.d(TAG, "Task removed - restarting service")
        
        // Zapisz stan
        saveState()
        
        // Restartuj serwis po usunięciu z listy zadań
        val restartServiceIntent = Intent(applicationContext, this::class.java)
        val pendingIntent = PendingIntent.getService(
            applicationContext,
            1,
            restartServiceIntent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
        alarmManager.set(
            android.app.AlarmManager.ELAPSED_REALTIME_WAKEUP,
            android.os.SystemClock.elapsedRealtime() + 1000,
            pendingIntent
        )
    }

    override fun onBind(intent: Intent?): IBinder? = null
}

