package info.guardianproject.intheclear;

import android.content.DialogInterface;

/**
 * Created by richy on 16.05.16.
 */
public interface CountdownProgressDialogInterface {
    interface OnCountdownFinishedListener {
        void onCountdownFinished(boolean firstShout);
    }

}
