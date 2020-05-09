// OnBatteryLevelChanged will be called when there is a change in the battery level
// You can get the batteryLevel through getCurrentBatteryLevel
// onLocationServiceStart is called after service starts
// onLocationServiceStop is called after service stops
// startLocationService will start the location service
// stopLocationService will stop the location service
// setUpLocationService and runInAppBackground will configure the location service
// onLocationUpdate will be called when Location gets updated
// You can get location values from getCurrentLocation()

package com.dell.battery_aware;

import android.os.Bundle;
import androidx.annotation.Nullable;
public class MainActivity extends working{
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setUpLocationService(setUpIntervalObject.LINEAR,3000,100);
        runAppInBackground(true,2);
    }
    @Override
    public void onBatteryLevelChanged() {
        super.onBatteryLevelChanged();
    }

    @Override
    public void onLocationServiceStart()
    {
        super.onLocationServiceStart();
    }
    @Override
    public void onLocationServiceStop()
    {
        super.onLocationServiceStop();
    }
    @Override
    public void onLocationUpdate() {
        super.onLocationUpdate();
    }
    @Override
    public void onSaveInstanceState(Bundle savedInstanceState)
    {
        super.onSaveInstanceState(savedInstanceState);
    }
}

