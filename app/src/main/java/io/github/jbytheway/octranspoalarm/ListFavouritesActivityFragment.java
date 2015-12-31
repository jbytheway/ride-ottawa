package io.github.jbytheway.octranspoalarm;

import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

/**
 * A placeholder fragment containing a simple view.
 */
public class ListFavouritesActivityFragment extends Fragment {

    public ListFavouritesActivityFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_list_favourites, container, false);
        mFavouriteList = (ListView) view.findViewById(R.id.favourite_list_view);

        // For now, we have a hardcoded string array for our list, for testing
        String[] array = new String[1];
        array[0] = "Hello";
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_1, array);
        mFavouriteList.setAdapter(adapter);

        return view;
    }

    @Override
    public void onDestroyView() {
        mFavouriteList = null;
    }

    private ListView mFavouriteList;
}
