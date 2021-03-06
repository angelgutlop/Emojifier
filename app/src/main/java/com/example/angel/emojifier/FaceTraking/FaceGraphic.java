package com.example.angel.emojifier.FaceTraking;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import com.example.angel.emojifier.Emojifier;
import com.google.android.gms.vision.face.Face;

/**
 * Graphic instance for rendering face position, orientation, and landmarks within an associated
 * <p>
 * graphic overlay view.
 */

class FaceGraphic extends GraphicOverlay.Graphic {


    private static final float ID_TEXT_SIZE = 40.0f;
    private static final float BOX_STROKE_WIDTH = 5.0f;

    private static final int COLOR_CHOICES[] = {
            Color.BLUE,
            Color.CYAN,
            Color.GREEN,
            Color.MAGENTA,
            Color.RED,
            Color.WHITE,
            Color.YELLOW
    };

    private static int mCurrentColorIndex = 0;

    private Paint mFacePositionPaint;
    private Paint mIdPaint;
    private Paint mBoxPaint;
    private volatile Face mFace;
    private int mFaceId;
    private Context mContext;

    private boolean showAdvancedData = true;

    FaceGraphic(GraphicOverlay overlay, Context context) {

        super(overlay);

        mContext = context;

        mCurrentColorIndex = (mCurrentColorIndex + 1) % COLOR_CHOICES.length;
        final int selectedColor = COLOR_CHOICES[mCurrentColorIndex];
        mFacePositionPaint = new Paint();
        mFacePositionPaint.setColor(selectedColor);
        mIdPaint = new Paint();
        mIdPaint.setColor(selectedColor);
        mIdPaint.setTextSize(ID_TEXT_SIZE);
        mBoxPaint = new Paint();
        mBoxPaint.setColor(selectedColor);
        mBoxPaint.setStyle(Paint.Style.STROKE);
        mBoxPaint.setStrokeWidth(BOX_STROKE_WIDTH);
    }

    void setId(int id) {
        mFaceId = id;
    }

    public int getId() {
        return mFaceId;
    }

    /**
     * Updates the face instance from the detection of the most recent frame. Invalidates the
     * <p>
     * relevant portions of the overlay to trigger a redraw.
     */

    void updateFace(Face face) {
        mFace = face;
        postInvalidate();

    }

    /**
     * Draws the face annotations for position on the supplied canvas.
     */

    @Override

    public void draw(Canvas canvas) {

        Face face = mFace;

        if (face == null) {

            return;

        }

// Draws a circle at the position of the detected face, with the face's track id below.

        float x = translateX(face.getPosition().x + face.getWidth() / 2);
        float y = translateY(face.getPosition().y + face.getHeight() / 2);


// Draws a bounding box around the face.

        float xOffset = scaleX(face.getWidth() / 2.0f);
        float yOffset = scaleY(face.getHeight() / 2.0f);
        float left = x - xOffset;
        float top = y - yOffset;
        float right = x + xOffset;
        float bottom = y + yOffset;


        canvas.drawRect(left, top, right, bottom, mBoxPaint);


        String felicidadString = Emojifier.getSmilletatusString(mContext, face);
        String ojosString = Emojifier.getEyeStatusString(mContext, face);

        if (showAdvancedData) {

            felicidadString = felicidadString + String.format("(%1.1f)", face.getIsSmilingProbability());
            ojosString = ojosString + String.format("(%1.1f-%1.1f)", face.getIsLeftEyeOpenProbability(), face.getIsRightEyeOpenProbability());
        }

        canvas.drawText("Felicidad: " + felicidadString, left, bottom + ID_TEXT_SIZE, mIdPaint);
        canvas.drawText(ojosString, left, bottom + 2 * ID_TEXT_SIZE, mIdPaint);

    }


}
