package com.precisebiometrics.android.app.pbbiometricsexample;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
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
 * Enrollment dialog.
 * This is a reference example of a {@link PBBiometryGUI} to handle
 * the events gotten from an enrollment process in order to show a user
 * the swiped fingerprint image together with instructions and other
 * information.
 */
public class EnrollDialog extends Dialog implements PBBiometryGUI {
    
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
    
    /** Image views for all the swiped samples in the enrollment */
    private ImageView thumbs[];
    /** Text view showing the number for the sample */
    private TextView thumbNumbers[];
    
    /** Holds the sample number. Increased by one for each "chosen image" */
    private int sampleNum = -1;
    
    /**
     * Creates an enroll dialog that shows the GUI used for enrollment.
     * 
     * @param activity The context of the dialog.
     * @param bio The biometric API, that can be used for cancellation.
     */
    public EnrollDialog(Activity activity, PBBiometry bio) {
        super(activity);
        this.activity = activity;
        this.bio = bio;
        
        setCancelable(false);
        getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_enroll);

        instructionText = (TextView)findViewById(R.id.instructionText);
        errorText = (TextView)findViewById(R.id.errorText);
        fpImage = (ImageView)findViewById(R.id.fingerprint);

        thumbs = new ImageView[3];
        thumbs[0] = (ImageView)findViewById(R.id.thumb1);
        thumbs[1] = (ImageView)findViewById(R.id.thumb2);
        thumbs[2] = (ImageView)findViewById(R.id.thumb3);
        thumbNumbers = new TextView[3];
        thumbNumbers[0] = (TextView)findViewById(R.id.thumb1Number);
        thumbNumbers[1] = (TextView)findViewById(R.id.thumb2Number);
        thumbNumbers[2] = (TextView)findViewById(R.id.thumb3Number);
        
        Button cancelButton = (Button)findViewById(R.id.cancelButton);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    EnrollDialog.this.bio.cancel();
                } catch (PBBiometryException e) {
                    // If cancellation fails it is a problem that you cannot
                    // abort the verification. What to do here probably depends
                    // on the situation. In the example, we do nothing which
                    // will give that the dialog is still here.
                }
                // The dialog will be dismissed by the result of the call
                // to PBBiometry.enrollFinger() in LocalDatabaseFragment
            }
        });
    }
    
    /**
     * Create a bitmap with rounded corners from a given bitmap. It is possible
     * to create rounded corners with arbitrary radius.
     * 
     * @param bitmap The source bitmap.
     * @param pixels Number of pixels to make radius (i.e. the radius).
     * @return Bitmap with rounded corners and border.
     */
    public static Bitmap getRoundedCornerBitmap(Bitmap bitmap, 
                                                int pixels) {
        Bitmap output = Bitmap.createBitmap(bitmap.getWidth(), 
                                            bitmap.getHeight(), 
                                            Config.ARGB_8888);
        Canvas canvas = new Canvas(output);
        
        final int color = 0xffffffff;
        final Paint paint = new Paint();
        final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
        final RectF rectF = new RectF(rect);
        final float roundPx = pixels;
        
        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(color);
        canvas.drawRoundRect(rectF, roundPx, roundPx, paint);
        
        paint.setXfermode(new PorterDuffXfermode(Mode.SRC_IN));
        canvas.drawBitmap(bitmap, rect, rect, paint);
        return output;
    }

    @Override
    public void displayChosenImage(final PBBiometryImage image)
            throws PBBiometryException {
        sampleNum++;
        
        activity.runOnUiThread(new Runnable() {
            
            @Override
            public void run() {
                
                Bitmap bitmap = getRoundedCornerBitmap(image.getBitmap(), 20);
                fpImage.setImageBitmap(bitmap);
                thumbs[sampleNum].setImageBitmap(bitmap);
                thumbNumbers[sampleNum].setVisibility(View.INVISIBLE);
                // Clear any possible error displayed since we now got a
                // valid image.
                errorText.setText("");
            }
        });
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
                    if (sampleNum > -1) {
                        // We have gotten our first sample and is
                        // going for our second or more.
                        instructionText.setText(R.string.swipe_finger_again);
                    } else {
                        // First sample swipe
                        instructionText.setText(R.string.swipe_finger);
                    }
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
                fpImage.setImageBitmap(image.getBitmap());
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
