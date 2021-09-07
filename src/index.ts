import { registerPlugin } from '@capacitor/core';

import type { MediaCapturePlugin } from './definitions';

const MediaCapture = registerPlugin<MediaCapturePlugin>('MediaCapture', {
  web: () => import('./web').then(m => new m.MediaCaptureWeb()),
});

export * from './definitions';
export { MediaCapture };
