
package info.guardianproject.intheclear;

import android.content.SharedPreferences;

public class WipeItem {
    public final int resId;
    public final boolean selected;

    public WipeItem(int resId, SharedPreferences prefs, String prefKey) {
        this.resId = resId;
        this.selected = prefs.getBoolean(prefKey, false);
    }
}
