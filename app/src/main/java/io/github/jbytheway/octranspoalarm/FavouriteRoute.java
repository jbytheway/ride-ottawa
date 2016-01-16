package io.github.jbytheway.octranspoalarm;

import com.orm.SugarRecord;

public class FavouriteRoute extends SugarRecord {
    @SuppressWarnings("unused")
    public FavouriteRoute() {
        // Required for Sugar
    }

    Route asRoute() {
        return new Route(RouteName, Direction);
    }

    public void saveRecursively() {
        save();
    }

    public void deleteRecursively() {
        delete();
    }

    public String RouteName;
    public int Direction;
    public FavouriteStop Stop;
}