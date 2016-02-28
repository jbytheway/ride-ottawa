package io.github.jbytheway.rideottawa.ui;

import android.content.Intent;
import android.os.Bundle;
import android.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

import org.joda.time.DateTime;
import org.joda.time.Interval;

import java.util.ArrayList;
import java.util.List;

import io.github.jbytheway.rideottawa.ArrivalEstimate;
import io.github.jbytheway.rideottawa.Favourite;
import io.github.jbytheway.rideottawa.ForthcomingTrip;
import io.github.jbytheway.rideottawa.OcTranspoApi;
import io.github.jbytheway.rideottawa.utils.IndirectArrayAdapter;
import io.github.jbytheway.rideottawa.RideOttawaApplication;
import io.github.jbytheway.rideottawa.OcTranspoDataAccess;
import io.github.jbytheway.rideottawa.R;
import io.github.jbytheway.rideottawa.Route;
import io.github.jbytheway.rideottawa.Stop;

public class ViewFavouriteActivityFragment extends Fragment implements OcTranspoApi.Listener {
    private static final String TAG = "ViewFavouriteFragment";

    public ViewFavouriteActivityFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Don't destroy Fragment on reconfiguration
        setRetainInstance(true);

        mOcTranspo = ((RideOttawaApplication) getActivity().getApplication()).getOcTranspo();
        // Need an empty list of trips to start with because the ListView will
        // be rendered before we get informed of our Favourite.
        mForthcomingTrips = new ArrayList<>();
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
                        return mForthcomingTrips;
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
                        TextView minutes_away = (TextView) v.findViewById(R.id.minutes_away);
                        TextView time_type = (TextView) v.findViewById(R.id.time_type);
                        Stop stop = trip.getStop();
                        stop_code.setText(stop.getCode());
                        stop_name.setText(stop.getName());
                        Route route = trip.getRoute();
                        route_name.setText(route.getName());
                        head_sign.setText(trip.getHeadSign());
                        arrival_time.setText(trip.getArrivalTimeString());
                        ArrivalEstimate ae = trip.getEstimatedArrival();
                        DateTime estimatedArrival = ae.getTime();
                        DateTime now = mOcTranspo.getNow();
                        long minutesAway;
                        if (now.isAfter(estimatedArrival)) {
                            Interval intervalToArrival = new Interval(estimatedArrival, now);
                            minutesAway = -intervalToArrival.toDuration().getStandardMinutes();
                        } else {
                            Interval intervalToArrival = new Interval(now, estimatedArrival);
                            minutesAway = intervalToArrival.toDuration().getStandardMinutes();
                        }
                        minutes_away.setText(getString(R.string.minutes_format, minutesAway));

                        switch (ae.getType()) {
                            case Gps:
                                time_type.setText(getString(R.string.gps_abbrev));
                                break;
                            case Schedule:
                                time_type.setText(getString(R.string.scheduled_abbrev));
                                break;
                            default:
                                throw new AssertionError("Unexpected estimate type "+ae.getType());
                        }
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
        mForthcomingTrips = mFavourite.getForthcomingTrips(mOcTranspo);
        // TODO: sort forthcoming trips
        mOcTranspo.getLiveDataForTrips(getActivity(), mForthcomingTrips, this);
        mTripAdapter.notifyDataSetChanged();
    }

    public void onApiFail(Exception e) {
        // TODO: report to user somehow?
    }

    public void onTripData() {
        mTripAdapter.notifyDataSetChanged();
    }

    private OcTranspoDataAccess mOcTranspo;
    private Favourite mFavourite;
    private List<ForthcomingTrip> mForthcomingTrips;
    private IndirectArrayAdapter<ForthcomingTrip> mTripAdapter;
    private TextView mName;
}