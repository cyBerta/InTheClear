package info.guardianproject.intheclear;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.provider.Telephony;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.IntentCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import info.guardianproject.panic.PanicUtils;
import info.guardianproject.utils.EndActivity;
import info.guardianproject.utils.Logger;
import info.guardianproject.views.PanicItem;

import java.util.ArrayList;

/**
 * Created by richy on 18.03.16.
 */
public class PanicActivity extends Activity implements View.OnClickListener, ScheduleServiceClient.IScheduleClientCallback  {
    private static final String TAG = PanicActivity.class.getName();
    SharedPreferences sp;
    boolean oneTouchPanic;
    //ListView listView;
    TextView shoutReadout, panicProgress, countdownReadout;
    Button controlPanic, cancelCountdown, panicControl;
    CustomCountdownTimer countDownTimer;
    private ScheduleServiceClient scheduleClient;
    ProgressDialog panicStatusDialog;
    String currentPanicStatus;
    private boolean stealthMode;
    int panicState;

    /**
     * Mehtods ot the Activity Life Cycle
     */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.panic);

        // Create a new service client and bind our activity to this service
        scheduleClient = new ScheduleServiceClient(this);
        //scheduleClient.startService();
        //scheduleClient.doBindService();

        sp = PreferenceManager.getDefaultSharedPreferences(getBaseContext());

        panicControl = (Button) findViewById(R.id.panicControl);
        shoutReadout = (TextView) findViewById(R.id.shoutReadout);
