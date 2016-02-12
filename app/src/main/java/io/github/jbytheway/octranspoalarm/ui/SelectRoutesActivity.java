package io.github.jbytheway.octranspoalarm.ui;

import android.os.Bundle;
import android.app.Activity;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import io.github.jbytheway.octranspoalarm.R;

public class SelectRoutesActivity extends AppCompatActivity {
    public static final String STOP_ID = "stop_id";
    public static final String SELECTED_ROUTES = "selected_stops";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_routes);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

}
