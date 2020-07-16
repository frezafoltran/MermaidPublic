package com.foltran.mermaid.model.location;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldPath;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Location implements Serializable {

    private String imageFieldPath = FieldPath.of(new String[]{"image"}).toString();

    private String queryName;
    private String locationName;
    private String stateName;
    private String countryName;

    private List<String> locationImagePaths;
    private String stockImage;

    private List<String> avgTemps;
    private List<String> avgSnow;
    private List<String> avgPrec;

    private Long population;
    private String elevation;
    private String closestLocation;
    private String[] coordinates;
    private int distToUser;

    private List<Integer> cityParks;
    private List<Map<String, Object>> nationalParks;
    private Map<String, Object> historicPlacesIds;
    private Map<String, Object> museums;
    private Double similarity;

    private String[] topMatchAttrs = new String[3];

    private Map<String, Map<String, Object>> ratingsDisplay;
    private List<Map<String, Object>> parsedRatings;
    private Map<String, Double> attrRatings;

    private String[] attrLabels = new String[]{
            "food_diversity", "food_price", "nightlife", "walkability",
            "history", "art", "weather_hot", "weather_cold",
            "monuments", "water", "mountain", "rock_formation",
            "forest", "parks", "hiking", "technology",
            "children", "commerce", "sport", "religion",
            "hotel"};

    public Location(String queryName, double similarity){

        this.locationImagePaths = new ArrayList<>();

        String[] queryParts = queryName.split(", ");

        locationName = queryParts[0];
        stateName = queryParts[1];
        countryName = queryParts[2];
        this.queryName= locationName + ", " + stateName;
        this.similarity = similarity;

    }

    public void addTopMatchRatingsWithUser(List<Double> curRatings){

        if (parsedRatings == null) getRatings();

        List<Map<String, Object>> ratingsDiffRelativeToUser = new ArrayList<>();

        for (int attrIndex = 0; attrIndex < curRatings.size(); attrIndex++){

            String curAttrName = attrLabels[attrIndex];

            // if location does not have rating as top rating, skip
            if (!attrRatings.keySet().contains(curAttrName)) continue;

            Double userRating = curRatings.get(attrIndex);

            Map<String, Object> potentialMatch = new HashMap<>();
            potentialMatch.put("attrName", curAttrName);
            potentialMatch.put("diffVal", Math.abs(userRating - attrRatings.get(curAttrName)));
            ratingsDiffRelativeToUser.add(potentialMatch);
        }

        Collections.sort(ratingsDiffRelativeToUser, new SortByAttrSimilarity());

        for (int i = 0; i < Math.min(3, ratingsDiffRelativeToUser.size()); i++){
            topMatchAttrs[i] = (String) ratingsDiffRelativeToUser.get(i).get("attrName");
        }
    }

    public String getTopMatch(int i){
        if (i > 2 || topMatchAttrs[i] == null) return "";
        return topMatchAttrs[i];
    }

    public Double getSimilarity(){ return similarity; }

    public String getLocationName() {
        return locationName;
    }

    public String getStateName() {
        return stateName;
    }

    public Double getLat(){
        return Double.valueOf(coordinates[0]);
    }

    public Double getLon(){
        return Double.valueOf(coordinates[1]);
    }

    public void setDistToUser(int val){
        this.distToUser = val;
    }

    public int getDistToUser(){
        return distToUser;
    }

    public List<String> getAvgTemps(){ return avgTemps;}

    public List<String> getAvgSnow(){ return avgSnow;}

    public List<String> getAvgPrec(){ return avgPrec;}

    public Long getPopulation(){ return population;}

    public String getElevation(){ return elevation;}

    public String getClosestLocation(){ return closestLocation; }

    public List<Map<String, Object>> getRatings(){

        if (parsedRatings != null) return parsedRatings;

        attrRatings = new HashMap<>();

        List<Map<String, Object>> out = new ArrayList<>();

        for (String attr : ratingsDisplay.keySet()){

            Map<String, Object> curItem = ratingsDisplay.get(attr);

            attrRatings.put(attr, (double) curItem.get("parent_rating"));

            Map<String, Object> ratingsInfo = new HashMap<>();
            ratingsInfo.put("attr", attr);
            ratingsInfo.put("attrScore", (double) curItem.get("parent_rating") * 100);
            ratingsInfo.put("subAttr", curItem.get("label"));
            ratingsInfo.put("subAttrLabelScore", curItem.get("display_label"));

            out.add(ratingsInfo);
        }

        this.parsedRatings = new ArrayList<>(out);
        return out;
    }

    public String getQueryName() {
        return this.queryName;
    }

    public int getNumCityParks(){
        if (cityParks == null) return 0;
        return cityParks.size();
    }

    public Integer getNumHistoricPlaces(){

        if (historicPlacesIds == null) return 0;

        List<Integer> ids = (List<Integer>) historicPlacesIds.get("ids");
        return ids.size();
    }

    public Map<String, Integer> getMuseumProfile(){

        if (museums == null) return null;

        Map<String, Integer> museumByType = (Map<String, Integer>) museums.get("stats");
        Map<String, Integer> out = new HashMap<>();

        double total = 0;
        for (Map.Entry elem : museumByType.entrySet()) {

            total += ((Number) elem.getValue()).doubleValue();
        }
        for (Map.Entry elem : museumByType.entrySet()) {
            String type = (String)elem.getKey();

            double cur = ((Number) elem.getValue()).doubleValue();

            double percent = 100 * cur/total;
            out.put(type, (int) percent);
        }

        return out;
    }

    public int getMuseumCount(){

        if (museums == null) return 0;

        List<Long> museumIds = ((List<Long>) museums.get("ids"));
        Long num1Long = (Long) museums.get("geoname_count");
        int num1 = num1Long.intValue();
        int num2 = museumIds.size();

        return Math.max(num1, num2);
    }

    public List<Map<String, Object>> getNationalParks(){return this.nationalParks; }

    public String getStockImagePath(){
        return "wiki_images/locations/" + countryName +
                "/" + stateName + "/" + this.stockImage;
    }

    private FieldPath getWeatherPath(String param){
        return FieldPath.of("climate", param);
    }

    private String getPopulationPath(){
        return FieldPath.of("basic_info", "population", "total").toString();
    }

    private String getCoordinatePath(){
        return FieldPath.of("basic_info", "coordinates").toString();
    }

    private String getClosestLocationPath(){
        return FieldPath.of("basic_info", "closest_location", "location").toString();
    }

    private String getElevationPath(){
        return FieldPath.of("basic_info", "elevation").toString();
    }

    private FieldPath getParkPath(String parkType){return FieldPath.of("parks", parkType);}

    private FieldPath getInfoPath(String pathEndPoint){ return FieldPath.of(pathEndPoint); }

    /**
     * This method receives a document from Firebase corresponding to a location
     * and parses only the data needed to display the location in a feed.
     *
     * @param document document from collection of locations
     */
    public void addFeedAttrs(DocumentSnapshot document){

        this.stockImage = document.getString(imageFieldPath).replace("/", "!slash_!");
        ratingsDisplay = (Map<String, Map<String, Object>>) document.get("ratings_display");

        coordinates = document.getString(getCoordinatePath()).split(", ");

    }

    public void addFirebaseAttrs(DocumentSnapshot document){

        //upload climate info
        avgTemps = (List<String>) document.get(getWeatherPath("avg_temp"));
        avgSnow = (List<String>) document.get(getWeatherPath("avg_snow_days"));
        avgPrec = (List<String>) document.get(getWeatherPath("avg_precipitation_days"));

        //upload basic info
        population = document.getLong(getPopulationPath());
        elevation = document.getString(getElevationPath());

        closestLocation = document.getString(getClosestLocationPath());

        //upload park info
        cityParks = (List<Integer>) document.get(getParkPath("city"));
        nationalParks = (List<Map<String, Object>>) document.get(getParkPath("national"));

        //historic places info
        historicPlacesIds = (Map<String, Object>) document.get(getInfoPath("historic_places"));

        museums = (Map<String, Object>) document.get(getInfoPath("museums"));

    }


    class SortByAttrSimilarity implements Comparator<Map<String, Object>> {

        public int compare(Map<String, Object> a, Map<String, Object> b){

            Double diff = (double) a.get("diffVal") - (double) b.get("diffVal");

            if (diff == 0) return 0;
            else if (diff > 0) return 1;
            else return -1;
        }
    }
}
