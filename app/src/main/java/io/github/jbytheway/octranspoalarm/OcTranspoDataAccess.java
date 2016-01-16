package io.github.jbytheway.octranspoalarm;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.widget.LinearLayout;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

public class OcTranspoDataAccess {
    OcTranspoDataAccess(Context context) {
        mHelper = new OcTranspoDbHelper(context);
        mDatabase = mHelper.getReadableDatabase();
    }

    private static final String[] STOP_COLUMNS = new String[]{"_id", "stop_id", "stop_code", "stop_name"};
    private static final String[] ROUTE_COLUMNS = new String[]{"route_short_name", "direction_id"};

    Cursor getRoutesForStopById(String stopId) {
        String[] args = {stopId};
        return mDatabase.rawQuery(
                "select distinct route_short_name, direction_id from stops " +
                "join stop_times on stops._id = stop_times.stop_id " +
                "join trips on trips.trip_id = stop_times.trip_id " +
                "join routes on trips.route_id = routes.route_id " +
                "where stops.stop_id = ?" +
                "order by CAST(routes.route_short_name AS INTEGER)", args);
    }

    /*
    Cursor getRoutesByIds(Collection<String> routeIds) {
        String[] routeIdArray = routeIds.toArray(new String[routeIds.size()]);
        return getRoutesByIds(routeIdArray);
    }

    Cursor getRoutesByIds(String[] routeIds) {
        String queryList;
        if (routeIds.length == 0) {
            queryList = "";
        } else {
            queryList = "?" + StringUtils.repeat(",?", routeIds.length - 1);
        }
        return mDatabase.query("routes", ROUTE_COLUMNS, "route_id in ("+queryList+")", routeIds, null, null, null);
    }
    */

    List<Route> routeCursorToList(Cursor c) {
        ArrayList<Route> result = new ArrayList<>();
        if (c.moveToFirst()) {
            //int id_column = c.getColumnIndex("route_id");
            int name_column = c.getColumnIndex("route_short_name");
            int direction_column = c.getColumnIndex("direction_id");

            while (true) {
                //String id = c.getString(id_column);
                String name = c.getString(name_column);
                int direction = c.getInt(direction_column);
                result.add(new Route(name, direction));

                if (!c.moveToNext()) {
                    break;
                }
            }
        }
        return result;
    }

    Cursor getAllStops(String orderBy) {
        return mDatabase.query("stops", STOP_COLUMNS, null, null, null, null, orderBy);
    }

    Stop getStop(String stopId) {
        Cursor c = mDatabase.query("stops", STOP_COLUMNS, "stop_id = ?", new String[]{stopId}, null, null, null);
        if (c.getCount() != 1) {
            throw new AssertionError("Requested invalid StopId " + stopId);
        }
        c.moveToFirst();
        int stopCodeColumn = c.getColumnIndex("stop_code");
        int stopNameColumn = c.getColumnIndex("stop_name");
        String stopCode = c.getString(stopCodeColumn);
        String stopName = c.getString(stopNameColumn);
        return new Stop(stopId, stopCode, stopName);
    }

    Stop getStop(long id) {
        Cursor c = mDatabase.query("stops", STOP_COLUMNS, "_id = ?", new String[]{""+id}, null, null, null);
        if (c.getCount() != 1) {
            throw new AssertionError("Requested invalid StopId " + id);
        }
        c.moveToFirst();
        int stopIdColumn = c.getColumnIndex("stop_id");
        int stopCodeColumn = c.getColumnIndex("stop_code");
        int stopNameColumn = c.getColumnIndex("stop_name");
        String stopId = c.getString(stopIdColumn);
        String stopCode = c.getString(stopCodeColumn);
        String stopName = c.getString(stopNameColumn);
        return new Stop(stopId, stopCode, stopName);
    }

    private OcTranspoDbHelper mHelper;
    private SQLiteDatabase mDatabase;
}
