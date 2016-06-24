package info.guardianproject.intheclear;

import android.content.*;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import info.guardianproject.utils.Logger;

/**
 * This class connects any activity with the schedule service.
 * 
 * @author cyBerta
 * @author paul.blundell
 */
public class ScheduleServiceClient {

	private static final String TAG = ScheduleServiceClient.class.getName();

	public interface IScheduleClientCallback{
		public void onCallbackReceived(String service, int callbackState, Bundle extraData);
	}

	// The hook into our service
	private ScheduleService mBoundService;
	// The context to start the service in
	private Context mContext;
	// A flag if we are connected to the service or not
	private boolean mIsBound;
	private ServiceCallbackReceiver serviceCallbackReceiver;



	public ScheduleServiceClient(Context context) {
		mContext = context;
			}
	
	/**
	 * Call this to connect your activity to your service
	 */
	public void doBindService() {
		Logger.logD(TAG, "doBindService");
		// Establish a connection with our service
		mContext.bindService(new Intent(mContext, ScheduleService.class), mConnection, Context.BIND_AUTO_CREATE);
		if (mContext instanceof  IScheduleClientCallback){
			Logger.logD(TAG, "mContext implements a callbackInterface!");
			serviceCallbackReceiver = new ServiceCallbackReceiver((IScheduleClientCallback)mContext);
			IntentFilter intentFilter = new IntentFilter();
			intentFilter.addAction(ShoutService.class.getName());
			intentFilter.addAction(ScheduleService.class.getName());
			mContext.registerReceiver(serviceCallbackReceiver, intentFilter);
		} else {
			Logger.logD(TAG, "mContext is not instanceof IScheduleClient");
		}

		mIsBound = true;
	}

	public boolean isServiceBound(){
		return mIsBound;
	}

	public void startService() {
		mContext.startService(new Intent(mContext, ScheduleService.class));
	}

	/**
	 * When you attempt to connect to the service, this connection will be called with the result.
	 * If we have successfully connected we instantiate our service object so that we can call methods on it.
	 */
	private ServiceConnection mConnection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName className, IBinder service) {
			// This is called when the connection with our service has been established, 
			// giving us the service object we can use to interact with our service.
			mBoundService = ((ScheduleService.ServiceBinder) service).getService();
			Intent callbackIntent = new Intent();
			callbackIntent.setAction(ScheduleService.class.getName());
			callbackIntent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
			callbackIntent.putExtra(ScheduleService.SERVICE_STATE, ScheduleService.SCHEDULESERVICECALLBACK_ONBIND);
			ScheduleServiceClient.this.serviceCallbackReceiver.onReceive(ScheduleServiceClient.this.mContext, callbackIntent);
		}

		public void onServiceDisconnected(ComponentName className) {
			mBoundService = null;
		}
	};

	/*starts the sms job*/
	public void startAlarm(){
		mBoundService.startAlarmTask(60000);
	}

	/*stops the sms job*/
	public void stopAlarm(){
		mBoundService.cancelAlarmTask();
	}

	public void startWiping() throws InterruptedException{
		mBoundService.startWipeTask();
	}

	public void stopWiping(){
		mBoundService.cancelWipeTask();
	}

	public void startPanic()throws InterruptedException{
		startAlarm();
		startWiping();
	}

	public void stopPanic(){
		stopAlarm();
		stopWiping();
	}

	public boolean isSMSPanicRunning(){
		boolean isRunning = false;
		if (mBoundService != null){
			isRunning = mBoundService.isAlarmTaskRunning();
		}
		return isRunning;
	}

	public long getLastShoutTime(){
		long lastShout = 0;
		if (mBoundService != null){
			lastShout = mBoundService.getAlarmTaskStartTime();
			Log.d(TAG, "Last Shout ServiceClient: " + lastShout);
		}
		return lastShout;
	}

	public boolean isWipePanicRunning(){
		boolean isRunning = false;
		if (mBoundService != null){
			isRunning = mBoundService.isWipeTaskRunning();
		}
		return isRunning;
	}

	public Bundle getCurrentWipeState(){
		Bundle bundle = null;
		if (mBoundService != null){
			bundle = mBoundService.getCurrentWipeState();
		}
		return bundle;
	}

	/**
	 * When you have finished with the service call this method to stop it 
	 * releasing your connection and resources
	 */
	public void doUnbindService() {
		Logger.logD(TAG, "unbind service");
		if (mIsBound) {
			Logger.logD(TAG, "was bound...");
			// Detach our existing connection.
			mContext.unbindService(mConnection);
			mContext.unregisterReceiver(serviceCallbackReceiver);
			serviceCallbackReceiver = null;
			mIsBound = false;
		}
	}

	/**
	 * This Broadcast receiver handles services callbacks and passes
	 * the result to the Activity that implements the callbackInterface
	 */

	private class ServiceCallbackReceiver extends BroadcastReceiver {

		private IScheduleClientCallback callbackImplementation;
		private ServiceCallbackReceiver(){}

		public ServiceCallbackReceiver(IScheduleClientCallback callbackImplementation){
			super();
			this.callbackImplementation = callbackImplementation;
		}

		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getAction() != null){
				Logger.logD(TAG, "onReceive: " + Logger.intentToString(intent));
				if (intent.getAction().equals(ScheduleService.class.getName())){
					Logger.logD(TAG, "ScheduleService callback received : " + Logger.intentToString(intent));
					int serviceState = intent.getIntExtra(ScheduleService.SERVICE_STATE, ScheduleService.SCHEDULESERVICECALLBACK_UNKNOWN);
					callbackImplementation.onCallbackReceived(ScheduleService.SERVICE_STATE, serviceState, intent.getBundleExtra(ScheduleService.SERVICE_STATE_EXTRA));
				} else if (intent.getAction().equals(ShoutService.class.getName())){
					Logger.logD(TAG, "ShoutService callback received : " + Logger.intentToString(intent));
					int serviceState = intent.getIntExtra(ShoutService.SERVICE_STATE, ShoutService.SHOUTSERVICECALLBACK_UNKNOWN);
					callbackImplementation.onCallbackReceived(ShoutService.SERVICE_STATE, serviceState, null);
				}
			}

		}
	}
}
