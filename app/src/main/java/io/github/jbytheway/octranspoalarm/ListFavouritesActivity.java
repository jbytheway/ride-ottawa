package io.github.jbytheway.octranspoalarm;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;

import java.io.File;

public class ListFavouritesActivity extends AppCompatActivity {
    private static final String TAG = "ListFavouritesActivity";
    int REQUEST_NEW_FAVOURITE = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_favourites);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(ListFavouritesActivity.this, EditFavouriteActivity.class);
                intent.putExtra(EditFavouriteActivity.NEW_FAVOURITE, true);

                startActivityForResult(intent, REQUEST_NEW_FAVOURITE);
            }
        });

        // For testing purposes we always wipe the database on startup, to ensure that we always have the latest version from the assets folder
        File dir = getFilesDir();
        File databasesDir = new File(dir.getParentFile(), "databases");
        File file = new File(databasesDir, OcTranspoDbHelper.DATABASE_NAME);
        Log.i(TAG, "Deleting database at " + file.getAbsolutePath());
        file.delete();
    }

}
