/*
 * Copyright 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Simple VideoView to display the just captured video

package com.whiteguru.capacitor.plugin.mediacapture.fragments

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.MediaController
import androidx.activity.addCallback
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.navigation.fragment.navArgs
import com.whiteguru.capacitor.plugin.mediacapture.R
import com.whiteguru.capacitor.plugin.mediacapture.databinding.FragmentVideoViewerBinding
import java.io.File


/**
 * VideoViewerFragment:
 *      Accept MediaStore URI and play it with VideoView (Also displaying file size and location)
 *      Note: Might be good to retrieve the encoded file mime type (not based on file type)
 */
class VideoViewerFragment : androidx.fragment.app.Fragment() {
    private val args: VideoViewerFragmentArgs by navArgs()

    // This property is only valid between onCreateView and onDestroyView.
    private var _binding: FragmentVideoViewerBinding? = null
    private val binding get() = _binding!!
    private val navController: NavController by lazy {
        Navigation.findNavController(requireActivity(), R.id.fragment_container)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requireActivity().onBackPressedDispatcher.addCallback(this) {
            deleteFile(args.uri)

            navController.popBackStack()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentVideoViewerBinding.inflate(inflater, container, false)

        return binding.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.okButton.apply {
            setOnClickListener {
                val resultIntent = Intent()

                resultIntent.setDataAndNormalize(args.uri)

                requireActivity().setResult(Activity.RESULT_OK, resultIntent)

                requireActivity().finish()
            }
        }

        binding.cancelButton.apply {
            setOnClickListener {
                deleteFile(args.uri)

                requireActivity().setResult(Activity.RESULT_CANCELED)

                requireActivity().finish()
            }
        }

        showVideo(args.uri)
    }

    override fun onDestroyView() {
        _binding = null

        super.onDestroyView()
    }

    /**
     * A helper function to play the recorded video. Note that VideoView/MediaController auto-hides
     * the play control menus, touch on the video area would bring it back for 3 second.
     */
    private fun showVideo(uri : Uri) {
        Log.i("VideoViewerFragment", "Uri: " + uri.toString())

        val mc = MediaController(requireContext())
        binding.videoViewer.apply {
            setVideoURI(uri)
            setMediaController(mc)
            requestFocus()
        }.start()

        mc.show(10000)
    }

    private fun deleteFile(uri: Uri){
        Log.i("VideoViewerFragment", "deleteFile "+uri.toString())

        File(uri.path).apply {
            exists() && delete()
        }
    }
}
