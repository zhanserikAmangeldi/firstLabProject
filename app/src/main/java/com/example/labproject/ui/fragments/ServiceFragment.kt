package com.example.labproject.ui.fragments

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi
import com.example.labproject.services.MusicService
import com.example.labproject.R

class ServiceFragment : Fragment() {

    private lateinit var tvCurrentTrack: TextView
    private lateinit var progressTrack: ProgressBar
    private lateinit var tvTrackCount: TextView
    private lateinit var trackReceiver: BroadcastReceiver
    private lateinit var trackListReceiver: BroadcastReceiver
    private lateinit var btnStartMusic: Button
    private lateinit var btnPreviousTrack: Button
    private lateinit var btnNextTrack: Button
    private var isPlaying: Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_service, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tvCurrentTrack = view.findViewById(R.id.tvCurrentTrack)
        progressTrack = view.findViewById(R.id.progressTrack)
        tvTrackCount = view.findViewById(R.id.tvTrackCount)
        btnStartMusic = view.findViewById(R.id.btnStartMusic)
        btnPreviousTrack = view.findViewById(R.id.btnPreviousTrack)
        btnNextTrack = view.findViewById(R.id.btnNextTrack)


        btnStartMusic.setOnClickListener {
            startMusicService(if (isPlaying) "PAUSE" else "PLAY")
        }

        btnPreviousTrack.setOnClickListener {
            startMusicService("PREVIOUS")
        }

        btnNextTrack.setOnClickListener {
            startMusicService("NEXT")
        }

        view.findViewById<Button>(R.id.btnRefreshMusic)?.setOnClickListener {
            startMusicService("REFRESH")
        }

        setupReceivers()
        updateButtonEnabledState(false)
        startMusicService("REFRESH")
    }

    private fun setupReceivers() {
        trackReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == "TRACK_UPDATED") {
                    activity?.runOnUiThread {
                        val trackTitle = intent.getStringExtra("track_title") ?: "Unknown Track"
                        val trackIndex = intent.getIntExtra("track_index", 0)
                        val totalTracks = intent.getIntExtra("total_tracks", 0)
                        isPlaying = intent.getBooleanExtra("is_playing", false)

                        updatePlayPauseButton(isPlaying)

                        tvCurrentTrack.text = "Now Playing: $trackTitle"
                        progressTrack.max = totalTracks
                        progressTrack.progress = trackIndex + 1
                    }
                }
            }
        }

        trackListReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == "TRACK_LIST_UPDATED") {
                    activity?.runOnUiThread {
                        val trackCount = intent.getIntExtra("track_count", 0)


                        tvTrackCount.text = "$trackCount tracks found"

                        if (trackCount == 0) {
                            tvCurrentTrack.text = "No music files found"
                            progressTrack.progress = 0
                            isPlaying = false
                            updatePlayPauseButton(isPlaying)
                        }

                        updateButtonEnabledState(trackCount > 0)
                    }
                }
            }
        }
    }

    private fun updatePlayPauseButton(isPlaying: Boolean) {
        btnStartMusic.apply {
            text = if (isPlaying) "Pause" else "Play"
            val icon = if (isPlaying) R.drawable.ic_stop else R.drawable.ic_play
            setCompoundDrawablesWithIntrinsicBounds(0, icon, 0, 0)
        }
    }

    private fun updateButtonEnabledState(enabled: Boolean) {
        btnStartMusic.isEnabled = enabled
        btnPreviousTrack.isEnabled = enabled
        btnNextTrack.isEnabled = enabled
    }

    override fun onResume() {
        super.onResume()
        manageReceivers(true)
    }

    @OptIn(UnstableApi::class)
    override fun onPause() {
        super.onPause()
        manageReceivers(false)
    }


    // Function to manage the registration and unregistration of receivers based on the provided flag
    private fun manageReceivers(register: Boolean) {
        val localContext = context ?: return

        try {
            if (register) {
                ContextCompat.registerReceiver(
                    localContext,
                    trackReceiver,
                    IntentFilter("TRACK_UPDATED"),
                    ContextCompat.RECEIVER_NOT_EXPORTED
                )
                ContextCompat.registerReceiver(
                    localContext,
                    trackListReceiver,
                    IntentFilter("TRACK_LIST_UPDATED"),
                    ContextCompat.RECEIVER_NOT_EXPORTED
                )
            } else {
                localContext.unregisterReceiver(trackReceiver)
                localContext.unregisterReceiver(trackListReceiver)
            }
        } catch (e: IllegalArgumentException) {
            Log.w("ServiceFragment", "Receiver was not registered or already unregistered")
        }
    }


    private fun startMusicService(action: String) {
        val intent = Intent(context, MusicService::class.java)
        intent.action = action
        context?.startForegroundService(intent)
    }
}