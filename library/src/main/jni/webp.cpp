//
// Created by liuchun on 2017/10/16.
//

#include <jni.h>
#include <array>
#include <memory>
#include <vector>
#include <android/bitmap.h>
#include <android/log.h>

#include "webp/decode.h"
#include "webp/demux.h"

#include "webp.h"
#include "jni_common.h"


using namespace glide;

/**
 * A holder for WebPDemuxer and its buffer. WebPDemuxer is needed by both WebPImage and
 * instances of WebPFrameIterator and it can't be released until all of them are done with it. This
 * wrapper is meant to be used inside of a std::shared_ptr to manage the resource.
 */
class WebPDemuxerWrapper {

public:
    WebPDemuxerWrapper(
            std::unique_ptr<WebPDemuxer, decltype(&WebPDemuxDelete)>&& pDemuxer,
            std::vector<uint8_t>&& pBuffer) :
            m_pDemuxer(std::move(pDemuxer)),
            m_pBuffer(std::move(pBuffer)) {
    }

    virtual ~WebPDemuxerWrapper() {
        //FBLOGD("Deleting Demuxer");
    }

    WebPDemuxer* get() {
        return m_pDemuxer.get();
    }

    size_t getBufferSize() {
        return m_pBuffer.size();
    }

private:
    std::unique_ptr<WebPDemuxer, decltype(&WebPDemuxDelete)> m_pDemuxer;
    std::vector<uint8_t> m_pBuffer;
};

/**
 * Native WebPImage struct
 */
struct WebPImage {

    /** Reference to the Demuxer */
    std::shared_ptr<WebPDemuxerWrapper> spDemuxer;

    /** Cached width of the image */
    int pixelWidth;

    /** Cached height of the image */
    int pixelHeight;

    /** Cached number of the frames in the image */
    int numFrames;

    /** Cached loop count for the image. 0 means infinite. */
    int loopCount;

    /** Duration of all the animation (the sum of all the frames duration) */
    int durationMs;

    /** Cached background color for the image frame rendering */
    int backgroundColor;

    /** Array of each frame's duration (size of array is numFrames) */
    std::vector<jint> frameDurationsMs;

    /** Reference counter. Instance is deleted when it goes from 1 to 0 */
    size_t refCount;
};

/**
 * Native WebPFrame struct
 */
struct WebPFrame {

    /* Reference to the Demuxer */
    std::shared_ptr<WebPDemuxerWrapper> spDemuxer;

    /** Frame number for the image. Starts at 1. */
    int frameNum;

    /** X offset for the frame relative to the image canvas */
    int xOffset;

    /** Y offset for the frame relative to the image canvas */
    int yOffset;

    /** Display duration for the frame in ms*/
    int durationMs;

    /** Width of this frame */
    int width;

    /** Height of this frame */
    int height;

    /** Whether the next frame might need to be blended with this frame */
    bool disposeToBackgroundColor;

    /** Whether this frame needs to be blended with the previous frame */
    bool blendWithPreviousFrame;

    /** Raw encoded bytes for the frame. Points to existing memory managed by WebPDemuxerWrapper */
    const uint8_t* pPayload;

    /** Size of payload in bytes */
    size_t payloadSize;

    /** Reference counter. Instance is deleted when it goes from 1 to 0 */
    size_t refCount;
};

// Class Names
static const char* const kWebpImageClassName =
        "com/bumptech/glide/integration/webp/WebpImage";
static const char* const kWebpFrameClassName =
        "com/bumptech/glide/integration/webp/WebpFrame";

// Cached fields related to WebPImage
static jclass sClazzWebPImage;
static jmethodID sWebPImageConstructor;
static jfieldID sWebPImageFieldNativePtr;

// Cached fields related to WebPFrame
static jclass sClazzWebPFrame;
static jmethodID sWebPFrameConstructor;
static jfieldID sWebPFrameFieldNativePtr;

////////////////////////////////////////////////////////////////
/// Related to WebPImage
////////////////////////////////////////////////////////////////

/**
 * Creates a new WebPImage from the specified buffer.
 *
 * @param vBuffer the vector containing the bytes
 * @return a newly allocated WebPImage
 */
