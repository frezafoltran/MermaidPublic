package com.foltran.mermaid.ui.profile;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import com.foltran.mermaid.MainActivity;
import com.foltran.mermaid.R;
import com.foltran.mermaid.adapter.item_mover.OnStartDragListener;
import com.foltran.mermaid.adapter.item_mover.RecyclerListAdapter;
import com.foltran.mermaid.adapter.item_mover.SimpleItemTouchHelperCallback;
import com.foltran.mermaid.database.requests.UpdateUserInfo;
import com.foltran.mermaid.model.trip.TripCard;
import com.foltran.mermaid.ui.recommendation_feed.RecommendationFeedViewModel;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.functions.HttpsCallableResult;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.zip.Inflater;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link TripStylePreferencesFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class TripStylePreferencesFragment extends Fragment implements OnStartDragListener {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private ItemTouchHelper mItemTouchHelper;
    private TextView btnNext;
    private TextView btnReset;

    private LayoutInflater inflater;
    private View root;
    private MainActivity activity;
    private Context context;
    private ViewGroup container;

    private TripStylePreferencesFragment tripStylePreferencesFragment;
    private Map<String, List<Integer>> tripStyleAttrs = new HashMap<>();

    private int profileSettingsGridNumColumns = 2;
    RecommendationFeedViewModel recommendationFeedViewModel;

    public TripStylePreferencesFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment TripStylePreferencesFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static TripStylePreferencesFragment newInstance(String param1, String param2) {
        TripStylePreferencesFragment fragment = new TripStylePreferencesFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
        recommendationFeedViewModel = ViewModelProviders.of(requireActivity()).get(RecommendationFeedViewModel.class);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.fragment_trip_style_preferences, container, false);

        this.root = root;
        this.inflater = inflater;
        this.context = getContext();
        this.container = container;
        this.activity = (MainActivity) getActivity();
        this.tripStylePreferencesFragment = this;

        tripStyleAttrs.put("Outdoor Adventures", new ArrayList<>(Arrays.asList(3, 9, 10, 12, 11, 14)));
        tripStyleAttrs.put("Foodie", new ArrayList<>(Arrays.asList(17, 0, 1)));
        tripStyleAttrs.put("Relaxing", new ArrayList<>(Arrays.asList(17, 20, 0, 3, 13)));
        tripStyleAttrs.put("Local Culture", new ArrayList<>(Arrays.asList(5, 4, 8, 19, 15)));
        tripStyleAttrs.put("Parties", new ArrayList<>(Arrays.asList(1, 2)));
        tripStyleAttrs.put("Family Friendly", new ArrayList<>(Arrays.asList(16, 18, 3, 9, 15)));

        btnNext = root.findViewById(R.id.next_location_select);
        btnNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showLocationGridPopup();
            }
        });

        return root;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);


        List<String> items = Arrays.asList(getContext().getResources().getStringArray(R.array.types_of_trip_labels));
        List<String> subItems = Arrays.asList(getContext().getResources().getStringArray(R.array.types_of_trip_sub_labels));

        List<Drawable> icons = new ArrayList<>();
        icons.add(getContext().getResources().getDrawable(R.drawable.ic_nature_24px));
        icons.add(getContext().getResources().getDrawable(R.drawable.ic_food_diversity_24px));
        icons.add(getContext().getResources().getDrawable(R.drawable.ic_hotel_24px));
        icons.add(getContext().getResources().getDrawable(R.drawable.ic_monuments_24px));
        icons.add(getContext().getResources().getDrawable(R.drawable.ic_nightlife_24px));
        icons.add(getContext().getResources().getDrawable(R.drawable.ic_children_24px));


        final RecyclerListAdapter adapter = new RecyclerListAdapter(
                getActivity(), null,this, items, subItems,
                icons, false);

        final RecyclerView recyclerView = view.findViewById(R.id.recycler_view);
        recyclerView.setHasFixedSize(true);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        ItemTouchHelper.Callback callback = new SimpleItemTouchHelperCallback(adapter);
        mItemTouchHelper = new ItemTouchHelper(callback);
        mItemTouchHelper.attachToRecyclerView(recyclerView);


        btnReset = root.findViewById(R.id.reset_profile);
        btnReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                List<String> curOrder = adapter.getItemsOrdered();
                List<Double> ratingVector = new ArrayList<>();
                for (int i = 0; i < 21; i ++) ratingVector.add((double) 0);

                for (int i = 0; i < curOrder.size(); i++){

                    double curWeight = 1 - i/5.0;
                    for (Integer attrIndex : tripStyleAttrs.get(curOrder.get(i))){

                        if (ratingVector.get(attrIndex) > 0) continue;
                        ratingVector.set(attrIndex, curWeight);
                    }
                }

                UpdateUserInfo updateUserInfoRequest = new UpdateUserInfo((MainActivity) getActivity(), null, tripStylePreferencesFragment);
                try {
                    updateUserInfoRequest.execute("", ratingVector).get();
                } catch (ExecutionException | InterruptedException e) {
                    Toast.makeText(context, "Error writing change to db", Toast.LENGTH_LONG).show();
                }
            }
        });

    }

    @Override
    public void onStartDrag(RecyclerView.ViewHolder viewHolder) {
        mItemTouchHelper.startDrag(viewHolder);
    }

    private List<Object[]> getSurveyLocations(){

        List<String> labels = new ArrayList<>(Arrays.asList(
                "New York, NY, USA",
                "Boston, MA, USA",
                "Anchorage, AK, USA",
                "New Orleans, LA, USA"));

        Collections.shuffle(labels);

        String packageName = context.getPackageName();
        Resources res = context.getResources();
        List<Integer> logos = new ArrayList<>(Arrays.asList(
                res.getIdentifier("mermaid_tail", "drawable", packageName),
                res.getIdentifier("mermaid_tail", "drawable", packageName),
                res.getIdentifier("mermaid_tail", "drawable", packageName),
                res.getIdentifier("mermaid_tail", "drawable", packageName)
        ));

        List<Object[]> out = new ArrayList<>();

        for (int i = 0; i < labels.size(); i++){
            out.add(new Object[]{labels.get(i), logos.get(i)});
        }

        return out;
    }

    private void showLocationGridPopup(){

        View popupView = inflater.inflate(R.layout.trip_style_preferences_locations_grid, null);

        final GridView profileGrid = (GridView) popupView.findViewById(R.id.location_grid);

        TextView nextBtn = popupView.findViewById(R.id.next_location_select);
        nextBtn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                CustomAdapter customAdapter = new CustomAdapter(context, getSurveyLocations());
                profileGrid.setAdapter(customAdapter);
            }
        });

        CustomAdapter customAdapter = new CustomAdapter(context, getSurveyLocations());
        profileGrid.setAdapter(customAdapter);

        profileGrid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

            }
        });

        setGridViewHeightBasedOnChildren(profileGrid, profileSettingsGridNumColumns);

        // create the popup window
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);

        int width = (int) (0.8 * displayMetrics.widthPixels);
        int height = (int) (0.7 * displayMetrics.heightPixels);
        boolean focusable = true; // lets taps outside the popup also dismiss it
        final PopupWindow popupWindow = new PopupWindow(popupView, width, height, focusable);

        popupWindow.showAtLocation(container, Gravity.CENTER, 0, 0);

        popupView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                popupWindow.dismiss();
                return true;
            }
        });

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
                                Log.d("OUT!!!!==", "SUCCESS");
                                recommendationFeedViewModel.setRecalculatingRecommendations(2);
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
        //((MainActivity) getActivity()).refreshRecommendationCallback(recommendationFeedGCF + result);

    }


    public class CustomAdapter extends BaseAdapter {

        Context context;
        List<String> labels;
        List<Integer> logos;

        public CustomAdapter(Context context, List<Object[]> params) {
            this.context = context;
            this.labels = new ArrayList<>();
            this.logos = new ArrayList<>();

            for (int i = 0; i < params.size(); i++){

                Object[] cur = (Object[]) params.get(i);
                labels.add((String) cur[0]);
                logos.add((int) cur[1]);
            }
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
            totalHeight *= rows;
        }

        ViewGroup.LayoutParams params = gridView.getLayoutParams();
        params.height = totalHeight;
        gridView.setLayoutParams(params);

    }

}
