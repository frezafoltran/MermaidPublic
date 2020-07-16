package com.foltran.mermaid.ui.map_box;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.res.ResourcesCompat;
import androidx.lifecycle.ViewModelProviders;

import android.animation.ObjectAnimator;
import android.animation.TypeEvaluator;
import android.animation.ValueAnimator;
import android.app.ProgressDialog;
import android.graphics.Bitmap;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.StrictMode;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.foltran.mermaid.R;
import com.foltran.mermaid.database.requests.LocalLocationTrie;
import com.foltran.mermaid.model.trip.MapCard;
import com.foltran.mermaid.ui.new_trip.NewTripViewModel;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.geometry.LatLngBounds;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.maps.Style;

import com.mapbox.mapboxsdk.style.layers.PropertyFactory;
import com.mapbox.mapboxsdk.style.layers.SymbolLayer;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;
import com.mapbox.mapboxsdk.utils.BitmapUtils;

import java.util.ArrayList;
import java.util.List;


public class EditTripMapActivity extends AppCompatActivity implements
        OnMapReadyCallback, MapboxMap.OnMapClickListener {

    private MapView mapView;
    private MapboxMap mapboxMap;

    private ImageView addLocationMarkerBtn;
    private EditText addLocationEdit;
    private LinearLayout searchResults;

    LocalLocationTrie locationSearch;
    private Boolean isLocationSearchOpen = false;
    private int MAX_RESULTS = 5;

    private LatLng currentPosition = new LatLng(41.8, -87.6);
    private LatLngBounds currentPositionBounds;

    private List<Feature> markers = new ArrayList<>();
    private GeoJsonSource geoJsonSource;

    private ValueAnimator animator;

    private  Bitmap mapPinpoint;
    CameraPosition initialPosition;
    int INITIAL_ZOOM = 8;

    NewTripViewModel model;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        model = ViewModelProviders.of(this).get(NewTripViewModel.class);

        Mapbox.getInstance(this, getString(R.string.mapbox_access_token));
        setContentView(R.layout.activity_edit_trip_map);

        locationSearch = new LocalLocationTrie();

        mapView = findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);

        addLocationEdit = findViewById(R.id.add_location_edit);
        addLocationMarkerBtn = findViewById(R.id.add_location_btn);
        searchResults = findViewById(R.id.search_results);

        Drawable drawable = ResourcesCompat.getDrawable(getResources(), R.drawable.ic_location_on_red_highlight_24dp, null);
        mapPinpoint = BitmapUtils.getBitmapFromDrawable(drawable);

        initialPosition = new CameraPosition.Builder()
                .target(currentPosition)
                .zoom(INITIAL_ZOOM)
                .build();

        mapView.getMapAsync(this);

        setupClickers();
    }

    @Override
    public void onMapReady(@NonNull final MapboxMap mapboxMap) {
        this.mapboxMap = mapboxMap;

        /*
        if (currentPositionBounds == null) {
            mapboxMap.setCameraPosition(initialPosition);
            markers.add(Feature.fromGeometry(
                    Point.fromLngLat(currentPosition.getLongitude(), currentPosition.getLatitude())));
        }
        else {
            mapboxMap.setCameraPosition(mapboxMap.getCameraForLatLngBounds(currentPositionBounds));
        }

         */

        final MapCard card = (MapCard) model.curDisplayedCard();
        mapboxMap.setCameraPosition(mapboxMap.getCameraForLatLngBounds(card.getCurrentPositionBounds()));


        mapboxMap.setStyle(Style.MAPBOX_STREETS, new Style.OnStyleLoaded() {
            @Override
            public void onStyleLoaded(@NonNull Style style) {

                style.addImage(("marker_icon"), mapPinpoint);
                geoJsonSource = new GeoJsonSource("source-id",
                        FeatureCollection.fromFeatures(card.getMarkers()));

                style.addSource(geoJsonSource);

                style.addLayer(
                    new SymbolLayer("layer-id", "source-id")
                            .withProperties(
                                    PropertyFactory.iconImage("marker_icon"),
                                    PropertyFactory.iconIgnorePlacement(true),
                                    PropertyFactory.iconAllowOverlap(true)
                            )
                );

                //Toast.makeText(EditTripMapActivity.this, "TODO", Toast.LENGTH_LONG).show();

                mapboxMap.addOnMapClickListener(EditTripMapActivity.this);
            }
        });


        // To account for new security measures regarding file management that were released with Android Nougat.
        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());
    }

    @Override
    public boolean onMapClick(@NonNull LatLng point) {

        // When the user clicks on the map, we want to animate the marker to that
        // location.
        if (animator != null && animator.isStarted()) {
            currentPosition = (LatLng) animator.getAnimatedValue();
            animator.cancel();
        }

        animator = ObjectAnimator
                .ofObject(latLngEvaluator, currentPosition, point)
                .setDuration(2000);
        animator.addUpdateListener(animatorUpdateListener);
        animator.start();

        currentPosition = point;
        return true;
    }

    private void setupClickers(){

        addLocationEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {


                List<String> possibleMatches = locationSearch.getMatches(s.toString().toLowerCase());

                if (possibleMatches == null) return;

                searchResults.removeAllViews();

                for (int i = 0; i < possibleMatches.size(); i++) {

                    if (i > MAX_RESULTS) break;
                    buildSearchResult(possibleMatches.get(i));
                }
            }
        });

        addLocationMarkerBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (!isLocationSearchOpen) {
                    addLocationEdit.setVisibility(View.VISIBLE);
                    addLocationMarkerBtn.setImageDrawable(getResources().getDrawable(R.drawable.ic_crop_square_black_24dp));
                }
                else{
                    addLocationEdit.setVisibility(View.GONE);
                    addLocationMarkerBtn.setImageDrawable(getResources().getDrawable(R.drawable.ic_add_black_48dp));
                    searchResults.removeAllViews();
                    addLocationEdit.setText("");
                }
                isLocationSearchOpen = !isLocationSearchOpen;

                /*
                markers.add(Feature.fromGeometry(
                        Point.fromLngLat(currentPosition.getLongitude() + (Math.random() * 10),
                                currentPosition.getLatitude() * (Math.random() * 10))));

                 */

                //geoJsonSource.setGeoJson(FeatureCollection.fromFeatures(markers));
            }
        });
    }

    private void buildSearchResult(final String result){

        TextView resultView = new TextView(this);
        LinearLayout.LayoutParams params=new LinearLayout.LayoutParams
                (ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);
        resultView.setLayoutParams(params);

        resultView.setText(result);

        resultView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addLocationEdit.setText(result);
                searchResults.removeAllViews();
            }
        });

        searchResults.addView(resultView);

    }

    private void setupPointsToShow(List<double[]> points){

        List<LatLng> pointsCoord = new ArrayList<>();

        for (double[] point : points){
            pointsCoord.add(new LatLng(point[0], point[1]));

            //String sourceId = "id_" + point[0] + "/" + point[1];
            markers.add(Feature.fromGeometry(Point.fromLngLat(point[1], point[0])));
        }

        currentPositionBounds =  new LatLngBounds.Builder().includes(pointsCoord).build();
    }

    private final ValueAnimator.AnimatorUpdateListener animatorUpdateListener =
            new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator valueAnimator) {
                    LatLng animatedPosition = (LatLng) valueAnimator.getAnimatedValue();
                    //geoJsonSource.setGeoJson(Point.fromLngLat(animatedPosition.getLongitude(), animatedPosition.getLatitude()));
                }
            };

    // Class is used to interpolate the marker animation.
    private static final TypeEvaluator<LatLng> latLngEvaluator = new TypeEvaluator<LatLng>() {

        private final LatLng latLng = new LatLng();

        @Override
        public LatLng evaluate(float fraction, LatLng startValue, LatLng endValue) {
            latLng.setLatitude(startValue.getLatitude()
                    + ((endValue.getLatitude() - startValue.getLatitude()) * fraction));
            latLng.setLongitude(startValue.getLongitude()
                    + ((endValue.getLongitude() - startValue.getLongitude()) * fraction));
            return latLng;
        }
    };

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mapView.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mapView.onStop();
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (animator != null) {
            animator.cancel();
        }
        if (mapboxMap != null) {
            mapboxMap.removeOnMapClickListener(this);
        }
        mapView.onDestroy();
    }
}
