package io.github.jbytheway.octranspoalarm;

import android.content.Context;
import com.readystatesoftware.sqliteasset.SQLiteAssetHelper;

import io.github.jbytheway.octranspoalarm.utils.DownloadableDatabase;

public class OcTranspoDbHelper extends DownloadableDatabase {
    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "octranspo_data.sqlite";
    public static final String DATABASE_URL = "https://github.com/jbytheway/octranspo-db/raw/master/octranspo_data.sqlite.gz";

    public OcTranspoDbHelper(Context context) {
        super(context, DATABASE_NAME, DATABASE_URL, null, DATABASE_VERSION);
    }
}
