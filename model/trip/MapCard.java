package com.foltran.mermaid.model.trip;

import android.util.Log;

import com.foltran.mermaid.App;
import com.foltran.mermaid.R;
import com.foltran.mermaid.model.location.Location;
import com.mapbox.api.staticmap.v1.MapboxStaticMap;
import com.mapbox.api.staticmap.v1.StaticMapCriteria;
import com.mapbox.api.staticmap.v1.models.StaticMarkerAnnotation;
import com.mapbox.api.staticmap.v1.models.StaticPolylineAnnotation;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.Point;
import com.mapbox.geojson.utils.PolylineUtils;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.geometry.LatLngBounds;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MapCard extends TripCard {

    private List<Location> locations = new ArrayList<>();

    //Chicago, IL coordinates
    private double DEFAULT_LAT = 41.878;
    private double DEFAULT_LON = -87.629;
    private String[] mapPreviewLabels = new String[]{"a", "b", "c", "d", "e", "f"};

    private MapboxStaticMap mapPreview;

    private LatLngBounds currentPositionBounds;
    private List<LatLng> pointsCoord = new ArrayList<>();
    private List<Feature> markers = new ArrayList<>();

    List<StaticPolylineAnnotation> mapLine;

    private List<LatLng> defaultPointsCoord = new ArrayList<>();
    private List<Feature> defaultMarkers = new ArrayList<>();
    private List<Point> defaultTripPath = new ArrayList<>();
    private List<StaticPolylineAnnotation> defaultMapLine;
    private List<StaticMarkerAnnotation> defaultAllMarkers = new ArrayList<>();

    private List<StaticMarkerAnnotation> allMarkers = new ArrayList<>();
    private List<Point> tripPath = new ArrayList<>();

    public MapCard(){

        this.setType("mapCard");
        this.needsPager = false;

        prepareDefaultMap();
        setMapPreview();
    }

    private void prepareDefaultMap(){

        StaticMarkerAnnotation marker1 = StaticMarkerAnnotation.builder().name(StaticMapCriteria.LARGE_PIN)
                .lnglat(Point.fromLngLat(DEFAULT_LON, DEFAULT_LAT))
                .label(mapPreviewLabels[0])
                .build();

        StaticMarkerAnnotation marker2 = StaticMarkerAnnotation.builder().name(StaticMapCriteria.LARGE_PIN)
                .lnglat(Point.fromLngLat(DEFAULT_LON + 10, DEFAULT_LAT + 5))
                .label(mapPreviewLabels[1])
                .build();

        defaultAllMarkers.add(marker1);
        defaultAllMarkers.add(marker2);

        defaultPointsCoord.add(new LatLng(DEFAULT_LAT, DEFAULT_LON));
        defaultPointsCoord.add(new LatLng(DEFAULT_LAT + 5, DEFAULT_LON + 10));

        defaultMarkers.add(Feature.fromGeometry(Point.fromLngLat(DEFAULT_LON, DEFAULT_LAT)));
        defaultMarkers.add(Feature.fromGeometry(Point.fromLngLat(DEFAULT_LON + 10, DEFAULT_LAT + 5)));

        defaultTripPath.add(Point.fromLngLat(DEFAULT_LON, DEFAULT_LAT));
        defaultTripPath.add(Point.fromLngLat(DEFAULT_LON + 10, DEFAULT_LAT + 5));

        defaultMapLine = new ArrayList<>(Arrays.asList(
                StaticPolylineAnnotation.builder()
                        .polyline(PolylineUtils.encode(defaultTripPath, 5))
                        .build())
        );
    }

    public void addLocationAt(int index, Location location){

        int addAt = Math.min(index, locations.size());
        locations.add(addAt, location);

        StaticMarkerAnnotation marker = StaticMarkerAnnotation.builder().name(StaticMapCriteria.LARGE_PIN)
                .lnglat(Point.fromLngLat(location.getLon(), location.getLat()))
                .label(mapPreviewLabels[index])
                .build();

        allMarkers.add(addAt, marker);
        tripPath.add(addAt, Point.fromLngLat(location.getLon(), location.getLat()));

        pointsCoord.add(addAt, new LatLng(location.getLat(), location.getLon()));
        markers.add(addAt, Feature.fromGeometry(Point.fromLngLat(location.getLon(), location.getLat())));
    }

    public double getMainLat(){
        if (locations.size() == 0) return DEFAULT_LAT;

        return locations.get(0).getLat();
    }

    public double getMainLon(){
        if (locations.size() == 0) return DEFAULT_LON;

        return locations.get(0).getLon();
    }

    public List<Location> getLocations(){
        return locations;
    }

    public void setMapPreview(){
        mapPreview = buildMapPreview();
    }

    public MapboxStaticMap getMapPreview(){
        return mapPreview;
    }

    private MapboxStaticMap defaultMapPreview(){

        currentPositionBounds =  new LatLngBounds.Builder().includes(defaultPointsCoord).build();

        return MapboxStaticMap.builder()
                .accessToken(App.getContext().getString(R.string.mapbox_access_token))
                .styleId(StaticMapCriteria.SATELLITE_STREETS_STYLE)
                .staticPolylineAnnotations(defaultMapLine)
                .staticMarkerAnnotations(defaultAllMarkers)
                .cameraAuto(true)
                .width(300)
                .height(300)
                .retina(true)
                .logo(false)
                .build();
    }

    private MapboxStaticMap buildMapPreview(){

        if (locations.size() < 2) return defaultMapPreview();

        currentPositionBounds =  new LatLngBounds.Builder().includes(pointsCoord).build();

        mapLine = new ArrayList<>(Arrays.asList(
                StaticPolylineAnnotation.builder()
                        .polyline(PolylineUtils.encode(tripPath, 5))
                        .build())
        );

        MapboxStaticMap staticImage = MapboxStaticMap.builder()
                .accessToken(App.getContext().getString(R.string.mapbox_access_token))
                .styleId(StaticMapCriteria.SATELLITE_STREETS_STYLE)
                .staticPolylineAnnotations(mapLine)
                .staticMarkerAnnotations(allMarkers)
                .cameraAuto(true)
                .width(300)
                .height(300)
                .retina(true)
                .logo(false)
                .build();

        return staticImage;
    }

    public List<Feature> getMarkers(){
        return markers;
    }

    public List<StaticPolylineAnnotation> getMapLine(){
        return mapLine;
    }

    public LatLngBounds getCurrentPositionBounds(){
        return currentPositionBounds;
    }

}
