package io.github.jbytheway.rideottawa.ui;

import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.location.Location;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.FilterQueryProvider;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

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

import io.github.jbytheway.rideottawa.RideOttawaApplication;
import io.github.jbytheway.rideottawa.OcTranspoDataAccess;
import io.github.jbytheway.rideottawa.R;
import io.github.jbytheway.rideottawa.Stop;

public class SelectStopActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, LocationListener {
    public static final String SELECTED_STOP = "selected_stop";
    private static final String TAG = "SelectStopActivity";

    private static final int REQUEST_CHECK_SETTINGS = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Location stuff
        mLastLocation = null;
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(10);
        mLocationRequest.setNumUpdates(1);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);

        mOcTranspo = ((RideOttawaApplication) getApplication()).getOcTranspo();
        if (mGoogleClient == null) {
            mGoogleClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addApi(LocationServices.API)
                    .build();
        }

        setContentView(R.layout.activity_select_stop);

        ListView stopList = (ListView) findViewById(R.id.stop_list);

        // For the cursor adapter, specify which columns go into which views
        String[] fromColumns = {"stop_code", "stop_name"};
        int[] toViews = {R.id.stop_code, R.id.stop_name};

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        final String orderBy = sharedPreferences.getString(SettingsActivityFragment.PREF_SORT_STOPS, "stop_name");

        Cursor cursor = mOcTranspo.getAllStops(orderBy, mLastLocation);

        mAdapter = new SimpleCursorAdapter(this,
                R.layout.select_stop_list_item, cursor,
                fromColumns, toViews, 0);
        mAdapter.setFilterQueryProvider(new FilterQueryProvider() {
            @Override
            public Cursor runQuery(CharSequence constraint) {
                return mOcTranspo.getAllStopsMatching(constraint.toString(), orderBy, mLastLocation);
            }
        });

        stopList.setAdapter(mAdapter);

        stopList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent result = new Intent();
                Stop stop = mOcTranspo.getStop(id);
                String stopId = stop.getId();
                result.putExtra(SELECTED_STOP, stopId);
                setResult(RESULT_OK, result);
                finish();
            }
        });

        mStopFilter = (EditText) findViewById(R.id.stop_filter);
        mStopFilter.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                triggerListUpdate();
            }
        });
    }

    @Override
    protected void onStart() {
        mGoogleClient.connect();
        super.onStart();
    }

    private void triggerListUpdate() {
        mAdapter.getFilter().filter(mStopFilter.getText());
    }

    @Override
    public void onConnectionSuspended(int i) {
        // Don't think this callback is important
        Log.d(TAG, "onConnectionSuspended");
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.d(TAG, "Connected to Google play");
        try {
            mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleClient);
        } catch (SecurityException e) {
            // Don't care
            Log.d(TAG, "Not permitted to obtain location");
            return;
        }
        Log.d(TAG, "Got location " + mLastLocation);
        if (mLastLocation == null) {
            // this is the tricky case, where we need to jump through many more hoops to get a location
            triggerLocationRequest();
        } else {
            // Yay!
            triggerListUpdate();
        }
    }

    private void triggerLocationRequest() {
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(mLocationRequest);
        final PendingResult<LocationSettingsResult> result =
                LocationServices.SettingsApi.checkLocationSettings(
                        mGoogleClient, builder.build());
        result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
            @Override
            public void onResult(LocationSettingsResult result) {
                final Status status = result.getStatus();

                switch (status.getStatusCode()) {
                    case LocationSettingsStatusCodes.SUCCESS:
                        Log.d(TAG, "SUCCESS");
                        try {
                            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleClient, mLocationRequest, SelectStopActivity.this);
                        } catch (SecurityException e) {
                            // Ignore
                        }
                        break;
                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                        Log.d(TAG, "RESOLUTION_REQUIRED");
                        try {
                            status.startResolutionForResult(SelectStopActivity.this, REQUEST_CHECK_SETTINGS);
                        } catch (IntentSender.SendIntentException e) {
                            // Ignore
                        }
                        break;
                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                        Log.d(TAG, "SETTINGS_CHANGE_UNAVAILABLE");
                        break;
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case REQUEST_CHECK_SETTINGS:
                if (resultCode == RESULT_OK) {
                    // This means we can once again try to get a location
                    triggerLocationRequest();
                }
                break;
            default:
                throw new AssertionError("Unexpected request code "+requestCode);
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.d(TAG, "onLocationChanged: Got location " + mLastLocation);
        mLastLocation = location;
        triggerListUpdate();
    }

    @Override
    protected void onStop() {
        mGoogleClient.disconnect();
        super.onStop();
    }

    private OcTranspoDataAccess mOcTranspo;
    private GoogleApiClient mGoogleClient;
    private SimpleCursorAdapter mAdapter;
    private EditText mStopFilter;
    private Location mLastLocation;
    private LocationRequest mLocationRequest;
}
