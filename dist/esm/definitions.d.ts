export interface MediaCapturePlugin {
    captureAudio(options: CaptureAudioOptions): Promise<MediaFileResult>;
    captureImage(options: CaptureImageOptions): Promise<MediaFileResult>;
    captureVideo(options: CaptureVideoOptions): Promise<MediaFileResult>;
    getFormatData(options: FormatDataOptions): Promise<MediaFileData>;
}
export interface MediaFile {
    /**
     * The name of the file, without path information.
     */
    name: string;
    /**
     * The full path of the file, including the name.
     */
    path: string;
    /**
     * The file's mime type
     */
    type: string;
    /**
     * The size of the file, in bytes.
     */
    size: number;
}
export interface MediaFileData {
    /**
     * The actual format of the audio and video content.
     */
    codecs: string;
    /**
     * The average bitrate of the content. The value is zero for images.
     */
    bitrate: number;
    /**
     * The height of the image or video in pixels. The value is zero for audio clips.
     */
    height: number;
    /**
     * The width of the image or video in pixels. The value is zero for audio clips.
     */
    width: number;
    /**
     * The length of the video or sound clip in seconds. The value is zero for images.
     */
    duration: number;
}
export interface MediaFileResult {
    file: MediaFile;
}
export interface CaptureAudioOptions {
    /**
     * Maximum duration of an audio sound clip, in seconds. This does not work on Android devices.
     */
    duration?: number;
}
export interface CaptureImageOptions {
    /**
     * Maximum number of images to capture. This limit is not supported on iOS, only one image will be taken per invocation.
     */
    limit?: number;
}
export interface CaptureVideoOptions {
    /**
     * Maximum duration per video clip.
     */
    duration?: number;
    /**
     * Quality of the video. This parameter can only be used with Android.
     */
    quality?: number;
}
export interface FormatDataOptions {
    filePath: string;
    mimeType?: string;
}
