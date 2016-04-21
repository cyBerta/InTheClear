package info.guardianproject.views;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import info.guardianproject.intheclear.R;

/**
 * Created by richy on 16.04.16.
 */
public class PanicItem extends LinearLayout{
    private ImageView checkBoxImage;
    private TextView itemText;

    public PanicItem(Context context) {
        super(context);

        init();
    }

    public PanicItem(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public PanicItem(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public PanicItem(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    private void init() {
        inflate(getContext(), R.layout.panic_item, this);
        this.checkBoxImage = (ImageView) findViewById(R.id.item_image);
        this.itemText = (TextView) findViewById(R.id.item_text);
    }

    public void setItemSelected(boolean selected){
        this.checkBoxImage.setImageResource(selected ? R.drawable.green_checkmark : R.drawable.red_minus);
    }
    public void setItemText(String text){
        if (text != null) this.itemText.setText(text);
    }

    public void resetItem(){
        this.checkBoxImage.setImageResource(-1);
        this.itemText.setText("");
    }
 }
