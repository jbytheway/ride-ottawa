package io.github.jbytheway.rideottawa;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.graphics.ColorUtils;

import java.util.Objects;

public class Route implements Parcelable {
    @SuppressWarnings("unused")
    private static final String TAG = "Route";

    Route(String name, int direction) {
        //mId = id;
        mName = name;
        mDirection = direction;
    }

    //public String getRouteId() { return mId; }
    public String getName() { return mName; }
    public int getDirection() { return mDirection; }

    //private String mId;
    private final String mName;
    private final int mDirection;

    public int getColour() {
        final double INT_RANGE = Math.pow(2, 32);
        int hash = mName.hashCode() * 1284865837;
        double hue = hash * 360.0 / INT_RANGE;
        // Make colours for opposite direction complementary
        if (mDirection == 1) {
            hue += 180;
        }
        // Make sure hue is in valid range
        if (hue < 0) {
            hue += 360;
        } else {
            hue %= 360;
        }
        float saturation = 1;
        float lightness = 0.7f;
        return ColorUtils.HSLToColor(new float[]{(float) hue, saturation, lightness});
    }

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
