
package info.guardianproject.intheclear;

import android.app.*;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.ResultReceiver;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import info.guardianproject.intheclear.ITCConstants.Preference;
import info.guardianproject.utils.EndActivity;

import java.util.ArrayList;

public class PanicActivity extends Activity implements OnClickListener, OnDismissListener {

    private static final String TAG = PanicActivity.class.getName();
    SharedPreferences sp;
    boolean oneTouchPanic;

    ListView listView;
    TextView shoutReadout, panicProgress, countdownReadout;
    Button controlPanic, cancelCountdown, panicControl;
 //   ScheduledTaskReceiver scheduledTaskReceiver;

    int panicState = ITCConstants.PanicState.AT_REST;
    public static final String RETURNFROMNOTIFICATION = "ReturnFromNotifiaction";
    public static final String CANCELNOTIFICATION = "CancelNotification";
    Dialog countdown;
    CountDownTimer cd;

    ProgressDialog panicStatusDialog;
    String currentPanicStatus;

    public static final String RESULT_RECEIVER = "resultReceiver";
    private ResultReceiver resultReceiver = new ResultReceiver(new Handler()) {
        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {
            switch (resultCode) {

                case PanicService.UPDATE_PROGRESS:
                    if (resultData.getString(ITCConstants.UPDATE_UI) != null){
                        updateProgressWindow(resultData.getString(ITCConstants.UPDATE_UI));
                    }
                    if (resultData.getInt(ITCConstants.UPDATE_PANICSTATE) != 0){
                        panicState = resultData.getInt(ITCConstants.UPDATE_PANICSTATE);
                        if (panicState == ITCConstants.PanicState.IN_CONTINUED_PANIC) {
                            Log.d(TAG, "inContinuedPanic now...");
                            executeNewCountdown(ITCConstants.Duration.CONTINUED_PANIC);
                        }
                    }
                    break;
            }
        }
    };

