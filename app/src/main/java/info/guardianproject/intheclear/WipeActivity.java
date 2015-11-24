
package info.guardianproject.intheclear;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.SparseBooleanArray;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ListView;

import info.guardianproject.intheclear.ITCConstants.Preference;

import java.util.ArrayList;

public class WipeActivity extends Activity {
    private static final String TAG = "Wipe";

    private SharedPreferences sp;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.wipe);

        sp = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        final ListView listView = (ListView) findViewById(R.id.wipeListView);
        listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        listView.setBackgroundColor(getResources().getColor(android.R.color.white));

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

        Button cancelButton = (Button) findViewById(R.id.cancel);
        cancelButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        Button okButton = (Button) findViewById(R.id.ok);
        okButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                Editor editor = sp.edit();
                SparseBooleanArray checkedItems = listView.getCheckedItemPositions();
                for (int i = 0; i < checkedItems.size(); i++) {
                    switch (checkedItems.keyAt(i)) {
                        case 0:
                            editor.putBoolean(Preference.DEFAULT_WIPE_CONTACTS,
                                    checkedItems.valueAt(i));
                            break;
                        case 1:
                            editor.putBoolean(Preference.DEFAULT_WIPE_PHOTOS,
                                    checkedItems.valueAt(i));
                            break;
                        case 2:
                            editor.putBoolean(Preference.DEFAULT_WIPE_CALLLOG,
                                    checkedItems.valueAt(i));
                            break;
                        case 3:
                            editor.putBoolean(Preference.DEFAULT_WIPE_SMS,
                                    checkedItems.valueAt(i));
                            break;
                        case 4:
                            editor.putBoolean(Preference.DEFAULT_WIPE_CALENDAR,
                                    checkedItems.valueAt(i));
                            break;
                        case 5:
                            editor.putBoolean(Preference.DEFAULT_WIPE_FOLDERS,
                                    checkedItems.valueAt(i));
                            break;
                    }
                }
                editor.apply();
                finish();
            }
        });
    }
}
