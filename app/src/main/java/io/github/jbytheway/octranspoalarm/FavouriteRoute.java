package io.github.jbytheway.octranspoalarm;

import com.orm.SugarRecord;

public class FavouriteRoute extends SugarRecord {
    @SuppressWarnings("unused")
    public FavouriteRoute() {
        // Required for Sugar
    }

    public void saveRecursively() {
        save();
    }

    public void deleteRecursively() {
        delete();
    }

    public String RouteId;
    public String RouteName;
    public FavouriteStop Stop;
}