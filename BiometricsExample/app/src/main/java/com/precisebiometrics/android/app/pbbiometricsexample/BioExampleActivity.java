package com.precisebiometrics.android.app.pbbiometricsexample;

import java.util.Locale;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.FragmentTransaction;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;

import com.precisebiometrics.android.mtk.api.PBInitializedCallback;
import com.precisebiometrics.android.mtk.api.bio.PBBiometry;
import com.precisebiometrics.android.mtk.api.bio.PBBiometryException;

/**
 * GUI for the Precise Biometrics Mobile Toolkit for Android 
 * biometrics example.
 * 
 * The app shows the use of the Mobile Toolkit Biometrics API for Android.
 * There are two different tabs showcasing:
 * <ul>
 * <li>Local Database<br/> 
 *     The functionality when using a local database for the fingerprint
 *     providing its own GUI.</li>
 * <li>Device Owner Database<br/>
 *     The functionality when utilizing the built-in Device Owner Database
 *     concept where the Tactivo Manager handles enrollment of fingerprint
 *     templates and the templates are stored within Tactivo Manager. In this
 *     case the default enrollment and verification GUI provided by the Tactivo
 *     Manager is used.
 *     
 * Note that the sample app focuses on showing the use of the Biometrics API
 * and that limited efforts has been made to create a stunning GUI. The GUI 
 * parts are intentionally made simplistic such that the source code relating
 * to the Biometrics API will be more prominent and not drown in irrelevant 
 * GUI code.
 */
public class BioExampleActivity extends FragmentActivity implements ActionBar.TabListener {

    /** The Biometrics API. */
    private PBBiometry bio;
    
    /** The number of fragments/tabs in the activity. */
    private final static int COUNT = 2;

    /** Holds the fragments */
    private BiometricsFragment fragments[] = new BiometricsFragment[COUNT]; 

    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link android.support.v4.app.FragmentPagerAdapter} derivative, which
     * will keep every loaded fragment in memory. If this becomes too memory
     * intensive, it may be best to switch to a
     * {@link android.support.v4.app.FragmentStatePagerAdapter}.
     */
    SectionsPagerAdapter mSectionsPagerAdapter;

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    ViewPager mViewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bio_example);

        // Set up the action bar.
        final ActionBar actionBar = getActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

        // Create the adapter that will return a fragment for each of the three
        // primary sections of the app.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        // When swiping between different sections, select the corresponding
        // tab. We can also use ActionBar.Tab#select() to do this if we have
        // a reference to the Tab.
        mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                actionBar.setSelectedNavigationItem(position);
            }
        });
        
        fragments[0] = new LocalDatabaseFragment();
        fragments[1] = new DeviceOwnerDatabaseFragment();

        // For each of the sections in the app, add a tab to the action bar.
        for (int i = 0; i < mSectionsPagerAdapter.getCount(); i++) {
            // Create a tab with text corresponding to the page title defined by
            // the adapter. Also specify this Activity object, which implements
            // the TabListener interface, as the callback (listener) for when
            // this tab is selected.
            actionBar.addTab(
                    actionBar.newTab()
                            .setText(mSectionsPagerAdapter.getPageTitle(i))
                            .setTabListener(this));
        }
        
        startBiometrics();
    }

    /**
     * Notifies the created fragments with information that the Biometrics API
     * are activated or deactivated.
     * 
     * @param activated True if Biometrics API is activated and false if 
     *              deactivated.
     */
    private void notifyBiometricsState(boolean activated) {
        for (BiometricsFragment frag: fragments) {
            if (activated) {
                frag.biometricsActivated(bio);
            } else {
                frag.biometricsDeactivated();
            }
        }
    }
    
    /**
     * Initializes the Biometrics API.
     */
    private void startBiometrics() {
        // Create the biometrics API object and provide
        // the callback that is informed when the initialization
        // is ready.
        bio = new PBBiometry(getApplicationContext(), 
                             new PBInitializedCallback() {
            @Override
            public void uninitialized() {
                // Note that with the setup in this example app, this will be
                // called every time we leave the app, e.g. when moving to 
                // the default GUI of the toolkit.
                notifyBiometricsState(false);
                String msg = "Biometrics API connection has been lost";
                Log.d(Constants.LOG_TAG, msg);
            }
            
            @Override
            public void initialized() {
                notifyBiometricsState(true);
            }
            
            @Override
            public void initializationFailed() {
                notifyBiometricsState(false);
                String error = 
                    "Unable to initialize the Biometrics API.\n" +
                    "Perhaps the Tactivo Manager is not installed.";
                showInfoDialog(error, "Error");
                Log.e(Constants.LOG_TAG, error);
            }
        });
    }
    
    /**
     * Closes the Biometrics API.
     */
    private void closeBiometrics() {
        try {
            bio.close();
        } catch (PBBiometryException e) {
            Log.e(Constants.LOG_TAG, 
                  "Failed closing biometrics on stop of activity", e);
        }
    }
    
    @Override
    protected void onDestroy() {
        closeBiometrics();
        super.onDestroy();
    }

    @Override
    protected void onStart() {
        // Register for notification of device attach and detach
        IntentFilter filter = 
            new IntentFilter("com.precisebiometrics.android.mtk.DEVICE_ATTACHED");
        filter.addAction("com.precisebiometrics.android.mtk.DEVICE_DETACHED");        
        this.registerReceiver(AccessoryListener.getInstance(), filter);
        super.onStart();
    }

    @Override
    protected void onStop() {
        // Unregister for notification of device attach and detach such that we
        // do not get these when the application is in the background.
        // Note however that applications could want to know of device connect
        // and disconnect also when in the background.
        this.unregisterReceiver(AccessoryListener.getInstance());
        super.onStop();
    }
    
    /**
     * Show an information dialog to the user.
     * 
     * @param message Message to display.
     * @param title Title of the dialog.
     */
    protected void showInfoDialog(final String message, final String title) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
            AlertDialog.Builder builder = 
                new AlertDialog.Builder(BioExampleActivity.this);
            builder.setMessage(message)
                   .setTitle(title)
                   .setPositiveButton("OK", null);
            builder.create().show();
            }
        });
    }

    @Override
    public void onTabSelected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
        // When the given tab is selected, switch to the corresponding page in
        // the ViewPager.
        mViewPager.setCurrentItem(tab.getPosition());
    }

    @Override
    public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
    }

    @Override
    public void onTabReselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {
        
        /**
         * Constructor.
         * 
         * @param fm The fragment manager to use.
         */
        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            return fragments[position];
        }

        @Override
        public int getCount() {
            return COUNT;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            Locale l = Locale.getDefault();
            switch (position) {
                case 0:
                    return getString(R.string.title_local).toUpperCase(l);
                case 1:
                    return getString(R.string.title_device).toUpperCase(l);
            }
            return null;
        }
    }
}
