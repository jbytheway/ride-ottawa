package io.github.jbytheway.rideottawa.ui;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import io.github.jbytheway.rideottawa.R;

public class SelectRoutesActivity extends AppCompatActivity {
    public static final String STOP_ID = "stop_id";
    public static final String SELECTED_ROUTES = "selected_stops";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.select_routes_activity);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
    }

}
