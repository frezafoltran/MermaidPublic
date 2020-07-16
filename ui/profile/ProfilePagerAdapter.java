package com.foltran.mermaid.ui.profile;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.net.ParseException;

import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.PopupWindow;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import androidx.fragment.app.FragmentManager;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.PagerAdapter;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestManager;
import com.bumptech.glide.signature.ObjectKey;
import com.foltran.mermaid.MainActivity;
import com.foltran.mermaid.R;
import com.foltran.mermaid.adapter.profile.TripGridAdapter;
import com.foltran.mermaid.database.requests.UpdateUserInfo;
import com.foltran.mermaid.model.profile.TripGridRow;
import com.foltran.mermaid.repositories.UserRepository;
import com.foltran.mermaid.storage.images.GlideApp;
import com.foltran.mermaid.ui.map_box.MapBoxActivity;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;

import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.mapbox.api.staticmap.v1.MapboxStaticMap;
import com.mapbox.api.staticmap.v1.StaticMapCriteria;
import com.mapbox.geojson.Point;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static com.foltran.mermaid.repositories.UserRepository.attrIds;


public class ProfilePagerAdapter extends PagerAdapter {
    /**
     * ProfilePagerAdapter provides the tab interface for the profile page. It inflates the layouts
     * for the pages that show the user's trips, settings and map features. The ProfilePagerAdapter
     * class is used in the ProfileFragment fragment.
     */

    Context context;
    ViewGroup container;
    View itemView;

    Activity activity;
    FragmentManager fm;
    ProfileFragment profileFragment;

    int numTabs = 3, navBarWidth;
    LayoutInflater layoutInflater;
    int profileSettingsGridNumColumns = 4;
    int profileSettingsPopupGridNumColumns = 4;

    UpdateUserInfo updateUserInfoRequest;
    CollectionReference postCollectionReference;

    private List<TripGridRow> profileFeed;
    private RecyclerView profileFeedRecycler;
    private TripGridAdapter profileFeedAdapter;

    private DisplayMetrics displayMetrics = new DisplayMetrics();
    private int width;
    private int height;

    private List<String> attrLabels = new ArrayList<>();

    private GridView profileGrid;
    List<String> allLabels = new ArrayList<>();
    List<Integer> allLogos = new ArrayList<>();

    List<String> popupLabels;
    List<Integer> popupLogos;

    ImageView mapPreview;

    Boolean isAttrGridsSet = false;
    TextView addAttrPopup;
    CustomAdapter attrGridAdapter;

    Boolean addedUserMapPreviewFlag = false;

    Boolean[] isTabCollapsed = new Boolean[]{false, true, false};

    private Double ATTR_RATING_THRESH = 0.5;

    public ProfilePagerAdapter(Context context, Activity activity, FragmentManager fm, ProfileFragment profileFragment) {

        this.context = context;
        this.activity = activity;
        this.fm = fm;
        this.profileFragment = profileFragment;
        postCollectionReference = profileFragment.getDb().collection("trip_posts");

        layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        Resources resources = context.getResources();
        int resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android");
        if (resourceId > 0) {
            this.navBarWidth = resources.getDimensionPixelSize(resourceId);
        }

        activity.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        width = displayMetrics.widthPixels;
        height = displayMetrics.heightPixels;

        UserRepository.addAllAttrLabels(attrLabels, activity);
    }

    @Override
    public int getCount() {
        return numTabs;
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == ((FrameLayout) object);
    }

