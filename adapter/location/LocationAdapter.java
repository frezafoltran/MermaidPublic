package com.foltran.mermaid.adapter.location;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.ViewPager;

import com.foltran.mermaid.R;
import com.foltran.mermaid.model.location.Location;

import java.text.DecimalFormat;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import static android.view.View.GONE;

public class LocationAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private List<Location> locations;
    private Context context;
    private Fragment fragment;
    private ViewGroup container;

    private final int VIEW_TYPE_ITEM = 0;
    private final int VIEW_TYPE_LOADING = 1;

    public static class MyViewHolder extends RecyclerView.ViewHolder {

        public ViewPager viewPager;
        public CardView locationFeedItemWrapper;
        public TextView locationTitle;
        public TextView similarityScore;

        public TextView[] ratingLabels = new TextView[3];

        public MyViewHolder(View v) {
            super(v);

            locationFeedItemWrapper = v.findViewById(R.id.location_feed_item_wrapper);
            viewPager = (ViewPager) v.findViewById(R.id.feed_card_wrapper);
            locationTitle = v.findViewById(R.id.location_title);
            similarityScore = v.findViewById(R.id.similarity_score);

            ratingLabels[0] = v.findViewById(R.id.rating1_label);
            ratingLabels[1] = v.findViewById(R.id.rating2_label);
            ratingLabels[2] = v.findViewById(R.id.rating3_label);

        }
    }

    private class LoadingViewHolder extends RecyclerView.ViewHolder {

        ProgressBar progressBar;

        public LoadingViewHolder(@NonNull View itemView) {
            super(itemView);
            progressBar = itemView.findViewById(R.id.progressBar);
        }
    }

    public LocationAdapter(Context context, List<Location> feedPosts, Fragment fragment) {
        this.fragment = fragment;
        this.context = context;
        this.locations = feedPosts;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent,
                                                           int viewType) {

        /*
        CardView v = (CardView) LayoutInflater.from(parent.getContext())
                .inflate(R.layout.location_feed_item, parent, false);

        container = parent;
        MyViewHolder vh = new MyViewHolder(v);

        return vh;
         */
        container = parent;
        if (viewType == VIEW_TYPE_ITEM) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.location_feed_item, parent, false);
            return new MyViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.loading_recycler_view_item, parent, false);
            return new LoadingViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder holder, int position) {

        if (holder instanceof MyViewHolder) {
            final Location cur = locations.get(position);

            //display location name
            ((MyViewHolder) holder).locationTitle.setText(cur.getQueryName());

            //displays similarity of location to user profile
            DecimalFormat df = new DecimalFormat("###");
            ((MyViewHolder) holder).similarityScore.setText(df.format(cur.getSimilarity() * 100) + "% ");

            setupTopMatchCards(((MyViewHolder) holder), cur);

            ViewPager viewPager = ((MyViewHolder) holder).viewPager;

            LocationPagerAdapter pagerAdapter = new LocationPagerAdapter(
                    context,
                    fragment,
                    cur);

            viewPager.setAdapter(pagerAdapter);
        }
        else{

        }

    }

    @Override
    public int getItemCount() {
        return locations == null ? 0 : locations.size();
    }

    @Override
    public int getItemViewType(int position) {
        return locations.get(position) == null ? VIEW_TYPE_LOADING : VIEW_TYPE_ITEM;
    }

    private void setupTopMatchCards(MyViewHolder holder, Location cur){

        for (int i = 0; i < holder.ratingLabels.length; i++){

            String curMatch = cur.getTopMatch(i);
            if (!curMatch.equals("")){
                holder.ratingLabels[i].setText(curMatch);
                setupRatingPopup(holder.ratingLabels[i], curMatch);
            }
            else{
                holder.ratingLabels[i].setVisibility(GONE);
            }

        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setupRatingPopup(final TextView ratingView, String attrName){

        View popupView = LayoutInflater.from(context).inflate(R.layout.attribute_snippet_explanation, null);

        TextView attrExplanation = popupView.findViewById(R.id.attr_explanation);

        String explanationText;
        switch (attrName){
            case "nightlife":
                explanationText = "measure the quantity/quality of bars, clubs, casinos...";
                break;
            case "parks":
                explanationText = "measure the number of urban parks";
                break;
            case "commerce":
                explanationText = "measure the quantity/quality of malls, shops...";
                break;
            case "water":
                explanationText = "measure the quantity/quality of waterfalls, beaches, lakes...";
                break;
            case "hotel":
                explanationText = "measures the quantity/quality of resorts, hotels and spas";
                break;
            case "sport":
                explanationText = "measures the quantity/quality of stadiums, sport teams...";
                break;
            case "food_diversity":
                explanationText = "measures the quantity/quality of restaurants";
                break;
            default:
                explanationText = "TODO";
        }

        attrExplanation.setText(explanationText);

        boolean focusable = true;
        final PopupWindow filterPopupWindow  = new PopupWindow(popupView, LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT, focusable);

        popupView.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));

        final double popupHeight = popupView.getMeasuredHeight();

        ratingView.setOnTouchListener(new View.OnTouchListener() {

            private Timer timer = new Timer();
            private long LONG_PRESS_TIMEOUT = 250;
            private boolean wasLong = false;

            @Override public boolean onTouch(View v, final MotionEvent event) {

                if (event.getAction() == android.view.MotionEvent.ACTION_DOWN) {

                    ratingView.setBackgroundColor(context.getResources().getColor(R.color.colorDarkBlue));

                    timer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            wasLong = true;
                            ((Activity) context).runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    filterPopupWindow.showAtLocation(ratingView,
                                            Gravity.NO_GRAVITY, (int)event.getX(), (int)(event.getY() - 2.5*popupHeight));
                                }
                            });
                        }
                    }, LONG_PRESS_TIMEOUT);
                    return true;

                } else if (event.getAction() == android.view.MotionEvent.ACTION_UP
                        || event.getAction() == MotionEvent.ACTION_CANCEL
                        || event.getAction() == MotionEvent.ACTION_OUTSIDE) {

                    ratingView.setBackgroundColor(context.getResources().getColor(R.color.colorLightBlue));
                    timer.cancel();
                    filterPopupWindow.dismiss();
                    if(!wasLong){
                    }
                    timer = new Timer();
                    return true;
                }
                return false;
            }
        });
    }
}
