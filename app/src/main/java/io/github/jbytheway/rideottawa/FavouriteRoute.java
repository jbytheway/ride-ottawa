package io.github.jbytheway.rideottawa;

import android.database.Cursor;

import com.orm.SugarRecord;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class FavouriteRoute extends SugarRecord {
    @SuppressWarnings("unused")
    public FavouriteRoute() {
        // Required for Sugar
    }

    public Route asRoute() {
        return new Route(RouteName, Direction);
    }

    public void saveRecursively() {
        save();
    }

    public void deleteRecursively() {
        delete();
    }

    public ArrayList<ForthcomingTrip> updateForthcomingTrips(OcTranspoDataAccess ocTranspo, ArrayList<ForthcomingTrip> oldTrips) {
        Cursor c = ocTranspo.getForthcomingTrips(Stop.StopId, RouteName, Direction);
        List<ForthcomingTrip> newTrips = ocTranspo.stopTimeCursorToList(c);

        // We want to keep the old trips where they match the new ones, but add also the new ones
        // and not include old ones which aren't in the new ones

        // Convert old to HashMap
        HashMap<Integer, ForthcomingTrip> oldTripsById = new HashMap<>();

        for (ForthcomingTrip trip : oldTrips) {
            oldTripsById.put(trip.getTripId(), trip);
        }

        ArrayList<ForthcomingTrip> result = new ArrayList<>();

        for (ForthcomingTrip trip : newTrips) {
            int key = trip.getTripId();
            if (oldTripsById.containsKey(key)) {
                result.add(oldTripsById.get(key));
            } else {
                result.add(trip);
            }
        }

        return result;
    }

    public String RouteName;
    public int Direction;
    public FavouriteStop Stop;
}