package io.github.jbytheway.octranspoalarm;

import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

import org.apache.commons.collections4.IteratorUtils;

import java.util.List;

public class ListFavouritesActivityFragment extends Fragment {
    private static final String TAG = "ListFavouritesFragment";

    public ListFavouritesActivityFragment() {
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
                    name.setText(f.getName());
                }
            }
        );
        mFavouriteList.setAdapter(mAdapter);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public void onDestroyView() {
        mFavouriteList = null;
    }

    private ListView mFavouriteList;
    private IndirectArrayAdapter<Favourite> mAdapter;
}
