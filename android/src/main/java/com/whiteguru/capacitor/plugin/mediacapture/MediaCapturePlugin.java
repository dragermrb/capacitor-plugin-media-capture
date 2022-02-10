package com.whiteguru.capacitor.plugin.mediacapture;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.webkit.MimeTypeMap;

import androidx.activity.result.ActivityResult;
import androidx.core.content.FileProvider;

import com.getcapacitor.JSArray;
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

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;

@CapacitorPlugin(
        name = "MediaCapture",
        permissions = {@Permission(strings = {Manifest.permission.CAMERA}, alias = MediaCapturePlugin.CAMERA)}
)
public class MediaCapturePlugin extends Plugin {

    // Permission alias constants
    static final String CAMERA = "camera";
    private static final String VIDEO_3GPP = "video/3gpp";
    private static final String VIDEO_MP4 = "video/mp4";
    private static final String AUDIO_3GPP = "audio/3gpp";
    private static final String[] AUDIO_TYPES = new String[]{"audio/3gpp", "audio/aac", "audio/amr", "audio/wav"};

    // Message constants
    private static final String PERMISSION_DENIED_ERROR_CAMERA = "User denied access to camera";
    private static final String FILE_SAVE_ERROR = "Unable to create file on disk";
    private static final String MEDIA_PROCESS_NO_FILE_ERROR = "Unable to process media, file not found on disk";

    private String fileSavePath;

    @PluginMethod
    public void captureAudio(PluginCall call) {
        int limit = call.getInt("limit", 1);
        int duration = call.getInt("duration", 0);

        if (checkCameraPermissions(call)) {
            Intent intent = new Intent(android.provider.MediaStore.Audio.Media.RECORD_SOUND_ACTION);

            startActivityForResult(call, intent, "captureAudioResult");
        }
    }

    @PluginMethod
    public void captureImage(PluginCall call) {
        int limit = call.getInt("limit", 1);

        call.unimplemented();
    }

    @PluginMethod
    public void captureVideo(PluginCall call) {
        int duration = call.getInt("duration", 0);
        float quality = call.getFloat("quality", 1.0F);

        if (checkCameraPermissions(call)) {
            Intent intent = new Intent(android.provider.MediaStore.ACTION_VIDEO_CAPTURE);

            try {
                String appId = getAppId();
                File videoFile = this.createTempFile(getActivity(), "mp4");
                fileSavePath = videoFile.getAbsolutePath();
                Uri imageFileUri = FileProvider.getUriForFile(getActivity(), appId + ".fileprovider", videoFile);
                intent.putExtra(MediaStore.EXTRA_OUTPUT, imageFileUri);
                intent.putExtra(MediaStore.EXTRA_DURATION_LIMIT, duration);
                intent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, quality);
            } catch (Exception ex) {
                call.reject(FILE_SAVE_ERROR, ex);
                return;
            }

            startActivityForResult(call, intent, "captureResult");
        }
    }

    @PluginMethod
    public void getFormatData(PluginCall call) {
        String filePath = call.getString("filePath");
        String mimeType = call.getString("mimeType");

        Uri fileUrl = filePath.startsWith("file:") ? Uri.parse(filePath) : Uri.fromFile(new File(filePath));
        JSObject obj = new JSObject();

        // setup defaults
        obj.put("height", 0);
        obj.put("width", 0);
        obj.put("bitrate", 0);
        obj.put("duration", 0);
        obj.put("codecs", "");

        // If the mimeType isn't set the rest will fail
        // so let's see if we can determine it.
        if (mimeType == null || mimeType.equals("") || "null".equals(mimeType)) {
            mimeType = getMimeType(fileUrl);
        }

        if (Arrays.asList(AUDIO_TYPES).contains(mimeType)) {
            obj = getAudioVideoData(filePath, obj, false);
        } else if (mimeType.equals(VIDEO_3GPP) || mimeType.equals(VIDEO_MP4)) {
            obj = getAudioVideoData(filePath, obj, true);
        }

        call.resolve(obj);
    }

    @ActivityCallback
    private void captureResult(PluginCall call, ActivityResult result) {
        if (call == null) {
            call.reject("No call");
            return;
        }

        if (fileSavePath == null) {
            call.reject(MEDIA_PROCESS_NO_FILE_ERROR);
            return;
        }

        Intent data = result.getData();
        Context context = getBridge().getActivity().getApplicationContext();

        JSArray files = new JSArray();
        if (result.getResultCode() == Activity.RESULT_OK && data != null) {
            //Uri uri = data.getData();
            //Uri uri = Uri.fromFile(new File(fileSavePath));

            JSObject ret = new JSObject();
            JSObject mediaFile = this.createMediaFile(new File(fileSavePath));
            if (mediaFile != null) {
                ret.put("file", mediaFile);
                call.resolve(ret);
            } else {
                call.reject("cant create mediaFile: " + fileSavePath);
            }
        } else if (result.getResultCode() == Activity.RESULT_CANCELED) {
            call.reject("canceled");
        }
    }

    /**
     * Creates a JSObject that represents a File from the Uri
     *
     * @param file the File of the audio/image/video
     * @return a JSObject that represents a File
     */
    private JSObject createMediaFile(File file) {
        Context context = getBridge().getActivity().getApplicationContext();
        Uri uri = Uri.fromFile(file);
        String mimeType = context.getContentResolver().getType(uri);

        JSObject ret = new JSObject();

        ret.put("name", file.getName());
        ret.put("path", uri);
        ret.put("type", mimeType);
        ret.put("size", file.length());

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
            case "captureAudio":
                captureAudio(call);
                break;
            case "captureVideo":
                captureVideo(call);
                break;
        }
    }

    private File createTempFile(Activity activity, String extension) throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ENGLISH).format(new Date());
        String fileName = extension + "_" + timeStamp + "_";
        File storageDir = getContext().getCacheDir();

      return File.createTempFile(fileName, "." + extension, storageDir);
    }

    /**
     * Get the Image specific attributes
     *
     * @param filePath path to the file
     * @param obj      represents the Media File Data
     * @param video    if true get video attributes as well
     * @return a JSObject that represents the Media File Data
     */
    private JSObject getAudioVideoData(String filePath, JSObject obj, boolean video) {
        MediaPlayer player = new MediaPlayer();
        try {
            player.setDataSource(filePath);
            player.prepare();
            obj.put("duration", player.getDuration() / 1000);
            if (video) {
                obj.put("height", player.getVideoHeight());
                obj.put("width", player.getVideoWidth());
            }
        } catch (IOException e) {
            Logger.debug(getLogTag(), "Error loading video file: " + e.getMessage());
        }

        return obj;
    }

    /**
     * Returns the mime type of the data specified by the given URI string.
     *
     * @param uri Uri the URI string of the data
     * @return the mime type of the specified data
     */
    private String getMimeType(Uri uri) {
        String mimeType = null;
        if ("content".equals(uri.getScheme())) {
            Context context = getBridge().getActivity().getApplicationContext();
            mimeType = context.getContentResolver().getType(uri);
        } else {
            mimeType = getMimeTypeForExtension(uri.getPath());
        }

        return mimeType;
    }

    public String getMimeTypeForExtension(String path) {
        String extension = path;
        int lastDot = extension.lastIndexOf('.');
        if (lastDot != -1) {
            extension = extension.substring(lastDot + 1);
        }
        // Convert the URI string to lower case to ensure compatibility with MimeTypeMap (see CB-2185).
        extension = extension.toLowerCase(Locale.getDefault());
        if (extension.equals("3ga")) {
            return "audio/3gpp";
        }
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
    }
}
