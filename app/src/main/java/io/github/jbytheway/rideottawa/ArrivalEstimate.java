package io.github.jbytheway.rideottawa;

import org.joda.time.DateTime;

public class ArrivalEstimate {
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

    DateTime mTime;
    Type mType;
}
