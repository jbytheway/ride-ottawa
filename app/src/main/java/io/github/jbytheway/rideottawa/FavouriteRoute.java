package io.github.jbytheway.rideottawa;

import android.database.Cursor;
import android.support.annotation.NonNull;

import com.orm.SugarRecord;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

public class FavouriteRoute extends SugarRecord implements Comparable<FavouriteRoute> {
    @SuppressWarnings("unused")
    private static final String TAG = "FavouriteRoute";

    @SuppressWarnings("unused")
    public FavouriteRoute() {
        // Required for Sugar
    }

    public Route asRoute(OcTranspoDataAccess ocTranspo) {
        return ocTranspo.getRoute(RouteName, Direction);
    }

    public void saveRecursively() {
        save();
    }

    public void deleteRecursively() {
        delete();
    }

    public ArrayList<ForthcomingTrip> updateForthcomingTrips(OcTranspoDataAccess ocTranspo, Collection<ForthcomingTrip> oldTrips) {
        Cursor c = ocTranspo.getForthcomingTrips(Stop.StopId, RouteName, Direction, Destination);
        List<ForthcomingTrip> newTrips = ocTranspo.stopTimeCursorToList(c);

        // We want to keep the old trips where they match the new ones, but add also the new ones
        // and not include old ones which aren't in the new ones

        // Convert old to HashMap
        HashMap<TripUid, ForthcomingTrip> oldTripsById = new HashMap<>();

        for (ForthcomingTrip trip : oldTrips) {
            oldTripsById.put(trip.getTripUid(), trip);
        }

        ArrayList<ForthcomingTrip> result = new ArrayList<>();

        for (ForthcomingTrip trip : newTrips) {
            TripUid key = trip.getTripUid();
            if (oldTripsById.containsKey(key)) {
                result.add(oldTripsById.get(key));
            } else {
                result.add(trip);
            }
        }

        return result;
    }

    @Override
    public int compareTo(@NonNull FavouriteRoute another) {
        int routeCompare;
        try {
            int myRoute = Integer.parseInt(RouteName);
            int otherRoute = Integer.parseInt(another.RouteName);
            routeCompare = Integer.compare(myRoute, otherRoute);
        } catch (NumberFormatException e) {
            routeCompare = RouteName.compareTo(another.RouteName);
        }
        if (routeCompare != 0) {
            return routeCompare;
        }
        return Integer.compare(Direction, another.Direction);
    }

    public String RouteName;
    public int Direction;
    public String Destination;
    public FavouriteStop Stop;
}