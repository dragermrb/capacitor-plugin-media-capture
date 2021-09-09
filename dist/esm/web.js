import { WebPlugin } from '@capacitor/core';
export class MediaCaptureWeb extends WebPlugin {
    captureAudio(options) {
        console.log('captureAudio', options);
        throw this.unimplemented('Not implemented on web.');
    }
    captureImage(options) {
        console.log('captureImage', options);
        throw this.unimplemented('Not implemented on web.');
    }
    captureVideo(options) {
        console.log('captureVideo', options);
        throw this.unimplemented('Not implemented on web.');
    }
    getFormatData(options) {
        console.log('getFormatData', options);
        throw this.unimplemented('Not implemented on web.');
    }
}
//# sourceMappingURL=web.js.map