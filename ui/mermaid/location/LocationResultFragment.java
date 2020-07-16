package com.foltran.mermaid.ui.mermaid.location;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import android.util.DisplayMetrics;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TableRow;
import android.widget.TextView;

import com.bumptech.glide.RequestManager;
import com.foltran.mermaid.MainActivity;
import com.foltran.mermaid.R;
import com.foltran.mermaid.adapter.SimpleImagePagerAdapter;
import com.foltran.mermaid.model.location.Location;
import com.foltran.mermaid.storage.images.GlideApp;
import com.foltran.mermaid.storage.images.LocalImageUtil;
import com.google.android.material.tabs.TabLayout;

import com.mapbox.api.staticmap.v1.MapboxStaticMap;
import com.mapbox.api.staticmap.v1.StaticMapCriteria;
import com.mapbox.geojson.Point;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.lang.StrictMath.cos;
import static java.lang.StrictMath.sin;


public class LocationResultFragment extends Fragment {

    Location location;
    ViewPager locationImage;
    SimpleImagePagerAdapter pagerAdapter;

    Display display;
    DisplayMetrics displayMetrics = new DisplayMetrics();
    static float displayWidth;
    static int mapPreviewHeight = 200;


    static RequestManager glide;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.fragment_location_result, container, false);

        final Toolbar toolbar = root.findViewById(R.id.htab_toolbar);
        AppCompatActivity appCompatActivity = ((AppCompatActivity) getActivity());

        appCompatActivity.setSupportActionBar(toolbar);
        if (appCompatActivity.getSupportActionBar() != null){
            appCompatActivity.getSupportActionBar().setTitle("Profile");
        }
        appCompatActivity.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivity().onBackPressed();
            }
        });

        Bundle bundle = this.getArguments();
        if (bundle != null) {
            location = (Location) bundle.getSerializable("locationObj");
        }
        else{
            location = new Location("", -1);
        }


        locationImage = (ViewPager) root.findViewById(R.id.location_image);
        glide = GlideApp.with(getContext());

        getActivity().getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        display = getActivity().getWindowManager().getDefaultDisplay();
        float screenDensity  = getResources().getDisplayMetrics().density;

        displayWidth = displayMetrics.widthPixels/screenDensity;


        ((AppCompatActivity)getActivity()).getSupportActionBar().setDisplayShowCustomEnabled(false);
        ((AppCompatActivity)getActivity()).getSupportActionBar().setTitle(location.getQueryName());

        addImage();

        final ViewPager viewPager = root.findViewById(R.id.htab_viewpager);

        LocationResultAdapter adapter = new LocationResultAdapter(getContext(), getActivity(), location);

        viewPager.setAdapter(adapter);

        TabLayout tabLayout = root.findViewById(R.id.htab_tabs);
        tabLayout.setupWithViewPager(viewPager);

        return root;
    }


    private void addImage(){

        pagerAdapter = new SimpleImagePagerAdapter(
                getContext(), location.getStockImagePath(),
                R.layout.simple_image_pager_view, R.id.imageView, this);

        locationImage.setAdapter(pagerAdapter);
    }

    public static class LocationResultAdapter extends PagerAdapter {

        Location location;
        Context context;
        Activity activity;
        String[] tabLabels = new String[]{"About", "Ratings"};
        int navBarWidth, totalWidth;
        LayoutInflater layoutInflater;

        int highlightYellowColor;


        public LocationResultAdapter(Context context, Activity activity, Location location) {

            this.context = context;
            layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            this.location = location;

            Resources resources = context.getResources();
            int resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android");
            if (resourceId > 0) {
                this.navBarWidth = resources.getDimensionPixelSize(resourceId);
            }

            DisplayMetrics displayMetrics = new DisplayMetrics();
            activity.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
            this.totalWidth = displayMetrics.widthPixels;
            this.activity = activity;
            this.highlightYellowColor = context.getResources().getColor(R.color.colorYellowHighlight);

        }

        @Override
        public int getCount() {
            return tabLabels.length;
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == ((FrameLayout) object);
        }

        @Override
        public Object instantiateItem(final ViewGroup container, final int position) {

            View itemView;

            if (position == 0) {
                itemView = layoutInflater.inflate(R.layout.location_result_about, container, false);


                final ImageView tempGraphView = (ImageView) itemView.findViewById(R.id.temp_graph);
                final ImageView snowGraphView = (ImageView) itemView.findViewById(R.id.snow_graph);
                final ImageView precGraphView = (ImageView) itemView.findViewById(R.id.prec_graph);

                final TextView milesFromUser = itemView.findViewById(R.id.location_miles_from_user);
                if (location.getDistToUser() != 0){
                    milesFromUser.setText(String.valueOf(location.getDistToUser()));
                }

                MapboxStaticMap staticImage = MapboxStaticMap.builder()
                        .accessToken(activity.getResources().getString(R.string.mapbox_access_token))
                        .styleId(StaticMapCriteria.SATELLITE_STREETS_STYLE)
                        .cameraPoint(Point.fromLngLat(location.getLon(), location.getLat()))
                        .cameraZoom(8)
                        .width((int)displayWidth) // Image width
                        .height(mapPreviewHeight) // Image height
                        .retina(true) // Retina 2x image will be returned
                        .logo(false)
                        .build();

                String imageUrl = staticImage.url().toString();
                ImageView mapPreviewView = itemView.findViewById(R.id.location_map_preview);

                glide.load(imageUrl).into(mapPreviewView);

                final LinearLayout weatherPrecBtn = itemView.findViewById(R.id.avg_prec_graph);
                final LinearLayout weatherTempBtn = itemView.findViewById(R.id.avg_temp_graph);
                final LinearLayout weatherSnowBtn = itemView.findViewById(R.id.avg_snow_graph);

                weatherPrecBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        snowGraphView.setVisibility(View.GONE);
                        tempGraphView.setVisibility(View.GONE);
                        precGraphView.setVisibility(View.VISIBLE);
                        weatherTempBtn.setBackgroundColor(Color.WHITE);
                        weatherSnowBtn.setBackgroundColor(Color.WHITE);
                        weatherPrecBtn.setBackgroundColor(highlightYellowColor);
                    }
                });

                weatherTempBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        snowGraphView.setVisibility(View.GONE);
                        precGraphView.setVisibility(View.GONE);
                        tempGraphView.setVisibility(View.VISIBLE);
                        weatherPrecBtn.setBackgroundColor(Color.WHITE);
                        weatherSnowBtn.setBackgroundColor(Color.WHITE);
                        weatherTempBtn.setBackgroundColor(highlightYellowColor);
                    }
                });

                weatherSnowBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        precGraphView.setVisibility(View.GONE);
                        tempGraphView.setVisibility(View.GONE);
                        snowGraphView.setVisibility(View.VISIBLE);
                        weatherPrecBtn.setBackgroundColor(Color.WHITE);
                        weatherTempBtn.setBackgroundColor(Color.WHITE);
                        weatherSnowBtn.setBackgroundColor(highlightYellowColor);
                    }
                });

                addWeatherGraph(tempGraphView, location.getAvgTemps(), true);
                addWeatherGraph(snowGraphView, location.getAvgSnow(), false);
                addWeatherGraph(precGraphView, location.getAvgPrec(), false);

                addBasicInfo(itemView);
                addParkInfo(itemView);
                addPOIsInfo(itemView);
            }
            else{
                itemView = layoutInflater.inflate(R.layout.location_result_ratings, container, false);

                addRatings(itemView);
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
            return tabLabels[position];
        }


        private CardView createRatingCard(){

            CardView card = new CardView(context);

            // Set the CardView layoutParams
            ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );

            card.setLayoutParams(params);
            card.setRadius(9);
            card.setContentPadding(15, 15, 15, 15);

            card.setCardBackgroundColor(Color.WHITE);
            card.setCardElevation(9);
            card.setMaxCardElevation(15);


            return card;
        }

        public void addRatings(View itemView){

            LinearLayout ratingsWrapper = itemView.findViewById(R.id.location_ratings_wrapper);

            for (Map<String, Object> cur : location.getRatings()) {

                Double attrScore = (Double) cur.get("attrScore");
                String attrLabel = (String) cur.get("attr");

                LinearLayout progressBarWrap = new LinearLayout(context);

                progressBarWrap.setLayoutParams(new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                progressBarWrap.setOrientation(LinearLayout.HORIZONTAL);

                //create label
                TextView label = new TextView(context);
                label.setText(attrLabel.replace("_", " "));
                label.setTextAppearance(context, R.style.RatingTextView);
                ViewGroup.LayoutParams params = new TableRow.LayoutParams(0,
                        ViewGroup.LayoutParams.WRAP_CONTENT, 1f);

                label.setLayoutParams(params);

                label.setGravity(Gravity.CENTER_VERTICAL);

                //create progress bar
                ProgressBar progressBar = new ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal);
                LinearLayout.LayoutParams progressBarParams = new LinearLayout.LayoutParams(
                        0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);

                int mediumMargin = context.getResources().getDimensionPixelOffset(R.dimen.margin_medium);
                progressBarParams.setMargins(mediumMargin, mediumMargin, mediumMargin, mediumMargin);
                progressBar.setLayoutParams(progressBarParams);
                progressBar.setProgressDrawable(ContextCompat.getDrawable(context, R.drawable.progress_bar_gradient));
                progressBar.setVisibility(View.VISIBLE);

                Integer intScore = attrScore.intValue();
                progressBar.setProgress(intScore);

                progressBarWrap.addView(label);
                progressBarWrap.addView(progressBar);

                LinearLayout cardLayout = new LinearLayout(context);

                cardLayout.setLayoutParams(new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                cardLayout.setOrientation(LinearLayout.VERTICAL);

                String subAttrLabel = (String) cur.get("subAttr");
                String subAttrLabelScore = (String) cur.get("subAttrLabelScore");
                TextView extraInfo = new TextView(context);
                extraInfo.setText("since it has a " + subAttrLabelScore + " number of " + subAttrLabel);

                cardLayout.addView(progressBarWrap);
                cardLayout.addView(extraInfo);

                CardView cardWrapper = createRatingCard();
                cardWrapper.addView(cardLayout);
                //cardWrapper.addView(progressBarWrap);
                ratingsWrapper.addView(cardWrapper);

            }

        }

        public void addBasicInfo(View itemView){

            TextView locationPopulation = itemView.findViewById(R.id.location_population);
            locationPopulation.setText("" + location.getPopulation());

            TextView locationElevation = itemView.findViewById(R.id.location_elevation);
            locationElevation.setText(location.getElevation());

            TextView closestLocation = itemView.findViewById(R.id.closest_location);
            closestLocation.setText(location.getClosestLocation());
        }

        public void addPOIsInfo(View itemView){

            TextView historicPlaces = itemView.findViewById(R.id.num_historic_places);
            historicPlaces.setText("" + location.getNumHistoricPlaces());

            TextView museums = itemView.findViewById(R.id.museum_count);
            museums.setText("" + location.getMuseumCount());

            /*
            Map<String, Integer> allTypes = location.getMuseumProfile();

            if (allTypes != null) {
                ImageView museumTypeCanvasHolder = itemView.findViewById(R.id.museum_types);
                museumTypeCanvasHolder.setImageBitmap(drawPieChart(allTypes));
            }

             */

        }

        private void addParkInfo(View itemView){

            TextView numCityParks = itemView.findViewById(R.id.city_parks_num);
            numCityParks.setText("" + location.getNumCityParks());

            List<Map<String, Object>> nationalParks = location.getNationalParks();
            View nationalParksWrapper =  itemView.findViewById(R.id.national_park_wrapper);
            if(((LinearLayout) nationalParksWrapper).getChildCount() > 0)
                ((LinearLayout) nationalParksWrapper).removeAllViews();

            if (nationalParks != null) {
                for (Map<String, Object> nationalPark : nationalParks) {

                    TextView cur = new TextView(context);
                    String dist = String.format("%.2f", nationalPark.get("distance_in_km"));
                    String txt = nationalPark.get("national_park_name") + " at " + dist + "Km";
                    cur.setText(txt);
                    //valueTV.setId(5);
                    cur.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT));

                    ((LinearLayout) nationalParksWrapper).addView(cur);
                }
            }
        }

        public void addWeatherGraph(ImageView weatherGraph, List<String> vals, Boolean isTemperature){


            int canvasHeight = 300;
            double canvasWidth = totalWidth * 0.7;

            int textSize = 48;
            int strokeSize = 10;
            int horizOffset = textSize;
            int vertOffset = horizOffset + 100;

            Bitmap bitmap = Bitmap.createBitmap(totalWidth,
                    canvasHeight + vertOffset, Bitmap.Config.ARGB_8888);

            Canvas canvas = new Canvas(bitmap);

            Paint paint = new Paint();
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(strokeSize);
            paint.setAntiAlias(true);
            paint.setShader(new LinearGradient(
                    0, horizOffset, 0, canvasHeight + horizOffset,
                    context.getResources().getColor(R.color.colorRedHighlight),
                    context.getResources().getColor(R.color.colorDarkBlue), Shader.TileMode.MIRROR));

            //degree symbol on xml &#xb0;
            String celsiusSym = "\u2103";
            String fahrenheitSym = "\u2109";

            Paint textPaint = new Paint();
            textPaint.setColor(Color.BLACK);
            textPaint.setTextSize(textSize);

            int numPoints = 12;
            String[] labels = new String[12];
            String[] months = new String[12];
            Double[] scaledTemps = new Double[12];

            double minTemp = Double.MAX_VALUE;
            double maxTemp = Double.MIN_VALUE;

            for (int i = 0; i < labels.length; i++){

                if (i == 0 || i == labels.length - 1 || i == (labels.length)/2) {

                    if (isTemperature) labels[i] = vals.get(i) + fahrenheitSym;
                    else labels[i] = vals.get(i);

                    if (i == 0) months[i] = "Jan";
                    if (i == labels.length - 1) months[i] = "Dec";
                    if (i == (labels.length)/2) months[i] = "Jul";
                }
                else{
                    labels[i] = "";
                    months[i] = "";
                }

                minTemp = Math.min(minTemp, Double.parseDouble(vals.get(i)));
                maxTemp = Math.max(maxTemp, Double.parseDouble(vals.get(i)));
            }

            for (int i = 0; i < 12; i++){
                scaledTemps[i] = (maxTemp - Double.parseDouble(vals.get(i))) * (canvasHeight/(maxTemp - minTemp));
            }

            int initOffset = (int)(totalWidth - canvasWidth)/2;
            int offset = (int) canvasWidth/(numPoints - 1);

            for (int i = 0; i < numPoints - 1; i++){

                int x1 = offset * i + initOffset;
                double y1 = scaledTemps[i] + horizOffset;

                int x2 = offset * (i + 1) + initOffset;
                double y2 = scaledTemps[i + 1] + horizOffset;

                canvas.drawLine(x1, (int) y1, x2, (int) y2, paint);
                canvas.drawText(labels[i], x1, (int) y1, textPaint);

                canvas.drawText(months[i], x1, canvasHeight + vertOffset - textSize, textPaint);

                if (i == numPoints - 2){
                    canvas.drawText(labels[i + 1], x2, (int) y2, textPaint);
                    canvas.drawText(months[i + 1], x2, canvasHeight + vertOffset - textSize, textPaint);
                }
            }

            weatherGraph.setImageBitmap(bitmap);
        }

        public Bitmap drawPieChart(Map<String, Integer> allTypes){

            int allColors[] = {
                    context.getResources().getColor(R.color.colorDarkBlue),
                    context.getResources().getColor(R.color.colorMediumBlue),
                    context.getResources().getColor(R.color.colorLightBlue),
                    context.getResources().getColor(R.color.colorRedHighlight),
                    context.getResources().getColor(R.color.colorYellowHighlight),
                    context.getResources().getColor(R.color.colorLightGrey),
                    context.getResources().getColor(R.color.colorBlack),
                    context.getResources().getColor(R.color.colorWhite),
                    context.getResources().getColor(R.color.colorLightBlue),
                    context.getResources().getColor(R.color.colorDarkBlue),
            };

            List<Integer> colors = new ArrayList<>();
            List<Integer> slices = new ArrayList<>();
            List<String> labels = new ArrayList<>();

            int c = 0;
            for (String key : allTypes.keySet()){
                labels.add(key);
                slices.add(allTypes.get(key));
                colors.add(allColors[c++]);
            }

            double width = totalWidth;
            double height = totalWidth;

            double halfWidth = totalWidth * 0.5;
            double halfHeight = totalWidth * 0.5;


            int textSize = 68;

            Bitmap bmp = Bitmap.createBitmap((int) width,
                    (int) halfHeight, Bitmap.Config.ARGB_8888);

            Canvas canvas = new Canvas(bmp);

            int arcRadius = (int)halfWidth/2;
            int rectPadding = 42;

            RectF box = new RectF(
                    rectPadding + arcRadius,
                    rectPadding,
                    (int)halfWidth-rectPadding + arcRadius,
                    (int)halfHeight-rectPadding);

            //get value for 100%
            int sum = 0;
            for (int slice : slices) {
                sum += slice;
            }

            Paint paint = new Paint();
            paint.setAntiAlias(true);

            Paint textPaint = new Paint();
            textPaint.setColor(Color.BLACK);
            textPaint.setTextSize(textSize);

            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(1f);
            paint.setStyle(Paint.Style.FILL_AND_STROKE);
            float start = 0;
            //draw slices
            for(int i =0; i < slices.size(); i++){

                paint.setColor(colors.get(i));
                float angle;
                angle = ((360.0f / sum) * slices.get(i));

                double radians = Math.toRadians(start + angle/2);

                canvas.drawArc(box, start, angle, true, paint);

                // draw label
                double yHelper = (arcRadius - rectPadding) * sin(radians);
                double xHelper = (arcRadius - rectPadding) * cos(radians);

                int labelLength = labels.get(i).length();
                double xPadding = xHelper < 0 ?  labelLength * textSize/2 + labelLength : -labelLength;
                double yPadding = yHelper > 0 ?  textSize/2 : 0;

                double txtWidth = halfWidth + xHelper - xPadding;
                //double txtHeight = yHelper > 0 ? arcRadius - yHelper : arcRadius + Math.abs(yHelper);
                double txtHeight = arcRadius + yHelper + yPadding;


                canvas.drawText(labels.get(i), (int)txtWidth, (int)txtHeight, textPaint);

                start += angle;
            }

            return bmp;
        }

    }
}
