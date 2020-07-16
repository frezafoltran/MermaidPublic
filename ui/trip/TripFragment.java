package com.foltran.mermaid.ui.trip;

import android.os.Bundle;

import androidx.fragment.app.Fragment;


import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.foltran.mermaid.R;
import com.foltran.mermaid.adapter.trip.TripCardAdapter;
import com.foltran.mermaid.model.trip.TripCardWrapper;


/**
 * A simple {@link Fragment} subclass.
 */
public class TripFragment extends Fragment {

    private String TAG = "----- Trip Fragment: ";
    private TripCardWrapper trip;
    private TextView tripTitleView;

    private ListView tripList;
    private TripCardAdapter tripAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View root = inflater.inflate(R.layout.fragment_trip, container, false);

        tripList = (ListView) root.findViewById(R.id.trip_feed);
        tripTitleView = root.findViewById(R.id.trip_title);


        Bundle bundle = this.getArguments();
        if (bundle != null) {
            trip = (TripCardWrapper) bundle.getSerializable("curPost");
            buildTrip();
        }
        else{
            Toast.makeText(getContext(), "Failed", Toast.LENGTH_SHORT).show();
        }

        return root;
    }

    private void buildTrip(){

        tripTitleView.setText(trip.getTripTitle());

        tripAdapter = new TripCardAdapter(trip.getAllCards(), getContext(), null);
        tripList.setAdapter(tripAdapter);

    }
}
