package io.github.jbytheway.octranspoalarm;

import android.app.Fragment;
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

        // For now, we have a hardcoded string array for our list, for testing
        String[] array = new String[4];
        array[0] = "Hello";
        array[1] = "Hello";
        array[2] = "Hello";
        array[3] = "Hello";
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_1, array);
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
