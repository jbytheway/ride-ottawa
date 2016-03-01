package io.github.jbytheway.rideottawa.ui;

import android.os.Bundle;
import android.preference.PreferenceFragment;

import io.github.jbytheway.rideottawa.R;

public class SettingsActivityFragment extends PreferenceFragment {
    public static final String PREF_WIFI_ONLY = "pref_download_wifi_only";

    public SettingsActivityFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
    }
}
