package io.github.jbytheway.rideottawa.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import com.google.common.collect.Iterators;

import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.github.jbytheway.rideottawa.BuildConfig;
import io.github.jbytheway.rideottawa.Favourite;
import io.github.jbytheway.rideottawa.OcTranspoDataAccess;
import io.github.jbytheway.rideottawa.PendingAlarmData;
import io.github.jbytheway.rideottawa.RideOttawaApplication;
import io.github.jbytheway.rideottawa.utils.IndirectArrayAdapter;
import io.github.jbytheway.rideottawa.R;

public class ListFavouritesActivityFragment extends Fragment {
    @SuppressWarnings("unused")
    private static final String TAG = "ListFavouritesFragment";

    public ListFavouritesActivityFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Don't destroy Fragment on reconfiguration
        setRetainInstance(true);

        // This Fragment adds options to the ActionBar
        setHasOptionsMenu(true);

        mOcTranspo = ((RideOttawaApplication) getActivity().getApplication()).getOcTranspo();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_list_favourites, container, false);

        mNoFavouritesHint = (TextView) view.findViewById(R.id.no_favourites_hint);
        mEditFavouriteHint = (TextView) view.findViewById(R.id.list_favourites_hint);

        mFavouriteList = (ListView) view.findViewById(R.id.favourite_list_view);
        final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        final Context context = getActivity();

        mAdapter = new IndirectArrayAdapter<>(
            context,
            R.layout.favourite_list_item,
            new IndirectArrayAdapter.ListGenerator<Favourite>() {
                @Override
                public List<Favourite> makeList() {
                    List<Favourite> favourites = new ArrayList<>();
                    Iterators.addAll(favourites, Favourite.findAll(Favourite.class));
                    Collections.sort(favourites, new Favourite.CompareNames());
                    return favourites;
                }
            },
            new IndirectArrayAdapter.ViewGenerator<Favourite>() {
                @Override
                public void applyView(View v, Favourite f) {
                    TextView name = (TextView) v.findViewById(R.id.name);
                    name.setText(f.Name);
                    boolean smallFavourites = sharedPreferences.getBoolean("pref_small_favourites", false);
                    if (smallFavourites) {
                        //noinspection deprecation
                        name.setTextAppearance(context, android.R.style.TextAppearance_DeviceDefault_Medium);
                    } else {
                        //noinspection deprecation
                        name.setTextAppearance(context, android.R.style.TextAppearance_DeviceDefault_Large);
                    }
                }
            }
        );
        mFavouriteList.setAdapter(mAdapter);

        mFavouriteList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (!mOcTranspo.isDatabaseAvailable()) {
                    mActivity.notifyNoDatabase();
                    return;
                }
                Favourite item = mAdapter.getItem(position);
                Intent intent = new Intent(getActivity(), ViewFavouriteActivity.class);
                intent.putExtra(ViewFavouriteActivity.FAVOURITE_ID, item.getId());

                startActivity(intent);
            }
        });

        mFavouriteList.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                if (!mOcTranspo.isDatabaseAvailable()) {
                    mActivity.notifyNoDatabase();
                    return true;
                }
                Favourite item = mAdapter.getItem(position);
                Intent intent = new Intent(getActivity(), EditFavouriteActivity.class);
                intent.putExtra(EditFavouriteActivity.NEW_FAVOURITE, false);
                intent.putExtra(EditFavouriteActivity.FAVOURITE_ID, item.getId());

                startActivity(intent);

                return true;
            }
        });

        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mActivity = (ListFavouritesActivity) getActivity();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_list_favourites, menu);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        MenuItem alarms = menu.findItem(R.id.menu_alarms);
        long numAlarms = PendingAlarmData.count(PendingAlarmData.class);

        alarms.setVisible(numAlarms > 0);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_alarms: {
                Intent intent = new Intent(getActivity(), ListAlarmsActivity.class);
                startActivity(intent);
                return true;
            }
            case R.id.menu_settings: {
                Intent intent = new Intent(getActivity(), SettingsActivity.class);
                startActivity(intent);
                return true;
            }
            case R.id.menu_database: {
                DatabaseCheckDialog databaseCheckDialog = new DatabaseCheckDialog();
                String databaseEndDate;
                String lastUpdateCheck;
                if (mOcTranspo.isDatabaseAvailable()) {
                    DateTimeFormatter formatter = DateTimeFormat.fullDate();
                    databaseEndDate = formatter.print(mOcTranspo.getDatabaseEndDate());
                    lastUpdateCheck = formatter.print(mOcTranspo.getLastUpdateCheck());
                } else {
                    databaseEndDate = null;
                    lastUpdateCheck = null;
                }
                Bundle args = new Bundle();
                args.putString(DatabaseCheckDialog.DATABASE_END_DATE, databaseEndDate);
                args.putString(DatabaseCheckDialog.LAST_UPDATE_CHECK, lastUpdateCheck);
                databaseCheckDialog.setArguments(args);
                databaseCheckDialog.show(getActivity().getFragmentManager(), "DatabaseCheckDialog");
                return true;
            }
            case R.id.menu_about: {
                AboutDialog aboutDialog = new AboutDialog();
                aboutDialog.show(getActivity().getFragmentManager(), "AboutDialog");
                return true;
            }
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public static class DatabaseCheckDialog extends DialogFragment {
        public static final String LAST_UPDATE_CHECK = "LastUpdateCheck";
        public static final String DATABASE_END_DATE = "DatabaseEndDate";

        interface DatabaseCheckListener {
            void doDatabaseUpdate();
        }

        public DatabaseCheckDialog() {
            // Default constructor required for DialogFragments
            // Real construction happens in onAttach
        }

        @Override
        public void onAttach(Activity activity) {
            super.onAttach(activity);
            Bundle args = getArguments();

            mLastUpdateCheck = args.getString(LAST_UPDATE_CHECK);
            mDatabaseEndDate = args.getString(DATABASE_END_DATE);
            mListener = ((ListFavouritesActivity) activity).getDatabaseCheckListener();
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstance) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            String message;
            if (mDatabaseEndDate == null) {
                message = getString(R.string.no_database_yet);
            } else {
                message = getString(R.string.database_check_message, mLastUpdateCheck, mDatabaseEndDate);
            }
            builder
                    .setMessage(message)
                    .setNegativeButton(R.string.close_dialog, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dismiss();
                        }
                    })
                    .setPositiveButton(R.string.force_database_update, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mListener.doDatabaseUpdate();
                    }
                });
            return builder.create();
        }

        private String mLastUpdateCheck;
        private String mDatabaseEndDate;
        private DatabaseCheckListener mListener;
    }

    public static class AboutDialog extends DialogFragment {
        public AboutDialog() {
            // Default constructor required for DialogFragments
            // Real construction happens in onAttach
        }

        @Override
        public void onAttach(Activity activity) {
            super.onAttach(activity);
            //Bundle args = getArguments();
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstance) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            String version = BuildConfig.VERSION_NAME;
            final String sourceUrl = getString(R.string.source_url);
            String message = getString(R.string.about_dialog_message, version, sourceUrl);
            builder
                    .setMessage(message)
                    .setNegativeButton(R.string.close_dialog, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // Nothing
                        }
                    })
                    .setPositiveButton(R.string.go_to_source, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(sourceUrl));
                            startActivity(intent);
                        }
                    });
            return builder.create();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        mAdapter.notifyDataSetChanged();

        long numFavourites = Favourite.count(Favourite.class);

        if (numFavourites == 0) {
            mNoFavouritesHint.setVisibility(View.VISIBLE);
            mEditFavouriteHint.setVisibility(View.GONE);
        } else if (numFavourites < 4) {
            mNoFavouritesHint.setVisibility(View.GONE);
            mEditFavouriteHint.setVisibility(View.VISIBLE);
        } else {
            mNoFavouritesHint.setVisibility(View.GONE);
            mEditFavouriteHint.setVisibility(View.GONE);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mFavouriteList = null;
        super.onDestroyView();
    }

    private OcTranspoDataAccess mOcTranspo;
    private TextView mNoFavouritesHint;
    private TextView mEditFavouriteHint;
    private ListFavouritesActivity mActivity;
    private ListView mFavouriteList;
    private IndirectArrayAdapter<Favourite> mAdapter;
}
