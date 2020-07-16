package com.foltran.mermaid.model.trip;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * This class is a wrapper for Trip cards and composes the entire trip
 */
public class TripCardWrapper implements Serializable {

    private String tripTitle;
    private List<TripCard> cards;

    private int curIndexDisplayed;  //used when editing a trip, which card is showing

    public TripCardWrapper(){
        cards = new ArrayList<>();
    }

    public void setTripTitle(String s){tripTitle = s;}

    public String getTripTitle(){return tripTitle; }

    public List<TripCard> getAllCards(){
        return cards;
    }

    /**
     * This method is used when FeedCardSerializable is the first card of trip. That is, when
     * trip is retrieved from database and images are given as paths in Google Firebase storage
     * @return List with the paths for images in feedCard
     */
    public List<String> getImagePaths(){

        if (cards == null || cards.size() == 0) return null;

        TripCard firstCard = cards.get(0);
        if (!(firstCard instanceof FeedCardSerializable)) return null;

        FeedCardSerializable feedCard = (FeedCardSerializable) firstCard;

        return feedCard.imagePaths;
    }

    /**
     * This method is equivalent to getImagePaths but for the text
     * @return List with the shortTexts for feedCard
     */
    public List<String> getShortTexts(){

        if (cards == null || cards.size() == 0) return null;

        TripCard firstCard = cards.get(0);
        if (!(firstCard instanceof FeedCardSerializable)) return null;

        FeedCardSerializable feedCard = (FeedCardSerializable) firstCard;

        return feedCard.shortTexts;
    }

    public List<TripCard> getSerializableCardsWithoutUris(){

        List<TripCard> out = new ArrayList<>(cards);
        FeedCard feedCard = (FeedCard) cards.get(0);
        out.set(0, feedCard.getSerializable());

        return out;
    }

    public TripCard addCard(TripCard card){
        this.cards.add(card);
        return card;
    }

    public void removeCard(int index){
        cards.remove(index);
        curIndexDisplayed = Math.min(curIndexDisplayed, cards.size() - 1);
    }

    public void moveCards(int from, int to){
        Collections.swap(cards, from, to);
        if (curIndexDisplayed == from){
            curIndexDisplayed = to;
        }
        else if (curIndexDisplayed == to){
            curIndexDisplayed = from;
        }
    }

    public String getCardTypeAt(int index){
        return this.cards.get(index).getType();
    }

    public TripCard getCardAt(int index){ return this.cards.get(index); }

    public String getCurCardType(){
        return this.cards.get(curIndexDisplayed).getType();
    }

    public TripCard getCurDisplayedCard(){
        return this.cards.get(curIndexDisplayed);
    }

    public void setCurIndexDisplayed(int index){
        curIndexDisplayed=index;
    }

    public Boolean curDisplayedNeedsPager(){
        return this.cards.get(curIndexDisplayed).needsPager;
    }

    public int getCurIndexDisplayed(){return curIndexDisplayed; }

    public int getNumCards(){ return cards.size(); }
}
