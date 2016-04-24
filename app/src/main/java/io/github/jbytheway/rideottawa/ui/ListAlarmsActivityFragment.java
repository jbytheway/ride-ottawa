package io.github.jbytheway.rideottawa.ui;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.common.collect.ImmutableList;

import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.List;

import io.github.jbytheway.rideottawa.Alarm;
import io.github.jbytheway.rideottawa.ForthcomingTrip;
import io.github.jbytheway.rideottawa.OcTranspoApi;
import io.github.jbytheway.rideottawa.OcTranspoDataAccess;
import io.github.jbytheway.rideottawa.PendingAlarmData;
import io.github.jbytheway.rideottawa.R;
import io.github.jbytheway.rideottawa.RideOttawaApplication;
import io.github.jbytheway.rideottawa.utils.IndirectArrayAdapter;

public class ListAlarmsActivityFragment extends Fragment {
    private static final int MINIMUM_REFRESH_SECONDS = 15;

    public ListAlarmsActivityFragment() {
        mAlarms = new ArrayList<>();
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

    static class AlarmWithId {
        public AlarmWithId(long id, Alarm alarm) {
            Id = id;
            Alarm = alarm;
        }

        public final long Id;
        public final Alarm Alarm;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.list_alarms_fragment, container, false);

        TextView noAlarmsMessage = (TextView) view.findViewById(R.id.no_alarms_message);
        long numAlarms = PendingAlarmData.count(PendingAlarmData.class);

        if (numAlarms == 0) {
            noAlarmsMessage.setVisibility(View.VISIBLE);
        } else {
            noAlarmsMessage.setVisibility(View.GONE);
        }

        ListView alarmList = (ListView) view.findViewById(R.id.alarms);

        final Alarm.OnRefreshedListener listener = new Alarm.OnRefreshedListener() {
            @Override
            public void onRefreshed(Alarm alarm) {
                mAlarmAdapter.notifyDataSetChanged();
            }
        };

        final DisplayTripHelper tripHelper = new DisplayTripHelper(getActivity());

        createAlarms(listener, tripHelper);

        mAlarmAdapter = new IndirectArrayAdapter<>(
                getActivity(),
                R.layout.list_alarms_list_item,
                new IndirectArrayAdapter.ListGenerator<AlarmWithId>() {
                    @Override
                    public List<AlarmWithId> makeList() {
                        return mAlarms;
                    }
                },
                new IndirectArrayAdapter.ViewGenerator<AlarmWithId>() {
                    @Override
                    public void applyView(View v, final AlarmWithId alarmWithId) {
                        Alarm alarm = alarmWithId.Alarm;
                        ForthcomingTrip trip = alarm.getForthcomingTrip();
                        tripHelper.applyView(v, trip);

                        DateTime alarmTime = alarm.getTimeOfAlarm();

                        TextView alarmTimeView = (TextView) v.findViewById(R.id.alarm_time);
                        alarmTimeView.setText(tripHelper.formatTime(alarmTime));

                        TextView alarmMinutesAway = (TextView) v.findViewById(R.id.alarm_minutes_away);
                        tripHelper.formatMinutesAway(alarmTime, alarmMinutesAway);

                        Button deleteButton = (Button) v.findViewById(R.id.delete_button);
                        deleteButton.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                PendingAlarmData alarmData = PendingAlarmData.findById(PendingAlarmData.class, alarmWithId.Id);
                                // If the alarm just went off, it might be deleted, so check for null
                                if (alarmData != null) {
                                    PendingAlarmData.delete(alarmData);
                                }
                                createAlarms(listener, tripHelper);
                                mAlarmAdapter.notifyDataSetChanged();
                            }
                        });
                    }
                }
        );

        alarmList.setAdapter(mAlarmAdapter);

        return view;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_view_alarms, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_refresh:
                refreshIfLateEnough(true);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void createAlarms(Alarm.OnRefreshedListener listener, DisplayTripHelper tripHelper) {
        mAlarms.clear();
        for (PendingAlarmData alarmData : ImmutableList.copyOf(PendingAlarmData.findAll(PendingAlarmData.class))) {
            Alarm alarm = alarmData.makeAlarm(listener, mOcTranspo);
            if (alarm == null) {
                alarmData.delete();
            } else {
                mAlarms.add(new AlarmWithId(alarmData.getId(), alarm));
                tripHelper.addFavouriteStop(alarm.getFavouriteStop());
            }
        }

        refresh();
    }

    private boolean refreshIfLateEnough(boolean showMessage) {
        DateTime now = new DateTime();
        if (now.minusSeconds(MINIMUM_REFRESH_SECONDS).isBefore(mLastRefresh)) {
            // Too soon; we won't refresh yet
            if (showMessage) {
                Toast.makeText(getActivity(), getString(R.string.skipping_refresh_too_soon), Toast.LENGTH_LONG).show();
            }
            return false;
        } else {
            refresh();
            return true;
        }
    }

    private void refresh() {
        // The process of updating the Forthcoming trips can take a while (maybe half
        // a second or more depending on how many routes are involved.
        // Therefore we want to do it on a background thread.

        // First we check if we're already refreshing (very unlikely, because we force a fairly
        // long delay between refreshes)
        for (AlarmWithId alarmWithId : mAlarms) {
            alarmWithId.Alarm.refreshTimeEstimate(OcTranspoApi.Synchronicity.Asyncronous, getActivity(), mOcTranspo);
        }

        mLastRefresh = DateTime.now();

        /* Leaving out auto-refreshing for now
        // Whether we actually refreshed or not, trigger another refresh
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (isResumed()) {
                    refreshIfLateEnough(false);
                }
            }
        }, AUTO_REFRESH_SECONDS * 1000);*/
    }

    private OcTranspoDataAccess mOcTranspo;
    private ArrayList<AlarmWithId> mAlarms;
    private IndirectArrayAdapter<AlarmWithId> mAlarmAdapter;
    private DateTime mLastRefresh;
}
