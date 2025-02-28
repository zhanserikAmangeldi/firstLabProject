package com.example.labproject.adapters

import android.annotation.SuppressLint
import android.icu.text.SimpleDateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.labproject.R
import com.example.labproject.models.CalendarEvent
import java.util.Date
import java.util.Locale

class CalendarAdapter(private val events: List<CalendarEvent>) :
    RecyclerView.Adapter<CalendarAdapter.EventViewHolder>() {

    class EventViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTitle: TextView = view.findViewById(R.id.tvEventTitle)
        val tvDateTime: TextView = view.findViewById(R.id.tvEventDateTime)
        val tvLocation: TextView = view.findViewById(R.id.tvEventLocation)
        val tvDescription: TextView = view.findViewById(R.id.tvEventDescription)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_calendar_event, parent, false)
        return EventViewHolder(view)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        val event = events[position]

        holder.tvTitle.text = event.title

        val dateFormat = SimpleDateFormat("EEE, MMM d, yyyy • h:mm a", Locale.getDefault())
        val startDate = Date(event.startTime)
        val endDate = Date(event.endTime)
        holder.tvDateTime.text = "${dateFormat.format(startDate)} - ${
            SimpleDateFormat("h:mm a", Locale.getDefault()).format(endDate)
        }"

        holder.tvLocation.text = event.location.ifEmpty { "No location" }
        holder.tvDescription.text = event.description.ifEmpty { "No description" }
    }

    override fun getItemCount() = events.size
}