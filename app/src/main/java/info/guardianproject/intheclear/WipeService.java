
package info.guardianproject.intheclear;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;



import java.util.Timer;
import java.util.TimerTask;

public class WipeService extends Service {
    boolean callbackAttached = false;
    String callbackClass;
    Context _c;
    Timer t;
    TimerTask tt;

    public class LocalBinder extends Binder {
        public WipeService getService() {
            return WipeService.this;
        }
    }

    private final IBinder binder = new LocalBinder();

    public void addCallbackTo(String callbackClass) {
        this.callbackClass = callbackClass;
        this.callbackAttached = true;
    }

    public void wipePIMData(Context c, boolean contacts, boolean photos, boolean callLog,
            boolean sms, boolean calendar, boolean sdcard) {
        new PIMWiper(getBaseContext(), contacts, photos, callLog, sms, calendar, sdcard).start();

        // kill the calling activity
        Intent toKill = new Intent();
        Log.d(ITCConstants.Log.ITC, "the kill filter is called: " + c.getClass().toString());
        toKill.setAction(c.getClass().toString());
        getBaseContext().sendBroadcast(toKill);

    }

    @Override
    public IBinder onBind(Intent i) {
        return binder;
    }
}
