package io.github.jbytheway.octranspoalarm.ui;

import android.content.Intent;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import org.apache.commons.collections4.IteratorUtils;

import java.util.List;

import io.github.jbytheway.octranspoalarm.Favourite;
import io.github.jbytheway.octranspoalarm.IndirectArrayAdapter;
import io.github.jbytheway.octranspoalarm.R;

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
                    name.setText(f.Name);
                }
            }
        );
        mFavouriteList.setAdapter(mAdapter);

        mFavouriteList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Favourite item = mAdapter.getItem(position);
                Intent intent = new Intent(getActivity(), ViewFavouriteActivity.class);
                intent.putExtra(ViewFavouriteActivity.FAVOURITE_ID, item.getId());

                startActivity(intent);
            }
        });

        mFavouriteList.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
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
    public void onResume() {
        super.onResume();
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public void onDestroyView() {
        mFavouriteList = null;
        super.onDestroyView();
    }

    private ListView mFavouriteList;
    private IndirectArrayAdapter<Favourite> mAdapter;
}
