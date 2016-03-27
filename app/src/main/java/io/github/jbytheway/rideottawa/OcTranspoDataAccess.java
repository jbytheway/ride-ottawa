package io.github.jbytheway.rideottawa;

import android.app.ProgressDialog;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.support.annotation.Nullable;
import android.util.Log;

import com.orm.dsl.NotNull;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.joda.time.Interval;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

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

    public void checkForUpdates(boolean wifiOnly, DateTime ifOlderThan, ProgressDialog progressDialog, DownloadableDatabase.UpdateListener listener) {
        mHelper.checkForUpdates(wifiOnly, ifOlderThan, progressDialog, listener);
    }

    public boolean isDatabaseAvailable() {
        return mHelper.isDatabaseAvailable();
    }

    public @Nullable DateTime getLastUpdateCheck() {
        return mHelper.getLastUpdateCheck();
    }

    public @Nullable DateTime getDatabaseEndDate() {
        if (isDatabaseAvailable()) {
            SQLiteDatabase database = mHelper.getReadableDatabase();
            Cursor c = database.rawQuery(
                    "select date from days order by date desc limit 1", new String[]{});
            int col = c.getColumnIndex("date");
            c.moveToFirst();
            String date = c.getString(col);
            c.close();
            return mIsoDateFormatter.parseDateTime(date);
        } else {
            return null;
        }
    }

    @SuppressWarnings("unused")
    public void deleteDatabase() {
        mHelper.deleteDatabase();
    }

    private static final String[] ROUTE_COLUMNS = new String[]{"route_short_name", "directed_routes.direction_id", "route_modal_headsign"};

    private static final String[] STOP_COLUMNS = new String[]{"stops._id", "stops.stop_id", "stops.stop_code", "stops.stop_name"};

    public Route getRoute(String routeName, int directionId) {
        SQLiteDatabase database = mHelper.getReadableDatabase();
        String cols = StringUtils.join(ROUTE_COLUMNS, ", ");
        Cursor c = database.rawQuery(
                "select distinct " + cols + " " +
                        "from routes " +
                        "join directed_routes on routes.route_id = directed_routes.route_id " +
                        "where routes.route_short_name = ? and directed_routes.direction_id = ?",
                new String[]{routeName, "" + directionId});
        if (c.getCount() != 1) {
            throw new AssertionError("Requested invalid Route " + routeName + ", " + directionId);
        }
        List<Route> routes = routeCursorToList(c);
        return routes.get(0);
    }

    public Cursor getRoutesForStopById(String stopId) {
        SQLiteDatabase database = mHelper.getReadableDatabase();
        String cols = StringUtils.join(ROUTE_COLUMNS, ", ");
        return database.rawQuery(
                "select distinct " + cols + " from stops " +
                        "join stop_times on stops._id = stop_times.stop_id " +
                        "join trips on trips.trip_id = stop_times.trip_id " +
                        "join routes on trips.route_id = routes.route_id " +
                        "join directed_routes on trips.route_id = directed_routes.route_id " +
                        "where stops.stop_id = ? and directed_routes.direction_id = trips.direction_id " +
                        "order by CAST(routes.route_short_name AS INTEGER)", new String[]{stopId});
    }

    public Cursor getRoutesBetweenStops(@NotNull String fromStopId, @NotNull String toStopId) {
        Log.d(TAG, "Fetching routes from " + fromStopId + " to " + toStopId);
        SQLiteDatabase database = mHelper.getReadableDatabase();
        String cols = StringUtils.join(ROUTE_COLUMNS, ", ");
        String[] args = new String[]{fromStopId, toStopId};
        return database.rawQuery(
                "select distinct " + cols + " " +
                        "from stops as dest_stop " +
                        "join stop_times as dest_stop_time on dest_stop_time.stop_id = dest_stop._id " +
                        "join trips on dest_stop_time.trip_id = trips.trip_id " +
                        "join stop_times as start_stop_time on start_stop_time.trip_id = trips.trip_id " +
                        "join stops as start_stop on start_stop_time.stop_id = start_stop._id " +
                        "join routes on trips.route_id = routes.route_id " +
                        "join directed_routes on trips.route_id = directed_routes.route_id " +
                        "where start_stop.stop_id = ? " +
                        "and dest_stop.stop_id = ? " +
                        "and start_stop_time.stop_sequence < dest_stop_time.stop_sequence " +
                        "and directed_routes.direction_id = trips.direction_id " +
                        "order by CAST(routes.route_short_name AS INTEGER)",
                args
        );
    }

    /*
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
            int headsign_column = c.getColumnIndex("route_modal_headsign");

            while (true) {
                //String id = c.getString(id_column);
                String name = c.getString(name_column);
                int direction = c.getInt(direction_column);
                String modalHeadSign = c.getString(headsign_column);
                result.add(new Route(name, direction, modalHeadSign));

                if (!c.moveToNext()) {
                    break;
                }
            }
        }
        c.close();
        return result;
    }

    public Cursor getAllStopsMatchingReachableFrom(@Nullable String constraint, @Nullable String fromStopId, String orderBy, Location location) {
        String cols = StringUtils.join(STOP_COLUMNS, ", ");
        String whereClause = "";
        ArrayList<String> args = new ArrayList<>();
        if (constraint != null) {
            whereClause += " and (stops.stop_name like ? or stops.stop_code like ?)";
            String namePattern = "%"+constraint+"%";
            String codePattern = constraint+"%";
            args.add(namePattern);
            args.add(codePattern);
        }

        if (orderBy.equals("proximity")) {
            // We need to sort by distance.  This means we need our current location
            if (location == null) {
                orderBy = "stops.stop_name";
            } else {
                orderBy = "(stops.stop_lat-?)*(stops.stop_lat-?)+(stops.stop_lon-?)*(stops.stop_lon-?)";
                String lat = ""+location.getLatitude();
                String lon = ""+location.getLongitude();
                args.add(lat);
                args.add(lat);
                args.add(lon);
                args.add(lon);
            }
        } else {
            orderBy = "stops." + orderBy;
        }

        SQLiteDatabase database = mHelper.getReadableDatabase();
        whereClause += " order by " + orderBy;
        if (fromStopId == null) {
            return database.rawQuery("select " + cols + " from stops where 1=1 " + whereClause, args.toArray(new String[args.size()]));
        } else {
            args.add(0, fromStopId);
            // This big messy query is about finding those stops reachable on any route from
            // fromStopId
            return database.rawQuery(
                    "select distinct " + cols + " " +
                    "from stops " +
                    "join stop_times as dest_stop_time on dest_stop_time.stop_id = stops._id " +
                    "join trips on dest_stop_time.trip_id = trips.trip_id " +
                    "join stop_times as start_stop_time on start_stop_time.trip_id = trips.trip_id " +
                    "join stops as start_stop on start_stop_time.stop_id = start_stop._id " +
                    "where start_stop.stop_id = ? " +
                    "and start_stop_time.stop_sequence < dest_stop_time.stop_sequence " +
                    whereClause,
                    args.toArray(new String[args.size()])
            );
        }
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

    public List<Stop> stopCursorToList(Cursor c) {
        ArrayList<Stop> result = new ArrayList<>();
        if (c.moveToFirst()) {
            int stopIdColumn = c.getColumnIndex("stop_id");
            int stopCodeColumn = c.getColumnIndex("stop_code");
            int stopNameColumn = c.getColumnIndex("stop_name");

            while (true) {
                String stopId = c.getString(stopIdColumn);
                String stopCode = c.getString(stopCodeColumn);
                String stopName = c.getString(stopNameColumn);
                result.add(new Stop(stopId, stopCode, stopName));

                if (!c.moveToNext()) {
                    break;
                }
            }
        }
        c.close();
        return result;
    }

    public DateTime getNow() {
        // Get the current instant in the Ottawa time zone
        DateTime now = new DateTime();
        return now.withZone(mOttawaTimeZone);
    }

    public Cursor getForthcomingTrips(String stopId, String routeName, int direction, String destinationStopId) {
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

        String[] args = new String[]{stopId, routeName, "" + direction, today, "" + minTime};
        String extraJoin = "";
        String extraWhere = "";

        if (destinationStopId != null) {
            //Log.d(TAG, "destinationStopId = "+destinationStopId);
            extraJoin = "join stop_times as dest_stop_time on dest_stop_time.trip_id = trips.trip_id " +
                        "join stops as dest_stop on dest_stop._id = dest_stop_time.stop_id ";
            extraWhere = "and dest_stop.stop_id = ? ";
            args = ArrayUtils.add(args, destinationStopId);
        }

        // Now we can finally make a query
        SQLiteDatabase database = mHelper.getReadableDatabase();
        String query =
                "select stops.stop_id, stops.stop_code, stops.stop_name, " +
                "route_short_name, trips.direction_id, " +
                "trips.trip_id, trip_headsign, date, stop_times.arrival_time, " +
                "stop_times_start.arrival_time as start_arrival_time, " +
                "last_stop.stop_id as last_stop_id, " +
                "last_stop.stop_code as last_stop_code, " +
                "last_stop.stop_name as last_stop_name, " +
                "route_modal_headsign from stop_times " +
                "join trips on stop_times.trip_id = trips.trip_id " +
                "join days on days.service_id = trips.service_id " +
                "join routes on trips.route_id = routes.route_id " +
                "join directed_routes on trips.route_id = directed_routes.route_id " +
                "join stops on stop_times.stop_id = stops._id " +
                "join stop_times as stop_times_start on stop_times_start.trip_id = trips.trip_id " +
                "join stop_times as last_stop_time on last_stop_time.trip_id = trips.trip_id " +
                "join stops as last_stop on last_stop._id = last_stop_time.stop_id " +
                extraJoin +
                "where stops.stop_id = ? " +
                "and routes.route_short_name = ? " +
                "and trips.direction_id = ? " +
                "and directed_routes.direction_id = trips.direction_id " +
                "and days.date = ? " +
                "and stop_times.arrival_time >= ? " +
                "and stop_times_start.stop_sequence = 1 " +
                "and last_stop_time.stop_sequence = trips.last_stop_sequence " +
                extraWhere +
                "order by stop_times.arrival_time " +
                "limit 10";
        //Log.d(TAG, "Query = " + query + ", stops.stop_id = " + stopId);
        return database.rawQuery(query, args);

        // FIXME: also fetch trips with times which derive from the previous day (i.e. which started yesterday)
        // and the next day
    }

    public List<ForthcomingTrip> stopTimeCursorToList(Cursor c) {
        ArrayList<ForthcomingTrip> result = new ArrayList<>();
        if (c.moveToFirst()) {
            int stop_id_column = c.getColumnIndex("stop_id");
            int stop_code_column = c.getColumnIndex("stop_code");
            int stop_name_column = c.getColumnIndex("stop_name");
            int last_stop_id_column = c.getColumnIndex("last_stop_id");
            int last_stop_code_column = c.getColumnIndex("last_stop_code");
            int last_stop_name_column = c.getColumnIndex("last_stop_name");
            int route_name_column = c.getColumnIndex("route_short_name");
            int route_modal_headsign_column = c.getColumnIndex("route_modal_headsign");
            int direction_column = c.getColumnIndex("direction_id");
            int head_sign_column = c.getColumnIndex("trip_headsign");
            int trip_id_column = c.getColumnIndex("trip_id");
            int date_column = c.getColumnIndex("date");
            int arrival_time_column = c.getColumnIndex("arrival_time");
            int start_time_column = c.getColumnIndex("start_arrival_time");

            while (true) {
                String stopId = c.getString(stop_id_column);
                String stopCode = c.getString(stop_code_column);
                String stopName = c.getString(stop_name_column);
                String lastStopId = c.getString(last_stop_id_column);
                String lastStopCode = c.getString(last_stop_code_column);
                String lastStopName = c.getString(last_stop_name_column);
                String routeName = c.getString(route_name_column);
                String modalHeadsign = c.getString(route_modal_headsign_column);
                int direction = c.getInt(direction_column);
                String headsign = c.getString(head_sign_column);
                int trip_id = c.getInt(trip_id_column);
                String date = c.getString(date_column);
                int arrivalTime = c.getInt(arrival_time_column);
                int startTime = c.getInt(start_time_column);
                DateTime midnight = mIsoDateFormatter.parseDateTime(date).withZoneRetainFields(mOttawaTimeZone);
                //Log.d(TAG, "arrivalTime="+arrivalTime+", startTime="+startTime);
                Stop stop = new Stop(stopId, stopCode, stopName);
                Stop lastStop = new Stop(lastStopId, lastStopCode, lastStopName);
                Route route = new Route(routeName, direction, modalHeadsign);
                result.add(new ForthcomingTrip(stop, route, headsign, lastStop, trip_id, midnight, arrivalTime, startTime));

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
