package io.github.jbytheway.rideottawa;

import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.common.io.CharStreams;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;
import com.koushikdutta.ion.Response;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.Objects;
import java.util.concurrent.CancellationException;

import io.github.jbytheway.rideottawa.db.Route;

public class OcTranspoApi {
    private static final String TAG = "OcTranspoApi";
    private static final String URL_ROOT = "https://api.octranspo1.com/v1.2/";
    private static final String NEXT_TRIPS_URL = URL_ROOT + "GetNextTripsForStop";
    private static final String ROUTE_SUMMARY_URL = URL_ROOT + "GetRouteSummaryForStop";

    public enum Synchronicity {
        Syncronous,
        Asyncronous,
    }

    public interface Listener {
        void onApiFail(@Nullable Exception e);
        void onTripData();
    }

    public OcTranspoApi(Context context) {
        mDirectionCache = new HashMap<>();
        // If you get a compile error here it is because you must provide the code an API key
        // These can be obtained by signing up at https://octranspo-new.3scale.net/signup
        // Having obtained an app id and API key, create a file app/src/main/res/values/api.xml
        // defining them as string resources with the names used here.
        mAppId = context.getString(R.string.app_id);
        mApiKey = context.getString(R.string.api_key);
        mOttawaTimeZone = DateTimeZone.forID("America/Toronto");
        mStartTimeFormat = DateTimeFormat.forPattern("HH:mm");
        mProcessingDateTimeFormat = DateTimeFormat.forPattern("yyyyMMddHHmmss");
    }

    private static class QueryArgs {
        QueryArgs(final Context context, final TimeQuery query, final Collection<ForthcomingTrip> trips, final Listener listener) {
            Context = context;
            Query = query;
            Trips = trips;
            Listener = listener;
        }

        final Context Context;
        final TimeQuery Query;
        final Collection<ForthcomingTrip> Trips;
        final Listener Listener;
    }

    private static class QueryResult {
        QueryResult(Integer responseCode, String response) {
            ResponseCode = responseCode;
            Response = response;
            Exception = null;
        }

        QueryResult(Exception exception) {
            ResponseCode = null;
            Response = null;
            Exception = exception;
        }

        final Integer ResponseCode;
        final String Response;
        final Exception Exception;
    }

    private QueryResult syncronousQuery(final QueryArgs args) {
        TimeQuery query = args.Query;

        // curl -d "appID=${appId}&apiKey=${apiKey}&stopNo=${stopCode}&routeNo=${routeName}&format=json" https://api.octranspo1.com/v1.2/GetNextTripsForStop
        final String routeName = query.Route.getName();

        Log.d(TAG, "Submitting API query, stopCode="+query.StopCodeToQuery+", routeName="+routeName);

        URL url;
        try {
            url = new URL(NEXT_TRIPS_URL);
        } catch (MalformedURLException e) {
            throw new AssertionError("Malformed URL "+NEXT_TRIPS_URL);
        }
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) url.openConnection();
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");

            Uri.Builder builder = new Uri.Builder()
                    .appendQueryParameter("appID", mAppId)
                    .appendQueryParameter("apiKey", mApiKey)
                    .appendQueryParameter("stopNo", query.StopCodeToQuery)
                    .appendQueryParameter("routeNo", routeName)
                    .appendQueryParameter("format", "json");
            String postContent = builder.build().getEncodedQuery();

            conn.setFixedLengthStreamingMode(postContent.length());

            OutputStream os = conn.getOutputStream();
            OutputStreamWriter writer = new OutputStreamWriter(os, "UTF-8");
            writer.write(postContent);
            writer.close();

            conn.connect();

            int responseCode = conn.getResponseCode();

            InputStream is = conn.getInputStream();
            InputStreamReader isr = new InputStreamReader(is, "UTF-8");
            String result = CharStreams.toString(isr);

