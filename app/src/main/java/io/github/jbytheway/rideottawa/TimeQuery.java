package io.github.jbytheway.rideottawa;

import java.util.Objects;

import io.github.jbytheway.rideottawa.db.Route;

/*
 * This class is for encapsulating the information needed to query the API for GPS data.
 *
 * In particular, it is hashable so that ForthComingTrips can be grouped into sets which should
 * be queried together.
 */
class TimeQuery {
    public TimeQuery(String stopCodeWanted, String stopCodeToQuery, io.github.jbytheway.rideottawa.db.Route route) {
        StopCodeWanted = stopCodeWanted;
        StopCodeToQuery = stopCodeToQuery;
        Route = route;
    }

    // StopCodeWanted is currently unnecessary, but it seems a good idea to leave
    public final String StopCodeWanted;
    public final String StopCodeToQuery;
    public final Route Route;

    @Override
    public int hashCode() {
        return Objects.hash(StopCodeWanted, StopCodeToQuery, Route);
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof TimeQuery) {
            TimeQuery other = (TimeQuery) o;
            return StopCodeWanted.equals(other.StopCodeWanted) &&
                    StopCodeToQuery.equals(other.StopCodeToQuery) &&
                    Route.equals(other.Route);
        }

        return false;
    }
}
