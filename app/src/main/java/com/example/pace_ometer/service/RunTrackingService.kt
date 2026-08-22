package com.example.pace_ometer.service

import android.app.Service
import android.content.Intent
import android.location.Location
import android.os.Binder
import android.os.IBinder
import com.example.pace_ometer.PaceometerApp
import com.example.pace_ometer.data.db.entity.RunSampleEntity
import com.example.pace_ometer.data.db.entity.SampleSource
import com.example.pace_ometer.data.repository.RunRepository
import com.example.pace_ometer.data.settings.SettingsRepository
import com.example.pace_ometer.sensors.location.LocationTracker
import com.example.pace_ometer.util.formatDistanceMeters
import com.example.pace_ometer.util.formatDurationMs
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class RunTrackingService : Service() {

    companion object {
        const val ACTION_START = "com.example.pace_ometer.action.START"
        const val ACTION_PAUSE = "com.example.pace_ometer.action.PAUSE"
        const val ACTION_RESUME = "com.example.pace_ometer.action.RESUME"
        const val ACTION_STOP = "com.example.pace_ometer.action.STOP"
    }

    inner class LocalBinder : Binder() {
        val service: RunTrackingService get() = this@RunTrackingService
    }

    private val binder = LocalBinder()
    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(serviceJob)

    private lateinit var runRepository: RunRepository
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var locationTracker: LocationTracker

    private val _runState = MutableStateFlow(RunState())
    val runState: StateFlow<RunState> = _runState

    private var locationJob: Job? = null
    private var tickerJob: Job? = null
    private var lastLocation: Location? = null
    private var lastTickElapsedRealtime: Long = 0

    override fun onCreate() {
        super.onCreate()
        val app = application as PaceometerApp
        runRepository = app.runRepository
        settingsRepository = app.settingsRepository
        locationTracker = LocationTracker(LocationServices.getFusedLocationProviderClient(this))
        RunNotificationFactory.ensureChannel(this)
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startRun()
            ACTION_PAUSE -> pauseRun()
            ACTION_RESUME -> resumeRun()
            ACTION_STOP -> stopRun()
        }
        return START_NOT_STICKY
    }

    private fun startRun() {
        if (_runState.value.phase != RunPhase.IDLE) return
        serviceScope.launch {
            val settings = settingsRepository.userSettings.first()
            val runId = runRepository.startRun(
                startTimeEpochMs = System.currentTimeMillis(),
                unitSystem = settings.unitSystem.name,
                bodyWeightKg = settings.bodyWeightKg.toDouble()
            )
            _runState.value = RunState(phase = RunPhase.RUNNING, runId = runId)
            lastLocation = null
            lastTickElapsedRealtime = android.os.SystemClock.elapsedRealtime()
            startForeground(
                RunNotificationFactory.NOTIFICATION_ID,
                RunNotificationFactory.build(this@RunTrackingService, RunPhase.RUNNING, "0.00 km", "0:00")
            )
            beginLocationUpdates()
            beginTicker()
        }
    }

    private fun pauseRun() {
        if (_runState.value.phase != RunPhase.RUNNING) return
        _runState.value = _runState.value.copy(phase = RunPhase.PAUSED)
        updateNotification()
    }

    private fun resumeRun() {
        if (_runState.value.phase != RunPhase.PAUSED) return
        lastTickElapsedRealtime = android.os.SystemClock.elapsedRealtime()
        _runState.value = _runState.value.copy(phase = RunPhase.RUNNING)
        updateNotification()
    }

    private fun stopRun() {
        val state = _runState.value
        if (state.phase == RunPhase.IDLE || state.runId == null) {
            stopSelf()
            return
        }
        locationJob?.cancel()
        tickerJob?.cancel()
        serviceScope.launch {
            val run = runRepository.getRun(state.runId)
            if (run != null) {
                val endTime = System.currentTimeMillis()
                val avgPace = if (state.distanceMeters > 0) {
                    (state.movingDurationMs / 1000.0) / (state.distanceMeters / 1000.0)
                } else null
                runRepository.updateRun(
                    run.copy(
                        endTimeEpochMs = endTime,
                        totalDistanceMeters = state.distanceMeters,
                        totalDurationMs = endTime - run.startTimeEpochMs,
                        movingDurationMs = state.movingDurationMs,
                        averagePaceSecPerKm = avgPace,
                        elevationGainMeters = state.elevationGainMeters,
                        elevationLossMeters = state.elevationLossMeters
                    )
                )
            }
            _runState.value = state.copy(phase = RunPhase.STOPPED)
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    /** Called by the UI after the stop-time save/discard prompt has been resolved. */
    fun resetToIdle() {
        _runState.value = RunState()
    }

    private fun beginLocationUpdates() {
        locationJob = serviceScope.launch {
            locationTracker.locationUpdates().collect { location ->
                onNewLocation(location)
            }
        }
    }

    private fun beginTicker() {
        tickerJob = serviceScope.launch {
            while (true) {
                delay(1000)
                if (_runState.value.phase == RunPhase.RUNNING) {
                    val now = android.os.SystemClock.elapsedRealtime()
                    val delta = now - lastTickElapsedRealtime
                    lastTickElapsedRealtime = now
                    _runState.value = _runState.value.copy(
                        movingDurationMs = _runState.value.movingDurationMs + delta
                    )
                    updateNotification()
                }
            }
        }
    }

    private fun onNewLocation(location: Location) {
        val state = _runState.value
        if (state.phase != RunPhase.RUNNING || state.runId == null) {
            lastLocation = location
            return
        }
        val previous = lastLocation
        lastLocation = location

        var newDistance = state.distanceMeters
        var pace = state.currentPaceSecPerKm
        var gain = state.elevationGainMeters
        var loss = state.elevationLossMeters

        if (previous != null) {
            val deltaMeters = previous.distanceTo(location).toDouble()
            val deltaSeconds = (location.time - previous.time) / 1000.0
            if (deltaMeters in 0.1..50.0 && deltaSeconds > 0) {
                newDistance += deltaMeters
                val speedMps = deltaMeters / deltaSeconds
                if (speedMps > 0.2) pace = (1000.0 / speedMps)
            }
            if (previous.hasAltitude() && location.hasAltitude()) {
                val elevationDelta = location.altitude - previous.altitude
                if (elevationDelta > 0) gain += elevationDelta else loss += -elevationDelta
            }
        }

        val newState = state.copy(
            distanceMeters = newDistance,
            currentPaceSecPerKm = pace,
            elevationMeters = if (location.hasAltitude()) location.altitude else state.elevationMeters,
            elevationGainMeters = gain,
            elevationLossMeters = loss
        )
        _runState.value = newState

        serviceScope.launch {
            runRepository.addSample(
                RunSampleEntity(
                    runId = state.runId,
                    timestampEpochMs = System.currentTimeMillis(),
                    latitude = location.latitude,
                    longitude = location.longitude,
                    elevationMeters = if (location.hasAltitude()) location.altitude else null,
                    cumulativeDistanceMeters = newDistance,
                    instantaneousPaceSecPerKm = pace,
                    sourceFlags = SampleSource.GPS_FIX
                )
            )
        }
    }

    private fun updateNotification() {
        val state = _runState.value
        val notification = RunNotificationFactory.build(
            this,
            state.phase,
            formatDistanceMeters(state.distanceMeters),
            formatDurationMs(state.movingDurationMs)
        )
        getSystemService(android.app.NotificationManager::class.java)
            .notify(RunNotificationFactory.NOTIFICATION_ID, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
    }
}
