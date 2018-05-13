package com.example.angel.emojifier;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.Log;
import android.util.SparseArray;
import android.widget.Toast;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;
import com.google.android.gms.vision.face.Landmark;


public class Emojifier {

    private static final String TAG = "Emojifier";
    private static FaceDetector detector;


    public enum ESTADO_OJOS {
        CERRADOS,
        DERECHO_CERRADO,
        IZQUIERDO_CERRADO,
        ABIERTOS,
        MUY_ABIERTOS,
        DESCONOCIDO;

    }

    public enum ESTADO_SONRISA {
        MUY_TRISTE,
        TRISTE,
        NEUTRAL,
        CONTENTO,
        HAPPY
    }

    public static SparseArray<Face> detectFaces(Context context, Bitmap bitmap) {

        detector = new FaceDetector.Builder(context)
                .setTrackingEnabled(false)
                .setClassificationType(FaceDetector.ALL_CLASSIFICATIONS)
                .setMode(FaceDetector.FAST_MODE)
                .build();


        if (!detector.isOperational()) {
            Toast.makeText(context, "El detector de caras no funciona en este dispositivo", Toast.LENGTH_SHORT).show();
            return null;
        }
        Frame frame = new Frame.Builder().setBitmap(bitmap).build();

        SparseArray<Face> faces = detector.detect(frame);
        detector.release();
        return faces;
    }


    public static Bitmap drawLandmarks(Bitmap bitmap, SparseArray<Face> faces) {


        Bitmap workingBitmap = Bitmap.createBitmap(bitmap);
        Bitmap mutableBitmap = workingBitmap.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(mutableBitmap);

        Paint paint = new Paint();
        paint.setColor(Color.GREEN);
        int scale = 1;

        for (int i = 0; i < faces.size(); ++i) {
            Face face = faces.valueAt(i);
            for (Landmark landmark : face.getLandmarks()) {
                int cx = (int) (landmark.getPosition().x * scale);
                int cy = (int) (landmark.getPosition().y * scale);
                canvas.drawCircle(cx, cy, 10, paint);
            }
        }

        return mutableBitmap;

    }


    public static void getClassifications(SparseArray<Face> faces) {


        for (int i = 0; i < faces.size(); i++) {

            Face face = faces.valueAt(i);
            float lefEyeProb = face.getIsLeftEyeOpenProbability();
            float rightEyeProb = face.getIsRightEyeOpenProbability();
            float smillingProb = face.getIsSmilingProbability();

            String formatrStr = "Face %d ---> Left Open: %f, Right Open: %f, Smilling Probability: %f";
            String mess = String.format(formatrStr, i, lefEyeProb, rightEyeProb, smillingProb);
            Log.d(TAG, mess);
        }
    }


    public static int getEmoji(Context context, Face face) {


        int id = 0;

        float smillingProb = face.getIsSmilingProbability();


        ESTADO_OJOS estado_ojos = getEyesStatus(context, face);


        switch (estado_ojos) {
            case DERECHO_CERRADO: {
                if (smillingProb < R.dimen.SONRISA_NEUTRO) return R.drawable.e00guino_der_triste;
                else return R.drawable.e02guino_der_sonrisa;
            }

            case IZQUIERDO_CERRADO: {
                if (smillingProb < R.dimen.SONRISA_NEUTRO) return R.drawable.e01guino_izq_triste;
                else return R.drawable.e03guino_izq_sonrisa;
            }

            case CERRADOS: {

                ESTADO_SONRISA estado_sonrisa = gestSmilleStatus(context, face);

                switch (estado_sonrisa) {
                    case MUY_TRISTE:
                        return R.drawable.e10cerrados_muy_triste;
                    case TRISTE:
                        return R.drawable.e11cerrados_triste;
                    case NEUTRAL:
                        return R.drawable.e12cerrados_normal;
                    case CONTENTO:
                        return R.drawable.e13cerrados_contento;
                    case HAPPY:
                        return R.drawable.e14cerrados_muy_contento;
                    default:
                        return R.drawable.e25sin_sonrisa;
                }

            }
            case DESCONOCIDO: {
                return R.drawable.e26noface;
            }

            default: {

                ESTADO_SONRISA estado_sonrisa = gestSmilleStatus(context, face);

                switch (estado_sonrisa) {
                    case MUY_TRISTE:
                        return R.drawable.e20abiertos_muy_triste;
                    case TRISTE:
                        return R.drawable.e21abiertos_triste;
                    case NEUTRAL:
                        if (estado_ojos == ESTADO_OJOS.MUY_ABIERTOS)
                            return R.drawable.e222muy_abiertos_neutral;
                        else return R.drawable.e221abiertos_neutral;
                    case CONTENTO:
                        return R.drawable.e23abiertos_contento;
                    case HAPPY:
                        return R.drawable.e24abiertos_muy_contento;
                    default:
                        return R.drawable.e25sin_sonrisa;
                }
            }
        }
    }


