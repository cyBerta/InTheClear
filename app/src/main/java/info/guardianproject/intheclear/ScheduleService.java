package info.guardianproject.intheclear;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import java.util.Calendar;

/**
 * TODO: check if this service could be shut down after sending the alarm manager the messsage to repeat the notifications
 * ANSWER: NO, as the schedule services alarm task holds the pendingIntent which is used as a signature for cancelling alarmManager
 * */
public class ScheduleService extends Service {



	private static final String TAG = ScheduleService.class.getName();
	public final static String SERVICE_STATE = ScheduleService.class.getName().concat(".STATE");
	public final static int SCHEDULESERVICECALLBACK_START = 0;
	public final static int SCHEDULESERVICECALLBACK_STOP = 1;
	public final static int SCHEDULESERVICECALLBACK_UNKNOWN = -1;
	public final static int SCHEDULESERVICECALLBACK_ONBIND = 2;


	private AlarmTask alarmTask;
	/**
	 * Class for clients to access
	 */
	public class ServiceBinder extends Binder {
		ScheduleService getService() {
			return ScheduleService.this;
		}
	}


	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.i("ScheduleService", "Received startAlarmTask id " + startId + ": " + intent);

		if (intent.getAction() != null && intent.getAction().equals("StopService")) {
				cancelAlarmTask();
				Log.i(TAG, "Stop Service");
		}


		return START_STICKY;


		// We want this service to continue running until it is explicitly stopped, so return sticky.
	}

	@Override
	public IBinder onBind(Intent intent) {

		Log.d(TAG, "ScheduleService onBind");
		return mBinder;
	}

	@Override
	public boolean onUnbind(Intent intent) {
		Log.i(TAG, "unbinding Service from Activity!");
		return super.onUnbind(intent);
	}

	// This is the object that receives interactions from clients. See
	private final IBinder mBinder = new ServiceBinder();
/*
	/**
	 * Show an alarm for a certain date when the alarm is called it will pop up a notification
	 */
/*	public void setAlarm(Calendar c) {
		// This starts a new thread to set the alarm
		// You want to push off your tasks onto a new thread to free up the UI to carry on responding
		if (alarmTask == null){
			alarmTask = new AlarmTask(this, c);
			Log.i(TAG, "alarmTask setAlarm - alarmTask != null now");
		}
	}
*/

	public void startAlarmTask(int milliseconds) {
		Log.d(TAG, "startAlarmTask");
		if (alarmTask == null){
			alarmTask = new AlarmTask(this, System.currentTimeMillis());
 		}
		alarmTask.setRepeatingTime(milliseconds);
		alarmTask.run();
	}


	public void cancelAlarmTask(){
		if (alarmTask != null){
			alarmTask.cancel();
			Log.i(TAG, "alarmTask was cancelled!");
		} else {
			Log.i(TAG, "alarm Task was null when cancel was called");
		}
	}

	@Override
	public void onDestroy(){
		Log.i(TAG, "ScheduleService - onDestroy");
		super.onDestroy();
	}
}