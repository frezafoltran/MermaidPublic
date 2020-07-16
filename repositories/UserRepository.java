package com.foltran.mermaid.repositories;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;

import com.foltran.mermaid.R;
import com.foltran.mermaid.model.location.Location;
import com.foltran.mermaid.model.profile.TripGridRow;
import com.foltran.mermaid.ui.profile.ProfileViewModel;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class UserRepository {

    private static UserRepository instance;
    private ProfileViewModel model;

    //all attribute ids. This is used to add ids to xml file
    public static List<Integer> attrIds = new ArrayList<>(Arrays.asList(
            R.id.attr_0,
            R.id.attr_1,
            R.id.attr_2,
            R.id.attr_3,
            R.id.attr_4,
            R.id.attr_5,
            R.id.attr_6,
            R.id.attr_7,
            R.id.attr_8,
            R.id.attr_9,
            R.id.attr_10,
            R.id.attr_11,
            R.id.attr_12,
            R.id.attr_13,
            R.id.attr_14,
            R.id.attr_15,
            R.id.attr_16,
            R.id.attr_17,
            R.id.attr_18,
            R.id.attr_19,
            R.id.attr_20
            ));

    public static UserRepository getInstance(){
        if (instance == null) {
            instance = new UserRepository();
        }
        return instance;
    }

    private Utils.RepoCallback userCallback = new Utils.RepoCallback() {

        @Override
        public void getUserInfoCallback(DocumentSnapshot document) {

            if (model == null) return;

            model.setUserInfo(document);
            Utils.getPostObject(this, new ArrayList<>((List<String>) document.get("posts")), 0);

        }

        @Override
        public void getPostObjectCallback(TripGridRow row){
            model.addRowToPostFeed(row);
        }

        @Override
        public void getUserRecommendedLocationsCallback(List<Location> result){}

        @Override
        public void getLocationCallback(Location location){}

        @Override
        public void getMapLocationCallback(Location location, int positionInMap, int mapPositionInTrip){}

        @Override
        public void getLocationRatingCallback(List<Location> result){}

        @Override
        public void getUserMapPreviewCallback(StorageReference imgRef){
        }

    };

    /**
     * Get all user info needed to build profile page
     * @param model used to store data in MutableLiveData objects
     */
    public void getUserInfo(ProfileViewModel model){

        this.model = model;
        Utils.getUserInfo(userCallback);
    }



    /**
     * Options of actions to perform on existing attributes in profile page
     * Used within AlertDialog when user clicks on attribute
     * @return list of options of actions
     */
    public static CharSequence[] attrPopupOptions(){
        return new CharSequence[]{"delete attribute", "decrease importance"};
    }

    /**
     * Title of AlertDialog when user clicks to manage attribute
     * @param attr attribute clicked
     * @return complete title
     */
    public static String attrPopupTitle(String attr){
        return "Manage " + attr;
    }


    /**
     * Adds all attributes labels to array labels, based on the resources in activity
     * @param labels array where labels are added
     * @param activity location of resources
     */
    public static void addAllAttrLabels(List<String> labels, Activity activity){

        Resources activityResources = activity.getResources();
        labels.add(activityResources.getString(R.string.attr_0));
        labels.add(activityResources.getString(R.string.attr_1));
        labels.add(activityResources.getString(R.string.attr_2));
        labels.add(activityResources.getString(R.string.attr_3));
        labels.add(activityResources.getString(R.string.attr_4));
        labels.add(activityResources.getString(R.string.attr_5));
        labels.add(activityResources.getString(R.string.attr_6));
        labels.add(activityResources.getString(R.string.attr_7));
        labels.add(activityResources.getString(R.string.attr_8));
        labels.add(activityResources.getString(R.string.attr_9));
        labels.add(activityResources.getString(R.string.attr_10));
        labels.add(activityResources.getString(R.string.attr_11));
        labels.add(activityResources.getString(R.string.attr_12));
        labels.add(activityResources.getString(R.string.attr_13));
        labels.add(activityResources.getString(R.string.attr_14));
        labels.add(activityResources.getString(R.string.attr_15));
        labels.add(activityResources.getString(R.string.attr_16));
        labels.add(activityResources.getString(R.string.attr_17));
        labels.add(activityResources.getString(R.string.attr_18));
        labels.add(activityResources.getString(R.string.attr_19));
        labels.add(activityResources.getString(R.string.attr_20));
    }

    /**
     * Adds all attributes symbols to array symbols, based on the resources in context
     * @param symbols array where labels are added
     * @param context location of resources
     */
    public static void addAllSymbols(List<Integer> symbols, Context context){

        String packageName = context.getPackageName();
        Resources res = context.getResources();
        symbols.add(res.getIdentifier("ic_food_diversity_24px", "drawable", packageName));
        symbols.add(res.getIdentifier("ic_food_price_24px", "drawable", packageName));
        symbols.add(res.getIdentifier("ic_nightlife_24px", "drawable", packageName));    //nightlife
        symbols.add(res.getIdentifier("ic_walkability_24px", "drawable", packageName));
        symbols.add(res.getIdentifier("ic_history_24px", "drawable", packageName));
        symbols.add(res.getIdentifier("ic_art_24px", "drawable", packageName));
        symbols.add(res.getIdentifier("ic_wb_sunny_black_24dp", "drawable", packageName));    //weather_hot
        symbols.add(res.getIdentifier("ic_ac_unit_black_24dp", "drawable", packageName));    //weather_cold
        symbols.add(res.getIdentifier("ic_monuments_24dp", "drawable", packageName));    //monuments
        symbols.add(res.getIdentifier("ic_water_24dp", "drawable", packageName));    //water
        symbols.add(res.getIdentifier("ic_mountain_24dp", "drawable", packageName));    //mountain
        symbols.add(res.getIdentifier("mermaid_tail_transparent", "drawable", packageName));    //rock_formation
        symbols.add(res.getIdentifier("ic_nature_24dp", "drawable", packageName));    //forest
        symbols.add(res.getIdentifier("mermaid_tail_transparent", "drawable", packageName));    //parks
        symbols.add(res.getIdentifier("mermaid_tail_transparent", "drawable", packageName));    //hiking
        symbols.add(res.getIdentifier("mermaid_tail_transparent", "drawable", packageName));    //technology
        symbols.add(res.getIdentifier("mermaid_tail_transparent", "drawable", packageName));    //children
        symbols.add(res.getIdentifier("ic_commerce_24dp", "drawable", packageName));    //commerce
        symbols.add(res.getIdentifier("mermaid_tail_transparent", "drawable", packageName));    //sport
        symbols.add(res.getIdentifier("mermaid_tail_transparent", "drawable", packageName));    //religion
        symbols.add(res.getIdentifier("ic_hotel_24dp", "drawable", packageName));    //hotel
    }
}
