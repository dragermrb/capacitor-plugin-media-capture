# capacitor-plugin-media-capture

Capacitor plugin to capture audio and video

## Install

```bash
npm install capacitor-plugin-media-capture
npx cap sync
```

## API

<docgen-index>

* [`captureAudio(...)`](#captureaudio)
* [`captureImage(...)`](#captureimage)
* [`captureVideo(...)`](#capturevideo)
* [`getFormatData(...)`](#getformatdata)
* [Interfaces](#interfaces)

</docgen-index>

<docgen-api>
<!--Update the source file JSDoc comments and rerun docgen to update the docs below-->

### captureAudio(...)

```typescript
captureAudio(options: CaptureAudioOptions) => any
```

| Param         | Type                                                                |
| ------------- | ------------------------------------------------------------------- |
| **`options`** | <code><a href="#captureaudiooptions">CaptureAudioOptions</a></code> |

**Returns:** <code>any</code>

--------------------


### captureImage(...)

```typescript
captureImage(options: CaptureImageOptions) => any
```

| Param         | Type                                                                |
| ------------- | ------------------------------------------------------------------- |
| **`options`** | <code><a href="#captureimageoptions">CaptureImageOptions</a></code> |

**Returns:** <code>any</code>

--------------------


### captureVideo(...)

```typescript
captureVideo(options: CaptureVideoOptions) => any
```

| Param         | Type                                                                |
| ------------- | ------------------------------------------------------------------- |
| **`options`** | <code><a href="#capturevideooptions">CaptureVideoOptions</a></code> |

**Returns:** <code>any</code>

--------------------


### getFormatData(...)

```typescript
getFormatData(options: FormatDataOptions) => any
```

| Param         | Type                                                            |
| ------------- | --------------------------------------------------------------- |
| **`options`** | <code><a href="#formatdataoptions">FormatDataOptions</a></code> |

**Returns:** <code>any</code>

--------------------


### Interfaces


#### CaptureAudioOptions

| Prop           | Type                | Description                                                                                 |
| -------------- | ------------------- | ------------------------------------------------------------------------------------------- |
| **`duration`** | <code>number</code> | Maximum duration of an audio sound clip, in seconds. This does not work on Android devices. |


#### MediaFileResult

| Prop       | Type                                            |
| ---------- | ----------------------------------------------- |
| **`file`** | <code><a href="#mediafile">MediaFile</a></code> |


#### MediaFile

| Prop       | Type                | Description                                     |
| ---------- | ------------------- | ----------------------------------------------- |
| **`name`** | <code>string</code> | The name of the file, without path information. |
| **`path`** | <code>string</code> | The full path of the file, including the name.  |
| **`type`** | <code>string</code> | The file's mime type                            |
| **`size`** | <code>number</code> | The size of the file, in bytes.                 |


#### CaptureImageOptions

| Prop        | Type                | Description                                                                                                           |
| ----------- | ------------------- | --------------------------------------------------------------------------------------------------------------------- |
| **`limit`** | <code>number</code> | Maximum number of images to capture. This limit is not supported on iOS, only one image will be taken per invocation. |


#### CaptureVideoOptions

| Prop           | Type                | Description                                                         |
| -------------- | ------------------- | ------------------------------------------------------------------- |
| **`duration`** | <code>number</code> | Maximum duration per video clip.                                    |
| **`quality`**  | <code>number</code> | Quality of the video. This parameter can only be used with Android. |


#### FormatDataOptions

| Prop           | Type                |
| -------------- | ------------------- |
| **`filePath`** | <code>string</code> |
| **`mimeType`** | <code>string</code> |


#### MediaFileData

| Prop           | Type                | Description                                                                     |
| -------------- | ------------------- | ------------------------------------------------------------------------------- |
| **`codecs`**   | <code>string</code> | The actual format of the audio and video content.                               |
| **`bitrate`**  | <code>number</code> | The average bitrate of the content. The value is zero for images.               |
| **`height`**   | <code>number</code> | The height of the image or video in pixels. The value is zero for audio clips.  |
| **`width`**    | <code>number</code> | The width of the image or video in pixels. The value is zero for audio clips.   |
| **`duration`** | <code>number</code> | The length of the video or sound clip in seconds. The value is zero for images. |

</docgen-api>
