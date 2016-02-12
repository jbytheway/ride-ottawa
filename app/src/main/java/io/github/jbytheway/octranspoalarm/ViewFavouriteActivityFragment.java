package io.github.jbytheway.octranspoalarm;

import android.content.Intent;
import android.os.Bundle;
import android.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class ViewFavouriteActivityFragment extends Fragment {
    private static final String TAG = "ViewFavouriteFragment";

    public ViewFavouriteActivityFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Don't destroy Fragment on reconfiguration
        setRetainInstance(true);

        mOcTranspo = ((OcTranspoApplication) getActivity().getApplication()).getOcTranspo();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_view_favourite, container, false);

        mName = (TextView) view.findViewById(R.id.name);

        ListView tripList = (ListView) view.findViewById(R.id.trip_list);

        mTripAdapter = new IndirectArrayAdapter<>(
                getActivity(),
                R.layout.view_favourite_list_item,
                new IndirectArrayAdapter.ListGenerator<ForthcomingTrip>() {
                    @Override
                    public List<ForthcomingTrip> makeList() {
                        // Unfortunately, this can get called before mFavourite is set
                        if (mFavourite == null) {
                            return new ArrayList<>();
                        } else {
                            return mFavourite.getForthcomingTrips(mOcTranspo);
                        }
                    }
                },
                new IndirectArrayAdapter.ViewGenerator<ForthcomingTrip>() {
                    @Override
                    public void applyView(View v, final ForthcomingTrip trip) {
                        TextView stop_code = (TextView) v.findViewById(R.id.stop_code);
                        TextView stop_name = (TextView) v.findViewById(R.id.stop_name);
                        TextView route_name = (TextView) v.findViewById(R.id.route_name);
                        TextView head_sign = (TextView) v.findViewById(R.id.head_sign);
                        TextView arrival_time = (TextView) v.findViewById(R.id.arrival_time);
                        Stop stop = trip.getStop();
                        stop_code.setText(stop.getCode());
                        stop_name.setText(stop.getName());
                        Route route = trip.getRoute();
                        route_name.setText(route.getName());
                        head_sign.setText(trip.getHeadSign());
                        arrival_time.setText(trip.getArrivalTimeString());
                    }
                }
        );

        tripList.setAdapter(mTripAdapter);

        return view;
    }

    public void initialize(Intent intent) {
        long favouriteId = intent.getLongExtra(EditFavouriteActivity.FAVOURITE_ID, -1);
        if (favouriteId == -1) {
            Log.e(TAG, "Missing FAVOURITE_ID in ViewFavourite Intent");
        } else {
            mFavourite = Favourite.findById(Favourite.class, favouriteId);
            populateFromFavourite();
        }
    }

    private void populateFromFavourite() {
        mName.setText(mFavourite.Name);
        mTripAdapter.notifyDataSetChanged();
    }

    private OcTranspoDataAccess mOcTranspo;
    private Favourite mFavourite;
    private IndirectArrayAdapter<ForthcomingTrip> mTripAdapter;
    private TextView mName;
}
