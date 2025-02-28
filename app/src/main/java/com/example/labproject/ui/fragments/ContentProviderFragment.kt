package com.example.labproject.ui.fragments

import android.Manifest
import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.content.ContentUris
import android.content.pm.PackageManager
import android.icu.util.Calendar
import android.os.Bundle
import android.provider.CalendarContract
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.labproject.adapters.CalendarAdapter
import com.example.labproject.models.CalendarEvent
import com.example.labproject.R
import java.text.SimpleDateFormat
import java.util.Locale

class ContentProviderFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private var calendarEvents = mutableListOf<CalendarEvent>()
    private lateinit var adapter: CalendarAdapter

    private var startDate: Calendar = Calendar.getInstance()
    private var endDate: Calendar = Calendar.getInstance().apply {
        add(Calendar.DAY_OF_MONTH, 30)
    }
    private lateinit var tvDateRange: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_content_provider, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(context)

        adapter = CalendarAdapter(calendarEvents)
        recyclerView.adapter = adapter

        tvDateRange = view.findViewById(R.id.tvDateRange)
        updateDateRangeText()

        view.findViewById<Button>(R.id.btnSelectStartDate).setOnClickListener {
            showDatePicker(true)
        }

        view.findViewById<Button>(R.id.btnSelectEndDate).setOnClickListener {
            showDatePicker(false)
        }

        view.findViewById<Button>(R.id.btnFetchEvents).setOnClickListener {
            fetchCalendarEvents()
        }
    }

    private fun showDatePicker(isStartDate: Boolean) {
        val calendar = if (isStartDate) startDate else endDate

        DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                if (isStartDate) {
                    startDate.set(year, month, dayOfMonth)
                    if (endDate.timeInMillis < startDate.timeInMillis) {
                        endDate.timeInMillis = startDate.timeInMillis
                        endDate.add(Calendar.DAY_OF_MONTH, 1)
                    }
                } else {
                    val tempCal = Calendar.getInstance()
                    tempCal.set(year, month, dayOfMonth)

                    if (tempCal.timeInMillis >= startDate.timeInMillis) {
                        endDate = tempCal
                    } else {
                        Toast.makeText(
                            context,
                            "End date cannot be before start date",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
                updateDateRangeText()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    @SuppressLint("SetTextI18n")
    private fun updateDateRangeText() {
        val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
        val startFormatted = dateFormat.format(startDate.time)
        val endFormatted = dateFormat.format(endDate.time)
        tvDateRange.text = "From $startFormatted to $endFormatted"
    }

    private fun fetchCalendarEvents() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.READ_CALENDAR
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.READ_CALENDAR), 101)
            return
        }

        calendarEvents.clear()

        val startTimeMillis = startDate.timeInMillis
        val endTimeMillis = endDate.timeInMillis

        val builder = CalendarContract.Instances.CONTENT_URI.buildUpon()
        ContentUris.appendId(builder, startTimeMillis)
        ContentUris.appendId(builder, endTimeMillis)
        val uri = builder.build()

        val projection = arrayOf(
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.DESCRIPTION,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END,
            CalendarContract.Instances.EVENT_LOCATION
        )

        val sortOrder = "${CalendarContract.Instances.BEGIN} ASC"

        val cursor = context?.contentResolver?.query(
            uri,
            projection,
            null,
            null,
            sortOrder
        )

        cursor?.use {
            val titleIndex = it.getColumnIndex(CalendarContract.Instances.TITLE)
            val descriptionIndex = it.getColumnIndex(CalendarContract.Instances.DESCRIPTION)
            val startIndex = it.getColumnIndex(CalendarContract.Instances.BEGIN)
            val endIndex = it.getColumnIndex(CalendarContract.Instances.END)
            val locationIndex = it.getColumnIndex(CalendarContract.Instances.EVENT_LOCATION)

            while (it.moveToNext()) {
                val title = if (titleIndex != -1) it.getString(titleIndex) else "No Title"
                val description = if (descriptionIndex != -1) it.getString(descriptionIndex) else ""
                val start = if (startIndex != -1) it.getLong(startIndex) else 0
                val end = if (endIndex != -1) it.getLong(endIndex) else 0
                val location = if (locationIndex != -1) it.getString(locationIndex) else ""

                calendarEvents.add(CalendarEvent(title, description, start, end, location))
            }
        }

        if (calendarEvents.isEmpty()) {
            Toast.makeText(context, "No events found in the selected date range", Toast.LENGTH_SHORT).show()
        }

        adapter.notifyDataSetChanged()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == 101 && grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            fetchCalendarEvents()
        } else {
            Toast.makeText(
                context,
                "Calendar permission is required to fetch events",
                Toast.LENGTH_LONG
            ).show()
        }
    }
}