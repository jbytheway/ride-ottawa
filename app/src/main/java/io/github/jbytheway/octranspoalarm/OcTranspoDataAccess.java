package io.github.jbytheway.octranspoalarm;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class OcTranspoDataAccess {
    OcTranspoDataAccess(Context context) {
        mHelper = new OcTranspoDbHelper(context);
        mDatabase = mHelper.getReadableDatabase();
    }

    Cursor getRoutesForStop(String stopName) {
        String[] args = {stopName};
        return mDatabase.rawQuery(
                "select distinct routes.route_id, route_short_name from stops " +
                "join stop_times on stops.id = stop_times.stop_id " +
                "join trips on trips.trip_id = stop_times.trip_id " +
                "join routes on trips.route_id = routes.route_id " +
                "where stop_code = ?" +
                "order by CAST(routes.route_short_name AS INTEGER)", args);
    }

    private OcTranspoDbHelper mHelper;
    private SQLiteDatabase mDatabase;
}
