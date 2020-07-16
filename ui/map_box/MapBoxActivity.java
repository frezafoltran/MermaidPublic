package com.foltran.mermaid.ui.map_box;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;

import android.animation.ObjectAnimator;
import android.animation.TypeEvaluator;
import android.animation.ValueAnimator;
import android.app.ProgressDialog;
import android.graphics.Bitmap;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.StrictMode;
import android.view.View;
import android.widget.Toast;

import com.foltran.mermaid.R;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.geometry.LatLngBounds;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.maps.Style;
import com.mapbox.mapboxsdk.snapshotter.MapSnapshot;
import com.mapbox.mapboxsdk.snapshotter.MapSnapshotter;
import com.mapbox.mapboxsdk.style.layers.PropertyFactory;
import com.mapbox.mapboxsdk.style.layers.SymbolLayer;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;
import com.mapbox.mapboxsdk.utils.BitmapUtils;

import java.io.ByteArrayOutputStream;


public class MapBoxActivity extends AppCompatActivity implements
        OnMapReadyCallback, MapboxMap.OnMapClickListener {

    private MapView mapView;
    private MapboxMap mapboxMap;
    private MapSnapshotter mapSnapshotter;
    private FloatingActionButton cameraFab;
    private boolean hasStartedSnapshotGeneration;


    private LatLng currentPosition = new LatLng(41.8, -87.6);
    private GeoJsonSource geoJsonSource;
    private ValueAnimator animator;

    private  Bitmap mapPinpoint;
    CameraPosition initialPosition;
    int INITIAL_ZOOM = 8;

    private FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
    FirebaseStorage storage = FirebaseStorage.getInstance();
    StorageReference storageReference = storage.getReference();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Mapbox access token is configured here. This needs to be called either in your application
        // object or in the same activity which contains the mapview.
        Mapbox.getInstance(this, getString(R.string.mapbox_access_token));

        // This contains the MapView in XML and needs to be called after the access token is configured.
        setContentView(R.layout.activity_map_box);

        cameraFab = findViewById(R.id.camera_share_snapshot_image_fab);
        cameraFab.setImageResource(R.drawable.mermaid_tail_transparent);

        hasStartedSnapshotGeneration = false;

        mapView = findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);

        Drawable drawable = ResourcesCompat.getDrawable(getResources(), R.drawable.ic_location_on_red_highlight_24dp, null);
        mapPinpoint = BitmapUtils.getBitmapFromDrawable(drawable);

        initialPosition = new CameraPosition.Builder()
                .target(currentPosition)
                .zoom(INITIAL_ZOOM)
                .build();

        // Set a callback for when MapboxMap is ready to be used
        mapView.getMapAsync(this);
    }

    @Override
    public void onMapReady(@NonNull final MapboxMap mapboxMap) {
        this.mapboxMap = mapboxMap;

        mapboxMap.setCameraPosition(initialPosition);

        geoJsonSource = new GeoJsonSource("source-id",
                Feature.fromGeometry(Point.fromLngLat(currentPosition.getLongitude(),
                        currentPosition.getLatitude())));

        mapboxMap.setStyle(Style.MAPBOX_STREETS, new Style.OnStyleLoaded() {
            @Override
            public void onStyleLoaded(@NonNull Style style) {


                style.addImage(("marker_icon"), mapPinpoint);
                style.addSource(geoJsonSource);

                style.addLayer(new SymbolLayer("layer-id", "source-id")
                        .withProperties(
                                PropertyFactory.iconImage("marker_icon"),
                                PropertyFactory.iconIgnorePlacement(true),
                                PropertyFactory.iconAllowOverlap(true)
                        ));

                Toast.makeText(MapBoxActivity.this, "TODO", Toast.LENGTH_LONG).show();

                mapboxMap.addOnMapClickListener(MapBoxActivity.this);

                // When user clicks the map, start the snapshotting process with the given parameters
                cameraFab.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (!hasStartedSnapshotGeneration) {
                            hasStartedSnapshotGeneration = true;
                            Toast.makeText(MapBoxActivity.this, "loading", Toast.LENGTH_LONG).show();
                            startSnapShot(
                                    mapboxMap.getProjection().getVisibleRegion().latLngBounds,
                                    mapView.getMeasuredHeight(),
                                    mapView.getMeasuredWidth());
                        }
                    }
                });
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

    private final ValueAnimator.AnimatorUpdateListener animatorUpdateListener =
            new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator valueAnimator) {
                    LatLng animatedPosition = (LatLng) valueAnimator.getAnimatedValue();
                    geoJsonSource.setGeoJson(Point.fromLngLat(animatedPosition.getLongitude(), animatedPosition.getLatitude()));
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



    /**
     * Creates bitmap from given parameters, and creates a notification with that bitmap
     *
     * @param latLngBounds of map
     * @param height       of map
     * @param width        of map
     */
    private void startSnapShot(final LatLngBounds latLngBounds, final int height, final int width) {
        mapboxMap.getStyle(new Style.OnStyleLoaded() {
            @Override
            public void onStyleLoaded(@NonNull Style style) {
                if (mapSnapshotter == null) {
                    // Initialize snapshotter with map dimensions and given bounds
                    MapSnapshotter.Options options =
                            new MapSnapshotter.Options(width, height)
                                    .withRegion(latLngBounds)
                                    .withCameraPosition(mapboxMap.getCameraPosition())
                                    .withLogo(false)
                                    .withStyle(style.getUri());

                    mapSnapshotter = new MapSnapshotter(MapBoxActivity.this, options);
                } else {
                    // Reuse pre-existing MapSnapshotter instance
                    mapSnapshotter.setSize(width, height);
                    mapSnapshotter.setRegion(latLngBounds);
                    mapSnapshotter.setCameraPosition(mapboxMap.getCameraPosition());
                }

                mapSnapshotter.start(new MapSnapshotter.SnapshotReadyCallback() {
                    @Override
                    public void onSnapshotReady(MapSnapshot snapshot) {

                        Bitmap bitmapOfMapSnapshotImage = snapshot.getBitmap();

                        uploadImage(bitmapOfMapSnapshotImage, "map_preview");

                        hasStartedSnapshotGeneration = false;
                    }
                });
            }
        });
    }


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
        // Make sure to stop the snapshotter on pause if it exists
        if (mapSnapshotter != null) {
            mapSnapshotter.cancel();
        }
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

    private void uploadImage(Bitmap bitmap, String imageLabel) {

        if(bitmap != null && !user.isAnonymous()) {
            final ProgressDialog progressDialog = new ProgressDialog(this);
            progressDialog.setTitle("Uploading...");
            progressDialog.show();

            StorageReference ref = storageReference.child("images/map_screenshots/" + user.getEmail() + "/" + imageLabel);

            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 50, stream);
            byte[] byteArray = stream.toByteArray();

            ref.putBytes(byteArray)
                    .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            progressDialog.dismiss();
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            progressDialog.dismiss();
                        }
                    })
                    .addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                            double progress = (100.0*taskSnapshot.getBytesTransferred()/taskSnapshot
                                    .getTotalByteCount());
                            progressDialog.setMessage("Uploaded "+(int)progress + "%");
                        }
                    });

        }
    }
}
