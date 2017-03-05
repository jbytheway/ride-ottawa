package io.github.jbytheway.rideottawa;

import android.os.Parcel;
import android.os.Parcelable;

import org.joda.time.DateTime;

import java.util.Objects;

public class TripUid implements Parcelable {
    public TripUid(int id, DateTime midnight) {
        if (!midnight.getZone().getID().equals("America/Toronto")) {
            throw new AssertionError("Time in wrong time zone (" + midnight.getZone().getID() + ")");
        }
        mTripId = id;
        mMidnight = midnight;
    }

    public int getTripId() {
        return mTripId;
    }

    public DateTime getMidnight() {
        return mMidnight;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof TripUid) {
            TripUid other = (TripUid) o;
            return mTripId == other.mTripId && mMidnight.equals(other.mMidnight);
        }

        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(mTripId, mMidnight);
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel out, int flags) {
        out.writeInt(mTripId);
        out.writeSerializable(mMidnight);
    }

    public static final Parcelable.Creator<TripUid> CREATOR = new Parcelable.Creator<TripUid>() {
        public TripUid createFromParcel(Parcel in) {
            int tripId = in.readInt();
            DateTime midnight = (DateTime) in.readSerializable();
            return new TripUid(tripId, midnight);
        }

        public TripUid[] newArray(int size) {
            return new TripUid[size];
        }
    };

    private final int mTripId;
    private final DateTime mMidnight;
}
