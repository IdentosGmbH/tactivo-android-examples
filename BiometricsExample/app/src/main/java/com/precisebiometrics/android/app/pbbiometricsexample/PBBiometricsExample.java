package com.precisebiometrics.android.app.pbbiometricsexample;

import android.app.Application;
import android.content.Context;

/**
 * Application class for the PB Biometrics Example. The class is responsible for
 * creating the needed singleton instances in the application that is in need
 * of the application context. It also registers the broadcast receiver that
 * will be notified of when devices are attached/detached.
 */
public class PBBiometricsExample extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        initSingletons(this);
    }

    /**
     * Initiates all the singletons in the application with the 
     * given application context.
     * 
     * @param ctx The application context.
     */
    public void initSingletons(Context ctx) {
        LocalDatabase.initInstance(this);
    }

}
