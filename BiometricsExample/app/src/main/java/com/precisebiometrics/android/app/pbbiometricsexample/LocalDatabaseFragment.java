package com.precisebiometrics.android.app.pbbiometricsexample;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RadioGroup;

import com.precisebiometrics.android.mtk.api.bio.PBBiometry;
import com.precisebiometrics.android.mtk.api.bio.PBBiometryException;
import com.precisebiometrics.android.mtk.biometrics.BiometryConstants;
import com.precisebiometrics.android.mtk.biometrics.PBBiometryEnrollConfig;
import com.precisebiometrics.android.mtk.biometrics.PBBiometryFinger;
import com.precisebiometrics.android.mtk.biometrics.PBBiometryFinger.PBFingerPosition;
import com.precisebiometrics.android.mtk.biometrics.PBBiometryUser;
import com.precisebiometrics.android.mtk.biometrics.PBBiometryVerifyConfig;

/**
 * A fragment for the GUI for the examples that use a local database for the
 * fingerprint templates.
 */
public class LocalDatabaseFragment extends BiometricsFragment {

    /** Holds the button "Enroll Finger" */
    private Button enrollFingerButton;
    /** Holds the button "Delete Finger" */
    private Button deleteFingerButton;
    /** Holds the button "Verify" */
    private Button verifyButton;
    /** Holds the button "Practice" */
    private Button practiceButton;
    /** Holds the radio group for finger selection */
    private RadioGroup radioGroup;
    
