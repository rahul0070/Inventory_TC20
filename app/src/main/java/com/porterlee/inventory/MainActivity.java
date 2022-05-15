/*
 * Copyright (C) 2015-2019 Zebra Technologies Corporation and/or its affiliates
 * All rights reserved.
 */
package com.porterlee.inventory;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.RelativeSizeSpan;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.porterlee.inventory.InventoryDatabase.ItemTable;
import com.porterlee.inventory.InventoryDatabase.LocationTable;
import com.symbol.emdk.barcode.ScanDataCollection;
import com.symbol.emdk.barcode.ScannerResults;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends TC20Scanner implements ItemRecyclerAdapter.ItemClickListener, LocationRecyclerAdapter.ItemClickListener {
    public static final String EXTRA_MESSAGE = "";

    private TextView textViewStatus = null;
    private TextView textCount = null;
    private TextView textLastScanned = null;
    private TextView textLocation = null;
    private LinearLayout containerLayout = null;
    private LinearLayout infoLayout = null;
    private TextView containerText = null;
    private TextView containerIdView = null;
    private LinearLayout welcomeView = null;
    private TextView companyText = null;

    private Menu mOptionsMenu;

    private RecyclerView recyclerView;
    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager layoutManager;

    private RecyclerView recyclerLocationView;
    private RecyclerView.Adapter locationAdapter;
    private RecyclerView.LayoutManager locationLayoutManager;
    //private SelectableRecyclerView itemRecyclerView; //?//

    private int scannerIndex = 0; // Keep the selected scanner
    // Keep the default scanner
    private String statusString = "";

    private boolean bSoftTriggerSelected = false;
    private boolean bDecoderSettingsChanged = false;
    private boolean bExtScannerDisconnected = false;
    private final Object lock = new Object();

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final String OUTPUT_FILE_HEADER = String.format(Locale.US, "%s|%s|%s|%s|%s|v%s|%d", BuildConfig.FLAVOR, "Inventory", BuildConfig.BUILD_TYPE, BuildConfig.FLAVOR, BuildConfig.BUILD_TYPE, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE);
    private static final String INVENTORY_DIR_NAME = Config.INVENTORY_DIR_NAME;

    private File outputFile;
    private File outputDir;

    private AsyncTask<Void, Float, String> saveTask;
    private String barcode;
    private long lastLocationId = -1;
    private String lastLocationBarcode = "";
    private String lastItemBarcode = "";
    private String emptyString = "";
    private boolean containerMode = false;

    // Recycler Views
    ItemRecyclerAdapter adapter;
    LocationRecyclerAdapter.LocationViewHolder lholder;
    private ArrayList<ArrayList<String>> itemList = new ArrayList<>();
    private ArrayList<String> locationList = new ArrayList<>();
    private ArrayList<String> containerList = new ArrayList<>();
    private List<View> viewList = new ArrayList<View>();

    private DataManager dataManager;
    //private com.porterlee.inventory.Device device;
    MediaPlayer player;
    MediaPlayer player2;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        dataManager = new DataManager(getApplicationContext());
        initializeUI();
        if (getPermissions()){
            dataManager.initializeFiles();
        }
        else {
            Log.v(TAG, "Error initializing files.");
            databaseLoadError();
        }

        if (dataManager.noData()) welcomeScreen(1);
        else welcomeScreen(0);

        refreshView();

        // Test function to check database.
        try {
            dataManager.testDatabaseWithLog();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

//    public void onScanDecode(DecodeResult decodeResult)
//    {
//        barcode = decodeResult.toString();
//        barcode = barcode.replace("\n", "").replace("\r", "");
//        barcode = barcode.replaceAll("\\s","");
//        onBarcodeScanned();
//    }


    @Override
    public void onData(ScanDataCollection scanDataCollection) {
        // Scan event listener function for Zebra API
        super.onData(scanDataCollection);
        Log.v(TAG, "Scanned");
        if ((scanDataCollection != null) && (scanDataCollection.getResult() == ScannerResults.SUCCESS)) {
            ArrayList<ScanDataCollection.ScanData> scanData = scanDataCollection.getScanData();
            for (ScanDataCollection.ScanData data : scanData) {
                barcode = data.getData();
                onBarcodeScanned();
            }
        }
    }
    //////////////////////////
    public void initializeUI() {
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);
        setDefaultOrientation();

        textCount = (TextView) findViewById(R.id.textViewCount);
        textLastScanned = (TextView) findViewById(R.id.textViewScanned);
        textLocation = (TextView) findViewById(R.id.textViewLocation);

        textViewStatus = (TextView) findViewById(R.id.textViewStatus);
        recyclerView = (RecyclerView) findViewById(R.id.item_recycler);
        recyclerView.setHasFixedSize(true);

        recyclerLocationView = (RecyclerView) findViewById(R.id.location_recycler);
        recyclerLocationView.setHasFixedSize(true);

        layoutManager = new LinearLayoutManager(this);
        //((LinearLayoutManager) layoutManager).setReverseLayout(true);
        recyclerView.setLayoutManager(layoutManager);

        locationLayoutManager = new LinearLayoutManager(this);
        recyclerLocationView.setLayoutManager(locationLayoutManager);

        adapter = new ItemRecyclerAdapter(this, itemList, this);
        adapter.setClickListener((ItemRecyclerAdapter.ItemClickListener) this);
        recyclerView.setAdapter(adapter);

        locationAdapter = new LocationRecyclerAdapter(this, locationList, this);
        ((LocationRecyclerAdapter) locationAdapter).setClickListener((LocationRecyclerAdapter.ItemClickListener) this);
        recyclerLocationView.setAdapter(locationAdapter);

        final RecyclerView.ItemAnimator locationRecyclerAnimator = new DefaultItemAnimator();
        locationRecyclerAnimator.setAddDuration(100);
        locationRecyclerAnimator.setChangeDuration(100);
        locationRecyclerAnimator.setMoveDuration(100);
        locationRecyclerAnimator.setRemoveDuration(100);
        recyclerView.setItemAnimator(locationRecyclerAnimator);

        // Horizontal divider for Recycler Views
        DividerItemDecoration horizontalDecoration = new DividerItemDecoration(recyclerView.getContext(),
                DividerItemDecoration.VERTICAL);

        Drawable horizontalDivider = ContextCompat.getDrawable(MainActivity.this, R.drawable.horizontal_divider);
        horizontalDecoration.setDrawable(horizontalDivider);
        recyclerView.addItemDecoration(horizontalDecoration);
        recyclerLocationView.addItemDecoration(horizontalDecoration);

        containerLayout = findViewById(R.id.containerWelcome);
        infoLayout = findViewById(R.id.info);
        containerIdView = findViewById(R.id.containerId);
        welcomeView = findViewById(R.id.welcomeLayout);
        companyText = findViewById(R.id.company);
        switchUI(2);

        Intent intent = new Intent(this, SettingsActivity.class);
        dataManager.setPreferences();

        player = MediaPlayer.create(this, R.raw.beep_short);
        player2 = MediaPlayer.create(this, R.raw.robot);

        companyText.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Uri uri = Uri.parse("http://www.porterlee.com");
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                startActivity(intent);
            }
        });
    }

    @Override
    public void onItemClick(View view, final int position) {
        // OnClick event handler for ItemRecycler.
        new AlertDialog.Builder(MainActivity.this)
                .setCancelable(false)
                .setTitle("Remove Item")
                .setMessage("Do you want to remove item " + barcode + " ?")
                .setNegativeButton(R.string.action_no, null)
                .setPositiveButton(R.string.action_yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        itemRemoveAction(position);
                    }
                }).create().show();
    }

    @Override
    public void onLocationClick(View view, final int position) {
        // OnClick event handler for LocationRecycler.
        Log.v(TAG, "Location listener");
        if (containerMode) {
            makeToast("Complete current container");
            return;
        }
        lastLocationId = position+1;
        lastLocationBarcode = dataManager.locationBarcodeValue(lastLocationId);
        updateUI(textLocation, lastLocationBarcode);
        updateItemRecycler();
    }

    public void itemRemoveAction(int position){
        ArrayList<String> item = itemList.get(position);
        String barcode = item.get(0);

        if (containerMode) {
            containerList.remove(position);
            adapter.notifyItemRemoved(position);
            return;
        }

        if (dataManager.isOpenContainer(barcode)) {
            ArrayList<String> barcodeList = new ArrayList<String>();
            long containerId = dataManager.getContainerId(barcode);
            barcodeList = dataManager.getItemsInContainer(containerId);

            dataManager.deleteEntry(barcode, ItemTable.NAME);
            for (String b : barcodeList) {
                dataManager.deleteEntry(b, ItemTable.NAME);
            }
            refreshView();
            return;
        }

        dataManager.deleteEntry(barcode, ItemTable.NAME);
        itemList.remove(position);
        adapter.notifyItemRemoved(position);
    }

    public void editQuantity(int position) {
        final String barcodeToEdit = itemList.get(position).get(0);

        final AlertDialog tempAlertDialog = new AlertDialog.Builder(this)
                .setView(R.layout.edit_quantity_layout)
                .setPositiveButton("Save", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        final AlertDialog alertDialog = (AlertDialog) dialog;
                        final EditText quantityBox = alertDialog.findViewById(R.id.quantity_entry);
                        String data = quantityBox.getText().toString();
                            if (!dataManager.updateQuantity(lastLocationId, barcodeToEdit, Integer.parseInt(data))) {
                                makeToast("Error while updating quantity");
                            };
                        refreshView();
                    }
                }).setNeutralButton("Clear", null)
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        makeToast("Quantity Update Canceled");
                    }
                }).setCancelable(false)
                .create();

        final int Quantity = dataManager.getQuantity(lastLocationId, barcodeToEdit);
        tempAlertDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(final DialogInterface dialog) {
                final AlertDialog alertDialog = (AlertDialog) dialog;
                final EditText commentsBox = alertDialog.findViewById(R.id.quantity_entry);
                commentsBox.setText(String.valueOf(Quantity));

            }
        });
        tempAlertDialog.show();

    }

    public void selectItem(View view){
        viewList.add(view);
        for (View item : viewList){
            Log.v(TAG, "View " + String.valueOf(viewList.size()));
            item.setSelected(false);
        }
        view.setSelected(true);
    }

