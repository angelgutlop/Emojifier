package com.example.angel.emojifier.FaceTraking;

import android.content.Context;

import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.MultiProcessor;
import com.google.android.gms.vision.Tracker;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;

public class TrackingFaces {


    private static final String TAG = "TrackingFaces";
    private GraphicOverlay mGraphicOverlay;
    private Context mContext;
    FaceDetector detector = null;

    private static final float FPS = 30;
    private static final int VRES = 720;
    private static final int HRES = 1080;


    private CameraSource mCameraSource = null;

    public CameraSource getCameraSource() {
        return mCameraSource;
    }


    //todo release detector
    public void configureCamera(Context context, GraphicOverlay overlay) {

        mContext = context;
        mGraphicOverlay = overlay;

        detector = createDetector(mContext);

        mCameraSource = new CameraSource.Builder(context, detector)
                .setRequestedPreviewSize(HRES, VRES)
                .setFacing(CameraSource.CAMERA_FACING_BACK)
                .setRequestedFps(FPS)
                .setAutoFocusEnabled(true)
                .build();
    }


    public CameraSource switchCamera() {

        detector.release();
        detector = null;

        detector = createDetector(mContext);

        int newFacing;

        int oldFacing = mCameraSource.getCameraFacing();

        if (oldFacing == CameraSource.CAMERA_FACING_BACK)
            newFacing = CameraSource.CAMERA_FACING_FRONT;
        else newFacing = CameraSource.CAMERA_FACING_BACK;

        mCameraSource = new CameraSource.Builder(mContext, detector)
                .setRequestedPreviewSize(HRES, VRES)
                .setFacing(newFacing)
                .setRequestedFps(FPS)
                .setAutoFocusEnabled(true)
                .build();

        return mCameraSource;
    }

    private FaceDetector createDetector(Context context) {
        FaceDetector det = new FaceDetector.Builder(mContext)
                .setLandmarkType(FaceDetector.ALL_LANDMARKS)
                .setClassificationType(FaceDetector.ALL_CLASSIFICATIONS)
                .build();

        det.setProcessor(new MultiProcessor.Builder<>(new GraphicFaceTrackerFactory()).build());

        return det;

    }


    private class GraphicFaceTrackerFactory implements MultiProcessor.Factory<Face> {
        @Override
        public Tracker<Face> create(Face face) {
            return new GraphicFaceTracker(mGraphicOverlay, mContext);
        }
    }

    private class GraphicFaceTracker extends Tracker<Face> {
        private GraphicOverlay mOverlay;
        private FaceGraphic mFaceGraphic;

        GraphicFaceTracker(GraphicOverlay overlay, Context context) {
            mOverlay = overlay;
            mFaceGraphic = new FaceGraphic(overlay, context);
        }

        /**
         * Start tracking the detected face instance within the face overlay.
         */
        @Override
        public void onNewItem(int faceId, Face item) {
            mFaceGraphic.setId(faceId);
        }

        /**
         * Update the position/characteristics of the face within the overlay.
         */
        @Override
        public void onUpdate(FaceDetector.Detections<Face> detectionResults, Face face) {
            mOverlay.add(mFaceGraphic);
            mFaceGraphic.updateFace(face);
        }

        /**
         * Hide the graphic when the corresponding face was not detected.  This can happen for
         * intermediate frames temporarily (e.g., if the face was momentarily blocked from
         * view).
         */
        @Override
        public void onMissing(FaceDetector.Detections<Face> detectionResults) {
            mOverlay.remove(mFaceGraphic);
        }

        /**
         * Called when the face is assumed to be gone for good. Remove the graphic annotation from
         * the overlay.
         */
        @Override
        public void onDone() {
            mOverlay.remove(mFaceGraphic);
        }
    }

}
