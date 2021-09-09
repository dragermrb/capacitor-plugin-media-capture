var capacitorMediaCapture = (function (exports, core) {
    'use strict';

    const MediaCapture = core.registerPlugin('MediaCapture', {
        web: () => Promise.resolve().then(function () { return web; }).then(m => new m.MediaCaptureWeb()),
    });

    class MediaCaptureWeb extends core.WebPlugin {
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

    var web = /*#__PURE__*/Object.freeze({
        __proto__: null,
        MediaCaptureWeb: MediaCaptureWeb
    });

    exports.MediaCapture = MediaCapture;

    Object.defineProperty(exports, '__esModule', { value: true });

    return exports;

}({}, capacitorExports));
//# sourceMappingURL=plugin.js.map
