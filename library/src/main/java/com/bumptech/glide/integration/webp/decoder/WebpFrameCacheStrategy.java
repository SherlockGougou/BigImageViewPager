package com.bumptech.glide.integration.webp.decoder;

/**
 * author: liuchun
 * date: 2019-05-15
 */
public final class WebpFrameCacheStrategy {

    public static final WebpFrameCacheStrategy NONE = new Builder().noCache().build();

    public static final WebpFrameCacheStrategy AUTO = new Builder().cacheAuto().build();

    public static final WebpFrameCacheStrategy ALL = new Builder().cacheAll().build();

    public enum CacheControl {
        CACHE_NONE,
        CACHE_LIMITED,
        CACHE_AUTO,
        CACHE_ALL,
    }

    private CacheControl mCacheStrategy;
    private int mCacheSize;

    private WebpFrameCacheStrategy(Builder builder) {
        this.mCacheStrategy = builder.cacheControl;
        this.mCacheSize = builder.cacheSize;
    }

    public CacheControl getCacheControl() {
        return this.mCacheStrategy;
    }

    public boolean noCache() {
        return mCacheStrategy == CacheControl.CACHE_NONE;
    }

    public boolean cacheAuto() {
        return mCacheStrategy == CacheControl.CACHE_AUTO;
    }

    public boolean cacheAll() {
        return mCacheStrategy == CacheControl.CACHE_ALL;
    }

    public int getCacheSize() {
        return this.mCacheSize;
    }

    public final static class Builder {
        private CacheControl cacheControl;
        private int cacheSize;

        public Builder noCache() {
            this.cacheControl = CacheControl.CACHE_NONE;
            return this;
        }

        public Builder cacheAll() {
            this.cacheControl = CacheControl.CACHE_ALL;
            return this;
        }

        public Builder cacheAuto() {
            this.cacheControl = CacheControl.CACHE_AUTO;
            return this;
        }

        public Builder cacheLimited() {
            this.cacheControl = CacheControl.CACHE_LIMITED;
            return this;
        }

        public Builder cacheControl(CacheControl control) {
            this.cacheControl = control;
            return this;
        }

        public Builder cacheSize(int cacheSize) {
            this.cacheSize = cacheSize;
            if (cacheSize == 0) {
                this.cacheControl = CacheControl.CACHE_NONE;
            } else if (cacheSize == Integer.MAX_VALUE) {
                this.cacheControl = CacheControl.CACHE_ALL;
            } else {
                this.cacheControl = CacheControl.CACHE_LIMITED;
            }
            return this;
        }

        public WebpFrameCacheStrategy build() {
            return new WebpFrameCacheStrategy(this);
        }
    }
}
