package com.example.labproject

import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.Bundle
import android.provider.Settings
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView


class BroadcastFragment : Fragment(), AirplaneModeReceiver.AirplaneModeListener {

    private lateinit var tvAirplaneStatus: TextView
    private val airplaneModeReceiver = AirplaneModeReceiver()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_broadcast, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tvAirplaneStatus = view.findViewById(R.id.tvAirplaneStatus)

        val isAirplaneModeOn = Settings.Global.getInt(
            context?.contentResolver,
            Settings.Global.AIRPLANE_MODE_ON, 0
        ) != 0

        updateStatus(isAirplaneModeOn)

        airplaneModeReceiver.listener = this
    }

    override fun onResume() {
        super.onResume()
        context?.registerReceiver(
            airplaneModeReceiver,
            IntentFilter(Intent.ACTION_AIRPLANE_MODE_CHANGED)
        )
    }

    override fun onPause() {
        super.onPause()
        context?.unregisterReceiver(airplaneModeReceiver)
    }

    override fun onAirplaneModeChanged(isEnabled: Boolean) {
        updateStatus(isEnabled)
    }

    private fun updateStatus(isEnabled: Boolean) {
        view?.findViewById<ImageView>(R.id.ivAirplaneMode)?.apply {
            setImageResource(if (isEnabled) R.drawable.ic_airplane_mode_on else R.drawable.ic_airplane_mode_off)
        }
        val status = if (isEnabled) "ON" else "OFF"
        val color = if (isEnabled) Color.RED else Color.GREEN

        tvAirplaneStatus.text = "Airplane Mode: $status"
        tvAirplaneStatus.setTextColor(color)
    }
}