package com.foltran.mermaid.ui.map_box;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.StrictMode;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import com.foltran.mermaid.App;
import com.foltran.mermaid.MainActivity;
import com.foltran.mermaid.R;
import com.foltran.mermaid.adapter.item_mover.OnStartDragListener;
import com.foltran.mermaid.adapter.item_mover.RecyclerListAdapter;
import com.foltran.mermaid.adapter.item_mover.SimpleItemTouchHelperCallback;
import com.foltran.mermaid.database.requests.LocalLocationTrie;
import com.foltran.mermaid.model.location.Location;
import com.foltran.mermaid.model.trip.MapCard;
import com.foltran.mermaid.model.trip.TripCard;
import com.foltran.mermaid.ui.new_trip.NewTripViewModel;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
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

/**
 * A simple {@link Fragment} subclass.
 */
public class EditTripMapFragment extends Fragment implements OnMapReadyCallback, MapboxMap.OnMapClickListener, OnStartDragListener {

    private MapView mapView;
    private MapboxMap mapboxMap;

    private ImageView addLocationMarkerBtn;
    private EditText addLocationEdit;
    private LinearLayout searchResults;

    private LinearLayout reorderMarkersBtn;
    ItemTouchHelper mItemTouchHelper;

    LocalLocationTrie locationSearch;
    private Boolean isLocationSearchOpen = false;
    private int MAX_RESULTS = 5;

    private LatLng currentPosition = new LatLng(41.8, -87.6);
    private GeoJsonSource geoJsonSource;

    private ValueAnimator animator;

    private Bitmap mapPinpoint;
    CameraPosition initialPosition;
    int INITIAL_ZOOM = 8;

    BottomNavigationView navBar;

    NewTripViewModel model;

    public EditTripMapFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle bundle){
        super.onCreate(bundle);

        model = ViewModelProviders.of(requireActivity()).get(NewTripViewModel.class);

        model.curDisplayedCardLiveData().observe(this, new Observer<TripCard>() {
            @Override
            public void onChanged(TripCard card) {

                if (geoJsonSource != null) {
                    geoJsonSource.setGeoJson(FeatureCollection.fromFeatures(((MapCard) card).getMarkers()));
                }
            }
        });

        Mapbox.getInstance(getContext(), getString(R.string.mapbox_access_token));


        navBar = getActivity().findViewById(R.id.nav_view);
        locationSearch = new LocalLocationTrie();


        Drawable drawable = ResourcesCompat.getDrawable(getResources(), R.drawable.ic_location_on_red_highlight_24dp, null);
        mapPinpoint = BitmapUtils.getBitmapFromDrawable(drawable);

        initialPosition = new CameraPosition.Builder()
                .target(currentPosition)
                .zoom(INITIAL_ZOOM)
                .build();

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.fragment_edit_trip_map, container, false);

        ((MainActivity) getActivity()).setContainerFitWindow(false);
        navBar.setVisibility(View.GONE);

        mapView = root.findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);

        mapView.getMapAsync(this);

        addLocationEdit = root.findViewById(R.id.add_location_edit);
        addLocationMarkerBtn = root.findViewById(R.id.add_location_btn);
        searchResults = root.findViewById(R.id.search_results);

        reorderMarkersBtn = root.findViewById(R.id.reorder_trip);

        setupClickers(inflater, container);

        return root;
    }

    @Override
    public void onMapReady(@NonNull final MapboxMap mapboxMap) {
        this.mapboxMap = mapboxMap;

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
                mapboxMap.addOnMapClickListener(EditTripMapFragment.this);
            }
        });


        // To account for new security measures regarding file management that were released with Android Nougat.
        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());
    }

    @Override
    public boolean onMapClick(@NonNull LatLng point) {
        return true;
    }

    /**
     * Handles the manage cards popup movement of items
     * @param viewHolder The holder of the view to drag.
     */
    @Override
    public void onStartDrag(RecyclerView.ViewHolder viewHolder) {
        mItemTouchHelper.startDrag(viewHolder);
    }

    private void setupClickers(final LayoutInflater inflater, final ViewGroup container){

        addLocationEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

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
                    searchResults.setVisibility(View.VISIBLE);
                }
                else{
                    addLocationEdit.setVisibility(View.GONE);
                    addLocationMarkerBtn.setImageDrawable(getResources().getDrawable(R.drawable.ic_add_black_48dp));
                    searchResults.removeAllViews();
                    searchResults.setVisibility(View.GONE);
                    addLocationEdit.setText("");
                }
                isLocationSearchOpen = !isLocationSearchOpen;

            }
        });

        reorderMarkersBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setupMarkerReorderPopup(inflater, container);
            }
        });
    }

    private void setupMarkerReorderPopup(LayoutInflater inflater, ViewGroup container){

        View popupView = inflater.inflate(R.layout.edit_trip_map_reorder_markers_popup, null);

        List<String> labels = new ArrayList<>();
        List<Drawable> icons = new ArrayList<>();

        MapCard card = (MapCard) model.curDisplayedCard();

        for (Location location : card.getLocations()){

            icons.add(App.getContext().getResources().getDrawable(R.drawable.mermaid_tail_transparent));
            labels.add(location.getLocationName());
        }

        RecyclerListAdapter adapter = new RecyclerListAdapter(getActivity(), null,
                this, labels, null, icons, false);

        RecyclerView recyclerView = popupView.findViewById(R.id.reorder_cards_recycler);
        recyclerView.setHasFixedSize(true);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        ItemTouchHelper.Callback callback = new SimpleItemTouchHelperCallback(adapter);
        mItemTouchHelper = new ItemTouchHelper(callback);
        mItemTouchHelper.attachToRecyclerView(recyclerView);

        int width = LinearLayout.LayoutParams.WRAP_CONTENT;
        int height = LinearLayout.LayoutParams.WRAP_CONTENT;
        boolean focusable = true; // lets taps outside the popup also dismiss it
        PopupWindow manageMarkersPopup = new PopupWindow(popupView, width, height, focusable);

        manageMarkersPopup.showAtLocation(container, Gravity.CENTER, 0, 0);

        manageMarkersPopup.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                //backgroundWrapper.setAlpha(1f);
            }
        });

    }

    private void buildSearchResult(final String result){

        TextView resultView = new TextView(getContext());
        LinearLayout.LayoutParams params=new LinearLayout.LayoutParams
                (ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);
        resultView.setLayoutParams(params);

        resultView.setText(result);

        resultView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addLocationEdit.setText(result);
                searchResults.removeAllViews();

                model.addLocationToWorkingMap(result + ", usa", 0, model.curDisplayedCardIndex());
            }
        });

        searchResults.addView(resultView);

    }

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
        ((MainActivity) getActivity()).setContainerFitWindow(true);
        navBar.setVisibility(View.GONE);
    }

    @Override
    public void onStart() {
        super.onStart();
        mapView.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
        mapView.onStop();
        ((MainActivity) getActivity()).setContainerFitWindow(true);
        navBar.setVisibility(View.VISIBLE);
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
    public void onDestroy() {
        super.onDestroy();
        if (animator != null) {
            animator.cancel();
        }
        if (mapboxMap != null) {
            mapboxMap.removeOnMapClickListener(this);
        }
        mapView.onDestroy();
        ((MainActivity) getActivity()).setContainerFitWindow(true);
        navBar.setVisibility(View.VISIBLE);
    }
}
