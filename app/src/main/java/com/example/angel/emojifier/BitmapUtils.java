package com.example.angel.emojifier;

import android.graphics.Bitmap;
import android.graphics.Matrix;

public class BitmapUtils {

    public static Bitmap RotateBitmap(Bitmap source, float angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }
}
