import { registerPlugin } from '@capacitor/core';
const MediaCapture = registerPlugin('MediaCapture', {
    web: () => import('./web').then(m => new m.MediaCaptureWeb()),
});
export * from './definitions';
export { MediaCapture };
//# sourceMappingURL=index.js.map