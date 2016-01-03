package io.github.jbytheway.octranspoalarm;

import android.app.Fragment;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

public class EditFavouriteActivityFragment extends Fragment {
    private static final String TAG = "EditFavouriteFragment";

    public EditFavouriteActivityFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Don't destroy Fragment on reconfiguration
        setRetainInstance(true);

        // This Fragment adds options to the ActionBar
        setHasOptionsMenu(true);

        mOcTranspo = ((OcTranspoApplication) getActivity().getApplication()).getOcTranspo();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_edit_favourite, container, false);
        mStopRouteList = (ListView) view.findViewById(R.id.stop_route_list_view);

        Cursor c = mOcTranspo.getRoutesForStop("3038");
        String[] array = new String[c.getCount()];
        int route_column = c.getColumnIndex("route_short_name");
        c.moveToFirst();
        for (int i = 0; i < array.length; ++i) {
            array[i] = c.getString(route_column);
            if (!c.moveToNext())
                break;
        }

        c.close();

        ArrayAdapter<String> adapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_list_item_1, array);
        mStopRouteList.setAdapter(adapter);

        mName = (TextView) view.findViewById(R.id.name);

        Button saveButton = (Button) view.findViewById(R.id.save_button);
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateFavourite();
                mFavourite.save();
                getActivity().finish();
            }
        });

        return view;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_edit_favourite, menu);
    }

    public void initialize(Intent intent) {
        boolean newFavourite = intent.getBooleanExtra(EditFavouriteActivity.NEW_FAVOURITE, true);

        if (newFavourite) {
            mFavourite = new Favourite();
        } else {
            long favouriteId = intent.getLongExtra(EditFavouriteActivity.FAVOURITE_ID, -1);
            if (favouriteId == -1) {
                Log.e(TAG, "Missing FAVOURITE_ID in EditFavourite Intent");
            } else {
                mFavourite = Favourite.findById(Favourite.class, favouriteId);
                populateFromFavourite();
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.edit_menu_delete:
                Long id = mFavourite.getId();
                if (id != null) {
                    mFavourite.deleteRecursively();
                }
                getActivity().finish();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onDetach() {
        mFavourite = null;
        super.onDetach();
    }

    @Override
    public void onDestroyView() {
        mStopRouteList = null;
        super.onDestroyView();
    }

    private void populateFromFavourite() {
        mName.setText(mFavourite.Name);
    }

    private void updateFavourite() {
        mFavourite.Name = mName.getText().toString();
    }

    private OcTranspoDataAccess mOcTranspo;
    private ListView mStopRouteList;
    private Favourite mFavourite;
    private TextView mName;
}
