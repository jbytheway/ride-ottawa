package io.github.jbytheway.octranspoalarm;

import android.os.Bundle;
import android.app.Activity;

public class EditFavouriteActivity extends Activity {
    public static final String NEW_FAVOURITE = "new_favourite";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_favourite);
        getActionBar().setDisplayHomeAsUpEnabled(true);
    }

}
