package com.foltran.mermaid.model.trip;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class FeedCardSerializable extends TripCard implements Serializable {

    List<String> shortTexts;
    List<String> imagePaths;

    public FeedCardSerializable(List<String> shortText, List<String> imagePaths){

        this.shortTexts = new ArrayList<>(shortText);
        if (imagePaths != null) {
            this.imagePaths = new ArrayList<>(imagePaths);
        }

        this.setType("feedCard");
        this.setSubType("feedCardNoImages");
        this.needsPager = true;
    }

    public List<String> getShortText(){
        return this.shortTexts;
    }

    public String getShortTextAt(int i){
        return shortTexts.get(i);
    }

    public void setShortText(List<String> shortText){
        this.shortTexts = new ArrayList<>(shortText);
    }

    public String getImagePathAt(int i){
        return imagePaths.get(i);
    }

    public int getNumCards(){ return shortTexts.size(); }
}
