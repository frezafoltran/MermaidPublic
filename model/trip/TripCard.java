package com.foltran.mermaid.model.trip;

import android.graphics.Bitmap;
import android.net.Uri;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/*
This class is a model to build trips. An instance of TripCard is a building block of a Trip.
 */

public class TripCard {

    private String type;
    private String subType = "";    //used to distinguish feedCard from serializable feedCard
    public Boolean needsPager;
    private Boolean isEditable;

    public TripCard(){}

    public Boolean requiresPager(){return type.equals("feedCard");}

    public Boolean getIsEditable(){return isEditable;}
    public void setIsEditable(Boolean flag){this.isEditable = flag;}

    public String getType(){
        return type;
    }

    public void setType(String type){ this.type = type;}

    public String getSubType(){
        return subType;
    }

    public void setSubType(String subType){ this.subType = subType;}

}

