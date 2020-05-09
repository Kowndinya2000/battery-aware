package com.dell.battery_aware;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.BatteryManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;

import java.text.DateFormat;
import java.util.Date;
public class working extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {
    double total_distance = 0;
    protected static final String TAG = "location-updates-sample";
    private static  long UPDATE_INTERVAL_IN_MILLISECONDS = 13000;
    public static long getUPDATE_INTERVAL_IN_MILLISECONDS(){
        return UPDATE_INTERVAL_IN_MILLISECONDS;
    }
    private static  long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS =
            UPDATE_INTERVAL_IN_MILLISECONDS / 2;
    public boolean global_button = false;
    private final static String REQUESTING_LOCATION_UPDATES_KEY = "requesting-location-updates-key";
    private final static String LOCATION_KEY = "location-key";
    private final static String LAST_UPDATED_TIME_STRING_KEY = "last-updated-time-string-key";

    public static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;
    private static final int REQUEST_CHECK_SETTINGS = 10;
    public GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;
    private Location mCurrentLocation;
    private String mLastUpdateTime;
    private String percentageLabel,timeRunning;

    protected class Interval
    {
       protected long MIN_INTERVAL;
       protected long STEP;
       protected FunctionWrap global_wrap;
       protected final FunctionWrap LINEAR = () ->   MIN_INTERVAL+(STEP*(100-Long.parseLong(getCurrentBatteryLevel())));
       protected final FunctionWrap EXPONENTIAL = () -> MIN_INTERVAL+((long)(Math.exp(STEP))*(100-Long.parseLong(getCurrentBatteryLevel())));
       protected boolean runInBackground;
       protected long MAGNITUDE = 0;
       protected boolean inForeGround = true;
       protected boolean isCharging;
    }


    protected Interval setUpIntervalObject = new Interval();
    protected void runAppInBackground(boolean flag,long Magnitude)
    {
        if(Magnitude < 1) setUpIntervalObject.MAGNITUDE = 1;
        setUpIntervalObject.MAGNITUDE = Magnitude;
        setUpIntervalObject.runInBackground = flag;
    }
    protected void setUpLocationService(FunctionWrap func,long MIN_INTERVAL, long STEP)
    {
        setUpIntervalObject.global_wrap = func;
        setUpIntervalObject.MIN_INTERVAL = MIN_INTERVAL;
        setUpIntervalObject.STEP = STEP;
    }

