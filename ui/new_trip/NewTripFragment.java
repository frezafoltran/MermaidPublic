package com.foltran.mermaid.ui.new_trip;


import android.os.Bundle;

import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
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

import androidx.annotation.NonNull;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import androidx.lifecycle.ViewModelProviders;
import androidx.viewpager.widget.ViewPager;
import com.foltran.mermaid.R;
import com.foltran.mermaid.database.requests.LocalLocationTrie;
import com.google.android.material.tabs.TabLayout;

import org.w3c.dom.Text;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


public class NewTripFragment extends Fragment {


    CoordinatorLayout newTripWrapper;
    DisplayMetrics displayMetrics = new DisplayMetrics();
    PopupWindow startTripPopup;

    int displayWidth;
    int displayHeight;

    int MAX_RESULTS = 4;
    LocalLocationTrie locationSearch;

    LinearLayout matchesWrapper;
    LinearLayout allLocationsEditViews;
    EditText locationEditText;
    TextView tripStartPopupQuestion;

    LinearLayout nextButton;
    LinearLayout doneButton;
    List<String> locationsVisited = new ArrayList<>();
    String originLocation;

    int numEditTextViews = 1;
    NewTripViewModel model;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        locationSearch = new LocalLocationTrie();
        model = ViewModelProviders.of(requireActivity()).get(NewTripViewModel.class);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.fragment_new_trip, container, false);

        final ViewPager viewPager = root.findViewById(R.id.htab_viewpager);

        setupViewPager(viewPager);

        TabLayout tabLayout = root.findViewById(R.id.htab_tabs);
        tabLayout.setupWithViewPager(viewPager);

        newTripWrapper = root.findViewById(R.id.new_trip_wrapper);

        // screen configurations
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        displayWidth = displayMetrics.widthPixels;
        displayHeight = displayMetrics.heightPixels;

        showTripStartPopup(inflater, container);

        return root;
    }

    private TextWatcher onChangeLocationEditText(final Boolean addsNewEditBox,
                                                 final int editBoxPosition,
                                                 final EditText curEditText){

        return new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {

                curEditText.setBackground(getActivity().getResources().getDrawable(R.color.colorLightGrey));

                List<String> possibleMatches = locationSearch.getMatches(s.toString().toLowerCase());

                if (possibleMatches == null) return;

                matchesWrapper.removeAllViews();

                for (int i = 0; i < possibleMatches.size(); i++) {

                    if (i > MAX_RESULTS) break;
                    buildSearchResult(possibleMatches.get(i), editBoxPosition, addsNewEditBox);

                }
            }
        };
    }

    private void showTripStartPopup(LayoutInflater inflater, ViewGroup container){

        final View popupView = inflater.inflate(R.layout.popup_new_trip_start, null);

        matchesWrapper = popupView.findViewById(R.id.search_results);
        locationEditText = popupView.findViewById(R.id.location_search_text);
        allLocationsEditViews = popupView.findViewById(R.id.all_locations_edit_views);
        tripStartPopupQuestion = popupView.findViewById(R.id.trip_start_popup_question);

        nextButton = popupView.findViewById(R.id.button_next);
        doneButton = popupView.findViewById(R.id.button_done);

        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tripStartPopupOriginQuestion();
            }
        });

        locationEditText.addTextChangedListener(onChangeLocationEditText(true, 0, locationEditText));

        int width = (int) (displayWidth * 0.8);
        int height = (int) (displayHeight * 0.5);

        startTripPopup = new PopupWindow(popupView, width, height, true);

        startTripPopup.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                newTripWrapper.setAlpha(1f);
            }
        });

        newTripWrapper.setAlpha(0.5f);
        startTripPopup.showAtLocation(container, Gravity.CENTER, 0, 0);
    }

    private void buildSearchResult(String match, final int editTextIndex, final Boolean addsNewEditBox){

        final TextView newMatch = new TextView(getContext());
        LinearLayout.LayoutParams params=new LinearLayout.LayoutParams
                (ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);

        params.bottomMargin  = 10;

        newMatch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // factor of 2 accounts for the symbol images
                if (addsNewEditBox){

                    if (locationsVisited.size() > editTextIndex){
                        locationsVisited.set(editTextIndex, newMatch.getText().toString());
                    }
                    else{
                        locationsVisited.add(newMatch.getText().toString());
                    }

                    ((EditText) allLocationsEditViews.getChildAt(2 * editTextIndex)).setText(newMatch.getText());
                    allLocationsEditViews.getChildAt(2 * editTextIndex).setBackground(getActivity().getResources().getDrawable(R.color.colorLightBlue));
                }
                // in case it's asking for location where user left from
                else{

                    originLocation = newMatch.getText().toString();
                    ((EditText) allLocationsEditViews.getChildAt(0)).setText(newMatch.getText());
                    allLocationsEditViews.getChildAt(0).setBackground(getActivity().getResources().getDrawable(R.color.colorLightBlue));
                }
                matchesWrapper.removeAllViews();

                // only adds new editText if last box
                if (numEditTextViews - 1 == editTextIndex && addsNewEditBox) {
                    addNewLocationEditView();
                }
            }
        });

        newMatch.setLayoutParams(params);
        newMatch.setText(match);
        matchesWrapper.addView(newMatch);

    }

    private void addNewLocationEditView(){

        EditText searchView = createSimpleEditText();
        searchView.addTextChangedListener(onChangeLocationEditText(true, numEditTextViews++, searchView));

        allLocationsEditViews.addView(createSimpleArrowDown());
        allLocationsEditViews.addView(searchView);

    }

    private void tripStartPopupOriginQuestion(){

        tripStartPopupQuestion.setText("Where did you leave from?");

        allLocationsEditViews.removeAllViews();

        EditText searchView = createSimpleEditText();
        searchView.addTextChangedListener(onChangeLocationEditText(false, 0, searchView));

        allLocationsEditViews.addView(searchView);

        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tripStartPopupDateQuestion();
            }
        });
    }

    private void tripStartPopupDateQuestion(){

        nextButton.setVisibility(View.GONE);
        doneButton.setVisibility(View.VISIBLE);

        //create map with locations added
        doneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                int locationIndex = 0;
                int totalPositions = 1 + locationsVisited.size();

                if (originLocation != null && !originLocation.equals("")) {
                    model.addLocationToWorkingMap(originLocation, locationIndex++, null);
                }

                for (String location : locationsVisited){
                    model.addLocationToWorkingMap(location, locationIndex++, null);
                }
            }
        });

        allLocationsEditViews.removeAllViews();

        tripStartPopupQuestion.setText("When did you travel?");
        EditText dateView = createSimpleEditText();

        DateFormat dateFormat = new SimpleDateFormat("MM/yy");
        Date date = new Date();
        dateView.setText(dateFormat.format(date));
        //dateView.addTextChangedListener(onChangeLocationEditText(false, 0, searchView));

        allLocationsEditViews.addView(dateView);
        allLocationsEditViews.addView(createSimpleTextView("From:"));
        allLocationsEditViews.addView(createSimpleTextView(originLocation));
        allLocationsEditViews.addView(createSimpleTextView("To:"));

        for (int i = 0; i < locationsVisited.size(); i++){

            String destination = locationsVisited.get(i);

            allLocationsEditViews.addView(createSimpleTextView(destination));

            if (i != locationsVisited.size() - 1) {
                allLocationsEditViews.addView(createSimpleArrowDown());
            }
        }
    }

    private EditText createSimpleEditText(){

        EditText out = new EditText(getContext());
        LinearLayout.LayoutParams params=new LinearLayout.LayoutParams
                (ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);

        out.setLayoutParams(params);

        return out;
    }

    private ImageView createSimpleArrowDown(){

        ImageView downArrow = new ImageView(getContext());
        downArrow.setImageDrawable(getActivity().getResources().getDrawable(R.drawable.ic_keyboard_arrow_down_black_24dp));
        LinearLayout.LayoutParams symParams = new LinearLayout.LayoutParams
                (ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);

        downArrow.setLayoutParams(symParams);

        return downArrow;
    }

    private TextView createSimpleTextView(String text){

        TextView out = new TextView(getContext());
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams
                (ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);

        out.setGravity(Gravity.CENTER_HORIZONTAL);
        out.setLayoutParams(params);
        out.setText(text);

        return out;
    }

    private void setupViewPager(ViewPager viewPager) {


        FragmentManager fm = getChildFragmentManager();
        ViewPagerAdapter adapter = new ViewPagerAdapter(
                fm, FragmentPagerAdapter.BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);

        PostTripFragment postTripFragment = new PostTripFragment();

        PreviewTripFragment previewFragment = new PreviewTripFragment(
                ContextCompat.getColor(getContext(), R.color.colorWhite));

        EditCardFragment editFragment = new EditCardFragment(
                getContext(),
                ContextCompat.getColor(getContext(), R.color.colorWhite),
                previewFragment);

        editFragment.setSelfFragment(editFragment);

        adapter.addFrag(editFragment, "Edit");
        adapter.addFrag(previewFragment, "Preview");
        adapter.addFrag(postTripFragment, "Post");

        viewPager.setAdapter(adapter);
    }


    private static class ViewPagerAdapter extends FragmentPagerAdapter {

        final List<Fragment> mFragmentList = new ArrayList<>();
        final List<String> mFragmentTitleList = new ArrayList<>();

        public ViewPagerAdapter(FragmentManager fm, int behavior) {
            super(fm, behavior);
        }

        @Override
        public Fragment getItem(int position) {
            return mFragmentList.get(position);
        }

        @Override
        public int getCount() {
            return mFragmentList.size();
        }

        public void addFrag(Fragment fragment, String title) {
            mFragmentList.add(fragment);
            mFragmentTitleList.add(title);
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return mFragmentTitleList.get(position);
        }
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        if (startTripPopup != null) startTripPopup.dismiss();
    }

    @Override
    public void onPause(){
       super.onPause();
       if (startTripPopup != null) startTripPopup.dismiss();
    }

}
