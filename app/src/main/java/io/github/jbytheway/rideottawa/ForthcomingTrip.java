package io.github.jbytheway.rideottawa;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

public class ForthcomingTrip {
    public ForthcomingTrip(Stop stop, Route route, String headSign, int tripId, DateTime midnight, int time, int startTime) {
        mStop = stop;
        mRoute = route;
        mHeadSign = headSign;
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

    public int getTripId() {
        return mTripId;
    }

    public String getArrivalTimeString() {
        return stringifyTime(mTime);
    }

    public DateTime getArrivalTimeDateTime() {
        return mMidnight.plusMinutes(mTime);
    }

    public String getStartTimeString() {
        return stringifyTime(mStartTime);
    }

    public ArrivalEstimate getEstimatedArrival() {
        if (mEstimatedArrival != null) {
            return new ArrivalEstimate(mEstimatedArrival, ArrivalEstimate.Type.Gps);
        }

        return new ArrivalEstimate(getArrivalTimeDateTime(), ArrivalEstimate.Type.Schedule);
    }

    public void provideLiveData(DateTime processingTime, int minutesAway) {
        mEstimatedArrival = processingTime.plusMinutes(minutesAway);
    }

    private String stringifyTime(int time) {
        DateTime dateTime = mMidnight.plusMinutes(time);
        DateTimeFormatter formatter = DateTimeFormat.forPattern("HH:mm");
        return formatter.print(dateTime);
    }

    private final Stop mStop;
    private final Route mRoute;
    private final String mHeadSign;
    private final int mTripId;
    private final DateTime mMidnight; // the origin from which times are measured
    private final int mTime;
    private final int mStartTime;
    private DateTime mEstimatedArrival;
}
