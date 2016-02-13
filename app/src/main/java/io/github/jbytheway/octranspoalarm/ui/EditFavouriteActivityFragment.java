package io.github.jbytheway.octranspoalarm.ui;

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

import io.github.jbytheway.octranspoalarm.Favourite;
import io.github.jbytheway.octranspoalarm.FavouriteRoute;
import io.github.jbytheway.octranspoalarm.FavouriteStop;
import io.github.jbytheway.octranspoalarm.IndirectArrayAdapter;
import io.github.jbytheway.octranspoalarm.OcTranspoApplication;
import io.github.jbytheway.octranspoalarm.OcTranspoDataAccess;
import io.github.jbytheway.octranspoalarm.R;
import io.github.jbytheway.octranspoalarm.Route;
import io.github.jbytheway.octranspoalarm.Stop;

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

        mOcTranspo = ((OcTranspoApplication) getActivity().getApplication()).getOcTranspo();
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
                            return new ArrayList<FavouriteStop>();
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

                        Button addRouteButton = (Button) v.findViewById(R.id.add_route_button);
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

                        LinearLayout routeList = (LinearLayout) v.findViewById(R.id.route_list);
                        routeList.removeAllViews();
                        List<FavouriteRoute> routes = favouriteStop.getRoutes();
                        for (FavouriteRoute route : routes) {
                            View routeView = inflater.inflate(android.R.layout.simple_list_item_1, routeList, false);
                            TextView routeName = (TextView) routeView.findViewById(android.R.id.text1);
                            routeName.setText(route.RouteName);
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