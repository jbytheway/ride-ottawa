package io.github.jbytheway.rideottawa.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Fragment;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.annotation.ColorRes;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.NumberPicker;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Interval;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import io.github.jbytheway.rideottawa.AlarmService;
import io.github.jbytheway.rideottawa.ArrivalEstimate;
import io.github.jbytheway.rideottawa.Favourite;
import io.github.jbytheway.rideottawa.FavouriteRoute;
import io.github.jbytheway.rideottawa.FavouriteStop;
import io.github.jbytheway.rideottawa.ForthcomingTrip;
import io.github.jbytheway.rideottawa.OcTranspoApi;
import io.github.jbytheway.rideottawa.TripUid;
import io.github.jbytheway.rideottawa.utils.IndirectArrayAdapter;
import io.github.jbytheway.rideottawa.RideOttawaApplication;
import io.github.jbytheway.rideottawa.OcTranspoDataAccess;
import io.github.jbytheway.rideottawa.R;
import io.github.jbytheway.rideottawa.Route;
import io.github.jbytheway.rideottawa.Stop;
import io.github.jbytheway.rideottawa.utils.TimeUtils;

public class ViewFavouriteActivityFragment extends Fragment implements OcTranspoApi.Listener {
    private static final String TAG = "ViewFavouriteFragment";
    private static final int AUTO_REFRESH_SECONDS = 30;
    private static final int MINIMUM_REFRESH_SECONDS = 15;
    private static final int MAX_FORTHCOMING_TRIPS = 50;
    private static final int MAX_ALARM_MINUTES_WARNING = 60;
    private static final int DEFAULT_ALARM_MINUTES_WARNING = 5;

