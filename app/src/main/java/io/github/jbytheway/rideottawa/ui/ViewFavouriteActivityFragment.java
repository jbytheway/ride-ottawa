package io.github.jbytheway.rideottawa.ui;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Fragment;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import io.github.jbytheway.rideottawa.ArrivalEstimate;
import io.github.jbytheway.rideottawa.Favourite;
import io.github.jbytheway.rideottawa.ForthcomingTrip;
import io.github.jbytheway.rideottawa.OcTranspoApi;
import io.github.jbytheway.rideottawa.utils.IndirectArrayAdapter;
import io.github.jbytheway.rideottawa.RideOttawaApplication;
import io.github.jbytheway.rideottawa.OcTranspoDataAccess;
import io.github.jbytheway.rideottawa.R;
import io.github.jbytheway.rideottawa.Route;
import io.github.jbytheway.rideottawa.Stop;

public class ViewFavouriteActivityFragment extends Fragment implements OcTranspoApi.Listener {
    private static final String TAG = "ViewFavouriteFragment";
    private static final int AUTO_REFRESH_SECONDS = 30;
    private static final int MINIMUM_REFRESH_SECONDS = 15;

    public ViewFavouriteActivityFragment() {
        // Required empty public constructor
        mTimeFormatter = DateTimeFormat.forPattern("HH:mm");
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Don't destroy Fragment on reconfiguration
        setRetainInstance(true);

        // This Fragment adds options to the ActionBar
        setHasOptionsMenu(true);

        mContext = getActivity();
        mOcTranspo = ((RideOttawaApplication) getActivity().getApplication()).getOcTranspo();
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
        // Need an empty list of trips to start with because the ListView will
        // be rendered before we get informed of our Favourite.
        mForthcomingTrips = new ArrayList<>();
        mHandler = new Handler();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_view_favourite, container, false);

        ListView tripList = (ListView) view.findViewById(R.id.trip_list);

        mTripAdapter = new IndirectArrayAdapter<>(
                getActivity(),
                R.layout.view_favourite_list_item,
                new IndirectArrayAdapter.ListGenerator<ForthcomingTrip>() {
                    @Override
                    public List<ForthcomingTrip> makeList() {
                        Collections.sort(mForthcomingTrips, new Comparator<ForthcomingTrip>() {
                            @Override
                            public int compare(ForthcomingTrip lhs, ForthcomingTrip rhs) {
                                return lhs.getEstimatedArrival().compareTo(rhs.getEstimatedArrival());
                            }
                        });
                        return mForthcomingTrips;
                    }
                },
                new IndirectArrayAdapter.ViewGenerator<ForthcomingTrip>() {
                    @Override
                    public void applyView(View v, final ForthcomingTrip trip) {
                        TextView stop_code = (TextView) v.findViewById(R.id.stop_code);
                        TextView stop_name = (TextView) v.findViewById(R.id.stop_name);
                        TextView route_name = (TextView) v.findViewById(R.id.route_name);
                        TextView head_sign = (TextView) v.findViewById(R.id.head_sign);
                        TextView arrival_time_scheduled = (TextView) v.findViewById(R.id.arrival_time_scheduled);
                        TextView arrival_time_estimated = (TextView) v.findViewById(R.id.arrival_time_estimated);
                        TextView label_time_estimated = (TextView) v.findViewById(R.id.label_time_estimated);
                        TextView minutes_away = (TextView) v.findViewById(R.id.minutes_away);
                        TextView time_type = (TextView) v.findViewById(R.id.time_type);
                        Stop stop = trip.getStop();
                        stop_code.setText(stop.getCode());
                        stop_name.setText(stop.getName(mContext));
                        Route route = trip.getRoute();
                        route.applyToTextView(route_name);
                        boolean showHeadsigns = mSharedPreferences.getBoolean(SettingsActivityFragment.PREF_SHOW_HEADSIGNS, false);
                        if (showHeadsigns) {
                            head_sign.setText(trip.getHeadSign());
                        } else {
                            String lastStopName = trip.getLastStop().getName(mContext);
                            head_sign.setText(lastStopName);
                        }
                        arrival_time_scheduled.setText(mTimeFormatter.print(trip.getArrivalTime()));
                        ArrivalEstimate ae = trip.getEstimatedArrival();
                        DateTime estimatedArrival = ae.getTime();
                        DateTime now = mOcTranspo.getNow();
                        // Using Duration.getStandardMinutes rounds towards zero, where we want to
                        // round to nearest.  So we get the duration in seconds and do the rounding
                        // ourselves.
                        long secondsAway;
                        if (now.isAfter(estimatedArrival)) {
                            Interval intervalToArrival = new Interval(estimatedArrival, now);
                            secondsAway = -intervalToArrival.toDuration().getStandardSeconds();
                        } else {
                            Interval intervalToArrival = new Interval(now, estimatedArrival);
                            secondsAway = intervalToArrival.toDuration().getStandardSeconds();
                        }
                        long minutesAway = (Math.round((double) secondsAway)/60);
                        minutes_away.setText(getString(R.string.minutes_format, minutesAway));

                        if (ae.getType() == ArrivalEstimate.Type.Schedule) {
                            arrival_time_estimated.setVisibility(View.INVISIBLE);
                            label_time_estimated.setVisibility(View.INVISIBLE);
                        } else {
                            arrival_time_estimated.setVisibility(View.VISIBLE);
                            label_time_estimated.setVisibility(View.VISIBLE);
                            arrival_time_estimated.setText(mTimeFormatter.print(estimatedArrival));
                        }

                        switch (ae.getType()) {
                            case Gps: {
                                time_type.setText(getString(R.string.gps_abbrev));
                                //noinspection deprecation
                                int colour = getResources().getColor(R.color.time_gps);
                                minutes_away.setTextColor(colour);
                                arrival_time_estimated.setTextColor(colour);
                                break;
                            }
                            case GpsOld: case NoLongerGps: {
                                time_type.setText(getString(R.string.gps_old_abbrev));
                                int colour;
                                if (minutesAway < 0) {
                                    //noinspection deprecation
                                    colour = getResources().getColor(R.color.time_past);
                                } else {
                                    //noinspection deprecation
                                    colour = getResources().getColor(R.color.time_gps_old);
                                }
                                minutes_away.setTextColor(colour);
                                arrival_time_estimated.setTextColor(colour);
                                break;
                            }
                            case Schedule: {
                                time_type.setText(getString(R.string.scheduled_abbrev));
                                if (minutesAway < 0) {
                                    //noinspection deprecation
                                    minutes_away.setTextColor(getResources().getColor(R.color.time_past));
                                } else {
                                    //noinspection deprecation
                                    minutes_away.setTextColor(getResources().getColor(R.color.time_scheduled));
                                }
                                break;
                            }
                            default:
                                throw new AssertionError("Unexpected estimate type "+ae.getType());
                        }

                        // Override the above text if we are waiting for data
                        if (trip.isWaitingForLiveData()) {
                            time_type.setText(R.string.waiting_for_data_abbrev);
                        }
                    }
                }
        );

