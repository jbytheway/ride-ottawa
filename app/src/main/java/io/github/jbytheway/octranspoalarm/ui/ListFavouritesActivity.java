package io.github.jbytheway.octranspoalarm.ui;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import java.io.IOException;

import io.github.jbytheway.octranspoalarm.OcTranspoApplication;
import io.github.jbytheway.octranspoalarm.OcTranspoDataAccess;
import io.github.jbytheway.octranspoalarm.R;
import io.github.jbytheway.octranspoalarm.utils.DownloadableDatabase;

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

        // For testing purposes this code will the database on startup, to help test the behaviour of new installs
//        File dir = getFilesDir();
//        File databasesDir = new File(dir.getParentFile(), "databases");
//        File file = new File(databasesDir, OcTranspoDbHelper.DATABASE_NAME);
//        Log.i(TAG, "Deleting database at " + file.getAbsolutePath());
//        file.delete();

        OcTranspoDataAccess ocTranspo = ((OcTranspoApplication) getApplication()).getOcTranspo();
        final ProgressDialog dialog = new ProgressDialog(this);
        dialog.setMessage(getString(R.string.checking_for_updates));
        dialog.show();
        try {
            ocTranspo.checkForUpdates(dialog, new DownloadableDatabase.UpdateListener() {
                @Override
                public void onSuccess() {
                    dialog.dismiss();
                }

                @Override
                public void onFail(Exception e, boolean fatal) {
                    if (fatal) {
                        fail();
                    } else {
                        Toast.makeText(ListFavouritesActivity.this, R.string.download_failed_but_continuing, Toast.LENGTH_LONG).show();
                        dialog.dismiss();
                    }
                }
            });
        } catch (IOException e) {
            Log.e(TAG, "Error checking for database updates", e);
            fail();
        }
    }

    void fail() {
        // TODO: Pop up a message so the user knows why we died
        finish();
    }
}