/////////////////// RECYCLER MANAGING /////////////////////////

    public void refreshView() {
        // Refresh UI (recycler views) when there is a change in database.
        updateLocationRecycler();
        updateItemRecycler();
        updateUI(textCount, dataManager.getCount());
        updateUI(textLocation, dataManager.locationBarcodeValue(lastLocationId));
        return;
    }

    private boolean getPermissions() {
        boolean ans = false;
        if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            makeToast("Write external storage permission is required for this");
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, 2);
        }

        return true;
    }

    public DataManager getDataManager() {
        return dataManager;
    }


    public void updateLocationRecycler() {
        Log.v(TAG, "Updating Location View");
        clearLocationRecycler();
        String tableName = LocationTable.NAME;
        Cursor c = dataManager.getLocationCursor();
        c.moveToLast();

        //Updating last location ID to previously scanned location ID
        if (lastLocationId == -1) {
            try {
                lastLocationId = c.getInt(0);
                Log.w(TAG, "Location ID in recycler" + lastLocationId);
                lastLocationBarcode = c.getString(1);
                updateUI(textLocation, dataManager.locationBarcodeValue(lastLocationId));
            } catch (Exception e) {
                Log.w(TAG, "Location database empty");
                lastLocationId = -1;
                return;
            }
        }
        c.moveToFirst();
        if (c.getCount() == 0){
            return;
        }
        do{
            addToRecycler(c.getString(1), 0, tableName);
        }while(c.moveToNext());
    }

    public void updateItemRecycler() {
        clearItemRecycler();
        String tableName = ItemTable.NAME;
        Log.w(TAG, "Last Location: " + lastLocationId);
        Cursor c = dataManager.getDatabase().rawQuery("SELECT * FROM " + tableName + " WHERE location" + " = " + lastLocationId, null);
        c.moveToFirst();

        try{
            String test = c.getString(1);
        }
        catch(Exception e){
            makeToast("There are no items in the selected location");
            //String test = c.getString(1);
            return;
        }

        do{
            addToRecycler(c.getString(1), c.getLong(2), tableName);
        }while(c.moveToNext());
    }

    public void addToRecycler(String barcode, long quantity, String recyclerName) {
        if (recyclerName.equals(LocationTable.NAME)) {
            locationList.add(barcode);
            locationAdapter.notifyItemInserted(locationList.size());
        } else {

            itemList.add(createNewItem(barcode, String.valueOf(quantity)));
            adapter.notifyItemInserted(itemList.size());
        }
    }



