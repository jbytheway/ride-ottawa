package io.github.jbytheway.octranspoalarm;

import android.app.Fragment;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

/**
 * A placeholder fragment containing a simple view.
 */
public class EditFavouriteActivityFragment extends Fragment {

    private static final String TAG = "EditFavouriteFragment";

    public EditFavouriteActivityFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_edit_favourite, container, false);
        mStopRouteList = (ListView) view.findViewById(R.id.stop_route_list_view);

        String[] array = new String[4];
        OcTranspoDbHelper dbHelper = new OcTranspoDbHelper(getActivity());
        SQLiteDatabase database = dbHelper.getReadableDatabase();
        Cursor c = database.rawQuery("SELECT * FROM stop_times", null);
        int arrival_time_column = c.getColumnIndex("arrival_time");
        c.moveToFirst();

        // For now, we have a hardcoded string array for our list, for testing
        for (int i = 0; i < array.length; ++i) {
            array[i] = c.getString(arrival_time_column);
            if (!c.moveToNext())
                break;
        }

        c.close();

        ArrayAdapter<String> adapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_list_item_1, array);
        mStopRouteList.setAdapter(adapter);
        Log.i(TAG, "Added adapter");
        return view;
    }

    @Override
    public void onDestroyView() {
        mStopRouteList = null;
    }

    private ListView mStopRouteList;
}
