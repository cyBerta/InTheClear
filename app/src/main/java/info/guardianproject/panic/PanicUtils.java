
package info.guardianproject.panic;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.provider.Telephony;
import android.text.TextUtils;
import android.util.Log;

public class PanicUtils {

    static String getCallingPackageName(Activity activity) {
        // getCallingPackage() was unstable until android-18, use this
        String packageName = activity.getCallingActivity().getPackageName();
        if (TextUtils.isEmpty(packageName)) {
            packageName = activity.getIntent().getPackage();
        }
        if (TextUtils.isEmpty(packageName)) {
            Log.e(activity.getPackageName(),
                    "Received blank Panic.ACTION_DISCONNECT Intent, it must be sent using startActivityForResult()!");
        }
        return packageName;
    }

    public static boolean isDefaultSMSApp(Context c){
        int currentapiVersion = android.os.Build.VERSION.SDK_INT;
        if (currentapiVersion >= Build.VERSION_CODES.KITKAT){
            return PanicUtils.isNewDefaultSMSApp(c);
        } else {
            return true;
        }
    }

    @TargetApi(19)
    private static boolean isNewDefaultSMSApp(Context c){
            return c.getPackageName().equals(Telephony.Sms.getDefaultSmsPackage(c));
    }



}
