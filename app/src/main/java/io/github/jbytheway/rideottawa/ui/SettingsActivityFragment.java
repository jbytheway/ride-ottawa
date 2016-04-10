package io.github.jbytheway.rideottawa.ui;

import android.os.Bundle;
import android.preference.PreferenceFragment;

import io.github.jbytheway.rideottawa.R;

public class SettingsActivityFragment extends PreferenceFragment {
    public static final String PREF_WIFI_ONLY = "pref_download_wifi_only";
    public static final String PREF_SORT_STOPS = "pref_sort_stops_by";
    public static final String PREF_WHAT_DESTINATION = "pref_what_destination";
    public static final String PREF_TITLE_CASE_STOPS = "pref_title_case_stops";

    public SettingsActivityFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
    }
}
