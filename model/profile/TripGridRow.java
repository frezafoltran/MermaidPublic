package com.foltran.mermaid.model.profile;

import com.foltran.mermaid.model.trip.FeedCardSerializable;
import com.foltran.mermaid.model.trip.ShortTextCard;
import com.foltran.mermaid.model.trip.TripCardWrapper;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TripGridRow {

    int MAX_PER_ROW = 2;
    int curNumRows = 0;

    // variables for display in grid
    private ArrayList<String>[] images = new ArrayList[MAX_PER_ROW];
    private String[] titles = new String[MAX_PER_ROW];

    //all unparsed info for trip
    private DocumentSnapshot[] trips = new DocumentSnapshot[MAX_PER_ROW];

    public TripGridRow(){}

    public void addEntry(List<String> images, String title, DocumentSnapshot result){
        if (curNumRows < MAX_PER_ROW){

            this.images[curNumRows] = new ArrayList<>(images);

            this.titles[curNumRows] = title;
            this.trips[curNumRows] = result;
        }
        curNumRows++;
    }

    /**
     * Seriliazes the trip document from trip_posts from Firebase into a TripCardWrapper object.
     * This object is passed in a Bundle to the TripFragment for display
     * @param col the column of grid wanted
     * @return curTrip, a serialized TripCardWrapper object
     */
    public TripCardWrapper getTripAt(int col){
        if (col > curNumRows) return null;

        TripCardWrapper curTrip = new TripCardWrapper();

        DocumentSnapshot curDoc = trips[col];
        curTrip.setTripTitle((String) curDoc.get("title"));
        for (Map<String, Object> card : (List<Map<String, Object>>) curDoc.get("cards")){

            if (card == null) continue;

            if (card.get("type").equals("feedCard")) {
                List<String> shortTexts = (List<String>) card.get("shortText");
                curTrip.addCard(new FeedCardSerializable(shortTexts, images[col]));
            }
            else if (card.get("type").equals("shortTextCard")){
                curTrip.addCard(new ShortTextCard(card));
            }

        }


        return curTrip;
    }

    public int getRowSize(){ return curNumRows; }

    public String getImageAt(int col, int imageIndex) {
        if (col > curNumRows) return null;
        return images[col].get(imageIndex);
    }

    public String getTitleAt(int i) {
        if (i > curNumRows) return null;
        return titles[i];
    }
}
