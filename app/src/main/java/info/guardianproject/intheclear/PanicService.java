
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
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import info.guardianproject.intheclear.ITCConstants.Preference;
import info.guardianproject.panic.Panic;

import java.io.File;
import java.util.ArrayList;

public class PanicService extends IntentService {
    private static final String TAG = PanicService.class.getName();
    public static final String ACTION_START_ACTIVITY = PanicService.class.getName().concat(".ACTION_START_ACTIVITY");
    public static final String ACTION_CANCEL = PanicService.class.getName().concat(".ACTION_CANCEL");

    public static final int UPDATE_PROGRESS = 0;

    public PanicService() {
        super(TAG);
    }

    private NotificationManager nm;
    private SharedPreferences prefs;
    private ResultReceiver resultReceiver;

    final Handler h = new Handler();
    boolean isPanicing = false;
    boolean cancelCommunicationToPanicActivity = false;
    boolean firstShout = false;

    WipeService wipeService;
    ShoutController shoutController;

    ArrayList<File> selectedFolders;
    String /*userDisplayName,*/ defaultPanicMsg, configuredFriends;

    @Override
    public void onCreate() {
        super.onCreate();
        nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
    //    if (!TextUtils.isEmpty(PhoneInfo.getIMEI()))
            shoutController = new ShoutController(getBaseContext());
        alignPreferences();
        Log.d(TAG, "at the end of onCreate");
    }

    private void alignPreferences() {
        prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());

        selectedFolders = new ArrayList<File>();
//        userDisplayName = prefs.getString(ITCConstants.Preference.USER_DISPLAY_NAME, ""); //nicht genutzt bisher
        configuredFriends = prefs.getString(ITCConstants.Preference.CONFIGURED_FRIENDS, "");
        defaultPanicMsg = prefs.getString(ITCConstants.Preference.DEFAULT_PANIC_MSG, "");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent.getAction() != null && intent.getAction().equals(PanicService.ACTION_CANCEL)){
            cancelCommunicationToPanicActivity = true;
        }

        Log.d(TAG, "onStartCommand called.  cancelCommunicationToPanicActivity: " + cancelCommunicationToPanicActivity);
        return super.onStartCommand(intent, flags, startId);
    }

    private int shout() {
        Log.d(TAG, "shout called");
        if (shoutController == null){
            Log.d(TAG, "but shoutController is null!");
            return ITCConstants.Results.NOT_AVAILABLE;
        }
        int result = ITCConstants.Results.FAIL;
        updatePanicUi(getString(R.string.KEY_PANIC_PROGRESS_1));
        showNotification(getString(R.string.KEY_PANIC_PROGRESS_1));
        updatePanicProgress(ITCConstants.PanicState.IN_SHOUT);
        h.post(new Runnable() {

            @Override
            public void run() {
                Log.d(TAG, "runMethod in Shout");
                if (isPanicing) {
                    Log.d(TAG, "if-condition in runMethod in Shout");
                    // TODO: this should actually be confirmed.
                    shoutController.sendSMSShout(
                            configuredFriends,
                            defaultPanicMsg,
                            ShoutController.buildShoutData(getResources())
                            );
                    Log.d(ITCConstants.Log.ITC, "this is a shout going out...");
                } else {
                    Log.d(TAG, "it isn't panicing... ");
                        }
            }

        });
        result = ITCConstants.Results.A_OK;
        return result;
    }


    public void updatePanicProgress(int panicStateProgress) {
        if (resultReceiver != null && !cancelCommunicationToPanicActivity) {
            final Bundle bundle = new Bundle();
            bundle.putInt(ITCConstants.UPDATE_PANICSTATE, panicStateProgress);
            resultReceiver.send(UPDATE_PROGRESS, bundle);
            Log.d(TAG, "panic progress message sent to resultReceiver: " + panicStateProgress);
        } else {
            Log.d(TAG, "resultReceiver is null OR cancelCommunicationToPanicActivity is true!");
        }
    }

    public void updatePanicUi(String message) {
        if (resultReceiver != null && !cancelCommunicationToPanicActivity) {
            final Bundle bundle = new Bundle();
            bundle.putString(ITCConstants.UPDATE_UI, message);
            resultReceiver.send(UPDATE_PROGRESS, bundle);
            Log.d(TAG, "message sent to resultReceiver: " + message);
        } else {
            Log.d(TAG, "resultReceiver is null!");
        }
    }

    private int wipe() {
        int result = ITCConstants.Results.FAIL;
        updatePanicUi(getString(R.string.KEY_PANIC_PROGRESS_2));
        if (firstShout){
            showNotification(getString(R.string.KEY_PANIC_PROGRESS_2));
        }
        updatePanicProgress(ITCConstants.PanicState.IN_WIPE);
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
        isPanicing = false;
//        shoutController.tearDownSMSReceiver();
        Log.d(TAG, "try to updatePanicProgress to ITCConstants.PanicState.AT_REST (" + ITCConstants.PanicState.AT_REST +")" );
        updatePanicProgress(ITCConstants.PanicState.AT_REST);
    }


    @Override
    protected void onHandleIntent(Intent intent) {
        Log.i(TAG, "onHandleIntent " + intent);
        String packageName = intent.getPackage();
        Log.i(TAG, "getPackage() " + packageName);
        // TODO use TrustedIntents here to check trust

        if (intent.hasExtra(PanicActivity.CANCELNOTIFICATION) || cancelCommunicationToPanicActivity){
            nm.cancel(R.string.remote_service_start_id);
            //stopRunnables();

            return;
        }
        resultReceiver = intent.getParcelableExtra(PanicActivity.RESULT_RECEIVER);
        if (resultReceiver == null){
            Log.d(TAG, "onHandleIntent resultReceiver == null!!");
        }
        isPanicing = true;
        if (intent.getIntExtra(ITCConstants.UPDATE_PANICSTATE, -1) == ITCConstants.PanicState.IN_FIRST_SHOUT){
            firstShout = true;
        } else {
            firstShout = false;
        }

        showNotification(null);
        int shoutResult = shout();
        if (shoutResult == ITCConstants.Results.A_OK
                || shoutResult == ITCConstants.Results.NOT_AVAILABLE)
            if (wipe() == ITCConstants.Results.A_OK) {
                updatePanicUi(getString(R.string.KEY_PANIC_PROGRESS_3));
                //Run the ticker only at the first time
                if (firstShout){
                    showNotification(getString(R.string.KEY_PANIC_PROGRESS_3));
                }
                updatePanicProgress(ITCConstants.PanicState.IN_CONTINUED_PANIC);
            } else {
                Log.d(ITCConstants.Log.ITC, "SOMETHING WAS WRONG WITH WIPE");
               // Todo: PopUp-Benachrichtigung falls nicht klappt! & frage ob isPanicing = false soll?
                updatePanicProgress(ITCConstants.PanicState.FAILED);
            }
        else {
            Log.d(ITCConstants.Log.ITC, "SOMETHING WAS WRONG WITH SHOUT");
            // Todo: PopUp-Benachrichtigung falls nicht klappt! & frage ob isPanicing = false soll?
            updatePanicProgress(ITCConstants.PanicState.FAILED);
         }
    }

    private void showNotification(String notificationInfo) {
        Log.d(TAG, "showNotification called!");
        Intent backToPanic = newBackToPanicIntent(true);
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this);


       PendingIntent pi = PendingIntent.getActivity(
                this,
                ITCConstants.Results.RETURN_FROM_PANIC,
                backToPanic,
                PendingIntent.FLAG_UPDATE_CURRENT
        );

