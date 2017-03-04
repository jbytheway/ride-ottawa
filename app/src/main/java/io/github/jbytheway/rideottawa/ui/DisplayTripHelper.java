package io.github.jbytheway.rideottawa.ui;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.ColorRes;
import android.support.annotation.StringRes;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.util.HashMap;

import io.github.jbytheway.rideottawa.ArrivalEstimate;
import io.github.jbytheway.rideottawa.Favourite;
import io.github.jbytheway.rideottawa.FavouriteRoute;
import io.github.jbytheway.rideottawa.FavouriteStop;
import io.github.jbytheway.rideottawa.ForthcomingTrip;
import io.github.jbytheway.rideottawa.NoSuchRouteError;
import io.github.jbytheway.rideottawa.OcTranspoDataAccess;
import io.github.jbytheway.rideottawa.R;
import io.github.jbytheway.rideottawa.RideOttawaApplication;
import io.github.jbytheway.rideottawa.db.Route;
import io.github.jbytheway.rideottawa.db.Stop;
import io.github.jbytheway.rideottawa.utils.IndirectArrayAdapter;
import io.github.jbytheway.rideottawa.utils.TimeUtils;

public class DisplayTripHelper implements IndirectArrayAdapter.ViewGenerator<ForthcomingTrip> {
    public DisplayTripHelper(Activity activity) {
        mContext = activity;
        mOttawaTimeZone = DateTimeZone.forID("America/Toronto");
        mTimeFormatter = DateTimeFormat.forPattern("HH:mm");
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
        mOcTranspo = ((RideOttawaApplication) activity.getApplication()).getOcTranspo();
        mChosenDestinations = new HashMap<>();
    }

    public void addFavourite(Favourite favourite) {
        boolean someRoutesMissing = false;

        for (FavouriteStop stop : favourite.getStops()) {
            someRoutesMissing = addFavouriteStop(stop) | someRoutesMissing;
        }

        if (someRoutesMissing) {
            Toast.makeText(mContext, R.string.some_routes_missing, Toast.LENGTH_LONG).show();
        }
    }

    public boolean addFavouriteStop(FavouriteStop favouriteStop) {
        Stop stop = favouriteStop.asStop(mOcTranspo);

        if (mChosenDestinations.containsKey(stop)) {
            return false;
        }

        boolean someRoutesMissing = false;

        HashMap<Route, String> destinationsByRoute = new HashMap<>();
        for (FavouriteRoute favouriteRoute : favouriteStop.getRoutes()) {
            String destinationId = favouriteRoute.Destination;
            Route route;
            try {
                route = favouriteRoute.asRoute(mOcTranspo);
            } catch (NoSuchRouteError e) {
                favouriteRoute.deleteRecursively();
                someRoutesMissing = true;
                continue;
            }
            if (destinationId == null) {
                destinationsByRoute.put(route, null);
            } else {
                Stop destination = mOcTranspo.getStop(destinationId);
                destinationsByRoute.put(route, destination.getName(mContext));
            }
        }
        mChosenDestinations.put(stop, destinationsByRoute);

        return someRoutesMissing;
    }

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
        String defaultWhatDestination = mContext.getString(R.string.pref_default_what_destination);
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
                HashMap<Route, String> endsByRoute = mChosenDestinations.get(stop);
                String destination = null;
                if (endsByRoute != null) {
                    destination = endsByRoute.get(route);
                }
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
        arrival_time_scheduled.setText(formatTime(trip.getArrivalTime()));
        ArrivalEstimate ae = trip.getEstimatedArrival();
        DateTime estimatedArrival = ae.getTime();
        long minutesAway = formatMinutesAway(estimatedArrival, minutes_away);

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

        time_type.setText(mContext.getString(timeTypeString));

        if (minutesAway < 0) {
            arrivalTimeColour = R.color.time_past;
        }

        //noinspection deprecation
        int colour = mContext.getResources().getColor(arrivalTimeColour);
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

    public String formatTime(DateTime time) {
        return mTimeFormatter.print(time);
    }

    public long formatMinutesAway(DateTime time, TextView view) {
        DateTime now = mOcTranspo.getNow().withZone(mOttawaTimeZone);
        long minutesAway = TimeUtils.minutesDifference(now, time);
        view.setText(mContext.getString(R.string.minutes_format, minutesAway));
        int minutesAwayAppearance;
        if (minutesAway >= -199) {
            minutesAwayAppearance = android.R.style.TextAppearance_DeviceDefault_Large;
        } else if (minutesAway >= -999 ){
            minutesAwayAppearance = android.R.style.TextAppearance_DeviceDefault_Medium;
        } else {
            minutesAwayAppearance = android.R.style.TextAppearance_DeviceDefault_Small;
        }
        //noinspection deprecation
        view.setTextAppearance(mContext, minutesAwayAppearance);

        return minutesAway;
    }

    private final Context mContext;
    private final DateTimeZone mOttawaTimeZone;
    private final DateTimeFormatter mTimeFormatter;
    private final OcTranspoDataAccess mOcTranspo;
    private final SharedPreferences mSharedPreferences;
    private final HashMap<Stop, HashMap<Route, String>> mChosenDestinations;
}
