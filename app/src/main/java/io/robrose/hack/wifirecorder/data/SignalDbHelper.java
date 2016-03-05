package io.robrose.hack.wifirecorder.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import io.robrose.hack.wifirecorder.data.SignalContract.SignalEntry;

/**
 * Created by Robert on 3/5/2016.
 * Borrowed from: https://github.com/udacity/Sunshine-Version-2/blob/4.23_fix_settings/app/src/main/java/com/example/android/sunshine/app/data/WeatherDbHelper.java
 */
public class SignalDbHelper extends SQLiteOpenHelper {
        private static final int DATABASE_VERSION = 1;

        static final String DATABASE_NAME = "weather.db";

        public SignalDbHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase sqLiteDatabase) {
            final String SQL_CREATE_SIGNAL_TABLE = "CREATE TABLE " + SignalEntry.TABLE_NAME + " (" +
                    SignalEntry._ID + " INTEGER PRIMARY KEY," +
                    SignalEntry.COLUMN_SSID + " TEXT NOT NULL, " +
                    SignalEntry.COLUMN_LOCATION + " TEXT NOT NULL, " +
                    SignalEntry.COLUMN_MAC + " TEXT NOT NULL, " +
                    SignalEntry.COLUMN_STRENGTH + " INTEGER NOT NULL, " +
                    SignalEntry.COLUMN_FREQ + " INTEGER, " +
                    SignalEntry.COLUMN_LAT + " REAL, " +
                    SignalEntry.COLUMN_LONG + " REAL, " +
                    SignalEntry.COLUMN_ALT + " REAL " +
                    " );";

            sqLiteDatabase.execSQL(SQL_CREATE_SIGNAL_TABLE);
        }

        @Override
        public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
            sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + SignalEntry.TABLE_NAME);
            onCreate(sqLiteDatabase);
        }
}
