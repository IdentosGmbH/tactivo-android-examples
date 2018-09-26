package com.precisebiometrics.android.app.pbbiometricsexample;

import com.precisebiometrics.android.mtk.api.bio.PBBiometry;

import android.app.AlertDialog;
import android.support.v4.app.Fragment;
import android.util.Log;

/**
 * A fragment for the GUI for the examples that use a local database for the
 * fingerprint templates.
 */
public abstract class BiometricsFragment extends Fragment 
        implements Constants {
    
    /** The Biometrics API. */
    protected PBBiometry bio;

    /**
     * Override this function if your fragment need to perform any specific
     * action when the biometrics is activated/initialized.
     * 
     * @param bio The biometrics object used to performed biometric operations.
     */
    public void biometricsActivated(PBBiometry bio) {
    }
    
    /**
     * Override this function if your fragment need to perform any specific
     * action when the biometrics is deactivated/uninitialized.
     */
    public void biometricsDeactivated() {
    }
    
    /**
     * Publish an error to the user.
     * 
     * @param error The error message to publish.
     */
    protected void publishError(final String error) {
        showInfoDialog(error, R.string.error);
        Log.e(Constants.LOG_TAG, error);
    }

    /**
     * Show an information dialog to the user.
     * 
     * @param messageResId String resource Id for message to display.
     * @param titleResId String resource Id for the title of the dialog.
     */
    protected void showInfoDialog(int messageResId, 
                                  int titleResId) {
        showInfoDialog(getString(messageResId), titleResId);
    }

    /**
     * Show an information dialog to the user. If possible it is recommended
     * to use {@link #showInfoDialog(int, int)} in favor of this function.
     * 
     * @param message Message to display.
     * @param titleResId String resource Id for the title of the dialog.
     */
    protected void showInfoDialog(final String message, 
                                  final int titleResId) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(new Runnable() {
                
                @Override
                public void run() {
                AlertDialog.Builder builder = 
                    new AlertDialog.Builder(getActivity());
                builder.setMessage(message)
                       .setTitle(titleResId)
                       .setPositiveButton(R.string.ok, null);
                builder.create().show();
                }
            });
        }
    }
}
