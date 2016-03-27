package io.github.jbytheway.rideottawa;

import org.joda.time.DateTime;

public class ForthcomingTrip {
    public ForthcomingTrip(Stop stop, Route route, String headSign, Stop lastStop, int tripId, DateTime midnight, int time, int startTime) {
        mStop = stop;
        mRoute = route;
        mHeadSign = headSign;
        mLastStop = lastStop;
        mTripId = tripId;
        mMidnight = midnight;
        mTime = time;
        mStartTime = startTime;
    }

    public Stop getStop() {
        return mStop;
    }

    public Route getRoute() {
        return mRoute;
    }

    public String getHeadSign() {
        return mHeadSign;
    }

    public Stop getLastStop() { return mLastStop; }

    public int getTripId() {
        return mTripId;
    }

    public DateTime getArrivalTime() {
        return mMidnight.plusMinutes(mTime);
    }

    public DateTime getStartTime() {
        return mMidnight.plusMinutes(mStartTime);
    }

    public TripUid getTripUid() { return new TripUid(getTripId(), mMidnight); }

    public ArrivalEstimate getEstimatedArrival() {
        if (mEstimatedArrival != null) {
            ArrivalEstimate.Type type;
            if (mNoGpsOnLastData) {
                type = ArrivalEstimate.Type.NoLongerGps;
            } else if (mEstimateAge != null && mEstimateAge > 1) {
                type = ArrivalEstimate.Type.GpsOld;
            } else {
                type = ArrivalEstimate.Type.Gps;
            }
            return new ArrivalEstimate(mEstimatedArrival, type);
        }

        return new ArrivalEstimate(getArrivalTime(), ArrivalEstimate.Type.Schedule);
    }

    public void notifyLiveResponseReceived() {
        // This is called before the below (provideLiveData) is called
        // For trips where no GPS data is available, only this is called and provideLiveData is not
        mNoGpsOnLastData = true;
    }

    public void provideLiveData(DateTime processingTime, int minutesAway, double estimateAge) {
        mEstimatedArrival = processingTime.plusMinutes(minutesAway);
        mEstimateAge = estimateAge;
        mNoGpsOnLastData = false;
    }

    private final Stop mStop;
    private final Route mRoute;
    private final String mHeadSign;
    private final Stop mLastStop;
    private final int mTripId;
    private final DateTime mMidnight; // the origin from which times are measured
    private final int mTime;
    private final int mStartTime;
    private boolean mNoGpsOnLastData;
    private DateTime mEstimatedArrival;
    private Double mEstimateAge;
}
