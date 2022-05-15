package com.porterlee.inventory;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteCantOpenDatabaseException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.os.Environment;
import android.text.format.DateFormat;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

public class DataManager extends MainActivity {
    private File databaseFile;
    private SQLiteDatabase mDatabase;
    private Context context;

    private File outputFile;
    private File archiveDirectory;
    private File outputDir;
    private File internalDir;

    private SQLiteStatement IS_DUPLICATE_STATEMENT;
    private SQLiteStatement LAST_ITEM_BARCODE_STATEMENT;
    private SQLiteStatement TOTAL_ITEM_COUNT;
    private SQLiteStatement TOTAL_LOCATION_COUNT;
    private SQLiteStatement UPDATE_QUANTITY;

    private static final String INVENTORY_DIR_NAME = Config.INVENTORY_DIR_NAME;
    private static final String ARCHIVE_DIR_NAME = "Archives";

    private static final String DATE_FORMAT = "yyyy/MM/dd kk:mm:ss";
    private static final String TAG = MainActivity.class.getSimpleName();

    private SharedPreferences preferences;

    public DataManager(Context context) {
        this.context = context;
    }

    public void initializeDB() {
        // Function to initialize database.
        internalDir = new File(context.getFilesDir(), INVENTORY_DIR_NAME);
        databaseFile = new File(internalDir, InventoryDatabase.FILE_NAME);
        mDatabase = SQLiteDatabase.openOrCreateDatabase(databaseFile, null);

        InventoryDatabase.ItemTable.create(mDatabase);
        InventoryDatabase.LocationTable.create(mDatabase);

        IS_DUPLICATE_STATEMENT = mDatabase.compileStatement("SELECT COUNT(*) FROM " + InventoryDatabase.ItemTable.NAME + " WHERE " + InventoryDatabase.ItemTable.Keys.LOCATION_ID + " IN ( SELECT " + InventoryDatabase.LocationTable.Keys.ID + " FROM " + InventoryDatabase.LocationTable.NAME + " WHERE " + InventoryDatabase.LocationTable.Keys.BARCODE + " IN ( SELECT " + InventoryDatabase.LocationTable.Keys.BARCODE + " FROM " + InventoryDatabase.LocationTable.NAME + " WHERE " + InventoryDatabase.LocationTable.Keys.ID + " = ? ) ) AND " + InventoryDatabase.ItemTable.Keys.BARCODE + " = ?;");
        LAST_ITEM_BARCODE_STATEMENT = mDatabase.compileStatement("SELECT " + InventoryDatabase.ItemTable.Keys.BARCODE + " FROM " + InventoryDatabase.ItemTable.NAME + " ORDER BY " + InventoryDatabase.ItemTable.Keys.ID + " DESC LIMIT 1;");
        TOTAL_ITEM_COUNT = mDatabase.compileStatement("SELECT COUNT(*) FROM " + InventoryDatabase.ItemTable.NAME);
        TOTAL_LOCATION_COUNT = mDatabase.compileStatement("SELECT COUNT(*) FROM " + InventoryDatabase.LocationTable.NAME);
        UPDATE_QUANTITY = mDatabase.compileStatement("UPDATE " + InventoryDatabase.ItemTable.NAME + " SET " + InventoryDatabase.QUANTITY + " = ? WHERE " + InventoryDatabase.ID + " = ?");

    }

    public boolean initializeFiles() {
        // Initialize text files (Final output files) and call function to load database.
        File path = context.getExternalFilesDir(null);
        File directory = context.getFilesDir();
        Log.w(TAG, String.valueOf(directory));
        archiveDirectory = new File(directory, ARCHIVE_DIR_NAME);

        archiveDirectory.mkdirs();
        outputDir = new File(Environment.getExternalStorageDirectory(), INVENTORY_DIR_NAME);

        outputDir.mkdirs();
        outputFile = new File(outputDir.getAbsolutePath(), getFileName());
        internalDir = new File(directory, INVENTORY_DIR_NAME);

        internalDir.mkdirs();
        databaseFile = new File(internalDir, InventoryDatabase.FILE_NAME);

        try {
            initializeDB();
            return true;
        } catch (SQLiteCantOpenDatabaseException e) {
            try {
                //System.out.println(databaseFile.exists());
                if (databaseFile.renameTo(File.createTempFile("error", ".db", archiveDirectory))) {

                } else {
                    return false;
                }
            } catch (IOException e1) {
                e1.printStackTrace();
                return false;
            }
        }
        return true;
    }

    public File getOutputDir() {
        return outputDir;
    }


    public boolean deleteDB() {
        return databaseFile.delete();
    }

    public boolean checkIfDuplicate(long locationId, String barcode) {
        IS_DUPLICATE_STATEMENT.bindLong(1, locationId);
        IS_DUPLICATE_STATEMENT.bindString(2, barcode);
        return IS_DUPLICATE_STATEMENT.simpleQueryForLong() > 0;
    }

