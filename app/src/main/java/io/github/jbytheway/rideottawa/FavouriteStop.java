package io.github.jbytheway.rideottawa;

import com.google.common.collect.HashMultimap;
import com.orm.SugarRecord;
import com.orm.dsl.Ignore;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.github.jbytheway.rideottawa.db.Route;
import io.github.jbytheway.rideottawa.db.Stop;

public class FavouriteStop extends SugarRecord {
    @SuppressWarnings("unused")
    private static final String TAG = "FavouriteStop";

    public static final int DEFAULT_MINUTES_WARNING = 5;

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

    public Stop asStop(OcTranspoDataAccess ocTranspo) {
        return ocTranspo.getStop(StopId);
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

    public List<ForthcomingTrip> updateForthcomingTrips(Collection<ForthcomingTrip> trips, OcTranspoDataAccess ocTranspo) {
        if (!mPendingRoutes.isEmpty()) {
            throw new AssertionError("Should only be called on saved Stops");
        }

        // First split the input trips by route
        HashMultimap<Route, ForthcomingTrip> tripsSplit = HashMultimap.create();

        for (ForthcomingTrip trip : trips) {
            Route key = trip.getRoute();
            tripsSplit.put(key, trip);
        }

        ArrayList<ForthcomingTrip> result = new ArrayList<>();
        for (FavouriteRoute route : getRoutes()) {
            Route key = route.asRoute(ocTranspo);
            Set<ForthcomingTrip> tripsForThisRoute = tripsSplit.get(key);
            result.addAll(route.updateForthcomingTrips(ocTranspo, tripsForThisRoute));
        }

        return result;
    }

    public String StopId;
    @SuppressWarnings("unused")
    public Favourite Favourite;
    public int MinutesWarning;

    @Ignore
    private final ArrayList<FavouriteRoute> mPendingRoutes;
}
