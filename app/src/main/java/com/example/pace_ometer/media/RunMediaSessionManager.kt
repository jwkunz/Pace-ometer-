package com.example.pace_ometer.media

import android.content.Context
import android.os.Looper
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.SimpleBasePlayer
import androidx.media3.session.MediaSession
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture

/**
 * Holds an active MediaSession for the duration of a run so the phone routes a connected
 * Bluetooth headset's inline play/pause button to us -- exactly how Android already handles
 * media buttons, no custom BLE handling needed. onPlay/onPause map directly to resume/pause.
 */
class RunMediaSessionManager(
    private val context: Context,
    private val onPlay: () -> Unit,
    private val onPause: () -> Unit
) {
    private var player: RunPlayer? = null
    private var mediaSession: MediaSession? = null

    fun start() {
        val runPlayer = RunPlayer(onPlay, onPause)
        player = runPlayer
        mediaSession = MediaSession.Builder(context, runPlayer).build()
    }

    fun setPlaying(isPlaying: Boolean) {
        player?.updatePlaying(isPlaying)
    }

    fun stop() {
        mediaSession?.release()
        player?.release()
        mediaSession = null
        player = null
    }

    private class RunPlayer(
        private val onPlay: () -> Unit,
        private val onPause: () -> Unit
    ) : SimpleBasePlayer(Looper.getMainLooper()) {

        private var playWhenReady = true

        // SimpleBasePlayer requires a non-empty playlist whenever the playback state isn't
        // IDLE/ENDED; this run "session" isn't really playing media, so a single placeholder
        // item is enough to satisfy that and let the media button routing work.
        private val runMediaItem = MediaItemData.Builder("pace-ometer-run")
            .setMediaItem(MediaItem.Builder().setMediaId("pace-ometer-run").build())
            .build()

        override fun getState(): State =
            State.Builder()
                .setAvailableCommands(
                    Player.Commands.Builder()
                        .addAll(Player.COMMAND_PLAY_PAUSE, Player.COMMAND_GET_TIMELINE)
                        .build()
                )
                .setPlaybackState(Player.STATE_READY)
                .setPlayWhenReady(playWhenReady, Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST)
                .setPlaylist(listOf(runMediaItem))
                .setCurrentMediaItemIndex(0)
                .build()

        override fun handleSetPlayWhenReady(playWhenReady: Boolean): ListenableFuture<*> {
            this.playWhenReady = playWhenReady
            if (playWhenReady) onPlay() else onPause()
            invalidateState()
            return Futures.immediateVoidFuture()
        }

        fun updatePlaying(isPlaying: Boolean) {
            playWhenReady = isPlaying
            invalidateState()
        }
    }
}
