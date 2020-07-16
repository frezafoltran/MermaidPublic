package com.foltran.mermaid.ui.recommendation_feed;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.foltran.mermaid.MainActivity;
import com.foltran.mermaid.R;
import com.foltran.mermaid.adapter.location.LocationAdapter;

import com.foltran.mermaid.database.requests.LocalLocationTrie;
import com.foltran.mermaid.model.location.Location;


import java.util.ArrayList;
import java.util.Arrays;

import java.util.List;


/**
 * The RecommendationFeedFragment presents the user with the feed that display the location
 * recommendations. It also presents a search interface to look up different cities and filters
 * to filter the recommendation results.
 */
public class RecommendationFeedFragment extends Fragment{

    private RecyclerView feedRecycler;
    private Boolean isFeedRecyclerLoading = false;
    private LocationAdapter feedAdapter;

    private View root;
    private ViewGroup container;
    private LayoutInflater inflater;
    private CoordinatorLayout recomendationFeedWrapper;


    private MainActivity activity;

    private int screenWidth;
    private int screenHeight;

    private DisplayMetrics displayMetrics = new DisplayMetrics();

    private LocalLocationTrie locationTrie;

    // top bar search and filter views
    private EditText userQueryText;
    private TextView filterBtn;
    private LinearLayout searchBarWrapper;

    private PopupWindow filterPopupWindow;
    private Boolean isSimilarityCheckBoxChecked = false;
    private Boolean isDistanceCheckBoxChecked = false;

    private LinearLayout loadingRecommendationsView;

    private int ABSOLUTE_MAX_POPULATION = 1000000000;
    private int maxPopulation = ABSOLUTE_MAX_POPULATION;

    private int feedItemCount = 0;
    float popUpBGOpacity = 0.5f;
    boolean isFeedCached;