    public boolean isEmpty() {
        if (noItems() && noLocation()) {
            return true;
        }
        else {
            return false;
        }
    }

    public boolean noItems() {
        if (TOTAL_ITEM_COUNT.simpleQueryForLong() < 1) {
            return true;
        }
        else {
            return false;
        }
    }

    public boolean noLocation() {
        if (TOTAL_LOCATION_COUNT.simpleQueryForLong() < 1) {
            return true;
        }
        else {
            return false;
        }
    }

    public long insertLocationData(String barcode) {
        ContentValues newLocation = new ContentValues();
        newLocation.put(InventoryDatabase.BARCODE, barcode);
        newLocation.put(InventoryDatabase.DESCRIPTION, "");
        newLocation.put(InventoryDatabase.TAGS, "");
        newLocation.put(InventoryDatabase.DATE_TIME, String.valueOf(formatDate(System.currentTimeMillis())));

        long rowID = mDatabase.insert(InventoryDatabase.LocationTable.NAME, null, newLocation);
        return rowID;
    }

    public long insertItemData(String barcode, long locationId, String containerOption, long container_id) {
        ContentValues newItem = new ContentValues();
        newItem.put(InventoryDatabase.BARCODE, barcode);
        newItem.put(InventoryDatabase.LOCATION_ID, locationId);
        newItem.put(InventoryDatabase.DESCRIPTION, "");
        newItem.put(InventoryDatabase.TAGS, "");
        newItem.put(InventoryDatabase.DATE_TIME, String.valueOf(formatDate(System.currentTimeMillis())));
        newItem.put(InventoryDatabase.CONTAINER_OPEN_STATUS, containerOption);
        newItem.put(InventoryDatabase.CONTAINER_ID, container_id);

        long rowID = mDatabase.insert(InventoryDatabase.ItemTable.NAME, null, newItem);
        return rowID;
    }

    private CharSequence formatDate(long millis) {
        return DateFormat.format(DATE_FORMAT, millis).toString();
    }

    public void deleteLocationID(int locationId) {
        mDatabase.delete(InventoryDatabase.ItemTable.NAME, "location = ?", new String[]{String.valueOf(locationId)});
    }

    public Cursor getLocationCursor() {
        // Returns Cursor of Location database.
        String tableName = InventoryDatabase.LocationTable.NAME;
        Cursor c = mDatabase.rawQuery("SELECT * FROM " + tableName, null);
        return c;
    }

    public String getCount(){
        // Returns total number of Items scanned.
        String tableName = InventoryDatabase.ItemTable.NAME;
        Cursor c = mDatabase.rawQuery("SELECT * FROM " + tableName, null);
        int count = c.getCount();
        if (count == 0){
            return "";
        }
        else{
            String data = String.valueOf(count);
            return data;
        }
    }

    public String getFileName() {
        return "data.txt";
//        if (BuildConfig.FLAVOR.equals("LIMS")) {
//            return "LIMS_Inventory.txt";
//        }
//        else if (BuildConfig.FLAVOR.equals("EMS")) {
//            return "EMS_Inventory.txt";
//        }
//        return "ERROR.txt";
    }

    public SQLiteDatabase getDatabase() {
        return mDatabase;
    }

    public boolean deleteEntry(String name, String tableName) {
        // When user prompts to remove an item from Item barcode field.
        boolean value = mDatabase.delete(tableName, "BARCODE = ?", new String[]{name}) > 0;
        return value;
    }

