package io.github.jbytheway.rideottawa.ui;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Point;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

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
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.github.jbytheway.rideottawa.RideOttawaApplication;
import io.github.jbytheway.rideottawa.OcTranspoDataAccess;
import io.github.jbytheway.rideottawa.R;
import io.github.jbytheway.rideottawa.Route;
import io.github.jbytheway.rideottawa.Stop;
import io.github.jbytheway.rideottawa.utils.IndirectArrayAdapter;

public class SelectStopActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, LocationListener {
    public static final String FROM_STOP_ID = "from_stop_id";
    public static final String SELECTED_STOP = "selected_stop";

    private static final String TAG = "SelectStopActivity";

    private static final int REQUEST_CHECK_SETTINGS = 1;
    private static final int REQUEST_PERMISSION_LOCATION = 2;

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

        mStopList = new ArrayList<>();

        setContentView(R.layout.activity_select_stop);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

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

        ListView stopList = (ListView) findViewById(R.id.stop_list);

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        mOrderBy = sharedPreferences.getString(SettingsActivityFragment.PREF_SORT_STOPS, "stop_name");

        Intent intent = getIntent();
        mFromStopId = intent.getStringExtra(FROM_STOP_ID);
        if (mFromStopId == null) {
            //noinspection ConstantConditions
            getSupportActionBar().setTitle(R.string.title_activity_select_stop);
        } else {
            //noinspection ConstantConditions
            getSupportActionBar().setTitle(R.string.title_activity_select_destination);
        }

        final LayoutInflater inflater = getLayoutInflater();
        final int routeNameWidthPixels = getResources().getDimensionPixelSize(R.dimen.route_name_width);
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int screenWidth = size.x;
        final int numColumns = Math.max((int) Math.floor(screenWidth / routeNameWidthPixels) - 1, 1);
        Log.d(TAG, "Setting " + numColumns + " columns (" + screenWidth + " / " + routeNameWidthPixels + ")");

        mAdapter = new IndirectArrayAdapter<>(
                this,
                R.layout.select_stop_list_item,
                new IndirectArrayAdapter.ListGenerator<Stop>() {
                    @Override
                    public List<Stop> makeList() {
                        return mStopList;
                    }
                },
                new IndirectArrayAdapter.ViewGenerator<Stop>() {
                    @Override
                    public void applyView(View v, final Stop stop) {
                        Context context = SelectStopActivity.this;
                        TextView stopCodeView = (TextView) v.findViewById(R.id.stop_code);
                        stopCodeView.setText(stop.getCode());
                        TextView stopNameView = (TextView) v.findViewById(R.id.stop_name);
                        stopNameView.setText(stop.getName(context));
                        GridLayout routesView = (GridLayout) v.findViewById(R.id.routes);
                        routesView.removeAllViews();
                        routesView.setColumnCount(numColumns);

                        Cursor c = mOcTranspo.getRoutesForStopById(stop.getId());
                        List<Route> routes = mOcTranspo.routeCursorToList(c);
                        HashMap<String, Integer> headsignMap = new HashMap<>();
                        for (Route route : routes) {
                            TextView routeView = (TextView) inflater.inflate(R.layout.route_name_only, routesView, false);
                            route.applyToTextView(routeView);
                            routesView.addView(routeView);

                            String headsign = route.getModalHeadSign();
                            Integer count = headsignMap.get(headsign);
                            if (count == null) {
                                count = 0;
                            }
                            ++count;
                            headsignMap.put(headsign, count);
                        }

                        LinearLayout routeInfo = (LinearLayout) v.findViewById(R.id.route_info);
                        if (routes.size() > 3) {
                            routeInfo.setOrientation(LinearLayout.VERTICAL);
                        } else {
                            routeInfo.setOrientation(LinearLayout.HORIZONTAL);
                        }

                        // Find the most popular headsigns
                        ArrayList<Map.Entry<String, Integer>> countedHeadsigns = new ArrayList<>(headsignMap.entrySet());
                        Collections.sort(countedHeadsigns, new Comparator<Map.Entry<String, Integer>>() {
                            @Override
                            public int compare(Map.Entry<String, Integer> lhs, Map.Entry<String, Integer> rhs) {
                                int integerCompare = Integer.compare(lhs.getValue(), rhs.getValue());
                                if (integerCompare != 0) {
                                    return -integerCompare;
                                }
                                return lhs.getKey().compareTo(rhs.getKey());
                            }
                        });
                        ArrayList<String> headsigns = new ArrayList<>(Lists.transform(
                                countedHeadsigns,
                                new Function<Map.Entry<String,Integer>, String>() {
                                @Override
                                public String apply(Map.Entry<String, Integer> input) {
                                    return input.getKey();
                                }
                            }));

                        final int maxHeadsigns = 4;
                        if (headsigns.size() > maxHeadsigns) {
                            headsigns.subList(maxHeadsigns, headsigns.size()).clear();
                            headsigns.add(getString(R.string.ellipsis));
                        }
                        String mostPopularHeadsigns = Joiner.on(", ").join(headsigns);
                        TextView headsignView = (TextView) v.findViewById(R.id.head_sign);
                        headsignView.setText(getString(R.string.to_destination, mostPopularHeadsigns));
                    }
                }
        );

