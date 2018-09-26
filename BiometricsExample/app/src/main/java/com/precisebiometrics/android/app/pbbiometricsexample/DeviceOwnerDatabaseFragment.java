package com.precisebiometrics.android.app.pbbiometricsexample;

import java.util.List;

import com.precisebiometrics.android.mtk.api.bio.PBBiometry;
import com.precisebiometrics.android.mtk.api.bio.PBBiometryException;
import com.precisebiometrics.android.mtk.biometrics.PBBiometryTrust;
import com.precisebiometrics.android.mtk.biometrics.PBBiometryVerifyConfig;
import com.precisebiometrics.android.mtk.biometrics.PBBiometryVerifyConfig.PBFalseAcceptRate;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

/**
 * A fragment for the GUI for the examples that use the Device Owner Database
 * for storing the fingerprint templates.
 */
public class DeviceOwnerDatabaseFragment extends BiometricsFragment {

    /** 
     * A trust Id randomly selected to be the one trust this application
     * will hold to the Device Owner Database. 
     */
    private static final int TRUST_ID = 47;

    /** Holds the button "Enroll Trust" */
    private Button enrollTrustButton;
    /** Holds the button "Delete Trust" */
    private Button deleteTrustButton;
    /** Holds the button "List Trusts" */
    private Button listTrustsButton;
    /** Holds the button "Verify" */
    private Button verifyButton;
    /** Holds the button "Enroll in Manager" */
    private Button enrollManagerButton;
    /** Holds the text are to log in */
    private TextView logText;
    