    public void testDatabaseWithLog() throws IOException {
        // Debugger function to print out database to a text file.
        String tableName = InventoryDatabase.ItemTable.NAME;
        String locationTableName = InventoryDatabase.LocationTable.NAME;
        Cursor c = mDatabase.rawQuery("SELECT * FROM " + tableName, null);
        File path = context.getExternalFilesDir(null);
        File testFile = new File(path, "database_test.txt");

        FileOutputStream clearContent = new FileOutputStream(testFile, false);
        clearContent.write(" ".getBytes());
        clearContent.close();

        while(c.moveToNext()){
            FileOutputStream stream = new FileOutputStream(testFile, true);
            try {
                String data = c.getString(0) + "-" + c.getString(1) + "-" + c.getString(2) + "-" +  c.getString(3) + "-" + c.getString(7) + "-" + c.getString(8);
                stream.write(data.getBytes());
                String newLine = "\n";
                stream.write(newLine.getBytes());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        Cursor c2 = mDatabase.rawQuery("SELECT * FROM " + locationTableName, null);

        while(c2.moveToNext()){
            FileOutputStream stream = new FileOutputStream(testFile, true);
            try {
                String data = c2.getString(0) + "-" + c2.getString(1) + "-" + c2.getString(2) + "-" +  c2.getString(3);
                stream.write(data.getBytes());
                String newLine = "\n";
                stream.write(newLine.getBytes());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    public void resetTables() {
            mDatabase.execSQL("DROP TABLE IF EXISTS " + InventoryDatabase.ItemTable.NAME);
            InventoryDatabase.ItemTable.create(mDatabase);

            mDatabase.execSQL("DROP TABLE IF EXISTS " + InventoryDatabase.LocationTable.NAME);
            InventoryDatabase.LocationTable.create(mDatabase);
    }

    public String locationBarcodeValue(long index){
        // Returns location Barcode when given the location Index
        Cursor c = getLocationCursor();
        c.moveToLast();

        if (index == -1) {
            try {
                index = c.getInt(0);
                Log.w(TAG, "Location ID in recycler" + index);
                return c.getString(1);
            } catch (Exception e) {
                Log.w(TAG, "Location database empty");
                return "";
            }
        }

        c.moveToFirst();
        if (c.getCount() == 0){
            return "";
        }
        do{
            if (c.getInt(0) == index){
                return c.getString(1);
            }
        }while(c.moveToNext());
        return "";
    }

    public int checkIfExists(String locationBarcode){
        // Function checks if the scanned location barcode already exists in db.
        Log.v(TAG, "Checking if location exists in database");
        String tableName = InventoryDatabase.LocationTable.NAME;
        Cursor c1 = mDatabase.rawQuery("SELECT * FROM " + tableName, null);
        c1.moveToFirst();
        String currentBarcode;
        String checkingBarcode = locationBarcode.trim();
        int barcodeIndex = c1.getColumnIndex(InventoryDatabase.BARCODE);
        int IDIndex = c1.getColumnIndex(InventoryDatabase.ID);
        Log.v(TAG, "Here" + c1.getCount());
        do{
            currentBarcode = c1.getString(barcodeIndex).trim();
            if (currentBarcode.equals(checkingBarcode)){
                Log.v(TAG, "Exists");
                return c1.getInt(0);
            }
        }while(c1.moveToNext());
        return -1;
    }

    public long getContainerId() {
        long id = 0;
        int value;
        Cursor c = mDatabase.rawQuery("SELECT * FROM " + InventoryDatabase.ItemTable.NAME, null);
        c.moveToFirst();
        int index = c.getColumnIndex(InventoryDatabase.CONTAINER_ID);
        int indexContainer = c.getColumnIndex(InventoryDatabase.CONTAINER_OPEN_STATUS);
        if (c.getCount() == 0) {
            return 1;
        }
        do {
            value = c.getInt(index);
            if (value > id) {
                if (c.getString(indexContainer).equals("")){
                    id = value;
                }
            }
        } while(c.moveToNext());
//        Log.v(TAG, "HERE-22 " + String.valueOf(id));
        id += 1;
        return id;
    }

    public long getContainerId(String barcode) {
        Cursor c = getItemCursor();
        int index = c.getColumnIndex(InventoryDatabase.BARCODE);
        int cIndex = c.getColumnIndex(InventoryDatabase.CONTAINER_ID);
        do {
            if(barcode.equals(c.getString(index))) {
                return c.getInt(cIndex);
            }
        }while (c.moveToNext());
        return 0;
    }

    public boolean isOpenContainer(String barcode) {
        Cursor c = getItemCursor();
        int index = c.getColumnIndex(InventoryDatabase.BARCODE);
        int cIndex = c.getColumnIndex(InventoryDatabase.CONTAINER_OPEN_STATUS);

        do {
            if(barcode.equals(c.getString(index))) {
                if (c.getString(cIndex).equals("Y")) {
                    return true;
                }
            }
        }while (c.moveToNext());
        return false;
    }

    public ArrayList<String> getItemsInContainer(long containerId) {
        Log.v(TAG, "HERE-IS");
        ArrayList<String> barcodeList = new ArrayList<>();
        Cursor c = getItemCursor();
        int index = c.getColumnIndex(InventoryDatabase.CONTAINER_ID);
        int cIndex = c.getColumnIndex(InventoryDatabase.CONTAINER_OPEN_STATUS);
        int bIndex = c.getColumnIndex(InventoryDatabase.BARCODE);
        do{
            Log.v(TAG, "HERE-II" + String.valueOf(c.getInt(index)) + " : " + String.valueOf(containerId));
            if (c.getInt(index) == (int) containerId) {

                if (c.getString(cIndex).equals("")){
                    Log.v(TAG, "HERE-IO");
                    barcodeList.add(c.getString(bIndex));
                }
            }
        }while(c.moveToNext());
        return barcodeList;
    }

    public Cursor getItemCursor() {
        Cursor c = mDatabase.rawQuery("SELECT * FROM " + InventoryDatabase.ItemTable.NAME, null);
        c.moveToFirst();
        return c;
    }

    public void setPreferences() {
        Log.v(TAG, "pref set");
        preferences = context.getSharedPreferences("Inventory_preference", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        if(!preferences.contains("allowDuplicates")){
            Log.v(TAG, "new1");
            editor.putString("allowDuplicates", "true");
            editor.putString("allowExternal", "true");
            editor.putString("allowEmptyLocation", "false");
            editor.commit();
        }
    }

    public boolean allowDuplicates() {
        String option = preferences.getString("allowDuplicates", "");
        if (option.equals("true")) return true;
        else if (option.equals("false")) return false;
        else return false;
    }

    public boolean allowExternal() {
        String option = preferences.getString("allowExternal", "");
        if (option.equals("true")) return true;
        else if (option.equals("false")) return false;
        else return false;
    }

    public boolean noData() {
        Cursor c = getLocationCursor();
        c.moveToFirst();
        if (c.getCount() == 0) return true;
        else return false;
    }

    public SharedPreferences getPreference() {
        return preferences;
    }

    public boolean identifyLocationBarcode(String barcode) {
        boolean result = false;
        if (BuildConfig.FLAVOR.equals("EMS")) result = BarcodeType.Location.isOfType(barcode);
        else if (BuildConfig.FLAVOR.equals("LIMS")) result = BarcodeType.LocationLIMS.isOfType(barcode);
        else if (BuildConfig.FLAVOR.equals("LAM")) result = BarcodeType.LocationLIMS.isOfType(barcode);
        return result;
    }

    public boolean identifyEMSContainer(String barcode) {
        if (BarcodeType.Container.isOfType(barcode)) {
            return true;
        }
        return false;
    }

    public boolean identifyContainerBarcode(String barcode) {
        boolean result = true;
        if (BuildConfig.FLAVOR.equals("EMS")) {
            result = BarcodeType.Container.isOfType(barcode);
        }
        else if (BuildConfig.FLAVOR.equals("LIMS")) {
            result = BarcodeType.ContainerLIMS.isOfType(barcode);
        }
        else if (BuildConfig.FLAVOR.equals("LAM")) {
            result = BarcodeType.ContainerLIMS.isOfType(barcode);
        }
        return result;
    }

    public boolean acceptedItem(String barcode) {
        boolean result = true;
        if (BuildConfig.FLAVOR.equals("EMS")) {
            result = BarcodeType.Container.isOfType(barcode) || BarcodeType.Item.isOfType(barcode);
        }
        else if (BuildConfig.FLAVOR.equals("LIMS")) {
            result = BarcodeType.ContainerLIMS.isOfType(barcode) || BarcodeType.ItemLIMS.isOfType(barcode) || BarcodeType.CaseLIMS.isOfType(barcode);
        }
        else if (BuildConfig.FLAVOR.equals("LAM")) {
            result = BarcodeType.ContainerLIMS.isOfType(barcode) || BarcodeType.ItemLAM.isOfType(barcode) || BarcodeType.CaseLIMS.isOfType(barcode);
        }
        return result;
    }


    public boolean identifyLIMSContainer(String barcode) {
        if (BarcodeType.ContainerLIMS.isOfType(barcode)) {
            return true;
        }
        return false;
    }

    public boolean identifyLAMContainer(String barcode) {
        if (BarcodeType.ContainerLIMS.isOfType(barcode)) return true;
        else return false;
    }

    public int getQuantity(long locationId, String barcode) {
        Cursor c = getItemCursor();
        int bIndex = c.getColumnIndex(InventoryDatabase.BARCODE);
        int locIndex = c.getColumnIndex(InventoryDatabase.LOCATION_ID);
        int quantityIndex = c.getColumnIndex(InventoryDatabase.QUANTITY);
        do {
            if (c.getString(bIndex).equals(barcode) && c.getLong(locIndex) == locationId){
                return c.getInt(quantityIndex);
            }
        }
        while(c.moveToNext());
        return 0;
    }

    public boolean updateQuantity(long locationId, String barcode, int quantity) {
        ContentValues newValues = new ContentValues();
        newValues.put(InventoryDatabase.ItemTable.Keys.QUANTITY, quantity);

        String quantityColumn = "quantity";
        String barcodeValue = "barcode";
        String locValue = "location";
        SQLiteStatement statement = mDatabase.compileStatement("UPDATE " + InventoryDatabase.ItemTable.NAME + " SET " + quantityColumn + " = ? WHERE " + barcodeValue + " = ? AND " + locValue + " = ? ");
        statement.bindString(2, barcode);
        statement.bindLong(1, quantity);
        statement.bindLong(3, locationId);
        statement.execute();
        return true;
    }
}
