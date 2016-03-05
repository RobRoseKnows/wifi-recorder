package io.robrose.hack.wifirecorder.data;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.net.Uri;
import android.provider.BaseColumns;

/**
 * This is the contract for the signal database. It holds all the constants for the database.
 * @author Robert Rose
 */
public class SignalContract {
    public static final String CONTENT_AUTHORITY = "io.robrose.hack.wifirecorder";

    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);

    public static final String PATH_SIGNAL = "signal";

    public static final class SignalEntry implements BaseColumns {
        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_SIGNAL).build();
        public static final String CONTENT_TYPE =
                ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_SIGNAL;
        public static final String CONTENT_ITEM_TYPE =
                ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_SIGNAL;

        public static final String TABLE_NAME = "signal";

        // The SSID of the network
        public static final String COLUMN_SSID = "ssid";
        // The location of the access point as a String
        public static final String COLUMN_LOCATION = "location";
        // The BSSID address of the access point as a String
        public static final String COLUMN_MAC = "mac";
        // The signal strength in dBm
        public static final String COLUMN_STRENGTH = "strength";
        // Frequency in MHz
        public static final String COLUMN_FREQ = "freq";
        // Latitude
        public static final String COLUMN_LAT = "latitude";
        // Longitude
        public static final String COLUMN_LONG = "longitude";

        public static Uri buildSignalLocation(String loc) {
            return CONTENT_URI.buildUpon().appendPath(loc).build();
        }
    }
}
