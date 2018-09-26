package com.precisebiometrics.android.app.pbbiometricsexample;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Bitmap;
import android.graphics.drawable.ColorDrawable;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.precisebiometrics.android.mtk.api.bio.PBBiometry;
import com.precisebiometrics.android.mtk.api.bio.PBBiometryException;
import com.precisebiometrics.android.mtk.api.bio.PBBiometryGUI;
import com.precisebiometrics.android.mtk.biometrics.PBBiometryFinger;
import com.precisebiometrics.android.mtk.biometrics.PBBiometryImage;

/**
 * Verification dialog.
 * This is a reference example of a {@link PBBiometryGUI} to handle
 * the events gotten from a verification process in order to show a user
 * the swiped fingerprint image together with instructions and other
 * information.
 */
public class VerifyDialog extends Dialog implements PBBiometryGUI {
    
    /** Text view for displaying swipe instructions. */
    private TextView instructionText;
    /** 
     * Text view for displaying error information, such as too small area 
     * etc
     */
    private TextView errorText;
    /** Image view that shows the live swiped fingerprint image */
    private ImageView fpImage;
    
    /** Biometrics API that can be used for cancellation */
    private PBBiometry bio;
    /** Activity that is the owner/displayer of this dialog */
    private Activity activity;
    
    /**
     * Creates a verify dialog that shows the GUI used for verification.
     * 
     * @param activity The context of the dialog.
     * @param bio The biometric API, that can be used for cancellation.
     */
    public VerifyDialog(Activity activity, PBBiometry bio) {
        super(activity);
        this.activity = activity;
        this.bio = bio;
        
        setCancelable(false);
        getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_verify);

        instructionText = (TextView)findViewById(R.id.instructionText);
        errorText = (TextView)findViewById(R.id.errorText);
        fpImage = (ImageView)findViewById(R.id.fingerprint);

        Button cancelButton = (Button)findViewById(R.id.cancelButton);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    VerifyDialog.this.bio.cancel();
                } catch (PBBiometryException e) {
                    // If cancellation fails it is a problem that you cannot
                    // abort the verification. What to do here probably depends
                    // on the situation. In the example, we do nothing which
                    // will give that the dialog is still here.
                }
                // The dialog will be dismissed by the result of the call
                // to PBBiometry.verifyFingers() in LocalDatabaseFragment
            }
        });
    }
    
    @Override
    public void displayChosenImage(PBBiometryImage image)
            throws PBBiometryException {
        displayImage(image);
    }

    @Override
    public void displayEvent(final PBEvent event, 
                             final PBBiometryFinger finger)
            throws PBBiometryException {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                switch (event) {
                case PBEventAlertAreaTooSmall:
                    errorText.setText(R.string.area_too_small);
                    break;
                case PBEventAlertFingerRejected:
                    errorText.setText(R.string.no_match);
                    break;
                case PBEventAlertImageCaptured:
                    // We remove the swipe instruction text here since it
                    // means that a image has been captured and it will be
                    // processed either for enrollment or verification.
                    // If it was not good enough a new PBEventPromptSwipeFinger
                    // will be issued.
                    instructionText.setText("");
                    break;
                case PBEventAlertQualityTooBad:
                    errorText.setText(R.string.quality_too_low);
                    break;
                case PBEventAlertSwipeTooFast:
                    errorText.setText(R.string.swipe_too_fast);
                    break;
                case PBEventAlertTemplateEnrolled:
                    break;
                case PBEventAlertTemplateExtracted:
                    break;
                case PBEventPromptSwipeFinger:
                    // Which finger that is to be swiped could be extracted
                    // from the finger input parameter and affect the string
                    // presented to the user.
                    instructionText.setText(R.string.swipe_finger);
                    break;
                }
            }
        });
    }

    @Override
    public void displayImage(final PBBiometryImage image) 
            throws PBBiometryException {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Bitmap bitmap = 
                    EnrollDialog.getRoundedCornerBitmap(image.getBitmap(), 20);
                fpImage.setImageBitmap(bitmap);
            }
        });
    }

    @Override
    public void displayQuality(int imageQuality, int area, 
                               int imageQualityThreshold, int areaThreshold)
            throws PBBiometryException {
        // We skip showing any reported quality to the user
    }
}
