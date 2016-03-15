package io.github.jbytheway.rideottawa;

import com.orm.SugarRecord;
import com.orm.dsl.Ignore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Favourite extends SugarRecord {
    @SuppressWarnings("unused")
    public Favourite() {
        // Required for Sugar
        mPendingStops = new ArrayList<>();
    }

    public String Name;

    public void saveRecursively() {
        save();
        for (FavouriteStop stop : mPendingStops) {
            stop.saveRecursively();
        }
        mPendingStops.clear();
        for (FavouriteStop stop : getStops()) {
            stop.saveRecursively();
        }
    }

    public void deleteRecursively() {
        for (FavouriteStop stop : getStops()) {
            stop.deleteRecursively();
        }
        delete();
    }

    public List<FavouriteStop> getStops() {
        ArrayList<FavouriteStop> all = new ArrayList<>();
        Long id = getId();
        if (id != null) {
            all.addAll(FavouriteStop.find(FavouriteStop.class, "favourite = ?", id.toString()));
        }
        all.addAll(mPendingStops);
        return all;
    }

    public FavouriteStop getStop(String stopId) {
        // First look in DB
        Long id = getId();
        if (id != null) {
            List<FavouriteStop> results = FavouriteStop.find(FavouriteStop.class, "favourite = ? and stop_id = ?", id.toString(), stopId);
            if (results.size() > 1) {
                throw new AssertionError("Unexpected number of matching stops " + results.size());
            } else if (results.size() == 1) {
                FavouriteStop result = results.get(0);
                if (result != null) {
                    return result;
                } else {
                    throw new AssertionError("Resulting FavouriteStop unexpectedly null");
                }
            }
            // else zero results, in which case fall through to checking pending
        }

        // DB failed, check pending
        for (FavouriteStop stop : mPendingStops) {
            if (stop.StopId.equals(stopId)) {
                return stop;
            }
        }

        String pendingList = "";
        for (FavouriteStop stop : mPendingStops) {
            pendingList += " '" + stop.StopId + "'";
        }

        throw new AssertionError("Couldn't find requested stop '"+stopId+"'; pending stops were"+pendingList);
    }

    public void addStop(String stopId) {
        FavouriteStop stop = new FavouriteStop();
        stop.StopId = stopId;
        stop.Favourite = this;
        mPendingStops.add(stop);
    }

    public void removeStop(FavouriteStop stop) {
        if (stop.getId() == null) {
            // It should be in the pending list
            mPendingStops.remove(stop);
        } else {
            stop.deleteRecursively();
        }
    }

    public ArrayList<ForthcomingTrip> updateForthcomingTrips(OcTranspoDataAccess ocTranspo, ArrayList<ForthcomingTrip> trips) {
        if (!mPendingStops.isEmpty()) {
            throw new AssertionError("Should only be called on saved Favourites");
        }

        // First split the input trips by stop
        HashMap<String, ArrayList<ForthcomingTrip>> tripsSplit = new HashMap<>();

        for (ForthcomingTrip trip : trips) {
            String key = trip.getStop().getId();
            if (tripsSplit.containsKey(key)) {
                tripsSplit.get(key).add(trip);
            } else {
                ArrayList<ForthcomingTrip> newList = new ArrayList<>();
                newList.add(trip);
                tripsSplit.put(key, newList);
            }
        }

        ArrayList<ForthcomingTrip> result = new ArrayList<>();
        for (FavouriteStop stop : getStops()) {
            String key = stop.StopId;
            ArrayList<ForthcomingTrip> tripsForThisStop;
            if (tripsSplit.containsKey(key)) {
                tripsForThisStop = tripsSplit.get(key);
            } else {
                tripsForThisStop = new ArrayList<>();
            }
            result.addAll(stop.updateForthcomingTrips(tripsForThisStop, ocTranspo));
        }

        return result;
    }

    @Ignore
    private final ArrayList<FavouriteStop> mPendingStops;
}
