import { WebPlugin } from '@capacitor/core';

import type { MediaCapturePlugin } from './definitions';

export class MediaCaptureWeb extends WebPlugin implements MediaCapturePlugin {
  async echo(options: { value: string }): Promise<{ value: string }> {
    console.log('ECHO', options);
    return options;
  }
}