        stopList.setAdapter(mAdapter);

        stopList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent result = new Intent();
                // FIXME: There is a race condition here; we might end up with a stop from a different
                // list than the one the user tapped, but I don't think it's possible to know for
                // sure given the structure of this code...
                Stop stop = mStopList.get(position);
                String stopId = stop.getId();
                result.putExtra(SELECTED_STOP, stopId);
                if (mFromStopId != null) {
                    result.putExtra(FROM_STOP_ID, mFromStopId);
                }
                setResult(RESULT_OK, result);
                finish();
            }
        });

        triggerListUpdate();
    }

    @Override
    protected void onStart() {
        mGoogleClient.connect();
        super.onStart();
    }

    private static class UpdateArgs {
        UpdateArgs(String filter, Location lastLocation) {
            Filter = filter;
            LastLocation = lastLocation;
        }

        String Filter;
        Location LastLocation;
    }

    private class UpdateListTask extends AsyncTask<UpdateArgs, Void, List<Stop>> {
        @Override
        protected List<Stop> doInBackground(UpdateArgs... params) {
            UpdateArgs args = params[0];

            Cursor c = mOcTranspo.getAllStopsMatchingReachableFrom(args.Filter, mFromStopId, mOrderBy, args.LastLocation);
            return mOcTranspo.stopCursorToList(c);
        }

        @Override
        protected void onPostExecute(List<Stop> newList) {
            mStopList = newList;
            mAdapter.notifyDataSetChanged();
        }
    }

    private void triggerListUpdate() {
        String filter = mStopFilter.getText().toString();
        new UpdateListTask().execute(new UpdateArgs(filter, mLastLocation));
    }

    @Override
    public void onConnectionSuspended(int i) {
        // Don't think this callback is important
        Log.d(TAG, "onConnectionSuspended");
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.d(TAG, "Connected to Google play");

        // The first thing we do is check whether we have the permission to get this location info
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED) {
            // Have the permission, go ahead and use it
            getLocation();
        } else {
            // Request the permission
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_PERMISSION_LOCATION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_PERMISSION_LOCATION:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    getLocation();
                } else {
                    Toast.makeText(this, R.string.location_permission_denied_response, Toast.LENGTH_LONG).show();
                }
                break;
            default:
                throw new AssertionError("Unexpected request code "+requestCode);
        }
    }

    private void getLocation() {
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
            public void onResult(@NonNull LocationSettingsResult result) {
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
    private String mFromStopId;
    private GoogleApiClient mGoogleClient;
    private List<Stop> mStopList;
    private IndirectArrayAdapter<Stop> mAdapter;
    private String mOrderBy;
    private EditText mStopFilter;
    private Location mLastLocation;
    private LocationRequest mLocationRequest;
}
