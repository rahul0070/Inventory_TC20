package com.porterlee.inventory;

import android.database.sqlite.SQLiteDatabase;

public class InventoryDatabase {
    public static final String FILE_NAME = "inventory.db";
    public static final String ID = "id";
    public static final String ITEM_ID = "item_id";
    public static final String PICTURE = "picture";
    public static final String BARCODE = "barcode";
    public static final String QUANTITY = "quantity";
    public static final String LOCATION_ID = "location";
    public static final String DESCRIPTION = "description";
    public static final String TAGS = "tags";
    public static final String DATE_TIME = "datetime";
    public static final String ITEM_BARCODE_INDEX = "item_barcode_index";
    public static final String LOCATION_BARCODE_INDEX = "location_barcode_index";
    public static final String CONTAINER_OPEN_STATUS = "container_status";
    public static final String CONTAINER_ID = "container_id";

    public static class ItemTable {
        public static final String NAME = "items";

        static public void create(SQLiteDatabase database) {
            database.execSQL("CREATE TABLE IF NOT EXISTS " + NAME + " ( " + ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " + BARCODE + " TEXT NOT NULL, " + QUANTITY + " INTEGER DEFAULT 1, " + LOCATION_ID + " INTEGER NOT NULL, " + DESCRIPTION + " TEXT NOT NULL, " + TAGS + " TEXT NOT NULL, " + DATE_TIME + " TEXT NOT NULL, " + CONTAINER_OPEN_STATUS + " TEXT NOT NULL, " + CONTAINER_ID + " INTEGER)");
            database.execSQL("CREATE INDEX IF NOT EXISTS " + ITEM_BARCODE_INDEX + " ON " + NAME + " ( " + BARCODE + " );");
        }

        public class Keys {
            public static final String ID = NAME + '.' + InventoryDatabase.ID;
            public static final String BARCODE = NAME + '.' + InventoryDatabase.BARCODE;
            public static final String QUANTITY = NAME + '.' + InventoryDatabase.QUANTITY;
            public static final String LOCATION_ID = NAME + '.' + InventoryDatabase.LOCATION_ID;
            public static final String DESCRIPTION = NAME + '.' + InventoryDatabase.DESCRIPTION;
            public static final String TAGS = NAME + '.' + InventoryDatabase.TAGS;
            public static final String DATE_TIME = NAME + '.' + InventoryDatabase.DATE_TIME;
            public static final String CONTAINER_STATUS = NAME + '.' + InventoryDatabase.CONTAINER_OPEN_STATUS;
            public static final String CONTAINER_ID = NAME + '.' + InventoryDatabase.CONTAINER_ID;
        }
    }

    public static class LocationTable {
        public static final String NAME = "locations";

        static public void create(SQLiteDatabase database) {
            database.execSQL("CREATE TABLE IF NOT EXISTS " + NAME + " ( " + ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " + BARCODE + " TEXT NOT NULL, " + DESCRIPTION + " TEXT NOT NULL, " + TAGS + " TEXT NOT NULL, " + DATE_TIME + " TEXT NOT NULL )");
            database.execSQL("CREATE INDEX IF NOT EXISTS " + LOCATION_BARCODE_INDEX + " ON " + NAME + " ( " + BARCODE + " );");
        }

        public class Keys {
            public static final String ID = NAME + '.' + InventoryDatabase.ID;
            public static final String BARCODE = NAME + '.' + InventoryDatabase.BARCODE;
            public static final String DESCRIPTION = NAME + '.' + InventoryDatabase.DESCRIPTION;
            public static final String TAGS = NAME + '.' + InventoryDatabase.TAGS;
            public static final String DATE_TIME = NAME + '.' + InventoryDatabase.DATE_TIME;
        }

        public class Tags {
            public static final String WARNING = "W";
            public static final String ERROR = "E";
        }
    }

    /*public class PicturesTable {
        public static final String NAME = "pictures";
        public static final String TABLE_CREATION = NAME + " ( " + ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " + ITEM_ID + " INTEGER, " + PICTURE + " BLOB, " + DESCRIPTION + " TEXT, " + TAGS + " TEXT, " + DATE_TIME + " TEXT );";
        public class Keys {
            public static final String ID = NAME + '.' + InventoryDatabase.ID;
            public static final String ITEM_ID = NAME + '.' + InventoryDatabase.ITEM_ID;
            public static final String PICTURE = NAME + '.' + InventoryDatabase.PICTURE;
            public static final String DESCRIPTION = NAME + '.' + InventoryDatabase.DESCRIPTION;
            public static final String TAGS = NAME + '.' + InventoryDatabase.TAGS;
            public static final String DATE_TIME = NAME + '.' + InventoryDatabase.DATE_TIME;
        }
    }*/
}
