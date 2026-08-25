package com.example.pace_ometer.service

import android.app.Service
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.SensorManager
import android.location.Location
import android.os.Binder
import android.os.IBinder
import androidx.core.content.ContextCompat
import com.example.pace_ometer.PaceometerApp
import com.example.pace_ometer.calories.CalorieEstimator
import com.example.pace_ometer.data.db.entity.RunSampleEntity
import com.example.pace_ometer.data.repository.RunRepository
import com.example.pace_ometer.data.settings.SettingsRepository
import com.example.pace_ometer.data.settings.UnitSystem
import com.example.pace_ometer.data.settings.UserSettings
import com.example.pace_ometer.media.RunMediaSessionManager
import com.example.pace_ometer.sensors.ble.HeartRateGattSensor
import com.example.pace_ometer.sensors.fusion.DistanceFusionEngine
import com.example.pace_ometer.sensors.fusion.FusedPoint
import com.example.pace_ometer.sensors.fusion.GpsFix
import com.example.pace_ometer.sensors.location.LocationTracker
import com.example.pace_ometer.sensors.motion.StepDetector
import com.example.pace_ometer.tts.AnnouncementContentBuilder
import com.example.pace_ometer.tts.AnnouncementScheduler
import com.example.pace_ometer.tts.AnnouncementSnapshot
import com.example.pace_ometer.tts.TtsAnnouncer
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
    private lateinit var stepDetector: StepDetector

    private val _runState = MutableStateFlow(RunState())
    val runState: StateFlow<RunState> = _runState

    private var locationJob: Job? = null
    private var stepJob: Job? = null
    private var heartRateJob: Job? = null
    private var tickerJob: Job? = null
    private var settingsJob: Job? = null
    private var lastTickElapsedRealtime: Long = 0
    private var lastElevationMeters: Double? = null

    private var fusionEngine = DistanceFusionEngine()
    private var heartRateSensor: HeartRateGattSensor? = null
    private var heartRateSum: Long = 0
    private var heartRateCount: Int = 0
    private var heartRateMax: Int? = null

    private var currentSettings: UserSettings = UserSettings()
    private var ttsAnnouncer: TtsAnnouncer? = null
    private var mediaSessionManager: RunMediaSessionManager? = null
    private var announcementScheduler: AnnouncementScheduler? = null
    private var segmentStartTimeMs: Long = 0
    private var segmentStartDistanceMeters: Double = 0.0
    private var segmentStartElevationMeters: Double? = null

    override fun onCreate() {
        super.onCreate()
        val app = application as PaceometerApp
        runRepository = app.runRepository
        settingsRepository = app.settingsRepository
        locationTracker = LocationTracker(LocationServices.getFusedLocationProviderClient(this))
        stepDetector = StepDetector(getSystemService(SensorManager::class.java))
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
            fusionEngine = DistanceFusionEngine()
            lastElevationMeters = null
            heartRateSum = 0
            heartRateCount = 0
            heartRateMax = null
            currentSettings = settings
            segmentStartTimeMs = System.currentTimeMillis()
            segmentStartDistanceMeters = 0.0
            segmentStartElevationMeters = null
            announcementScheduler = AnnouncementScheduler(announcementIntervalMeters(settings))
            ttsAnnouncer = TtsAnnouncer(this@RunTrackingService)
            mediaSessionManager = RunMediaSessionManager(
                context = this@RunTrackingService,
                onPlay = { resumeRun() },
                onPause = { pauseRun() }
            ).also { it.start() }
            lastTickElapsedRealtime = android.os.SystemClock.elapsedRealtime()
            startForeground(
                RunNotificationFactory.NOTIFICATION_ID,
                RunNotificationFactory.build(this@RunTrackingService, RunPhase.RUNNING, "0.00 km", "0:00")
            )
            beginLocationUpdates()
            beginStepUpdates()
            beginTicker()
            beginSettingsUpdates()
            settings.heartRateDeviceAddress?.let { beginHeartRateMonitoring(it) }
        }
    }

    /**
     * Keeps [currentSettings] (and anything derived from it, like the announcement interval)
     * live for the duration of the run, instead of frozen at whatever was configured at Start --
     * changes made in Settings mid-run now take effect on the very next tick/announcement.
     */
    private fun beginSettingsUpdates() {
        settingsJob = serviceScope.launch {
            settingsRepository.userSettings.collect { settings ->
                val newIntervalMeters = announcementIntervalMeters(settings)
                if (newIntervalMeters != announcementIntervalMeters(currentSettings)) {
                    announcementScheduler?.updateInterval(newIntervalMeters, _runState.value.distanceMeters)
                }
                currentSettings = settings
            }
        }
    }

    private fun announcementIntervalMeters(settings: UserSettings): Double =
        settings.announcementIntervalValue *
            if (settings.announcementIntervalUnit == UnitSystem.IMPERIAL) 1609.344 else 1000.0

    private fun pauseRun() {
        if (_runState.value.phase != RunPhase.RUNNING) return
        _runState.value = _runState.value.copy(phase = RunPhase.PAUSED)
        mediaSessionManager?.setPlaying(false)
        updateNotification()
    }

    private fun resumeRun() {
        if (_runState.value.phase != RunPhase.PAUSED) return
        lastTickElapsedRealtime = android.os.SystemClock.elapsedRealtime()
        _runState.value = _runState.value.copy(phase = RunPhase.RUNNING)
        mediaSessionManager?.setPlaying(true)
        updateNotification()
    }

    private fun stopRun() {
        val state = _runState.value
        if (state.phase == RunPhase.IDLE || state.runId == null) {
            stopSelf()
            return
        }
        locationJob?.cancel()
        stepJob?.cancel()
        heartRateJob?.cancel()
        tickerJob?.cancel()
        settingsJob?.cancel()
        heartRateSensor?.disconnect()
        heartRateSensor = null
        ttsAnnouncer?.shutdown()
        ttsAnnouncer = null
        mediaSessionManager?.stop()
        mediaSessionManager = null
        announcementScheduler = null
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
                        elevationLossMeters = state.elevationLossMeters,
                        avgHeartRateBpm = if (heartRateCount > 0) (heartRateSum / heartRateCount).toInt() else null,
                        maxHeartRateBpm = heartRateMax,
                        estimatedCalories = state.caloriesBurned
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

    private fun beginStepUpdates() {
        stepJob = serviceScope.launch {
            stepDetector.steps().collect { stepTimestampMs ->
                if (_runState.value.phase != RunPhase.RUNNING) return@collect
                if (fusionEngine.isInGpsGap(System.currentTimeMillis())) {
                    val fused = fusionEngine.onStepDetected(stepTimestampMs)
                    applyFusedPoint(fused)
                } else {
                    fusionEngine.onStepDuringGoodGps()
                }
            }
        }
    }

    private fun beginHeartRateMonitoring(deviceAddress: String) {
        val hasBluetoothConnect = ContextCompat.checkSelfPermission(
            this, android.Manifest.permission.BLUETOOTH_CONNECT
        ) == PackageManager.PERMISSION_GRANTED
        if (!hasBluetoothConnect) return

        val adapter = getSystemService(BluetoothManager::class.java)?.adapter ?: return
        val device = runCatching { adapter.getRemoteDevice(deviceAddress) }.getOrNull() ?: return

        val sensor = HeartRateGattSensor(applicationContext)
        heartRateSensor = sensor
        sensor.connect(device)
        heartRateJob = serviceScope.launch {
            sensor.readings.collect { reading ->
                heartRateSum += reading.bpm
                heartRateCount += 1
                heartRateMax = maxOf(heartRateMax ?: 0, reading.bpm)
                _runState.value = _runState.value.copy(heartRateBpm = reading.bpm)
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
                    val state = _runState.value
                    // Accumulate calories every second from the current pace, so both the on-screen
                    // total and the announced total stay continuously in sync (no separate per-segment calc).
                    val speedKmh = state.currentPaceSecPerKm?.takeIf { it > 0 }?.let { 3600.0 / it } ?: 0.0
                    val caloriesIncrement = CalorieEstimator.estimateSegmentCalories(
                        speedKmh, currentSettings.bodyWeightKg.toDouble(), delta
                    )
                    _runState.value = state.copy(
                        movingDurationMs = state.movingDurationMs + delta,
                        caloriesBurned = state.caloriesBurned + caloriesIncrement
                    )
                    updateNotification()
                }
            }
        }
    }

    private fun onNewLocation(location: Location) {
        val state = _runState.value
        if (state.phase != RunPhase.RUNNING || state.runId == null) return

        val fix = GpsFix(
            latitude = location.latitude,
            longitude = location.longitude,
            altitudeMeters = if (location.hasAltitude()) location.altitude else null,
            accuracyMeters = if (location.hasAccuracy()) location.accuracy else Float.MAX_VALUE,
            timestampMs = location.time
        )
        val fused = fusionEngine.onGpsFix(fix)
        applyFusedPoint(fused)
    }

    private fun applyFusedPoint(fused: FusedPoint) {
        val state = _runState.value
        if (state.phase != RunPhase.RUNNING || state.runId == null) return

        var gain = state.elevationGainMeters
        var loss = state.elevationLossMeters
        val previousElevation = lastElevationMeters
        if (fused.elevationMeters != null) {
            if (previousElevation != null) {
                val delta = fused.elevationMeters - previousElevation
                if (delta > 0) gain += delta else loss += -delta
            }
            lastElevationMeters = fused.elevationMeters
        }

        // Live pace over the segment in progress (since the last announcement, or run start) --
        // distinct from fused.instantaneousPaceSecPerKm, which is the continuously smoothed
        // "projected" pace used for the split-pace display/announcement.
        val segmentDurationMs = (fused.timestampMs - segmentStartTimeMs).coerceAtLeast(0)
        val segmentDistanceMeters = (fused.cumulativeDistanceMeters - segmentStartDistanceMeters).coerceAtLeast(0.0)
        val liveSegmentPace = if (segmentDistanceMeters > 0 && segmentDurationMs > 0) {
            (segmentDurationMs / 1000.0) / (segmentDistanceMeters / 1000.0)
        } else null

        val newState = state.copy(
            distanceMeters = fused.cumulativeDistanceMeters,
            currentPaceSecPerKm = fused.instantaneousPaceSecPerKm,
            segmentPaceSecPerKm = liveSegmentPace,
            elevationMeters = fused.elevationMeters ?: state.elevationMeters,
            elevationGainMeters = gain,
            elevationLossMeters = loss
        )
        _runState.value = newState

        val scheduler = announcementScheduler
        if (scheduler != null && scheduler.checkAndAdvance(fused.cumulativeDistanceMeters)) {
            announceSegment(fused, newState)
        }

        serviceScope.launch {
            runRepository.addSample(
                RunSampleEntity(
                    runId = state.runId!!,
                    timestampEpochMs = fused.timestampMs,
                    latitude = fused.latitude,
                    longitude = fused.longitude,
                    elevationMeters = fused.elevationMeters,
                    cumulativeDistanceMeters = fused.cumulativeDistanceMeters,
                    instantaneousPaceSecPerKm = fused.instantaneousPaceSecPerKm,
                    heartRateBpm = state.heartRateBpm,
                    sourceFlags = fused.sourceFlags
                )
            )
        }
    }

    /** Builds and speaks one announcement for the segment ending at [fused], and records its elevation change. */
    private fun announceSegment(fused: FusedPoint, state: RunState) {
        val now = fused.timestampMs

        val elevationChange = if (fused.elevationMeters != null && segmentStartElevationMeters != null) {
            fused.elevationMeters - segmentStartElevationMeters!!
        } else null

        _runState.value = _runState.value.copy(elevationChangeLastSegmentMeters = elevationChange)

        val snapshot = AnnouncementSnapshot(
            distanceMeters = fused.cumulativeDistanceMeters,
            elapsedDurationMs = state.movingDurationMs,
            elevationMeters = fused.elevationMeters,
            elevationChangeLastSegmentMeters = elevationChange,
            heartRateBpm = state.heartRateBpm,
            cadenceSpm = state.cadenceSpm,
            segmentPaceSecPerKm = state.segmentPaceSecPerKm,
            splitPaceSecPerKm = fused.instantaneousPaceSecPerKm,
            cumulativeCalories = state.caloriesBurned,
            clockTimeEpochMs = System.currentTimeMillis()
        )
        ttsAnnouncer?.speakAll(AnnouncementContentBuilder.build(currentSettings, snapshot))

        segmentStartTimeMs = now
        segmentStartDistanceMeters = fused.cumulativeDistanceMeters
        segmentStartElevationMeters = fused.elevationMeters ?: segmentStartElevationMeters
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
        heartRateSensor?.disconnect()
        serviceJob.cancel()
    }
}
