package com.example.labproject.ui.fragments

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.labproject.R
import com.example.labproject.services.MusicService

class ServiceFragment : Fragment() {

    private lateinit var currentTrackTextView: TextView
    private lateinit var progressTrackBar: ProgressBar
    private lateinit var trackCountTextView: TextView
    private lateinit var trackUpdateReceiver: BroadcastReceiver
    private lateinit var trackListUpdateReceiver: BroadcastReceiver
    private lateinit var progressUpdateReceiver: BroadcastReceiver
    private lateinit var playPauseButton: Button
    private lateinit var previousButton: Button
    private lateinit var nextButton: Button
    private lateinit var refreshButton: Button

    private var isPlaying: Boolean = false
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_service, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        currentTrackTextView = view.findViewById(R.id.tvCurrentTrack)
        progressTrackBar = view.findViewById(R.id.progressTrack)
        trackCountTextView = view.findViewById(R.id.tvTrackCount)
        playPauseButton = view.findViewById(R.id.btnStartMusic)
        previousButton = view.findViewById(R.id.btnPreviousTrack)
        nextButton = view.findViewById(R.id.btnNextTrack)
        refreshButton = view.findViewById(R.id.btnRefreshMusic)


        playPauseButton.setOnClickListener {
            startMusicService(if (isPlaying) "PAUSE" else "PLAY")
        }

        previousButton.setOnClickListener {
            startMusicService("PREVIOUS")
        }

        nextButton.setOnClickListener {
            startMusicService("NEXT")
        }

        refreshButton.setOnClickListener {
            startMusicService("REFRESH")
        }

        setupReceivers()
        updateButtonEnabledState(false)
        startMusicService("REFRESH")
    }

    private fun setupReceivers() {
        trackUpdateReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == MusicService.ACTION_TRACK_UPDATED) {
                    val trackTitle = intent.getStringExtra("track_title") ?: "Unknown Track"
                    val trackIndex = intent.getIntExtra("track_index", 0)
                    val totalTracks = intent.getIntExtra("total_tracks", 0)
                    isPlaying = intent.getBooleanExtra("is_playing", false)

                    updatePlayPauseButton(isPlaying)
                    currentTrackTextView.text = "Now Playing: $trackTitle"
                    trackCountTextView.text = if(totalTracks > 0) "Track ${trackIndex+1} of $totalTracks" else ""
                }
            }
        }

        trackListUpdateReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == MusicService.ACTION_TRACK_LIST_UPDATED) {
                    val trackCount = intent.getIntExtra("track_count", 0)
                    trackCountTextView.text = "$trackCount tracks found"

                    if (trackCount == 0) {
                        currentTrackTextView.text = "No music files found"
                        progressTrackBar.progress = 0
                        isPlaying = false
                        updatePlayPauseButton(isPlaying)
                    }
                    updateButtonEnabledState(trackCount > 0)
                }
            }
        }

        progressUpdateReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == MusicService.ACTION_TRACK_PROGRESS_UPDATED) {
                    val position = intent.getIntExtra("current_position", 0)
                    val duration = intent.getIntExtra("duration", 0)
                    if (duration > 0) {
                        progressTrackBar.max = duration
                        progressTrackBar.progress = position
                    }
                }
            }
        }
    }

    private fun updatePlayPauseButton(isPlaying: Boolean) {
        playPauseButton.apply {
            text = if (isPlaying) "Pause" else "Play"
            val icon = if (isPlaying) R.drawable.ic_stop else R.drawable.ic_play
            setCompoundDrawablesWithIntrinsicBounds(0, icon, 0, 0)
        }
    }

    private fun updateButtonEnabledState(enabled: Boolean) {
        playPauseButton.isEnabled = enabled
        previousButton.isEnabled = enabled
        nextButton.isEnabled = enabled
    }
    override fun onResume() {
        super.onResume()
        manageReceivers(true)
    }

    override fun onPause() {
        super.onPause()
        manageReceivers(false)
    }


    private fun manageReceivers(register: Boolean) {
        val localContext = context ?: return

        try {
            if (register) {
                ContextCompat.registerReceiver(localContext, trackUpdateReceiver,
                    IntentFilter(MusicService.ACTION_TRACK_UPDATED), ContextCompat.RECEIVER_NOT_EXPORTED)
                ContextCompat.registerReceiver(localContext, trackListUpdateReceiver,
                    IntentFilter(MusicService.ACTION_TRACK_LIST_UPDATED), ContextCompat.RECEIVER_NOT_EXPORTED)
                ContextCompat.registerReceiver(localContext, progressUpdateReceiver,
                    IntentFilter(MusicService.ACTION_TRACK_PROGRESS_UPDATED), ContextCompat.RECEIVER_NOT_EXPORTED)
            } else {
                localContext.unregisterReceiver(trackUpdateReceiver)
                localContext.unregisterReceiver(trackListUpdateReceiver)
                localContext.unregisterReceiver(progressUpdateReceiver)
            }
        } catch (e: IllegalArgumentException) {
            android.util.Log.w("ServiceFragment", "Receiver was not registered or already unregistered")
        }
    }


    private fun startMusicService(action: String) {
        val intent = Intent(context, MusicService::class.java)
        intent.action = action
        context?.startForegroundService(intent)
    }
}