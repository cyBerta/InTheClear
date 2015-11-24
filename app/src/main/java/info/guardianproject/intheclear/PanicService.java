
package info.guardianproject.intheclear;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;

import info.guardianproject.intheclear.ITCConstants.Preference;

import java.io.File;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class PanicService extends IntentService {
    private static final String TAG = "PanicController";

    public static final int UPDATE_PROGRESS = 0;

    public PanicService() {
        super(TAG);
    }

    private NotificationManager nm;
    private SharedPreferences prefs;
    private ResultReceiver resultReceiver;

    TimerTask shoutTimerTask;
    Timer t = new Timer();
    final Handler h = new Handler();
    boolean isPanicing = false;

    Intent backToPanic;
    int panicCount = 0;

    WipeService wipeService;
    ShoutController shoutController;

    ArrayList<File> selectedFolders;
    String userDisplayName, defaultPanicMsg, configuredFriends;

    @Override
    public void onCreate() {
        super.onCreate();
        nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (!TextUtils.isEmpty(PhoneInfo.getIMEI()))
            shoutController = new ShoutController(getBaseContext());
        backToPanic = new Intent(this, PanicActivity.class);
        alignPreferences();
        showNotification();
    }

    private void alignPreferences() {
        prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());

        selectedFolders = new ArrayList<File>();
        userDisplayName = prefs.getString(ITCConstants.Preference.USER_DISPLAY_NAME, "");
        configuredFriends = prefs.getString(ITCConstants.Preference.CONFIGURED_FRIENDS, "");
    }

    public void updatePanicUi(String message) {
        if (resultReceiver != null) {
            final Bundle bundle = new Bundle();
            bundle.putString(ITCConstants.UPDATE_UI, message);
            resultReceiver.send(UPDATE_PROGRESS, bundle);
        }
    }

    private int shout() {
        if (shoutController == null)
            return ITCConstants.Results.NOT_AVAILABLE;
        int result = ITCConstants.Results.FAIL;
        updatePanicUi(getString(R.string.KEY_PANIC_PROGRESS_1));

        shoutTimerTask = new TimerTask() {

            @Override
            public void run() {
                h.post(new Runnable() {

                    @Override
                    public void run() {
                        if (isPanicing) {
                            // TODO: this should actually be confirmed.
                            shoutController.sendSMSShout(
                                    configuredFriends,
                                    defaultPanicMsg,
                                    ShoutController.buildShoutData(getResources())
                                    );
                            Log.d(ITCConstants.Log.ITC, "this is a shout going out...");
                            panicCount++;
                        }
                    }
                });
            }

        };

        t.schedule(shoutTimerTask, 0, ITCConstants.Duriation.CONTINUED_PANIC);
        result = ITCConstants.Results.A_OK;
        return result;
    }

    private int wipe() {
        int result = ITCConstants.Results.FAIL;
        updatePanicUi(getString(R.string.KEY_PANIC_PROGRESS_2));
        new PIMWiper(
                getBaseContext(),
                prefs.getBoolean(Preference.DEFAULT_WIPE_CONTACTS, false),
                prefs.getBoolean(Preference.DEFAULT_WIPE_PHOTOS, false),
                prefs.getBoolean(Preference.DEFAULT_WIPE_CALLLOG, false),
                prefs.getBoolean(Preference.DEFAULT_WIPE_SMS, false),
                prefs.getBoolean(Preference.DEFAULT_WIPE_CALENDAR, false),
                prefs.getBoolean(Preference.DEFAULT_WIPE_FOLDERS, false)).start();
        result = ITCConstants.Results.A_OK;
        return result;
    }

    private void stopRunnables() {
        if (isPanicing) {
            if (shoutTimerTask != null)
                shoutTimerTask.cancel();
            isPanicing = false;
        }
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.i(TAG, "onHandleIntent " + intent);
        String packageName = intent.getPackage();
        Log.i(TAG, "getPackage() " + packageName);
        // TODO use TrustedIntents here to check trust

        resultReceiver = intent.getParcelableExtra(PanicActivity.RESULT_RECEIVER);

        isPanicing = true;
        int shoutResult = shout();
        if (shoutResult == ITCConstants.Results.A_OK
                || shoutResult == ITCConstants.Results.NOT_AVAILABLE)
            if (wipe() == ITCConstants.Results.A_OK) {
                updatePanicUi(getString(R.string.KEY_PANIC_PROGRESS_3));
            } else {
                Log.d(ITCConstants.Log.ITC, "SOMETHING WAS WRONG WITH WIPE");
            }
        else {
            Log.d(ITCConstants.Log.ITC, "SOMETHING WAS WRONG WITH SHOUT");
        }
    }

    private void showNotification() {
        backToPanic.putExtra("PanicCount", panicCount);
        backToPanic.putExtra("ReturnFrom", ITCConstants.Panic.RETURN);
        backToPanic.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

        Notification n = new Notification(
                R.drawable.panic,
                getString(R.string.KEY_PANIC_TITLE_MAIN),
                System.currentTimeMillis()
                );

        PendingIntent pi = PendingIntent.getActivity(
                this,
                ITCConstants.Results.RETURN_FROM_PANIC,
                backToPanic,
                PendingIntent.FLAG_UPDATE_CURRENT
                );

        n.setLatestEventInfo(
                this,
                getString(R.string.KEY_PANIC_TITLE_MAIN),
                getString(R.string.KEY_PANIC_RETURN),
                pi
                );

        nm.notify(R.string.remote_service_start_id, n);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopRunnables();
        nm.cancel(R.string.remote_service_start_id);
    }
}
