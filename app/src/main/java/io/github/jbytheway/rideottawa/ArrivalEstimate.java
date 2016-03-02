package io.github.jbytheway.rideottawa;

import android.support.annotation.NonNull;

import org.joda.time.DateTime;

public class ArrivalEstimate implements java.lang.Comparable<ArrivalEstimate> {
    public enum Type {
        Schedule,
        Gps,
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
