package io.github.jbytheway.rideottawa.ui;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
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
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

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
import io.github.jbytheway.rideottawa.utils.Keyboard;

public class EditFavouriteActivityFragment extends Fragment {
    private static final String TAG = "EditFavouriteFragment";

    private static final int REQUEST_NEW_STOP = 1;
    private static final int REQUEST_DESTINATION = 2;
    private static final int REQUEST_ROUTES = 3;

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
        View view = inflater.inflate(R.layout.edit_favourite_fragment, container, false);
        ListView stopList = (ListView) view.findViewById(R.id.stop_list);
        final Context context = getActivity();

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
                        stop_name.setText(stop.getName(context));

                        Button chooseRoutesButton = (Button) v.findViewById(R.id.choose_routes_button);
                        chooseRoutesButton.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                selectRoutesForStop(favouriteStop);
                            }
                        });

                        Button addDestinationButton = (Button) v.findViewById(R.id.add_destination_button);
                        addDestinationButton.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                addDestinationForStop(favouriteStop);
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
                            route.asRoute(mOcTranspo).applyToTextView(routeName);
                            TextView destinationView = (TextView) routeView.findViewById(R.id.destination);
                            String destinationStopId = route.Destination;
                            if (destinationStopId == null) {
                                destinationView.setVisibility(View.GONE);
                            } else {
                                Stop destination = mOcTranspo.getStop(route.Destination);
                                destinationView.setText(destination.getName(context));
                                destinationView.setVisibility(View.VISIBLE);
                            }
                            routeList.addView(routeView);
                        }
                    }
                }
        );

        stopList.setAdapter(mStopAdapter);

        mName = (TextView) view.findViewById(R.id.name);

        Button addStopButton = (Button) view.findViewById(R.id.add_stop);
        addStopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getActivity(), SelectStopActivity.class);
                startActivityForResult(intent, REQUEST_NEW_STOP);
            }
        });

        Button saveButton = (Button) view.findViewById(R.id.save);
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveAndClose();
            }
        });

        mHintArea = (LinearLayout) view.findViewById(R.id.hint_area);

        ImageButton closeHintButton = (ImageButton) view.findViewById(R.id.close_hint_button);
        closeHintButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "Hiding hint because close button clicked");
                hideHint();
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

    private void hideHint() {
        mHintArea.getLayoutParams().height = 0;
        mHintArea.requestLayout();
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

    private void selectRoutesForStop(FavouriteStop favouriteStop) {
        Intent intent = new Intent(getActivity(), SelectRoutesActivity.class);
        intent.putExtra(SelectRoutesActivity.STOP_ID, favouriteStop.StopId);
        List<FavouriteRoute> routes = favouriteStop.getRoutes();
        ArrayList<Route> selectedRoutes = new ArrayList<>();
        for (FavouriteRoute route : routes) {
            selectedRoutes.add(route.asRoute(mOcTranspo));
        }
        intent.putParcelableArrayListExtra(SelectRoutesActivity.SELECTED_ROUTES, selectedRoutes);
        startActivityForResult(intent, REQUEST_ROUTES);
    }

    private void addDestinationForStop(FavouriteStop favouriteStop) {
        Intent intent = new Intent(getActivity(), SelectStopActivity.class);
        intent.putExtra(SelectStopActivity.FROM_STOP_ID, favouriteStop.StopId);
        startActivityForResult(intent, REQUEST_DESTINATION);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Regardless of the reason we got back here, we really want to hide the soft keyboard,
        // because with it open, almost nothing of this view is visible
        Keyboard.hideKeyboard(getActivity());

        switch (requestCode) {
            case REQUEST_NEW_STOP:
                if (resultCode == Activity.RESULT_OK) {
                    String stopId = data.getStringExtra(SelectStopActivity.SELECTED_STOP);
                    // First check if we already have this stop, and if so, pass
                    if (mFavourite.hasStop(stopId)) {
                        Toast.makeText(getActivity(), R.string.duplicate_stop_message, Toast.LENGTH_LONG).show();
                    } else {
                        mFavourite.addStop(stopId);
                        // In the event that this stop has but a single Route, we wish to add it forthwith.
                        Cursor c = mOcTranspo.getRoutesForStopById(stopId);
                        FavouriteStop stop = mFavourite.getStop(stopId);
                        if (c.getCount() == 1) {
                            List<Route> routes = mOcTranspo.routeCursorToList(c);
                            stop.addRoute(routes.get(0), null);
                        } else if (c.getCount() == 0) {
                            // Strange case; there are no routes for this stop.
                            Toast.makeText(getActivity(), getString(R.string.no_routes_available), Toast.LENGTH_LONG).show();
                        }
                        c.close();
                        mStopAdapter.notifyDataSetChanged();
                    }
                }
                break;
            case REQUEST_ROUTES:
                if (resultCode == Activity.RESULT_OK) {
                    String stopId = data.getStringExtra(SelectRoutesActivity.STOP_ID);
                    FavouriteStop stop = mFavourite.getStop(stopId);
                    ArrayList<Route> selectedRoutes = data.getParcelableArrayListExtra(SelectRoutesActivity.SELECTED_ROUTES);
                    stop.updateRoutes(selectedRoutes, mOcTranspo);
                    if (stop.getId() != null) {
                        stop.saveRecursively();
                    }
                    mStopAdapter.notifyDataSetChanged();
                }
                break;
            case REQUEST_DESTINATION:
                if (resultCode == Activity.RESULT_OK) {
                    String fromStopId = data.getStringExtra(SelectStopActivity.FROM_STOP_ID);
                    String destStopId = data.getStringExtra(SelectStopActivity.SELECTED_STOP);
                    if (fromStopId == null) {
                        throw new AssertionError("Missing FROM_STOP_ID");
                    }
                    if (destStopId == null) {
                        throw new AssertionError("Missing SELECTED_STOP");
                    }
                    Cursor c = mOcTranspo.getRoutesBetweenStops(fromStopId, destStopId);
                    List<Route> newRoutes = mOcTranspo.routeCursorToList(c);
                    FavouriteStop stop = mFavourite.getStop(fromStopId);
                    stop.includeRoutes(newRoutes, destStopId, mOcTranspo);
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

    public void onBackPressed() {
        saveAndClose();
    }

    private void saveAndClose() {
        updateFavourite();
        List<FavouriteStop> stops = mFavourite.getStops();
        boolean hasStops = !stops.isEmpty();
        boolean hasName = !mFavourite.Name.isEmpty();
        Context context = getActivity();

        // Only keep it at all if it has at least something
        if (hasName || hasStops) {
            if (!hasName) {
                // If it has no name, concoct one
                FavouriteStop firstStop = stops.get(0);
                mFavourite.Name = firstStop.asStop(mOcTranspo).getName(context);
            }
            mFavourite.saveRecursively();
        } else {
            Toast.makeText(context, R.string.not_saving_empty_favourite, Toast.LENGTH_LONG).show();
        }
        getActivity().finish();
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

        // If there are things in the favourite already, then hide the hint
        // (we assume the user knows what they are doing at that point)
        List<FavouriteStop> stops = mFavourite.getStops();
        if (!stops.isEmpty()) {
            FavouriteStop firstStop = stops.get(0);
            List<FavouriteRoute> routes = firstStop.getRoutes();
            if (!routes.isEmpty()) {
                Log.d(TAG, "Hiding hint because favourite is populated");
                hideHint();
            }
        }
    }

    private void updateFavourite() {
        mFavourite.Name = mName.getText().toString();
    }

    private OcTranspoDataAccess mOcTranspo;
    private IndirectArrayAdapter<FavouriteStop> mStopAdapter;
    private Favourite mFavourite;
    private TextView mName;
    private LinearLayout mHintArea;
}