    /** Tells if the interaction widgets should be enabled or not. */
    private boolean widgetsEnabled = false;
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_device_owner_db, 
                                         container, false);

        enrollTrustButton = (Button) rootView.findViewById(R.id.enrollTrustButton);
        deleteTrustButton = (Button) rootView.findViewById(R.id.deleteTrustButton);
        listTrustsButton = (Button) rootView.findViewById(R.id.listTrustsButton);
        verifyButton = (Button) rootView.findViewById(R.id.verifyDeviceOwnerButton);
        enrollManagerButton = (Button) rootView.findViewById(R.id.enrollManagerButton);
        logText = (TextView)rootView.findViewById(R.id.dodbTextArea);
        // All widgets disabled until the Biometrics API is ready.
        setWidgetsEnabled();
        
        enrollTrustButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                enrollTrust(v);
            }
        });

        deleteTrustButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deleteTrust(v);
            }
        });

        listTrustsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listTrusts(v);
            }
        });

        verifyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                verifyDeviceOwner(v);
            }
        });

        enrollManagerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                enrollManager(v);
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
     * Enables/disables the widgets in this fragment.
     * It is enabled/disabled based on the value of the {@link #widgetsEnabled}
     * variable.
     */
    private void setWidgetsEnabled() {
        if (enrollTrustButton != null) {
            enrollTrustButton.setEnabled(widgetsEnabled);
        }
        if (deleteTrustButton != null) {
            deleteTrustButton.setEnabled(widgetsEnabled);
        }
        if (listTrustsButton != null) {
            listTrustsButton.setEnabled(widgetsEnabled);
        }
        if (verifyButton != null) {
            verifyButton.setEnabled(widgetsEnabled);
        }
        if (enrollManagerButton != null) {
            enrollManagerButton.setEnabled(widgetsEnabled);
        }
    }
    
    /**
     * Called when the enroll trust button is pressed in the Device Owner 
     * Database section.
     * Enrolls and creates a trust between this application and the Device
     * Owner Database. The trust will be returned on successful verifications.
     * 
     * @param view The button that was pressed.
     */
    public void enrollTrust(View view) {
        if (bio != null) {
            new Thread(new Runnable() {
                
                @Override
                public void run() {
                    try {
                        bio.enrollOwnerTrust(
                                             // Use our selected Id
                                             TRUST_ID, 
                                             // Nice display name for the app
                                             "PB Bio Example", 
                                             // We are not interested in any 
                                             // extra data stored with the trust
                                             // in this simple example
                                             null, 
                                             // We select a security level that
                                             // is equal to the default 
                                             // verification security level
                                             PBFalseAcceptRate.PBFalseAcceptRate10000);
                        
                        showInfoDialog(R.string.trust_enroll_successful, 
                                       R.string.info);
                    } catch (PBBiometryException e) {
                        publishError(e.getErrorDisplayName());
                    }
                }
            }).start();
        }
    }

    /**
     * Called when the delete trust button is pressed in the Device Owner 
     * Database section.
     * Deletes a previously enrolled trust. If the trust is not enrolled an
     * error message will be displayed.
     * 
     * @param view The button that was pressed.
     */
    public void deleteTrust(View view) {
        if (bio != null) {
            new Thread(new Runnable() {
                
                @Override
                public void run() {
                    try {
                        bio.removeOwnerTrust(TRUST_ID);
                        
                        showInfoDialog(R.string.trust_delete_successful, 
                                       R.string.info);
                    } catch (PBBiometryException e) {
                        publishError(e.getErrorDisplayName());
                    }
                }
            }).start();
        }
    }

    /**
     * Called when the list trusts button is pressed in the Device Owner 
     * Database section.
     * Gets the currently enrolled trusts and displays the in a list in the GUI.
     * 
     * @param view The button that was pressed.
     */
    public void listTrusts(View view) {
        if (bio != null) {
            new Thread(new Runnable() {
                
                @Override
                public void run() {
                    try {
                        final List<PBBiometryTrust> trusts = 
                            bio.getEnrolledTrusts();
                        
                        getActivity().runOnUiThread(new Runnable() {
                            
                            @Override
                            public void run() {
                                if (trusts.size() == 0) {
                                    logText.append("\n> " + getString(R.string.no_trusts_enrolled));
                                } else {
                                    logText.append("\n> " + getString(R.string.list_of_trusts));
                                    for (PBBiometryTrust trust: trusts) {
                                        logText.append("\n>   " + trust.getId() + 
                                                       " - " + 
                                                       getString((trust.isValid()?R.string.VALID:R.string.INVALID)) + 
                                                       " - " + 
                                                       trust.getTrusteeName() + 
                                                       " {" + 
                                                       trust.getFalseAcceptRate() +
                                                       "}");
                                    }
                                }
                            }
                        });
                        
                    } catch (PBBiometryException e) {
                        publishError(e.getErrorDisplayName());
                    }
                }
            }).start();
        }
    }

    /**
     * Called when the verify button is pressed in the Device Owner 
     * Database section.
     * Performs a verification against all the templates that are enrolled in
     * the Device Owner Database.
     * Enrollment is performed in the Tactivo Manager app. 
     * 
     * @param view The button that was pressed.
     */
    public void verifyDeviceOwner(View view) {
        if (bio != null) {
            new Thread(new Runnable() {
                
                @Override
                public void run() {
                    PBBiometryVerifyConfig config = new PBBiometryVerifyConfig();
                    try {
                        // Perform verification and receive the trusts
                        List<PBBiometryTrust> verTrusts =
                            bio.verifyOwner(
                                            // We use the default GUI
                                            null,
                                            // Default configuration is used
                                            config, 
                                            // We use our own GUI so GUI
                                            // configuration is useless here
                                            null);
                        
                        // No exception, so operation was successful
                        // But if the returned trusts was null it means that
                        // the finger did not match.
                        if (verTrusts == null) {
                            showInfoDialog(R.string.no_match, R.string.info);
                        } else {
                            // Here we need to check the trust
                            if (verTrusts.size() == 0) {
                                // No trusts, have we not enrolled any yet?
                                showInfoDialog(R.string.ver_but_no_trusts, 
                                               R.string.info);
                            } else {
                                for (PBBiometryTrust trust: verTrusts) {
                                    // Check if our expected trust exists
                                    if (trust.getId() == TRUST_ID) {
                                        // If the trust is granted it means that
                                        // it is still valid and has been
                                        // verified at a correct security level
                                        if (trust.isGranted()) {
                                            // This case should be the only that
                                            // is considered as really 
                                            // successful in the device owner 
                                            // and trusts scenario.
                                            showInfoDialog(R.string.verify_successful, 
                                                           R.string.info);
                                        } 
                                        // If it is only valid it means the 
                                        // verification security level was too
                                        // low and it need to be raised in the
                                        // PBBiometryVerifyConfig. Does not 
                                        // happen in this example since enrolled
                                        // trust is at a lower or equal level as
                                        // the verification process one.
                                        else if (trust.isValid()) {
                                            showInfoDialog(R.string.ver_but_sec_level, 
                                                           R.string.info);
                                        }
                                        // If it is not even valid it means that
                                        // the Device Owner Database has been
                                        // altered (new fingers enrolled or all
                                        // fingers deleted etc) such that the
                                        // trust contract between this app
                                        // and the Device Owner Database has
                                        // been broken.
                                        else {
                                            showInfoDialog(R.string.ver_but_trust_broken, 
                                                           R.string.error);
                                        }
                                    }
                                }
                            }
                        }
                    } catch (PBBiometryException e) {
                        publishError(e.getErrorDisplayName());
                    }
                }
            }).start();
        }
    }
    
    /**
     * Called when the enroll in manager button is pressed in the Device Owner 
     * Database section.
     * Redirects to the Tactivo Managers manage fingers section where enrollment
     * or template deletes against the Device Owner Database can be performed. 
     * 
     * @param view The button that was pressed.
     */
    public void enrollManager(View view) {
        PBBiometry.manageDeviceOwnerFingers(getActivity());
    }

}
