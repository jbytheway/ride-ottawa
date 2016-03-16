package io.github.jbytheway.rideottawa.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
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

import org.apache.commons.collections4.IteratorUtils;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.util.List;

import io.github.jbytheway.rideottawa.Favourite;
import io.github.jbytheway.rideottawa.OcTranspoDataAccess;
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
        mFavouriteList = (ListView) view.findViewById(R.id.favourite_list_view);

        mAdapter = new IndirectArrayAdapter<>(
            getActivity(),
            R.layout.favourite_list_item,
            new IndirectArrayAdapter.ListGenerator<Favourite>() {
                @Override
                public List<Favourite> makeList() {
                    return IteratorUtils.toList(Favourite.findAll(Favourite.class));
                }
            },
            new IndirectArrayAdapter.ViewGenerator<Favourite>() {
                @Override
                public void applyView(View v, Favourite f) {
                    TextView name = (TextView) v.findViewById(R.id.name);
                    name.setText(f.Name);
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
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_settings:
                Intent intent = new Intent(getActivity(), SettingsActivity.class);
                startActivity(intent);
                return true;
            case R.id.menu_database:
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

    @Override
    public void onResume() {
        super.onResume();
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mFavouriteList = null;
        super.onDestroyView();
    }

    private OcTranspoDataAccess mOcTranspo;
    private ListFavouritesActivity mActivity;
    private ListView mFavouriteList;
    private IndirectArrayAdapter<Favourite> mAdapter;
}
