package com.foltran.mermaid.repositories;

import androidx.lifecycle.MutableLiveData;

import com.foltran.mermaid.model.trip.FeedCardSerializable;
import com.foltran.mermaid.model.trip.TripCardWrapper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TripCardWrapperRepository {

    private static TripCardWrapperRepository instance;
    private List<TripCardWrapper> feedTripPosts = new ArrayList<>();

    public static TripCardWrapperRepository getInstance(){
        if (instance == null){
            instance = new TripCardWrapperRepository();
        }
        return instance;
    }

    public MutableLiveData<List<TripCardWrapper>> getFeedTripPosts(){
        setFeedTripPosts();
        MutableLiveData<List<TripCardWrapper>> data = new MutableLiveData<>();
        data.setValue(feedTripPosts);
        return data;
    }

    private void setFeedTripPosts(){
        TripCardWrapper trip = new TripCardWrapper();

        trip.setTripTitle("Summer in Chicago");
        trip.addCard(new FeedCardSerializable(
                new ArrayList<>(Arrays.asList("First stop, the Bean!")),
                new ArrayList<>(Arrays.asList("chicago.jpg"))));

        TripCardWrapper trip2 = new TripCardWrapper();

        trip2.setTripTitle("Summer in Chicago");
        trip2.addCard(new FeedCardSerializable(
                new ArrayList<>(Arrays.asList("chicago so pretty!")),
                new ArrayList<>(Arrays.asList("img_0.jpg"))));

        feedTripPosts.add(trip);
        feedTripPosts.add(trip2);
    }
}
