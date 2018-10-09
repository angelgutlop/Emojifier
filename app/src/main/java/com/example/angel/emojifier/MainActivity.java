package com.example.angel.emojifier;

import android.Manifest;
import android.app.Activity;
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
import java.util.Arrays;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

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
    @BindView(R.id.cameraSourcePreview)
    CameraSourcePreview cameraSourcePreview;
    @BindView(R.id.cameraOffImageView)
    ImageView cameraOffImageView;

    private static final int REQUEST_STORAGE_PERMISSION = 1;
    private static final int REQUEST_CAMERA = 2;


    TrackingFaces trackingFaces = new TrackingFaces();
    TrackingFaces.CAMERA_FACING mFacing = TrackingFaces.CAMERA_FACING.FRONT;

    private boolean canWriteExternalExtorage = false;
    private boolean canUseCamera = false;


    private File photoFile = null;
    private Bitmap photoBitmap;


    private DetectFaces detectFacesTask = new DetectFaces();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ButterKnife.bind(this);


        if (savedInstanceState != null) {
            String keyFacing = getResources().getString(R.string.keyFacing);
            if (savedInstanceState.containsKey(keyFacing)) {

                mFacing = TrackingFaces.CAMERA_FACING.valueOf(savedInstanceState.getString(keyFacing));
            }
        }

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


        //Pide los permisos necesarios para usar la camara


        canWriteExternalExtorage = chekRequestPremissions(Manifest.permission.WRITE_EXTERNAL_STORAGE, REQUEST_STORAGE_PERMISSION);
        canUseCamera = chekRequestPremissions(Manifest.permission.CAMERA, REQUEST_CAMERA);

        switchCameraFloantingButton.setVisibility(INVISIBLE);
        takePictureImageButton.setEnabled(false);

        if (canUseCamera) showCamera();

        hideImage();
        showProgresbar(false);

    }

    @Override
    protected void onDestroy() {
if (trackingFaces!=null)        trackingFaces.release();
        super.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {

        outState.putString(getResources().getString(R.string.keyFacing), mFacing.toString());
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onPause() {
        cameraSourcePreview.stop();
        super.onPause();
    }


    @Override
    protected void onResume() {
        if (canUseCamera) {
            startCamera();
        }
        super.onResume();

    }

    private void startCamera() {

        takePictureImageButton.setEnabled(true);

        trackingFaces.configureCamera(this, mGraphicOverlay, mFacing);
        cameraSourcePreview.start(trackingFaces, mGraphicOverlay);
        mGraphicOverlay.clear();
    }

    private void showCamera() {
        cameraOffImageView.setVisibility(View.INVISIBLE);
        cameraSourcePreview.setVisibility(View.VISIBLE);
        takePictureImageButton.setVisibility(View.VISIBLE);
        switchCameraFloantingButton.setVisibility(View.VISIBLE);
    }


    private void stopHideCamera() {


        cameraSourcePreview.setVisibility(View.INVISIBLE);
        cameraSourcePreview.stop();


        takePictureImageButton.setVisibility(INVISIBLE);
        switchCameraFloantingButton.setVisibility(INVISIBLE);
    }

    @OnClick(R.id.switchCameraButton)
    public void switchCamera() {
        cameraSourcePreview.toggleCamera();
        mFacing = cameraSourcePreview.getCameraFacing();
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

    @OnClick(R.id.closeImageButton)
    public void return2Camera() {
        hideImage();
        BitmapUtils.deleteTempFile(MainActivity.this, photoFile);
        photoFile = null;
        photoBitmap = null;

        showCamera();
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


        Bitmap rotatedBmp = BitmapUtils.RotateBitmap(bmp, 90);

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

private boolean requestingPermissions= false;
    private boolean chekRequestPremissions(final String PERMISO, final int idPERMISO) {


        if (ContextCompat.checkSelfPermission(this, PERMISO) != PackageManager.PERMISSION_GRANTED) {
            // Solicita los permisos y espera a la respuesta del usuario en onRequestPermissionsResult

            final Activity activity= this;
            Thread tr =new Thread( new Runnable() {
                @Override
                public void run() {
                    while(requestingPermissions==true) {
                        try {
                            Thread.sleep(10);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    };
                    requestingPermissions= true;
                    ActivityCompat.requestPermissions(activity, new String[]{PERMISO}, idPERMISO );

                }
            });
            tr.start();
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
                    canUseCamera = true;
                    showCamera();
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
            Toast.makeText(this, getResources().getString(R.string.permission_denied) + " " +Arrays.toString(permissions), Toast.LENGTH_SHORT).show();
        }

        requestingPermissions= false;
    }


    private class DetectFaces extends AsyncTask<Bitmap, Integer, SparseArray<Face>> {

        protected SparseArray<Face> doInBackground(Bitmap... bmps) {
            Thread.currentThread().setPriority(Thread.MAX_PRIORITY);

            Bitmap bmp = bmps[0];
            //   return trackingFaces.getFaces();
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





