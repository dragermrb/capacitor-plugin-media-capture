import { WebPlugin } from '@capacitor/core';

import type {
  CaptureAudioOptions,
  CaptureImageOptions,
  CaptureVideoOptions,
  FormatDataOptions,
  MediaCapturePlugin,
  MediaFileData,
  MediaFileResult,
} from './definitions';

export class MediaCaptureWeb extends WebPlugin implements MediaCapturePlugin {
  captureAudio(options: CaptureAudioOptions): Promise<MediaFileResult> {
    console.log('captureAudio', options);

    throw this.unimplemented('Not implemented on web.');
  }

  captureImage(options: CaptureImageOptions): Promise<MediaFileResult> {
    console.log('captureImage', options);

    throw this.unimplemented('Not implemented on web.');
  }

  captureVideo(options: CaptureVideoOptions): Promise<MediaFileResult> {
    console.log('captureVideo', options);

    throw this.unimplemented('Not implemented on web.');
  }

  getFormatData(options: FormatDataOptions): Promise<MediaFileData> {
    console.log('getFormatData', options);

    throw this.unimplemented('Not implemented on web.');
  }
}
