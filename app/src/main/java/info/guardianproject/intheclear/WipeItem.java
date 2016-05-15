
package info.guardianproject.intheclear;

import android.content.SharedPreferences;

public class WipeItem {
    private final int resId;
    private final boolean selected;

    public WipeItem(int resId, SharedPreferences prefs, String prefKey) {
        this.resId = resId;
        this.selected = prefs.getBoolean(prefKey, false);
    }

    public int getResId() {
        return resId;
    }

    public boolean getSelected(){
        return selected;
    }
}
