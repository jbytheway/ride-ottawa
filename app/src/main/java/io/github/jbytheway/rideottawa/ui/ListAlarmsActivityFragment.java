package io.github.jbytheway.rideottawa.ui;

import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.List;

import io.github.jbytheway.rideottawa.Alarm;
import io.github.jbytheway.rideottawa.ForthcomingTrip;
import io.github.jbytheway.rideottawa.OcTranspoDataAccess;
import io.github.jbytheway.rideottawa.PendingAlarmData;
import io.github.jbytheway.rideottawa.R;
import io.github.jbytheway.rideottawa.RideOttawaApplication;
import io.github.jbytheway.rideottawa.utils.IndirectArrayAdapter;

public class ListAlarmsActivityFragment extends Fragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Don't destroy Fragment on reconfiguration
        setRetainInstance(true);

        // This Fragment adds options to the ActionBar
        setHasOptionsMenu(true);

        Context context = getActivity();
        mOcTranspo = ((RideOttawaApplication) getActivity().getApplication()).getOcTranspo();
    }

    static class AlarmWithId {
        public AlarmWithId(long id, Alarm alarm) {
            Id = id;
            Alarm = alarm;
        }

        public long Id;
        public Alarm Alarm;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_list_alarms, container, false);

        TextView noAlarmsMessage = (TextView) view.findViewById(R.id.no_alarms_message);
        long numAlarms = PendingAlarmData.count(PendingAlarmData.class);

        if (numAlarms == 0) {
            noAlarmsMessage.setVisibility(View.VISIBLE);
        } else {
            noAlarmsMessage.setVisibility(View.GONE);
        }

        mAlarmList = (ListView) view.findViewById(R.id.alarms);

        final Alarm.OnRefreshedListener listener = new Alarm.OnRefreshedListener() {
            @Override
            public void onRefreshed(Alarm alarm) {
                mAlarmAdapter.notifyDataSetChanged();
            }
        };
        final ArrayList<AlarmWithId> alarms = new ArrayList<>();

        final DisplayTripHelper tripHelper = new DisplayTripHelper(getActivity());

        createAlarms(alarms, listener, tripHelper);

        mAlarmAdapter = new IndirectArrayAdapter<>(
                getActivity(),
                R.layout.list_alarms_list_item,
                new IndirectArrayAdapter.ListGenerator<AlarmWithId>() {
                    @Override
                    public List<AlarmWithId> makeList() {
                        return alarms;
                    }
                },
                new IndirectArrayAdapter.ViewGenerator<AlarmWithId>() {
                    @Override
                    public void applyView(View v, final AlarmWithId alarmWithId) {
                        Alarm alarm = alarmWithId.Alarm;
                        ForthcomingTrip trip = alarm.getForthcomingTrip();
                        tripHelper.applyView(v, trip);

                        Button deleteButton = (Button) v.findViewById(R.id.delete_button);
                        deleteButton.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                PendingAlarmData alarmData = PendingAlarmData.findById(PendingAlarmData.class, alarmWithId.Id);
                                PendingAlarmData.delete(alarmData);
                                createAlarms(alarms, listener, tripHelper);
                                mAlarmAdapter.notifyDataSetChanged();
                            }
                        });
                    }
                }
        );

        mAlarmList.setAdapter(mAlarmAdapter);

        return view;
    }

    private void createAlarms(ArrayList<AlarmWithId> alarms, Alarm.OnRefreshedListener listener, DisplayTripHelper tripHelper) {
        alarms.clear();
        for (PendingAlarmData alarmData : ImmutableList.copyOf(PendingAlarmData.findAll(PendingAlarmData.class))) {
            Alarm alarm = alarmData.makeAlarm(listener, mOcTranspo);
            alarms.add(new AlarmWithId(alarmData.getId(), alarm));
            tripHelper.addFavouriteStop(alarm.getFavouriteStop());
        }
    }

    private OcTranspoDataAccess mOcTranspo;
    private ListView mAlarmList;
    private IndirectArrayAdapter<AlarmWithId> mAlarmAdapter;
}
