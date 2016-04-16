
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

/**
 * Instances of SMSSender use Android's SmsManager in order to send a sms.
 * SMSSender defines a BroadcastReceiver as a subclass that handles incoming broadcasts about
 * the state of the sent sms. Moreover, it defines the callback interface SMSConfirmInterface
 * for notifying other components about the state of the sent sms.
 */
public class SMSSender {
    private static final String TAG = SMSSender.class.getName();
    PendingIntent _sentPI, _deliveredPI;

    private SMSConfirm smsconfirm;

    Context c;
    SmsManager sms;

    public SMSSender(Context c, Handler callback) {
        Log.d(TAG, "SMS Sender, registerReceiver");
        this.c = c;
        this.smsconfirm = SMSConfirm.getInstance();
    }

    public void sendSMS(String recipient, String messageData) {
        Intent initiatedIntent = new Intent(SMSConfirmInterface.INITIATED);
        initiatedIntent.putExtra(SMSConfirmInterface.recipient, recipient);
        initiatedIntent.putExtra(SMSConfirmInterface.messageData, messageData);
        Intent sentIntent = new Intent(SMSConfirmInterface.SENT);
        sentIntent.putExtra(SMSConfirmInterface.recipient, recipient);
        sentIntent.putExtra(SMSConfirmInterface.messageData, messageData);
        _sentPI = PendingIntent.getBroadcast(this.c, 0, sentIntent, 0);
        Intent deliveredIntent = new Intent(SMSConfirmInterface.DELIVERED);
        deliveredIntent.putExtra(SMSConfirmInterface.recipient, recipient);
        deliveredIntent.putExtra(SMSConfirmInterface.messageData, messageData);
        _deliveredPI = PendingIntent.getBroadcast(this.c, 0, deliveredIntent, 0);

        sms = SmsManager.getDefault();

        ArrayList<String> splitMsg = sms.divideMessage(messageData);
        for (String msg : splitMsg) {
            try {
                sms.sendTextMessage(recipient, null, msg, _sentPI, _deliveredPI);
                smsconfirm.onReceive(c, initiatedIntent);
            } catch (IllegalArgumentException e) {
                initiatedIntent.putExtra(SMSConfirmInterface.resultCode, Activity.RESULT_CANCELED);
                smsconfirm.onReceive(c, initiatedIntent);
            } catch (NullPointerException e) {
                initiatedIntent.putExtra(SMSConfirmInterface.resultCode, Activity.RESULT_CANCELED);
                smsconfirm.onReceive(c, initiatedIntent);
            }
        }

    }

    /**
     * Callback interface that classes can implement to be notified if SMSConfirm receives a
     * system broadcast about the state of a sent sms.
     */
    public interface SMSConfirmInterface {
        void onSMSSent(Intent intent);
        String resultCode = "resultCode";
        String recipient = "recipient";
        String messageData = "messageData";
        public final static String INITIATED = "SMS_INITIATED";
        public final static String SENT = "SMS_SENT";
        public final static String DELIVERED = "SMS_DELIVERED";
    }

    /**
     * Broadcast receiver with a Singleton pattern.
     * Allows to connect any Activity to that one instance of the SMSConfirm-BroadcastReceiver.
     * Sends incoming broadcast intents via a callback interface to components that implement SMSConfirmInterface
     * (for now ScheduleService).
     * Handles registering itself to the contexts on its own.
     */
    public static class SMSConfirm extends BroadcastReceiver {
        private static final SMSConfirm INSTANCE = new SMSConfirm();
        private Set<String> contexts;

        private List<WeakReference<SMSConfirmInterface>> confirmSMS;

        /**
         * Getter method to receive the singleton instance of SMSConfirm.
         * @return singleton instance of SMSConfirm
         */
        public static SMSConfirm getInstance() {
            return INSTANCE;
        }

        /**
         * Private constructor as this is a Singleton
         */
        private SMSConfirm(){
            contexts = Collections.synchronizedSet(new HashSet());
            confirmSMS = Collections.synchronizedList(new ArrayList<WeakReference<SMSConfirmInterface>>());
        }

        /**
         * adds a component that implements the SMSConfirmInterface to the list of "observers"
         * that get notified when a new intent is received
         * @param callback implementation of the SMSConfirmInterface
         */
        public void addSMSConfirmCallback(SMSConfirmInterface callback){
            Log.d(TAG, "addSMSConfirmCallbackInterface!");
            confirmSMS.add(new WeakReference<SMSConfirmInterface>(callback));
        }

        /**
         * removes a  component implementing the SMSConfirmInterface form the list of "observers"
         * @param callback implementation of the SMSConfirmInterface
         */
        public void removeSMSConfirmCallback(SMSConfirmInterface callback){
            synchronized (confirmSMS) {
                for (WeakReference<SMSConfirmInterface> callbackRef : confirmSMS) {
                    if (callbackRef.get() != null && callbackRef.get().equals(callback)) {
                        callbackRef.clear();
                        confirmSMS.remove(callbackRef);
                    }
                }
            }
        }

        /**
         * Method to check whether a  Context already registers the SMSConfirm-BroadcastReceiver.
         * @param c Context that should or should not register SMSConfirm
         * @return if true, the context has already registered SMSConfirm, otherwise not
         */
        public boolean isRegisteredIn(Context c){
            return contexts.contains(c.toString());

        }

        /**
         * Registers SMSConfirm to a Context if it is not already registered
         * If the Context also implements the SMSConfirmInterface for receiving callbacks,
         * the SMSConfirmInterface implementation will be added to the list of observing callback listeners
         * @param c Context that should register SMSConfirm and may implement SMSConfirmInterface
         * @return true if new registration was successful, false if registration was done before already
         */
        public boolean registerReceiverIn(Context c){
            if (contexts.contains(c.toString())){
                return false;
            }
            Log.d(TAG, "register ConfirmReceiver");
            contexts.add(c.toString());
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(SMSConfirmInterface.INITIATED);
            intentFilter.addAction(SMSConfirmInterface.SENT);
            intentFilter.addAction(SMSConfirmInterface.DELIVERED);
            c.registerReceiver(INSTANCE, intentFilter);

            if (c instanceof SMSConfirmInterface){
                addSMSConfirmCallback((SMSConfirmInterface) c);
            }
            return true;
        }

        /**
         * Unregisters SMSConfirm from a Context.
         * If that Context implements the SMSConfirmInterface remove that implementation from the list
         * of observing callback listeners
         * @param c
         * @return
         */
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

        /**
         * implementation of BroadcastReceiver's onReceive method
         * @param context
         * @param intent
         */
        @Override
        public void onReceive(Context context, Intent intent) {
            if (!intent.hasExtra(SMSConfirmInterface.resultCode)){
                intent.putExtra(SMSConfirmInterface.resultCode, getResultCode());
            }
            // As multiple threads may use that single instance, we have to ensure consistency
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

    }

}
