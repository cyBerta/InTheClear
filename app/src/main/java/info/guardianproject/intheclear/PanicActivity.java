
package info.guardianproject.intheclear;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
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

import info.guardianproject.intheclear.ITCConstants.Preference;
import info.guardianproject.utils.EndActivity;

import java.util.ArrayList;

public class PanicActivity extends Activity implements OnClickListener, OnDismissListener {

    SharedPreferences sp;
    boolean oneTouchPanic;

    ListView listView;
    TextView shoutReadout, panicProgress, countdownReadout;
    Button controlPanic, cancelCountdown, panicControl;

    Intent panic, toKill;
    int panicState = ITCConstants.PanicState.AT_REST;

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
                    updateProgressWindow(resultData.getString(ITCConstants.UPDATE_UI));
                    break;
            }
        }
    };

    private BroadcastReceiver killReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
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
        if (!oneTouchPanic) {
            panicControl.setText(this.getResources().getString(R.string.KEY_PANIC_BTN_PANIC));
            panicControl.setOnClickListener(this);
        } else {
            panicControl.setText(getString(R.string.KEY_PANIC_MENU_CANCEL));
            panicControl.setOnClickListener(this);
            doPanic();
        }
    }

    @Override
    public void onNewIntent(Intent i) {
        super.onNewIntent(i);
        setIntent(i);

        if (i.hasExtra("ReturnFrom") && i.getIntExtra("ReturnFrom", 0) == ITCConstants.Panic.RETURN) {
            // the app is being launched from the notification tray.

        }

        if (i.hasExtra("PanicCount"))
            Log.d(ITCConstants.Log.ITC, "Panic Count at: " + i.getIntExtra("PanicCount", 0));
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
            // if panic hasn't started, then just kill the countdown
            cd.cancel();
        }

        toKill = new Intent(this, EndActivity.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        finish();
        startActivity(toKill);

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
        panicState = ITCConstants.PanicState.IN_COUNTDOWN;
        panicControl.setText(getString(R.string.KEY_PANIC_MENU_CANCEL));
        cd = new CountDownTimer(ITCConstants.Duriation.COUNTDOWN,
                ITCConstants.Duriation.COUNTDOWNINTERVAL) {
            int t = 5;

            @Override
            public void onFinish() {
                // start the panic
                Intent intent = new Intent(getApplicationContext(), PanicService.class);
                intent.putExtra(RESULT_RECEIVER, resultReceiver);
                startService(intent);

                // kill the activity
                killActivity();
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
}
