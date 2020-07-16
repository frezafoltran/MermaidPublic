package com.foltran.mermaid.ui.new_trip;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.foltran.mermaid.model.location.Location;
import com.foltran.mermaid.model.trip.FeedCard;
import com.foltran.mermaid.model.trip.MapCard;
import com.foltran.mermaid.model.trip.TripCard;
import com.foltran.mermaid.model.trip.TripCardWrapper;
import com.foltran.mermaid.repositories.NewTripRepository;

import java.util.List;

public class NewTripViewModel extends ViewModel {

   MutableLiveData<TripCardWrapper> workingTrip;
   MutableLiveData<FeedCard> feedCard;

   MutableLiveData<TripCard> curDisplayedCard;
   MutableLiveData<MapCard> preloadedMap;

   NewTripRepository repo;

   public void init(){

       repo = NewTripRepository.getInstance();

       if (workingTrip != null) return;

       workingTrip = new MutableLiveData<>();
       feedCard = new MutableLiveData<>();
       preloadedMap = new MutableLiveData<>();
       curDisplayedCard = new MutableLiveData<>();

       TripCardWrapper newTrip = repo.initTrip(this);
       workingTrip.setValue(newTrip);
       feedCard.setValue((FeedCard) newTrip.getCardAt(0));
       preloadedMap.setValue(new MapCard());
       curDisplayedCard.setValue(null);

   }

   public void updateCurWorkingMapLocationInfo(Location location, int curPositionInMap, int mapPositionInTrip){

       TripCardWrapper trip = workingTrip.getValue();

       MapCard card = (MapCard) trip.getCardAt(mapPositionInTrip);
       card.addLocationAt(curPositionInMap, location);
       card.setMapPreview();

       workingTrip.postValue(trip);
       curDisplayedCard.postValue(workingTrip.getValue().getCurDisplayedCard());
   }

    /**
     *
     * @param locationQueryName name of location to query
     * @param position is the index within the map to add location
     * @param mapPositionInTrip is the index of mapCard within TripWrapper object
     */
   public void addLocationToWorkingMap(String locationQueryName, int position, Integer mapPositionInTrip){

       TripCardWrapper trip = workingTrip.getValue();

       if (position == 0 && mapPositionInTrip == null){
           trip.addCard(new MapCard());
           workingTrip.postValue(trip);
       }

       if (mapPositionInTrip == null){
           mapPositionInTrip = trip.getNumCards() - 1;
       }

       //if (numLocationsToLoad == -1) numLocationsToLoad = totalPositions;

       repo.getLocationForMap(locationQueryName + ", usa", position, mapPositionInTrip);
       /*
       if (numLocationsToLoad == -1) numLocationsToLoad = totalPositions;

       repo.getLocationForMap(locationQueryName + ", usa", position);
        */
   }

   public LiveData<TripCardWrapper> getWorkingTrip(){
        return workingTrip;
   }

   public LiveData<FeedCard> getFeedCard(){
       return feedCard;
   }

   public int curDisplayedCardIndex(){
       return workingTrip.getValue().getCurIndexDisplayed();
   }

   public TripCard getCardAt(int index){
       return workingTrip.getValue().getCardAt(index);
   }

   public TripCard curDisplayedCard(){
        return workingTrip.getValue().getCurDisplayedCard();
    }

    public LiveData<TripCard> curDisplayedCardLiveData(){
        return curDisplayedCard;
    }

   List<TripCard> getAllCards(){
       return workingTrip.getValue().getAllCards();
   }

   int numCards(){
       return workingTrip.getValue().getNumCards();
   }

   void setDisplayedCard(int index){
       TripCardWrapper curTrip = workingTrip.getValue();
       curTrip.setCurIndexDisplayed(index);
       workingTrip.postValue(curTrip);

       curDisplayedCard.postValue(workingTrip.getValue().getCurDisplayedCard());
   }

   void moveCards(int from, int to){
       TripCardWrapper curTrip = workingTrip.getValue();
       curTrip.moveCards(from, to);
       workingTrip.postValue(curTrip);
   }

   void setTripTitle(String s){
       TripCardWrapper curTrip = workingTrip.getValue();
       curTrip.setTripTitle(s);
       workingTrip.postValue(curTrip);
   }

   String curCardType(){
       return workingTrip.getValue().getCurCardType();
   }

   Boolean curCardNeedsPager(){
       return workingTrip.getValue().curDisplayedNeedsPager();
   }

   void addCard(TripCard card){
       TripCardWrapper curTrip = workingTrip.getValue();
       curTrip.addCard(card);
       workingTrip.postValue(curTrip);
   }

   Boolean removeCard(int index){
       TripCardWrapper curTrip = workingTrip.getValue();

       // do not let delete feed card (index 0)
       if (index >= curTrip.getNumCards() || index == 0) return false;

       curTrip.removeCard(index);
       workingTrip.postValue(curTrip);
       return true;
   }

}