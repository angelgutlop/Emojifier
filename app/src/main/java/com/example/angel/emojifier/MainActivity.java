package com.example.angel.emojifier;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.OrientationEventListener;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import com.otaliastudios.cameraview.CameraListener;
import com.otaliastudios.cameraview.CameraUtils;
import com.otaliastudios.cameraview.CameraView;
import com.otaliastudios.cameraview.Facing;

import java.io.File;


public class MainActivity extends AppCompatActivity {

    private FloatingActionButton switchCameraFloantingButton;

    private FloatingActionButton closeImageFloantingButton;
    private FloatingActionButton shareImageButton;
    private FloatingActionButton saveImageButton;

    private ImageButton takePictureImageButton;


    private CameraView cameraView;
    private ImageView capturedPhotoImageView;

    private static final int REQUEST_STORAGE_PERMISSION = 1;
    private static final int REQUEST_CAMERA = 2;


    private boolean canWriteExternalExtorage = false;

    private File tempPhotoFile = null;

    private OrientationEventListener mOrientationListener;
    private Bitmap tempBitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        takePictureImageButton = findViewById(R.id.takePictureImageButton);

        switchCameraFloantingButton = findViewById(R.id.switchCameraButton);
        closeImageFloantingButton = findViewById(R.id.closeImageButton);
        shareImageButton = findViewById(R.id.shareButton);
        saveImageButton = findViewById(R.id.saveButton);

        cameraView = findViewById(R.id.cameraView);
        capturedPhotoImageView = findViewById(R.id.photoCapture);

        //Oculta los botones de captura y rotacion de camara
        hideImage();


        takePictureImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cameraView.capturePicture();
            }
        });


        switchCameraFloantingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchCamera();
            }
        });

        closeImageFloantingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                return2Camera();
            }
        });

        //Configura la cámara para que procese la imagen; Esta funcion se llama tras capturepicture en el evento click


        final CameraUtils.BitmapCallback bitmapCallback = new CameraUtils.BitmapCallback() {
            @Override
            public void onBitmapReady(Bitmap bitmap) {
                processCapturePhoto(bitmap);
            }
        };

        cameraView.addCameraListener(new CameraListener() {
            @Override
            public void onPictureTaken(byte[] picture) {
                CameraUtils.decodeBitmap(picture, bitmapCallback);
            }
        });

        cameraView.setFacing(Facing.FRONT);

        //Pide los permisos necesarios para usar la camara
        if (chekRequestPremissions(Manifest.permission.CAMERA, REQUEST_CAMERA)) startCamera();
        canWriteExternalExtorage = chekRequestPremissions(Manifest.permission.WRITE_EXTERNAL_STORAGE, REQUEST_STORAGE_PERMISSION);


        //Controla la orientacion del dispositivo
        mOrientationListener = new OrientationEventListener(getApplicationContext()) {
            @Override
            public void onOrientationChanged(int orientation) {
                rotateViews(orientation);
            }
        };


        if (mOrientationListener.canDetectOrientation()) mOrientationListener.enable();
        else mOrientationListener.disable();


    }

    @Override
    protected void onDestroy() {
        mOrientationListener.disable();
        cameraView.stop();
        super.onDestroy();
    }


    private void startCamera() {

        cameraView.setVisibility(View.VISIBLE);
        cameraView.start();
        takePictureImageButton.setVisibility(View.VISIBLE);
        switchCameraFloantingButton.setVisibility(View.VISIBLE);
    }


    private void stopHideCamera() {

        cameraView.setVisibility(View.INVISIBLE);
        cameraView.stop();

        takePictureImageButton.setVisibility(View.INVISIBLE);
        switchCameraFloantingButton.setVisibility(View.INVISIBLE);
    }


    private void switchCamera() {

        cameraView.toggleFacing();

    }

    private void showImage(Bitmap bmp) {

        capturedPhotoImageView.setVisibility(View.VISIBLE);
        closeImageFloantingButton.setVisibility(View.VISIBLE);

        shareImageButton.setEnabled(canWriteExternalExtorage);
        saveImageButton.setEnabled(canWriteExternalExtorage);

        shareImageButton.setVisibility(View.VISIBLE);
        saveImageButton.setVisibility(View.VISIBLE);

        capturedPhotoImageView.setVisibility(View.VISIBLE);
        capturedPhotoImageView.setImageBitmap(bmp);
    }


    private void hideImage() {

        capturedPhotoImageView.setVisibility(View.INVISIBLE);
        closeImageFloantingButton.setVisibility(View.INVISIBLE);

        shareImageButton.setVisibility(View.INVISIBLE);
        saveImageButton.setVisibility(View.INVISIBLE);
        capturedPhotoImageView.setVisibility(View.INVISIBLE);
    }


    private void return2Camera() {
        hideImage();
        BitmapUtils.deleteTempFile(MainActivity.this, tempPhotoFile);
        tempPhotoFile = null;
        tempBitmap = null;
        startCamera();
    }

    @Override
    public void onBackPressed() {
        if (cameraView.getVisibility() == View.INVISIBLE) return2Camera();
        else super.onBackPressed();

    }

    private void processCapturePhoto(Bitmap bmp) {

        canWriteExternalExtorage = chekRequestPremissions(Manifest.permission.WRITE_EXTERNAL_STORAGE, REQUEST_STORAGE_PERMISSION);

        stopHideCamera();

        Bitmap rotatedBmp = BitmapUtils.RotateBitmap(bmp, 0);

        //c reescale bmp to fit the screen resolution

        Bitmap rescaledBmp = BitmapUtils.resamplePic(this, rotatedBmp);

        Bitmap detectedBMP = Emojifier.detectFaces(this, rescaledBmp);

        tempBitmap = rotatedBmp;

        if (canWriteExternalExtorage) {
            tempPhotoFile = BitmapUtils.createTempFile(this, rotatedBmp);
        }

        // capturedPhotoImageView.setImageBitmap(rotatedBmp);
        showImage(detectedBMP);

    }


    private void rotateViews(int orientation) {
        int rotationAngle = 0;
        if (orientation == 90 || orientation == 270) rotationAngle = orientation - 180;

        // takePictureImageButton.setRotation(rotationAngle);

        switchCameraFloantingButton.setRotation(rotationAngle);
    }


    public void compartirImagen(View view) {
        BitmapUtils.saveImage(this, tempBitmap);
        BitmapUtils.shareImage(this, tempPhotoFile);
    }

    public void salvarImagen(View view) {
        BitmapUtils.saveImage(this, tempBitmap);
    }


    private boolean chekRequestPremissions(String PERMISO, int idPERMISO) {


        if (ContextCompat.checkSelfPermission(this, PERMISO) != PackageManager.PERMISSION_GRANTED) {

            // Solicita los permisos y espera a la respuesta del usuario en onRequestPermissionsResult
            ActivityCompat.requestPermissions(this, new String[]{PERMISO}, idPERMISO);
            return false;
        } else {
            return true;

        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        // Called when you request permission to read and write to external storage

        if (grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

            switch (requestCode) {

                case REQUEST_CAMERA: {
                    startCamera();
                    break;
                }

                case REQUEST_STORAGE_PERMISSION: {
                    canWriteExternalExtorage = true;
                    break;
                }
            }
        } else {
            //Si no se conceden los permisos, avisa al usuario
            Toast.makeText(this, R.string.permission_denied, Toast.LENGTH_SHORT).show();
        }
    }


}
