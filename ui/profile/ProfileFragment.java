package com.foltran.mermaid.ui.profile;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProviders;
import androidx.palette.graphics.Palette;
import androidx.viewpager.widget.ViewPager;

import android.os.Handler;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.foltran.mermaid.MainActivity;
import com.foltran.mermaid.R;
import com.foltran.mermaid.database.requests.GetUserInfo;
import com.foltran.mermaid.model.profile.TripGridRow;
import com.foltran.mermaid.storage.images.ContrastTransform;
import com.foltran.mermaid.storage.images.LocalImageUtil;
import com.foltran.mermaid.ui.recommendation_feed.RecommendationFeedViewModel;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.functions.HttpsCallableResult;
import com.google.firebase.storage.StorageReference;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static android.app.Activity.RESULT_CANCELED;
import static android.app.Activity.RESULT_OK;


public class ProfileFragment extends Fragment {

    ImageView profileImage;
    TextView profileUsername;
    TextView curUserLocation;

    ProfilePagerAdapter profilePagerAdapter;
    ProfileFragment profileFragment;
    MainActivity activity;
    Bundle savedInstanceState;

    AppBarLayout appBarLayout;
    ViewPager viewPager;
    CollapsingToolbarLayout collapsingToolbarLayout;

    final FirebaseFirestore db = FirebaseFirestore.getInstance();


    Boolean[] isTabExpanded = new Boolean[]{true, false, true};
    Boolean isCurTabExpanded;

    Context context;
    final LocalImageUtil localImageUtil = new LocalImageUtil(null, this);

    RecommendationFeedViewModel recommendationFeedViewModel;
    ProfileViewModel profileViewModel;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        recommendationFeedViewModel = ViewModelProviders.of(requireActivity()).get(RecommendationFeedViewModel.class);

