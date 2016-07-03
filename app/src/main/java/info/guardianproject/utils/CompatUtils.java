package info.guardianproject.utils;

import android.annotation.TargetApi;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.view.View;

/**
 * Created by l on 03.07.16.
 */
public class CompatUtils {

     public static void setBackgroundDrawable(View v, Drawable drawable){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
               setBackgroundDrawableNew( v,  drawable);
        }else {
            setBackgroundDrawableOld( v,  drawable);
       }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    public static void setBackgroundDrawableNew(View v, Drawable drawable) {
        v.setBackground(drawable);
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static void setBackgroundDrawableOld(View v, Drawable drawable) {
        v.setBackgroundDrawable(drawable);
    }
}
