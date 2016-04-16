package info.guardianproject.intheclear;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.IBinder;
import android.preference.PreferenceManager;
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
	public final static int SCHEDULESERVICECALLBACK_ALARMTASK_STOPPED = 3;
	public final static int SCHEDULESERVICECALLBACK_ALARMTASK_STARTED = 4;
	public final static int SCHEDULESERVICECALLBACK_WIPETASK_STARTED = 5;
	public final static int SCHEDULESERVICECALLBACK_WIPETASK_STOPPED = 6;
	public final static int SCHEDULESERVICECALLBACK_SERVICE_STOPPED = 7;

	public final static String STOP_SCHEDULE_SERVICE = "StopService";
	public final static String STOP_SHOUT_TASK = "StopShoutTask";
	public final static String STOP_WIPE_TASK = "StopWipeTask";


	private AlarmTask alarmTask;
	private PIMWiper wipeTask;
	private SharedPreferences prefs;


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
		prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());

	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.i("ScheduleService", "Received startAlarmTask id " + startId);
		if (intent != null && intent.getAction() != null){
			Log.i("ScheduleService", "Received startAlarmTask id " + startId + ": " + intent.getAction());
			if (intent.getAction().equals(STOP_SCHEDULE_SERVICE)) {
				cancelAlarmTask();
				cancelWipeTask();
				stopSelf();
				Log.i(TAG, "Stop Service");
			} else if (intent.getAction().equals(STOP_SHOUT_TASK)){
				cancelAlarmTask();
			} else if (intent.getAction().equals(STOP_WIPE_TASK)){
				cancelWipeTask();
			}
		}


		// We want this service to continue running until it is explicitly stopped, so return sticky.
		return START_STICKY;
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

	public void startAlarmTask(int milliseconds) {
		Log.d(TAG, "startAlarmTask");
		if (alarmTask == null){
			alarmTask = new AlarmTask(this, System.currentTimeMillis());
 		}
		alarmTask.setRepeatingTime(milliseconds);
		alarmTask.run();
		broadcastServiceState(SCHEDULESERVICECALLBACK_ALARMTASK_STARTED);
	}


	/**
	 * 	cancels the alarmTask and broadcasts that to the receiver (atm. in ScheduleServiceClient)
	 */
	public void cancelAlarmTask(){
		if (alarmTask != null){
			alarmTask.cancel();
			Log.i(TAG, "alarmTask was cancelled!");
		} else {
			Log.i(TAG, "alarm Task was null when cancel was called");
		}
		broadcastServiceState(SCHEDULESERVICECALLBACK_ALARMTASK_STOPPED);
	}

	public void startWipeTask() throws InterruptedException {
		//TODO: check what happens if restarted? Inconsistent states?
		wipeTask = new PIMWiper(
				getBaseContext(),
				prefs.getBoolean(ITCConstants.Preference.DEFAULT_WIPE_CONTACTS, false),
				prefs.getBoolean(ITCConstants.Preference.DEFAULT_WIPE_PHOTOS, false),
				prefs.getBoolean(ITCConstants.Preference.DEFAULT_WIPE_CALLLOG, false),
				prefs.getBoolean(ITCConstants.Preference.DEFAULT_WIPE_SMS, false),
				prefs.getBoolean(ITCConstants.Preference.DEFAULT_WIPE_CALENDAR, false),
				prefs.getBoolean(ITCConstants.Preference.DEFAULT_WIPE_FOLDERS, false));
		wipeTask.start();
		broadcastServiceState(SCHEDULESERVICECALLBACK_WIPETASK_STARTED);
	}

	public void cancelWipeTask(){
		if (wipeTask != null){
			wipeTask.stopPIMWiper();
		}
		broadcastServiceState(SCHEDULESERVICECALLBACK_WIPETASK_STOPPED);

	}


	@Override
	public void onDestroy(){
		Log.i(TAG, "ScheduleService - onDestroy");
		broadcastServiceState(SCHEDULESERVICECALLBACK_SERVICE_STOPPED);
		SMSSender.SMSConfirm.getInstance().unregisterReceiverFrom(this);
		super.onDestroy();
	}

	//TODO: handle callbacks e.g. for failed sms
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