package com.foltran.mermaid.model.trip;

import android.net.Uri;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class FeedCard extends TripCard implements Serializable {

    List<Uri> imageUris = new ArrayList<>();
    List<Float> rotations = new ArrayList<>();
    List<Integer[]> simpleAttrs = new ArrayList<>(); // contrast and brightness

    List<String> bottomTexts = new ArrayList<>();

    public FeedCard(Map<String, Object> params){

        if (params != null) {
            imageUris = new ArrayList<>((List<Uri>) params.get("uris"));

            rotations = new ArrayList<>(Arrays.asList(new Float[imageUris.size()]));
            Collections.fill(rotations, 0f);

            simpleAttrs = new ArrayList<>(Arrays.asList(new Integer[imageUris.size()][2]));
            Collections.fill(simpleAttrs, new Integer[] {1, 0});

            bottomTexts = new ArrayList<>(Arrays.asList(new String[imageUris.size()]));
            Collections.fill(bottomTexts, "");
        }

        this.setType("feedCard");
        this.needsPager = true;
    }

    public List<Uri> getUris(){return imageUris;}

    public Uri getUriAt(int i){return imageUris.get(i);}

    public void addUri(Uri uri){
        if (uri != null) {
            imageUris.add(uri);
            rotations.add(0f);
            simpleAttrs.add(new Integer[]{1, 0});
            bottomTexts.add("");

        }
    }

    public void replaceUri(int index, Uri uri){
        if (uri != null){
            imageUris.set(index, uri);
        }
    }

    public void setRotation(int index, float rotation){
        rotations.set(index, rotation);
    }

    public float getRotation(int index){
        return rotations.get(index);
    }

    public void setSimpleAttrs(int index, Integer[] attrs){
        simpleAttrs.set(index, attrs.clone());
    }

    public Integer[] getSimpleAttrs(int index){
        return simpleAttrs.get(index);
    }

    public String getBottomTextAt(int i){return bottomTexts.get(i); }

    public void setBottomTextAt(int index, String bottomText){this.bottomTexts.set(index, bottomText); }


    public List<Uri> getImageUris(){
        return this.imageUris;
    }

    public List<String> getBottomTexts(){
        return this.bottomTexts;
    }

    public List<Float> getRotations(){
        return rotations;
    }

    public List<Integer[]> getSimpleAttrs(){
        return simpleAttrs;
    }

    public void setImageUris(List<Uri> imageUris){
        this.imageUris = new ArrayList<>(imageUris);
    }

    public void setBottomTexts(List<String> bottomTexts){
        this.bottomTexts = new ArrayList<>(bottomTexts);
    }

    public void setRotations(List<Float> rotations){
        this.rotations = new ArrayList<>(rotations);
    }

    public void setSimpleAttrs(List<Integer[]> simpleAttrs){
        this.simpleAttrs = new ArrayList<>(simpleAttrs);
    }

    public TripCard getSerializable(){

        FeedCardSerializable feedCardSerializable = new FeedCardSerializable(this.bottomTexts, null);

        return feedCardSerializable;
    }

}
