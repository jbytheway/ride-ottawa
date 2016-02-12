package io.github.jbytheway.octranspoalarm;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Objects;

public class Route implements Parcelable {
    Route(String name, int direction) {
        //mId = id;
        mName = name;
        mDirection = direction;
    }

    //public String getRouteId() { return mId; }
    public String getName() { return mName; }
    public int getDirection() { return mDirection; }

    //private String mId;
    private String mName;
    private int mDirection;

    @Override
    public int hashCode() {
        return Objects.hash(mName, mDirection);
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Route) {
            Route other = (Route) o;
            return getName().equals(other.getName()) && getDirection() == other.getDirection();
        }

        return false;
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel out, int flags) {
        out.writeString(mName);
        out.writeInt(mDirection);
    }

    public static final Parcelable.Creator<Route> CREATOR = new Parcelable.Creator<Route>() {
        public Route createFromParcel(Parcel in) {
            String name = in.readString();
            int direction = in.readInt();
            return new Route(name, direction);
        }

        public Route[] newArray(int size) {
            return new Route[size];
        }
    };
}
