package com.precisebiometrics.android.app.pbbiometricsexample;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StreamCorruptedException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.util.Log;

import com.precisebiometrics.android.mtk.api.bio.PBBiometryDatabase;
import com.precisebiometrics.android.mtk.api.bio.PBBiometryException;
import com.precisebiometrics.android.mtk.biometrics.BiometryConstants;
import com.precisebiometrics.android.mtk.biometrics.PBBiometryFinger;
import com.precisebiometrics.android.mtk.biometrics.PBBiometryFinger.PBFingerPosition;
import com.precisebiometrics.android.mtk.biometrics.PBBiometryTemplate;
import com.precisebiometrics.android.mtk.biometrics.PBBiometryTemplate.PBBiometryTemplateType;
import com.precisebiometrics.android.mtk.biometrics.PBBiometryUser;

/**
 * Biometrics API database implementation that can be provided as source for
 * biometric operations.
 */
public class LocalDatabase implements PBBiometryDatabase, Constants {

    /** Singleton instance. */
    private static LocalDatabase instance;
    /** The application context. */
    private Context ctx;

    /** The name of the file where the database is stored. */
    private static final String DATA_FILE_NAME = "DbContent.dat";
    
    /** Holds the templates in the database. */
    private Map<PBBiometryFinger, PBBiometryTemplate> templates = 
        new HashMap<PBBiometryFinger, PBBiometryTemplate>();
    
    /**
     * Initializes the singleton. Call this from the
     * per application onCreate initializer so that the
     * object is guaranteed to persist at all times.
     *
     * @param ctx Application context
     */
    public static void initInstance(Context ctx) {
        instance = new LocalDatabase(ctx);
    }

    /**
     * Return the singleton object. It is assumed that the singleton already has
     * been initialized using initInstance.
     * 
     * @return The database singleton or null in case of lacking initialization,
     *         which should be considered as a programming error.
     */
    public static LocalDatabase getInstance() {
        return instance;
    }

    /**
     * Hidden constructor as for the singleton pattern.
     * 
     * @param ctx Application context.
     */
    private LocalDatabase(Context ctx) {
        this.ctx = ctx;
        readDatabase();
    }

    /**
     * Writes the current content in the database to persistent storage
     * 
     * @return True if successful, otherwise false.
     */
    private boolean writeDatabase() {
        try {
            Log.d(LOG_TAG, "Writing LocalDatabase");
            FileOutputStream fos  =
                ctx.openFileOutput(DATA_FILE_NAME, Context.MODE_PRIVATE);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            
            // Write the number of entries that is stored.
            oos.writeInt(templates.size()); // int
            Log.v(LOG_TAG, 
                       "Writing LocalDatabase, nrOfEntries: " + 
                       templates.size());
            
            // Write all the data for each of the entries
            for (PBBiometryFinger finger: templates.keySet()) {
                PBBiometryTemplate templ = templates.get(finger);
                Log.v(LOG_TAG, 
                           "Writing LocalDatabase, entry: " + 
                           finger.getPosition() + "/" + 
                           finger.getUser().getId() + "/" + 
                           templ.getTemplateType());
                
                PBFingerPosition valPos = finger.getPosition();
                oos.writeObject(valPos); // int
                PBBiometryUser user = finger.getUser();
                int val = user.getId();
                oos.writeInt(val); // int
                PBBiometryTemplateType valType = templ.getTemplateType();
                oos.writeObject(valType); // int
                byte[] data = templ.getData();
                oos.writeInt(data.length); // int
                oos.write(data); // byte[]
            }
            oos.flush();
            oos.close();
            return true;
        } catch (FileNotFoundException e) {
            Log.e(LOG_TAG, "Exception thrown", e);
            return false;
        } catch (IOException e) {
            Log.e(LOG_TAG, "Exception thrown", e);
            return false;
        }
    }
    