jobject WebPImage_nativeCreateFromByteVector(JNIEnv* pEnv, std::vector<uint8_t>& vBuffer) {
    std::unique_ptr<WebPImage> spNativeWebpImage(new WebPImage());
    if (!spNativeWebpImage) {
        throwOutOfMemoryError(pEnv, "Unable to allocate native context");
        return 0;
    }

    // WebPData is on the stack as its only used during the call to WebPDemux.
    WebPData webPData;
    webPData.bytes = vBuffer.data();
    webPData.size = vBuffer.size();

    // Create the WebPDemuxer
    auto spDemuxer = std::unique_ptr<WebPDemuxer, decltype(&WebPDemuxDelete)> {
            WebPDemux(&webPData),
            WebPDemuxDelete
    };
    if (!spDemuxer) {
        // We may want to consider first using functions that will return a useful error code
        // if it fails to parse.
        throwIllegalArgumentException(pEnv, "Failed to create demuxer");
        //FBLOGW("unable to get demuxer");
        return 0;
    }

    spNativeWebpImage->pixelWidth = WebPDemuxGetI(spDemuxer.get(), WEBP_FF_CANVAS_WIDTH);
    spNativeWebpImage->pixelHeight = WebPDemuxGetI(spDemuxer.get(), WEBP_FF_CANVAS_HEIGHT);
    spNativeWebpImage->numFrames = WebPDemuxGetI(spDemuxer.get(), WEBP_FF_FRAME_COUNT);
    spNativeWebpImage->loopCount = WebPDemuxGetI(spDemuxer.get(), WEBP_FF_LOOP_COUNT);
    spNativeWebpImage->backgroundColor = WebPDemuxGetI(spDemuxer.get(), WEBP_FF_BACKGROUND_COLOR);

    // Compute cached fields that require iterating the frames.
    jint durationMs = 0;
    std::vector<jint> frameDurationsMs;
    WebPIterator iter;
    if (WebPDemuxGetFrame(spDemuxer.get(), 1, &iter)) {
        do {
            durationMs += iter.duration;
            frameDurationsMs.push_back(iter.duration);
        } while (WebPDemuxNextFrame(&iter));
        WebPDemuxReleaseIterator(&iter);
    }
    spNativeWebpImage->durationMs = durationMs;
    spNativeWebpImage->frameDurationsMs = frameDurationsMs;

    jintArray frameDurationsArr = pEnv->NewIntArray(spNativeWebpImage->numFrames);
    pEnv->SetIntArrayRegion(frameDurationsArr, 0, spNativeWebpImage->numFrames, spNativeWebpImage->frameDurationsMs.data());

    // Ownership of pDemuxer and vBuffer is transferred to WebPDemuxerWrapper here.
    // Note, according to Rob Arnold, createNew assumes we throw exceptions but we don't. Though
    // he claims this won't happen in practice cause "Linux will overcommit pages, we should only
    // get this error if we run out of virtual address space." Also, Daniel C may be working
    // on converting to exceptions.
    spNativeWebpImage->spDemuxer = std::shared_ptr<WebPDemuxerWrapper>(
            new WebPDemuxerWrapper(std::move(spDemuxer), std::move(vBuffer)));

    // Create the WebPImage with the native context.
    jobject ret = pEnv->NewObject(
            sClazzWebPImage,
            sWebPImageConstructor,
            (jlong) spNativeWebpImage.get(),
            (jint)spNativeWebpImage->pixelWidth,
            (jint)spNativeWebpImage->pixelHeight,
            (jint)spNativeWebpImage->numFrames,
            (jint)spNativeWebpImage->durationMs,
            frameDurationsArr,
            (jint)spNativeWebpImage->loopCount,
            (jint)spNativeWebpImage->backgroundColor);
    if (ret != nullptr) {
        // Ownership was transferred.
        spNativeWebpImage->refCount = 1;
        spNativeWebpImage.release();
    }
    return ret;
}


/**
 * Releases a reference to the WebPImageNativeContext and deletes it when the reference count
 * reaches 0
 */
void WebPImageNative_releaseRef(JNIEnv* pEnv, jobject thiz, WebPImage* p) {
    pEnv->MonitorEnter(thiz);
    p->refCount--;
    if (p->refCount == 0) {
        delete p;
    }
    pEnv->MonitorExit(thiz);
}

/**
 * Functor for getWebPImage that releases the reference.
 */
struct WebPImageNativeReleaser {
    JNIEnv* pEnv;
    jobject webpImage;

