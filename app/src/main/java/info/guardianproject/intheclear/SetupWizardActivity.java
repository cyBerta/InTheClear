package info.guardianproject.intheclear;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import info.guardianproject.utils.CompatUtils;

public class SetupWizardActivity extends AppCompatActivity {


    LinearLayout wizardStatusTrack;
    TextView wizardTitle;
    String[] titles;
    StatusCircle[] circles;
    Button btForward, btBack;

    /**
     * The number of pages (wizard steps) to show in this demo.
     */
    private static final int NUM_PAGES = 5;

    /**
     * The pager widget, which handles animation and allows swiping horizontally to access previous
     * and next wizard steps.
     */
    private ViewPager mPager;

    /**
     * The pager adapter, which provides the pages to the view pager widget.
     */
    private PagerAdapter mPagerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.setup_wizard);
        //initiiert die HeaderLeiste (gruener Punkt...)
        wizardStatusTrack = (LinearLayout) findViewById(R.id.wizard_header);
        //Title fuer den Header
        titles = getResources().getStringArray(R.array.WIZARD_TITLES);
        wizardTitle = (TextView) findViewById(R.id.wizard_title);

         // Instantiate a ViewPager and a PagerAdapter.
        mPager = (ViewPager) findViewById(R.id.pager);
        OurPageChangeListener ourPageChangeListener = new OurPageChangeListener();
       mPager.addOnPageChangeListener(ourPageChangeListener);
        mPagerAdapter = new ScreenSlidePagerAdapter(getSupportFragmentManager());
        mPager.setAdapter(mPagerAdapter);
        circles = new StatusCircle[mPagerAdapter.getCount()];
        btForward = (Button) findViewById(R.id.wizard_button_forward);
        btBack = (Button) findViewById(R.id.wizard_button_back);
        btForward.setText(R.string.KEY_WIZARD_NEXT);
        btBack.setText(R.string.KEY_WIZARD_BACK);
        btForward.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int currentPage = mPager.getCurrentItem();
                if(currentPage == NUM_PAGES-1) {
                    finish();
                }
                mPager.setCurrentItem(++currentPage);
            }
        });
        btBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int currentPage = mPager.getCurrentItem();
                mPager.setCurrentItem(--currentPage);
            }
        });
        ourPageChangeListener.onPageSelected(0);
    }

    @Override
    public void onBackPressed() {
        if (mPager.getCurrentItem() == 0) {
            // If the user is currently looking at the first step, allow the system to handle the
            // Back button. This calls finish() on this activity and pops the back stack.
            super.onBackPressed();
        } else {
            // Otherwise, select the previous step.
            mPager.setCurrentItem(mPager.getCurrentItem() - 1);
        }
    }

    /**
     * Diese Klasse ist ein Listener, der die aehnlich bleibenden Layout-Elemente der Activity an die jeweilige Fragment/Seiten-Position anpasst
     * Ergaenzt den Adapter, da dieser nur fuers Laden der Fragmente zustaendig ist
     */
    private class OurPageChangeListener implements ViewPager.OnPageChangeListener{
        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {}

        //TODO: Switch-Case spaeter
        @Override
        public void onPageSelected(int position) {
            selectHeader(position);
            switch (position) {
                case 0:
                    btBack.setVisibility(View.INVISIBLE);
                    break;
                case 1:
                    btBack.setVisibility(View.VISIBLE);
                    break;
                case 2:
                    break;
                case 3:
                    btForward.setText(R.string.KEY_WIZARD_NEXT);
                    break;
                case 4:
                    btForward.setText(R.string.KEY_FINISH);
                    break;
                default:
                    break;
            }
        }
        @Override
        public void onPageScrollStateChanged(int state) { }
    }

    public void selectHeader(int position){
        drawPageIndicators(position);

        try {
            wizardTitle.setText(titles[position]);
        } catch (ArrayIndexOutOfBoundsException e){
            wizardTitle.setText(null);
        }
    }

    private class StatusCircle {
        float x;
        int color;
        float y = 30f;
        float r = 8f;

        public StatusCircle(int color, float x) {
            this.x = x + 20f;
            this.color = color;
        }
    }


    /**
     * A simple pager adapter that represents 5 ScreenSlidePageFragment objects, in
     * sequence.
     */
    private class ScreenSlidePagerAdapter extends FragmentPagerAdapter {

        public ScreenSlidePagerAdapter(FragmentManager fm) {
            super(fm);
        }
        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case 0:
                    return new ViewPagerFragment0();
                case 1:
                    return new ViewPagerFragment0();
                case 2:
                    return new ViewPagerFragment0();
                case 3:
                    return new ViewPagerFragment0();
                case 4:
                    return new ViewPagerFragment0();
                default:
                    return new ViewPagerFragment0();
            }
        }

        @Override
        public int getCount() {
            return NUM_PAGES;
        }
    }

    public void drawPageIndicators(int position){

        for (int i = 0; i < NUM_PAGES; i++) {
            int color = Color.GRAY;
            if (i == position)
                color = ContextCompat.getColor(SetupWizardActivity.this, R.color.maGreen);
            circles[i] = new StatusCircle(color, i * 70);
        }

        CompatUtils.setBackgroundDrawable(wizardStatusTrack, new Drawable() {

            @Override
            public void draw(Canvas canvas) {
                Paint p = new Paint();
                for (StatusCircle sc : circles) {
                    p.setColor(sc.color);
                    canvas.drawCircle(sc.x, sc.y, sc.r, p);
                }
            }

            @Override
            public int getOpacity() {
                return 0;
            }

            @Override
            public void setAlpha(int alpha) {
            }

            @Override
            public void setColorFilter(ColorFilter cf) {
            }

        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mPager.clearOnPageChangeListeners();
    }
}


