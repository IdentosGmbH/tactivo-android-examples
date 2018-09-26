package com.precisebiometrics.android.app.pbsmartcardexample;

import java.util.List;

import android.app.Activity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

import com.precisebiometrics.android.mtk.api.PBInitializedCallback;
import com.precisebiometrics.android.mtk.api.mtksmartcardio.MTKTerminals;
import com.precisebiometrics.android.mtk.api.smartcardio.Card;
import com.precisebiometrics.android.mtk.api.smartcardio.CardChannel;
import com.precisebiometrics.android.mtk.api.smartcardio.CardException;
import com.precisebiometrics.android.mtk.api.smartcardio.CardTerminal;
import com.precisebiometrics.android.mtk.api.smartcardio.CardTerminals;
import com.precisebiometrics.android.mtk.api.smartcardio.CommandAPDU;
import com.precisebiometrics.android.mtk.api.smartcardio.ResponseAPDU;
import com.precisebiometrics.android.mtk.api.smartcardio.TerminalFactory;

/**
 * Simple example that shows how to access the smart card API, how to access the
 * smart card reader and how to communicate with a smart card. The app waits for
 * a card to be inserted in the reader slot and then attempts to read the card
 * Global Platfom Card Production Life Cycle (CPLC) data from the card (if
 * supported).
 */