    public ViewFavouriteActivityFragment() {
        // Required empty public constructor
        mTimeFormatter = DateTimeFormat.forPattern("HH:mm");
        mOttawaTimeZone = DateTimeZone.forID("America/Toronto");
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

        mSwipeRefresh = (SwipeRefreshLayout) view.findViewById(R.id.trip_list_swiper);
        mSwipeRefresh.setColorSchemeResources(R.color.colorPrimaryDark);
        mSwipeRefresh.setProgressBackgroundColorSchemeResource(R.color.colorAccent);
        mSwipeRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                if (!refreshIfLateEnough(true)) {
                    mSwipeRefresh.setRefreshing(false);
                }
            }
        });

        mTripList = (ListView) view.findViewById(R.id.trip_list);

        mTripAdapter = new IndirectArrayAdapter<>(
                getActivity(),
                R.layout.view_favourite_list_item,
                new IndirectArrayAdapter.ListGenerator<ForthcomingTrip>() {
                    @Override
                    public List<ForthcomingTrip> makeList() {
                        Collections.sort(mForthcomingTrips, new ForthcomingTrip.CompareEstimatedArrivals());
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
                        String defaultWhatDestination = getString(R.string.pref_default_what_destination);
                        String whatDestination = mSharedPreferences.getString(SettingsActivityFragment.PREF_WHAT_DESTINATION, defaultWhatDestination);
                        switch (whatDestination) {
                            case "headsign":
                                head_sign.setText(trip.getHeadSign());
                                break;
                            case "final_stop": {
                                String lastStopName = trip.getLastStop().getName(mContext);
                                head_sign.setText(lastStopName);
                                break;
                            }
                            case "chosen": {
                                String destination = mChosenDestinations.get(stop).get(route);
                                if (destination == null) {
                                    // Fall back on trip end
                                    String lastStopName = trip.getLastStop().getName(mContext);
                                    head_sign.setText(lastStopName);
                                } else {
                                    head_sign.setText(destination);
                                }
                                break;
                            }
                            default:
                                throw new AssertionError("Unexpected what_destination " + whatDestination);
                        }
                        arrival_time_scheduled.setText(mTimeFormatter.print(trip.getArrivalTime()));
                        ArrivalEstimate ae = trip.getEstimatedArrival();
                        DateTime estimatedArrival = ae.getTime();
                        DateTime now = mOcTranspo.getNow().withZone(mOttawaTimeZone);
                        long minutesAway = TimeUtils.minutesDifference(now, estimatedArrival);
                        minutes_away.setText(getString(R.string.minutes_format, minutesAway));

                        if (ae.getType() == ArrivalEstimate.Type.Schedule) {
                            arrival_time_estimated.setVisibility(View.INVISIBLE);
                            label_time_estimated.setVisibility(View.INVISIBLE);
                        } else {
                            arrival_time_estimated.setVisibility(View.VISIBLE);
                            label_time_estimated.setVisibility(View.VISIBLE);
                            arrival_time_estimated.setText(mTimeFormatter.print(estimatedArrival));
                        }

                        @ColorRes int arrivalTimeColour;
                        @StringRes int timeTypeString;
                        int estimationVisibility;

                        switch (ae.getType()) {
                            case Gps: {
                                timeTypeString = R.string.gps_abbrev;
                                arrivalTimeColour = R.color.time_gps;
                                estimationVisibility = View.VISIBLE;
                                break;
                            }
                            case GpsOld: case NoLongerGps: {
                                timeTypeString = R.string.gps_old_abbrev;
                                arrivalTimeColour = R.color.time_gps_old;
                                estimationVisibility = View.VISIBLE;
                                break;
                            }
                            case Schedule: {
                                timeTypeString = R.string.scheduled_abbrev;
                                arrivalTimeColour = R.color.time_scheduled;
                                estimationVisibility = View.INVISIBLE;
                                break;
                            }
                            case LastStop: {
                                timeTypeString = R.string.last_stop_abbrev;
                                arrivalTimeColour = R.color.time_scheduled;
                                estimationVisibility = View.INVISIBLE;
                                break;
                            }
                            default:
                                throw new AssertionError("Unexpected estimate type "+ae.getType());
                        }

                        time_type.setText(getString(timeTypeString));

                        if (minutesAway < 0) {
                            arrivalTimeColour = R.color.time_past;
                        }

                        //noinspection deprecation
                        int colour = getResources().getColor(arrivalTimeColour);
                        minutes_away.setTextColor(colour);
                        arrival_time_estimated.setTextColor(colour);

                        // Set visibility
                        arrival_time_estimated.setVisibility(estimationVisibility);
                        label_time_estimated.setVisibility(estimationVisibility);

                        // Override the above text if we are waiting for data
                        if (trip.isWaitingForLiveData()) {
                            time_type.setText(R.string.waiting_for_data_abbrev);
                        }
                    }
                }
        );

        mTripList.setAdapter(mTripAdapter);

        mTripList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                ForthcomingTrip trip = mForthcomingTrips.get(position);
                setAlarmFor(trip);
            }
        });

        return view;
    }

    public void initialize(Intent intent) {
        long favouriteId = intent.getLongExtra(EditFavouriteActivity.FAVOURITE_ID, -1);
        if (favouriteId == -1) {
            Log.e(TAG, "Missing FAVOURITE_ID in ViewFavourite Intent");
        } else {
            // Because of setRetainInstance we will sometimes be called when the appropriate Favourite
            // is already populated, in which case we do nothing
            if (mFavourite == null || mFavourite.getId() != favouriteId) {
                mFavourite = Favourite.findById(Favourite.class, favouriteId);
                populateFromFavourite();
            }
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
            case R.id.menu_help:
                helpDialog();
                return true;
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
        mChosenDestinations = new HashMap<>();
        for (FavouriteStop stop : mFavourite.getStops()) {
            HashMap<Route, String> destinationsByRoute = new HashMap<>();
            for (FavouriteRoute favouriteRoute : stop.getRoutes()) {
                String destinationId = favouriteRoute.Destination;
                Route route = favouriteRoute.asRoute(mOcTranspo);
                if (destinationId == null) {
                    destinationsByRoute.put(route, null);
                } else {
                    Stop destination = mOcTranspo.getStop(destinationId);
                    destinationsByRoute.put(route, destination.getName(mContext));
                }
            }
            mChosenDestinations.put(stop.asStop(mOcTranspo), destinationsByRoute);
        }
        refresh();
    }

    private boolean refreshIfLateEnough(boolean showMessage) {
        DateTime now = new DateTime();
        if (now.minusSeconds(MINIMUM_REFRESH_SECONDS).isBefore(mLastRefresh)) {
            // Too soon; we won't refresh yet
            if (showMessage) {
                Toast.makeText(getActivity(), getString(R.string.skipping_refresh_too_soon), Toast.LENGTH_LONG).show();
            }
            return false;
        } else {
            refresh();
            return true;
        }
    }

    private class RefreshTask extends AsyncTask<Void, Void, ArrayList<ForthcomingTrip>> {
        @Override
        protected ArrayList<ForthcomingTrip> doInBackground(Void... ignore) {
            ArrayList<ForthcomingTrip> newTrips = mFavourite.updateForthcomingTrips(mOcTranspo, mForthcomingTrips);
            // Truncate the list if it's absurdly large
            Collections.sort(newTrips, new ForthcomingTrip.CompareEstimatedArrivals());
            if (newTrips.size() > MAX_FORTHCOMING_TRIPS) {
                newTrips.subList(MAX_FORTHCOMING_TRIPS, newTrips.size()).clear();
            }

            return newTrips;
        }

        @Override
        protected void onPostExecute(ArrayList<ForthcomingTrip> newTrips) {
            // Note that we are assigning an entirely new array; the old one may still be
            // referenced in e.g. the API calling code, but that's fine.
            mForthcomingTrips = newTrips;
            for (ForthcomingTrip trip : mForthcomingTrips) {
                trip.notifyLiveUpdateRequested();
            }
            mLastRefresh = new DateTime();
            mOcTranspo.getLiveDataForTrips(mContext, mForthcomingTrips, false, ViewFavouriteActivityFragment.this);
            mTripAdapter.notifyDataSetChanged();
            mRefreshingNow = false;
            // Turn off the refreshing indicator
            mSwipeRefresh.setRefreshing(false);
        }
    }

    private void refresh() {
        // The process of updating the Forthcoming trips can take a while (maybe half
        // a second or more depending on how many routes are involved.
        // Therefore we want to do it on a background thread.

        // First we check if we're already refreshing (very unlikely, because we force a fairly
        // long delay between refreshes)
        if (!mRefreshingNow) {
            mRefreshingNow = true;
            // FIXME: The setRefreshing doesn't work for some reason the first time
            mSwipeRefresh.setRefreshing(true);
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

    public static class SetAlarmDialog extends DialogFragment {
        public SetAlarmDialog() {
            // Default constructor required for DialogFragments
            // Real construction happens in onAttach
        }

        public interface AlarmDialogListener {
            void setAlarmAt(long favouriteStopId, TripUid tripUid, int minutesWarning);
        }

        @Override
        public void onAttach(Activity activity) {
            super.onAttach(activity);
            mListener = ((ViewFavouriteActivity) activity).getFragment().getAlarmListener();
            Bundle args = getArguments();
            mFavouriteStopId = args.getLong("favourite_stop_id");
            mTripUid = args.getParcelable("trip_uid");
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstance) {
            LayoutInflater inflater = getActivity().getLayoutInflater();
            View view = inflater.inflate(R.layout.number_picker, null);

            TextView caption = (TextView) view.findViewById(R.id.caption);
            caption.setText(R.string.choose_minutes_warning);

            NumberPicker numberPicker = (NumberPicker) view.findViewById(R.id.number_picker);
            numberPicker.setMaxValue(MAX_ALARM_MINUTES_WARNING);
            numberPicker.setValue(DEFAULT_ALARM_MINUTES_WARNING);

            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder
                    .setView(view)
                    .setTitle(R.string.title_set_alarm_dialog)
                    .setPositiveButton(R.string.action_set_alarm, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            NumberPicker numberPicker = (NumberPicker) getDialog().findViewById(R.id.number_picker);
                            int minutesWarning = numberPicker.getValue();
                            mListener.setAlarmAt(mFavouriteStopId, mTripUid, minutesWarning);
                        }
                    })
                    .setNegativeButton(R.string.action_cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    });
            return builder.create();
        }

        private AlarmDialogListener mListener;
        private long mFavouriteStopId;
        private TripUid mTripUid;
    }

    private void setAlarmFor(ForthcomingTrip trip) {
        // Figure out which Favourite gave rise to this trip
        FavouriteStop favouriteStop = mFavourite.getStop(trip.getStop().getId());

        SetAlarmDialog dialog = new SetAlarmDialog();
        Bundle args = new Bundle();
        args.putLong("favourite_stop_id", favouriteStop.getId());
        args.putParcelable("trip_uid", trip.getTripUid());
        dialog.setArguments(args);
        dialog.show(getFragmentManager(), "SetAlarmDialog");
    }

    private SetAlarmDialog.AlarmDialogListener getAlarmListener() {
        return new SetAlarmDialog.AlarmDialogListener() {
            @Override
            public void setAlarmAt(long favouriteStopId, TripUid tripUid, int minutesWarning) {
                Intent intent = new Intent(getActivity(), AlarmService.class);
                Log.d(TAG, "favouriteStopId = "+favouriteStopId);
                intent.putExtra(AlarmService.FAVOURITE_STOP_ID, favouriteStopId);
                intent.putExtra(AlarmService.TRIP_UID, tripUid);
                intent.putExtra(AlarmService.MINUTES_WARNING, minutesWarning);
                getActivity().startService(intent);
            }
        };
    }

    public static class HelpDialog extends DialogFragment {
        public HelpDialog() {
            // Default constructor required for DialogFragments
            // Real construction happens in onAttach
        }

        @Override
        public void onAttach(Activity activity) {
            super.onAttach(activity);
            //Bundle args = getArguments();
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstance) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder
                    .setMessage(R.string.view_favourite_help)
                    .setNegativeButton(R.string.close_dialog, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dismiss();
                        }
                    });
            return builder.create();
        }
    }

    private void helpDialog() {
        HelpDialog dialog = new HelpDialog();
        Bundle args = new Bundle();
        dialog.setArguments(args);
        dialog.show(getFragmentManager(), "HelpDialog");
    }

    private final DateTimeZone mOttawaTimeZone;
    private Context mContext;
    private OcTranspoDataAccess mOcTranspo;
    private SharedPreferences mSharedPreferences;
    private Handler mHandler;
    private Favourite mFavourite;
    private HashMap<Stop, HashMap<Route, String>> mChosenDestinations;
    private ArrayList<ForthcomingTrip> mForthcomingTrips;
    private DateTime mLastRefresh;
    private boolean mRefreshingNow;
    private SwipeRefreshLayout mSwipeRefresh;
    private ListView mTripList;
    private IndirectArrayAdapter<ForthcomingTrip> mTripAdapter;
    private final DateTimeFormatter mTimeFormatter;

}
