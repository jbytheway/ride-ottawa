package io.github.jbytheway.rideottawa;

import androidx.annotation.NonNull;

import org.joda.time.DateTime;

public class ArrivalEstimate implements java.lang.Comparable<ArrivalEstimate> {
    public enum Type {
        Schedule, // No GPS data received
        LastStop, // Last stop, so we cannot expect any GPS data to be received
        Gps, // GPS data received
        GpsOld, // We received GPS data, but it's suspiciously outdated
        NoLongerGps, // We once received GPS data, but last time we checked we didn't
    }

    ArrivalEstimate(DateTime time, Type type) {
        mTime = time;
        mType = type;
    }

    public DateTime getTime() {
        return mTime;
    }

    public Type getType() {
        return mType;
    }

    @Override
    public int compareTo(@NonNull ArrivalEstimate r) {
        return getTime().compareTo(r.getTime());
    }

    private final DateTime mTime;
    private final Type mType;
}
