package io.github.jbytheway.rideottawa.ui;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import io.github.jbytheway.rideottawa.R;

public class SelectRoutesActivity extends AppCompatActivity {
    public static final String STOP_ID = "stop_id";
    public static final String SELECTED_ROUTES = "selected_stops";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.select_routes_activity);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
    }

}
