package io.github.jbytheway.octranspoalarm;

import com.orm.SugarRecord;

public class Favourite extends SugarRecord {
    @SuppressWarnings("unused")
    public Favourite() {
        // Required for Sugar
    }

    public String Name;

    public void deleteRecursively() {
        delete();
    }
}
