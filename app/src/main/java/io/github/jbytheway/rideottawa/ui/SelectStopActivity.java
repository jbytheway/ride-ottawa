package io.github.jbytheway.rideottawa.ui;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.FilterQueryProvider;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

import io.github.jbytheway.rideottawa.RideOttawaApplication;
import io.github.jbytheway.rideottawa.OcTranspoDataAccess;
import io.github.jbytheway.rideottawa.R;
import io.github.jbytheway.rideottawa.Stop;

public class SelectStopActivity extends AppCompatActivity {
    public static final String SELECTED_STOP = "selected_stop";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mOcTranspo = ((RideOttawaApplication) getApplication()).getOcTranspo();

        setContentView(R.layout.activity_select_stop);

        ListView stopList = (ListView) findViewById(R.id.stop_list);

        // For the cursor adapter, specify which columns go into which views
        String[] fromColumns = {"stop_code", "stop_name"};
        int[] toViews = {R.id.stop_code, R.id.stop_name};

        Cursor cursor = mOcTranspo.getAllStops("stop_name");

        mAdapter = new SimpleCursorAdapter(this,
                R.layout.select_stop_list_item, cursor,
                fromColumns, toViews, 0);
        mAdapter.setFilterQueryProvider(new FilterQueryProvider() {
            @Override
            public Cursor runQuery(CharSequence constraint) {
                return mOcTranspo.getAllStopsMatching(constraint.toString(), "stop_name");
            }
        });

        stopList.setAdapter(mAdapter);

        stopList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent result = new Intent();
                Stop stop = mOcTranspo.getStop(id);
                String stopId = stop.getId();
                result.putExtra(SELECTED_STOP, stopId);
                setResult(RESULT_OK, result);
                finish();
            }
        });

        EditText stopFilter = (EditText) findViewById(R.id.stop_filter);
        stopFilter.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                mAdapter.getFilter().filter(s.toString());
            }
        });
    }

    private OcTranspoDataAccess mOcTranspo;
    private SimpleCursorAdapter mAdapter;
}
