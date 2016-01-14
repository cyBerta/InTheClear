package info.guardianproject.views;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Pair;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;

import java.util.HashMap;
import java.util.HashSet;

/**
 * Created by richy on 08.01.16.
 */
public class PanicButton  extends ImageView
{
    private static final String TAG = PanicButton.class.getName();
    Bitmap panicButtonBtm;

    public PanicButton(Context context) {
        super(context);
        this.setDrawingCacheEnabled(true);
    }

    public PanicButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.setDrawingCacheEnabled(true);
    }

    public PanicButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.setDrawingCacheEnabled(true);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        panicButtonBtm = null;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (panicButtonBtm == null){
            panicButtonBtm = Bitmap.createBitmap(this.getDrawingCache());
            Log.d(TAG, "onTouch -> Bitmap created");
        }

        int color = 0;
        try {
            color = panicButtonBtm.getPixel((int) event.getX(), (int) event.getY());
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (color == Color.TRANSPARENT) {
            Log.d(TAG, "color is transparent -> return false");
            event.setAction(MotionEvent.ACTION_CANCEL);
        }

        return super.onTouchEvent(event);
    }

}
