package io.github.jbytheway.octranspoalarm;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

public class SelectStopActivity extends Activity {
    public static final String SELECTED_STOP = "selected_stop";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mOcTranspo = ((OcTranspoApplication) getApplication()).getOcTranspo();

        setContentView(R.layout.activity_select_stop);

        mStopList = (ListView) findViewById(R.id.stop_list);

        // For the cursor adapter, specify which columns go into which views
        String[] fromColumns = {"stop_code", "stop_name"};
        int[] toViews = {R.id.stop_code, R.id.stop_name};

        Cursor cursor = mOcTranspo.getAllStops("stop_name");

        mAdapter = new SimpleCursorAdapter(this,
                R.layout.select_stop_list_item, cursor,
                fromColumns, toViews, 0);

        mStopList.setAdapter(mAdapter);

        mStopList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
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
    }

    private OcTranspoDataAccess mOcTranspo;
    private ListView mStopList;
    private SimpleCursorAdapter mAdapter;
}
