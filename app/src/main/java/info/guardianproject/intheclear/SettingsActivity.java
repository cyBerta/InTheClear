
package info.guardianproject.intheclear;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.util.Log;

import info.guardianproject.panic.PanicReceiver;

import java.util.Locale;

public class SettingsActivity extends PreferenceActivity implements OnPreferenceChangeListener {
    PreferenceCategory pc;
    ListPreference lang;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);

        if (PanicReceiver.checkForDisconnectIntent(this)) {
            Log.i("ITCPreferences", "disconnecting from trigger");
            finish();
            return;
        }

        pc = (PreferenceCategory) findPreference(ITCConstants.Preference.WIPERCAT);
        pc.removePreference((CheckBoxPreference) findPreference("IsVirginUser"));

        lang = (ListPreference) findPreference(ITCConstants.Preference.DEFAULT_LANGUAGE);
        lang.setOnPreferenceChangeListener(this);
    }

    public void setNewLocale(String localeCode) {
        Configuration config = new Configuration();
        config.locale = new Locale(localeCode);
        getBaseContext().getResources().updateConfiguration(config,
                getBaseContext().getResources().getDisplayMetrics());
        Log.d(ITCConstants.Log.ITC, "current configuration = "
                + getBaseContext().getResources().getConfiguration().locale);
        // go to home to "reload" activity properly?
        startActivity(new Intent(this, InTheClearActivity.class));
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object obj) {
        if (preference == lang) {
            setNewLocale(obj.toString());
            return true;
        }
        return false;
    }

}