    /**
     * Reads the current content in the database from persistent storage
     * @return True if successful, otherwise false.
     */
    private boolean readDatabase() {
        FileInputStream fis;
        ObjectInputStream ois;
        
        try {
            Log.d(LOG_TAG, "Reading LocalDatabase");
            fis = ctx.openFileInput(DATA_FILE_NAME);
            ois = new ObjectInputStream(fis);
            
            templates.clear();
            int numEntries = ois.readInt();
            Log.v(LOG_TAG, 
                       "Reading LocalDatabase, nrOfEntries: " + numEntries);
            for (int i= 0; i < numEntries; i++) {
                PBFingerPosition fingerPosition = 
                    (PBFingerPosition)ois.readObject();
                int userId = ois.readInt();
                PBBiometryUser user = new PBBiometryUser(userId);
                PBBiometryFinger finger = 
                    new PBBiometryFinger(fingerPosition, user);
                PBBiometryTemplateType templType = 
                    (PBBiometryTemplateType)ois.readObject();
                int templLength = ois.readInt();
                byte[] templData = new byte[templLength];
                ois.read(templData);
                PBBiometryTemplate templ = new PBBiometryTemplate(templData, 
                                                                  templType);
                templates.put(finger, templ);
            }
            ois.close();
            return true;
        } catch (FileNotFoundException e1) {
            // File does not exist. Could be first time use.
            // Ignore
            return true;
        } catch (StreamCorruptedException e) {
            Log.e(LOG_TAG, "Exception thrown", e);
            return false;
        } catch (IOException e) {
            Log.e(LOG_TAG, "Exception thrown", e);
            return false;
        } catch (ClassNotFoundException e) {
            Log.e(LOG_TAG, "Exception thrown", e);
            return false;
        } 
    }
    
    @Override
    public boolean deleteTemplate(PBBiometryFinger finger) 
            throws PBBiometryException {
        Log.v(LOG_TAG, "Delete template called for finger: " + 
                   finger.getPosition());
        // Remove the template from the memory
        PBBiometryTemplate templ = templates.remove(finger);
        
        if (templ == null) {
            // Finger was not enrolled
            throw new PBBiometryException(BiometryConstants.STATUS_NOT_FOUND);
        }

        // Try to write the content of the database to file
        if (!writeDatabase()) {
            Log.e(LOG_TAG, "Unable to save database file");
            Log.i(LOG_TAG, 
                       "Recovering database by re-inserting in-memory " +
                       "deletion");
            // Re-insert template since we failed to write database to file.
            // Perhaps this will recover the data. It could be that the on-file
            // data is corrupt but we hope not.
            try {
                insertTemplate(templ, finger);
            } catch (PBBiometryException e) {
                // It has been messed up.
                // The state of the database is unfortunately unknown.
                Log.e(LOG_TAG, 
                           "Could not re-insert deleted template into " +
                           "memory cache of database.");
            }
            throw new PBBiometryException(BiometryConstants.STATUS_FILE_ERROR);
        } else {
            return true;
        }
    }

    @Override
    public List<PBBiometryFinger> getEnrolledFingers() {
        Log.v(LOG_TAG, "getEnrolledFingers called");
        return new ArrayList<PBBiometryFinger>(templates.keySet());
    }

    @Override
    public PBBiometryTemplate getTemplate(PBBiometryFinger finger) 
            throws PBBiometryException {
        Log.v(LOG_TAG, "getTemplate called");
        PBBiometryTemplate templ = templates.get(finger);
        if (templ == null) {
            throw new PBBiometryException(BiometryConstants.STATUS_NOT_FOUND);
        }
        return templ;
    }

    @Override
    public boolean insertTemplate(PBBiometryTemplate templ, 
                                  PBBiometryFinger finger)
        throws PBBiometryException {
        Log.v(LOG_TAG, 
                   "Insert template for finger : " + finger.getPosition() + 
                   "/" + finger.getUser().getId());
        // Insert the template in memory
        templates.put(finger, templ);
        
        // Try to write the content of the database to file
        if (!writeDatabase()) {
            Log.e(LOG_TAG, "Unable to save database file");
            Log.i(LOG_TAG, 
                       "Recovering database by deleting in-memory " +
                       "insertion");
            // Re-insert template since we failed to write database to file.
            // Perhaps this will recover the data. It could be that the on-file
            // data is corrupt but we hope not.
            deleteTemplate(finger);
            throw new PBBiometryException(BiometryConstants.STATUS_FILE_ERROR);
        } else {
            return true;
        }
    }

    @Override
    public boolean isTemplateEnrolled(PBBiometryFinger finger)
        throws PBBiometryException {
        Log.v(LOG_TAG, 
                   "Check if template is enrolled for finger: " + 
                   finger.getPosition() + 
                   "/" + finger.getUser().getId());
        return getTemplate(finger) != null;
    }

}