    WebPImageNativeReleaser(JNIEnv* pEnv, jobject webpImage) :
            pEnv(pEnv), webpImage(webpImage) {}

    void operator()(WebPImage* pNativeContext) {
        WebPImageNative_releaseRef(pEnv, webpImage, pNativeContext);
    }
};


/**
 * Gets the WebPImageNativeContext from the mNativeContext of the WebPImage object. This returns
 * a reference counted shared_ptr.
 *
 * @return the shared_ptr which will be a nullptr in the case where the object has already been
 *    disposed
 */
std::unique_ptr<WebPImage, WebPImageNativeReleaser>
getWebPImageNative(JNIEnv* pEnv, jobject thiz) {

    // A deleter that decrements the reference and possibly deletes the instance.
    WebPImageNativeReleaser releaser(pEnv, thiz);
    std::unique_ptr<WebPImage, WebPImageNativeReleaser> ret(nullptr, releaser);
    pEnv->MonitorEnter(thiz);
    WebPImage* pNativeContext =
            (WebPImage*) pEnv->GetLongField(thiz, sWebPImageFieldNativePtr);
    if (pNativeContext != nullptr) {
        pNativeContext->refCount++;
        ret.reset(pNativeContext);
    }
    pEnv->MonitorExit(thiz);
    return ret;
}

/**
 * Creates a new WebPImage from the specified byte buffer. The data from the byte buffer is copied
 * into native memory managed by WebPImage.
 *
 * @param byteBuffer A java.nio.ByteBuffer. Must be direct. Assumes data is the entire capacity
 *      of the buffer
 * @return a newly allocated WebPImage
 */
jobject WebPImage_nativeCreateFromDirectByteBuffer(JNIEnv* pEnv, jclass clazz, jobject byteBuffer) {
    jbyte* bbufInput = (jbyte*) pEnv->GetDirectBufferAddress(byteBuffer);
    if (!bbufInput) {
        throwIllegalArgumentException(pEnv, "ByteBuffer must be direct");
        return 0;
    }

    jlong capacity = pEnv->GetDirectBufferCapacity(byteBuffer);
    if (pEnv->ExceptionCheck()) {
        return 0;
    }

    std::vector<uint8_t> vBuffer(bbufInput, bbufInput + capacity);
    return WebPImage_nativeCreateFromByteVector(pEnv, vBuffer);
}


/**
 * Gets the Frame at the specified index.
 *
 * @param index the index of the frame
 * @return a newly created WebPFrame for the specified frame
 */
jobject WebPImage_nativeGetFrame(JNIEnv* pEnv, jobject thiz, jint index) {
    auto spNativeWebPImage = getWebPImageNative(pEnv, thiz);
    if (!spNativeWebPImage) {
        throwIllegalStateException(pEnv, "Already disposed");
        return nullptr;
    }

    WebPIterator iter = {0};

    // Note, in WebP, frame numbers are one-based.
    if (!WebPDemuxGetFrame(spNativeWebPImage->spDemuxer->get(), index + 1, &iter)) {
        throwIllegalStateException(pEnv, "unable to get frame");
        WebPDemuxReleaseIterator(&iter);
        return nullptr;
    }

    std::unique_ptr<WebPFrame> spNativeWebPFrame(new WebPFrame());
    if (!spNativeWebPFrame) {
        throwOutOfMemoryError(pEnv, "Unable to allocate WebPFrameNativeContext");
        WebPDemuxReleaseIterator(&iter);
        return nullptr;
    }

    spNativeWebPFrame->spDemuxer = spNativeWebPImage->spDemuxer;
    spNativeWebPFrame->frameNum = iter.frame_num;
    spNativeWebPFrame->xOffset = iter.x_offset;
    spNativeWebPFrame->yOffset = iter.y_offset;
    spNativeWebPFrame->durationMs = iter.duration;
    spNativeWebPFrame->width = iter.width;
    spNativeWebPFrame->height = iter.height;
    spNativeWebPFrame->disposeToBackgroundColor =
            iter.dispose_method == WEBP_MUX_DISPOSE_BACKGROUND;
    spNativeWebPFrame->blendWithPreviousFrame = iter.blend_method == WEBP_MUX_BLEND;
    spNativeWebPFrame->pPayload = iter.fragment.bytes;
    spNativeWebPFrame->payloadSize = iter.fragment.size;

    WebPDemuxReleaseIterator(&iter);
    jobject ret = pEnv->NewObject(
            sClazzWebPFrame,
            sWebPFrameConstructor,
            (jlong) spNativeWebPFrame.get(),
            (jint) spNativeWebPFrame->xOffset,
            (jint) spNativeWebPFrame->yOffset,
            (jint) spNativeWebPFrame->width,
            (jint) spNativeWebPFrame->height,
            (jint) spNativeWebPFrame->durationMs,
            (jboolean) spNativeWebPFrame->blendWithPreviousFrame,
            (jboolean) spNativeWebPFrame->disposeToBackgroundColor);
    if (ret != nullptr) {
        // Ownership was transferred.
        spNativeWebPFrame->refCount = 1;
        spNativeWebPFrame.release();
    }
    return ret;
}


