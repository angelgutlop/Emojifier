package com.example.angel.emojifier;

import android.content.Context;
import android.content.res.Resources;
import android.util.TypedValue;

public class ResourcesUtils {

    public static float getDimen(Context context, int id) {


        Resources resources = context.getResources();
        TypedValue outValue = new TypedValue();
        resources.getValue(id, outValue, true);
        return outValue.getFloat();
    }
}
