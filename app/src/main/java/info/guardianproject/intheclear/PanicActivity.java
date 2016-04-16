package info.guardianproject.intheclear;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.IntentCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import info.guardianproject.utils.EndActivity;

import java.util.ArrayList;

/**
 * Created by richy on 18.03.16.
 */
public class PanicActivity extends Activity implements View.OnClickListener, ScheduleServiceClient.IScheduleClientCallback  {
    private static final String TAG = PanicActivity.class.getName();
    SharedPreferences sp;
    boolean oneTouchPanic;
    ListView listView;
    TextView shoutReadout, panicProgress, countdownReadout;
    Button controlPanic, cancelCountdown, panicControl;
    CountDownTimer countDownTimer;
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
        scheduleClient.startService();
        scheduleClient.doBindService();

        sp = PreferenceManager.getDefaultSharedPreferences(getBaseContext());

        panicControl = (Button) findViewById(R.id.panicControl);
        shoutReadout = (TextView) findViewById(R.id.shoutReadout);
        listView = (ListView) findViewById(R.id.wipeItems);

        // if this is not a cell phone, then no need to show the panic message
        if (TextUtils.isEmpty(PhoneInfo.getIMEI())) {
            shoutReadout.setVisibility(View.GONE);
            TextView shoutReadoutTitle = (TextView) findViewById(R.id.shoutReadoutTitle);
            shoutReadoutTitle.setVisibility(View.GONE);
        } else {
            String panicMsg = sp.getString(ITCConstants.Preference.DEFAULT_PANIC_MSG, "");
            shoutReadout.setText("\n\n" + panicMsg + "\n\n"
                    + ShoutController.buildShoutData(getResources()));
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
        panicStatusDialog.setCancelable(false);



    }

    @Override
    protected void onStart() {
        super.onStart();
        alignPreferences();
        panicControl.setOnClickListener(this);

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

        listView.setAdapter(new WipeItemAdapter(this, wipeTasks));
        listView.setChoiceMode(ListView.CHOICE_MODE_NONE);
        listView.setClickable(false);
        stealthMode = sp.getBoolean("stealthMode", true);
    }



    @Override
    protected void onStop() {
        panicStatusDialog.dismiss();
        super.onStop();
    }


    @Override
    protected void onDestroy() {
        // When our activity is stopped ensure we also stop the connection to the service
        // this stops us leaking our activity into the system *bad*
        if(scheduleClient != null)
            scheduleClient.doUnbindService();
        super.onDestroy();
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
                    if (oneTouchPanic){
                        Log.d(TAG, "SCHEDULESERVICECALLBACK_ONBIND");
                        doPanic(true);
                    }
                    break;
                case ScheduleService.SCHEDULESERVICECALLBACK_ALARMTASK_STOPPED:
                    Log.d(TAG, "ScheduleService requests to stop panic");
                    stopCountDownTimer();
                    panicStatusDialog.cancel();
                    break;
                case ScheduleService.SCHEDULESERVICECALLBACK_ALARMTASK_STARTED:
                    showPanicStatus("first sms will be sent within the next minute!");
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
            doPanic(true);
        }
    }

    /*private methods*/

    private void stopCountDownTimer(){
        Log.d(TAG, "stopCountdownTimer!!");
        countDownTimer.cancel();
    }

    private void startCountdownTimer(final long countdownduration, final boolean firstShout){
        panicState = ITCConstants.PanicState.IN_COUNTDOWN;
        countDownTimer = new CountDownTimer(countdownduration,
                ITCConstants.Duration.COUNTDOWNINTERVAL){

            @Override
            public void onTick(long millisUntilFinished) {
                    showPanicStatus(
                            getString(R.string.KEY_PANIC_COUNTDOWNMSG) +
                                    " " + ((int) millisUntilFinished/1000-1) + " " +
                                    getString(R.string.KEY_SECONDS)
                    );
            }

            @Override
            public void onFinish() {
                Log.d(TAG, "onFininsh called");
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
                        showPanicStatus("Starting... ");
                    }

                } else {
                    Log.d(TAG, "not firstShout");
                    showPanicStatus("Repeating...");
                }


            }
        };
        countDownTimer.start();
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
        if (firstShout){
            startCountdownTimer(10000, firstShout);
        } else {
            startCountdownTimer(58000, false);
        }
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
        ActivityCompat.finishAffinity(this);
        startActivity(mainIntent);
    }

}