    boolean needCompleteFeedReset = false;
    
    
    private RecommendationFeedViewModel mRecommendationFeedViewModel;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState){
        super.onCreate(savedInstanceState);

        mRecommendationFeedViewModel = ViewModelProviders.of(requireActivity()).get(RecommendationFeedViewModel.class);
        isFeedCached = mRecommendationFeedViewModel.init();

        mRecommendationFeedViewModel.getRecommendationFeedLocations().observe(this, new Observer<List<Location>>() {
            @Override
            public void onChanged(List<Location> locations) {
                handleFeedLocationsChange(locations);
            }
        });

        mRecommendationFeedViewModel.getRecalculatingRecommendations().observe(this, new Observer<Integer>() {
            @Override
            public void onChanged(Integer code) {
                handleRecommendationFeedDone(code);
            }
        });

        // TODO store data structure in local memory to avoid reloading
        locationTrie = new LocalLocationTrie();
        
    }
    
    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.fragment_recommendation_feed, container, false);

        this.root = root;
        this.inflater = inflater;
        this.container = container;
        this.activity = (MainActivity) getActivity();
        this.recomendationFeedWrapper = root.findViewById(R.id.recommendation_feed_wrapper);

        getActivity().getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        screenHeight = displayMetrics.heightPixels;
        screenWidth = displayMetrics.widthPixels;

        userQueryText = root.findViewById(R.id.location_user_query);

        filterBtn = root.findViewById(R.id.filter_recommendations);

        searchBarWrapper = root.findViewById(R.id.search_bar_wrapper);
        loadingRecommendationsView = root.findViewById(R.id.loading_recommendations_wrapper);

        if (!isFeedCached){
            searchBarWrapper.setVisibility(View.GONE);
            loadingRecommendationsView.setVisibility(View.VISIBLE);
        }
        else{
            loadingRecommendationsView.setVisibility(View.GONE);
            searchBarWrapper.setVisibility(View.VISIBLE);
        }

        userQueryText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {

                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    return searchLocations();
                }

                return false;
            }
        });


        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {

        initFeedRecyclerView();
        initScrollListener();
    }

    @Override
    public void onStart(){
        super.onStart();

        filterRecommendationPopup(inflater, container);

        Integer updateFeedStatus = mRecommendationFeedViewModel.getRecalculatingRecommendations().getValue();

        //recalculation is happening, show loading symbol
        if (updateFeedStatus == 1){
            searchBarWrapper.setVisibility(View.GONE);
            loadingRecommendationsView.setVisibility(View.VISIBLE);
        }
        else if (updateFeedStatus == 2){
            loadingRecommendationsView.setVisibility(View.GONE);
            searchBarWrapper.setVisibility(View.VISIBLE);
        }

    }

    private Boolean searchLocations(){

        String curQuery = userQueryText.getText().toString().toLowerCase();

        String possibleMatch = locationTrie.getMatch(curQuery);
        if (possibleMatch.length() > 0){

            Toast.makeText(getContext(), "matched with " + possibleMatch, Toast.LENGTH_SHORT).show();
            String result = possibleMatch;
            List<String> userQuery = new ArrayList<>(Arrays.asList(result));

            needCompleteFeedReset = true;
            mRecommendationFeedViewModel.resetRecommendationFeed(userQuery);
        }
        else{
            Toast.makeText(getContext(), "No match :(", Toast.LENGTH_SHORT).show();
        }
        return true;
    }

    /**
     * Handles changes in UI when the content of the feed changes. Changes could include:
     * - deletion of entries (by applying filters)
     * - addition of entries (by scrolling down)
     * - change of order (by applying filters)
     *
     * @param locations the updates Array describing the filter
     */
    private void handleFeedLocationsChange(List<Location> locations){


        // in case the recommendations were entirely re-calculated, create adapter again
        // to avoid errors if user is scrolling through old content
        if (needCompleteFeedReset){
            feedAdapter = new LocationAdapter(getContext(), mRecommendationFeedViewModel.getRecommendationFeedLocations().getValue(), this);
            feedRecycler.setAdapter(feedAdapter);
            feedAdapter.notifyDataSetChanged();
            needCompleteFeedReset = false;
        }
        else if (feedItemCount < locations.size()) {
            feedAdapter.notifyItemInserted(locations.size() - 1);
        }
        else {
            feedAdapter.notifyDataSetChanged();
        }

        feedItemCount = locations.size();


        // if there are locations loaded in feed, stop showing loading symbol and show search bar
        if (searchBarWrapper.getVisibility() == View.GONE && locations.size() > 0){
            Log.d("----MADE VISIBLE", "EEEEE");
            loadingRecommendationsView.setVisibility(View.GONE);
            searchBarWrapper.setVisibility(View.VISIBLE);
        }

        // indicate there's no pending update
        mRecommendationFeedViewModel.setRecalculatingRecommendations(0);

        // indicate feed is cached to not sow loading symbol
        isFeedCached = true;
    }

    private void handleRecommendationFeedDone (Integer done){

        // done recalculating feed, needs to update UI
        if (done == 2){
            Log.d("--- handleRecFeedDone: ", " val: " + done);
            loadingRecommendationsView.setVisibility(View.GONE);
            searchBarWrapper.setVisibility(View.VISIBLE);

            needCompleteFeedReset = true;
            mRecommendationFeedViewModel.resetRecommendationFeed(null);
        }
        // still recalculation feed
        else if (done == 1){
            Log.d("--- handleRecFeedDone: ", " val: " + done);
            searchBarWrapper.setVisibility(View.GONE);
            loadingRecommendationsView.setVisibility(View.VISIBLE);
        }

    }

    /**
     * Initializes the adapter for the recommendation filter.
     * Should be called onViewCreated for a fragment
     */
    private void initFeedRecyclerView(){
        feedRecycler = (RecyclerView) root.findViewById(R.id.recommendation_feed);

        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getContext());
        feedRecycler.setLayoutManager(layoutManager);

        feedAdapter = new LocationAdapter(getContext(), mRecommendationFeedViewModel.getRecommendationFeedLocations().getValue(), this);
        feedRecycler.setAdapter(feedAdapter);

    }


    private void filterRecommendationPopup(final LayoutInflater inflater, final ViewGroup container){

        filterBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                final View filterPopupView = inflater.inflate(R.layout.popup_recommendation_filter, null);

                SeekBar populationFilter = filterPopupView.findViewById(R.id.population_filter);
                if (maxPopulation == 100000) populationFilter.setProgress(0);
                else if (maxPopulation == 300000) populationFilter.setProgress(1);
                if (maxPopulation == 1000000) populationFilter.setProgress(2);
                if (maxPopulation == ABSOLUTE_MAX_POPULATION) populationFilter.setProgress(3);

                populationFilter.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    int val = 0;

                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        val = progress;
                    }

                    public void onStartTrackingTouch(SeekBar seekBar) {
                    }

                    public void onStopTrackingTouch(SeekBar seekBar) {

                        Toast.makeText(getContext(), "cur val: " + val, Toast.LENGTH_LONG).show();
                        if (val == 0) maxPopulation = 100000;
                        else if (val == 1) maxPopulation = 300000;
                        else if (val == 2) maxPopulation = 1000000;
                        else maxPopulation = ABSOLUTE_MAX_POPULATION;
                    }
                });

                int popupWidth = (int) (screenWidth * 0.8);
                int popupHeight = (int) (screenHeight * 0.6);
                boolean focusable = true; // lets taps outside the popup also dismiss it
                filterPopupWindow= new PopupWindow(filterPopupView, popupWidth, popupHeight, focusable);

                recomendationFeedWrapper.setAlpha(0.5f);
                filterPopupWindow.showAtLocation(container, Gravity.CENTER, 0, 0);

                setupFilterCheckboxes(filterPopupView);

                TextView applyFilter = filterPopupView.findViewById(R.id.save_filter);
                applyFilter.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mRecommendationFeedViewModel.filterByPopulation(maxPopulation);
                        filterPopupWindow.dismiss();
                    }
                });

                filterPopupWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
                    @Override
                    public void onDismiss() {
                        recomendationFeedWrapper.setAlpha(1f);
                    }
                });

            }
        });

    }

    private void setupFilterCheckboxes(View filterPopupView){

        CheckBox similarityCheckBox = filterPopupView.findViewById(R.id.similarity_filter_checkbox);
        similarityCheckBox.setChecked(isSimilarityCheckBoxChecked);

        similarityCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked){
                    mRecommendationFeedViewModel.sortFeedBySimilarity();
                }
                isSimilarityCheckBoxChecked = isChecked;
            }
        });


        CheckBox distanceCheckBox = filterPopupView.findViewById(R.id.distance_filter_checkbox);
        distanceCheckBox.setChecked(isDistanceCheckBoxChecked);
        distanceCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked){
                    mRecommendationFeedViewModel.sortFeedByDistance();
                }
                isDistanceCheckBoxChecked = isChecked;
            }
        });

    }

    private void initScrollListener() {
        feedRecycler.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
            }

            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                LinearLayoutManager linearLayoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();

                if (!isFeedRecyclerLoading && linearLayoutManager != null) {


                    boolean reachedBottom = linearLayoutManager.findLastCompletelyVisibleItemPosition()
                            == mRecommendationFeedViewModel.getRecommendationFeedLocations().getValue().size() - 1;

                    if (reachedBottom){
                        loadMore();
                        isFeedRecyclerLoading = true;
                    }
                }
            }
        });
    }

    private void loadMore() {

        List<Location> locations = mRecommendationFeedViewModel.getRecommendationFeedLocations().getValue();

        // still adding element, do not add null again
        if (locations.size() > 0 && locations.get(locations.size() - 1) != null) {
            mRecommendationFeedViewModel.addLocationToFeed(null);
        }

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {

                mRecommendationFeedViewModel.updateFeed();
                isFeedRecyclerLoading = false;
            }
        }, 0);

    }
}
