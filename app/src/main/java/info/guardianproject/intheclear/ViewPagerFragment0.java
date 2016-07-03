package info.guardianproject.intheclear;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * Created by l on 14.06.16.
 */
public class ViewPagerFragment0 extends Fragment {

    TextView textView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(
                R.layout.view_pager_fragment_0, container, false);
        textView = (TextView) rootView.findViewById(R.id.wizard_explanation);
        String[] text = getResources().getStringArray(R.array.WIZARD_TEXT);
        textView.setText(text[0]);
        return rootView;
    }

}
