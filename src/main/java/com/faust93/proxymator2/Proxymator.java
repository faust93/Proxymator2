package com.faust93.proxymator2;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.PagerTabStrip;
import android.support.v4.view.ViewPager;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

public class Proxymator extends FragmentActivity {

    ViewPager mViewPager;
    TitleAdapter titleAdapter;
    MainFragment Main;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        mViewPager = (ViewPager) findViewById(R.id.viewpager);
        PagerTabStrip pagerTabStrip = (PagerTabStrip) findViewById(R.id.pagerTabStrip);
        pagerTabStrip.setDrawFullUnderline(true);
        pagerTabStrip.setTabIndicatorColor(Color.parseColor("#33B5E5"));

        Main = new MainFragment();

        titleAdapter = new TitleAdapter(getSupportFragmentManager());
        mViewPager.setAdapter(titleAdapter);
        mViewPager.setCurrentItem(0);


    }
    public class TitleAdapter extends FragmentPagerAdapter {
        private final String titles[] = new String[] {getResources().getString(R.string.main), getResources().getString(R.string.settings), getResources().getString(R.string.help) };
        private final Fragment frags[] = new Fragment[titles.length];

        public TitleAdapter(FragmentManager fm) {
            super(fm);
            frags[0] = Main;
            frags[1] = new SettingsFragment();
            frags[2] = new HelpFragment();
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return titles[position];
        }

        @Override
        public Fragment getItem(int position) {
            return frags[position];
        }

        @Override
        public int getCount() {
            return frags.length;
        }
    }

    @Override
    public void onRestoreInstanceState(Bundle SavedInstanceState){

    }
    @Override
    public void onSaveInstanceState(Bundle SavedInstanceState){

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);

        if(Main.get_rstate())
            menu.findItem(R.id.action_onoff).setIcon(android.R.drawable.ic_media_pause);
        else
            menu.findItem(R.id.action_onoff).setIcon(android.R.drawable.ic_media_play);

        return true;
    }

    // todo implement start/stop error checking
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
//        super.onOptionsItemSelected(item);
        switch(item.getItemId()){
            case R.id.action_onoff:

                if(Main.get_rstate()){
                    Main.doNotify(false,"");
                    Main.stop_proxy();
                    item.setIcon(android.R.drawable.ic_media_play);
                } else {
                    if(Main.start_proxy()){
                        Main.updatePrefs();
                        Main.doNotify(true,Main.getCurProfileName());
                        item.setIcon(android.R.drawable.ic_media_pause);
                    } else {
                        Toast.makeText(getApplicationContext(), getString(R.string.starting_error), Toast.LENGTH_SHORT).show();
                    }
                }
                break;
        }
        return true;
    }
}