    /** Tells if the interaction widgets should be enabled or not. */
    private boolean widgetsEnabled = false;
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_local_db, 
                                         container, false);

        enrollFingerButton = (Button) rootView.findViewById(R.id.enrollButton);
        deleteFingerButton = (Button) rootView.findViewById(R.id.deleteButton);
        verifyButton = (Button) rootView.findViewById(R.id.verifyButton);
        practiceButton = (Button) rootView.findViewById(R.id.practiceButton);
        radioGroup = (RadioGroup) rootView.findViewById(R.id.radioGroup);
        setWidgetsEnabled();
        
        verifyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                verifyLocal(v);
            }
        });

        practiceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                practice(v);
            }
        });

        enrollFingerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                enrollLocal(v);
            }
        });

        deleteFingerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deleteLocal(v);
            }
        });

        return rootView;
    }

    @Override
    public void biometricsActivated(PBBiometry bio) {
        this.bio = bio;
        widgetsEnabled = true;
        setWidgetsEnabled();
    }

    @Override
    public void biometricsDeactivated() {
        bio = null;
        widgetsEnabled = false;
        setWidgetsEnabled();
    }

    /**
     * Called when the delete button is pressed in the Local Database section.
     * Deletes the template for the finger that is selected in the GUI. If
     * that finger is not enrolled an error will be displayed. 
     * 
     * @param view The button that was pressed.
     */
    public void deleteLocal(View view) {
        if (bio != null) {
            try {
                LocalDatabase.getInstance().deleteTemplate(getSelectedFinger());
                showInfoDialog(R.string.delete_successful, R.string.info);
            } catch (PBBiometryException e) {
                publishError(e.getErrorDisplayName());
            }
        }
    }
    
    /**
     * Called when the enroll button is pressed in the Local Database section.
     * Performs a one finger image enrollment of the finger that is selected
     * in the GUI. Enrolled template is stored in the locally implemented
     * database. 
     * 
     * @param view The button that was pressed.
     */
    public void enrollLocal(View view) {
        if (bio != null) {
            final EnrollDialog dialog = new EnrollDialog(getActivity(), 
                                                         bio);
            dialog.show();
            new Thread(new Runnable() {
                
                @Override
                public void run() {
                    PBBiometryEnrollConfig config = 
                        new PBBiometryEnrollConfig();
                    try {
                        bio.enrollFinger(
                                         // Enroll the finger selected in the
                                         // GUI
                                         getSelectedFinger(), 
                                         // Use the local database for
                                         // template storage
                                         LocalDatabase.getInstance(), 
                                         // The GUI functionality is 
                                         // handled by the enroll dialog.
                                         dialog,
                                         // We use the recommended 3 sample 
                                         // enrollment. The GUI is also created
                                         // to show the 3 samples, see 
                                         // EnrollDialog
                                         3, 
                                         // Default configuration is used
                                         config, 
                                         // We use our own GUI so GUI
                                         // configuration is useless here
                                         null);
                        showInfoDialog(R.string.enroll_successful, 
                                       R.string.info);
                    } catch (PBBiometryException e) {
                        publishError(e.getErrorDisplayName());
                    } finally {
                        if (dialog != null) {
                            dialog.dismiss();
                        }
                    }
                }
            }).start();
        }
    }

    /**
     * Called when the swipe practice button is pressed in the Local Database 
     * section.
     * Performs image capture until the user presses the Exit button on the
     * GUI dialog. 
     * 
     * @param view The button that was pressed.
     */
    private void practice(View view) {
        if (bio != null) {
            final PracticeDialog dialog = new PracticeDialog(getActivity(), 
                                                             bio);
            dialog.show();
            new Thread(new Runnable() {
                
                @Override
                public void run() {
                    try {
                        bio.captureImages(// The GUI functionality is 
                                          // handled by the practice dialog.
                                          dialog, 
                                          null);
                        showInfoDialog(R.string.practice_complete, 
                                       R.string.info);
                    } catch (PBBiometryException e) {
                        // Cancellation is the normal way to end biometric image
                        // capture
                        if (e.getBiometryError() != 
                                BiometryConstants.STATUS_CANCELLED) {
                            publishError(e.getErrorDisplayName());
                        }
                    } finally {
                        if (dialog != null) {
                            dialog.dismiss();
                        }
                    }
                }
            }).start();
        }
    }
    
    /**
     * Called when the verify button is pressed in the Local Database section.
     * Performs a verification against all the templates that are enrolled in
     * the locally implemented database. 
     * 
     * @param view The button that was pressed.
     */
    public void verifyLocal(View view) {
        if (bio != null) {
            final VerifyDialog dialog = new VerifyDialog(getActivity(), 
                                                         bio);
            dialog.show();
            new Thread(new Runnable() {
                
                @Override
                public void run() {
                    PBBiometryVerifyConfig config = new PBBiometryVerifyConfig();
                    try {
                        LocalDatabase db = LocalDatabase.getInstance();
                        
                        
                        
                        // Perform verification and receive the verified finger
                        PBBiometryFinger verFinger =
                            bio.verifyFingers(
                                              // We wants to verify against all
                                              // enrolled fingers
                                              db.getEnrolledFingers(),
                                              // Use the local database for
                                              // template storage
                                              db,
                                              // The GUI functionality is 
                                              // handled by the verify dialog.
                                              dialog,
                                              // We just standard verifier which
                                              // is most common
                                              null,
                                              // Default configuration is used
                                              config, 
                                              // We use our own GUI so GUI
                                              // configuration is useless here
                                              null);
                        
                        // No exception, so operation was successful
                        // But if the returned finger was null it means that
                        // the finger did not match.
                        if (verFinger == null) {
                            showInfoDialog(R.string.no_match, 
                                           R.string.info);
                        } else {
                            int fingerNum = 1;
                            if (verFinger.getPosition() != 
                                    PBFingerPosition.PBFingerPositionLeftIndex) {
                                fingerNum = 2;
                            }
                            showInfoDialog(getString(R.string.finger_verified) +
                                           " " + fingerNum, 
                                           R.string.info);
                        }
                    } catch (PBBiometryException e) {
                        publishError(e.getErrorDisplayName());
                    } finally {
                        if (dialog != null) {
                            dialog.dismiss();
                        }
                    }
                }
            }).start();
        }
    }

    /**
     * Get the finger that is selected in the GUI (in LocalDatabaseFragment).
     * 
     * Note! that we here has chosen to represent the restricted two finger use
     * in the example app with exactly the left index and left middle fingers.
     * 
     * @return The selected finger.
     */
    private PBBiometryFinger getSelectedFinger() {
        int id = radioGroup.getCheckedRadioButtonId();
        // The finger positions here are simply randomly chosen for the example
        // app only.
        if (id == R.id.finger1) {
            return new PBBiometryFinger(PBFingerPosition.PBFingerPositionLeftIndex, 
                                        new PBBiometryUser(EXAMPLE_USER_ID));
        } else {
            return new PBBiometryFinger(PBFingerPosition.PBFingerPositionLeftMiddle, 
                                        new PBBiometryUser(EXAMPLE_USER_ID));
        }
    }

    /**
     * Enables/disables the widgets in this fragment.
     * It is enabled/disabled based on the value of the {@link #widgetsEnabled}
     * variable.
     */
    private void setWidgetsEnabled() {
        if (enrollFingerButton != null) {
            enrollFingerButton.setEnabled(widgetsEnabled);
        }
        if (deleteFingerButton != null) {
            deleteFingerButton.setEnabled(widgetsEnabled);
        }
        if (verifyButton != null) {
            verifyButton.setEnabled(widgetsEnabled);
        }
        if (radioGroup != null) {
            radioGroup.setEnabled(widgetsEnabled);
        }
    }
}
