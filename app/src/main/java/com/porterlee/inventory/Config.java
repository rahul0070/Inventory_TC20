package com.porterlee.inventory;

public class Config {
    public static final boolean display_quantity = true;
    public static final String typeContainer_base32Prefix = "A";
    public static final String typeContainer_base64Prefix = "a";
    public static final boolean typeContainer_hasCustodyOf = false;
    public static final boolean typeContainer_hasLabCode = true;
    public static final String[] typeContainer_otherPrefixes = null;
    public static final String typeItem_base32Prefix = "T";
    public static final String typeItem_base64Prefix = "t";
    public static final boolean typeItem_hasCustodyOf = false;
    public static final boolean typeItem_hasLabCode = true;
    public static final String[] typeItem_otherPrefixes = null;
    public static final String typeLocation_base32Prefix = null;
    public static final String typeLocation_base64Prefix = null;
    public static final boolean typeLocation_hasCustodyOf = false;
    public static final boolean typeLocation_hasLabCode = false;
    public static final String[] typeLocation_otherPrefixes = {"L"};

    public static final String typeLIMSItem_base32Prefix = "e1";
    public static final String typeLIMSItem_base64Prefix = "E";
    public static final String typeLIMSCase_base32Prefix = "w1";
    public static final String typeLIMSCase_base64Prefix = "W";
    public static final String typeLIMSContainer_base32Prefix = "m1";
    public static final String typeLIMSContainer_base64Prefix = "M";
    public static final String typeLIMSLocation_base32Prefix = "V";
    public static final String typeLIMSLocation__base64Prefix = "V";

    public static final String typeLAMItem_base64Prefix = "J";
    public static final String typeLAMItem_base32Prefix = "j1";

    public static final String INVENTORY_DIR_NAME = "Inventory";
}
