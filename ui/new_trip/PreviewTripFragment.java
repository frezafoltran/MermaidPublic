package com.foltran.mermaid.ui.new_trip;

import android.annotation.SuppressLint;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

import com.foltran.mermaid.MainActivity;
import com.foltran.mermaid.R;
import com.foltran.mermaid.adapter.trip.TripCardAdapter;
import com.foltran.mermaid.model.trip.TripCard;
import com.foltran.mermaid.model.trip.TripCardWrapper;

import java.util.ArrayList;
import java.util.List;

public class PreviewTripFragment extends Fragment {

    int color;

    private List<TripCard> nonEditableCards;
    private ListView tripList;
    private TripCardAdapter tripAdapter;

    private Bundle workingTripBundle;

    private MainActivity activity;
    private TextView tripTitle;

    private NewTripViewModel model;

    public PreviewTripFragment() {
    }

    @SuppressLint("ValidFragment")
    public PreviewTripFragment(int color) {
        this.color = color;
    }

    @Override
    public void onCreate(Bundle bundle){
        super.onCreate(bundle);
        model = ViewModelProviders.of(requireActivity()).get(NewTripViewModel.class);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.new_trip_preview, container, false);

        activity = (MainActivity) getActivity();
        tripList = (ListView) root.findViewById(R.id.new_trip_feed);
        tripTitle = root.findViewById(R.id.trip_title);
        nonEditableCards = new ArrayList<>();

        handleTripChanges();
        return root;
    }

    @Override
    public void onResume() {

        super.onResume();
        handleTripChanges();
    }

    private void handleTripChanges(){

        TripCardWrapper trip = model.getWorkingTrip().getValue();

        tripTitle.setText(trip.getTripTitle());
        nonEditableCards = new ArrayList<>(trip.getAllCards());
        tripAdapter = new TripCardAdapter(nonEditableCards, getContext(), null);
        tripList.setAdapter(tripAdapter);
    }

}
