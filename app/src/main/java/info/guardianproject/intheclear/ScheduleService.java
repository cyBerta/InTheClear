package info.guardianproject.intheclear;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import info.guardianproject.utils.Logger;

/**
 * TODO: check if this service could be shut down after sending the alarm manager the messsage to repeat the notifications
 * ANSWER: NO, as the schedule services alarm task holds the pendingIntent which is used as a signature for cancelling alarmManager
 * */
public class ScheduleService extends Service implements SMSSender.SMSConfirmInterface{



	private static final String TAG = ScheduleService.class.getName();
	public final static String SERVICE_STATE = ScheduleService.class.getName().concat(".STATE");
	public final static int SCHEDULESERVICECALLBACK_UNKNOWN = -1;
	public final static int SCHEDULESERVICECALLBACK_ONBIND = 2;
	public final static int SCHEDULESERVICECALLBACK_PANIC_STOPPED = 3;


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
	public void onCreate() {
		super.onCreate();
		SMSSender.SMSConfirm.getInstance().registerReceiverIn(this);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
	Log.i("ScheduleService", "Received startAlarmTask id " + startId + ": " + intent.getAction());

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
		broadcastServiceState(SCHEDULESERVICECALLBACK_PANIC_STOPPED);
	}

	@Override
	public void onDestroy(){
		Log.i(TAG, "ScheduleService - onDestroy");
		SMSSender.SMSConfirm.getInstance().unregisterReceiverFrom(this);
		super.onDestroy();
	}

	@Override
	public void onSMSSent(Intent intent) {

		info.guardianproject.utils.Logger.logD(TAG, "onSMSSent: " + Logger.intentToString(intent));
	}

	private void broadcastServiceState(int state){
		Log.d(TAG, "ScheduleService broadcastServiceState + " + state);
		Intent broadcastIntent = new Intent();
		broadcastIntent.setAction(ScheduleService.class.getName());
		broadcastIntent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
		broadcastIntent.putExtra(SERVICE_STATE, state);
		Log.d(TAG, "Send Broadcast, ServiceState: " + state + " - Service: " + ScheduleService.class.getName());
		sendBroadcast(broadcastIntent);
	}

}