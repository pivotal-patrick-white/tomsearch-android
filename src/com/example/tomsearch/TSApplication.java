package com.example.tomsearch;

import android.app.Application;
import android.graphics.Bitmap;
import android.support.v4.util.LruCache;

public class TSApplication extends Application {
    private LruCache<String, Bitmap> imageCache = null;

    public void setImageCache(LruCache<String, Bitmap> cache) {
        if (imageCache == null) {
            imageCache = cache;
        }
    }

    public LruCache<String, Bitmap> getImageCache() {
        return imageCache;
    }
}
