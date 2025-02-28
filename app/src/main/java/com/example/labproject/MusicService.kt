package com.example.labproject

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.os.Build
import android.os.IBinder
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.core.app.NotificationCompat
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi
import java.io.IOException
import java.util.Locale

class MusicService : Service() {
    private var mediaPlayer: MediaPlayer? = null
    private val NOTIFICATION_ID = 101
    private val CHANNEL_ID = "music_channel"

    private val MUSIC_DIRECTORY = "music"

    private var musicTracks = mutableListOf<MusicTrack>()

    private var currentTrackIndex = 0

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        loadMusicFiles()
    }

    @OptIn(UnstableApi::class)
    private fun loadMusicFiles() {
        try {
            musicTracks.clear()

            val files = assets.list(MUSIC_DIRECTORY) ?: return

            files.filter {
                it.endsWith(".mp3") || it.endsWith(".wav") ||
                        it.endsWith(".ogg") || it.endsWith(".aac") ||
                        it.endsWith(".flac")
            }.forEach { filename ->
                val title = filename
                    .substringBeforeLast('.')
                    .replace("_", " ")
                    .split(" ")
                    .joinToString(" ") { word ->
                        word.replaceFirstChar {
                            if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
                        }
                    }

                musicTracks.add(MusicTrack(title, "$MUSIC_DIRECTORY/$filename"))
            }

            Log.d("MusicService", "Loaded ${musicTracks.size} music tracks")
        } catch (e: IOException) {
            Log.e("MusicService", "Error loading music files", e)
        }
    }

    @SuppressLint("ForegroundServiceType")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            "PLAY" -> {
                if (mediaPlayer?.isPlaying != true) {
                    playMusic()
                }
            }
            "PAUSE" -> pauseMusic()
            "NEXT" -> nextTrack()
            "PREVIOUS" -> previousTrack()
            "REFRESH" -> {
                loadMusicFiles()
                val wasPlaying = mediaPlayer?.isPlaying ?: false
                if (musicTracks.isNotEmpty()) {
                    currentTrackIndex = 0;
                    prepareMediaPlayer()

                    if (wasPlaying) {
                        mediaPlayer?.start()
                        startForeground(NOTIFICATION_ID, getNotification())
                    }
                }
                sendTrackUpdateBroadcast()
                sendTrackListBroadcast()

            }
        }
        return START_NOT_STICKY
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
        val flag =
            PendingIntent.FLAG_IMMUTABLE

        val playIntent = Intent(this, MusicService::class.java).apply {
            action = "PLAY"
        }
        val playPendingIntent = PendingIntent.getService(
            this, 0, playIntent, flag
        )

        val pauseIntent = Intent(this, MusicService::class.java).apply {
            action = "PAUSE"
        }
        val pausePendingIntent = PendingIntent.getService(
            this, 1, pauseIntent, flag
        )
        val previousIntent = Intent(this, MusicService::class.java).apply {
            action = "PREVIOUS"
        }
        val previousPendingIntent = PendingIntent.getService(
            this, 3, previousIntent, flag
        )

        val nextIntent = Intent(this, MusicService::class.java).apply {
            action = "NEXT"
        }
        val nextPendingIntent = PendingIntent.getService(
            this, 4, nextIntent, flag
        )

        val currentTrack = if (musicTracks.isNotEmpty() && currentTrackIndex < musicTracks.size) {
            musicTracks[currentTrackIndex]
        } else {
            MusicTrack("No tracks available", "")
        }

        val isPlaying = mediaPlayer?.isPlaying ?: false
        val playPauseIcon = if (isPlaying) R.drawable.ic_stop else R.drawable.ic_play
        val playPauseText = if (isPlaying) "Pause" else "Play"
        val playPauseAction = if (isPlaying) pausePendingIntent else playPendingIntent

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Now Playing")
            .setContentText(currentTrack.title)
            .setSmallIcon(R.drawable.ic_music_note)
            .addAction(R.drawable.ic_previous, "Previous", previousPendingIntent)
            .addAction(playPauseIcon, playPauseText, playPauseAction)
            .addAction(R.drawable.ic_next, "Next", nextPendingIntent)
            .setStyle(
                androidx.media.app.NotificationCompat.MediaStyle()
                    .setShowActionsInCompactView(0, 1, 2))
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
        }

        if (mediaPlayer?.isPlaying != true) {
            mediaPlayer?.start()
            startForeground(NOTIFICATION_ID, getNotification())
        }
        sendTrackUpdateBroadcast()
    }

    private fun pauseMusic() {
        mediaPlayer?.pause()
        updateNotification()
        sendTrackUpdateBroadcast()
    }

    @OptIn(UnstableApi::class)
    private fun prepareMediaPlayer() {
        if (musicTracks.isEmpty()) return

        mediaPlayer?.release()
        mediaPlayer = MediaPlayer()

        try {
            val currentTrack = musicTracks[currentTrackIndex]
            val assetFileDescriptor = assets.openFd(currentTrack.filePath)
            mediaPlayer?.setDataSource(
                assetFileDescriptor.fileDescriptor,
                assetFileDescriptor.startOffset,
                assetFileDescriptor.length
            )
            assetFileDescriptor.close()
            mediaPlayer?.prepare()
            mediaPlayer?.setOnCompletionListener {
                nextTrack()
            }
        } catch (e: IOException) {
            Log.e("MusicService", "Error preparing media player", e)
            if(musicTracks.size > 1){
                nextTrack()
            }

        }
    }


    private fun nextTrack() {
        if (musicTracks.isEmpty()) return

        currentTrackIndex = (currentTrackIndex + 1) % musicTracks.size

        val wasPlaying = mediaPlayer?.isPlaying ?: false
        prepareMediaPlayer()

        if (wasPlaying) {
            mediaPlayer?.start()
            startForeground(NOTIFICATION_ID, getNotification())
        }

        updateNotification()

        sendTrackUpdateBroadcast()
    }

    private fun previousTrack() {
        if (musicTracks.isEmpty()) return

        currentTrackIndex = if (currentTrackIndex > 0)
            currentTrackIndex - 1
        else
            musicTracks.size - 1

        val wasPlaying = mediaPlayer?.isPlaying ?: false
        prepareMediaPlayer()

        if (wasPlaying) {
            mediaPlayer?.start()
            startForeground(NOTIFICATION_ID, getNotification())
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

        val intent = Intent("TRACK_UPDATED").apply {
            putExtra("track_title", musicTracks[currentTrackIndex].title)
            putExtra("track_index", currentTrackIndex)
            putExtra("total_tracks", musicTracks.size)
            putExtra("is_playing", mediaPlayer?.isPlaying ?: false)
        }
        sendBroadcast(intent)
    }

    private fun sendTrackListBroadcast() {
        val intent = Intent("TRACK_LIST_UPDATED").apply {
            putExtra("track_count", musicTracks.size)
            putExtra("track_titles", musicTracks.map { it.title }.toTypedArray())
        }
        sendBroadcast(intent)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        mediaPlayer = null
    }
    data class MusicTrack(val title: String, val filePath: String)
}