/**
 * Releases a reference to the WebPFrameNativeContext and deletes it when the reference count
 * reaches 0
 */
void WebPFrameNative_releaseRef(JNIEnv* pEnv, jobject thiz, WebPFrame* p) {
    // clear pending exception before call MonitorEnter
    if (pEnv->ExceptionOccurred()) {
        pEnv->ExceptionClear();
    }

    pEnv->MonitorEnter(thiz);
    p->refCount--;
    if (p->refCount == 0) {
        delete p;
    }
    pEnv->MonitorExit(thiz);
}


/**
 * Functor for getWebPFrameNativeContext.
 */
struct WebPFrameNativeReleaser {
    JNIEnv* pEnv;
    jobject webpFrame;

    WebPFrameNativeReleaser(JNIEnv* pEnv, jobject webpFrame) :
            pEnv(pEnv), webpFrame(webpFrame) {}

    void operator()(WebPFrame* pNativeContext) {
        WebPFrameNative_releaseRef(pEnv, webpFrame, pNativeContext);
    }
};


/**
 * Gets the WebPFrameNativeContext from the mNativeContext of the WebPFrame object. This returns
 * a reference counted pointer.
 *
 * @return the reference counted pointer which will be a nullptr in the case where the object has
 *    already been disposed
 */
std::unique_ptr<WebPFrame, WebPFrameNativeReleaser>
getWebPFrameNative(JNIEnv* pEnv, jobject thiz) {

    WebPFrameNativeReleaser releaser(pEnv, thiz);
    std::unique_ptr<WebPFrame, WebPFrameNativeReleaser> ret(nullptr, releaser);
    pEnv->MonitorEnter(thiz);
    WebPFrame* pNativeContext =
            (WebPFrame*) pEnv->GetLongField(thiz, sWebPFrameFieldNativePtr);
    if (pNativeContext != nullptr) {
        pNativeContext->refCount++;
        ret.reset(pNativeContext);
    }
    pEnv->MonitorExit(thiz);
    return ret;
}

/**
 * Gets the size in bytes used by the {@link WebPImage}. The implementation only takes into
 * account the encoded data buffer as the other data structures are relatively tiny.
 *
 * @return approximate size in bytes used by the {@link WebPImage}
 */
jint WebPImage_nativeGetSizeInBytes(JNIEnv* pEnv, jobject thiz) {
    auto spNativeWebPImage = getWebPImageNative(pEnv, thiz);
    if (!spNativeWebPImage) {
        throwIllegalStateException(pEnv, "Already disposed");
        return 0;
    }
    return spNativeWebPImage->spDemuxer->getBufferSize();
}


/**
 * Disposes the WebImage, freeing native resources.
 */
void WebImage_nativeDispose(JNIEnv* pEnv, jobject thiz) {
    pEnv->MonitorEnter(thiz);
    WebPImage* pNativeContext =
            (WebPImage*) pEnv->GetLongField(thiz, sWebPImageFieldNativePtr);
    if (pNativeContext != nullptr) {
        pEnv->SetLongField(thiz, sWebPImageFieldNativePtr, 0);
        WebPImageNative_releaseRef(pEnv, thiz, pNativeContext);
    }

    pEnv->MonitorExit(thiz);
}


/**
 * Finalizer for WebImage that frees native resources.
 */
void WebImage_nativeFinalize(JNIEnv* pEnv, jobject thiz) {
    WebImage_nativeDispose(pEnv, thiz);
}


