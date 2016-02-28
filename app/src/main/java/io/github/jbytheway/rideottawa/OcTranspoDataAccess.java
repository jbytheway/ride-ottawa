package io.github.jbytheway.rideottawa;

import android.app.ProgressDialog;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.joda.time.Interval;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.github.jbytheway.rideottawa.utils.DownloadableDatabase;

public class OcTranspoDataAccess {
    @SuppressWarnings("unused")
    public static final String TAG = "OcTranspoDataAccess";

    OcTranspoDataAccess(Context context) {
        mHelper = new OcTranspoDbHelper(context);
        mApi = new OcTranspoApi(context);
        mOttawaTimeZone = DateTimeZone.forID("America/Toronto");
        mIsoDateFormatter = DateTimeFormat.forPattern("yyyyMMdd");
    }

    public void checkForUpdates(ProgressDialog progressDialog, DownloadableDatabase.UpdateListener listener) throws IOException {
        mHelper.checkForUpdates(progressDialog, listener);
    }

    private static final String[] STOP_COLUMNS = new String[]{"_id", "stop_id", "stop_code", "stop_name"};

    public Cursor getRoutesForStopById(String stopId) {
        SQLiteDatabase database = mHelper.getReadableDatabase();
        return database.rawQuery(
                "select distinct route_short_name, direction_id from stops " +
                "join stop_times on stops._id = stop_times.stop_id " +
                "join trips on trips.trip_id = stop_times.trip_id " +
                "join routes on trips.route_id = routes.route_id " +
                "where stops.stop_id = ?" +
                "order by CAST(routes.route_short_name AS INTEGER)", new String[]{stopId});
    }