        tripList.setAdapter(mTripAdapter);

        return view;
    }

    public void initialize(Intent intent) {
        long favouriteId = intent.getLongExtra(EditFavouriteActivity.FAVOURITE_ID, -1);
        if (favouriteId == -1) {
            Log.e(TAG, "Missing FAVOURITE_ID in ViewFavourite Intent");
        } else {
            mFavourite = Favourite.findById(Favourite.class, favouriteId);
            populateFromFavourite();
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_view_favourite, menu);
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshIfLateEnough(false);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_refresh:
                refreshIfLateEnough(true);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void populateFromFavourite() {
        AppCompatActivity activity = (AppCompatActivity) getActivity();
        ActionBar actionBar = activity.getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(mFavourite.Name);
        }
        refresh();
    }

    private void refreshIfLateEnough(boolean showMessage) {
        DateTime now = new DateTime();
        if (now.minusSeconds(MINIMUM_REFRESH_SECONDS).isBefore(mLastRefresh)) {
            // Too soon; we won't refresh yet
            if (showMessage) {
                Toast.makeText(getActivity(), getString(R.string.skipping_refresh_too_soon), Toast.LENGTH_LONG).show();
            }
        } else {
            refresh();
        }
    }

    private class RefreshTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... _) {
            // NOTE: Assigning to mForthcomingTrips on another thread.
            // In an effort to make this reasonable, I made mForthcomingTrips volatile.
            // I think that's the correct Java approach.

            // Note that we are assigning an entirely new array; the old one may still be
            // referenced in e.g. the API calling code, but that's fine.
            mForthcomingTrips = mFavourite.updateForthcomingTrips(mOcTranspo, mForthcomingTrips);
            return null;
        }

        @Override
        protected void onPostExecute(Void _) {
            // Everything else gets run on the UI thread so we can safely mess with all fields
            for (ForthcomingTrip trip : mForthcomingTrips) {
                trip.notifyLiveUpdateRequested();
            }
            mLastRefresh = new DateTime();
            mOcTranspo.getLiveDataForTrips(mContext, mForthcomingTrips, ViewFavouriteActivityFragment.this);
            mTripAdapter.notifyDataSetChanged();
            mRefresingNow = false;
        }
    }

    private void refresh() {
        // The process of updating the Forthcoming trips can take a while (maybe half
        // a second or more depending on how many routes are involved.
        // Therefore we want to do it on a background thread.

        // First we check if we're already refreshing (very unlikely, because we force a fairly
        // long delay between refreshes)
        if (!mRefresingNow) {
            mRefresingNow = true;
            new RefreshTask().execute();
        }

        // Whether we actually refreshed or not, trigger another refresh
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (isResumed()) {
                    refreshIfLateEnough(false);
                }
            }
        }, AUTO_REFRESH_SECONDS * 1000);
    }

    public void onApiFail(Exception e) {
        Log.e(TAG, "API error", e);
        // TODO: report to user somehow?
    }

    public void onTripData() {
        mTripAdapter.notifyDataSetChanged();
    }

    private Context mContext;
    private OcTranspoDataAccess mOcTranspo;
    private SharedPreferences mSharedPreferences;
    private Handler mHandler;
    private Favourite mFavourite;
    private volatile ArrayList<ForthcomingTrip> mForthcomingTrips;
    private DateTime mLastRefresh;
    private boolean mRefresingNow;
    private IndirectArrayAdapter<ForthcomingTrip> mTripAdapter;
    private final DateTimeFormatter mTimeFormatter;

}
