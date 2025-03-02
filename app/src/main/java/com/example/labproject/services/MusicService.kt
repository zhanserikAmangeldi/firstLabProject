package com.example.labproject.services

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.example.labproject.R
import com.example.labproject.models.MusicTrack
import java.io.IOException

class MusicService : Service() {

    private val NOTIFICATION_ID = 1
    private val CHANNEL_ID = "music_channel"
    private val MUSIC_DIRECTORY = "music"

    private var mediaPlayer: MediaPlayer? = null
    private var musicTracks = mutableListOf<MusicTrack>()
    private var currentTrackIndex = 0
    private var isPlaying = false

    private val progressHandler = Handler(Looper.getMainLooper())
    private val updateProgressTask = object : Runnable {
        override fun run() {
            sendProgressUpdateBroadcast()
            progressHandler.postDelayed(this, 250)
        }
    }

    companion object {
        const val ACTION_TRACK_UPDATED = "ACTION_TRACK_UPDATED"
        const val ACTION_TRACK_LIST_UPDATED = "ACTION_TRACK_LIST_UPDATED"
        const val ACTION_TRACK_PROGRESS_UPDATED = "ACTION_TRACK_PROGRESS_UPDATED"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        loadMusicFiles()
        if (musicTracks.isNotEmpty()) {
            prepareMediaPlayer()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            "PLAY" -> {
                if (isPlaying) pauseMusic() else playMusic()
            }
            "PAUSE" -> pauseMusic()
            "NEXT" -> nextTrack()
            "PREVIOUS" -> previousTrack()
            "REFRESH" -> {
                val wasPlaying = isPlaying
                loadMusicFiles()
                currentTrackIndex = 0
                if (musicTracks.isNotEmpty()) {
                    prepareMediaPlayer()
                    startForeground(NOTIFICATION_ID, getNotification())
                    if (wasPlaying) {
                        mediaPlayer?.start()
                        startProgressUpdates()
                        isPlaying = true
                    }
                } else {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                }
                sendTrackUpdateBroadcast()
                sendTrackListBroadcast()
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        stopProgressUpdates()
        mediaPlayer?.release()
        mediaPlayer = null
        isPlaying = false
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun loadMusicFiles() {
        try {
            musicTracks.clear()
            val files = assets.list(MUSIC_DIRECTORY) ?: return
            files.filter { it.endsWith(".mp3") }.forEach { filename ->
                val title = filename.substringBeforeLast(".")
                    .replace("_", " ")
                    .split(" ")
                    .joinToString(" ") { it.replaceFirstChar(Char::titlecase) }
                musicTracks.add(MusicTrack(title, "$MUSIC_DIRECTORY/$filename"))
            }

            if (musicTracks.isEmpty()) {
                Log.d("MusicService", "No music files found in assets/$MUSIC_DIRECTORY")
                Toast.makeText(this, "No music files found", Toast.LENGTH_SHORT).show()
            }
        } catch (e: IOException) {
            Log.e("MusicService", "Error loading music files", e)
            Toast.makeText(this, "Error loading music files", Toast.LENGTH_SHORT).show()
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Music Player",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Music Player Controls"
            setSound(null, null)
        }
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    private fun getNotification(): Notification {
        val flag = PendingIntent.FLAG_IMMUTABLE
        val playIntent = Intent(this, MusicService::class.java).apply { action = "PLAY" }
        val playPendingIntent = PendingIntent.getService(this, 0, playIntent, flag)
        val previousIntent = Intent(this, MusicService::class.java).apply { action = "PREVIOUS" }
        val previousPendingIntent = PendingIntent.getService(this, 3, previousIntent, flag)
        val nextIntent = Intent(this, MusicService::class.java).apply { action = "NEXT" }
        val nextPendingIntent = PendingIntent.getService(this, 4, nextIntent, flag)

        val currentTrack = musicTracks.getOrNull(currentTrackIndex) ?: MusicTrack("No tracks available", "")

        val playPauseIcon = if (isPlaying) R.drawable.ic_stop else R.drawable.ic_play
        val playPauseText = if (isPlaying) "Pause" else "Play"

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(if (musicTracks.isNotEmpty()) "Now Playing" else "No Track")
            .setContentText(if (musicTracks.isNotEmpty()) currentTrack.title else "No tracks in playlist")
            .setSmallIcon(R.drawable.ic_music_note)
            .addAction(R.drawable.ic_previous, "Previous", previousPendingIntent)
            .addAction(playPauseIcon, playPauseText, playPendingIntent)
            .addAction(R.drawable.ic_next, "Next", nextPendingIntent)
            .setStyle(androidx.media.app.NotificationCompat.MediaStyle().setShowActionsInCompactView(0, 1, 2))
            .setOngoing(isPlaying)
            .build()
    }

    @SuppressLint("ForegroundServiceType")
    private fun playMusic() {
        if (musicTracks.isEmpty()) {
            Toast.makeText(this, "No music files found", Toast.LENGTH_SHORT).show()
            return
        }
        if (mediaPlayer == null) {
            prepareMediaPlayer()
            startForeground(NOTIFICATION_ID, getNotification())
        }
        if (mediaPlayer?.isPlaying != true) {
            mediaPlayer?.start()
            startProgressUpdates()
            isPlaying = true
            updateNotification()
            sendTrackUpdateBroadcast()
        }
    }

    private fun pauseMusic() {
        mediaPlayer?.pause()
        stopProgressUpdates()
        isPlaying = false
        updateNotification()
        stopForeground(STOP_FOREGROUND_DETACH)
        sendTrackUpdateBroadcast()
    }

    private fun prepareMediaPlayer() {
        if (musicTracks.isEmpty()) return
        mediaPlayer?.release()
        mediaPlayer = MediaPlayer()
        try {
            val currentTrack = musicTracks[currentTrackIndex]
            val assetFileDescriptor = assets.openFd(currentTrack.filePath)
            mediaPlayer?.setDataSource(assetFileDescriptor.fileDescriptor, assetFileDescriptor.startOffset, assetFileDescriptor.length)
            assetFileDescriptor.close()
            mediaPlayer?.prepare()
            mediaPlayer?.setOnCompletionListener { nextTrack() }
        } catch (e: IOException) {
            Log.e("MusicService", "Error preparing media player", e)
            Toast.makeText(this, "Error playing track: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            if (musicTracks.size > 1) nextTrack() else stopSelf()
        }
    }

    private fun nextTrack() {
        if (musicTracks.isEmpty()) return
        stopProgressUpdates()
        currentTrackIndex = (currentTrackIndex + 1) % musicTracks.size
        prepareMediaPlayer()
        if (isPlaying) {
            mediaPlayer?.start()
            startProgressUpdates()
        }
        updateNotification()
        sendTrackUpdateBroadcast()
    }

    private fun previousTrack() {
        if (musicTracks.isEmpty()) return
        stopProgressUpdates()
        currentTrackIndex = if (currentTrackIndex > 0) currentTrackIndex - 1 else musicTracks.size - 1
        prepareMediaPlayer()
        if (isPlaying) {
            mediaPlayer?.start()
            startProgressUpdates()
        }
        updateNotification()
        sendTrackUpdateBroadcast()
    }

    private fun updateNotification() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, getNotification())
    }

    private fun sendTrackUpdateBroadcast() {
        if (musicTracks.isEmpty()) return
        val intent = Intent(ACTION_TRACK_UPDATED).apply {
            action = ACTION_TRACK_UPDATED
            putExtra("track_title", musicTracks[currentTrackIndex].title)
            putExtra("track_index", currentTrackIndex)
            putExtra("total_tracks", musicTracks.size)
            putExtra("is_playing", isPlaying)
        }
        intent.setPackage(packageName)
        sendBroadcast(intent)
    }

    private fun sendTrackListBroadcast() {
        val intent = Intent(ACTION_TRACK_UPDATED).apply {
            action = ACTION_TRACK_LIST_UPDATED
            putExtra("track_count", musicTracks.size)
            putExtra("track_titles", musicTracks.map { it.title }.toTypedArray())
        }
        intent.setPackage(packageName)
        sendBroadcast(intent)
    }

    private fun sendProgressUpdateBroadcast() {
        if (mediaPlayer != null) {
            val intent = Intent(ACTION_TRACK_UPDATED).apply {
                action = ACTION_TRACK_PROGRESS_UPDATED
                putExtra("current_position", mediaPlayer!!.currentPosition)
                putExtra("duration", mediaPlayer!!.duration)
            }
            intent.setPackage(packageName)
            sendBroadcast(intent)
        }
    }

    private fun startProgressUpdates() {
        progressHandler.post(updateProgressTask)
    }

    private fun stopProgressUpdates() {
        progressHandler.removeCallbacks(updateProgressTask)
    }
}