            return new QueryResult(responseCode, result);
        } catch (IOException e) {
            return new QueryResult(e);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private class BackgroundQueryTask extends AsyncTask<QueryArgs, Void, QueryResult> {
        @Override
        protected QueryResult doInBackground(QueryArgs... params) {
            QueryArgs args = params[0];
            mArgs = args;
            return syncronousQuery(args);
        }

        @Override
        protected void onPostExecute(QueryResult result) {
            processStringResponse(mArgs.Context, mArgs.Query, mArgs.Trips, result.ResponseCode, result.Response, result.Exception, mArgs.Listener);
        }

        private QueryArgs mArgs;
    }

    /**
     * Query the OCTranspo API for live data for a collection of ForthcomingTrips.
     *
     * @param context Standard Android context object.
     * @param query The Query we are making (A stop code and route number)
     * @param trips The trips about which we want data.  Every trip in this collection *must* match
     *              the query.
     * @param listener The object which will receive callbacks about the results we find.
     */
    public void queryTimes(final Context context, final TimeQuery query, final Collection<ForthcomingTrip> trips, Synchronicity synchronicity, final Listener listener) {
        QueryArgs args = new QueryArgs(context, query, trips, listener);

        // We generally prefer querying asynchronously but in some circumstances
        // we need a synchronous version instead, so we offer these two alternatives
        switch (synchronicity) {
            case Syncronous:
                QueryResult result = syncronousQuery(args);
                processStringResponse(context, query, trips, result.ResponseCode, result.Response, result.Exception, listener);
                break;
            case Asyncronous:
                new BackgroundQueryTask().execute(args);
                break;
            default:
                throw new AssertionError("Unexpected synchronicity");
        }
    }

    private void processStringResponse(Context context, TimeQuery query, Collection<ForthcomingTrip> trips, Integer code, String response, Exception exception, Listener listener) {
        if (exception != null || code == null || code != 200) {
            if (exception != null && exception.getClass() == CancellationException.class) {
                // We are fine; no update was necessary
                listener.onTripData();
            } else {
                listener.onApiFail(exception);
            }
            return;
        }

        // Even when we get a response, that might be an error
        if (!response.startsWith("{")) {
            Log.e(TAG, "Error returned: " + response);
            listener.onApiFail(null);
            return;
        }

        try {
            ProcessJsonResponse(context, response, query, trips);
        } catch (JSONException jsonException) {
            Log.e(TAG, "JSON error with API result", jsonException);
            Log.e(TAG, "JSON was: " + response);
            listener.onApiFail(jsonException);
            return;
        }
        listener.onTripData();
    }

    private void ProcessJsonResponse(Context context, String jsonString, TimeQuery query, Collection<ForthcomingTrip> trips) throws JSONException {
        JSONObject json = new JSONObject(jsonString);
        JSONArray routeDirections = GetArrayOrObject(json.getJSONObject("GetNextTripsForStopResult").getJSONObject("Route"), "RouteDirection");
        final String stopCodeToQuery = query.StopCodeToQuery;
        final String routeName = query.Route.getName();
        Log.d(TAG, "Response for stop "+stopCodeToQuery+", route "+routeName+" has "+routeDirections.length()+" entries");

        // Before we start messing with the actual JSON, we inform each trip that they have received a response
        for (ForthcomingTrip trip : trips) {
            trip.notifyLiveResponseReceived();
        }

        String apiDirection = null;

        if (routeDirections.length() > 1) {
            // This is an awkward case; we have to match up the 0/1 direction info
            // from the database with the N/E/S/W direction info from the API
            // There is another API call which will tell us, but we don't want to
            // wait for that, so we cache the results, and if the cache lacks
            // the value right now, don't worry about it (but fire off a job
            // to fill the cache)
            DirectionKey key = new DirectionKey(stopCodeToQuery, query.Route);

            if (mDirectionCache.containsKey(key)) {
                apiDirection = mDirectionCache.get(key);
            } else {
                CacheDirection(context, key);
            }
        }

        for (int i = 0; i < routeDirections.length(); ++i) {
            JSONObject routeDirection = routeDirections.getJSONObject(i);
            if (apiDirection != null) {
                String direction = routeDirection.getString("Direction");
                if (!direction.equals(apiDirection)) {
                    // Skip over this direction
                    continue;
                }
            }

            // We want to process the provided trips in this direction
            String processingTimeString = routeDirection.getString("RequestProcessingTime");
            DateTime processingTime = mProcessingDateTimeFormat.parseDateTime(processingTimeString);
            processingTime = processingTime.withZoneRetainFields(mOttawaTimeZone);

            JSONArray liveTrips = GetArrayOrObject(routeDirection.getJSONObject("Trips"), "Trip");
            for (int j = 0; j < liveTrips.length(); ++j) {
                JSONObject liveTrip = liveTrips.getJSONObject(j);
                String tripStartTime = liveTrip.getString("TripStartTime");

                for (ForthcomingTrip trip : trips) {
                    DateTime startTime = trip.getStartTime();
                    String startTimeString = mStartTimeFormat.print(startTime);
                    if (startTimeString.equals(tripStartTime)) {
                        //Log.d(TAG, "Found matching trip!");
                        int minutesAway = Integer.parseInt(liveTrip.getString("AdjustedScheduleTime"));
                        double adjustmentAge = Double.parseDouble(liveTrip.getString("AdjustmentAge"));
                        if (adjustmentAge != -1) {
                            trip.provideLiveData(processingTime, minutesAway, adjustmentAge);
                        }
                        break;
                    }
                }
            }
        }
    }

    private void CacheDirection(Context context, final DirectionKey key) {
        // We want to launch a job to fill the requested cache entry
        final String stopCode = key.StopCode;

        Ion
            .with(context)
            .load(ROUTE_SUMMARY_URL)
            .setBodyParameter("appID", mAppId)
            .setBodyParameter("apiKey", mApiKey)
            .setBodyParameter("stopNo", stopCode)
            .setBodyParameter("format", "json")
            .asString()
            .withResponse()
            .setCallback(new FutureCallback<Response<String>>() {
                @Override
                public void onCompleted(Exception e, Response<String> result) {
                    if (result == null || result.getHeaders().code() != 200) {
                        Log.e(TAG, "CacheDirection HTTP error", e);
                        return;
                    }

                    String response = result.getResult();

                    try {
                        JSONObject json = new JSONObject(response);
                        JSONArray routes = json.getJSONObject("GetRouteSummaryForStopResult").getJSONObject("Routes").getJSONArray("Route");

                        String targetRouteName = key.Route.getName();
                        int targetDirectionId = key.Route.getDirection();

                        for (int i = 0; i < routes.length(); ++i) {
                            JSONObject route = routes.getJSONObject(i);

                            String routeName = ""+route.getInt("RouteNo");
                            if (!routeName.equals(targetRouteName)) {
                                continue;
                            }

                            int directionId = route.getInt("DirectionID");
                            if (directionId != targetDirectionId) {
                                continue;
                            }

                            // We have found the one we want
                            String directionString = route.getString("Direction");
                            mDirectionCache.put(key, directionString);
                            Log.d(TAG, "Successfully cached a direction");
                        }
                    } catch (JSONException jsonError) {
                        Log.e(TAG, "CacheDirection JSON error; json was "+response, jsonError);
                    }
                }
            });
    }

    private JSONArray GetArrayOrObject(JSONObject o, String member) throws JSONException {
        // There are cases where the API can return either an array or single object or nothing
        // Convert the latter to arrays of length 1 or 0 for consistency.
        if (!o.has(member)) {
            return new JSONArray(new Object[]{});
        }
        Object m = o.get(member);
        if (m.getClass() == JSONArray.class) {
            return (JSONArray) m;
        } else {
            return new JSONArray(new Object[]{m});
        }
    }

    private class DirectionKey {
        DirectionKey(String stopCode, io.github.jbytheway.rideottawa.db.Route route) {
            StopCode = stopCode;
            Route = route;
        }

        final String StopCode;
        final Route Route;

        @Override
        public int hashCode() {
            return Objects.hash(StopCode, Route);
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof DirectionKey) {
                DirectionKey other = (DirectionKey) o;
                return StopCode.equals(other.StopCode) && Route.equals(other.Route);
            }

            return false;
        }
    }

    private final String mAppId;
    private final String mApiKey;
    private final DateTimeZone mOttawaTimeZone;
    private final DateTimeFormatter mProcessingDateTimeFormat;
    private final DateTimeFormatter mStartTimeFormat;
    // Mapping from database directions to cardinal directions
    private final HashMap<DirectionKey, String> mDirectionCache;
}
