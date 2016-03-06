package io.robrose.hack.wifirecorder;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Environment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import java.io.File;
import java.io.FileWriter;
import java.util.List;

import au.com.bytecode.opencsv.CSVWriter;
import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.robrose.hack.wifirecorder.data.SignalContract;
import io.robrose.hack.wifirecorder.data.SignalDbHelper;

public class MainActivity extends AppCompatActivity {

    private WifiManager wifiManager;
    private ScanReceiver scanReceiver;
    private boolean currentlyChecking = false;

    @Bind(R.id.resultsListView) ListView resultsListView;
    @Bind(R.id.locationField) EditText locationField;
    @Bind(R.id.resultsTextView) TextView resultsTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        wifiManager=(WifiManager)getSystemService(Context.WIFI_SERVICE);
        scanReceiver = new ScanReceiver();

    }

    protected void onResume() {
        registerReceiver(scanReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        super.onResume();
    }

    protected void onPause() {
        unregisterReceiver(scanReceiver);
        super.onPause();
    }

    @OnClick(R.id.captureButton) void recordLocation() {
        // Make sure we can't spam button and get too many results.
        if(!currentlyChecking) {
            currentlyChecking = true;

            int permissionCheckWifi = ContextCompat.checkSelfPermission(this,
                    Manifest.permission.CHANGE_WIFI_STATE);
            if(permissionCheckWifi == 0) {
                wifiManager.startScan();
            }
        }
    }

    @OnClick(R.id.exportButton) void exportDB() {

        File dbFile = getDatabasePath("weather.db");
        SignalDbHelper dbhelper = new SignalDbHelper(getApplicationContext());
        File exportDir = new File(Environment.getExternalStorageDirectory(), "");
        if (!exportDir.exists()) {
            exportDir.mkdirs();
        }

        File file = new File(exportDir, "apexport.csv");
        try {
            file.createNewFile();
            CSVWriter csvWrite = new CSVWriter(new FileWriter(file));
            SQLiteDatabase db = dbhelper.getReadableDatabase();
            Cursor curCSV = db.rawQuery("SELECT * FROM signal", null);
            csvWrite.writeNext(curCSV.getColumnNames());
            while (curCSV.moveToNext()) {
                //Which column you want to exprort
                String arrStr[] = {curCSV.getString(0), curCSV.getString(1), curCSV.getString(2),
                        curCSV.getString(3), curCSV.getString(4), curCSV.getString(5)};
                csvWrite.writeNext(arrStr);
            }
            csvWrite.close();
            curCSV.close();
        } catch (Exception sqlEx) {
            Log.e("MainActivity", sqlEx.getMessage(), sqlEx);
        }
    }

    /**
     * This BroadcastReceiver handles what happens after the Wifi Scan completes. It displays
     * the information and adds it to the SQLite database.
     */
    private class ScanReceiver extends BroadcastReceiver {
        public void onReceive(Context context, Intent intent) {
            List<ScanResult> wifiList = wifiManager.getScanResults();
            String[] wifiText = new String[wifiList.size()];
            ContentValues[] results = new ContentValues[wifiList.size()];

            // This is not my favorite way to do this because the location field could change
            // during the time it takes to scan. The solution is not to do that.
            String location = String.valueOf(locationField.getText());
            resultsTextView.setText("Found " + wifiList.size() + " APs at " + location);

            for(int i = 0; i < wifiList.size(); i++) {
                ScanResult resultOn = wifiList.get(i);
                String macAddress = resultOn.BSSID;
                String ssid = resultOn.SSID;
                int signalStrength = resultOn.level;

                String toStringValue = "SSID: " + ssid + " MAC: " + macAddress + " Signal: " +
                        signalStrength + "dBm";
                wifiText[i] = toStringValue;

                ContentValues cvs = new ContentValues();
                cvs.put(SignalContract.SignalEntry.COLUMN_LOCATION, location);
                cvs.put(SignalContract.SignalEntry.COLUMN_SSID, ssid);
                cvs.put(SignalContract.SignalEntry.COLUMN_MAC, macAddress);
                cvs.put(SignalContract.SignalEntry.COLUMN_STRENGTH, signalStrength);

                results[i] = cvs;
            }

            resultsListView.setAdapter(new ArrayAdapter<String>(getApplicationContext(),
                    android.R.layout.simple_list_item_1, wifiText));
            getContentResolver().bulkInsert(SignalContract.SignalEntry.CONTENT_URI, results);
            currentlyChecking = false;
        }
    }
}
