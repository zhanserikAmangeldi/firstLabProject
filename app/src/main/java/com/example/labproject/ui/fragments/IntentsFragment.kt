package com.example.labproject.ui.fragments

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import com.example.labproject.R
import java.io.File
import java.io.FileOutputStream

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
        try {
            val cachePath = File(requireContext().cacheDir, "images")
            cachePath.mkdirs()
            val outputFile = File(cachePath, "shared_image.jpg")

            val inputStream = requireContext().contentResolver.openInputStream(imageUri)
            val outputStream = FileOutputStream(outputFile)
            inputStream?.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }

            val contentUri = FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.fileprovider",
                outputFile
            )

            val intent = Intent("com.instagram.share.ADD_TO_STORY").apply {
                setDataAndType(contentUri, "image/*")
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                putExtra("source_application", requireContext().packageName)
                putExtra("top_background_color", "#33FF33")
                putExtra("bottom_background_color", "#FF00FF")
            }

            val packageManager = requireActivity().packageManager
            if (intent.resolveActivity(packageManager) != null) {
                startActivity(intent)
            } else {
                Toast.makeText(requireContext(), "Instagram app is not installed", Toast.LENGTH_SHORT).show()
            }
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(requireContext(), "Instagram app is not installed", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Error sharing to Instagram: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}