//        listView = (ListView) findViewById(R.id.wipeItems);

        // if this is not a cell phone, then no need to show the panic message
        if (TextUtils.isEmpty(PhoneInfo.getIMEI())) {
            shoutReadout.setVisibility(View.GONE);
            TextView shoutReadoutTitle = (TextView) findViewById(R.id.shoutReadoutTitle);
            shoutReadoutTitle.setVisibility(View.GONE);
        } else {
            String panicMsg = sp.getString(ITCConstants.Preference.DEFAULT_PANIC_MSG, "");
            shoutReadout.setText( panicMsg
                    +"\n\n"+ ShoutController.buildShoutData(getResources()));
        }

        panicStatusDialog = new ProgressDialog(this);
        panicStatusDialog.setButton(
                getResources().getString(R.string.KEY_PANIC_MENU_CANCEL),
                new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (scheduleClient != null){
                            scheduleClient.stopPanic();
                            stopCountDownTimer();
                        }
                    }
                }
        );
        panicStatusDialog.setTitle(getResources().getString(R.string.KEY_PANIC_BTN_PANIC));
        panicStatusDialog.setCanceledOnTouchOutside(false);

    }

   @Override
    public void onBackPressed() {
       if (panicStatusDialog.isShowing()) {
           if (countDownTimer != null) {
               countDownTimer.cancel();
           }
           panicStatusDialog.dismiss();
       }
       super.onBackPressed();

   }

    @Override
    protected void onStart() {
        super.onStart();
        alignPreferences();
        panicControl.setOnClickListener(this);
        scheduleClient.startService();
        scheduleClient.doBindService();
        boolean isDefaultApp = PanicUtils.isDefaultSMSApp(this.getApplicationContext());
        if (isDeleteSMS() && !isDefaultApp){
            askForDefaultSMSPermission();
        }
        // listView.setAdapter(new WipeItemAdapter(this, wipeTasks));
        // listView.setChoiceMode(ListView.CHOICE_MODE_NONE);
        // listView.setClickable(false);
    }

    @Override
    public void onResume() {
        super.onResume();
        final ArrayList<WipeItem> wipeTasks = new ArrayList<WipeItem>(6);
        wipeTasks.add(0,
                new WipeItem(R.string.KEY_WIPE_WIPECONTACTS, sp, ITCConstants.Preference.DEFAULT_WIPE_CONTACTS));
        wipeTasks.add(1,
                new WipeItem(R.string.KEY_WIPE_WIPEPHOTOS, sp, ITCConstants.Preference.DEFAULT_WIPE_PHOTOS));
        wipeTasks.add(2,
                new WipeItem(R.string.KEY_WIPE_CALLLOG, sp, ITCConstants.Preference.DEFAULT_WIPE_CALLLOG));
        wipeTasks.add(3,
                new WipeItem(R.string.KEY_WIPE_SMS, sp, ITCConstants.Preference.DEFAULT_WIPE_SMS));
        wipeTasks.add(4,
                new WipeItem(R.string.KEY_WIPE_CALENDAR, sp, ITCConstants.Preference.DEFAULT_WIPE_CALENDAR));
        wipeTasks.add(5,
                new WipeItem(R.string.KEY_WIPE_SDCARD, sp, ITCConstants.Preference.DEFAULT_WIPE_FOLDERS));

        LinearLayout wipeItems = (LinearLayout) findViewById(R.id.wipeItems);
        wipeItems.removeAllViews();
        for (WipeItem item : wipeTasks){
            PanicItem panicItem = new PanicItem(this);
            panicItem.setItemText(getString(item.getResId()));
            Log.d(TAG, getString(item.getResId()));
            panicItem.setItemSelected(item.getSelected());
            wipeItems.addView(panicItem);
        }

        stealthMode = sp.getBoolean(ITCConstants.Preference.DEFAULT_HIDE_PANIC_ACTION, true);


    }



    @Override
    protected void onStop() {
        panicStatusDialog.dismiss();
        if(scheduleClient != null)
            scheduleClient.doUnbindService();
        super.onStop();
    }


    @Override
    protected void onDestroy() {
        // When our activity is stopped ensure we also stop the connection to the service
        // this stops us leaking our activity into the system *bad*

        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        //Log.d(TAG, "onActivity Result from Telephony");
        Logger.logD(TAG, "onActivityResult: " + Logger.intentToString(data) );

        switch (requestCode) {
            case ITCConstants.Results.RETURN_FROM_DEFAULT_SMS_REQUEST:
                if (Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT){

                    if (isDeleteSMS() && !PanicUtils.isDefaultSMSApp(this.getApplicationContext())){
                        if (scheduleClient.isSMSPanicRunning()) {
                            showPanicStatus("Cannot delete SMS until InTheClear is set to default SMS app!");
                        } else {
                            AlertDialog.Builder d = new AlertDialog.Builder(this);
                            d.setMessage("Cannot delete SMS until InTheClear is set to default SMS app! Proceed anyway?")
                                    .setCancelable(true)
                                    .setPositiveButton(getResources().getString(R.string.KEY_OK),
                                            new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {
                                                    startCountdownTimer(true);
                                                }
                                            })
                                    .setNegativeButton(getResources().getString(R.string.KEY_PANIC_MENU_CANCEL), null)
                            .  setNeutralButton("Choose default", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    askForDefaultSMSPermission();
                                }
                            });
                            AlertDialog a = d.create();
                            a.show();
                        }
                    } else {
                        //
                        /*Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && resultCode == Activity.RESULT_OK && PanicUtils.isDefaultSMSApp(this.getApplicationContext())) {*/
                        //boolean firstShout = data.getBooleanExtra("FIST_SHOUT", true);
                        if (scheduleClient.isSMSPanicRunning()) {
                            if (!continueCountdown()) {
                                showPanicStatus("first sms will be sent within the next minute!");
                            }
                        } else {
                            startCountdownTimer(true);
                        }
                    }
                }
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);


    }

    private boolean continueCountdown(){
        boolean isContinuingCountdown = false;
        long lastShout = scheduleClient.getLastShoutTime();
        if (lastShout != 0){
            long delta = 58000 - (System.currentTimeMillis() - lastShout);
            Log.d(TAG, "Last Shout  " + delta/1000);
            countDownTimer = new CustomCountdownTimer(delta, ITCConstants.Duration.COUNTDOWNINTERVAL);
            panicStatusDialog.show();
            countDownTimer.startCountdownTimer(false);
            isContinuingCountdown = true;
        }
        return isContinuingCountdown;
    }

    /**
     * Callback method of the ScheduleServiceClient
     **/

    @Override
    public void onCallbackReceived(String serviceState, int callback) {
        Log.d(TAG, "onCallbackReceived ServiceState: " + serviceState + " -> " + callback );

        if (serviceState.equals(ScheduleService.SERVICE_STATE)){
            switch (callback){
                case ScheduleService.SCHEDULESERVICECALLBACK_ONBIND:
                    if (scheduleClient.isSMSPanicRunning() || scheduleClient.isWipePanicRunning()){
                        if (!continueCountdown()){
                            showPanicStatus("first sms will be sent within the next minute!");
                        }
                    } else {
                        if (oneTouchPanic){
                            doPanic(true);
                        }
                    }
                    break;
                case ScheduleService.SCHEDULESERVICECALLBACK_ALARMTASK_STOPPED:
                    Log.d(TAG, "ScheduleService requests to stop panic");
                    stopCountDownTimer();
                    panicStatusDialog.cancel();
                    break;
                case ScheduleService.SCHEDULESERVICECALLBACK_ALARMTASK_STARTED:
                    showPanicStatus("first sms will be sent within the next minute!");
                    panicStatusDialog.setCancelable(true);
                    break;
                case ScheduleService.SCHEDULESERVICECALLBACK_WIPETASK_STARTED:
                    Log.d(TAG, "WipeTask started!");
                    break;
                case ScheduleService.SCHEDULESERVICECALLBACK_SERVICE_STOPPED:
                    Log.d(TAG, "Scheduleservise stopped");
                    break;
                default:
                    break;

            }
        } else if (serviceState.equals(ShoutService.SERVICE_STATE)){
            switch (callback){
               default:
                case ShoutService.SHOUTSERVICECALLBACK_START:
                    Log.d(TAG, "sendingSMS");
                    showPanicStatus("sending SMS...");

                    break;
                case ShoutService.SHOUTSERVICECALLBACK_STOP:
                    Log.d(TAG, "start countdown.");
                    repeatSMSPanicCountdown();
                    break;

            }
        }

    }

    /**
     * implementing Activities OnClickListener
     */
    @Override
    public void onClick(View v) {
        if (v == panicControl && scheduleClient.isServiceBound()) {
            if (!scheduleClient.isSMSPanicRunning() && !scheduleClient.isWipePanicRunning()){
                doPanic(true);
            }
        }
    }

    /*private methods*/

    private void stopCountDownTimer(){
        Log.d(TAG, "stopCountdownTimer!!");
        if (countDownTimer != null /*|| countDownTimer.isRunning()*/){
            countDownTimer.cancel();
        }
    }

    private void startCountdownTimer(final boolean firstShout) {
        int durationTmp = 58000;
        if (firstShout) {
            durationTmp = 10000;
        }

        final long countdownduration = durationTmp;
        panicState = ITCConstants.PanicState.IN_COUNTDOWN;
        countDownTimer = new CustomCountdownTimer(countdownduration, ITCConstants.Duration.COUNTDOWNINTERVAL);
        countDownTimer.startCountdownTimer(firstShout);
    }

    private void showPanicStatus(String message){
        panicStatusDialog.setMessage(message);
        panicStatusDialog.show();
    }

    public void repeatSMSPanicCountdown(){
        doPanic(false);
    }
    public void doPanic(boolean firstShout){
        Log.d(TAG, "doPanic - fistShout: " + firstShout);
        boolean isDefaultApp = PanicUtils.isDefaultSMSApp(this.getApplicationContext());
        if (isDeleteSMS() && !isDefaultApp){
            askForDefaultSMSPermission();
        } else {
            PanicActivity.this.startCountdownTimer(firstShout);
        }
    }

    private boolean isDeleteSMS(){
        return sp.getBoolean(ITCConstants.Preference.DEFAULT_WIPE_SMS, false);
    }
    private void askForDefaultSMSPermission(){
        Intent intent =
                new Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT);

        intent.putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME,
                getPackageName());
        //intent.putExtra("FIRST_SHOUT", firstShout);

        startActivityForResult(intent, ITCConstants.Results.RETURN_FROM_DEFAULT_SMS_REQUEST);
    }

    private void alignPreferences() {
        oneTouchPanic = false;
        String recipients = sp.getString(ITCConstants.Preference.CONFIGURED_FRIENDS, "");
        if (recipients.compareTo("") == 0) {
            AlertDialog.Builder d = new AlertDialog.Builder(this);
            d.setMessage(getResources().getString(R.string.KEY_SHOUT_PREFSFAIL))
                    .setCancelable(false)
                    .setPositiveButton(getResources().getString(R.string.KEY_OK),
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int id) {
                                    PanicActivity.this.launchPreferences();
                                }
                            });
            AlertDialog a = d.create();
            a.show();
        } else {
            oneTouchPanic = sp.getBoolean(ITCConstants.Preference.DEFAULT_ONE_TOUCH_PANIC, false);
        }
    }

    public void launchPreferences() {
        Intent toPrefs = new Intent(this, SettingsActivity.class);
        startActivity(toPrefs);
    }

    public void clearBackstackAndFinish(){
        Intent intent = new Intent(getApplicationContext(), EndActivity.class);
        ComponentName cn = intent.getComponent();
        Intent mainIntent = IntentCompat.makeRestartActivityTask(cn);
        try {
            ActivityCompat.finishAffinity(PanicActivity.this);
        } catch (NullPointerException npe){
            Log.d(TAG, npe.getMessage());
        }
        startActivity(mainIntent);
    }


    protected class CustomCountdownTimer extends CountDownTimer {
        private boolean firstShout = false;
        private boolean isRunning = false;

        /**
         * @param millisInFuture    The number of millis in the future from the call
         *                          to {@link #start()} until the countdown is done and {@link #onFinish()}
         *                          is called.
         * @param countDownInterval The interval along the way to receive
         *                          {@link #onTick(long)} callbacks.
         */
        public CustomCountdownTimer(long millisInFuture, long countDownInterval) {
            super(millisInFuture, countDownInterval);
        }

        public boolean isRunning(){
            return isRunning;
        }

        @Override
        public void onTick(long millisUntilFinished) {
            isRunning = true;
            if (firstShout || panicStatusDialog.isShowing()){
                showPanicStatus(
                        getString(R.string.KEY_PANIC_COUNTDOWNMSG) +
                                " " + ((int) millisUntilFinished/1000-1) + " " +
                                getString(R.string.KEY_SECONDS)
                );
            }
        }

        @Override
        public void onFinish() {
            Log.d(TAG, "onFininsh called");
            if (this.firstShout){
                Log.d(TAG, "firstShout");
                try{
                    scheduleClient.startPanic();
                } catch (InterruptedException e){
                    Log.d(TAG, "Wiptask was interrupted: " + e.getMessage());
                }
                this.firstShout = false;
                if (stealthMode){
                    clearBackstackAndFinish();
                } else {
                    if (panicStatusDialog.isShowing()){
                        showPanicStatus("Starting... ");
                    }
                }

            } else {
                Log.d(TAG, "not firstShout");
                if (stealthMode) {
                    clearBackstackAndFinish();
                } else {
                    if (panicStatusDialog.isShowing()){
                        showPanicStatus("Repeating...");
                    }
                }
            }
            isRunning = false;
        }

        public synchronized final CountDownTimer startCountdownTimer(boolean firstShout){
            if (firstShout) {
                this.firstShout = firstShout;
                panicStatusDialog.setCancelable(false);
            } else {
                panicStatusDialog.setCancelable(true);
            }
            return start();
        }


    }
}
