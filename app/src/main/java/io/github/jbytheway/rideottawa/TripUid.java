package io.github.jbytheway.rideottawa;

import org.joda.time.DateTime;

import java.util.Objects;

public class TripUid {
    public TripUid(int id, DateTime midnight) {
        mId = id;
        mMidnight = midnight;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof TripUid) {
            TripUid other = (TripUid) o;
            return mId == other.mId && mMidnight.equals(other.mMidnight);
        }

        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(mId, mMidnight);
    }

    private final int mId;
    private final DateTime mMidnight;
}
