package info.guardianproject.intheclear;

import android.R;
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.util.Log;

/**
 * This service is started when an Alarm has been raised
 * 
 * We pop a notification into the status bar for the user to click on
 * When the user clicks the notification a new activity is opened
 * 
 * @author paul.blundell
 */
public class NotifyService extends IntentService {
	public final static String SERVICE_STATE = NotifyService.class.getName().concat(".STATE");
	public final static int NOTIFYSERVICECALLBACK_START = 0;
	public final static int NOTIFYSERVICECALLBACK_STOP = 1;
	public final static int NOTIFYSERVICECALLBACK_UNKNOWN = -1;


	private static final String TAG = NotifyService.class.getName();

	/**
	 * Creates an IntentService.  Invoked by your subclass's constructor.
	 *
	 * @param name Used to name the worker thread, important only for debugging.
	 */
	public NotifyService(String name) {
		super(name);
	}

	public NotifyService(){
		super(NotifyService.class.getName());
	}

	// Unique id to identify the notification.
	private static final int NOTIFICATION = NotifyService.class.getName().hashCode();
	// Name of an intent extra we can use to identify if this service was started to create a notification	
	public static final String INTENT_NOTIFY_FIRST = "com.blundell.tut.service.INTENT_NOTIFY_FIRST";
    public static final String INTENT_NOTIFY = "com.blundell.tut.service.INTENT_NOTIFY";
	public static final String LOG_REPEATING = "com.blundell.tut.service.LOG_REPEATING";
	// The system notification manager
	private NotificationManager mNM;

	@Override
	public void onCreate() {
		super.onCreate();
		Log.i("NotifyService", "onCreate()");
		mNM = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
	}

	@Override
	public void onStart(Intent intent, int startId) {
		Log.d(TAG, "onStartIntent");
		super.onStart(intent, startId);
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		Log.i("LocalService", "Received startAlarmTask : " + intent);

		// If this service was started by out AlarmTask intent then we want to show our notification
		if(intent.getBooleanExtra(INTENT_NOTIFY, false))
			showNotification();

		if (intent.getBooleanExtra(LOG_REPEATING, false)){
			Log.i(TAG, "repeating");
		}

		Intent broadcastIntent = new Intent();
		broadcastIntent.setAction(NotifyService.class.getName());
		broadcastIntent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
		broadcastIntent.putExtra(NotifyService.SERVICE_STATE, NOTIFYSERVICECALLBACK_START);
		Log.d(TAG, "Send Broadcast");
		sendBroadcast(broadcastIntent);
	}




	/**
	 * Creates a notification and shows it in the OS drag-down status bar
	 */
	private void showNotification() {
		// This is the 'title' of the notification
		CharSequence title = "Alarm!!";
		// This is the icon to use on the notification
		int icon = R.drawable.ic_dialog_alert;
		// This is the scrolling text of the notification
		CharSequence text = "Your notification time is upon us.";		
		// What time to show on the notification
		long time = System.currentTimeMillis();
		
		Notification notification = new Notification(icon, text, time);

        Intent intent = new Intent(this, ScheduleService.class);
        intent.setAction(ScheduleService.STOP_SCHEDULE_SERVICE);

		// The PendingIntent to launch our activity if the user selects this notification
		PendingIntent contentIntent = PendingIntent.getService(this, 0, intent, 0);

		// Set the info for the views that show in the notification panel.
		notification.setLatestEventInfo(this, title, text, contentIntent);

		// Clear the notification when it is pressed
		notification.flags |= Notification.FLAG_AUTO_CANCEL;
		
		// Send the notification to the system.
		mNM.notify(NOTIFICATION, notification);
		
		// Stop the service when we are finished
		stopSelf();
	}

	public static int getNotificationId(){
		return NOTIFICATION;
	}
}