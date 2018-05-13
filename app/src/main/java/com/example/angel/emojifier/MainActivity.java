package com.example.angel.emojifier;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.SparseArray;
import android.view.OrientationEventListener;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.angel.emojifier.FaceTraking.CameraSourcePreview;
import com.example.angel.emojifier.FaceTraking.GraphicOverlay;
import com.example.angel.emojifier.FaceTraking.TrackingFaces;
import com.google.android.gms.vision.face.Face;

import java.io.File;

import butterknife.BindView;
import butterknife.ButterKnife;

import static android.view.View.INVISIBLE;


public class MainActivity extends AppCompatActivity {
    @BindView(R.id.switchCameraButton)
    FloatingActionButton switchCameraFloantingButton;
    @BindView(R.id.closeImageButton)
    FloatingActionButton closeImageFloantingButton;
    @BindView(R.id.shareButton)
    FloatingActionButton shareImageButton;
    @BindView(R.id.saveButton)
    FloatingActionButton saveImageButton;
    @BindView(R.id.takePictureImageButton)
    ImageButton takePictureImageButton;
    @BindView(R.id.progressBar)
    ProgressBar progressBar;
    @BindView(R.id.messageDuringDetecting)
    TextView progressBarTex;
    @BindView(R.id.photoCapture)
    ImageView capturedPhotoImageView;
    @BindView(R.id.faceOverlay)
    GraphicOverlay mGraphicOverlay;
    @BindView(R.id.cameraSource)
    CameraSourcePreview cameraSourcePreview;

    private static final int REQUEST_STORAGE_PERMISSION = 1;
    private static final int REQUEST_CAMERA = 2;


    TrackingFaces trackingFaces = new TrackingFaces();

    private boolean canWriteExternalExtorage = false;

    private File photoFile = null;
    private Bitmap photoBitmap;

    private OrientationEventListener mOrientationListener;

    private DetectFaces detectFacesTask = new DetectFaces();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ButterKnife.bind(this);


        //Oculta los botones de captura y rotacion de camara
        getSupportActionBar().hide();

        hideImage();
        showProgresbar(false);


        takePictureImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cameraSourcePreview.takePicture(new CameraSourcePreview.BitmapCallback() {
                    @Override
                    public void onBitmapReady(Bitmap bitmap) {
                        processCapturePhoto(bitmap);
                    }
                });
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


        trackingFaces.configureCamera(this, mGraphicOverlay);


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

        cameraSourcePreview.stop();
        super.onDestroy();
    }


    private void startCamera() {

        cameraSourcePreview.setVisibility(View.VISIBLE);

        cameraSourcePreview.start(trackingFaces, mGraphicOverlay);

        takePictureImageButton.setVisibility(View.VISIBLE);
        switchCameraFloantingButton.setVisibility(View.VISIBLE);
    }


    private void stopHideCamera() {

        cameraSourcePreview.setVisibility(View.INVISIBLE);
        cameraSourcePreview.stop();


        takePictureImageButton.setVisibility(INVISIBLE);
        switchCameraFloantingButton.setVisibility(INVISIBLE);
    }


    private void switchCamera() {
        cameraSourcePreview.toggleCamera();


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

        capturedPhotoImageView.setVisibility(INVISIBLE);
        closeImageFloantingButton.setVisibility(INVISIBLE);

        shareImageButton.setVisibility(INVISIBLE);
        saveImageButton.setVisibility(INVISIBLE);
        capturedPhotoImageView.setVisibility(INVISIBLE);
    }

    private void showProgresbar(boolean show) {
        int status = show ? View.VISIBLE : View.INVISIBLE;
        progressBar.setVisibility(status);
        progressBarTex.setVisibility(status);

    }


    private void return2Camera() {
        hideImage();
        BitmapUtils.deleteTempFile(MainActivity.this, photoFile);
        photoFile = null;
        photoBitmap = null;
        startCamera();
    }

    @Override
    public void onBackPressed() {

        if (cameraSourcePreview.getVisibility() == INVISIBLE) {
            detectFacesTask.cancel(true);
            showProgresbar(false);
            return2Camera();

        } else super.onBackPressed();

    }

    Bitmap rescaledBMP;

    private void processCapturePhoto(Bitmap bmp) {


        stopHideCamera();
        showProgresbar(true);


        Bitmap rotatedBmp = BitmapUtils.RotateBitmap(bmp, 0);

        //c reescale bmp to fit the screen resolution

        rescaledBMP = BitmapUtils.resamplePic(this, rotatedBmp);

        detectFacesTask = new DetectFaces();
        detectFacesTask.execute(rescaledBMP);

    }


    private void processCapturePhotoEnd(SparseArray<Face> faces) {
        Bitmap facesBMP = BitmapUtils.dibujarEmojis(this, rescaledBMP, faces);

        showImage(facesBMP);
        showProgresbar(false);

        canWriteExternalExtorage = chekRequestPremissions(Manifest.permission.WRITE_EXTERNAL_STORAGE, REQUEST_STORAGE_PERMISSION);

        photoBitmap = facesBMP;
        if (canWriteExternalExtorage) {
            photoFile = BitmapUtils.createTempFile(this, facesBMP);
        }

    }


    private void rotateViews(int orientation) {
        int rotationAngle = 0;
        if (orientation == 90 || orientation == 270) rotationAngle = orientation - 180;

        switchCameraFloantingButton.setRotation(rotationAngle);
    }


    public void compartirImagen(View view) {
        BitmapUtils.saveImage(this, photoBitmap);
        BitmapUtils.shareImage(this, photoFile);
    }

    public void salvarImagen(View view) {
        BitmapUtils.saveImage(this, photoBitmap);
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
        // Called when you request permission to read and write to externa storage

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


    private class DetectFaces extends AsyncTask<Bitmap, Integer, SparseArray<Face>> {
        protected SparseArray<Face> doInBackground(Bitmap... bmps) {
            Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
            Bitmap bmp = bmps[0];
            return Emojifier.detectFaces(MainActivity.this, bmp);
        }

        protected void onPostExecute(SparseArray<Face> faces) {
            processCapturePhotoEnd(faces);
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
        }
    }

}
