package com.example.labproject

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.navigation.fragment.findNavController

class MainFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_main, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Setup navigation to feature fragments
        view.findViewById<Button>(R.id.btnIntents).setOnClickListener {
            findNavController().navigate(R.id.action_mainFragment_to_intentsFragment)
        }

        view.findViewById<Button>(R.id.btnService).setOnClickListener {
            findNavController().navigate(R.id.action_mainFragment_to_serviceFragment)
        }

        view.findViewById<Button>(R.id.btnBroadcast).setOnClickListener {
            findNavController().navigate(R.id.action_mainFragment_to_broadcastFragment)
        }

        view.findViewById<Button>(R.id.btnContentProvider).setOnClickListener {
            findNavController().navigate(R.id.action_mainFragment_to_contentProviderFragment)
        }
    }
}