    private BroadcastReceiver killReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "killReciever on Receiver!");
            killActivity();
        }

    };
    IntentFilter killFilter = new IntentFilter();


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.panic);

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
                        cancelPanic();
                    }
                }
        );
        panicStatusDialog.setMessage(currentPanicStatus);
        panicStatusDialog.setTitle(getResources().getString(R.string.KEY_PANIC_BTN_PANIC));
        panicStatusDialog.setCanceledOnTouchOutside(false);
        panicStatusDialog.setCancelable(false);

     /*   if(savedInstanceState != null && savedInstanceState.get("startFromWidget") != null &&  savedInstanceState.getInt("startFromWidget", 0) != 0){
            doPanic();
        }*/

    }

    @Override
    public void onResume() {

        final ArrayList<WipeItem> wipeTasks = new ArrayList<WipeItem>(6);
        wipeTasks.add(0,
                new WipeItem(R.string.KEY_WIPE_WIPECONTACTS, sp, Preference.DEFAULT_WIPE_CONTACTS));
        wipeTasks.add(1,
                new WipeItem(R.string.KEY_WIPE_WIPEPHOTOS, sp, Preference.DEFAULT_WIPE_PHOTOS));
        wipeTasks.add(2,
                new WipeItem(R.string.KEY_WIPE_CALLLOG, sp, Preference.DEFAULT_WIPE_CALLLOG));
        wipeTasks.add(3,
                new WipeItem(R.string.KEY_WIPE_SMS, sp, Preference.DEFAULT_WIPE_SMS));
        wipeTasks.add(4,
                new WipeItem(R.string.KEY_WIPE_CALENDAR, sp, Preference.DEFAULT_WIPE_CALENDAR));
        wipeTasks.add(5,
                new WipeItem(R.string.KEY_WIPE_SDCARD, sp, Preference.DEFAULT_WIPE_FOLDERS));

        listView.setAdapter(new WipeItemAdapter(this, wipeTasks));
        listView.setChoiceMode(ListView.CHOICE_MODE_NONE);
        listView.setClickable(false);

        killFilter.addAction(this.getClass().toString());
        registerReceiver(killReceiver, killFilter);
        super.onResume();
    }

    @Override
    public void onStart() {
        super.onStart();
        alignPreferences();
        Log.d(TAG, "onStart called");
        //TODO: check if the following part ist still necessary --> s. onNewIntent
//----------------------------
        Intent i = getIntent();
        boolean returnFromNotification = false;
        if (i.getIntExtra(RETURNFROMNOTIFICATION, 0) == ITCConstants.Panic.RETURN) {
            returnFromNotification = true;
            i.removeExtra(RETURNFROMNOTIFICATION);
        }

        if (!oneTouchPanic) {
            //TODO: reduce switch if possible
            if (!returnFromNotification){
                panicControl.setText(this.getResources().getString(R.string.KEY_PANIC_BTN_PANIC));
                panicControl.setOnClickListener(this);
            } else {
                Toast.makeText(this, "Panic Job canceled!", Toast.LENGTH_SHORT).show();
                cancelPanic();
                panicControl.setText(this.getResources().getString(R.string.KEY_PANIC_BTN_PANIC));
                panicControl.setOnClickListener(this);
            }


        } else {
            Log.d(TAG, "Return From Notification: " + returnFromNotification);
            if (!returnFromNotification ){
                Toast.makeText(this, "Panic Job starting/continuing!", Toast.LENGTH_SHORT).show();

                panicControl.setText(getString(R.string.KEY_PANIC_MENU_CANCEL));
                panicControl.setOnClickListener(this);
                doPanic();
            }
            else {
                Toast.makeText(this, "Panic Job canceled!", Toast.LENGTH_SHORT).show();
                cancelPanic();
                panicControl.setText(this.getResources().getString(R.string.KEY_PANIC_BTN_PANIC));
                panicControl.setOnClickListener(this);
            }
        }

    }

    /*gets called from the Notification*/
    @Override
    public void onNewIntent(Intent i) {
        super.onNewIntent(i);
        Log.d(TAG, "onNewIntent called");

        boolean returnFromNotification = false;

        if (i.hasExtra(RETURNFROMNOTIFICATION) && i.getIntExtra(RETURNFROMNOTIFICATION, 0) == ITCConstants.Panic.RETURN) {
            // the app is being launched from the notification tray.
            Log.d(TAG, "Has Extra: ReturnFrom -> Notification");
            returnFromNotification = true;
            i.removeExtra(RETURNFROMNOTIFICATION);

        } else {
            Log.d(TAG, "HAS NOT EXTRA RETURN FROM!");
        }

        if (!returnFromNotification){
            panicControl.setText(this.getResources().getString(R.string.KEY_PANIC_BTN_PANIC));
            panicControl.setOnClickListener(this);
        } else {
            Toast.makeText(this, "Panic Job canceled!", Toast.LENGTH_SHORT).show();
            cancelPanic();
            panicControl.setText(this.getResources().getString(R.string.KEY_PANIC_BTN_PANIC));
            panicControl.setOnClickListener(this);
        }

        if (i.hasExtra("PanicCount"))
            Log.d(ITCConstants.Log.ITC, "Panic Count at: " + i.getIntExtra("PanicCount", 0));


        setIntent(i);

    }

    @Override
    public void onPause() {
        unregisterReceiver(killReceiver);

        super.onPause();
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

    public void cancelPanic() {
        if (panicState == ITCConstants.PanicState.IN_COUNTDOWN) {
            panicStatusDialog.cancel();
        }
        //allways cancel Countdown on cancelPanic (-> we now use more PanicStates e.g. IN_CONTINUED_PANIC, where the cd might be ongoing)
        cd.cancel();

        Intent i = new Intent(this, PanicService.class);
        i.putExtra(CANCELNOTIFICATION, " ");
        i.setAction(PanicService.ACTION_CANCEL);
        startService(i);
        // Log.d(TAG, "panicState: :" + panicState);

        //killActivity();
        finish();
    }

    @Override
    public void onBackPressed() {
        Log.d(TAG, "onBackPressed: PanicState: " + panicState);
        if (panicState != ITCConstants.PanicState.AT_REST) {
            Log.w(TAG, "not allowed to press back button atm!");
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onClick(View v) {
        if (v == panicControl && panicState == ITCConstants.PanicState.AT_REST) {
            doPanic();
        } else if (v == panicControl && panicState != ITCConstants.PanicState.AT_REST) {
            cancelPanic();

        }

    }

    @Override
    public void onDismiss(DialogInterface d) {

    }

    @Override
    protected void onDestroy() {
        panicStatusDialog.dismiss();
        super.onDestroy();
    }

    public void updateProgressWindow(String message) {
        panicStatusDialog.setMessage(message);
    }

    public void killActivity() {
        Intent toKill = new Intent(PanicActivity.this, EndActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(toKill);
    }

    public void launchPreferences() {
        Intent toPrefs = new Intent(this, SettingsActivity.class);
        startActivity(toPrefs);
    }

    private void doPanic() {

        executeNewCountdown(ITCConstants.Duration.COUNTDOWN);

    }

    private void executeNewCountdown(final long countdownduration){
        panicState = ITCConstants.PanicState.IN_COUNTDOWN;
        panicControl.setText(getString(R.string.KEY_PANIC_MENU_CANCEL));
        cd = new CountDownTimer(countdownduration,
                ITCConstants.Duration.COUNTDOWNINTERVAL) {
            int t = (int)(countdownduration/1000) - 1 ;

            @Override
            public void onFinish() {
                // start the panic
                startPanicService();
                // scheduleAlarm();
                // kill the activity doch lieber drin lassen?
                //  killActivity();
                //  FIXME: DONT FIXME ;) this causes an timing error:  killActivity();
            }


            @Override
            public void onTick(long millisUntilFinished) {
                panicStatusDialog.setMessage(
                        getString(R.string.KEY_PANIC_COUNTDOWNMSG) +
                                " " + t + " " +
                                getString(R.string.KEY_SECONDS)
                );
                t--;
            }

        };
        panicStatusDialog.show();
        cd.start();
    }

    private void startPanicService(){
        Intent intent = new Intent(getApplicationContext(), PanicService.class);
        intent.putExtra(RESULT_RECEIVER, resultReceiver);
        startService(intent);
    }
}
