package com.foltran.mermaid.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;

import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.foltran.mermaid.R;
import com.foltran.mermaid.model.trip.TripCardWrapper;
import com.foltran.mermaid.adapter.trip_post.TripPostAdapter;
import java.util.List;

public class HomeFragment extends Fragment {

    private  RecyclerView feedRecycler;

    private HomeViewModel mHomeViewModel;
    private TripPostAdapter mFeedAdapter;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mHomeViewModel = ViewModelProviders.of(requireActivity()).get(HomeViewModel.class);
        mHomeViewModel.init();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {


        View root = inflater.inflate(R.layout.fragment_home, container, false);
        feedRecycler = (RecyclerView) root.findViewById(R.id.home_feed);

        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {

        mHomeViewModel.getFeedTripPosts().observe(this, new Observer<List<TripCardWrapper>>() {
            @Override
            public void onChanged(List<TripCardWrapper> tripCardWrappers) {
                mFeedAdapter.notifyDataSetChanged();
            }
        });

        initFeedRecyclerView();

    }

    private void initFeedRecyclerView(){
        mFeedAdapter = new TripPostAdapter(getContext(), mHomeViewModel.getFeedTripPosts().getValue(), this);

        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getContext());
        feedRecycler.setLayoutManager(layoutManager);
        feedRecycler.setAdapter(mFeedAdapter);
    }

}
