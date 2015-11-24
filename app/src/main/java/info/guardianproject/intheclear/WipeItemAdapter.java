
package info.guardianproject.intheclear;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckedTextView;
import android.widget.ListView;

import java.util.List;

public class WipeItemAdapter extends ArrayAdapter<WipeItem> {

    public WipeItemAdapter(Context context, List<WipeItem> objects) {
        super(context, android.R.layout.simple_list_item_multiple_choice, objects);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        WipeItem wipeItem = getItem(position);

        ListView listView = (ListView) parent;
        listView.setItemChecked(position, wipeItem.selected);

        CheckedTextView ctv = (CheckedTextView) super.getView(position, convertView, parent);
        ctv.setChecked(wipeItem.selected);
        ctv.setText(wipeItem.resId);
        ctv.setTextColor(ctv.getResources().getColor(android.R.color.black));

        return ctv;
    }
}
