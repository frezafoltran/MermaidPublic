package com.foltran.mermaid.ui.home;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.foltran.mermaid.model.trip.TripCardWrapper;
import com.foltran.mermaid.repositories.TripCardWrapperRepository;

import java.util.List;

public class HomeViewModel extends ViewModel {

    private MutableLiveData<List<TripCardWrapper>> mFeedTripPosts;
    private TripCardWrapperRepository mTripCardWrapperRepo;


    public void init(){
        if (mFeedTripPosts != null) return;

        mTripCardWrapperRepo = TripCardWrapperRepository.getInstance();
        mFeedTripPosts = mTripCardWrapperRepo.getFeedTripPosts();

    }

    public LiveData<List<TripCardWrapper>> getFeedTripPosts(){
        return mFeedTripPosts;
    }

}