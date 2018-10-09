package com.example.angel.emojifier;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Environment;
import android.support.v4.content.FileProvider;
import android.util.DisplayMetrics;
import android.util.SparseArray;
import android.view.WindowManager;
import android.widget.Toast;

import com.google.android.gms.vision.face.Face;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import timber.log.Timber;

public class BitmapUtils {


    private static final String FILE_PROVIDER_AUTHORITY = "com.example.angel.emojifier.fileprovider";


    public static Bitmap flipBitmapHorizontally(Bitmap bitmap) {
        Matrix matrix = new Matrix();

        int cx = bitmap.getWidth() / 2;
        int cy = bitmap.getHeight() / 2;
        matrix.postScale(-1, 1, cx, cy);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

    public static Bitmap flipBitmapVertically(Bitmap bitmap) {
        Matrix matrix = new Matrix();

        int cx = bitmap.getWidth() / 2;
        int cy = bitmap.getHeight() / 2;
        matrix.postScale(1, -1, cx, cy);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

    public static Bitmap RotateBitmap(Bitmap source, float angle) {
        if (angle == 0) return source;
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }

    private static SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd-HHmmssS", Locale.ENGLISH);

    public static File createTempFile(Context context, Bitmap bitmap) {
        Date currentTime = Calendar.getInstance().getTime();
        String nombreFichero = simpleDateFormat.format(currentTime);
        File file = null;

        try {
            file = File.createTempFile(nombreFichero, ".jpg", context.getExternalCacheDir());

            FileOutputStream fOut = new FileOutputStream(file);

            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fOut);
            fOut.flush();
            fOut.close();

        } catch (IOException e) {
            Timber.d("Imposible escribir en la memoria externa: %s", e.getMessage());
            e.printStackTrace();
        }
        return file;
    }

    public static boolean deleteTempFile(Context context, File file) {
        if (file != null) return file.delete();
        return false;
    }


    static String saveImage(Context context, Bitmap image) {

        String savedImagePath = null;

        Date currentTime = Calendar.getInstance().getTime();
        String nombreFichero = simpleDateFormat.format(currentTime);

        String imageFileName = "JPEG_" + nombreFichero + ".jpg";
        File storageDir = new File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                        + "/Emojify");

        boolean success = true;
        if (!storageDir.exists()) {
            success = storageDir.mkdirs();
        }

        // Save the new Bitmap
        if (success) {
            File imageFile = new File(storageDir, imageFileName);
            savedImagePath = imageFile.getAbsolutePath();
            try {
                OutputStream fOut = new FileOutputStream(imageFile);
                image.compress(Bitmap.CompressFormat.JPEG, 100, fOut);
                fOut.close();
            } catch (Exception e) {
                e.printStackTrace();
            }

            // Add the image to the system gallery
            galleryAddPic(context, savedImagePath);

            // Show a Toast with the save location

            Toast.makeText(context, "Imagen salvada en" + savedImagePath, Toast.LENGTH_SHORT).show();
        }

        return savedImagePath;
    }


    static void shareImage(Context context, File imageFile) {
        // Create the share intent and start the share activity

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("image/*");
        shareIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        Uri photoURI = FileProvider.getUriForFile(context, FILE_PROVIDER_AUTHORITY, imageFile);
        shareIntent.putExtra(Intent.EXTRA_STREAM, photoURI);
        context.startActivity(shareIntent);
    }


    private static void galleryAddPic(Context context, String imagePath) {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File f = new File(imagePath);
        Uri contentUri = Uri.fromFile(f);
        mediaScanIntent.setData(contentUri);
        context.sendBroadcast(mediaScanIntent);
    }


    static Bitmap resamplePic(Context context, Bitmap bitmap) {

        // Get device screen size information
        DisplayMetrics metrics = new DisplayMetrics();
        WindowManager manager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);


        if (manager != null) {
            manager.getDefaultDisplay().getMetrics(metrics);
        } else {
            return bitmap;
        }


        int targetH = metrics.heightPixels;
        int targetW = metrics.widthPixels;

        // Get the dimensions of the original bitmap
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;

        byte[] bmpBytearray = bmp2Byte(bitmap);

        BitmapFactory.decodeByteArray(bmpBytearray, 0, bmpBytearray.length);

        int photoW = bmOptions.outWidth;
        int photoH = bmOptions.outHeight;

        // Determine how much to scale down the image
        int scaleFactor = Math.min(photoW / targetW, photoH / targetH);

        // Decode the image file into a Bitmap sized to fill the View
        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = scaleFactor;

        return BitmapFactory.decodeByteArray(bmpBytearray, 0, bmpBytearray.length);

    }


    public static Bitmap dibujarEmojis(Context context, Bitmap fondo, SparseArray<Face> faces) {

        Bitmap tmpBMP = fondo;

        for (int i = 0; i < faces.size(); i++) {
            tmpBMP = superponerEmoji(context, tmpBMP, faces.valueAt(i));
        }

        return tmpBMP;
    }

    public static Bitmap superponerEmoji(Context context, Bitmap fondo, Face face) {

        Bitmap workingBitmap = Bitmap.createBitmap(fondo);
        Bitmap finalBmp = workingBitmap.copy(Bitmap.Config.ARGB_8888, true);

        double scale = 0.9;


        int emojiRId = Emojifier.getEmoji(context, face);
        Bitmap emojiBMP = BitmapFactory.decodeResource(context.getResources(), emojiRId);

        emojiBMP = flipBitmapHorizontally(emojiBMP);

        double aspectRatio = (double) emojiBMP.getHeight() / (double) emojiBMP.getWidth();

        int emojiWith = (int) (face.getWidth() * scale);
        int emojiHeight = (int) (face.getWidth() * aspectRatio * scale);


        emojiBMP = Bitmap.createScaledBitmap(emojiBMP, emojiWith, emojiHeight, false);

        Canvas canvas = new Canvas(finalBmp);

        float emojiPositionX =
                (face.getPosition().x + face.getWidth() / 2) - emojiBMP.getWidth() / 2;
        float emojiPositionY =
                (face.getPosition().y + face.getHeight() / 2) - emojiBMP.getHeight() / 3;
        canvas.drawBitmap(emojiBMP, emojiPositionX, emojiPositionY, null);

        return finalBmp;

    }

    private static byte[] bmp2Byte(Bitmap bitmap) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
        return stream.toByteArray();

    }


}