////////////////////////////////////////////////////////////////
/// Related to WebPFrame
////////////////////////////////////////////////////////////////
/**
 * Renders the frame to the specified pixel array. The array is expected to have a size that
 * is at least the the width and height of the frame. The frame is rendered where each pixel is
 * represented as a 32-bit BGRA pixel. The rendered stride is the same as the frame width. Note,
 * the number of pixels written to the array may be smaller than the canvas if the frame's
 * width/height is smaller than the canvas.
 *
 * @param jPixels the array to render into
 */
void WebPFrame_nativeRenderFrame(
        JNIEnv* pEnv,
        jobject thiz,
        jint width,
        jint height,
        jobject bitmap) {
    auto spNativeWebPFrame = getWebPFrameNative(pEnv, thiz);
    if (!spNativeWebPFrame) {
        throwIllegalStateException(pEnv, "Already disposed");
        return;
    }

    AndroidBitmapInfo bitmapInfo;
    if (AndroidBitmap_getInfo(pEnv, bitmap, &bitmapInfo) != ANDROID_BITMAP_RESULT_SUCCESS) {
        throwIllegalStateException(pEnv, "Bad bitmap");
        return;
    }

    if (width < 0 || height < 0) {
        throwIllegalArgumentException(pEnv, "Width or height is negative !");
        return;
    }

    if (bitmapInfo.width < (unsigned) width || bitmapInfo.height < (unsigned) height) {
        throwIllegalStateException(pEnv, "Width or height is too small");
        return;
    }

    if (bitmapInfo.format != ANDROID_BITMAP_FORMAT_RGBA_8888) {
        spNativeWebPFrame.reset();
        throwIllegalStateException(pEnv, "Wrong color format");
        return;
    }

    WebPDecoderConfig config;
    int ret = WebPInitDecoderConfig(&config);
    if (!ret) {
        throwIllegalStateException(pEnv, "WebPInitDecoderConfig failed");
        return;
    }

    const uint8_t* pPayload = spNativeWebPFrame->pPayload;
    size_t payloadSize = spNativeWebPFrame->payloadSize;

    ret = (WebPGetFeatures(pPayload , payloadSize, &config.input) == VP8_STATUS_OK);
    if (!ret) {
        spNativeWebPFrame.reset();
        throwIllegalStateException(pEnv, "WebPGetFeatures failed");
        return;
    }

    uint8_t* pixels;
    if (AndroidBitmap_lockPixels(pEnv, bitmap, (void**) &pixels) != ANDROID_BITMAP_RESULT_SUCCESS) {
        spNativeWebPFrame.reset();
        throwIllegalStateException(pEnv, "Bad bitmap");
        return;
    }

    config.options.no_fancy_upsampling = 1;
    if (width != spNativeWebPFrame->width || height != spNativeWebPFrame->height) {
        config.options.use_scaling = true;
        config.options.scaled_width = width;
        config.options.scaled_height = height;
    }

    config.output.colorspace = MODE_rgbA;
    config.output.is_external_memory = 1;
    config.output.u.RGBA.rgba = pixels;
    config.output.u.RGBA.stride = bitmapInfo.stride;
    config.output.u.RGBA.size   = bitmapInfo.stride * bitmapInfo.height;

    ret = WebPDecode(pPayload, payloadSize, &config);
    AndroidBitmap_unlockPixels(pEnv, bitmap);
    if (ret != VP8_STATUS_OK) {
        __android_log_print(ANDROID_LOG_WARN, "GLIDE_WEBP",
                        "Failed to decode frame, ret=%d", ret);
        spNativeWebPFrame.reset();
        throwIllegalStateException(pEnv, "Failed to decode frame. VP8StatusCode: %d", ret);
    }
}


/**
 * Disposes the WebPFrameIterator, freeing native resources.
 */
void WebPFrame_nativeDispose(JNIEnv* pEnv, jobject thiz) {
    // clear pending exception before call MonitorEnter
    if (pEnv->ExceptionOccurred()) {
        pEnv->ExceptionClear();
    }

    //__android_log_print(ANDROID_LOG_DEBUG, "GLIDE_WEBP", "MonitorEnter called in WebPFrame_nativeDispose");
    pEnv->MonitorEnter(thiz);
    WebPFrame* pNativeContext =
            (WebPFrame*) pEnv->GetLongField(thiz, sWebPFrameFieldNativePtr);
    if (pNativeContext) {
        pEnv->SetLongField(thiz, sWebPFrameFieldNativePtr, 0);
        WebPFrameNative_releaseRef(pEnv, thiz, pNativeContext);
    }
    pEnv->MonitorExit(thiz);
}

