package io.github.jbytheway.rideottawa;

public class Trip {
    Trip(long id, Route route, int startTime) {
        mId = id;
        mRoute = route;
        mStartTime = startTime;
    }

    public long getId() { return mId; }
    public Route getRoute() { return mRoute; }
    public int getStartTime() { return mStartTime; }

    private final long mId;
    private final Route mRoute;
    private final int mStartTime;
}
