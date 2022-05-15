package com.porterlee.inventory;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

import com.symbol.emdk.EMDKManager;
import com.symbol.emdk.EMDKResults;
import com.symbol.emdk.barcode.BarcodeManager;
import com.symbol.emdk.barcode.ScanDataCollection;
import com.symbol.emdk.barcode.Scanner;
import com.symbol.emdk.barcode.ScannerConfig;
import com.symbol.emdk.barcode.ScannerException;
import com.symbol.emdk.barcode.ScannerInfo;
import com.symbol.emdk.barcode.StatusData;
import com.symbol.emdk.notification.Notification;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class TC20Scanner extends Activity implements EMDKManager.EMDKListener, Scanner.DataListener, Scanner.StatusListener, BarcodeManager.ScannerConnectionListener {
    private final TC20Scanner instance;
    private BarcodeManager barcodeManager = null;
    private Scanner scanner = null;
    private List<ScannerInfo> deviceList = null;
    private int defaultIndex = 0;
    private EMDKManager emdkManager = null;
    private static final String TAG = TC20Scanner.class.getSimpleName();

    private boolean bSoftTriggerSelected = false;
    private boolean bDecoderSettingsChanged = false;
    private boolean bExtScannerDisconnected = false;
    private String statusString;
    private int scannerIndex = 0;
    private final Object lock = new Object();

    public TC20Scanner() {
        this.instance = this;
        deviceList = new ArrayList<ScannerInfo>();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EMDKResults results = EMDKManager.getEMDKManager(getApplicationContext(), this);
        if (results.statusCode != EMDKResults.STATUS_CODE.SUCCESS) {
            return;
        }
    }

    public int selectScanner() {
        String name = null;
        int result = -1;
        for(int i = 0; i < deviceList.size(); i++) {
            name = deviceList.get(i).getFriendlyName();
            if(name.equalsIgnoreCase("2D Barcode Imager")){
                result = i;
            }
        }
        return result;
    }

    public void initScanner() {
        //updateStatus("Into initScanner");
        int scannerCode = 0;
        if (scanner == null) {
            //updateStatus("scanner NULL");
            if ((deviceList != null) && (deviceList.size() != 0)) {
                if (barcodeManager != null)
                    scannerCode = selectScanner();
                    if (scannerCode == -1) {
                        Log.v("Scanner", "Error with scanner initialization");
                    }
                    else{
                        scanner = barcodeManager.getDevice(deviceList.get(scannerCode));
                    }
                //updateStatus("Scanner Device Received");
            } else {
                Log.v("Scanner","Failed to get the specified scanner device! Please close and restart the application.");
                return;
            }
            if (scanner != null) {
                scanner.addDataListener(instance);
                scanner.addStatusListener(instance);
                try {
                    scanner.enable();
                } catch (ScannerException e) {
                    //updateStatus(e.getMessage());
                    deInitScanner();
                }
            } else {
                //updateStatus("Failed to initialize the scanner device.");
            }
        }
    }

    public void deInitScanner() {
        if (scanner != null) {
            try {
                scanner.disable();
            } catch (Exception e) {
                //updateStatus(e.getMessage());
            }

            try {
                scanner.removeDataListener(instance);
                scanner.removeStatusListener(instance);
            } catch (Exception e) {
                //updateStatus(e.getMessage());
            }

            try {
                scanner.release();
            } catch (Exception e) {
                //updateStatus(e.getMessage());
            }
            scanner = null;
        }
    }

    public void initBarcodeManager() {
        barcodeManager = (BarcodeManager) emdkManager.getInstance(EMDKManager.FEATURE_TYPE.BARCODE);
        // Add connection listener
        if (barcodeManager != null) {
            barcodeManager.addConnectionListener(instance);
        }
        //updateStatus("Barcode manager initialized");
    }

    public void deInitBarcodeManager() {
        if (emdkManager != null) {
            emdkManager.release(EMDKManager.FEATURE_TYPE.BARCODE);
        }
    }

    public void setDecoders() {
        // Configuring accepted barcode types
        feedbackSetup();
        if (scanner != null) {
            try {
                ScannerConfig config = scanner.getConfig();
                config.decoderParams.ean8.enabled = true;
                config.decoderParams.ean13.enabled = true;
                config.decoderParams.code39.enabled = true;
                config.decoderParams.code128.enabled = true;
                scanner.setConfig(config);
            } catch (ScannerException e) {
                //updateStatus(e.getMessage());
            }
        }
    }

    private void feedbackSetup(){
        Notification.Beep feedback = new Notification.Beep();
        feedback.time = 5;
        feedback.frequency = 2000;

        Notification.BeepParams parameters = new Notification.BeepParams();
        //parameters.pattern[(0,1)];
    }

    public void enumerateScannerDevices() {
        // Setting up scanner for first use
        if (barcodeManager != null) {
            List<String> friendlyNameList = new ArrayList<String>();
            int spinnerIndex = 0;
            deviceList = barcodeManager.getSupportedDevicesInfo();
            if ((deviceList != null) && (deviceList.size() != 0)) {
                Iterator<ScannerInfo> it = deviceList.iterator();
                while (it.hasNext()) {
                    ScannerInfo scnInfo = it.next();
                    friendlyNameList.add(scnInfo.getFriendlyName());
                    if (scnInfo.isDefaultScanner()) {
                        defaultIndex = spinnerIndex;
                    }
                    ++spinnerIndex;
                }
                //updateStatus("Enumerated Scanner Device");
            } else {
                //instance.makeToast("Please close and restart the application.");
            }
        }
    }

    public Scanner getScanner() {
        return scanner;
    }

    public List<ScannerInfo> getDeviceList() {
        return deviceList;
    }

    @Override
    public void onOpened(EMDKManager emdkManager) {
        Log.v(TAG, "Opened");
        this.emdkManager = emdkManager;
        initBarcodeManager();
        enumerateScannerDevices();
        initScanner();
    }

    @Override
    public void onClosed() {
        if (emdkManager != null) {
            emdkManager.release();
            emdkManager = null;
        }
    }

    @Override
    public void onConnectionChange(ScannerInfo scannerInfo, BarcodeManager.ConnectionState connectionState) {
        String status;
        String scannerName = "";
        String statusExtScanner = connectionState.toString();
        String scannerNameExtScanner = scannerInfo.getFriendlyName();
        if (deviceList.size() != 0) {
            scannerName = deviceList.get(scannerIndex).getFriendlyName();
        }
        if (scannerName.equalsIgnoreCase(scannerNameExtScanner)) {
            switch (connectionState) {
                case CONNECTED:
                    bSoftTriggerSelected = false;
                    synchronized (lock) {
                        initScanner();
                        bExtScannerDisconnected = false;
                    }
                    break;
                case DISCONNECTED:
                    bExtScannerDisconnected = true;
                    synchronized (lock) {
                        deInitScanner();
                    }
                    break;
            }
            status = scannerNameExtScanner + ":" + statusExtScanner;
            updateStatus(status);
        } else {
            bExtScannerDisconnected = false;
            status = statusString + " " + scannerNameExtScanner + ":" + statusExtScanner;
            updateStatus(status);
        }
    }

    @Override
    public void onData(ScanDataCollection scanDataCollection) {

    }

    @Override
    public void onStatus(StatusData statusData) {

        StatusData.ScannerStates state = statusData.getState();
        switch (state) {
            case IDLE:
                statusString = statusData.getFriendlyName() + " is enabled and idle...";
                // set trigger type
                if (bSoftTriggerSelected) {
                    getScanner().triggerType = Scanner.TriggerType.SOFT_ONCE;
                    bSoftTriggerSelected = false;
                } else {
                    getScanner().triggerType = Scanner.TriggerType.HARD;
                }
                // set decoders
                if (bDecoderSettingsChanged) {
                    setDecoders();
                    bDecoderSettingsChanged = false;
                }
                // submit read
                if (!getScanner().isReadPending() && !bExtScannerDisconnected) {
                    try {
                        getScanner().read();
                    } catch (ScannerException e) {
                        updateStatus(e.getMessage());
                    }
                }
                break;
            case WAITING:
                statusString = "Scanner is waiting for trigger press...";
                updateStatus(statusString);
                break;
            case SCANNING:
                statusString = "Scanning...";
                updateStatus(statusString);
                break;
            case DISABLED:
                statusString = statusData.getFriendlyName() + " is disabled.";
                updateStatus(statusString);
                break;
            case ERROR:
                statusString = "An error has occurred.";
                updateStatus(statusString);
                break;
            default:
                break;
        }
    }

    private void updateStatus(String status) {
        return;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (emdkManager != null) {
            emdkManager.release();
            emdkManager = null;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (emdkManager != null) {
            initBarcodeManager();
            enumerateScannerDevices();
            initScanner();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        deInitScanner();
        deInitBarcodeManager();
    }
}
