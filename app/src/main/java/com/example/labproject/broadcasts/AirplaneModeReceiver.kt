package com.example.labproject.broadcasts

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.widget.Toast

class AirplaneModeReceiver : BroadcastReceiver() {

    interface AirplaneModeListener {
        fun onAirplaneModeChanged(isEnabled: Boolean)
    }

    var listener: AirplaneModeListener? = null

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == Intent.ACTION_AIRPLANE_MODE_CHANGED) {
            val isAirplaneModeOn = Settings.Global.getInt(
                context?.contentResolver,
                Settings.Global.AIRPLANE_MODE_ON, 0
            ) != 0

            listener?.onAirplaneModeChanged(isAirplaneModeOn)

            val message = if (isAirplaneModeOn)
                "Airplane mode is ON"
            else
                "Airplane mode is OFF"

            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }
}