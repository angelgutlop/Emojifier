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
import android.widget.ImageView;
import android.widget.Toast;

import com.otaliastudios.cameraview.CameraListener;
import com.otaliastudios.cameraview.CameraUtils;
import com.otaliastudios.cameraview.CameraView;


public class MainActivity extends AppCompatActivity {


    private FloatingActionButton takePictureFloantingButton;
    private FloatingActionButton switchCameraFloantingButton;

    private FloatingActionButton closeImageFloantingButton;


    private CameraView cameraView;
    private ImageView capturedPhotoImageView;

    private static final int REQUEST_STORAGE_PERMISSION = 1;
    private static final int REQUEST_CAMERA = 2;
    private static final int REQUEST_IMAGE_CAPTURE = 3;


    private OrientationEventListener mOrientationListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        takePictureFloantingButton = findViewById(R.id.takePictureButton);
        switchCameraFloantingButton = findViewById(R.id.switchCameraButton);
        closeImageFloantingButton = findViewById(R.id.closeImageButton);

        cameraView = findViewById(R.id.cameraView);
        capturedPhotoImageView = findViewById(R.id.photoCapture);

        //Oculta los botones de captura y rotacion de camara
        takePictureFloantingButton.setVisibility(View.INVISIBLE);
        switchCameraFloantingButton.setVisibility(View.INVISIBLE);
        closeImageFloantingButton.setVisibility(View.INVISIBLE);

        capturedPhotoImageView.setVisibility(View.INVISIBLE);

        takePictureFloantingButton.setOnClickListener(new View.OnClickListener() {
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
                startCamera();
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
                // Create a bitmap or a file...
                // CameraUtils will read EXIF orientation for you, in a worker thread.
                CameraUtils.decodeBitmap(picture, bitmapCallback);
            }
        });


        //Pide los permisos necesarios para usar la camara
        if (chekRequestPremissions(Manifest.permission.CAMERA, REQUEST_CAMERA)) startCamera();


        //Controla la orientacion del dispositivo
        mOrientationListener = new OrientationEventListener(getApplicationContext()) {
            @Override
            public void onOrientationChanged(int orientation) {
                rotateViews(orientation);
            }
        };


        if (mOrientationListener.canDetectOrientation() == true) mOrientationListener.enable();
        else mOrientationListener.disable();


    }

    @Override
    protected void onDestroy() {
        mOrientationListener.disable();
        cameraView.stop();
        super.onDestroy();

    }


    private void startCamera() {
        capturedPhotoImageView.setVisibility(View.INVISIBLE);
        closeImageFloantingButton.setVisibility(View.INVISIBLE);

        cameraView.setVisibility(View.VISIBLE);
        cameraView.start();

        takePictureFloantingButton.setVisibility(View.VISIBLE);
        switchCameraFloantingButton.setVisibility(View.VISIBLE);
    }


    private void stopHideCamera() {

        cameraView.setVisibility(View.INVISIBLE);
        cameraView.stop();

        takePictureFloantingButton.setVisibility(View.INVISIBLE);
        switchCameraFloantingButton.setVisibility(View.INVISIBLE);
    }


    private void switchCamera() {

        cameraView.toggleFacing();

    }

    private void processCapturePhoto(Bitmap bmp) {

        stopHideCamera();
        capturedPhotoImageView.setVisibility(View.VISIBLE);
        closeImageFloantingButton.setVisibility(View.VISIBLE);

        Bitmap rotatedBmp = BitmapUtils.RotateBitmap(bmp, 90);
        capturedPhotoImageView.setImageBitmap(rotatedBmp);


    }


    private void rotateViews(int orientation) {
        int rotationAngle = 0;
        if (orientation == 90 || orientation == 270) rotationAngle = orientation - 180;
        takePictureFloantingButton.setRotation(rotationAngle);
        switchCameraFloantingButton.setRotation(rotationAngle);
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

                    break;
                }
            }
        } else {
            //Si no se conceden los permisos, avisa al usuario
            Toast.makeText(this, R.string.permission_denied, Toast.LENGTH_SHORT).show();
        }
    }


}
