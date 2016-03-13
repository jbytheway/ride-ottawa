package io.github.jbytheway.rideottawa.ui;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import io.github.jbytheway.rideottawa.Favourite;
import io.github.jbytheway.rideottawa.FavouriteRoute;
import io.github.jbytheway.rideottawa.FavouriteStop;
import io.github.jbytheway.rideottawa.utils.IndirectArrayAdapter;
import io.github.jbytheway.rideottawa.RideOttawaApplication;
import io.github.jbytheway.rideottawa.OcTranspoDataAccess;
import io.github.jbytheway.rideottawa.R;
import io.github.jbytheway.rideottawa.Route;
import io.github.jbytheway.rideottawa.Stop;

public class EditFavouriteActivityFragment extends Fragment {
    private static final String TAG = "EditFavouriteFragment";

    private static final int REQUEST_NEW_STOP = 1;
    private static final int REQUEST_ROUTES = 2;

    public EditFavouriteActivityFragment() {
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
    public View onCreateView(final LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_edit_favourite, container, false);
        ListView stopList = (ListView) view.findViewById(R.id.stop_list);

        mStopAdapter = new IndirectArrayAdapter<>(
                getActivity(),
                R.layout.edit_stop_list_item,
                new IndirectArrayAdapter.ListGenerator<FavouriteStop>() {
                    @Override
                    public List<FavouriteStop> makeList() {
                        if (mFavourite == null) {
                            return new ArrayList<>();
                        } else {
                            return mFavourite.getStops();
                        }
                    }
                },
                new IndirectArrayAdapter.ViewGenerator<FavouriteStop>() {
                    @Override
                    public void applyView(View v, final FavouriteStop favouriteStop) {
                        TextView stop_code = (TextView) v.findViewById(R.id.stop_code);
                        TextView stop_name = (TextView) v.findViewById(R.id.stop_name);
                        final Stop stop = mOcTranspo.getStop(favouriteStop.StopId);
                        stop_code.setText(stop.getCode());
                        stop_name.setText(stop.getName());

                        Button addRouteButton = (Button) v.findViewById(R.id.choose_routes_button);
                        addRouteButton.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                Intent intent = new Intent(getActivity(), SelectRoutesActivity.class);
                                intent.putExtra(SelectRoutesActivity.STOP_ID, stop.getId());
                                List<FavouriteRoute> routes = favouriteStop.getRoutes();
                                ArrayList<Route> selectedRoutes = new ArrayList<>();
                                for (FavouriteRoute route : routes) {
                                    selectedRoutes.add(route.asRoute());
                                }
                                intent.putParcelableArrayListExtra(SelectRoutesActivity.SELECTED_ROUTES, selectedRoutes);
                                startActivityForResult(intent, REQUEST_ROUTES);
                            }
                        });

                        Button deleteStopButton = (Button) v.findViewById(R.id.delete_button);
                        deleteStopButton.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                mFavourite.removeStop(favouriteStop);
                                mStopAdapter.notifyDataSetChanged();
                            }
                        });

                        LinearLayout routeList = (LinearLayout) v.findViewById(R.id.route_list);
                        routeList.removeAllViews();
                        List<FavouriteRoute> routes = favouriteStop.getRoutes();
                        for (FavouriteRoute route : routes) {
                            View routeView = inflater.inflate(R.layout.edit_stop_route_list_item, routeList, false);
                            TextView routeName = (TextView) routeView.findViewById(R.id.route_name);
                            route.asRoute().applyToTextView(routeName);
                            TextView destination = (TextView) routeView.findViewById(R.id.destination);
                            destination.setText(""); // TODO: destination in FavouriteRoute
                            routeList.addView(routeView);
                        }
                    }
                }
        );

        stopList.setAdapter(mStopAdapter);

        mName = (TextView) view.findViewById(R.id.name);

        FloatingActionButton fab = (FloatingActionButton) view.findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getActivity(), SelectStopActivity.class);
                startActivityForResult(intent, REQUEST_NEW_STOP);
            }
        });

        Button saveButton = (Button) view.findViewById(R.id.save_button);
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateFavourite();
                mFavourite.saveRecursively();
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
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_NEW_STOP:
                if (resultCode == Activity.RESULT_OK) {
                    String stopId = data.getStringExtra(SelectStopActivity.SELECTED_STOP);
                    mFavourite.addStop(stopId);
                    mStopAdapter.notifyDataSetChanged();
                }
                break;
            case REQUEST_ROUTES:
                if (resultCode == Activity.RESULT_OK) {
                    String stopId = data.getStringExtra(SelectRoutesActivity.STOP_ID);
                    FavouriteStop stop = mFavourite.getStop(stopId);
                    ArrayList<Route> selectedRoutes = data.getParcelableArrayListExtra(SelectRoutesActivity.SELECTED_ROUTES);
                    stop.updateRoutes(selectedRoutes);
                    if (stop.getId() != null) {
                        stop.saveRecursively();
                    }
                    mStopAdapter.notifyDataSetChanged();
                }
                break;
            default:
                throw new AssertionError("Unexpected request code");
        }
    }

    @Override
    public void onDetach() {
        mFavourite = null;
        super.onDetach();
    }

    @Override
    public void onDestroyView() {
        mStopAdapter = null;
        super.onDestroyView();
    }

    private void populateFromFavourite() {
        mName.setText(mFavourite.Name);
        mStopAdapter.notifyDataSetChanged();
    }

    private void updateFavourite() {
        mFavourite.Name = mName.getText().toString();
    }

    private OcTranspoDataAccess mOcTranspo;
    private IndirectArrayAdapter<FavouriteStop> mStopAdapter;
    private Favourite mFavourite;
    private TextView mName;
}