        profileViewModel = ViewModelProviders.of(requireActivity()).get(ProfileViewModel.class);
        profileViewModel.init();
        startObservers();

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.fragment_profile, container, false);

        viewPager = root.findViewById(R.id.htab_viewpager);
        profileFragment = this;
        activity = (MainActivity) getActivity();
        this.savedInstanceState = savedInstanceState;

        setupViewPager(viewPager);

        TabLayout tabLayout = root.findViewById(R.id.htab_tabs);
        tabLayout.setupWithViewPager(viewPager);

        tabLayout.addOnTabSelectedListener(tabListener);

        appBarLayout = root.findViewById(R.id.htab_appbar);
        appBarLayout.addOnOffsetChangedListener(collapseListener);

        collapsingToolbarLayout = (CollapsingToolbarLayout) root.findViewById(R.id.htab_collapse_toolbar);
        setUpOnScrollColorChange();

        profileImage = (ImageView) root.findViewById(R.id.profile_image);
        profileUsername = root.findViewById(R.id.profile_username);
        curUserLocation = root.findViewById(R.id.cur_user_location);

        curUserLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Location location = ((MainActivity) getActivity()).getUserLocation();
                if (location != null) {
                    Toast.makeText(getContext(), "Latitude : " + location.getLatitude() + "\nLongitude : " + location.getLongitude(), Toast.LENGTH_LONG).show();
                }
                else{
                    Toast.makeText(getContext(), "Issue getting location", Toast.LENGTH_LONG).show();
                }
            }
        });

        context = getContext();

        localImageUtil.setContext(context);
        profileImage.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                localImageUtil.selectImage();
            }
        });

        return root;
    }

    @Override
    public void onResume(){
        super.onResume();

    }

    AppBarLayout.OnOffsetChangedListener collapseListener = new AppBarLayout.OnOffsetChangedListener() {
        @Override
        public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
            if (Math.abs(verticalOffset) == appBarLayout.getTotalScrollRange()) {
                isCurTabExpanded = false;
            } else if (verticalOffset == 0) {
                isCurTabExpanded = true;
            }
        }
    };

    TabLayout.OnTabSelectedListener tabListener = new TabLayout.OnTabSelectedListener() {

        @Override
        public void onTabSelected(TabLayout.Tab tab) {
            // map tab
            if (tab.getPosition() == 1){
                expandAppbar(false);
            }
            else{
                expandAppbar(isTabExpanded[tab.getPosition()]);
            }
        }

        @Override
        public void onTabUnselected(TabLayout.Tab tab) {
            // map tab
            if (tab.getPosition() == 1){
                expandAppbar(true);
            }
            else{
                isTabExpanded[tab.getPosition()] = isCurTabExpanded;
            }
        }

        @Override
        public void onTabReselected(TabLayout.Tab tab) {
            // map tab
            if (tab.getPosition() == 1){
                expandAppbar(false);
            }
        }
    };

    private void setUpOnScrollColorChange(){

            collapsingToolbarLayout.setContentScrimColor(
                    ContextCompat.getColor(getContext(), R.color.colorLightBlue)
            );
            collapsingToolbarLayout.setStatusBarScrimColor(
                    ContextCompat.getColor(getContext(), R.color.colorLightBlue)
            );

    }

    public void expandAppbar(Boolean expanded){
        appBarLayout.setExpanded(expanded);
    }

    private void startObservers(){

        profileViewModel.getUserRatingVector().observe(this, new Observer<List<Double>>() {
            @Override
            public void onChanged(List<Double> doubles) {
                profilePagerAdapter.setupAttrGrid();
            }
        });


        profileViewModel.getUserPostFeed().observe(this, new Observer<List<TripGridRow>>() {
            @Override
            public void onChanged(List<TripGridRow> tripGridRows) {
                profilePagerAdapter.buildPostAdapter();
            }
        });

        profileViewModel.getUsername().observe(this, new Observer<String>() {
            @Override
            public void onChanged(String s) {
                profileUsername.setText(s);
            }
        });
    }

    public Bundle getSavedInstanceState(){
        return savedInstanceState;
    }

    public FirebaseFirestore getDb(){
        return db;
    }

    List<Double> getUserRatingVector(){
        return profileViewModel.getUserRatingVector().getValue();
    }

    List<String> getPostIds(){
        return profileViewModel.getUserPostIds().getValue();
    }

    List<TripGridRow> getPostFeed(){
        return profileViewModel.getUserPostFeed().getValue();
    }
    
    private void roundProfileImage(Bitmap bitmap){

        profileImage.invalidate();

        Bitmap imageRounded=Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), bitmap.getConfig());
        Canvas canvas=new Canvas(imageRounded);
        Paint mpaint = new Paint();
        mpaint.setAntiAlias(true);
        mpaint.setShader(new BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP));
        canvas.drawRoundRect((new RectF(0, 0, bitmap.getWidth() - 100, bitmap.getHeight() - 100)), 100, 100, mpaint); // Round Image Corner 100 100 100 100

        //contrast 0 to 10 (1 is default), brightness -255 to 255 (0 is default).
        //1.2f and -40f works well

        Glide.with(context)
                .load(imageRounded)
                .transform(new ContrastTransform(1.25f, -40f))
                .into(profileImage);

    }


    private void setupViewPager(ViewPager viewPager) {

        profilePagerAdapter = new ProfilePagerAdapter(
                getContext(), activity, activity.getSupportFragmentManager(), profileFragment);

        viewPager.setAdapter(profilePagerAdapter);
    }

    public void getPictureActivity(Intent intent, int reqCode){
        startActivityForResult(intent, reqCode);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(resultCode != RESULT_CANCELED) {

            Uri curUri = null;
            switch (requestCode) {
                case 0:
                    if (resultCode == RESULT_OK && data != null) {

                        curUri = localImageUtil.getUri();
                    }

                    break;
                case 1:
                    if (resultCode == RESULT_OK && data != null) {
                        Uri selectedImage =  data.getData();
                        String[] filePathColumn = {MediaStore.Images.Media.DATA};
                        if (selectedImage != null) {
                            Cursor cursor = context.getContentResolver().query(selectedImage,
                                    filePathColumn, null, null, null);
                            if (cursor != null) {
                                cursor.moveToFirst();
                                curUri = data.getData();
                                cursor.close();
                            }
                        }

                    }
                    break;
            }

            Bitmap bitmap = null;
            try {
                bitmap = MediaStore.Images.Media.getBitmap(context.getContentResolver(), curUri);
            } catch (IOException e) {
                Toast.makeText(context, "No file!", Toast.LENGTH_LONG).show();
            }
            roundProfileImage(bitmap);
        }
    }


    public void callBackRecalculateRecommendationFeed(final List<Double> ratingVector){

        Thread thread = new Thread() {
            @Override
            public void run() {

                FirebaseFunctions mFunctions;

                mFunctions = FirebaseFunctions.getInstance();
                Map<String, Object> data = new HashMap<>();
                data.put("ratingVector", ratingVector);

                mFunctions
                    .getHttpsCallable("updateRecommendationFeedForUser")
                    .call(data)
                        .addOnSuccessListener(new OnSuccessListener<HttpsCallableResult>() {
                    @Override
                    public void onSuccess(HttpsCallableResult httpsCallableResult) {

                        recommendationFeedViewModel.setRecalculatingRecommendations(2);
                        Log.d("OUT!!!!==", "SUCCESS");
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d("OUT!!!!==", "FAILL");
                    }
                });

            }
        };

        thread.start();
    }
}
