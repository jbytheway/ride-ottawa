package io.github.jbytheway.rideottawa.ui;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import io.github.jbytheway.rideottawa.R;

public class EditFavouriteActivity extends AppCompatActivity {
    public static final String NEW_FAVOURITE = "new_favourite";
    public static final String FAVOURITE_ID = "favourite_id";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_favourite);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        //noinspection ConstantConditions
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        EditFavouriteActivityFragment fragment =
                (EditFavouriteActivityFragment) getFragmentManager().findFragmentById(R.id.fragment);
        fragment.initialize(getIntent());
    }

}
