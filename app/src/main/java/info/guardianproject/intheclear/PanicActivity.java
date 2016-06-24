package info.guardianproject.intheclear;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.*;
import android.preference.PreferenceManager;
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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by richy on 18.03.16.
 */
public class PanicActivity extends Activity implements View.OnClickListener, ScheduleServiceClient.IScheduleClientCallback, CountdownProgressDialogInterface.OnCountdownFinishedListener {
    private static final String TAG = PanicActivity.class.getName();
    SharedPreferences sp;
    boolean oneTouchPanic;
    TextView shoutReadout;
    Button panicControl;
    CountdownProgressDialog countdownProgressDialog;
    private ScheduleServiceClient scheduleClient;
    private boolean stealthMode;
    int panicState;

    private AtomicBoolean uiReady = new AtomicBoolean(false);

    /**
     * Mehtods ot the Activity Life Cycle
     */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.panic);

        // Create a new service client and bind our activity to this service
        scheduleClient = new ScheduleServiceClient(this);
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

        countdownProgressDialog = new CountdownProgressDialog(this);
        countdownProgressDialog.setButton(getResources().getString(R.string.KEY_PANIC_MENU_CANCEL),
                new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (scheduleClient != null){
                            countdownProgressDialog.stopCountDown();
                            scheduleClient.stopPanic();
                        }
                    }
                });
        countdownProgressDialog.setTitle(getResources().getString(R.string.KEY_PANIC_BTN_PANIC));
        countdownProgressDialog.setCanceledOnTouchOutside(false);
        countdownProgressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                PanicActivity.this.finish();
            }
        });
        countdownProgressDialog.registerDialogCallbackReceiver(this);

    }

   @Override
    public void onBackPressed() {

       if (countdownProgressDialog.isShowing()){
           countdownProgressDialog.cancel();
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
                            countdownProgressDialog.updatePanicStatusExtra("Cannot delete SMS until InTheClear is set to default SMS app!");
                        }
                    } else {
                        //
                        /*Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && resultCode == Activity.RESULT_OK && PanicUtils.isDefaultSMSApp(this.getApplicationContext())) {*/
                        if (scheduleClient.isSMSPanicRunning()) {
                            if (!continueCountdown()) {
                                countdownProgressDialog.updateSMSPanicStatus("first sms will be sent within the next minute!");
                            }
                        } else {
                            countdownProgressDialog.startCountdown(true);
                        }
                    }
                }
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);


    }

    @Override
    public void onResume() {
        super.onResume();
        uiReady.compareAndSet(false, true);


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
        countdownProgressDialog.onActivityResume();
    }


    @Override
    protected void onPause() {
        uiReady.compareAndSet(true, false);
        countdownProgressDialog.onActivityPause();
        super.onPause();
    }

    @Override
    protected void onStop() {
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



    private void updateWipeStateMsg(Bundle b){
        String msg = "";
        int category;
        if (b != null) {
            int state = b.getInt(PIMWiper.pimWiperState);
            switch (state) {
                case PIMWiper.stateOnWipeStarted:
                    msg = "Wipe started";
                    break;
                case PIMWiper.stateOnWipeCancelled:
                    msg = "Wipe cancelled";
                    break;
                case PIMWiper.stateOnWipeCategoryFailed:
                    Exception e = (Exception) b.getSerializable(PIMWiper.pimWiperException);
                    msg = "Wipe failed: " + e.getMessage();
                    break;
                case PIMWiper.stateOnWipeFinished:
                    msg = "Wipe finished!!!";
                    break;
                case PIMWiper.stateOnWipeCategoryStart:
                    category = b.getInt(PIMWiper.pimWiperCategory);
                    msg = "Starting to wipe ";
                    msg = addCategoryToString(msg, category);
                    break;
                case PIMWiper.stateOnWipeCategoryFinished:
                    category = b.getInt(PIMWiper.pimWiperCategory);
                    msg = "Finished to wipe ";
                    msg = addCategoryToString(msg, category);
                    break;

                case PIMWiper.stateOnWipeFailed:
                    Serializable exception = b.getSerializable(PIMWiper.pimWiperException);
                    if (exception instanceof PIMWiper.PIMWiperNothingToWipeException){
                        msg = "Nothing selected to wipe!";
                    }
                    break;

                default:
                    break;
            }
            if (!TextUtils.isEmpty(msg)){
                countdownProgressDialog.updateWipePanicStatus(msg);
            }
        }
    }
    private boolean continueCountdown(){
        boolean isContinuingCountdown = false;
        long lastShout = scheduleClient.getLastShoutTime();
        Bundle b = scheduleClient.getCurrentWipeState();
        updateWipeStateMsg(b);

        if (lastShout != 0){
            long delta = 58000 - (System.currentTimeMillis() - lastShout);
            Log.d(TAG, "Last Shout  " + delta/1000);
            countdownProgressDialog.continueCountdown(delta);
            isContinuingCountdown = true;
        }
        return isContinuingCountdown;
    }

    private String addCategoryToString(String msg, int category){
        switch (category){
            case ITCConstants.Wipe.CALENDAR:
                msg = msg.concat(" calendar.");
                break;
            case ITCConstants.Wipe.CALLLOG:
                msg = msg.concat(" call log.");
                break;
            case ITCConstants.Wipe.CONTACTS:
                msg = msg.concat(" contacts.");
                break;
            case ITCConstants.Wipe.PHOTOS:
                msg = msg.concat(" photos.");
                break;
            case ITCConstants.Wipe.SMS:
                msg = msg.concat(" sms.");
                break;
            case ITCConstants.Wipe.SDCARD:
                msg =
                        msg.concat(" sdcard.");
                break;
        }
        return msg;
    }

    /**
     * Callback method of the ScheduleServiceClient
     **/

    @Override
    public void onCallbackReceived(String serviceState, int callback, Bundle extraData) {
        Log.d(TAG, "onCallbackReceived ServiceState: " + serviceState + " -> " + callback );

        if (serviceState.equals(ScheduleService.SERVICE_STATE)){
            switch (callback){
                case ScheduleService.SCHEDULESERVICECALLBACK_ONBIND:
                    if (scheduleClient.isSMSPanicRunning() || scheduleClient.isWipePanicRunning()){
                        //if (uiReady.get() && !continueCountdown()){
                        if (!continueCountdown()){
                            countdownProgressDialog.updateSMSPanicStatus("first sms will be sent within the next minute!");
                        }
                    } else {
                        if (oneTouchPanic){
                            doPanic(true);
                        }
                    }
                    break;
                case ScheduleService.SCHEDULESERVICECALLBACK_ALARMTASK_STOPPED:
                    Log.d(TAG, "ScheduleService requests to stop panic");
                    countdownProgressDialog.cancel();
                    break;
                case ScheduleService.SCHEDULESERVICECALLBACK_ALARMTASK_STARTED:
                    countdownProgressDialog.updateSMSPanicStatus("first sms will be sent within the next minute!");
                    countdownProgressDialog.setCancelable(true);
                    break;
                case ScheduleService.SCHEDULESERVICECALLBACK_SERVICE_STOPPED:
                    Log.d(TAG, "Scheduleservise stopped");
                    break;
                case ScheduleService.SCHEDULESERVICECALLBACK_WIPETASK_STATE_CHANGED:
                    updateWipeStateMsg(extraData);
                    break;
                default:
                    break;

            }
        } else if (serviceState.equals(ShoutService.SERVICE_STATE)){
            switch (callback){
                default:
                case ShoutService.SHOUTSERVICECALLBACK_START:
                    Log.d(TAG, "sendingSMS");
                    countdownProgressDialog.updateSMSPanicStatus("sending SMS...");

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

    public void repeatSMSPanicCountdown(){
        doPanic(false);
    }
    public void doPanic(boolean firstShout){
        Log.d(TAG, "doPanic - fistShout: " + firstShout);
        boolean isDefaultApp = PanicUtils.isDefaultSMSApp(this.getApplicationContext());
       // PanicActivity.this.startCountdownTimer(firstShout);
        countdownProgressDialog.startCountdown(firstShout);
        if (isDeleteSMS() && !isDefaultApp){
            countdownProgressDialog.updatePanicStatusExtra("Cannot delete SMS until InTheClear is set to default SMS app!");
            //countDownTimer.addExtraInfo("Cannot delete SMS until InTheClear is set to default SMS app!");
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
            if (uiReady.get()){
                a.show();
            }

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

    @Override
    public void onCountdownFinished(boolean firstShout) {
        if (firstShout){
            Log.d(TAG, "firstShout");
            try{
                scheduleClient.startPanic();
            } catch (InterruptedException e){
                Log.d(TAG, "Wiptask was interrupted: " + e.getMessage());
            }
            if (stealthMode){
                clearBackstackAndFinish();
            } else {
                if (countdownProgressDialog.isShowing()){
                        countdownProgressDialog.updateSMSPanicStatus("Starting...");
                }
            }

        } else {
            Log.d(TAG, "not firstShout");
            if (stealthMode) {
                clearBackstackAndFinish();
            } else {
                if (countdownProgressDialog.isShowing()){
                    countdownProgressDialog.updateSMSPanicStatus("Repeating...");
                }
            }
        }
    }


}