    protected long trigger()
    {
        if(setUpIntervalObject.isCharging)
        {
          return setUpIntervalObject.MIN_INTERVAL;
        }
        if(setUpIntervalObject.inForeGround)
        {
            return setUpIntervalObject.global_wrap.execute();
        }
        return (setUpIntervalObject.global_wrap.execute() * Math.abs(setUpIntervalObject.MAGNITUDE));
    }
    interface FunctionWrap
    {
        long execute();
    }
    public void onBatteryLevelChanged()
    {

    }
    BroadcastReceiver BatteryInfoReceiver = new BroadcastReceiver(){
        @Override
        public void onReceive(Context ctxt, Intent intent) {
            int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
            int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS,-1);
            boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL;
            setUpIntervalObject.isCharging = isCharging;
            String val = String.valueOf(level);
            int per = Integer.parseInt(val);
            long rem = 100 - per;
            percentageLabel = String.valueOf(level);
            UPDATE_INTERVAL_IN_MILLISECONDS = trigger();
            Log.i("Running in Foreground: ", String.valueOf(UPDATE_INTERVAL_IN_MILLISECONDS));
            timeRunning = String.valueOf(UPDATE_INTERVAL_IN_MILLISECONDS);
            onBatteryLevelChanged();
            if (mGoogleApiClient.isConnected() && global_button) {
                startLocationUpdates();
            }
        }
    };
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {

        super.onCreate(savedInstanceState);
        mLastUpdateTime = "";
        updateValuesFromBundle(savedInstanceState);
        buildGoogleApiClient();
        this.registerReceiver(this.BatteryInfoReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
    }
    public static String getREQUESTING_LOCATION_UPDATES_KEY(){
        return REQUESTING_LOCATION_UPDATES_KEY;
    }
    public static String getLOCATION_KEY(){
        return LOCATION_KEY;
    }
    public String getCurrentBatteryLevel()
    {
        return percentageLabel;
    }
    public String getCurrentUpdateInterval()
    {
        return timeRunning;
    }

    public void updateValuesFromBundle(Bundle savedInstanceState) {
        Log.i(TAG, "Updating values from bundle");
        if (savedInstanceState != null) {
            if (savedInstanceState.keySet().contains(REQUESTING_LOCATION_UPDATES_KEY)) {
                global_button = savedInstanceState.getBoolean(
                        REQUESTING_LOCATION_UPDATES_KEY);
            }

            if (savedInstanceState.keySet().contains(LOCATION_KEY)) {
                mCurrentLocation = savedInstanceState.getParcelable(LOCATION_KEY);
            }
            if (savedInstanceState.keySet().contains(LAST_UPDATED_TIME_STRING_KEY)) {
                mLastUpdateTime = savedInstanceState.getString(LAST_UPDATED_TIME_STRING_KEY);
            }
        }
    }
    public Location getLocation()
    {
        return mCurrentLocation;
    }
    public String getLastLocationUpdateTime()
    {
        return mLastUpdateTime;
    }
    protected synchronized void buildGoogleApiClient() {
        Log.i(TAG, "Building GoogleApiClient");
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        createLocationRequest();
    }
    public void createLocationRequest()
    {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS/2);
        Log.i("String:",String.valueOf(UPDATE_INTERVAL_IN_MILLISECONDS));
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }
    public boolean isLocationServiceOn(){
        return global_button;
    }

    public void startLocationService() {
        if (!isPlayServicesAvailable(this)) return;
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            startLocationUpdates();
        }
        else {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.ACCESS_FINE_LOCATION)) {
                showRationaleDialog();
            } else {
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
            }
        }
    }

    public void startLocationUpdates() {
        Log.i(TAG, "startLocationUpdates");

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(mLocationRequest);
        PendingResult<LocationSettingsResult> result =
                LocationServices.SettingsApi.checkLocationSettings(mGoogleApiClient, builder.build());
        result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
            @Override
            public void onResult(@NonNull LocationSettingsResult locationSettingsResult) {
                final Status status = locationSettingsResult.getStatus();

                switch (status.getStatusCode()) {
                    case LocationSettingsStatusCodes.SUCCESS:
                        if (ContextCompat.checkSelfPermission(working.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, working.this);
                        }
                        break;
                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                        try {
                            status.startResolutionForResult(working.this, REQUEST_CHECK_SETTINGS);
                        } catch (IntentSender.SendIntentException e) {
                        }
                        break;
                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                        break;
                }
            }
        });
        global_button = true;
        onLocationServiceStart();
    }
    protected void stopLocationUpdates() {
        Log.i(TAG, "stopLocationUpdates");
        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
        global_button = false;
        onLocationServiceStop();
    }
    public void onLocationServiceStop()
    {

    }
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startLocationUpdates();
                } else {
                    if (!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                        global_button = false;
                        Toast.makeText(working.this, "To enable the function of this application, please enable the location information permission of the application from the setting screen of the terminal.", Toast.LENGTH_SHORT).show();
                    } else {
                        showRationaleDialog();
                    }
                }
                break;
            }
        }
    }
    public void onLocationServiceStart()
    {

    }

    public void showRationaleDialog() {
        new AlertDialog.Builder(this)
                .setPositiveButton("To give permission", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ActivityCompat.requestPermissions(working.this,
                                new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);

                    }
                })
                .setNegativeButton("don't do", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Toast.makeText(working.this,"Location permission not allowed", Toast.LENGTH_SHORT).show();
                        global_button = false;
                    }
                })
                .setCancelable(false)
                .setMessage("This app needs to allow the use of location information.")
                .show();
    }
    public static boolean isPlayServicesAvailable(Context context) {
        int resultCode = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context);
        if (resultCode != ConnectionResult.SUCCESS) {
            GoogleApiAvailability.getInstance().getErrorDialog((Activity) context, resultCode, 2).show();
            return false;
        }
        return true;
    }
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_CHECK_SETTINGS:
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        startLocationUpdates();
                        break;
                    case Activity.RESULT_CANCELED:
                        break;
                }
                break;
        }
    }
    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }
    @Override
    public void onResume() {
        super.onResume();
        setUpIntervalObject.inForeGround = true;
        isPlayServicesAvailable(this);
        this.registerReceiver(this.BatteryInfoReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
    }
    @Override
    protected void onPause() {
        super.onPause();
        setUpIntervalObject.inForeGround = false;
        Log.i("Running in Background: ", getCurrentUpdateInterval());
    }
    @Override
    protected void onStop() {
        super.onStop();
        if (!setUpIntervalObject.runInBackground)
        {
            stopLocationUpdates();
            mGoogleApiClient.disconnect();
        }

    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.i(TAG, "onConnected");
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        if (mCurrentLocation == null) {
            mCurrentLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
            mLastUpdateTime = DateFormat.getTimeInstance().format(new Date());
        }
        if (global_button) {
            startLocationUpdates();
        }
    }
    @Override
    public void onLocationChanged(Location location) {
        Log.i(TAG, "onLocationChanged");
        mCurrentLocation = location;
        mLastUpdateTime = DateFormat.getTimeInstance().format(new Date());
        onLocationUpdate();
    }
    public void onLocationUpdate()
    {

    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.i(TAG, "Connection suspended");
        mGoogleApiClient.connect();
    }
    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.i(TAG, "Connection failed: ConnectionResult.getErrorCode() = " + connectionResult.getErrorCode());
    }
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putBoolean(REQUESTING_LOCATION_UPDATES_KEY, global_button);
        savedInstanceState.putParcelable(LOCATION_KEY, mCurrentLocation);
        savedInstanceState.putString(LAST_UPDATED_TIME_STRING_KEY, mLastUpdateTime);
        super.onSaveInstanceState(savedInstanceState);
    }
    @Override
    public boolean moveTaskToBack(boolean nonRoot)
    {
        return super.moveTaskToBack(setUpIntervalObject.runInBackground);
    }

}

