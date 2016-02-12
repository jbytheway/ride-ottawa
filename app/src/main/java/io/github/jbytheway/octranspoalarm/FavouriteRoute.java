package io.github.jbytheway.octranspoalarm;

import android.database.Cursor;

import com.orm.SugarRecord;

import java.util.List;

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

    public List<ForthcomingTrip> getForthcomingTrips(OcTranspoDataAccess ocTranspo) {
        Cursor c = ocTranspo.getForthcomingTrips(Stop.StopId, RouteName, Direction);
        return ocTranspo.stopTimeCursorToList(c);
    }

    public String RouteName;
    public int Direction;
    public FavouriteStop Stop;
}