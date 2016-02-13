package io.github.jbytheway.octranspoalarm.ui;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import io.github.jbytheway.octranspoalarm.R;

public class ViewFavouriteActivity extends AppCompatActivity {
    public static final String FAVOURITE_ID = "favourite_id";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_favourite);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        //noinspection ConstantConditions
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        ViewFavouriteActivityFragment fragment =
                (ViewFavouriteActivityFragment) getFragmentManager().findFragmentById(R.id.fragment);
        fragment.initialize(getIntent());
    }
}
