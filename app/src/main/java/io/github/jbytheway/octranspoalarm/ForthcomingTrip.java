package io.github.jbytheway.octranspoalarm;

public class ForthcomingTrip {
    public ForthcomingTrip(Stop stop, Route route, String headSign, int time) {
        mStop = stop;
        mRoute = route;
        mHeadSign = headSign;
        mTime = time;
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

    public String getArrivalTimeString() {
        // FIXME: does the wrong thing on days when time changes
        int hours = (mTime / 60) % 24;
        int minutes = mTime % 60;
        return String.format("%02d:%02d", hours, minutes);
    }

    Stop mStop;
    Route mRoute;
    String mHeadSign;
    int mTime;
}
