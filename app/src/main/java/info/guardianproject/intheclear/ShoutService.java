package info.guardianproject.intheclear;

import android.app.*;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import info.guardianproject.utils.Logger;

/**
 * Created by richy on 18.03.16.
 */
public class ShoutService extends IntentService {
    public final static String SERVICE_STATE = ShoutService.class.getName().concat(".STATE");
    public final static int SHOUTSERVICECALLBACK_START = 0;
    public final static int SHOUTSERVICECALLBACK_STOP = 1;
    public final static int SHOUTSERVICECALLBACK_UNKNOWN = -1;

    // Unique id to identify the notification.
    private static final int NOTIFICATION = ShoutService.class.getName().hashCode();
    // Name of an intent extra we can use to identify if this service was started to create a notification
    public static final String INTENT_NOTIFY = ShoutService.class.getName().concat(".SHOUT");
    private static final String TAG = ShoutService.class.getName();
    // The system notification manager
    private NotificationManager mNM;
    ShoutController shoutController;
    private SharedPreferences prefs;
    String defaultPanicMsg, configuredFriends;

    final Handler h = new Handler();



    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     *
     * @param name Used to name the worker thread, important only for debugging.
     */
    public ShoutService(String name) {
        super(name);
    }
    public ShoutService(){
        super(ShoutService.class.getName());
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mNM = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        shoutController = new ShoutController(getBaseContext());
        alignPreferences();

    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Logger.logD(TAG, "ShoutService received : " + Logger.intentToString(intent));

        // If this service was started by out AlarmTask intent then we want to show our notification
        if(intent.getBooleanExtra(INTENT_NOTIFY, false)){
            showNotification();
        }
        shout();
        broadcastServiceState(SHOUTSERVICECALLBACK_START);
    }

    private void broadcastServiceState(int state){
        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction(ShoutService.class.getName());
        broadcastIntent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
        broadcastIntent.putExtra(SERVICE_STATE, state);
        Log.d(TAG, "Send Broadcast, ServiceState: " + state + " - Service: " + ShoutService.class.getName());
        sendBroadcast(broadcastIntent);
    }

    private void alignPreferences() {
        prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());

        configuredFriends = prefs.getString(ITCConstants.Preference.CONFIGURED_FRIENDS, "");
        defaultPanicMsg = prefs.getString(ITCConstants.Preference.DEFAULT_PANIC_MSG, "");
    }

    private void shout() {
        Log.d(TAG, "shout called");
        if (shoutController != null) {
            h.post(new Runnable() {

                @Override
                public void run() {
                    Log.d(TAG, "runMethod in Shout");
                    shoutController.sendSMSShout(
                            configuredFriends,
                            defaultPanicMsg,
                            ShoutController.buildShoutData(getResources())
                    );
                    Log.d(ITCConstants.Log.ITC, "this is a shout going out...");

                }

            });
        }
    }

    private void showNotification() {
        Log.d(TAG, "sendNotification");
        // This is the 'title' of the notification
        CharSequence title = getString(R.string.KEY_PANIC_TITLE_MAIN);
        // This is the icon to use on the notification
        int icon = R.drawable.panic;
        // This is the scrolling text of the notification
        CharSequence text = getString(R.string.KEY_PANIC_PROGRESS_1);
        // What time to show on the notification
        long time = System.currentTimeMillis();

        Intent intent = new Intent(this, ScheduleService.class);
        intent.setAction(ScheduleService.STOP_SCHEDULE_SERVICE);

        // The PendingIntent to launch our activity if the user selects this notification
        PendingIntent contentIntent = PendingIntent.getService(this, 0, intent, 0);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this);
        notificationBuilder.
                setContentIntent(contentIntent).
                setSmallIcon(icon).
                setContentTitle(title).
                setAutoCancel(true). //muss auf true, sonst verschwindet Notification nicht!
                setContentText(this.getResources().getString(R.string.KEY_PANIC_RETURN));
        notificationBuilder.setTicker(text);


        Notification notification = notificationBuilder.build();
        // Clear the notification when it is pressed
        notification.flags |= Notification.FLAG_AUTO_CANCEL;

        // Send the notification to the system.
        mNM.notify(NOTIFICATION, notification);

    }

    public static int getNotificationId(){
        return NOTIFICATION;
    }



    @Override
    public void onDestroy() {
        broadcastServiceState(SHOUTSERVICECALLBACK_STOP);
        super.onDestroy();
    }
}
