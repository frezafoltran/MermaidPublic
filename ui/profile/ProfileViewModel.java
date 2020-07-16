package com.foltran.mermaid.ui.profile;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.foltran.mermaid.model.profile.TripGridRow;
import com.foltran.mermaid.repositories.UserRepository;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class ProfileViewModel extends ViewModel {


    MutableLiveData<List<Double>> userRatingVector;
    MutableLiveData<String> username;
    MutableLiveData<List<String>> userPostIds;

    MutableLiveData<List<TripGridRow>> userPostFeed;

    UserRepository mUserRepo;

    public void init(){

        // indicates data is cached
        if (userRatingVector != null) return;

        mUserRepo = UserRepository.getInstance();

        userRatingVector = new MutableLiveData<>();
        username = new MutableLiveData<>();
        userPostIds = new MutableLiveData<>();

        userPostFeed = new MutableLiveData<>();
        userPostFeed.setValue(new ArrayList<TripGridRow>());

        mUserRepo.getUserInfo(this);
    }

    LiveData<List<Double>> getUserRatingVector(){
        return userRatingVector;
    }

    LiveData<String> getUsername(){
        return username;
    }

    LiveData<List<String>> getUserPostIds(){
        return userPostIds;
    }

    LiveData<List<TripGridRow>> getUserPostFeed(){
        return userPostFeed;
    }

    public void setUserInfo(DocumentSnapshot document){
        this.userRatingVector.postValue(new ArrayList<>((List<Double>) document.get("ratings")));
        this.userPostIds.postValue(new ArrayList<>((List<String>) document.get("posts")));
        this.username.postValue((String) document.get("username"));
    }

    public void addRowToPostFeed(TripGridRow postRow){
        List<TripGridRow> curRows = userPostFeed.getValue();
        curRows.add(postRow);
        userPostFeed.postValue(curRows);
    }

}
