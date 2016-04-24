package io.github.jbytheway.rideottawa.db;

import io.github.jbytheway.rideottawa.db.Route;

public class Trip {
    public Trip(long id, Route route, int startTime, String headsign) {
        mId = id;
        mRoute = route;
        mStartTime = startTime;
        mHeadsign = headsign;
    }

    public long getId() { return mId; }
    public Route getRoute() { return mRoute; }
    public int getStartTime() { return mStartTime; }
    public String getHeadsign() { return mHeadsign; }

    private final long mId;
    private final Route mRoute;
    private final int mStartTime;
    private final String mHeadsign;
}
