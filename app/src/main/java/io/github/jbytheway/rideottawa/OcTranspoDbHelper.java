package io.github.jbytheway.rideottawa;

import android.content.Context;

import io.github.jbytheway.rideottawa.utils.DownloadableDatabase;

class OcTranspoDbHelper extends DownloadableDatabase {
    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "octranspo_data.sqlite";
    private static final String DATABASE_URL = "https://github.com/jbytheway/octranspo-db/raw/master/octranspo_data.sqlite.gz";

    public OcTranspoDbHelper(Context context) {
        super(context, DATABASE_NAME, DATABASE_URL, null, DATABASE_VERSION);
    }
}