///////////////////////// END /////////////////////////////

    private void databaseLoadError() {
        //Handling database load error.
        new AlertDialog.Builder(MainActivity.this)
                .setCancelable(false)
                .setTitle("Database Load Error")
                .setMessage("There was an error loading the inventory file and it could not be archived.\n\nWould you like to delete the it?\n\nAnswering no will close the app.")
                .setNegativeButton(R.string.action_no, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                }).setPositiveButton(R.string.action_yes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (!dataManager.deleteDB()) {
                    makeToast("The file could not be deleted", "long");
                    player.start();
                    finish();
                    return;
                }
                makeToast("The file was closed.", "long");
                dataManager.initializeDB();
                //mDatabase = SQLiteDatabase.openOrCreateDatabase(databaseFile, null);
            }
        }).setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
            }
        }).create().show();
    }

    public void onBarcodeScanned() {
        // Event of barcode scanned, called from onData() which is the event listener for Zebra API
        Log.w(TAG, "Barcode scanned");
        if (saveTask != null) {
            Log.w(TAG, "Save task not NULL");
            makeToast("Cannot scan while saving");
            player.start();
            return;
        }

        if (barcode == null || barcode.equals("")) {
            Log.w(TAG, "Empty barcode");
            makeToast("Error scanning barcode: Empty result", "long");
            player.start();
            return;
        }

        if (barcode.equals("READ_FAIL")) {
            addItem(barcode, emptyString, 0);
            return;
        }

        final boolean isDuplicate = dataManager.checkIfDuplicate(lastLocationId, barcode);

        if (isDuplicate && !dataManager.allowDuplicates()) {
            makeToast("Duplicate barcode scanned. Go to settings to allow entering duplicates.");
            player.start();
            return;
        }

        if (dataManager.identifyLocationBarcode(barcode)) {
            welcomeScreen(0);
            Log.w(TAG, "Barcode recognized (Location)");
            Log.v(TAG, "Barcode: "+String.valueOf(BuildConfig.FLAVOR));
            if (containerMode) {
                makeToast("Finish current container");
                player.start();
                return;
            }
            player2.start();
            addBarcodeLocation(barcode);
            return;
        }

        if (dataManager.identifyContainerBarcode(barcode)) {
            Log.w(TAG, "Barcode recognized (Container)");
            player2.start();

            if (lastLocationId == -1) {
                makeToast("A Location has not been scanned");
                player.start();
                return;
            }

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (!isFinishing()){
                        new AlertDialog.Builder(MainActivity.this)
                                .setTitle("Container Scanned")
                                .setMessage("Is the container sealed?")
                                .setCancelable(false)
                                .setNegativeButton(R.string.action_no, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        containerMode = true;
                                        long containerId = dataManager.getContainerId();
                                        Log.v(TAG, "GGG: " + String.valueOf(containerId));
                                        addItem(barcode, "Y", containerId);
                                        startContainer(barcode);
                                    }
                                })
                                .setPositiveButton(R.string.action_yes, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        addItem(barcode, "N", 0);
                                    }
                                }).show();
                    }
                }
            });
        }

        else if (!dataManager.allowExternal()){
            // Looking for item barcode.
            if (BarcodeType.getBarcodeType(barcode).equals(BarcodeType.Invalid)) {
                //AbstractScanner.onScanComplete(false);
                Log.w(TAG, "Barcode \"" + barcode + "\" not recognised");
                makeToast("Barcode \"" + barcode + "\" not recognised");
                player.start();
                return;
            }

            if (dataManager.acceptedItem(barcode)) {
                    Log.w(TAG, "Barcode recognized");
                    if (containerMode) {
                        //addItem(barcode, "Y");
                        containerAdd(barcode);
                        return;
                    }
                    addItem(barcode, emptyString, 0);
            }

            else{
                Log.w(TAG, "Barcode not recognized");
                makeToast("Barcode not Valid. Go to settings to allow unrestricted entry.");
                player.start();
            }
        }

        else {
            // Allowing external
            addItem(barcode, emptyString, 0);
        }
    }

    private void addItem(@NonNull String barcode, String containerOption, long container_id) {
        Log.w(TAG, containerOption);
        // Function to add item barcode to database.
        if (saveTask != null) return;
        if (lastLocationId == -1) {
            Log.w(TAG, "A location has not been scanned");
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(), "A location has not been scanned", Toast.LENGTH_LONG).show();
                }
            });
            player.start();
            return;
        }
        Log.w(TAG, String.valueOf(lastLocationId));

        if (dataManager.insertItemData(barcode, lastLocationId, containerOption, container_id) == -1) {
            Utils.vibrate(this.getApplicationContext());
            Log.w(TAG, "Error adding item \"" + barcode + "\" to the inventory");
            Toast.makeText(this, "Error adding item \"" + barcode + "\" to the inventory", Toast.LENGTH_LONG).show();
            return;
        }
        updateData(barcode);
        updateUI(textLastScanned, barcode);
        updateUI(textCount, dataManager.getCount());
    }

    private void addBarcodeLocation(@NonNull String barcode) {
        // Adding a location barcode to database.
        if (saveTask != null) return;

        long rowID;
        if (lastLocationId != -1) {
            int checkValue = dataManager.checkIfExists(barcode);
            if (checkValue != -1) {
                // Location exists in database
                Log.v(TAG, "Location exists");
                rowID = checkValue;
                lastLocationId = rowID;
                lastLocationBarcode = barcode;
                Log.v(TAG, "New changed location ID: " + lastLocationId);
                refreshView();
                return;
            }
        }

        Log.v(TAG, "Location doesn't exist");
        rowID = dataManager.insertLocationData(barcode);

        if (rowID == -1) {
            Log.w(TAG, "Error adding location \"" + barcode + "\" to the inventory");
            Toast.makeText(this, "Error adding location \"" + barcode + "\" to the inventory", Toast.LENGTH_LONG).show();
            return;
        }

        if (!lastLocationBarcode.equals(barcode)) {
            lastLocationId = rowID;
            Log.w(TAG, "Location id updated");
            lastLocationBarcode = barcode;
        }
        Log.v(TAG, "String " + barcode);
        updateUI(textLocation, barcode);
        refreshView();
    }


    ////////////////////////


    /////////////
    // UI
    /////////////


    public ArrayList<String> createNewItem(String barcode, String quantity) {
        ArrayList<String> item = new ArrayList<>();
        item.add(barcode);
        item.add(quantity);
        return item;
    }

    private void startContainer(String barcode) {
        switchUI(1);
        updateUI(containerIdView, barcode);

    }

    private void containerAdd(String barcode) {
        addToRecycler(barcode, 0,"item");
        containerList.add(barcode);
    }

    private void switchUI(int value) {
        if (value == 1) {
            //recyclerLocationView.getLayoutParams().height = 130;
            containerLayout.setVisibility(View.VISIBLE);
            infoLayout.setVisibility(View.GONE);
            setContainerRecycler();
        }
        else if (value == 0){
            //recyclerLocationView.getLayoutParams().height = 140;
            containerLayout.setVisibility(View.GONE);
            infoLayout.setVisibility(View.VISIBLE);
            refreshView();
        }
        else{
            containerLayout.setVisibility(View.GONE);
            infoLayout.setVisibility(View.VISIBLE);
        }
    }

    private void welcomeScreen(final int value) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (value == 1) {
                    welcomeView.setVisibility(View.VISIBLE);
                    infoLayout.setVisibility(View.GONE);
                    //postSave();
                    //mOptionsMenu.findItem(R.id.cancel_save).setVisible(false);

                }
                else {
                    welcomeView.setVisibility(View.GONE);
                    infoLayout.setVisibility(View.VISIBLE);
                    //mOptionsMenu.findItem(R.id.cancel_save).setVisible(true);
                }
            }
        });
    }

    private void setContainerRecycler() {
        clearItemRecycler();
    }

    public void containerOkButton(View view) {
        long containerId = dataManager.getContainerId();
        for (String s : containerList) {
            addItem(s, "", containerId);
        }
        containerList.clear();
        switchUI(0);
        containerMode = false;
    }

    public void containerCancelButton(View view) {
        containerList.clear();
        switchUI(0);
        containerMode = false;
    }

    public void clearItemRecycler(){
        int size = itemList.size();
        itemList.clear();
        adapter.notifyItemRangeRemoved(0, size);
    }

    public void clearLocationRecycler(){
        int size = locationList.size();
        locationList.clear();
        locationAdapter.notifyItemRangeRemoved(0, size);
    }

    private void updateStatus(final String status) {
//        runOnUiThread(new Runnable() {
//            @Override
////            public void run() {
////                textLastScanned.setText(status);
////            }
//        });
    }

    private void updateData(final String result) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                addToRecycler(result, 1, ItemTable.NAME);
            }
        });
    }

    private void updateUI(final TextView component, final String value){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                component.setText(value.trim());
            }
        });
    }

    public void makeToast(final String value){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), value, Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void makeToast(final String value, String type){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), value, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void setDefaultOrientation() {
        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        int width = dm.widthPixels;
        int height = dm.heightPixels;
        if (width > height) {
            setContentView(R.layout.activity_main);
        } else {
            setContentView(R.layout.activity_main);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        mOptionsMenu = menu;
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);

        for(int i = 0; i < menu.size(); i++) {
            MenuItem item = menu.getItem(i);
            SpannableString spanString = new SpannableString(menu.getItem(i).getTitle().toString());
            int end = spanString.length();
            spanString.setSpan(new RelativeSizeSpan(1.0f), 0, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            item.setTitle(spanString);
        }
        return true;
        //return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_remove_all:
                if (!dataManager.isEmpty()) {
                    //getScanner().setIsEnabled(false);
                    new AlertDialog.Builder(this)
                            .setCancelable(true)
                            .setTitle("Clear Inventory")
                            .setMessage("Are you sure you want to clear this inventory?")
                            .setNegativeButton(R.string.action_no, null)
                            .setPositiveButton(R.string.action_yes, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    if (saveTask != null) {
                                        return;
                                    }
                                    //changedSinceLastArchive = true;
                                    dataManager.resetTables();
                                    refreshView();

                                    lastLocationId = -1;
                                    lastLocationBarcode = "";
                                    lastItemBarcode = "";
                                    welcomeScreen(1);

                                    updateUI(textCount, "");
                                    updateUI(textLastScanned, "");
                                    updateUI(textLocation, "");

                                    makeToast("Inventory cleared.");
                                }
                            }).setOnDismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialog) {
                        }
                    }).create().show();
                } else {

                }
                return true;
            case R.id.action_save_to_file:
                if (dataManager.noItems()) {
                    SharedPreferences preferences = getApplicationContext().getSharedPreferences("Inventory_preference", Context.MODE_PRIVATE);
                    String emptyLocationSavePreference = preferences.getString("allowEmptyLocation", "");
                    if (emptyLocationSavePreference == "false"){
                        makeToast("There are no items in this inventory");
                        return true;
                    }
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                        makeToast("Write external storage permission is required for this");
                        ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                        return true;
                    }
                }

                if (saveTask == null) {
                    makeToast("Saving...");
                    saveTask = new SaveToFileTask().execute();
                    Log.w(TAG, "Saving");
                    postSave();
                } else {
                    saveTask.cancel(false);
                    postSave();
                }

                return true;
            case R.id.cancel_save:
                if (saveTask != null) {

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (!isFinishing()){
                                new AlertDialog.Builder(MainActivity.this)
                                        .setTitle("Cancel Save")
                                        .setMessage("Are you sure you want to stop saving this file?")
                                        .setCancelable(false)
                                        .setNegativeButton(R.string.action_no, null)
                                        .setPositiveButton(R.string.action_yes, new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                if (saveTask != null && !saveTask.isCancelled()) {
                                                    saveTask.cancel(false);
                                                }
                                            }
                                        }).show();
                            }
                        }
                    });
                } else {
                    postSave();
                }
                return true;

            case R.id.action_settings:
                Intent settingsIntent = new Intent(this, SettingsActivity.class);
                startActivity(settingsIntent);

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void preSave() {
        mOptionsMenu.findItem(R.id.action_save_to_file).setVisible(false);
        mOptionsMenu.findItem(R.id.cancel_save).setVisible(true);
        mOptionsMenu.findItem(R.id.action_remove_all).setVisible(false);
        onPrepareOptionsMenu(mOptionsMenu);
    }

    private void postSave() {
        saveTask = null;
        mOptionsMenu.findItem(R.id.action_save_to_file).setVisible(true);
        mOptionsMenu.findItem(R.id.cancel_save).setVisible(false);
        mOptionsMenu.findItem(R.id.action_remove_all).setVisible(true);
        onPrepareOptionsMenu(mOptionsMenu);
    }

