package io.github.jbytheway.rideottawa.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Toast;

import io.github.jbytheway.rideottawa.RideOttawaApplication;
import io.github.jbytheway.rideottawa.OcTranspoDataAccess;
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

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        boolean wifiOnly = sharedPreferences.getBoolean(SettingsActivityFragment.PREF_WIFI_ONLY, true);

        mOcTranspo = ((RideOttawaApplication) getApplication()).getOcTranspo();

        // For testing purposes this code will the database on startup, to help test the behaviour of new installs
        //mOcTranspo.deleteDatabase();

        tryDatabaseUpdate(wifiOnly);
    }

    private void tryDatabaseUpdate(boolean wifiOnly) {
        final ProgressDialog dialog = new ProgressDialog(this);
        dialog.setMessage(getString(R.string.checking_for_updates));
        dialog.show();
        mOcTranspo.checkForUpdates(wifiOnly, dialog, new DownloadableDatabase.UpdateListener() {
            @Override
            public void onSuccess() {
                dialog.dismiss();
            }

            @Override
            public void onFail(Exception e, String message, boolean wifiRelated, boolean fatal) {
                if (fatal) {
                    fail(e, message, wifiRelated);
                    dialog.dismiss();
                } else {
                    String toastText = getString(R.string.continuing_with_old_db, message);
                    Toast.makeText(ListFavouritesActivity.this, toastText, Toast.LENGTH_LONG).show();
                    dialog.dismiss();
                }
            }
        });
    }

    public static class FatalErrorDialog extends DialogFragment {
        public static final String MESSAGE = "message";
        public static final String WIFI_RELATED = "wifi";

        interface FatalErrorListener {
            void abort();
            void tryAgainWithoutWifi();
        }

        public FatalErrorDialog() {
            // Default constructor required for DialogFragments
            // Real construction happens in onAttach
        }

        @Override
        public void onAttach(Activity activity) {
            super.onAttach(activity);
            Bundle args = getArguments();

            mMessage = args.getString(MESSAGE);
            mWifiRelated = args.getBoolean(WIFI_RELATED);
            mListener = ((ListFavouritesActivity) activity).getFatalErrorListener();
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstance) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder
                    .setMessage(mMessage)
                    .setNegativeButton(R.string.close_app, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            mListener.abort();
                        }
                    });
            if (mWifiRelated) {
                builder.setNeutralButton(R.string.try_again_without_wifi, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mListener.tryAgainWithoutWifi();
                    }
                });
            }
            return builder.create();
        }

        private String mMessage;
        private boolean mWifiRelated;
        private FatalErrorListener mListener;
    }

    private void fail(Exception e, String message, boolean wifiRelated) {
        String dialogMessage = getString(R.string.no_database_fatal_error, message);
        FatalErrorDialog errorDialog = new FatalErrorDialog();
        Bundle args = new Bundle();
        args.putString(FatalErrorDialog.MESSAGE, dialogMessage);
        args.putBoolean(FatalErrorDialog.WIFI_RELATED, wifiRelated);
        errorDialog.setArguments(args);
        errorDialog.show(getFragmentManager(), "FatalErrorDialog");
    }

    FatalErrorDialog.FatalErrorListener getFatalErrorListener() {
        return new FatalErrorDialog.FatalErrorListener() {
            @Override
            public void abort() {
                ListFavouritesActivity.this.finish();
            }

            @Override
            public void tryAgainWithoutWifi() {
                ListFavouritesActivity.this.tryDatabaseUpdate(false);
            }
        };
    }

    private OcTranspoDataAccess mOcTranspo;
}
