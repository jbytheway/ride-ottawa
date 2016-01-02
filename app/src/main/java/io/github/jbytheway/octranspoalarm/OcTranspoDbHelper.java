package io.github.jbytheway.octranspoalarm;

import android.content.Context;
import com.readystatesoftware.sqliteasset.SQLiteAssetHelper;

public class OcTranspoDbHelper extends SQLiteAssetHelper {
    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "octranspo_data.sqlite";

    public OcTranspoDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }
}
