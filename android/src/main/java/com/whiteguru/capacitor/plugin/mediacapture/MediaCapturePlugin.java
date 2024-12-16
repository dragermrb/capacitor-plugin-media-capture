package com.whiteguru.capacitor.plugin.mediacapture;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.provider.MediaStore;

import androidx.activity.result.ActivityResult;
import androidx.annotation.NonNull;

import com.getcapacitor.JSObject;
import com.getcapacitor.Logger;
import com.getcapacitor.PermissionState;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.ActivityCallback;
import com.getcapacitor.annotation.CapacitorPlugin;
import com.getcapacitor.annotation.Permission;
import com.getcapacitor.annotation.PermissionCallback;
import com.whiteguru.capacitor.plugin.mediacapture.fragments.CaptureFragment;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

@CapacitorPlugin(
    name = "MediaCapture",
    permissions = {
        @Permission(strings = { Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO }, alias = MediaCapturePlugin.CAMERA)
    }
)
public class MediaCapturePlugin extends Plugin {
    // Permission alias constants
    static final String CAMERA = "camera";

    // Message constants
    private static final String PERMISSION_DENIED_ERROR_CAMERA = "User denied access to camera";
    private static final String FILE_SAVE_ERROR = "Unable to create file on disk";

    @PluginMethod
    public void captureVideo(PluginCall call) {
        int duration = call.getInt("duration", 0);
        String quality = call.getString("quality", "sd");
        int frameRate = call.getInt("frameRate", 30);
        Long sizeLimit = Long.valueOf(call.getInt("sizeLimit", 0));

        if (checkCameraPermissions(call)) {
            Intent intent = new Intent(getActivity(), MediaCapturePluginActivity.class);

            intent.putExtra(MediaStore.EXTRA_DURATION_LIMIT, duration);
            intent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, quality);
            intent.putExtra(CaptureFragment.EXTRA_FRAME_RATE, frameRate);
            intent.putExtra(MediaStore.EXTRA_SIZE_LIMIT, sizeLimit);

            try {
                intent.putExtra(MediaStore.EXTRA_OUTPUT, this.createTempFile("mp4"));
            } catch (Exception ex) {
                call.reject(FILE_SAVE_ERROR, ex);
                return;
            }

            startActivityForResult(call, intent, "captureResult");
        }
    }

    @ActivityCallback
    private void captureResult(PluginCall call, ActivityResult result) {
        if (call == null) {
            call.reject("No call");
            return;
        }

        Intent resultIntent = result.getData();

        if (result.getResultCode() == Activity.RESULT_OK && resultIntent != null && resultIntent.getData() != null) {
            Uri resultUri = resultIntent.getData();
            JSObject ret = new JSObject();

            ret.put("file", this.createMediaFile(resultUri));

            call.resolve(ret);
        } else if (result.getResultCode() == Activity.RESULT_CANCELED) {
            call.reject("canceled");
        }
    }

    /**
     * Creates a JSObject that represents a File from the Uri
     *
     * @param uri the Uri of the audio/image/video
     * @return a JSObject that represents a File
     */
    private JSObject createMediaFile(Uri uri) {
        JSObject ret = new JSObject();

        ret.put("name", uri.getLastPathSegment());
        ret.put("path", uri);
        ret.put("type", getMimeTypeFromUri(uri));
        ret.put("size", getFileSizeFromUri(uri));

        return ret;
    }

    private boolean checkCameraPermissions(PluginCall call) {
        // if the manifest does not contain the camera permissions key, we don't need to ask the user
        boolean needCameraPerms = isPermissionDeclared(CAMERA);
        boolean hasCameraPerms = !needCameraPerms || getPermissionState(CAMERA) == PermissionState.GRANTED;

        if (!hasCameraPerms) {
            requestPermissionForAlias(CAMERA, call, "cameraPermissionsCallback");
            return false;
        }
        return true;
    }

    /**
     * Completes the plugin call after a camera permission request
     *
     * @param call the plugin call
     */
    @PermissionCallback
    private void cameraPermissionsCallback(PluginCall call) {
        if (getPermissionState(CAMERA) != PermissionState.GRANTED) {
            Logger.debug(getLogTag(), "User denied camera permission: " + getPermissionState(CAMERA).toString());
            call.reject(PERMISSION_DENIED_ERROR_CAMERA);
            return;
        }

        switch (call.getMethodName()) {
            case "captureVideo":
                captureVideo(call);
                break;
        }
    }

    @NonNull
    private File createTempFile(String extension) throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ENGLISH).format(new Date());
        String fileName = "VID_" + timeStamp + "_";
        File storageDir = getContext().getCacheDir();

        return File.createTempFile(fileName, "." + extension, storageDir);
    }

    /**
     * Returns the mime type of the data specified by the given URI.
     *
     * @param uri Uri the URI string of the data
     * @return the mime type of the specified data
     */
    private String getMimeTypeFromUri(Uri uri) {
        return getContext().getContentResolver().getType(uri);
    }

    /**
     * Returns the file size in bytes of the data specified by the given URI.
     *
     * @param uri Uri the URI string of the data
     * @return the file size in bytes
     */
    @NonNull
    private Long getFileSizeFromUri(Uri uri) {
        File file = new File(uri.getPath());

        return file.length();
    }
}
