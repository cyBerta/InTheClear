package info.guardianproject.intheclear;


import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.CountDownTimer;
import android.os.Handler;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.*;
import android.util.Log;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ProgressBar;
import info.guardianproject.panic.PanicUtils;

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
    private CharSequence extraInfo = "";
    private CharSequence mSMSProgressMessage = "";
    private CharSequence mWipeProgressMessage = "";
    private AtomicBoolean uiReady = new AtomicBoolean(false);
    private CustomCountdownTimer countDownTimer;
    private boolean mShowMessage;
    private Drawable animationDrawable;
    private TimedQueueHandler timedQueue;
    private Handler uiHandler;
    private ArrayList<WeakReference<CountdownProgressDialogInterface.OnCountdownFinishedListener>> finishCallbackListener;
    private boolean firstShout = false;
    private  ProgressBar animatedProgressBar;
    private Thread timedQueueThread;

    public CountdownProgressDialog(Context context) {
        super(context);


        finishCallbackListener = new ArrayList<>();
        animationDrawable = PanicUtils.getDrawable(context,R.drawable.panic);
        timedQueue = new TimedQueueHandler();
        uiHandler = new Handler();

    }


    public CountdownProgressDialog(Context context, int theme) {
        super(context, theme);
        finishCallbackListener = new ArrayList<>();
        animationDrawable  = PanicUtils.getDrawable(context, R.drawable.panic);
        timedQueue = new TimedQueueHandler();

    }

    public void registerDialogCallbackReceiver(CountdownProgressDialogInterface.OnCountdownFinishedListener listenerImpl){
        WeakReference<CountdownProgressDialogInterface.OnCountdownFinishedListener> reference = new WeakReference<CountdownProgressDialogInterface.OnCountdownFinishedListener>(listenerImpl);
        finishCallbackListener.add(reference);
    }

    private void notifyProgressFinishedListeners(){
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
       timedQueue.push(wipeMsg);
    }



    public void updatePanicStatusExtra(String extraMsg){
        extraInfo = (!TextUtils.isEmpty(extraMsg) ? extraMsg : "");
        showPanicStatus(extraInfo, mSMSProgressMessage, mWipeProgressMessage);
    }

    public void showPanicStatus(String msg){
        showPanicStatus(msg, null, null);
    }

    public void startCountdown(boolean firstShout){
        this.firstShout = firstShout;
        int duration = 58000;
        if (firstShout) {
            duration = 10000;
        }
        timedQueue.start();
        timedQueueThread = new Thread(timedQueue);
        timedQueueThread.start();
        countDownTimer = new CustomCountdownTimer(duration, ITCConstants.Duration.COUNTDOWNINTERVAL);
        countDownTimer.startCountdownTimer();
    }

    public void continueCountdown(long duration){
        firstShout = false;
        timedQueue.start();
        timedQueueThread = new Thread(timedQueue);
        timedQueueThread.start();
        countDownTimer = new CustomCountdownTimer(duration, ITCConstants.Duration.COUNTDOWNINTERVAL);
        countDownTimer.startCountdownTimer();
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

    @Override
    public void onStart() {
        super.onStart();

        animatedProgressBar = (ProgressBar) findViewById(android.R.id.progress);
        if (firstShout){
            animatedProgressBar.setIndeterminateDrawable(animationDrawable);
            Animation animation = AnimationUtils.loadAnimation(CountdownProgressDialog.this.getContext(), R.anim.animation_fade_in_out);
            animatedProgressBar.startAnimation(animation);
            CountdownProgressDialog.this.setCancelable(false);
            CountdownProgressDialog.this.setIndeterminate(true);
        } else {
            animatedProgressBar.clearAnimation();
            animatedProgressBar.setIndeterminateDrawable(animationDrawable);
            animatedProgressBar.invalidateDrawable(animationDrawable);
            CountdownProgressDialog.this.setCancelable(true);
            CountdownProgressDialog.this.setIndeterminate(false);
            //  CountdownProgressDialog.this.invalidateProgressBar();
        }
    }

    @Override
    protected void onStop() {
        animatedProgressBar.setIndeterminateDrawable(null);
        animatedProgressBar.clearAnimation();
        animatedProgressBar = null;
        timedQueue.stop();
        super.onStop();
    }

    @Override
    public void show() {
        super.show();
        if (!firstShout){
            animatedProgressBar.clearAnimation();
        }
    }


    protected class CustomCountdownTimer extends CountDownTimer {

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
            notifyProgressFinishedListeners();
            if (firstShout) firstShout = false;
            isRunning = false;
        }

        public synchronized final CountDownTimer startCountdownTimer(){



            return start();
        }
    }

    private class TimedQueueHandler implements Runnable {
        private LinkedList<String> queue;
        private boolean running = true;
        public TimedQueueHandler(){
            queue = new LinkedList<>();
        }

        @Override
        public void run() {
            while (running){
                try {
                    Thread.sleep(750);
                    poll();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        public synchronized void push(String msg){
            queue.add(msg);
        }

        public synchronized void poll(){
            String wipeMsg = queue.poll();
            if (!TextUtils.isEmpty(wipeMsg)){
                mWipeProgressMessage = wipeMsg;
            }
          //  mWipeProgressMessage = (!TextUtils.isEmpty(wipeMsg) ? wipeMsg : "");
            uiHandler.post(new Runnable() {
                @Override
                public void run() {
                    showPanicStatus(extraInfo, mSMSProgressMessage, mWipeProgressMessage);
                }
            });
        }

        public void stop(){
            running = false;
        }

        public void start(){
            running = true;
        }

    }

}