    @Override
    public Object instantiateItem(final ViewGroup container, final int position) {

        this.container = container;

        if (position == 0) {
            itemView = layoutInflater.inflate(R.layout.dummy_fragment, container, false);

            profileFeedRecycler = (RecyclerView) itemView.findViewById(R.id.dummyfrag_scrollableview);

            profileFeedRecycler.setPadding(0,0,0, navBarWidth);

            RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(context);
            profileFeedRecycler.setLayoutManager(layoutManager);
            profileFeedRecycler.setHasFixedSize(true);

            if (profileFeedAdapter != null) {
                profileFeedRecycler.setAdapter(profileFeedAdapter);
            }
        }
        else if (position == 1){
            itemView = layoutInflater.inflate(R.layout.fragment_profile_map, container, false);

            mapPreview = itemView.findViewById(R.id.map_pre_image);
            addUserMapPreview();

            mapPreview.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(activity, MapBoxActivity.class);
                    activity.startActivity(intent);
                }
            });

        }
        else{
            itemView = layoutInflater.inflate(R.layout.fragment_profile_settings, container, false);

            profileGrid = (GridView) itemView.findViewById(R.id.profile_grid);
            addAttrPopup = itemView.findViewById(R.id.add_attr_popup);

            //check if data is already parsed and if adapter has not yet been set
            if (!isAttrGridsSet && popupLogos != null && popupLabels != null){
                setupAttrGridAdapter();
                addAttrPopup();
                isAttrGridsSet = true;
            }

            addManualAttrPopup(itemView, container);

            //change trip preferences
            Button changeTripStylePref = itemView.findViewById(R.id.trip_style_preferences);

            changeTripStylePref.setOnClickListener(new Button.OnClickListener() {
                @Override
                public void onClick(View v) {
                    FragmentManager fragmentManager  = fm;
                    NavController f = NavHostFragment.findNavController(fragmentManager.getPrimaryNavigationFragment());
                    f.navigate(R.id.navigation_trip_style_preferences);
                }
            });

        }

        container.addView(itemView);
        return itemView;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        container.removeView((FrameLayout) object);
    }

    @Override
    public CharSequence getPageTitle(int position) {

        if (position == 0) return "Trips";
        if (position == 1) return "Map";
        else return "Settings";
    }

    void addUserMapPreview(){

        MapboxStaticMap staticImage = MapboxStaticMap.builder()
                .accessToken(activity.getResources().getString(R.string.mapbox_access_token))
                .styleId(StaticMapCriteria.SATELLITE_STREETS_STYLE)
                .cameraPoint(Point.fromLngLat(-70, 45))
                .cameraZoom(8)
                .width(300) // Image width
                .height(300) // Image height
                .retina(true) // Retina 2x image will be returned
                .logo(false)
                .build();

        String imageUrl = staticImage.url().toString();

        GlideApp.with(context).load(imageUrl).into(mapPreview);
    }

    void setAddedUserMapPreviewFlag(Boolean flag){
        this.addedUserMapPreviewFlag = flag;
    }


    void setupAttrGrid(){

        // already set attr views
        if (isAttrGridsSet) return;

        List<Double> curRatingVals = new ArrayList<>(profileFragment.getUserRatingVector());

        UserRepository.addAllAttrLabels(allLabels, activity);
        UserRepository.addAllSymbols(allLogos, context);

        List<String> includedLabels = new ArrayList<>();
        List<Integer> includedLogos = new ArrayList<>();

        popupLabels = new ArrayList<>();
        popupLogos = new ArrayList<>();

        for (int i = 0; i < curRatingVals.size(); i++){

            if (curRatingVals.get(i) > ATTR_RATING_THRESH){
                includedLabels.add(allLabels.get(i));
                includedLogos.add(allLogos.get(i));
            }
            else{
                popupLabels.add(allLabels.get(i));
                popupLogos.add(allLogos.get(i));
            }
        }

        attrGridAdapter = new CustomAdapter(context, includedLabels, includedLogos);

        // not yet called created view
        if (addAttrPopup == null || profileGrid == null) return;

        setupAttrGridAdapter();
        addAttrPopup();
        isAttrGridsSet = true;
    }

    private void setupAttrGridAdapter(){

        profileGrid.setAdapter(attrGridAdapter);
        setGridViewHeightBasedOnChildren(profileGrid, profileSettingsGridNumColumns);

        final CharSequence[] options = UserRepository.attrPopupOptions();

        profileGrid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                AlertDialog.Builder builder = new AlertDialog.Builder(context);

                TextView titleView = view.findViewById(R.id.profile_settings_grid_label);
                builder.setTitle(UserRepository.attrPopupTitle(titleView.getText().toString()));

                builder.setItems(options, new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int item) {

                        if (options[item].equals("Take Photo")) {


                        } else if (options[item].equals("Choose from Gallery")) {


                        } else if (options[item].equals("Cancel")) {
                            dialog.dismiss();
                        }
                    }
                });
                builder.show();
            }
        });
    }


    private void addManualAttrPopup(final View itemView, final ViewGroup container){

        Button manuallyEditAttr = itemView.findViewById(R.id.manual_profile_edit);
        manuallyEditAttr.setOnClickListener(new Button.OnClickListener(){

            @Override
            public void onClick(View v){
                final View popupView = layoutInflater.inflate(R.layout.popup_profile_settings_manually_edit_attrs, null);

                int popupWidth = (int) (width * 0.8);
                int popupHeight = (int) (height * 0.6);
                boolean focusable = true; // lets taps outside the popup also dismiss it
                final PopupWindow popupWindow = new PopupWindow(popupView, popupWidth, popupHeight, focusable);


                LinearLayout attrWrapper = popupView.findViewById(R.id.attr_wrapper);
                if (attrWrapper.getChildCount() > 0) attrWrapper.removeAllViews();

                for (int attrCount = 0; attrCount < attrLabels.size(); attrCount++){

                    LinearLayout attrRow = new LinearLayout(context);
                    attrRow.setLayoutParams(new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT));

                    attrRow.setOrientation(LinearLayout.HORIZONTAL);

                    TextView labelView = new TextView(context);
                    labelView.setLayoutParams(new TableRow.LayoutParams(0,
                            ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

                    labelView.setText(attrLabels.get(attrCount));

                    EditText editView = new EditText(context);
                    editView.setLayoutParams(new TableRow.LayoutParams(0,
                            ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
                    editView.setLines(1);

                    List<Double> curAttrVals = profileFragment.getUserRatingVector();
                    if (curAttrVals.size() > attrCount){
                        editView.setText("" + curAttrVals.get(attrCount));
                    }

                    editView.setId(attrIds.get(attrCount));

                    attrRow.addView(labelView);
                    attrRow.addView(editView);
                    attrWrapper.addView(attrRow);
                }


                TextView doneBtn = popupView.findViewById(R.id.save_changes);
                doneBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        if (validateChanges(popupView)) popupWindow.dismiss();
                        else Toast.makeText(context, "You must enter valid values", Toast.LENGTH_LONG).show();
                    }
                });
                popupWindow.showAtLocation(container, Gravity.CENTER, 0, 0);

                popupView.setOnTouchListener(new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        popupWindow.dismiss();
                        return true;
                    }
                });

            }
        });

    }

    private Boolean validateChanges(View popupView){

        List<Double> vals = new ArrayList<>();
        for (int attr : attrIds){

            EditText curEdit = popupView.findViewById(attr);
            String curString = curEdit.getText().toString();

            if (curString.length() == 0){
                vals.add(Double.valueOf(0));
                continue;
            }

            Double curVal;
            try {
                curVal = Double.parseDouble(curString);
            }
            catch (ParseException | NumberFormatException e){ return false; }
            vals.add(curVal);
        }

        profileFragment.recommendationFeedViewModel.setRecalculatingRecommendations(1);
        updateUserInfoRequest = new UpdateUserInfo((MainActivity) activity, profileFragment, null);
        try {
            updateUserInfoRequest.execute("", vals).get();
        } catch (ExecutionException | InterruptedException e) {
            Toast.makeText(context, "Error writing change to db", Toast.LENGTH_LONG).show();
        }


        return true;
    }

    private void addAttrPopup(){

        addAttrPopup.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                View popupView = layoutInflater.inflate(R.layout.profile_settings_new_attr_popup, null);

                GridView profileGrid = (GridView) popupView.findViewById(R.id.profile_grid_add_attr);


                CustomAdapter customAdapter = new CustomAdapter(context, popupLabels, popupLogos);
                profileGrid.setAdapter(customAdapter);

                profileGrid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                    }
                });

                setGridViewHeightBasedOnChildren(profileGrid, profileSettingsPopupGridNumColumns);

                // create the popup window
                int popupWidth = LinearLayout.LayoutParams.WRAP_CONTENT;
                int popupHeight = LinearLayout.LayoutParams.WRAP_CONTENT;
                boolean focusable = true; // lets taps outside the popup also dismiss it
                final PopupWindow popupWindow = new PopupWindow(popupView, popupWidth, popupHeight, focusable);


                popupWindow.showAtLocation(container, Gravity.CENTER, 0, 0);

                popupView.setOnTouchListener(new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        popupWindow.dismiss();
                        return true;
                    }
                });
            }
        });
    }

    void buildPostAdapter(){

        List<String> postIds = profileFragment.getPostIds();
        if (postIds != null){

            profileFeed = new ArrayList<>(profileFragment.getPostFeed());
            profileFeedAdapter = new TripGridAdapter(profileFeed, context, profileFragment);

            if (profileFeedRecycler != null) {
                profileFeedRecycler.setAdapter(profileFeedAdapter);
            }
        }

    }

    public class CustomAdapter extends BaseAdapter {

        Context context;
        List<String> labels;
        List<Integer> logos;

        public CustomAdapter(Context context, List<String> labels, List<Integer> logos) {
            this.context = context;
            this.labels = labels;
            this.logos = logos;
        }
        @Override
        public int getCount() {
            return labels.size();
        }
        @Override
        public Object getItem(int i) {
            return null;
        }
        @Override
        public long getItemId(int i) {
            return 0;
        }
        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {

            LayoutInflater inflater = LayoutInflater.from(context);
            view = inflater.inflate(R.layout.profile_settings_grid_view, null);

            TextView label = (TextView) view.findViewById(R.id.profile_settings_grid_label);
            label.setText(labels.get(i));

            ImageView icon = (ImageView) view.findViewById(R.id.icon);
            icon.setImageResource(logos.get(i)); // set logo images
            return view;
        }
    }

    public void setGridViewHeightBasedOnChildren(GridView gridView, int columns) {
        ListAdapter listAdapter = gridView.getAdapter();
        if (listAdapter == null) {
            // pre-condition
            return;
        }

        int totalHeight = 0;
        int items = listAdapter.getCount();
        int rows = 0;

        View listItem = listAdapter.getView(0, null, gridView);
        listItem.measure(0, 0);
        totalHeight = listItem.getMeasuredHeight();

        float x = 1;
        if( items > columns ){
            x = items/columns;
            rows = (int) (x + 1);
            totalHeight *= rows + 1;
        }

        ViewGroup.LayoutParams params = gridView.getLayoutParams();
        params.height = totalHeight;
        gridView.setLayoutParams(params);

    }
}