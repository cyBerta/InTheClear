package info.guardianproject.intheclear;


import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Typeface;
import android.os.CountDownTimer;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.*;
import android.util.Log;

import java.lang.ref.WeakReference;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by richy on 16.05.16.
 */
public class CountdownProgressDialog extends ProgressDialog implements DialogInterface.OnShowListener,
        DialogInterface.OnCancelListener,
        DialogInterface.OnDismissListener,
        CountdownProgressDialogInterface {

    public static final String TAG = CountdownProgressDialog.class.getName();

    private Queue<Runnable> uiQueue;

    private CharSequence extraInfo = "";
    private CharSequence mSMSProgressMessage;
    private CharSequence mWipeProgressMessage;
    private AtomicBoolean uiReady = new AtomicBoolean(false);
    private CustomCountdownTimer countDownTimer;
    private boolean mShowMessage;

    private ArrayList<WeakReference<CountdownProgressDialogInterface.OnCountdownFinishedListener>> finishCallbackListener;

    public CountdownProgressDialog(Context context) {
        super(context);
        finishCallbackListener = new ArrayList<>();
    }

    public CountdownProgressDialog(Context context, int theme) {
        super(context, theme);
        finishCallbackListener = new ArrayList<>();
    }

    public void registerDialogCallbackReceiver(CountdownProgressDialogInterface.OnCountdownFinishedListener listenerImpl){
        WeakReference<CountdownProgressDialogInterface.OnCountdownFinishedListener> reference = new WeakReference<CountdownProgressDialogInterface.OnCountdownFinishedListener>(listenerImpl);
        finishCallbackListener.add(reference);
    }

    private void notifyProgressFinishedListeners(boolean firstShout){
        Iterator<WeakReference<OnCountdownFinishedListener>> i = finishCallbackListener.iterator();
        while (i.hasNext()) {
            WeakReference<CountdownProgressDialogInterface.OnCountdownFinishedListener> reference = i.next();
            if (reference.get() == null){
                i.remove();
            } else {
                reference.get().onCountdownFinished(firstShout);
            }
        }
    }


    private void showPanicStatus(CharSequence extraInfo, CharSequence smsMsg, CharSequence wipeMsg){

        CharSequence progressMessages = "";

        if (!TextUtils.isEmpty(extraInfo)){
            SpannableString spanStringExtra = new SpannableString(TextUtils.concat(extraInfo, "\n\n"));
            spanStringExtra.setSpan(new StyleSpan(Typeface.ITALIC), 0, spanStringExtra.length(), 0);
            progressMessages = spanStringExtra;
        }

        if (!TextUtils.isEmpty(smsMsg)){
            SpannableString spanSMSMsg = new SpannableString(TextUtils.concat(smsMsg, "\n"));
            spanSMSMsg.setSpan(new BulletSpan(15), 0, spanSMSMsg.length(), 0);
            progressMessages = TextUtils.concat(progressMessages, spanSMSMsg);

        }

        if (!TextUtils.isEmpty(wipeMsg)){
            SpannableString spanWipeMsg = new SpannableString(TextUtils.concat(wipeMsg, "\n"));
            spanWipeMsg.setSpan(new BulletSpan(15), 0, spanWipeMsg.length(), 0);
            progressMessages = TextUtils.concat(progressMessages, spanWipeMsg);
        }



       /* CharSequence t1 = getText(R.string.xxx1);
        SpannableString s1 = new SpannableString(t1);
        s1.setSpan(new BulletSpan(15), 0, t1.length(), 0);
        CharSequence t2 = getText(R.string.xxx2);
        SpannableString s2 = new SpannableString(t2);
        s2.setSpan(new BulletSpan(15), 0, t2.length(), 0);
        textView.setText(TextUtils.concat(s1, s2));*/



        //CharSequence progressMessages = TextUtils.concat( spanStringExtra, spanSMSMsg, spanWipeMsg);

        CountdownProgressDialog.this.setMessage(progressMessages);
        mShowMessage = true;
        if (uiReady.get()){
            CountdownProgressDialog.this.show();
        }
    }

    public void stopCountDown(){
        Log.d(TAG, "stopCountdownTimer!!");
        if (countDownTimer != null ){
            countDownTimer.cancel();
        }
    }

    public void updateSMSPanicStatus(String smsMsg){
        mSMSProgressMessage = (!TextUtils.isEmpty(smsMsg) ? smsMsg : "");
        showPanicStatus(extraInfo, mSMSProgressMessage, mWipeProgressMessage);
    }

    public void updateWipePanicStatus(String wipeMsg){
        mWipeProgressMessage = (!TextUtils.isEmpty(wipeMsg) ? wipeMsg : "");
        showPanicStatus(extraInfo, mSMSProgressMessage, mWipeProgressMessage);
    }
    public void updatePanicStatusExtra(String extraMsg){
        extraInfo = (!TextUtils.isEmpty(extraMsg) ? extraMsg : "");
        showPanicStatus(extraInfo, mSMSProgressMessage, mWipeProgressMessage);
    }

    public void showPanicStatus(String msg){
        showPanicStatus(msg, null, null);
    }

    public void startCountdown(boolean firstShout){
        int duration = 58000;
        if (firstShout) {
            duration = 10000;
        }

        countDownTimer = new CustomCountdownTimer(duration, ITCConstants.Duration.COUNTDOWNINTERVAL);
        countDownTimer.startCountdownTimer(firstShout);
    }

    public void continueCountdown(long duration){
        countDownTimer = new CustomCountdownTimer(duration, ITCConstants.Duration.COUNTDOWNINTERVAL);
        countDownTimer.startCountdownTimer(false);
    }



    //Dialog Interface listeners
    @Override
    public void onCancel(DialogInterface dialog) {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        mShowMessage = false;
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }



    @Override
    public void onShow(DialogInterface dialog) {

    }



    public void onActivityResume(){
        uiReady.compareAndSet(false, true);
        if (mShowMessage){
            this.show();
        }
    }


    public void onActivityPause(){
        uiReady.compareAndSet(true, false);
        this.dismiss();
    }


    protected class CustomCountdownTimer extends CountDownTimer {

        private boolean firstShout = false;
        private boolean isRunning = false;
        private Context activityContext;

        /**
         * @param millisInFuture    The number of millis in the future from the call
         *                          to {@link #start()} until the countdown is done and {@link #onFinish()}
         *                          is called.
         * @param countDownInterval The interval along the way to receive
         *                          {@link #onTick(long)} callbacks.
         */
        private CustomCountdownTimer(long millisInFuture, long countDownInterval) {
            super(millisInFuture, countDownInterval);
        }

        public CustomCountdownTimer(long millisInFuture, long countDownInterval, Context c){
            super(millisInFuture, countDownInterval);

            this.activityContext = c;
        }

        public boolean isRunning(){
            return isRunning;
        }



        @Override
        public void onTick(long millisUntilFinished) {
            isRunning = true;

            SpannableString seconds = new SpannableString(String.valueOf((int) millisUntilFinished/1000-1));
            seconds.setSpan(new StyleSpan(Typeface.BOLD), 0, seconds.length(), 0);
            seconds.setSpan(new RelativeSizeSpan(1.15f), 0, seconds.length(), 0);
            SpannableString keyPanicCountdown;
            if(firstShout){
                keyPanicCountdown = new SpannableString(getContext().getString(R.string.KEY_PANIC_COUNTDOWNMSG));
            } else {
                keyPanicCountdown =  new SpannableString(getContext().getString(R.string.KEY_SHOUT_COUNTDOWNMSG));
            }
            CharSequence keySeconds = getContext().getString(R.string.KEY_SECONDS);
            mSMSProgressMessage = TextUtils.concat(keyPanicCountdown," ",
                    seconds, " ",
                    keySeconds);
                    /*getContext().getString(R.string.KEY_PANIC_COUNTDOWNMSG) +
                        " " + seconds + " " +
                        getContext().getString(R.string.KEY_SECONDS);*/
            showPanicStatus(extraInfo, mSMSProgressMessage, mWipeProgressMessage);

        }

        @Override
        public void onFinish() {
            mSMSProgressMessage = "";
            notifyProgressFinishedListeners(firstShout);
            isRunning = false;
        }

        public synchronized final CountDownTimer startCountdownTimer(boolean firstShout){
            if (firstShout) {
                this.firstShout = firstShout;
                CountdownProgressDialog.this.setCancelable(false);
            } else {
                CountdownProgressDialog.this.setCancelable(true);
            }
            return start();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
    }


}
