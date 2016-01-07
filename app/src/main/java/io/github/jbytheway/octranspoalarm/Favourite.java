package io.github.jbytheway.octranspoalarm;

import com.orm.SugarRecord;
import com.orm.dsl.Ignore;
import com.orm.dsl.NotNull;

import java.util.ArrayList;
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
            if (results.size() != 1) {
                throw new AssertionError("Unexpected number of matching stops " + results.size());
            }
            FavouriteStop result = results.get(0);
            if (result != null) {
                return result;
            }
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

    @Ignore
    private ArrayList<FavouriteStop> mPendingStops;
}
