package io.github.jbytheway.octranspoalarm;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class SelectRoutesActivityFragment extends Fragment {

    public SelectRoutesActivityFragment() {
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
        View view = inflater.inflate(R.layout.fragment_select_routes, container, false);

        ListView routeList = (ListView) view.findViewById(R.id.route_list);

        Intent intent = getActivity().getIntent();
        final String stopId = intent.getStringExtra(SelectRoutesActivity.STOP_ID);
        ArrayList<Route> selectedRoutesArray = intent.getParcelableArrayListExtra(SelectRoutesActivity.SELECTED_ROUTES);
        if (selectedRoutesArray == null) {
            throw new AssertionError("Intent lacked SELECTED_ROUTES");
        }
        mSelectedRoutes = new HashSet<>(selectedRoutesArray);

        Cursor cursor = mOcTranspo.getRoutesForStopById(stopId);
        final List<Route> routes = mOcTranspo.routeCursorToList(cursor);
        mAdapter = new IndirectArrayAdapter<>(
                getActivity(),
                R.layout.select_route_list_item,
                new IndirectArrayAdapter.ListGenerator<Route>() {
                    @Override
                    public List<Route> makeList() {
                        return routes;
                    }
                },
                new IndirectArrayAdapter.ViewGenerator<Route>() {
                    @Override
                    public void applyView(View v, final Route route) {
                        CheckBox check = (CheckBox) v.findViewById(R.id.check);
                        check.setText(route.getName());
                        // Must remove the listener first because if this View is reused from a
                        // previous call then it might mess with mSelectedRoutes in a bad way
                        // when we set the checked state here.
                        check.setOnCheckedChangeListener(null);
                        check.setChecked(mSelectedRoutes.contains(route));
                        check.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                            @Override
                            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                                if (isChecked) {
                                    mSelectedRoutes.add(route);
                                } else {
                                    mSelectedRoutes.remove(route);
                                }
                            }
                        });
                    }
                }
        );

        routeList.setAdapter(mAdapter);

        Button saveButton = (Button) view.findViewById(R.id.save_button);

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent result = new Intent();
                ArrayList<Route> selectedRoutesArrayList = new ArrayList<>(mSelectedRoutes);
                result.putExtra(SelectRoutesActivity.STOP_ID, stopId);
                result.putParcelableArrayListExtra(SelectRoutesActivity.SELECTED_ROUTES, selectedRoutesArrayList);
                getActivity().setResult(Activity.RESULT_OK, result);
                getActivity().finish();
            }
        });

        return view;
    }

    private OcTranspoDataAccess mOcTranspo;
    private IndirectArrayAdapter<Route> mAdapter;
    private HashSet<Route> mSelectedRoutes;
}
