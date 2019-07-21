package io.github.jbytheway.rideottawa;

import android.content.Context;
import androidx.annotation.Nullable;
import android.util.Log;

import org.joda.time.DateTime;

import java.util.ArrayList;

import io.github.jbytheway.rideottawa.db.Route;
import io.github.jbytheway.rideottawa.db.Stop;
import io.github.jbytheway.rideottawa.db.StopTime;
import io.github.jbytheway.rideottawa.db.Trip;

public class Alarm {
    private static final String TAG = "Alarm";

    public Alarm(FavouriteStop favouriteStop, TripUid tripUid, int minutesWarning, OnRefreshedListener listener, PendingAlarmData fromPendingData, OcTranspoDataAccess ocTranspo) {
        mFavouriteStop = favouriteStop;
        mFavourite = favouriteStop.Favourite;
        mMinutesWarning = minutesWarning;
        mOnRefreshedListener = listener;
        mFromPendingData = fromPendingData;

        mStop = favouriteStop.asStop(ocTranspo);
        mTrip = ocTranspo.getTrip(tripUid.getTripId());
        Stop lastStop = ocTranspo.getLastStopOf(mTrip);
        StopTime tripAtStop = ocTranspo.getTimeAtStop(mTrip, mStop);

        mForthcomingTrip =  new ForthcomingTrip(
                mStop, mTrip.getRoute(), mTrip.getHeadsign(), lastStop, tripUid.getTripId(),
                tripUid.getMidnight(), tripAtStop.Time, mTrip.getStartTime()
        );

        if (tripAtStop.StopSequence == 1) {
            Stop secondStop = ocTranspo.getNthStopOf(2, mTrip);
            StopTime secondStopTime = ocTranspo.getTimeAtStop(mTrip, secondStop);
            mForthcomingTrip.useSecondStop(secondStop, secondStopTime.Time);
        }

        mTimeEstimate = mForthcomingTrip.getEstimatedArrival().getTime();
    }

    public interface OnRefreshedListener {
        void onRefreshed(Alarm alarm);
    }

    public Stop getStop() {
        return mStop;
    }

    public Route getRoute() {
        return mTrip.getRoute();
    }

    public Favourite getFavourite() {
        return mFavourite;
    }

    public FavouriteStop getFavouriteStop() {
        return mFavouriteStop;
    }

    public DateTime getTimeOfBus() {
        return mTimeEstimate;
    }

    public DateTime getTimeOfAlarm() {
        return mTimeEstimate.minusMinutes(mMinutesWarning);
    }

    public ForthcomingTrip getForthcomingTrip() {
        return mForthcomingTrip;
    }

    public PendingAlarmData makePendingAlarmData(long timeToCheck) {
        // We try to update the existing PendingAlarmData where possible
        if (mFromPendingData == null) {
            return new PendingAlarmData(mForthcomingTrip.getTripUid(), mFavouriteStop.getId(), mMinutesWarning, timeToCheck);
        } else {
            mFromPendingData.setTimeToCheck(timeToCheck);
            return mFromPendingData;
        }
    }

    public void delete() {
        if (mFromPendingData != null) {
            mFromPendingData.delete();
        }
    }

    public void refreshTimeEstimate(OcTranspoApi.Synchronicity synchronicity, Context context, OcTranspoDataAccess ocTranspo) {
        ArrayList<ForthcomingTrip> forthcomingTrips = new ArrayList<>();
        forthcomingTrips.add(mForthcomingTrip);

        Log.d(TAG, "Alarm getting live data");
        ocTranspo.getLiveDataForTrips(context, forthcomingTrips, synchronicity, new OcTranspoApi.Listener() {
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

    private final Stop mStop;
    private final Trip mTrip;
    private final FavouriteStop mFavouriteStop;
    private final Favourite mFavourite;
    private final int mMinutesWarning;
    private final ForthcomingTrip mForthcomingTrip;
    private final OnRefreshedListener mOnRefreshedListener;
    private final PendingAlarmData mFromPendingData;
    private DateTime mTimeEstimate;
}
