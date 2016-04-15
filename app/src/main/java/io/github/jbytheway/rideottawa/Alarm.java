package io.github.jbytheway.rideottawa;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.Log;

import org.joda.time.DateTime;

import java.util.ArrayList;

public class Alarm {
    private static final String TAG = "Alarm";

    public Alarm(FavouriteStop favouriteStop, TripUid tripUid, int minutesWarning, OnRefreshedListener listener, OcTranspoDataAccess ocTranspo) {
        mFavouriteStop = favouriteStop;
        mFavourite = favouriteStop.Favourite;
        mMinutesWarning = minutesWarning;
        mOnRefreshedListener = listener;

        mStop = favouriteStop.asStop(ocTranspo);
        mTrip = ocTranspo.getTrip(tripUid.getTripId());
        int timeAtStop = ocTranspo.getTimeAtStop(mTrip, mStop);

        mForthcomingTrip =  new ForthcomingTrip(
                mStop, mTrip.getRoute(), null, null, tripUid.getTripId(),
                tripUid.getMidnight(), timeAtStop, mTrip.getStartTime()
        );

        mTimeEstimate = mForthcomingTrip.getEstimatedArrival().getTime();
    }

    interface OnRefreshedListener {
        void onRefreshed(Alarm alarm);
    }

    Stop getStop() {
        return mStop;
    }

    Route getRoute() {
        return mTrip.getRoute();
    }

    Favourite getFavourite() {
        return mFavourite;
    }

    DateTime getTimeOfBus() {
        return mTimeEstimate;
    }

    DateTime getTimeOfAlarm() {
        return mTimeEstimate.minusMinutes(mMinutesWarning);
    }

    PendingAlarmData makePendingAlarmData(long timeToCheck) {
        return new PendingAlarmData(mForthcomingTrip.getTripUid(), mFavouriteStop.getId(), mMinutesWarning, timeToCheck);
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

    private Stop mStop;
    private Trip mTrip;
    private FavouriteStop mFavouriteStop;
    private Favourite mFavourite;
    private int mMinutesWarning;
    private ForthcomingTrip mForthcomingTrip;
    private OnRefreshedListener mOnRefreshedListener;
    private DateTime mTimeEstimate;
}
