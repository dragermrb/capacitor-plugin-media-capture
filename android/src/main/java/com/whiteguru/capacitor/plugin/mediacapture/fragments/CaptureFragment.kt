/**
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

package com.whiteguru.capacitor.plugin.mediacapture.fragments

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.os.Bundle
import android.provider.MediaStore.*
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.concurrent.futures.await
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.util.Consumer
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.whenCreated
import androidx.navigation.NavController
import androidx.navigation.Navigation
import com.whiteguru.capacitor.plugin.mediacapture.R
import com.whiteguru.capacitor.plugin.mediacapture.databinding.FragmentCaptureBinding
import com.whiteguru.capacitor.plugin.mediacapture.extensions.getAspectRatio
import com.whiteguru.capacitor.plugin.mediacapture.extensions.getAspectRatioString
import com.whiteguru.capacitor.plugin.mediacapture.extensions.getNameString
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.io.File


class CaptureFragment : Fragment() {
    private var outputFile: File? = null
    private var limitInSeconds = 0
    private var quality: Quality = Quality.SD
    private var sizeLimit = 0L

    // UI with ViewBinding
    private var _captureViewBinding: FragmentCaptureBinding? = null
    private val captureViewBinding get() = _captureViewBinding!!
    private val timeLeftLive = MutableLiveData<String>()

    /** Host's navigation controller */
    private val navController: NavController by lazy {
        Navigation.findNavController(requireActivity(), R.id.fragment_container)
    }

    private val cameraCapabilities = mutableListOf<CameraCapability>()

    private lateinit var videoCapture: VideoCapture<Recorder>
    private var currentRecording: Recording? = null
    private lateinit var recordingState: VideoRecordEvent

    // Camera UI  states and inputs
    enum class UiState {
        IDLE,       // Not recording, all UI controls are active.
        RECORDING,  // Camera is recording, only display Stop button.
        FINALIZED,  // Recording just completes, disable all RECORDING UI controls.
        RECOVERY    // For future use.
    }

    private var cameraIndex = 0
    private var audioEnabled = true

    private val mainThreadExecutor by lazy { ContextCompat.getMainExecutor(requireContext()) }
    private var enumerationDeferred: Deferred<Unit>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Get the Intent that started this activity and extract the string

        val extras = requireActivity().intent.extras
        if (extras != null) {
            quality = getQualityFromString(extras.getString(EXTRA_VIDEO_QUALITY, "sd"))
            limitInSeconds = extras.getInt(EXTRA_DURATION_LIMIT, 0)
            sizeLimit = extras.getLong(EXTRA_SIZE_LIMIT, 10L * 1024 * 1024)

            val file = extras.getSerializable(EXTRA_OUTPUT) as File
            if (file != null) {
                outputFile = file
            }
        }
    }

    // System function implementations
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _captureViewBinding = FragmentCaptureBinding.inflate(inflater, container, false)
        return captureViewBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initCameraFragment()
    }

    override fun onDestroyView() {
        _captureViewBinding = null
        super.onDestroyView()
    }

    /**
     *   Always bind preview + video capture use case combinations in this sample
     *   (VideoCapture can work on its own). The function should always execute on
     *   the main thread.
     */
    private suspend fun bindCaptureUsecase() {
        val cameraProvider = ProcessCameraProvider.getInstance(requireContext()).await()

        val cameraSelector = getCameraSelector(cameraIndex)

        // create the user required QualitySelector (video resolution): we know this is
        // supported, a valid qualitySelector will be created.
        //val quality = cameraCapabilities[cameraIndex].qualities[qualityIndex]
        //val qualitySelector = QualitySelector.from(quality)
        val qualitySelector = QualitySelector.from(
            quality,
            FallbackStrategy.lowerQualityOrHigherThan(quality)
        )

        captureViewBinding.previewView.updateLayoutParams<ConstraintLayout.LayoutParams> {
            val orientation = this@CaptureFragment.resources.configuration.orientation
            dimensionRatio = quality.getAspectRatioString(
                quality,
                (orientation == Configuration.ORIENTATION_PORTRAIT)
            )
        }

        val preview = Preview.Builder()
            .setTargetAspectRatio(quality.getAspectRatio(quality))
            .build().apply {
                setSurfaceProvider(captureViewBinding.previewView.surfaceProvider)
            }

        val recorder = Recorder.Builder()
            .setQualitySelector(qualitySelector)
            .build()

        videoCapture = VideoCapture.withOutput(recorder)

        try {
            cameraProvider.unbindAll()

            cameraProvider.bindToLifecycle(
                viewLifecycleOwner,
                cameraSelector,
                videoCapture,
                preview
            )
        } catch (exc: Exception) {
            // we are on main thread, let's reset the controls on the UI.
            Log.e(TAG, "Use case binding failed", exc)
            resetUIandState("bindToLifecycle failed: $exc")
        }

        enableUI(true)
    }

    /**
     * Kick start the video recording
     *   - config Recorder to capture to MediaStoreOutput
     *   - register RecordEvent Listener
     *   - apply audio request from user
     *   - start recording!
     * After this function, user could start/stop recording and application listens
     * to VideoRecordEvent for the current recording status.
     */
    @SuppressLint("MissingPermission")
    private fun startRecording() {
        val fileOutput = FileOutputOptions.Builder(outputFile!!)
            .apply { if (sizeLimit > 0) setFileSizeLimit(sizeLimit) }
            .build()

        // configure Recorder and Start recording to the mediaStoreOutput.
        currentRecording = videoCapture.output
            .prepareRecording(requireActivity(), fileOutput)
            .apply { if (audioEnabled) withAudioEnabled() }
            .start(mainThreadExecutor, captureListener)

        Log.i(TAG, "Recording started")
    }

    private fun stopRecording(){
        captureViewBinding.stopButton.visibility = View.INVISIBLE

        if (currentRecording != null && recordingState !is VideoRecordEvent.Finalize) {
            val recording = currentRecording
            if (recording != null) {
                recording.stop()
                currentRecording = null
            }
        }
    }

    /**
     * CaptureEvent listener.
     */
    private val captureListener = Consumer<VideoRecordEvent> { event ->
        // cache the recording state
        if (event !is VideoRecordEvent.Status)
            recordingState = event

        updateUI(event)

        if (event is VideoRecordEvent.Finalize) {
            // display the captured video
            lifecycleScope.launch {
                navController.navigate(
                    CaptureFragmentDirections.actionCaptureToVideoViewer(
                        event.outputResults.outputUri
                    )
                )
            }
        }
    }

    /**
     * Retrieve the asked camera's type(lens facing type). In this sample, only 2 types:
     *   idx is even number:  CameraSelector.LENS_FACING_BACK
     *          odd number:   CameraSelector.LENS_FACING_FRONT
     */
    private fun getCameraSelector(idx: Int): CameraSelector {
        if (cameraCapabilities.size == 0) {
            Log.i(TAG, "Error: This device does not have any camera, bailing out")
            requireActivity().finish()
        }
        return (cameraCapabilities[idx % cameraCapabilities.size].camSelector)
    }

    data class CameraCapability(val camSelector: CameraSelector, val qualities: List<Quality>)

    /**
     * Query and cache this platform's camera capabilities, run only once.
     */
    init {
        enumerationDeferred = lifecycleScope.async {
            whenCreated {
                val provider = ProcessCameraProvider.getInstance(requireContext()).await()

                provider.unbindAll()
                for (camSelector in arrayOf(
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    CameraSelector.DEFAULT_FRONT_CAMERA
                )) {
                    try {
                        // just get the camera.cameraInfo to query capabilities
                        // we are not binding anything here.
                        if (provider.hasCamera(camSelector)) {
                            val camera = provider.bindToLifecycle(requireActivity(), camSelector)
                            QualitySelector
                                .getSupportedQualities(camera.cameraInfo)
                                .filter { quality ->
                                    //listOf(Quality.UHD, Quality.FHD, Quality.HD, Quality.SD)
                                    listOf(Quality.SD)
                                        .contains(quality)
                                }.also {
                                    cameraCapabilities.add(CameraCapability(camSelector, it))
                                }
                        }
                    } catch (exc: java.lang.Exception) {
                        Log.e(TAG, "Camera Face $camSelector is not supported")
                    }
                }
            }
        }
    }

    /**
     * One time initialize for CameraFragment (as a part of fragment layout's creation process).
     * This function performs the following:
     *   - initialize but disable all UI controls except the Quality selection.
     *   - set up the Quality selection recycler view.
     *   - bind use cases to a lifecycle camera, enable UI controls.
     */
    private fun initCameraFragment() {
        initializeUI()
        viewLifecycleOwner.lifecycleScope.launch {
            if (enumerationDeferred != null) {
                enumerationDeferred!!.await()
                enumerationDeferred = null
            }

            bindCaptureUsecase()
        }
    }

    /**
     * Initialize UI. Preview and Capture actions are configured in this function.
     * Note that preview and capture are both initialized either by UI or CameraX callbacks
     * (except the very 1st time upon entering to this fragment in onCreateView()
     */
    @SuppressLint("ClickableViewAccessibility", "MissingPermission")
    private fun initializeUI() {
        captureViewBinding.cameraButton.apply {
            setOnClickListener {
                cameraIndex = (cameraIndex + 1) % cameraCapabilities.size

                // camera device change is in effect instantly:
                //   - restart preview

                enableUI(false)
                viewLifecycleOwner.lifecycleScope.launch {
                    bindCaptureUsecase()
                }
            }
            isEnabled = false
        }

        // React to user touching the capture button
        captureViewBinding.captureButton.apply {
            setOnClickListener {
                if (!this@CaptureFragment::recordingState.isInitialized ||
                    recordingState is VideoRecordEvent.Finalize
                ) {
                    enableUI(false)  // Our eventListener will turn on the Recording UI.
                    startRecording()
                } else {
                    when (recordingState) {
                        is VideoRecordEvent.Start -> {
                            currentRecording?.pause()
                            captureViewBinding.stopButton.visibility = View.VISIBLE
                        }
                        is VideoRecordEvent.Pause -> currentRecording?.resume()
                        is VideoRecordEvent.Resume -> currentRecording?.pause()
                        else -> throw IllegalStateException("recordingState in unknown state")
                    }
                }
            }
            isEnabled = false
        }

        captureViewBinding.stopButton.apply {
            setOnClickListener {
                stopRecording()
            }

            // ensure the stop button is initialized disabled & invisible
            visibility = View.INVISIBLE
            isEnabled = false
        }

        timeLeftLive.observe(viewLifecycleOwner) {
            captureViewBinding.timeLeft.apply {
                post { text = it }
            }
        }
        timeLeftLive.value = ""
    }

    /**
     * UpdateUI according to CameraX VideoRecordEvent type:
     *   - user starts capture.
     *   - this app disables all UI selections.
     *   - this app enables capture run-time UI (stop).
     *   - user controls recording with run-time UI, eventually tap "stop" to end.
     *   - this app informs CameraX recording to stop with recording.stop() (or recording.close()).
     *   - CameraX notify this app that the recording is indeed stopped, with the Finalize event.
     *   - this app starts VideoViewer fragment to view the captured result.
     */
    private fun updateUI(event: VideoRecordEvent) {
        val state = if (event is VideoRecordEvent.Status) recordingState.getNameString()
        else event.getNameString()
        when (event) {
            is VideoRecordEvent.Status -> {
                // placeholder: we update the UI with new status after this when() block,
                // nothing needs to do here.
            }
            is VideoRecordEvent.Start -> {
                showUI(UiState.RECORDING, event.getNameString())
            }
            is VideoRecordEvent.Finalize -> {
                showUI(UiState.FINALIZED, event.getNameString())
            }
        }

        val stats = event.recordingStats
        val size = stats.numBytesRecorded / 1000
        val time = java.util.concurrent.TimeUnit.NANOSECONDS.toSeconds(stats.recordedDurationNanos)
        var text = "${state}: recorded ${size}KB, in ${time}second"
        if (event is VideoRecordEvent.Finalize)
            text = "${text}\nFile saved to: ${event.outputResults.outputUri}"

        Log.i(TAG, "recording event: $text")

        if (limitInSeconds > 0) {
            timeLeftLive.value = "${limitInSeconds - time}\""

            if (time >= limitInSeconds) {
                stopRecording();
            }
        } else {
            timeLeftLive.value = "${time}\""
        }
    }

    /**
     * Enable/disable UI:
     *    User could select the capture parameters when recording is not in session
     *    Once recording is started, need to disable able UI to avoid conflict.
     */
    private fun enableUI(enable: Boolean) {
        arrayOf(
            captureViewBinding.cameraButton,
            captureViewBinding.captureButton,
            captureViewBinding.stopButton,
        ).forEach {
            it.isEnabled = enable
        }
        // disable the camera button if no device to switch
        if (cameraCapabilities.size <= 1) {
            captureViewBinding.cameraButton.isEnabled = false
        }
    }

    /**
     * initialize UI for recording:
     *  - at recording: hide audio, qualitySelection,change camera UI; enable stop button
     *  - otherwise: show all except the stop button
     */
    private fun showUI(state: UiState, status: String = "idle") {
        captureViewBinding.let {
            when (state) {
                UiState.IDLE -> {
                    it.captureButton.visibility = View.VISIBLE
                    it.stopButton.visibility = View.INVISIBLE
                    it.cameraButton.visibility= View.VISIBLE
                }
                UiState.RECORDING -> {
                    it.cameraButton.visibility = View.INVISIBLE

                    it.captureButton.visibility = View.INVISIBLE
                    it.captureButton.isEnabled = false

                    it.stopButton.visibility = View.VISIBLE
                    it.stopButton.isEnabled = true
                }
                UiState.FINALIZED -> {
                    it.captureButton.visibility = View.VISIBLE
                    it.captureButton.isEnabled = true
                    it.stopButton.visibility = View.INVISIBLE
                }
                else -> {
                    val errorMsg = "Error: showUI($state) is not supported"
                    Log.e(TAG, errorMsg)
                    return
                }
            }
        }
    }

    /**
     * ResetUI (restart):
     *    in case binding failed, let's give it another change for re-try. In future cases
     *    we might fail and user get notified on the status
     */
    private fun resetUIandState(reason: String) {
        enableUI(true)
        showUI(UiState.IDLE, reason)

        cameraIndex = 0
    }

    private fun getQualityFromString(qualityValue: String): Quality {
        return when (qualityValue) {
            "uhd" -> Quality.UHD
            "fhd" -> Quality.FHD
            "hd" -> Quality.HD
            "sd" -> Quality.SD
            else -> Quality.SD
        }
    }

    companion object {
        // default Quality selection if no input from UI
        const val DEFAULT_QUALITY_IDX = 0
        val TAG: String = CaptureFragment::class.java.simpleName
    }
}