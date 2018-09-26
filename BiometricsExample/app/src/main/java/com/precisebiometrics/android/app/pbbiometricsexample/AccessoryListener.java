package com.precisebiometrics.android.app.pbbiometricsexample;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

/**
 * Listener for attach and detach of supported devices.
 * 
 * This class simply shows how to get notified of when a supported
 * device is attached or detached from the Android unit.
 * In this simple example it is shown simply by displaying a toast.
 */
public class AccessoryListener extends BroadcastReceiver {

    /** Sole singleton instance. */
    private static AccessoryListener instance = null;
    
    /**
     * Gets the sole instance of this class.
     * 
     * @return Sole instance of this class.
     */
    public static AccessoryListener getInstance() {
        if (instance == null) {
            instance = new AccessoryListener();
        }
        
        return instance;
    }
    
    /**
     * Hidden default constructor for singleton.
     */
    private AccessoryListener() {
    }
    
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(Constants.LOG_TAG, 
              "BroadcastReceiver got intent: " + intent.getAction());
        int id = intent.getIntExtra("id", -1);
        int type = intent.getIntExtra("type", -1);
        String typeName = "UNKNOWN TYPE";
        switch (type) {
        case 1: typeName = "Smartcard"; break;
        case 2: typeName = "Biometric sensor"; break;
        case 3: typeName = "Combined Smartcard and biometric sensor"; break;
        }
        String action = " device attached";
        if (intent.getAction().equalsIgnoreCase("com.precisebiometrics.android.mtk.DEVICE_DETACHED")) {
            action = " device detached";
        }
        Toast.makeText(context, typeName + action + " (" + id + ")", 
                       Toast.LENGTH_SHORT).show();
    }

}
