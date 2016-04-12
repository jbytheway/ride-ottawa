package io.github.jbytheway.rideottawa;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.Log;

import org.joda.time.DateTime;

import java.util.ArrayList;

public class Alarm {
    private static final String TAG = "Alarm";

    public Alarm(FavouriteStop favouriteStop, TripUid tripUid, int minutesWarning, OnRefreshedListener listener, OcTranspoDataAccess ocTranspo) {
        mMinutesWarning = minutesWarning;
        mOnRefreshedListener = listener;

        Stop stop = favouriteStop.asStop(ocTranspo);
        Trip trip = ocTranspo.getTrip(tripUid.getTripId());
        int timeAtStop = ocTranspo.getTimeAtStop(trip, stop);

        mForthcomingTrip =  new ForthcomingTrip(
                stop, trip.getRoute(), null, null, tripUid.getTripId(),
                tripUid.getMidnight(), timeAtStop, trip.getStartTime()
        );

        mTimeEstimate = mForthcomingTrip.getEstimatedArrival().getTime();
    }

    interface OnRefreshedListener {
        void onRefreshed(Alarm alarm);
    }

    DateTime getTime() {
        return mTimeEstimate.minusMinutes(mMinutesWarning);
    }

    public void refreshTimeEstimate(Context context, OcTranspoDataAccess ocTranspo) {
        ArrayList<ForthcomingTrip> forthcomingTrips = new ArrayList<>();
        forthcomingTrips.add(mForthcomingTrip);

        Log.d(TAG, "Alarm getting live data");
        ocTranspo.getLiveDataForTrips(context, forthcomingTrips, true, new OcTranspoApi.Listener() {
            @Override
            public void onApiFail(@Nullable Exception e) {
                Log.d(TAG, "onApiFail: exception=" + e);
                // No update to time available
                mOnRefreshedListener.onRefreshed(Alarm.this);
            }

            @Override
            public void onTripData() {
                // The forthcoming trip will have been updated in place
                mTimeEstimate = mForthcomingTrip.getEstimatedArrival().getTime();
                Log.d(TAG, "onTripData "+mForthcomingTrip.getEstimatedArrival().getType());
                mOnRefreshedListener.onRefreshed(Alarm.this);
            }
        });
    }

    private int mMinutesWarning;
    private ForthcomingTrip mForthcomingTrip;
    private OnRefreshedListener mOnRefreshedListener;
    private DateTime mTimeEstimate;
}
