package io.github.jbytheway.rideottawa.ui;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

import io.github.jbytheway.rideottawa.R;

public class EditFavouriteActivity extends AppCompatActivity {
    @SuppressWarnings("unused")
    private static final String TAG = "EditFavouriteActivity";

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

        getFragment().initialize(getIntent());
    }

    private EditFavouriteActivityFragment getFragment() {
        return (EditFavouriteActivityFragment) getFragmentManager().findFragmentById(R.id.fragment);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                getFragment().onBackPressed();
                return super.onOptionsItemSelected(item);
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onBackPressed() {
        getFragment().onBackPressed();
        super.onBackPressed();
    }
}
