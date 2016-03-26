package io.github.jbytheway.rideottawa;

import com.orm.SugarRecord;
import com.orm.dsl.Ignore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public class FavouriteStop extends SugarRecord {
    @SuppressWarnings("unused")
    private static final String TAG = "FavouriteStop";

    @SuppressWarnings("unused")
    public FavouriteStop() {
        // Required for Sugar
        mPendingRoutes = new ArrayList<>();
    }

    public void saveRecursively() {
        save();
        for (FavouriteRoute route : mPendingRoutes) {
            route.saveRecursively();
        }
        mPendingRoutes.clear();
        for (FavouriteRoute route : getRoutes()) {
            route.saveRecursively();
        }
    }

    public void deleteRecursively() {
        for (FavouriteRoute route : getRoutes()) {
            route.deleteRecursively();
        }
        delete();
    }

    public List<FavouriteRoute> getRoutes() {
        ArrayList<FavouriteRoute> all = new ArrayList<>();
        Long id = getId();
        if (id != null) {
            all.addAll(FavouriteRoute.find(FavouriteRoute.class, "stop = ?", id.toString()));
        }
        all.addAll(mPendingRoutes);
        Collections.sort(all);
        return all;
    }

    public void addRoute(Route r, String destination) {
        addRoute(r.getName(), r.getDirection(), destination);
    }

    public void addRoute(String routeName, int direction, String destination) {
        FavouriteRoute route = new FavouriteRoute();
        route.Stop = this;
        route.RouteName = routeName;
        route.Direction = direction;
        route.Destination = destination;
        mPendingRoutes.add(route);
    }

    public void updateRoutes(List<Route> routes, OcTranspoDataAccess ocTranspo) {
        // This is supposed to set the current set of routes to be those given in the argument
        HashSet<Route> desiredRoutes = new HashSet<>(routes);

        for (FavouriteRoute favRoute : getRoutes()) {
            Route rawRoute = favRoute.asRoute(ocTranspo);
            if (desiredRoutes.contains(rawRoute)) {
                desiredRoutes.remove(rawRoute);
            } else {
                if (favRoute.getId() == null) {
                    mPendingRoutes.remove(favRoute);
                } else {
                    favRoute.deleteRecursively();
                }
            }
        }

        // At this point everything left in desiredRoutes is a thing we need to add
        for (Route newRoute : desiredRoutes) {
            addRoute(newRoute, null);
        }
    }

    public void includeRoutes(List<Route> routes, String destStopId, OcTranspoDataAccess ocTranspo) {
        HashSet<Route> newRoutes = new HashSet<>(routes);

        // First add destination to routes we already have
        for (FavouriteRoute favRoute : getRoutes()) {
            Route rawRoute = favRoute.asRoute(ocTranspo);
            if (newRoutes.contains(rawRoute)) {
                if (favRoute.Destination == null) {
                    favRoute.Destination = destStopId;
                    if (favRoute.getId() != null) {
                        favRoute.saveRecursively();
                    }
                }
                newRoutes.remove(rawRoute);
            }
        }

        // At this point everything left in newRoutes is a thing we need to add
        for (Route newRoute : newRoutes) {
            addRoute(newRoute, destStopId);
        }
    }

    public List<ForthcomingTrip> updateForthcomingTrips(ArrayList<ForthcomingTrip> trips, OcTranspoDataAccess ocTranspo) {
        if (!mPendingRoutes.isEmpty()) {
            throw new AssertionError("Should only be called on saved Stops");
        }

        // First split the input trips by route
        HashMap<Route, ArrayList<ForthcomingTrip>> tripsSplit = new HashMap<>();

        for (ForthcomingTrip trip : trips) {
            Route key = trip.getRoute();
            if (tripsSplit.containsKey(key)) {
                tripsSplit.get(key).add(trip);
            } else {
                ArrayList<ForthcomingTrip> newList = new ArrayList<>();
                newList.add(trip);
                tripsSplit.put(key, newList);
            }
        }

        ArrayList<ForthcomingTrip> result = new ArrayList<>();
        for (FavouriteRoute route : getRoutes()) {
            Route key = route.asRoute(ocTranspo);
            ArrayList<ForthcomingTrip> tripsForThisRoute;
            if (tripsSplit.containsKey(key)) {
                tripsForThisRoute = tripsSplit.get(key);
            } else {
                tripsForThisRoute = new ArrayList<>();
            }
            result.addAll(route.updateForthcomingTrips(ocTranspo, tripsForThisRoute));
        }

        return result;
    }

    public String StopId;
    @SuppressWarnings("unused")
    public Favourite Favourite;

    @Ignore
    private final ArrayList<FavouriteRoute> mPendingRoutes;
}