/**
 * Finalizer for WebPFrame that frees native resources.
 */
void WebPFrame_nativeFinalize(JNIEnv* pEnv, jobject thiz) {
    WebPFrame_nativeDispose(pEnv, thiz);
}


////////////////////////////////////////////////////////////////
/// Related to WebPBitmapFactory
////////////////////////////////////////////////////////////////
#define RETURN_NULL_IF_EXCEPTION(env) \
  if (env->ExceptionOccurred()) {\
    return {};\
  }

static constexpr const char* kWebpBitmapFactoryClassName = "com/bumptech/glide/integration/webp/WebpBitmapFactory";

static jclass webpBitmapFactoryClass;
static jclass runtimeExceptionClass;

static jmethodID createBitmapFunction;
static jmethodID setOutDimensionsFunction;


std::vector<uint8_t> readStreamFully(JNIEnv* env, jobject is, jbyteArray inTempStorage) {
    // read start
    std::vector<uint8_t> read_buffer;

    jclass inputStreamJClass = env->FindClass("java/io/InputStream");
    jmethodID readMethodId = env->GetMethodID(inputStreamJClass, "read", "([B)I");

    while (true) {

        const int chunk_size = env->CallIntMethod(is, readMethodId, inTempStorage);

        if (chunk_size < 0) {
            return read_buffer;
        }

        if (chunk_size > 0) {
            jbyte* data = env->GetByteArrayElements(inTempStorage, nullptr);
            RETURN_NULL_IF_EXCEPTION(env);

            read_buffer.insert(read_buffer.end(), data, data + chunk_size);
            env->ReleaseByteArrayElements(inTempStorage, data, JNI_ABORT);
            RETURN_NULL_IF_EXCEPTION(env);
        }
    }
}

static jboolean setOutDimensions(JNIEnv* env, jobject bitmapOptions, int image_width, int image_height) {
    jboolean hadDecodeBounds = env->CallStaticBooleanMethod(webpBitmapFactoryClass, setOutDimensionsFunction, bitmapOptions, image_width, image_height);
    return hadDecodeBounds;
}


static jobject createBitmap(JNIEnv* env, int image_width, int image_height, jobject bitmapOptions) {
    jobject bitmap = env->CallStaticObjectMethod(webpBitmapFactoryClass, createBitmapFunction, image_width, image_height, bitmapOptions);
    return bitmap;
}


