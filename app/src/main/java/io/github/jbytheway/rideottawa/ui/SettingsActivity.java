package io.github.jbytheway.rideottawa.ui;


import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import io.github.jbytheway.rideottawa.R;

public class SettingsActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        //noinspection ConstantConditions
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }
}