    public static ESTADO_OJOS getEyesStatus(Context context, Face face) {

        float lefEyeProb = face.getIsLeftEyeOpenProbability();
        float rightEyeProb = face.getIsRightEyeOpenProbability();
        float ojoNeutro = ResourcesUtils.getDimen(context, R.dimen.OJO_NEUTRO);

        if (lefEyeProb < ojoNeutro && rightEyeProb < ojoNeutro)
            return ESTADO_OJOS.CERRADOS;
        if (lefEyeProb > ojoNeutro && rightEyeProb < ojoNeutro)
            return ESTADO_OJOS.DERECHO_CERRADO;
        if (lefEyeProb < ojoNeutro && rightEyeProb > ojoNeutro)
            return ESTADO_OJOS.IZQUIERDO_CERRADO;

        float ojoMuyAbierto = ResourcesUtils.getDimen(context, R.dimen.OJO_MUY_ABIERTO);
        if (lefEyeProb > ojoMuyAbierto || rightEyeProb > ojoMuyAbierto)
            return ESTADO_OJOS.MUY_ABIERTOS;

        else if (lefEyeProb < ojoMuyAbierto || rightEyeProb < ojoMuyAbierto)
            return ESTADO_OJOS.ABIERTOS;

        return ESTADO_OJOS.DESCONOCIDO;
    }

    public static String getSmilletatusString(Context context, Face face) {
        Emojifier.ESTADO_SONRISA sonrisa = Emojifier.gestSmilleStatus(context, face);

        switch (sonrisa) {
            case HAPPY:
                return "Muy contento";
            case CONTENTO:
                return "Contento";
            case NEUTRAL:
                return "Poco animado";
            case TRISTE:
                return "Triste";
            case MUY_TRISTE:
                return "Muy triste";
        }
        return "";
    }

    public static String getEyeStatusString(Context context, Face face) {
        Emojifier.ESTADO_OJOS ojos = Emojifier.getEyesStatus(context, face);

        switch (ojos) {
            case MUY_ABIERTOS:
                return "Ojos muy abiertos";
            case ABIERTOS:
                return "Ojos abiertos";
            case CERRADOS:
                return "Ojos cerrados";
            case DERECHO_CERRADO:
                return "Guiño derecho";
            case IZQUIERDO_CERRADO:
                return "Giño izquierdo";
            case DESCONOCIDO:
                return "No se";
        }

        return "";


    }

    public static ESTADO_SONRISA gestSmilleStatus(Context context, Face face) {
        float smillingProb = face.getIsSmilingProbability();

        float muyTriste = ResourcesUtils.getDimen(context, R.dimen.SONRISA_MUY_TRISTE);
        float triste = ResourcesUtils.getDimen(context, R.dimen.SONRISA_TRISTE);
        float neutro = ResourcesUtils.getDimen(context, R.dimen.SONRISA_NEUTRO);
        float contento = ResourcesUtils.getDimen(context, R.dimen.SONRISA_CONTENTO);
        float muyContento = ResourcesUtils.getDimen(context, R.dimen.SONRISA_HAPPY);


        if (smillingProb > muyTriste && smillingProb < triste)
            return ESTADO_SONRISA.MUY_TRISTE;
        else if (smillingProb >= triste && smillingProb < neutro)
            return ESTADO_SONRISA.TRISTE;
        else if (smillingProb >= neutro && smillingProb < contento)
            return ESTADO_SONRISA.NEUTRAL;
        else if (smillingProb >= contento && smillingProb < muyContento)
            return ESTADO_SONRISA.CONTENTO;
        else return ESTADO_SONRISA.HAPPY;

    }


}