jobject doDecode(
        JNIEnv* env,
        uint8_t* encoded_image,
        unsigned encoded_image_length,
        jobject bitmapOptions,
        jfloat scale) {

    // Options manipulation is taken from https://github.com/android/platform_frameworks_base/blob/master/core/jni/android/graphics/BitmapFactory.cpp
    int image_width = 0;
    int image_height = 0;

    jobject bitmap = nullptr;

    WebPGetInfo(
            encoded_image,
            encoded_image_length,
            &image_width,
            &image_height);

//    WebPBitstreamFeatures features;
//    WebPGetFeatures(
//            encoded_image,
//            encoded_image_length,
//            &features);
//
//    __android_log_print(ANDROID_LOG_INFO, "GLIDE_WEBP",
//                        "width=%d, height=%d, hasAlpha=%s, hasAnimation=%s",
//                        features.width,
//                        features.height,
//                        features.has_alpha ? "true" : "false",
//                        features.has_animation ? "true" : "false");

    __android_log_print(ANDROID_LOG_INFO, "GLIDE_WEBP","webp width:%d, height:%d, scale:%f", image_width, image_height, scale);

    WebPDecoderConfig config;
    WebPInitDecoderConfig(&config);

    if ((bitmapOptions != nullptr) &&
        (setOutDimensions(env, bitmapOptions, image_width, image_height))) {
        return {};
    }

    if (scale != 1.0f) {
        image_width = int(image_width * scale + 0.5f);
        image_height = int(image_height  * scale + 0.5f);
        config.options.use_scaling = 1;
        config.options.scaled_width = image_width;
        config.options.scaled_height = image_height;
    }

    __android_log_print(ANDROID_LOG_INFO, "GLIDE_WEBP","bitmap width:%d, height:%d, scale:%f", image_width, image_height, scale);
    bitmap = createBitmap(env, image_width, image_height, bitmapOptions);
    RETURN_NULL_IF_EXCEPTION(env);

    AndroidBitmapInfo bitmapInfo;
    int rc = AndroidBitmap_getInfo(env, bitmap, &bitmapInfo);
    if (rc != ANDROID_BITMAP_RESULT_SUCCESS) {
        env->ThrowNew(runtimeExceptionClass, "Decode error get bitmap info");
        return JNI_FALSE;
    }

    void* raw_pixels = nullptr;

    rc = AndroidBitmap_lockPixels(env, bitmap, (void**) &raw_pixels);
    if (rc != ANDROID_BITMAP_RESULT_SUCCESS) {
        env->ThrowNew(runtimeExceptionClass, "Decode error locking pixels");
        return JNI_FALSE;
    }

    config.output.colorspace = MODE_rgbA;
    config.output.u.RGBA.rgba = (uint8_t*) raw_pixels;
    config.output.u.RGBA.stride = bitmapInfo.stride;
    config.output.u.RGBA.size = bitmapInfo.height * bitmapInfo.stride;
    config.output.is_external_memory = 1;

    WebPDecode(encoded_image, encoded_image_length, &config);

    rc = AndroidBitmap_unlockPixels(env, bitmap);
    if (rc != ANDROID_BITMAP_RESULT_SUCCESS) {
        env->ThrowNew(runtimeExceptionClass, "Decode error unlocking pixels");
        return {};
    }

    if (bitmapOptions != nullptr) {
        setOutDimensions(env, bitmapOptions, image_width, image_height);
    }

    return bitmap;
}


static jobject nativeDecodeStream(
        JNIEnv* env,
        jclass clazz,
        jobject is,
        jobject bitmapOptions,
        jfloat scale,
        jbyteArray inTempStorage) {
    auto encoded_image = readStreamFully(env, is, inTempStorage);
    if (!encoded_image.empty()) {
        return doDecode(env, encoded_image.data(), encoded_image.size(), bitmapOptions, scale);
    }
    return {};

}

static jobject nativeDecodeByteArray(
        JNIEnv* env,
        jclass clazz,
        jbyteArray array,
        jint offset,
        jint length,
        jobject bitmapOptions,
        jfloat scale,
        jbyteArray inTempStorage) {

    // get image into decoded heap
    jbyte* data = env->GetByteArrayElements(array, nullptr);
    if (env->ExceptionCheck() == JNI_TRUE) {
        env->ReleaseByteArrayElements(inTempStorage, data, JNI_ABORT);
        RETURN_NULL_IF_EXCEPTION(env);
    }
    if (data == nullptr || offset + length > env->GetArrayLength(array)) {
        env->ReleaseByteArrayElements(array, data, JNI_ABORT);
        RETURN_NULL_IF_EXCEPTION(env);
    }
    jobject bitmap = doDecode(env, reinterpret_cast<uint8_t*>(data) + offset, length, bitmapOptions, scale);
    env->ReleaseByteArrayElements(array, data, JNI_ABORT);
    RETURN_NULL_IF_EXCEPTION(env);

    return bitmap;
}


static JNINativeMethod sWebPImageMethods[] = {
        { "nativeCreateFromDirectByteBuffer",
                "(Ljava/nio/ByteBuffer;)Lcom/bumptech/glide/integration/webp/WebpImage;",
                (void*)WebPImage_nativeCreateFromDirectByteBuffer },
        { "nativeGetFrame",
                "(I)Lcom/bumptech/glide/integration/webp/WebpFrame;",
                (void*)WebPImage_nativeGetFrame },
        { "nativeGetSizeInBytes",
                "()I",
                (void*)WebPImage_nativeGetSizeInBytes },
        { "nativeDispose",
                "()V",
                (void*)WebImage_nativeDispose },
        { "nativeFinalize",
                "()V",
                (void*)WebImage_nativeFinalize },
};

static JNINativeMethod sWebPFrameMethods[] = {
        { "nativeRenderFrame",
                "(IILandroid/graphics/Bitmap;)V",
                (void*)WebPFrame_nativeRenderFrame },
        { "nativeDispose",
                "()V",
                (void*)WebPFrame_nativeDispose },
        { "nativeFinalize",
                "()V",
                (void*)WebPFrame_nativeFinalize },
};

