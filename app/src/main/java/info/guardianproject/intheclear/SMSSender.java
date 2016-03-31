
package info.guardianproject.intheclear;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.telephony.SmsManager;
import android.util.Log;

import java.lang.ref.WeakReference;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class SMSSender implements SMSTesterConstants {
    private static final String TAG = SMSSender.class.getName();
    PendingIntent _sentPI, _deliveredPI;

    private SMSConfirm smsconfirm;
    boolean result = false;

    Context c;
    SmsManager sms;
//    private static SMSThread smsThread;
//    private Handler smsHandler;

    public SMSSender(Context c, Handler callback) {
        Log.d(TAG, "SMS Sender, registerReceiver");
        this.c = c;
        this.smsconfirm = SMSConfirm.getInstance();
//        this.smsconfirm.registerReceiverIn(c);

    }

    public void unregisterSMSConfirmReceiver(){
        this.smsconfirm.unregisterReceiverFrom(c);
    }

    public void sendSMS(String recipient, String messageData) {
        Intent initiatedIntent = new Intent(INITIATED);
        initiatedIntent.putExtra("recipient", recipient);
        initiatedIntent.putExtra("messageData", messageData);
        Intent sentIntent = new Intent(SENT);
        sentIntent.putExtra("recipient", recipient);
        sentIntent.putExtra("messageData", messageData);
        _sentPI = PendingIntent.getBroadcast(this.c, 0, sentIntent, 0);
        Intent delieveredIntent = new Intent(DELIVERED);
        delieveredIntent.putExtra("recipient", recipient);
        delieveredIntent.putExtra("messageData", messageData);
        _deliveredPI = PendingIntent.getBroadcast(this.c, 0, new Intent(DELIVERED), 0);

        sms = SmsManager.getDefault();

        ArrayList<String> splitMsg = sms.divideMessage(messageData);
        for (String msg : splitMsg) {
            try {
                sms.sendTextMessage(recipient, null, msg, _sentPI, _deliveredPI);
                smsconfirm.onReceive(c, initiatedIntent);
            } catch (IllegalArgumentException e) {
                initiatedIntent.putExtra("resultCode CANCELLED", Activity.RESULT_CANCELED);
                smsconfirm.onReceive(c, initiatedIntent);
            } catch (NullPointerException e) {
                initiatedIntent.putExtra("resultCode CANCELLED", Activity.RESULT_CANCELED);
                smsconfirm.onReceive(c, initiatedIntent);
            }
        }

    }

 /*   public void exitWithResult(boolean result, int process, int status) {
        Log.d(TAG, "exitWithResult");
        Message smsStatus = new Message();
        Map<String, Integer> msg = new HashMap<String, Integer>();
        int r = 1;
        if (result != false)
            r = -1;

        msg.put("smsResult", r);
        msg.put("process", process);
        msg.put("status", status);

        smsStatus.obj = msg;
        smsHandler.sendMessage(smsStatus);
    }
*/

/*    public class SMSThread extends Thread {

        private final String TAG = SMSThread.class.getName();

        @Override
        public void run() {
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(SENT);
            intentFilter.addAction(DELIVERED);
            c.registerReceiver(smsconfirm, intentFilter);

        }

        public void exitWithResult(boolean result, int process, int status) {
            Log.d(TAG, "exitWithResult");
            Message smsStatus = new Message();
            Map<String, Integer> msg = new HashMap<String, Integer>();
            int r = 1;
            if (result != false)
                r = -1;

            msg.put("smsResult", r);
            msg.put("process", process);
            msg.put("status", status);

            smsStatus.obj = msg;
            smsHandler.sendMessage(smsStatus);
        }
    }
    */

    public interface SMSConfirmInterface {
        public void onSMSSent(Intent intent);
    }

    public static class SMSConfirm extends BroadcastReceiver {
        private static final SMSConfirm INSTANCE = new SMSConfirm();
        private Set<String> contexts;

        private List<WeakReference<SMSConfirmInterface>> confirmSMS;

        public static SMSConfirm getInstance() {
            return INSTANCE;
        }

        private SMSConfirm(){
            contexts = Collections.synchronizedSet(new HashSet());
            confirmSMS = Collections.synchronizedList(new ArrayList<WeakReference<SMSConfirmInterface>>());
        }

        public void addSMSConfirmCallback(SMSConfirmInterface callback){
            Log.d(TAG, "addSMSConfirmCallbackInterface!");
            confirmSMS.add(new WeakReference<SMSConfirmInterface>(callback));
        }

        public void removeSMSConfirmCallback(SMSConfirmInterface callback){
            synchronized (confirmSMS) {
                for (WeakReference<SMSConfirmInterface> callbackRef : confirmSMS) {
                    if (callbackRef.get() != null && callbackRef.get().equals(callback)) {
//                    callbackRef.clear();
                        confirmSMS.remove(callbackRef);
                    }
                }
            }
        }


        public boolean isRegisteredIn(Context c){
            return contexts.contains(c.toString());

        }

        public boolean registerReceiverIn(Context c){
            if (contexts.contains(c.toString())){
                return false;
            }
            Log.d(TAG, "register ConfirmReceiver");
            contexts.add(c.toString());
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(SENT);
            intentFilter.addAction(DELIVERED);
            c.registerReceiver(INSTANCE, intentFilter);

            if (c instanceof SMSConfirmInterface){
                addSMSConfirmCallback((SMSConfirmInterface) c);
            }
            return true;
        }

        public boolean unregisterReceiverFrom(Context c){
            if (contexts.contains(c.toString())){
                Log.d(TAG, "unregisterReceiverFromContext = true");
                c.unregisterReceiver(this);
                contexts.remove(c.toString());
                if (c instanceof SMSConfirmInterface){
                    removeSMSConfirmCallback((SMSConfirmInterface) c);
                }
                return true;
            } else {
                Log.d(TAG, "unregisterReceiverFromContext = false");
                return false;
            }
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            intent.putExtra("resultCode", getResultCode());
            synchronized (confirmSMS){
                for (WeakReference<SMSConfirmInterface> callbackRef : confirmSMS){
                    if (callbackRef.get() != null){
                        SMSConfirmInterface callback = callbackRef.get();
                        callback.onSMSSent(intent);
                    } else {
                        confirmSMS.remove(callbackRef);
                    }
                }
            }
        }

        //   @Override
     /*   public void onReceive(Context context, Intent intent) {
            if (intent.getAction().compareTo(SENT) == 0) {
                if (getResultCode() != SMS_SENT) {
                    // the attempt to send has failed.
                    smsThread.exitWithResult(false, SMS_SENDING, getResultCode());
                    context.unregisterReceiver(this);
                }
            } else if (intent.getAction().compareTo(DELIVERED) == 0) {
                if (getResultCode() != SMS_DELIVERED) {
                    // the attempt to deliver has failed.
                    smsThread.exitWithResult(false, SMS_DELIVERY, getResultCode());
                    context.unregisterReceiver(this);
                } else {
                    smsThread.exitWithResult(true, SMS_DELIVERY, getResultCode());
                    context.unregisterReceiver(this);

                }
            }

        }*/

    }

}
