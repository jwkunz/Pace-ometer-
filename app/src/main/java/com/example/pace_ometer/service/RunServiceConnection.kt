package com.example.pace_ometer.service

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Binds to [RunTrackingService] on demand and exposes its [RunState] to the UI layer,
 * without requiring the UI to hold a direct service reference.
 */
class RunServiceConnection(private val context: Context) {

    private val _runState = MutableStateFlow(RunState())
    val runState: StateFlow<RunState> = _runState.asStateFlow()

    private var service: RunTrackingService? = null
    private var bound = false
    private var stateJob: Job? = null
    private val scope = CoroutineScope(SupervisorJob())

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val localBinder = binder as? RunTrackingService.LocalBinder ?: return
            service = localBinder.service
            stateJob = scope.launch { localBinder.service.runState.collect { _runState.value = it } }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            service = null
            stateJob?.cancel()
        }
    }

    fun bind() {
        if (bound) return
        val intent = Intent(context, RunTrackingService::class.java)
        bound = context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
    }

    fun unbind() {
        if (!bound) return
        context.unbindService(connection)
        bound = false
    }

    fun start() = sendAction(RunTrackingService.ACTION_START)
    fun pause() = sendAction(RunTrackingService.ACTION_PAUSE)
    fun resume() = sendAction(RunTrackingService.ACTION_RESUME)
    fun stop() = sendAction(RunTrackingService.ACTION_STOP)

    fun resetToIdle() {
        service?.resetToIdle()
    }

    private fun sendAction(action: String) {
        val intent = Intent(context, RunTrackingService::class.java).setAction(action)
        ContextCompat.startForegroundService(context, intent)
    }
}
