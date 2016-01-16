package io.github.jbytheway.octranspoalarm;

import com.orm.SugarRecord;
import com.orm.dsl.Ignore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

public class FavouriteStop extends SugarRecord {
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
        return all;
    }

    public void addRoute(Route r) {
        addRoute(r.getName(), r.getDirection());
    }

    public void addRoute(String routeName, int direction) {
        FavouriteRoute route = new FavouriteRoute();
        route.Stop = this;
        route.RouteName = routeName;
        route.Direction = direction;
        mPendingRoutes.add(route);
    }

    public void updateRoutes(List<Route> routes, OcTranspoDataAccess ocTranspo) {
        // This is supposed to set the current set of routes to be those given in the argument
        HashSet<Route> desiredRoutes = new HashSet<>(routes);

        for (FavouriteRoute favRoute : getRoutes()) {
            Route rawRoute = favRoute.asRoute();
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
            addRoute(newRoute);
        }
    }

    public String StopId;
    public Favourite Favourite;

    @Ignore
    private ArrayList<FavouriteRoute> mPendingRoutes;
}
