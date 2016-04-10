package io.github.jbytheway.rideottawa;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import org.apache.commons.lang3.text.WordUtils;

import io.github.jbytheway.rideottawa.ui.SettingsActivityFragment;

public class Stop {
    Stop(String id, String code, String name) {
        mId = id;
        mCode = code;
        mName = name;
    }

    public String getId() { return mId; }
    public String getCode() { return mCode; }
    public String getName(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        boolean titleCase = preferences.getBoolean(SettingsActivityFragment.PREF_TITLE_CASE_STOPS, false);
        if (titleCase) {
            return titleCaseOf(mName);
        } else {
            return mName;
        }
    }

    public static String titleCaseOf(String stopName) {
        // FIXME: This incorrectly capitalizes the 'd' in Place d'Orleans
        return WordUtils.capitalizeFully(stopName, ' ', '1', '2', '3', '4', '.', '/', '\'', '-');
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Stop) {
            Stop other = (Stop) o;
            return mId.equals(other.mId);
        }

        return false;
    }

    @Override
    public int hashCode() {
        return mId.hashCode();
    }

    private final String mId;
    private final String mCode;
    private final String mName;
}
