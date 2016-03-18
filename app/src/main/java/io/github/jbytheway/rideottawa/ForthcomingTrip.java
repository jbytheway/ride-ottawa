package io.github.jbytheway.rideottawa;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

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

    public ArrivalEstimate getEstimatedArrival() {
        if (mEstimatedArrival != null) {
            ArrivalEstimate.Type type;
            if (mEstimateAge != null && mEstimateAge > 1) {
                type = ArrivalEstimate.Type.GpsOld;
            } else {
                type = ArrivalEstimate.Type.Gps;
            }
            return new ArrivalEstimate(mEstimatedArrival, type);
        }

        return new ArrivalEstimate(getArrivalTime(), ArrivalEstimate.Type.Schedule);
    }

    public void provideLiveData(DateTime processingTime, int minutesAway, double estimateAge) {
        mEstimatedArrival = processingTime.plusMinutes(minutesAway);
        mEstimateAge = estimateAge;
    }

    private final Stop mStop;
    private final Route mRoute;
    private final String mHeadSign;
    private final Stop mLastStop;
    private final int mTripId;
    private final DateTime mMidnight; // the origin from which times are measured
    private final int mTime;
    private final int mStartTime;
    private DateTime mEstimatedArrival;
    private Double mEstimateAge;
}
