package io.github.jbytheway.rideottawa;

import java.util.Objects;

public class TimeQuery {
    public TimeQuery(String stopCode, io.github.jbytheway.rideottawa.Route route) {
        StopCode = stopCode;
        Route = route;
    }

    public String StopCode;
    public Route Route;

    @Override
    public int hashCode() {
        return Objects.hash(StopCode, Route);
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof TimeQuery) {
            TimeQuery other = (TimeQuery) o;
            return StopCode.equals(other.StopCode) && Route.equals(other.Route);
        }

        return false;
    }
}
