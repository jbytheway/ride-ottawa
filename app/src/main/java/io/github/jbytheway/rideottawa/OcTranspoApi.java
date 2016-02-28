package io.github.jbytheway.rideottawa;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.Log;

import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;
import com.koushikdutta.ion.Response;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collection;
import java.util.HashMap;
import java.util.Objects;
import java.util.concurrent.CancellationException;

public class OcTranspoApi {
    private static final String TAG = "OcTranspoApi";
    private static final String URL_ROOT = "https://api.octranspo1.com/v1.2/";
    private static final String NEXT_TRIPS_URL = URL_ROOT + "GetNextTripsForStop";
    private static final String ROUTE_SUMMARY_URL = URL_ROOT + "GetRouteSummaryForStop";

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
        mProcessingTimeFormat = DateTimeFormat.forPattern("yyyyMMddHHmmss");
    }

    public void queryTimes(final Context context, final TimeQuery query, final Collection<ForthcomingTrip> trips, final Listener listener) {
        // curl -d "appID=${appId}&apiKey=${apiKey}&stopNo=${stopCode}&routeNo=${routeName}&format=json" https://api.octranspo1.com/v1.2/GetNextTripsForStop
        final String stopCode = query.StopCode;
        final String routeName = query.Route.getName();

        Ion
            .with(context)
            .load(NEXT_TRIPS_URL)
            .setBodyParameter("appID", mAppId)
            .setBodyParameter("apiKey", mApiKey)
            .setBodyParameter("stopNo", stopCode)
            .setBodyParameter("routeNo", routeName)
            .setBodyParameter("format", "json")
            .asString()
            .withResponse()
            .setCallback(new FutureCallback<Response<String>>() {
                @Override
                public void onCompleted(Exception httpException, Response<String> result) {
                    Integer code = null;
                    if (result != null) {
                        code = result.getHeaders().code();
                    }
                    Log.d(TAG, "Fetched API result; e=" + httpException + "; code=" + code);
                    if (httpException != null || code == null || code != 200) {
                        if (httpException != null && httpException.getClass() == CancellationException.class) {
                            // We are fine; no update was necessary
                            listener.onTripData();
                        } else {
                            listener.onApiFail(httpException);
                        }
                        return;
                    }

                    // Even when we get a response, that might be an error
                    String response = result.getResult();
                    if (!response.startsWith("{")) {
                        Log.e(TAG, "Error returned: " + response);
                        listener.onApiFail(null);
                        return;
                    }

                    try {
                        ProcessJsonResponse(context, result.getResult(), query, trips);
                    } catch (JSONException jsonException) {
                        Log.e(TAG, "JSON error with API result", jsonException);
                        listener.onApiFail(jsonException);
                        return;
                    }
                    listener.onTripData();
                }
            });
    }

    private void ProcessJsonResponse(Context context, String jsonString, TimeQuery query, Collection<ForthcomingTrip> trips) throws JSONException {
        JSONObject json = new JSONObject(jsonString);
        JSONArray routeDirections = GetArrayOrObject(json.getJSONObject("GetNextTripsForStopResult").getJSONObject("Route"), "RouteDirection");
        final String stopCode = query.StopCode;
        final String routeName = query.Route.getName();
        Log.d(TAG, "Response for stop "+stopCode+", route "+routeName+" has "+routeDirections.length()+" entries");

        String apiDirection = null;

        if (routeDirections.length() > 1) {
            // This is an awkward case; we have to match up the 0/1 direction info
            // from the database with the N/E/S/W direction info from the API
            // There is another API call which will tell us, but we don't want to
            // wait for that, so we cache the results, and if the cache lacks
            // the value right now, don't worry about it (but fire off a job
            // to fill the cache)
            DirectionKey key = new DirectionKey(stopCode, query.Route);

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
            DateTime processingTime = mProcessingTimeFormat.parseDateTime(routeDirection.getString("RequestProcessingTime"));

            JSONArray liveTrips = GetArrayOrObject(routeDirection.getJSONObject("Trips"), "Trip");
            for (int j = 0; j < liveTrips.length(); ++j) {
                JSONObject liveTrip = liveTrips.getJSONObject(j);
                String tripStartTime = liveTrip.getString("TripStartTime");

                for (ForthcomingTrip trip : trips) {
                    if (trip.getStartTimeString().equals(tripStartTime)) {
                        Log.d(TAG, "Found matching trip!");
                        int minutesAway = Integer.parseInt(liveTrip.getString("AdjustedScheduleTime"));
                        if (minutesAway != -1) {
                            trip.provideLiveData(processingTime, minutesAway);
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

                    try {
                        JSONObject json = new JSONObject(result.getResult());
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
                        Log.e(TAG, "CacheDirection JSON error", jsonError);
                    }
                }
            });
    }

    private JSONArray GetArrayOrObject(JSONObject o, String member) throws JSONException {
        // There are cases where the API can return either an array or single object
        // Convert the latter to an array of length 1 for consistency.
        Object m = o.get(member);
        if (m.getClass() == JSONArray.class) {
            return (JSONArray) m;
        } else {
            return new JSONArray(new Object[]{m});
        }
    }

    private class DirectionKey {
        DirectionKey(String stopCode, Route route) {
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
    private final DateTimeFormatter mProcessingTimeFormat;
    // Mapping from database directions to cardinal directions
    private final HashMap<DirectionKey, String> mDirectionCache;
}