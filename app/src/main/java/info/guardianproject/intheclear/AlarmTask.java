package info.guardianproject.intheclear;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.util.Calendar;

/**
 * Set an alarm for the date passed into the constructor
 * When the alarm is raised it will start the ShoutService
 * 
 * This uses the android build in alarm manager *NOTE* if the phone is turned off this alarm will be cancelled
 * 
 * This will run on it's own thread.
 * 
 * @author paul.blundell
 * @author cyBerta
 *
 */
public class AlarmTask implements Runnable{
	private static final String TAG = AlarmTask.class.getName();
	// The android system alarm manager
	private final AlarmManager am;
	// Your context to retrieve the alarm manager from
	private final Context context;

	private PendingIntent pendingIntent;

	private long repeating = -1;

	private NotificationManager mNM;
	long timeInMillies;


	public AlarmTask(Context context, long timeInMillies) {
		this.timeInMillies = timeInMillies;
		this.context = context;
		this.am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		mNM = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
	}

	/**
	 * sets the time period the alarm gets restarted
	 */
	public void setRepeatingTime(long repeatingTime){
		this.repeating = repeatingTime;
	}

	/**
	 * cancels the scheduled alarm and removes the notification from the system bar
	 */
	public void cancel(){
		am.cancel(pendingIntent);
		mNM.cancel(ShoutService.getNotificationId());
		Log.i(TAG, "AlarmTask cancelled");
	}
	
	@Override
	public void run() {
		// Request to start a service when the alarm date is upon us
		// We don't start an activity as we just want to pop up a notification into the system bar not a full activity
		if (pendingIntent != null){
			am.cancel(pendingIntent);
		}
		pendingIntent = buildPendingIntent();
		// Sets an alarm - note this alarm will be lost if the phone is turned off and on again
		if (repeating != -1){
			am.setRepeating(AlarmManager.RTC_WAKEUP, timeInMillies, repeating, pendingIntent);
		} else {
			am.set(AlarmManager.RTC_WAKEUP, timeInMillies, pendingIntent);
		}
	}

	/**
	 * creates a PendingIntent
	 * @return  PendingIntent that starts the ShoutService and initiates a notification
     */
	private PendingIntent buildPendingIntent(){
		Intent intent = new Intent(context, ShoutService.class);
		intent.putExtra(ShoutService.INTENT_NOTIFY, true);
		return PendingIntent.getService(context, 0, intent, 0);
	}
}
