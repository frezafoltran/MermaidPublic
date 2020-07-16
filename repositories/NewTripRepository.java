package com.foltran.mermaid.repositories;

import com.foltran.mermaid.model.location.Location;
import com.foltran.mermaid.model.profile.TripGridRow;
import com.foltran.mermaid.model.trip.FeedCard;
import com.foltran.mermaid.model.trip.TripCard;
import com.foltran.mermaid.model.trip.TripCardWrapper;
import com.foltran.mermaid.ui.new_trip.NewTripViewModel;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.storage.StorageReference;

import java.util.List;

public class NewTripRepository {

    static NewTripRepository instance;
    NewTripViewModel model;

    public static NewTripRepository getInstance(){
        if (instance == null) instance = new NewTripRepository();

        return  instance;
    }

    private Utils.RepoCallback newTripCallback = new Utils.RepoCallback() {

        @Override
        public void getUserInfoCallback(DocumentSnapshot document) {}

        @Override
        public void getPostObjectCallback(TripGridRow row){ }

        @Override
        public void getUserRecommendedLocationsCallback(List<Location> result){}

        @Override
        public void getLocationCallback(Location location){
            //model.updateCurWorkingMapLocationInfo(location, positionInMap);
        }

        @Override
        public void getMapLocationCallback(Location location, int positionInMap, int mapPositionInTrip){
            model.updateCurWorkingMapLocationInfo(location, positionInMap, mapPositionInTrip);
        }

        @Override
        public void getLocationRatingCallback(List<Location> result){}

        @Override
        public void getUserMapPreviewCallback(StorageReference imgRef){
        }

    };

    public TripCardWrapper initTrip(NewTripViewModel model){

        if (this.model == null && model != null){
            this.model = model;
        }

        TripCardWrapper trip = new TripCardWrapper();
        TripCard feedCard = new FeedCard(null);

        trip.addCard(feedCard);
        trip.setCurIndexDisplayed(0);

        return trip;
    }

    public void getLocationForMap(String locationQueryName, int position, int mapPositionInTrip){

        Location location = new Location(locationQueryName, -1);
        Utils.getMapLocation(newTripCallback, location, position, mapPositionInTrip);

    }
}
