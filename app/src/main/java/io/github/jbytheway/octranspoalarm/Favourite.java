package io.github.jbytheway.octranspoalarm;

import com.orm.SugarRecord;

public class Favourite extends SugarRecord {
    public Favourite() {
    }

    public Favourite(String name) {
        mName = name;
    }

    String mName;
}
