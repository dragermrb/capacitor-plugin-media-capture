export interface MediaCapturePlugin {
  captureVideo(options: CaptureVideoOptions): Promise<MediaFileResult>;
}

export interface CaptureVideoOptions {
  /**
   * Maximum duration per video clip.
   */
  duration?: number;

  /**
   * Quality of the video. `uhd` for 4K ultra HD video size (2160p). `fhd` for full HD video size (1080p). `hd` for HD video size (720p). `sd` for SD video size (480p).
   */
  quality?: 'uhd' | 'fhd' | 'hd' | 'sd';

  /**
   * Max file size in bytes.
   */
  sizeLimit?: number;
}

export interface MediaFileResult {
  file: MediaFile;
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