static JNINativeMethod sWebpBitmapFactoryMethods[] = {
        {"nativeDecodeStream",
                "(Ljava/io/InputStream;Landroid/graphics/BitmapFactory$Options;F[B)Landroid/graphics/Bitmap;",
                (void*)nativeDecodeStream
        },
        {"nativeDecodeByteArray",
                "([BIILandroid/graphics/BitmapFactory$Options;F[B)Landroid/graphics/Bitmap;",
                (void*)nativeDecodeByteArray
        },
};


/**
 * Called by JNI_OnLoad to initialize the classes.
 */
int initWebPImage(JNIEnv* pEnv) {
    // WebPImage
    sClazzWebPImage = findClassOrThrow(pEnv, kWebpImageClassName);
    if (sClazzWebPImage == NULL) {
        return JNI_ERR;
    }

    // WebPImage.mNativePtr
    sWebPImageFieldNativePtr = getFieldIdOrThrow(pEnv, sClazzWebPImage, "mNativePtr", "J");
    if (!sWebPImageFieldNativePtr) {
        return JNI_ERR;
    }

    // WebPImage.<init>
    sWebPImageConstructor = getMethodIdOrThrow(pEnv, sClazzWebPImage, "<init>", "(JIIII[III)V");
    if (!sWebPImageConstructor) {
        return JNI_ERR;
    }

    int result = pEnv->RegisterNatives(
            sClazzWebPImage,
            sWebPImageMethods,
            std::extent<decltype(sWebPImageMethods)>::value);
    if (result != JNI_OK) {
        return result;
    }

    // WebPFrame
    sClazzWebPFrame = findClassOrThrow(pEnv, kWebpFrameClassName);
    if (sClazzWebPFrame == NULL) {
        return JNI_ERR;
    }

    // WebPFrame.mNativePtr
    sWebPFrameFieldNativePtr = getFieldIdOrThrow(pEnv, sClazzWebPFrame, "mNativePtr", "J");
    if (!sWebPFrameFieldNativePtr) {
        return JNI_ERR;
    }

    // WebPFrame.<init>
    sWebPFrameConstructor = getMethodIdOrThrow(pEnv, sClazzWebPFrame, "<init>", "(JIIIIIZZ)V");
    if (!sWebPFrameConstructor) {
        return JNI_ERR;
    }

    result = pEnv->RegisterNatives(
            sClazzWebPFrame,
            sWebPFrameMethods,
            std::extent<decltype(sWebPFrameMethods)>::value);
    if (result != JNI_OK) {
        return result;
    }

    return JNI_OK;
}

/**
 * Called by JNI_OnLoad to initialize the classes.
 */
int initWebpBitmapFactory(JNIEnv* env) {
    // find java classes and method
    webpBitmapFactoryClass = findClassOrThrow(env, kWebpBitmapFactoryClassName);
    if (webpBitmapFactoryClass == NULL) {
        return JNI_ERR;
    }

    createBitmapFunction = env->GetStaticMethodID(webpBitmapFactoryClass, "createBitmap", "(IILandroid/graphics/BitmapFactory$Options;)Landroid/graphics/Bitmap;");
    if (!createBitmapFunction) {
        return JNI_ERR;
    }

    setOutDimensionsFunction = env->GetStaticMethodID(webpBitmapFactoryClass, "setOutDimensions", "(Landroid/graphics/BitmapFactory$Options;II)Z");
    if (!setOutDimensionsFunction) {
        return JNI_ERR;
    }

    int result = env->RegisterNatives(
            webpBitmapFactoryClass,
            sWebpBitmapFactoryMethods,
            std::extent<decltype(sWebpBitmapFactoryMethods)>::value);
    if (result != JNI_OK) {
        return result;
    }

    return JNI_OK;
}

/**
 * Called by VM when so load
 */
JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void* reserved) {
    JNIEnv* env;

    if (vm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_4) != JNI_OK) {
        return -1;
    }

    if (initWebPImage(env) != JNI_OK) {
        return -1;
    }

    if (initWebpBitmapFactory(env) != JNI_OK) {
        return -1;
    }

    return JNI_VERSION_1_4;
}