package io.github.jbytheway.rideottawa.ui;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import io.github.jbytheway.rideottawa.R;

public class ViewFavouriteActivity extends AppCompatActivity {
    public static final String FAVOURITE_ID = "favourite_id";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.view_favourite_activity);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        //noinspection ConstantConditions
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        getFragment().initialize(getIntent());
    }

    public ViewFavouriteActivityFragment getFragment() {
        return (ViewFavouriteActivityFragment) getFragmentManager().findFragmentById(R.id.fragment);
    }
}