public class SmartCardExampleActivity extends Activity implements
        OnClickListener, PBInitializedCallback {

    // SmartCardIO objects
    private TerminalFactory factory = null;
    private CardTerminals terminals = null;
    private List<CardTerminal> terminalList = null;
    private CardTerminal terminal = null;
    private Card card = null;
    private CardChannel channel = null;

    /** Holds the connection state with the API */
    private boolean scConnected = false;

    /** The work thread */
    private Thread smartCardWorkThread = null;

    /** Flag used to cancel the work thread */
    private boolean canceled = false;

    /** The main canvas for displaying status messages */
    private TextView txt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_smart_card_example);
        txt = (TextView) findViewById(R.id.textViewStatus);
        txt.setOnClickListener(this);
        txt.setMovementMethod(new ScrollingMovementMethod());
        txt.setText("");
        append("Tactivo Smart Card Example.");
        append("This app monitors the smart card reader slot and attempts to read the CPLC data from the inserted smart card.");
        append("The app loop can be restarted at any time by tapping the screen.");
        append("");
    }

    @Override
    protected void onStart() {

        try {
            // Establish the connection with the toolkit using 'this'
            // as receiver for the PBInitializedCallback callback methods.
            MTKTerminals.initialize(getApplicationContext(),
                                    SmartCardExampleActivity.this);
        } catch (Exception e) {
            append("MTKTerminals.initialize FAILED:" + e.toString());
            e.printStackTrace();
        }
        super.onStart();
    }

    @Override
    protected void onStop() {

        try {
            // Cancel and exit the work thread
            killThread();
            // Release the connection with the toolkit
            MTKTerminals.close();
        } catch (Exception e) {
            append("MTKTerminals.close FAILED:" + e.toString());
            e.printStackTrace();
        }

        super.onStop();
    }

    /**
     * Helper method to append text from any thread.
     * 
     * @param text Text to append.
     */
    public void append(final String text) {
        runOnUiThread(new Runnable() {
            public void run() {
                txt.append(text + "\n");
            }
        });
    }

    @Override
    public void initializationFailed() {
        append("Unable to initialize the Biometrics API.");
        append("Perhaps the Tactivo Manager is not installed.");
        scConnected = false;
        // Cancel and exit the work thread
        killThread();
    }

    @Override
    public void initialized() {
        append("Smart Card API connection established");
        scConnected = true;
        // Launch the working thread that will do smart card
        spawnThread();
    }

    @Override
    public void uninitialized() {
        append("Smart Card API connection is closed/lost");
        scConnected = false;
        // Cancel and exit the work thread
        killThread();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
        case R.id.textViewStatus:
            // Clicking the TextView will cancel the ongoing thread and launch a
            // new
            killThread();
            spawnThread();
            break;
        default:
            break;
        }
    }

    /**
     * Helper method close an active work thread
     */
    private synchronized void killThread() {

        // Notify the thread that it is time to exit
        canceled = true;

        if (smartCardWorkThread == null) {
            // No thread exist
            return;
        }
        if (!smartCardWorkThread.isAlive()) {
            // Old thread that is already terminated
            return;
        }

        try {
            // Wait for the thread to exit before returning
            smartCardWorkThread.join();
        } catch (InterruptedException e) {
            append("smartCardWorkThread.join FAILED: " + e.toString());
            e.printStackTrace();
        }
    }

    /**
     * Read, parse and display the inserted card CPLC data, if any.
     * 
     * @throws CardException
     */
    private void readCPLC() throws CardException {

        // Get the basic channel
        channel = card.getBasicChannel();

        // See www.globalplatform.org for more information about this command.
        // CLA = 0x80
        // INS = 0xCa
        // P1 = 0x9F
        // P2 = 0x7F
        // Le = 0x00
        CommandAPDU GET_DATA_CardProductionLifeCycle = 
            new CommandAPDU(0x80, 0xCA, 0x9F, 0x7F, 0x00);

        ResponseAPDU cardResponse;

        // Send the command to the card
        cardResponse = channel.transmit(GET_DATA_CardProductionLifeCycle);

        // Check SW1 if we provided wrong Le
        if (cardResponse.getSW1() == 0x6C) {
            // Modify the command with correct Le reported by the card in SW2.
            GET_DATA_CardProductionLifeCycle = 
                new CommandAPDU(0x80, 0xCA, 0x9F, 0x7F, cardResponse.getSW2());

            // Re-send the command but now with correct Le
            cardResponse = channel.transmit(GET_DATA_CardProductionLifeCycle);
        }

        // Check if the card has data for us to collect
        if (cardResponse.getSW1() == 0x61) {

            // Issue a GET RESPONSE command using SW2 as Le
            CommandAPDU GET_RESPONSE = 
                new CommandAPDU(0x00, 0xC0, 0x00, 0x00, cardResponse.getSW2());
            cardResponse = channel.transmit(GET_RESPONSE);
        }

        // Check the final result of the GET DATA CPLC command
        if (cardResponse.getSW() != 0x9000) {
            // The card does not support Global Platform
            append(String.format("Card responded with SW:%04x", 
                                 cardResponse.getSW()));
            append("This card does not support the Global Platform " +
                   "GET CPLC command");
            return;
        }

        // we do not validate the data in this example - we assume that it is
        // correct...
        byte[] buf = cardResponse.getBytes();
        int i = 0;
        append("Card CPLC Data:");
        append(String.format("-IC fabricator = %02X%02X", 
                             buf[i++], buf[i++]));
        append(String.format("-IC type = %02X%02X", 
                             buf[i++], buf[i++]));
        append(String.format("-OS ID = %02X%02X", 
                             buf[i++], buf[i++]));
        append(String.format("-OS release date = %02X%02X", 
                             buf[i++], buf[i++]));
        append(String.format("-OS release level = %02X%02X", 
                             buf[i++], buf[i++]));
        append(String.format("-IC fabrication date = %02X%02X", 
                             buf[i++],buf[i++]));
        append(String.format("-IC serial number = %02X%02X%02X%02X", 
                             buf[i++],buf[i++], buf[i++], buf[i++]));
        append(String.format("-IC batch ID = %02X%02X", 
                             buf[i++], buf[i++]));
        append(String.format("-IC module fabrictor = %02X%02X", 
                             buf[i++],buf[i++]));
        append(String.format("-IC module package date = %02X%02X", 
                             buf[i++],buf[i++]));
        append(String.format("-ICC manufacturer = %02X%02X", 
                             buf[i++], buf[i++]));
        append(String.format("-ICC embedding date = %02X%02X", 
                             buf[i++],buf[i++]));
        append(String.format("-IC pre-personalizer = %02X%02X", 
                             buf[i++],buf[i++]));
        append(String.format("-IC pre-perso. equipment date = %02X%02X",
                             buf[i++], buf[i++]));
        append(String.format("-IC pre-perso. equipment ID = %02X%02X%02X%02X",
                             buf[i++], buf[i++], buf[i++], buf[i++]));
        append(String.format("-IC personzalizer = %02X%02X", 
                             buf[i++], buf[i++]));
        append(String.format("-IC perso. date = %02X%02X", 
                             buf[i++], buf[i++]));
        append(String.format("-IC perso. equipment ID = %02X%02X%02X%02X",
                             buf[i++], buf[i++], buf[i++], buf[i++]));
    }

    /**
     * Creates and starts a new work thread
     */
    private synchronized void spawnThread() {

        // The thread doing the heavy lifting
        smartCardWorkThread = new Thread() {

            public void run() {

                try {
                    // Get the default factory
                    factory = TerminalFactory.getDefault();
                    // Get the terminals for the selected factory
                    terminals = factory.terminals();
                    // Get the list of currently connected terminals
                    terminalList = terminals.list();

                    if (terminalList.isEmpty()) {
                        append("No smart card reader detected");
                        append("Connect a Tactivo and press screen to continue");
                        return;
                    }
                    // Use the first found terminal in the list
                    terminal = terminalList.get(0);
                    append("Found smart card reader: " + terminal.getName());

                    // Run the outer loop as long as the thread isn't canceled,
                    // as long as the connection with the API is intact or until
                    // something goes unexpectedly wrong (like the smart card
                    // reader is disconnected)
                    while (scConnected && !canceled) {

                        // Test if there is a smart card in the reader
                        if (terminal.isCardPresent() == false) {
                            // No smart card present, prompt the user to insert
                            // a card
                            append("Insert smart card...");
                            // Poll the slot status regularly so that we have a
                            // chance to exit the thread if it is canceled
                            while (!terminal.isCardPresent() && !canceled) {
                                // Wait for a card insertion event
                                terminal.waitForCardPresent(750);
                            }

                            // Exit thread if something has canceled it
                            if (canceled) {
                                return;
                            }
                        }

                        // A card is present in the reader slot
                        append("Card present!");

                        try {
                            // Connect to the smart card with either T0 or T1
                            card = terminal.connect("*");

                            append("Connected to card with the "
                                   + card.getProtocol() + " protocol");
                            append("Card ATR:" + card.getATR().toString());

                            // Try to read the CPLC data from the card
                            readCPLC();

                            // Disconnect from the card and reset it.
                            card.disconnect(true);
                        } catch (CardException e) {
                            append("CardException caught:" + e.toString());
                        } catch (Exception e) {
                            append("Unexpected exception caught:" + e.toString());
                            e.printStackTrace();
                        }

                        // Check if we have been canceled
                        if (canceled) {
                            return;
                        }

                        // Prompt the user to remove the card
                        append("Remove card...");
                        // Poll the slot status regularly so that we have a
                        // chance to exit the thread if it is canceled
                        while (terminal.isCardPresent() == true && !canceled) {
                            // Wait for the card to be removed.
                            terminal.waitForCardAbsent(750);
                        }
                    }

                    return;
                }
                // Catch more unexpected exceptions in the outer loop.
                catch (CardException e) {
                    append("CardException caught:" + e.toString());
                    e.printStackTrace();
                } catch (Exception e) {
                    append("Unexpected exception caught:" + e.toString());
                    e.printStackTrace();
                }

                append("Work thread terminated due to exception!");
                append("Click the screen to restart the smart card thread.");
            }
        };

        // Reset the cancel flag
        canceled = false;
        // Start the work thread
        smartCardWorkThread.start();
    }
}
