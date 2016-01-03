package io.github.jbytheway.octranspoalarm;

import com.orm.SugarRecord;

public class Favourite extends SugarRecord {
    @SuppressWarnings("unused")
    public Favourite() {
        // Required for Sugar
    }

    public Favourite(String name) {
        mName = name;
    }

    public String getName() { return mName; }

    String mName;
}
