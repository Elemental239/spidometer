package com.application.elemental.spidometer;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import java.io.IOException;

public class SpeedActivity extends Activity {
    private static String TAG = SpeedActivity.class.getSimpleName();
    private static String GPS_IN_WORK_SAVE_KEY = "GPS_IN_WORK_SAVE_KEY";
    private static String LATEST_LOCATION_SAVE_KEY = "LATEST_LOCATION_SAVE_KEY";

    private TextView m_speedTextView = null;
    private Button m_startStopButton = null;

    private LocationManager m_locationManager = null;
    private SpeedometerLocationListener m_locationListener = new SpeedometerLocationListener(this);
    private StartStopButtonOnClickListener m_startStopButtonListener = new StartStopButtonOnClickListener(this);
    private Location m_latestLocation = null;
    private long m_latestTime = 0;
    private boolean m_bWasStarted = false;
    private FileLogger m_logger = new FileLogger(this);
    private boolean m_bFirstRun = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_speed);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        Log.i(TAG,"App started");

        m_speedTextView = (TextView)findViewById(R.id.SpeedText);

        m_speedTextView.setText("0");
        m_speedTextView.setTextSize(120);

        m_startStopButton = (Button)findViewById(R.id.StartStopButton);
        m_startStopButton.setText("Start");

        m_startStopButton.setOnClickListener(m_startStopButtonListener);

        try {
            m_logger.StartNewFile();
        } catch (IOException e)
        {
            ShowErrorToUser(e.getMessage());
            return;
        }

        m_logger.i("onCreate()");

        if (savedInstanceState != null)
        {
            m_bWasStarted = savedInstanceState.getBoolean(GPS_IN_WORK_SAVE_KEY);
            m_latestLocation = savedInstanceState.getParcelable(LATEST_LOCATION_SAVE_KEY);

            if (m_bWasStarted)
            {
                EnableGPS(this);
                m_startStopButton.setText("Stop");

                if (m_latestLocation != null)
                {
                    m_speedTextView.setText(Integer.toString(Math.round(m_latestLocation.getSpeed())));
                }
            }
        }
    }

    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 1: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    EnableGPS(this);
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
            }
            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    private void EnableGPS(Context context)
    {
        m_logger.i("EnableGPS call");

        m_locationManager = (LocationManager)getSystemService(context.LOCATION_SERVICE);

        if (m_locationManager == null)
        {
            m_logger.i("No location manager");
            ShowErrorToUser("gps unsupported");
            return;
        }

        boolean bGPSTurnedOn = m_locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);

        if (!bGPSTurnedOn)
        {
            m_logger.i("gps is off");
            ShowErrorToUser("Please turn gps on and restart app");
            return;
        }

        if (checkLocationPermission())
        {
            m_logger.i("start request updates");
            m_locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, m_locationListener);
        }
    }

    public boolean checkLocationPermission()
    {
        String permission = "android.permission.ACCESS_FINE_LOCATION";
        int res = this.checkCallingOrSelfPermission(permission);
        return (res == PackageManager.PERMISSION_GRANTED);
    }


    class SpeedometerLocationListener implements LocationListener
    {
        private SpeedActivity m_activity;

        SpeedometerLocationListener(SpeedActivity activity) { m_activity = activity; }

        @Override
        public void onLocationChanged(Location location) {
            m_logger.i("Updated Location " + location);

            if (m_latestLocation == null)
            {
                m_logger.i("First point, init variables");
                m_latestLocation = location;
                m_latestTime = System.currentTimeMillis();
            }
            else
            {


                double speed_from_location_ms = location.getSpeed();
                long currentTime = System.currentTimeMillis();
//                Date currentDate = new Date();
                float fDistance = location.distanceTo(m_latestLocation);
                long nMillisecs = currentTime - m_latestTime;
//
                if (nMillisecs == 0)
                    return;
//
                double speed_ms = fDistance / (nMillisecs / 1000);
                double speed_km_hour = speed_ms * 18 / 5;

                String location_speed_string = Integer.toString((int)Math.round(speed_from_location_ms * 18 / 5));
                String calculated_speed_string = Integer.toString((int)Math.round(speed_km_hour));
                String outputText =  location_speed_string + "|" + calculated_speed_string;

                m_logger.i("update speed: fDistance = " + fDistance + "; nMillisecs = " + nMillisecs + "; speed_ms = " + speed_ms +
                        "; speed_km_hour = " + speed_km_hour + "; Math.round(speed_km_hour) = " + Math.round(speed_km_hour));

                m_activity.runOnUiThread(new SpeedSetTextRunnable(m_activity, outputText));

                m_latestLocation = location;
                m_latestTime = currentTime;
            }
        }

        @Override
        public void onStatusChanged(String s, int i, Bundle bundle) {

        }

        @Override
        public void onProviderEnabled(String s) {

        }

        @Override
        public void onProviderDisabled(String s) {

        }


        class SpeedSetTextRunnable implements Runnable
        {
            private SpeedActivity m_activity;
            private String m_speedText;

            SpeedSetTextRunnable(SpeedActivity activity, String text) { m_activity = activity; m_speedText = text; }

            @Override
            public void run() {
                m_activity.m_speedTextView.setText(m_speedText);
            }
        }
    }

    class StartStopButtonOnClickListener implements View.OnClickListener
    {
        private SpeedActivity m_activity;

        StartStopButtonOnClickListener(SpeedActivity activity) { m_activity = activity; }

        @Override
        public void onClick(View view) {
            m_logger.i("StartStopButtonOnClickListener::onClick(): Button clicked");

            if (!m_bWasStarted)
            {
                if (m_bFirstRun)
                {
                    m_bFirstRun = false;
                } else {
                    try {
                        m_logger.StartNewFile();
                    } catch (IOException e)
                    {
                        ShowErrorToUser(e.getMessage());
                        return;
                    }
                }

                m_logger.i("StartStopButtonOnClickListener::onClick(): Enable gps");

                if (checkLocationPermission())
                {
                    EnableGPS(m_activity);
                }
                else
                {
                    ActivityCompat.requestPermissions(m_activity,new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
                }

                m_bWasStarted = true;
                m_startStopButton.setText("Stop");
            }
            else
            {
                m_logger.i("StartStopButtonOnClickListener::onClick(): Disable gps");
                m_locationManager.removeUpdates(m_locationListener);
                m_latestLocation = null;
                //m_latestTime = null;
                m_bWasStarted = false;
                m_startStopButton.setText("Start");
                m_speedTextView.setText("0");

            }
        }
    }

    private void ShowErrorToUser(String message)
    {
        if(m_speedTextView == null)
            return;

        m_speedTextView.setTextSize(30);
        m_speedTextView.setText(message);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState, @Nullable PersistableBundle persistentState) {
        super.onCreate(savedInstanceState, persistentState);
        m_logger.i( "onCreate");
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        m_logger.i( "onRestoreInstanceState");
    }

    @Override
    protected void onResume() {
        super.onResume();
        m_logger.i( "onResume");
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        m_logger.i( "onPostResume");
    }

    @Override
    protected void onPause() {
        super.onPause();
        m_logger.i( "onPause");
    }

    @Override
    protected void onStop() {
        super.onStop();
        m_logger.i( "onStop");
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        m_logger.i( "onSaveInstanceState");

        outState.putBoolean(GPS_IN_WORK_SAVE_KEY, m_bWasStarted);
        outState.putParcelable(LATEST_LOCATION_SAVE_KEY, m_latestLocation);

        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        m_logger.i( "onRestart");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        m_logger.i( "onDestroy");
        m_logger.Close();
    }
}
