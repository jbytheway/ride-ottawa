package io.github.jbytheway.rideottawa.ui;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;

import io.github.jbytheway.rideottawa.RideOttawaApplication;
import io.github.jbytheway.rideottawa.OcTranspoDataAccess;
import io.github.jbytheway.rideottawa.OcTranspoDbHelper;
import io.github.jbytheway.rideottawa.R;
import io.github.jbytheway.rideottawa.utils.DownloadableDatabase;

public class ListFavouritesActivity extends AppCompatActivity {
    private static final String TAG = "ListFavouritesActivity";
    private static final int REQUEST_NEW_FAVOURITE = 0;

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

        boolean wifiOnly = true;

        OcTranspoDataAccess ocTranspo = ((RideOttawaApplication) getApplication()).getOcTranspo();

        // For testing purposes this code will the database on startup, to help test the behaviour of new installs
        //ocTranspo.deleteDatabase();

        final ProgressDialog dialog = new ProgressDialog(this);
        dialog.setMessage(getString(R.string.checking_for_updates));
        dialog.show();
        ocTranspo.checkForUpdates(wifiOnly, dialog, new DownloadableDatabase.UpdateListener() {
            @Override
            public void onSuccess() {
                dialog.dismiss();
            }

            @Override
            public void onFail(Exception e, String message, boolean fatal) {
                if (fatal) {
                    fail(e, message);
                } else {
                    String toastText = getString(R.string.continuing_with_old_db, message);
                    Toast.makeText(ListFavouritesActivity.this, toastText, Toast.LENGTH_LONG).show();
                    dialog.dismiss();
                }
            }
        });
    }

    private class FatalErrorDialog extends DialogFragment {
        FatalErrorDialog(Exception e, String message) {
            mMessage = message;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstance) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder
                    .setMessage(mMessage)
                    .setNeutralButton(R.string.close_app, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            ListFavouritesActivity.this.finish();
                        }
                    });
            return builder.create();
        }

        private String mMessage;
    }

    private void fail(Exception e, String message) {
        String dialogMessage = getString(R.string.no_database_fatal_error, message);
        new FatalErrorDialog(e, dialogMessage).show(getFragmentManager(), "FatalErrorDialog");
    }
}
