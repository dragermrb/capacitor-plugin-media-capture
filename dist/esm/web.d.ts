import { WebPlugin } from '@capacitor/core';
import type { CaptureAudioOptions, CaptureImageOptions, CaptureVideoOptions, FormatDataOptions, MediaCapturePlugin, MediaFileData, MediaFileResult } from './definitions';
export declare class MediaCaptureWeb extends WebPlugin implements MediaCapturePlugin {
    captureAudio(options: CaptureAudioOptions): Promise<MediaFileResult>;
    captureImage(options: CaptureImageOptions): Promise<MediaFileResult>;
    captureVideo(options: CaptureVideoOptions): Promise<MediaFileResult>;
    getFormatData(options: FormatDataOptions): Promise<MediaFileData>;
}
