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

    public void addRoute(OcTranspoDataAccess.Route r) {
        addRoute(r.getRouteId(), r.getName());
    }

    public void addRoute(String routeId, String routeName) {
        FavouriteRoute route = new FavouriteRoute();
        route.Stop = this;
        route.RouteId = routeId;
        route.RouteName = routeName;
        mPendingRoutes.add(route);
    }

    public void updateRoutes(String[] routeIds, OcTranspoDataAccess ocTranspo) {
        // This is supposed to set the current set of routes to be those given in the argument
        HashSet<String> desiredRouteIds = new HashSet<>(Arrays.asList(routeIds));

        for (FavouriteRoute route : getRoutes()) {
            if (desiredRouteIds.contains(route.RouteId)) {
                desiredRouteIds.remove(route.RouteId);
            } else {
                if (route.getId() == null) {
                    mPendingRoutes.remove(route);
                } else {
                    route.deleteRecursively();
                }
            }
        }

        // At this point everything left in desiredRouteIds is a thing we need to add
        List<OcTranspoDataAccess.Route> newRoutes = ocTranspo.routeCursorToList(ocTranspo.getRoutesByIds(desiredRouteIds));
        for (OcTranspoDataAccess.Route newRoute : newRoutes) {
            addRoute(newRoute);
        }
    }

    public String StopId;
    public Favourite Favourite;

    @Ignore
    private ArrayList<FavouriteRoute> mPendingRoutes;
}
