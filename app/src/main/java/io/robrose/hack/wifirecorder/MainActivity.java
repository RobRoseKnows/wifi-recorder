package io.robrose.hack.wifirecorder;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;

import java.util.List;

import butterknife.Bind;
import butterknife.OnClick;
import io.robrose.hack.wifirecorder.data.SignalContract;

public class MainActivity extends AppCompatActivity
        implements GoogleApiClient.OnConnectionFailedListener, GoogleApiClient.ConnectionCallbacks {

    private WifiManager wifiManager;
    private ScanReceiver scanReceiver;
    private GoogleApiClient mGoogleApiClient;
    private Location mLastLocation;
    private boolean currentlyChecking = false;

    @Bind(R.id.resultsListView) ListView resultsListView;
    @Bind(R.id.locationField) EditText locationField;
    @Bind(R.id.resultsTextView) TextView resultsTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }
        setContentView(R.layout.activity_main);

        wifiManager=(WifiManager)getSystemService(Context.WIFI_SERVICE);
        scanReceiver = new ScanReceiver();
    }

    protected void onStart() {
        mGoogleApiClient.connect();
        super.onStart();
    }

    protected void onResume() {
        registerReceiver(scanReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        super.onResume();
    }

    protected void onPause() {
        unregisterReceiver(scanReceiver);
        super.onPause();
    }

    protected void onStop() {
        mGoogleApiClient.disconnect();
        super.onStop();
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        // An unresolvable error has occurred and a connection to Google APIs
        // could not be established. Display an error message, or handle
        // the failure silently

        // ...
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        int permissionCheck = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION);
        if(permissionCheck == 0) {
            mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @OnClick(R.id.captureButton)
    private void recordLocation() {
        // Make sure we can't spam button and get too many results.
        if(!currentlyChecking) {
            currentlyChecking = true;
            int permissionCheckLocation = ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION);
            if(permissionCheckLocation == 0) {
                mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
            }

            int permissionCheckWifi = ContextCompat.checkSelfPermission(this,
                    Manifest.permission.CHANGE_WIFI_STATE);
            if(permissionCheckWifi == 0) {
                wifiManager.startScan();
            }
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
                int signalFreq = resultOn.centerFreq0;
                double lastLat = mLastLocation.getLatitude();
                double lastLong = mLastLocation.getLongitude();
                double lastAlt = mLastLocation.getAltitude();

                String toStringValue = "SSID: " + ssid + " MAC: " + macAddress + " Signal: " +
                        signalStrength + "dBm";
                wifiText[i] = toStringValue;

                ContentValues cvs = new ContentValues();
                cvs.put(SignalContract.SignalEntry.COLUMN_LOCATION, location);
                cvs.put(SignalContract.SignalEntry.COLUMN_SSID, ssid);
                cvs.put(SignalContract.SignalEntry.COLUMN_MAC, macAddress);
                cvs.put(SignalContract.SignalEntry.COLUMN_STRENGTH, signalStrength);
                cvs.put(SignalContract.SignalEntry.COLUMN_FREQ, signalFreq);
                cvs.put(SignalContract.SignalEntry.COLUMN_LAT, lastLat);
                cvs.put(SignalContract.SignalEntry.COLUMN_LONG, lastLong);
                cvs.put(SignalContract.SignalEntry.COLUMN_ALT, lastAlt);

                results[i] = cvs;
            }

            resultsListView.setAdapter(new ArrayAdapter<String>(getApplicationContext(),
                    android.R.layout.simple_list_item_1, wifiText));
            getContentResolver().bulkInsert(SignalContract.BASE_CONTENT_URI, results);
            currentlyChecking = false;
        }
    }
}
