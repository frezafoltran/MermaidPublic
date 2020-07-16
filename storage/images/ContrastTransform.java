package com.foltran.mermaid.storage.images;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;

import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation;

import java.nio.charset.Charset;
import java.security.MessageDigest;

public class ContrastTransform extends BitmapTransformation {

    private static final String ID = "com.foltran";
    private final byte[] ID_BYTES = ID.getBytes(Charset.forName("UTF-8"));

    private float contrast;
    private float brightness;

    public ContrastTransform(float contrast, float brightness){
        this.contrast = contrast;
        this.brightness = brightness;
    }

    @Override
    public Bitmap transform(BitmapPool pool, Bitmap toTransform, int outWidth, int outHeight) {


        ColorMatrix cm = new ColorMatrix(new float[]
                {
                        contrast, 0, 0, 0, brightness,
                        0, contrast, 0, 0, brightness,
                        0, 0, contrast, 0, brightness,
                        0, 0, 0, 1, 0
                });
        Bitmap mEnhancedBitmap = Bitmap.createBitmap(toTransform.getWidth(), toTransform.getHeight(), toTransform
                .getConfig());
        Canvas canvas = new Canvas(mEnhancedBitmap);
        Paint paint = new Paint();
        paint.setColorFilter(new ColorMatrixColorFilter(cm));
        canvas.drawBitmap(toTransform, 0, 0, paint);

        return mEnhancedBitmap;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof ContrastTransform;
    }

    @Override
    public int hashCode() {
        return ID.hashCode();
    }

    @Override
    public void updateDiskCacheKey(MessageDigest messageDigest) {
        messageDigest.update(ID_BYTES);
    }
}