///// SAVE TASKS /////

    private class SaveToFileTask extends AsyncTask<Void, Float, String> {
        private static final int MAX_UPDATES = 100;

        protected String doInBackground(Void... voids) {
            Log.w(TAG, "Save action");
            Cursor itemCursor = dataManager.getDatabase().rawQuery("SELECT " + ItemTable.Keys.BARCODE + ", " + ItemTable.Keys.QUANTITY + ", " + ItemTable.Keys.LOCATION_ID + ", " + ItemTable.Keys.DATE_TIME + ", " + ItemTable.Keys.CONTAINER_STATUS + " FROM " + ItemTable.NAME + " ORDER BY " + ItemTable.Keys.LOCATION_ID + " ASC;", null);
            Cursor locationCursor = dataManager.getDatabase().rawQuery("SELECT " + LocationTable.Keys.ID + ", " + LocationTable.Keys.BARCODE + ", " + LocationTable.Keys.DATE_TIME + " FROM " + LocationTable.NAME + " ORDER BY " + LocationTable.Keys.ID + " ASC;", null);

            itemCursor.moveToFirst();
            int itemBarcodeIndex = itemCursor.getColumnIndex(InventoryDatabase.BARCODE);
            int itemQuantityIndex = itemCursor.getColumnIndex(InventoryDatabase.QUANTITY);
            int itemLocationIdIndex = itemCursor.getColumnIndex(InventoryDatabase.LOCATION_ID);
            int itemDateTimeIndex = itemCursor.getColumnIndex(InventoryDatabase.DATE_TIME);
            int itemContainerOption = itemCursor.getColumnIndex(InventoryDatabase.CONTAINER_OPEN_STATUS);

            locationCursor.moveToFirst();
            int locationIdIndex = locationCursor.getColumnIndex(InventoryDatabase.ID);
            int locationBarcodeIndex = locationCursor.getColumnIndex(InventoryDatabase.BARCODE);
            int locationDateTimeIndex = locationCursor.getColumnIndex(InventoryDatabase.DATE_TIME);

            PrintStream printStream = null;
            //Log.w(TAG, "All set");
            try {
                Log.w(TAG, "Try action");
                File outputDir = dataManager.getOutputDir();
                outputFile = new File(outputDir.getAbsolutePath(), dataManager.getFileName());
                //outputDir.mkdirs();
                final File TEMP_OUTPUT_FILE = File.createTempFile("tmp", ".txt", outputDir);

                int totalItemCount = itemCursor.getCount() + 1;
                int currentLocationId = -1;

                printStream = new PrintStream(TEMP_OUTPUT_FILE);
                //Cursor locationCursor;

                int tempLocation;
                int itemIndex = 0;
                int updateNum = 0;

                printStream.print(OUTPUT_FILE_HEADER + "\r\n");
                printStream.flush();


                SharedPreferences preferences = getApplicationContext().getSharedPreferences("Inventory_preference", Context.MODE_PRIVATE);
                String emptyLocationSavePreference = preferences.getString("allowEmptyLocation", "");
                itemCursor.moveToFirst();
                locationCursor.moveToFirst();

                if (emptyLocationSavePreference == "true") {
                    Log.v(TAG, "Save true");
                    while(!locationCursor.isAfterLast()) {
                        if (isCancelled()) {
                            Log.w(TAG, "Save cancelled");
                            return "Save canceled";
                        }
                        currentLocationId = locationCursor.getInt(locationIdIndex);
                        printStream.printf("\"%s\"|\"%s\"\r\n", locationCursor.getString(locationBarcodeIndex), locationCursor.getString(locationDateTimeIndex));
                        printStream.flush();

                        itemCursor.moveToFirst();
                        int itemCount = 0;

                        while (!itemCursor.isAfterLast()) {
                            if (itemCursor.getInt(itemLocationIdIndex) == currentLocationId) {
                                if (Config.display_quantity) {
                                    printStream.printf("\"%s\"|\"%s\"|\"%s\"|\"%d\"\r\n", itemCursor.getString(itemBarcodeIndex), itemCursor.getString(itemDateTimeIndex), itemCursor.getString(itemContainerOption), itemCursor.getLong(itemQuantityIndex));
                                } else {
                                    printStream.printf("\"%s\"|\"%s\"|\"%s\"\r\n", itemCursor.getString(itemBarcodeIndex), itemCursor.getString(itemDateTimeIndex), itemCursor.getString(itemContainerOption));
                                }
                                printStream.flush();
                                itemCount ++;
                            }
                            itemCursor.moveToNext();
                        }
                        locationCursor.moveToNext();
                    }
                }
                else {
                    Log.v(TAG, "Save false");
                    while (!itemCursor.isAfterLast()) {
                        if (isCancelled()) {
                            Log.w(TAG, "Save cancelled");
                            return "Save canceled";
                        }

                        final float tempProgress = ((float) itemIndex) / totalItemCount;
                        if (tempProgress * MAX_UPDATES > updateNum) {
                            publishProgress(tempProgress);
                            updateNum++;
                        }
                        tempLocation = itemCursor.getInt(itemLocationIdIndex);

                        if (tempLocation != currentLocationId) {
                            currentLocationId = tempLocation;

                            if (locationCursor.getInt(locationIdIndex) != currentLocationId){
                                locationCursor.moveToFirst();
                            }

                            while (locationCursor.getInt(locationIdIndex) != currentLocationId) {
                                locationCursor.moveToNext();

                                if (locationCursor.isAfterLast()) {
                                    Log.w(TAG, "Location of \"" + itemCursor.getString(itemBarcodeIndex).trim() + "\" does not exist");
                                    return "Location of \"" + itemCursor.getString(itemBarcodeIndex).trim() + "\" does not exist";
                                }
                            }
                            printStream.printf("\"%s\"|\"%s\"\r\n", locationCursor.getString(locationBarcodeIndex), locationCursor.getString(locationDateTimeIndex));
                            printStream.flush();
                        }

                        if (Config.display_quantity) {
                            printStream.printf("\"%s\"|\"%s\"|\"%s\"|\"%d\"\r\n", itemCursor.getString(itemBarcodeIndex), itemCursor.getString(itemDateTimeIndex), itemCursor.getString(itemContainerOption), itemCursor.getLong(itemQuantityIndex));
                        } else {
                            printStream.printf("\"%s\"|\"%s\"|\"%s\"\r\n", itemCursor.getString(itemBarcodeIndex), itemCursor.getString(itemDateTimeIndex), itemCursor.getString(itemContainerOption));
                        }
                        printStream.flush();

                        itemCursor.moveToNext();
                        itemIndex++;
                    }
                }

                if (outputFile.exists() && !outputFile.delete()) {
                    //noinspection ResultOfMethodCallIgnored
                    TEMP_OUTPUT_FILE.delete();
                    Log.w(TAG, "Could not delete existing output file");
                    return "Could not delete existing output file";
                }

                Utils.refreshExternalPath(MainActivity.this.getApplicationContext(), outputFile);

                if (!TEMP_OUTPUT_FILE.renameTo(outputFile)) {
                    TEMP_OUTPUT_FILE.delete();
                    Log.w(TAG, "Could not rename temp file to \"" + outputFile.getName() + "\"");
                    return "Could not rename temp file to \"" + outputFile.getName() + "\"";
                }

                Utils.refreshExternalPath(MainActivity.this.getApplicationContext(), outputFile);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                Log.w(TAG, "ERROR");
                return "FileNotFoundException occurred while saving";
            } catch (IOException e) {
                e.printStackTrace();
                Log.w(TAG, "ERROR");
                return "IOException occurred while saving";
            } finally {

                if (printStream != null)
                    printStream.close();
                itemCursor.close();
                locationCursor.close();
            }
            Log.v(TAG, "Saved to: " + outputFile.getAbsolutePath());
            saveTask = null;
            return "Saved to file";
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (permissions.length != 0 && grantResults.length != 0) {
            for (int i = 0; i < permissions.length; i++) {
                if (permissions[i].equals(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) && grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    if (requestCode == 1) {
                        if (saveTask == null) {
                            preSave();
                            //archiveDatabase();
                            makeToast("Saving...");
                            saveTask = new SaveToFileTask().execute();
                            postSave();
                        } else {
                            saveTask.cancel(false);
                            postSave();
                        }
                    }
                }
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
}