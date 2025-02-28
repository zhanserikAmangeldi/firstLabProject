package com.example.labproject.ui.fragments

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import com.example.labproject.R


class IntentsFragment : Fragment() {

    private lateinit var imageView: ImageView
    private var selectedImageUri: Uri? = null

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            imageView.setImageURI(it)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_intents, container, false)
        imageView = view.findViewById(R.id.imgPreview)
        val pickImageButton: Button = view.findViewById(R.id.btnPickImage)
        val shareButton: Button = view.findViewById(R.id.btnShareInstagram)

        pickImageButton.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        shareButton.setOnClickListener {
            selectedImageUri?.let { shareImageToInstagramStories(it) } ?: run {
                Toast.makeText(requireContext(), "Please select an image first.", Toast.LENGTH_SHORT).show()
            }
        }

        return view
    }

    private fun shareImageToInstagramStories(imageUri: Uri) {
        val intent = Intent("com.instagram.share.ADD_TO_STORY").apply {
            setDataAndType(imageUri, "image/*")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            setPackage("com.instagram.android")
        }

        activity?.grantUriPermission(
            "com.instagram.android",
            imageUri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION
        )

        if (isInstagramInstalled()) {
            startActivity(intent)
        } else {
            Toast.makeText(requireContext(), "Instagram is not installed.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun isInstagramInstalled(): Boolean {
        return try {
            requireContext().packageManager.getPackageInfo("com.instagram.android", 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }
}