package io.github.jbytheway.octranspoalarm;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class OcTranspoDataAccess {
    OcTranspoDataAccess(Context context) {
        mHelper = new OcTranspoDbHelper(context);
        mDatabase = mHelper.getReadableDatabase();
    }

    public class Stop {
        Stop(String id, String code, String name) {
            mId = id;
            mCode = code;
            mName = name;
        }

        public String getId() { return mId; }
        public String getCode() { return mCode; }
        public String getName() { return mName; }

        private String mId;
        private String mCode;
        private String mName;
    }

    private static final String[] STOP_COLUMNS = new String[]{"_id", "stop_id", "stop_code", "stop_name"};

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
