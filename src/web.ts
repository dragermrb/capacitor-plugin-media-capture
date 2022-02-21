import { WebPlugin } from '@capacitor/core';

import type {
  CaptureVideoOptions,
  MediaCapturePlugin,
  MediaFileResult,
} from './definitions';

export class MediaCaptureWeb extends WebPlugin implements MediaCapturePlugin {
  captureVideo(options: CaptureVideoOptions): Promise<MediaFileResult> {
    console.log('captureVideo', options);

    throw this.unimplemented('Not implemented on web.');
  }
}