//Todo: möglich noch die Zeit zu setzen? aber vielleicht überflüssig

        notificationBuilder.
                setContentIntent(pi).
                setSmallIcon(R.drawable.panic).
                setAutoCancel(true). //muss auf true, sonst verschwindet Notification nicht!
                setContentText(this.getResources().getString(R.string.KEY_PANIC_RETURN)).
               // setContentInfo(notificationInfo + " - contentInfo").
                setContentTitle(this.getResources().getString(R.string.KEY_PANIC_TITLE));
        if (notificationInfo != null && ! notificationInfo.isEmpty()){
            notificationBuilder.setTicker(notificationInfo);
        }

        Notification n = notificationBuilder.build();

        nm.notify(R.string.remote_service_start_id, n);
    }

    @Override
    public void onDestroy() {
        stopRunnables();
        Log.d(TAG, "PanicService onDestroy called");
     //   nm.cancel(R.string.remote_service_start_id); // sonst verschwindet notification zu schnell
        cancelCommunicationToPanicActivity = false;
        super.onDestroy();

    }

    private Intent newBackToPanicIntent(boolean fromNotification){
        Log.d(TAG, "BacktoPanicIntent");
        Intent i = new Intent(this, PanicActivity.class);
        if (fromNotification){
            Log.d(TAG, "fromNotification, put extra ReturnFrom!");
            i.putExtra(PanicActivity.RETURNFROMNOTIFICATION, ITCConstants.Panic.RETURN); //Konstante draus machen
            // i.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

        }

        return i;
    }
}