    /*
    private static final String[] ROUTE_COLUMNS = new String[]{"route_short_name", "direction_id"};

    Cursor getRoutesByIds(Collection<String> routeIds) {
        String[] routeIdArray = routeIds.toArray(new String[routeIds.size()]);
rm         return getRoutesByIds(routeIdArray);
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

    public List<Route> routeCursorToList(Cursor c) {
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
        c.close();
        return result;
    }

    public Cursor getAllStops(String orderBy) {
        SQLiteDatabase database = mHelper.getReadableDatabase();
        return database.query("stops", STOP_COLUMNS, null, null, null, null, orderBy);
    }

    public Cursor getAllStopsMatching(String constraint, String orderBy) {
        SQLiteDatabase database = mHelper.getReadableDatabase();
        String namePattern = "%"+constraint+"%";
        String codePattern = constraint+"%";
        return database.query("stops", STOP_COLUMNS, "stop_name like ? or stop_code like ?", new String[]{namePattern, codePattern}, null, null, orderBy);
    }

    public Stop getStop(String stopId) {
        SQLiteDatabase database = mHelper.getReadableDatabase();
        Cursor c = database.query("stops", STOP_COLUMNS, "stop_id = ?", new String[]{stopId}, null, null, null);
        if (c.getCount() != 1) {
            throw new AssertionError("Requested invalid StopId " + stopId);
        }
        c.moveToFirst();
        int stopCodeColumn = c.getColumnIndex("stop_code");
        int stopNameColumn = c.getColumnIndex("stop_name");
        String stopCode = c.getString(stopCodeColumn);
        String stopName = c.getString(stopNameColumn);
        c.close();
        return new Stop(stopId, stopCode, stopName);
    }

    public Stop getStop(long id) {
        SQLiteDatabase database = mHelper.getReadableDatabase();
        Cursor c = database.query("stops", STOP_COLUMNS, "_id = ?", new String[]{""+id}, null, null, null);
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
        c.close();
        return new Stop(stopId, stopCode, stopName);
    }

    public DateTime getNow() {
        // Get the current instant in the Ottawa time zone
        DateTime now = new DateTime();
        return now.withZone(mOttawaTimeZone);
    }

    public Cursor getForthcomingTrips(String stopId, String routeName, int direction) {
        DateTime now = getNow();

        // Next we need to figure out the midnight that started this day in Ottawa
        DateTime startOfDay = now.withTimeAtStartOfDay();
        // And how much time has passed since then
        Interval intervalSinceMidnight = new Interval(startOfDay, now);
        Duration sinceMidnight = intervalSinceMidnight.toDuration();
        long minutesSinceMidnight = sinceMidnight.getStandardMinutes();
        long minTime = minutesSinceMidnight - 5;

        // Format the date in ISO format (which is what the db uses)
        String today = mIsoDateFormatter.print(now);

        // Now we can finally make a query
        SQLiteDatabase database = mHelper.getReadableDatabase();
        return database.rawQuery(
                "select stop_times.stop_id, stop_code, stop_name, route_short_name, direction_id, " +
                        "trip_headsign, date, stop_times.arrival_time, " +
                        "stop_times_start.arrival_time as start_arrival_time from stop_times " +
                        "join trips on stop_times.trip_id = trips.trip_id " +
                        "join days on days.service_id = trips.service_id " +
                        "join routes on trips.route_id = routes.route_id " +
                        "join stops on stop_times.stop_id = stops._id " +
                        "join stop_times as stop_times_start on stop_times_start.trip_id = trips.trip_id " +
                        "where stops.stop_id = ? " +
                        "and routes.route_short_name = ? " +
                        "and trips.direction_id = ? " +
                        "and days.date = ? " +
                        "and stop_times.arrival_time >= ? " +
                        "and stop_times_start.stop_sequence = 1 " +
                        "order by stop_times.arrival_time " +
                        "limit 10",
                new String[]{stopId, routeName, "" + direction, today, ""+minTime});

        // FIXME: also fetch trips with times which derive from the previous day (i.e. which started yesterday)
    }

    public List<ForthcomingTrip> stopTimeCursorToList(Cursor c) {
        ArrayList<ForthcomingTrip> result = new ArrayList<>();
        if (c.moveToFirst()) {
            int stop_id_column = c.getColumnIndex("stop_id");
            int stop_code_column = c.getColumnIndex("stop_code");
            int stop_name_column = c.getColumnIndex("stop_name");
            int route_name_column = c.getColumnIndex("route_short_name");
            int direction_column = c.getColumnIndex("direction_id");
            int head_sign_column = c.getColumnIndex("trip_headsign");
            int date_column = c.getColumnIndex("date");
            int arrival_time_column = c.getColumnIndex("arrival_time");
            int start_time_column = c.getColumnIndex("start_arrival_time");

            while (true) {
                String stopId = c.getString(stop_id_column);
                String stopCode = c.getString(stop_code_column);
                String stopName = c.getString(stop_name_column);
                String routeName = c.getString(route_name_column);
                int direction = c.getInt(direction_column);
                String headSign = c.getString(head_sign_column);
                String date = c.getString(date_column);
                int arrivalTime = c.getInt(arrival_time_column);
                int startTime = c.getInt(start_time_column);
                DateTime midnight = mIsoDateFormatter.parseDateTime(date).withZoneRetainFields(mOttawaTimeZone);
                //Log.d(TAG, "arrivalTime="+arrivalTime+", startTime="+startTime);
                result.add(new ForthcomingTrip(new Stop(stopId, stopCode, stopName), new Route(routeName, direction), headSign, midnight, arrivalTime, startTime));

                if (!c.moveToNext()) {
                    break;
                }
            }
        }
        c.close();
        return result;
    }

    public void getLiveDataForTrips(Context context, Collection<ForthcomingTrip> trips, OcTranspoApi.Listener apiListener) {
        // Uniqify the info we need to pass to the API
        // FIXME: Do we worry about cases where we don't think there should be a bus (because the last one was too long ago)
        // but in fact there is (because the last one is very late)?  Currently such will not be caught.
        HashMap<TimeQuery, ArrayList<ForthcomingTrip>> queries = new HashMap<>();
        for (ForthcomingTrip trip : trips) {
            TimeQuery query = new TimeQuery(trip.getStop().getCode(), trip.getRoute());
            if (queries.containsKey(query)) {
                queries.get(query).add(trip);
            } else {
                ArrayList<ForthcomingTrip> theseTrips = new ArrayList<>();
                theseTrips.add(trip);
                queries.put(query, theseTrips);
            }
        }

        // Trigger all those queries
        for (Map.Entry<TimeQuery, ArrayList<ForthcomingTrip>> entry : queries.entrySet()) {
            mApi.queryTimes(context, entry.getKey(), entry.getValue(), apiListener);
        }
    }

    private final OcTranspoDbHelper mHelper;
    private final OcTranspoApi mApi;
    private final DateTimeZone mOttawaTimeZone;
    private final DateTimeFormatter mIsoDateFormatter;
}