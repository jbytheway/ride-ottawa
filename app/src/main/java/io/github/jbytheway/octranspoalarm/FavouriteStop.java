package io.github.jbytheway.octranspoalarm;

import com.orm.SugarRecord;

public class FavouriteStop extends SugarRecord {
    @SuppressWarnings("unused")
    public FavouriteStop() {
        // Required for Sugar
    }

    public void saveRecursively() {
        save();
    }

    public void deleteRecursively() {
        delete();
    }

    public String StopId;
    public Favourite Favourite;
}
