package com.foltran.mermaid.adapter.trip_post;

import android.app.Activity;
import android.content.Context;

import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.ViewPager;

import com.foltran.mermaid.R;
import com.foltran.mermaid.model.trip.TripCardWrapper;


import java.util.List;

/**
 *  The TripPostAdapter class is an adapter for the main feed where trips from other users
 *  are accessible. It takes a List of TripCardWrapper objects and displays them in a
 *  RecyclerView. This class uses TripPostPagerAdapter to allow for a Pager view of multiple
 *  cards in the feed
 */
public class TripPostAdapter extends RecyclerView.Adapter<TripPostAdapter.MyViewHolder> {

    private List<TripCardWrapper> feedPosts;
    private DisplayMetrics displayMetrics;
    private Context ctx;
    private Fragment fragment;

    static class MyViewHolder extends RecyclerView.ViewHolder {

        private ViewPager viewPager;
        private TextView tripTitleView;

        private MyViewHolder(View v) {
            super(v);
            viewPager = (ViewPager) v.findViewById(R.id.feed_card_wrapper);
            tripTitleView = v.findViewById(R.id.trip_title);
        }
    }

    public TripPostAdapter(Context ctx, List<TripCardWrapper> feedPosts, Fragment fragment) {
        this.fragment = fragment;
        this.ctx = ctx;
        this.feedPosts = feedPosts;
        displayMetrics = new DisplayMetrics();
        ((Activity) ctx).getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
    }

    @Override
    public TripPostAdapter.MyViewHolder onCreateViewHolder(ViewGroup parent,
                                                           int viewType) {

        CardView v = (CardView) LayoutInflater.from(parent.getContext())
                .inflate(R.layout.fragment_home_feed_item, parent, false);

        TextView username = v.findViewById(R.id.post_author_username);
        username.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                FragmentManager fm = fragment.getFragmentManager();

                NavController f = NavHostFragment.findNavController(fm.getPrimaryNavigationFragment());
                f.navigate(R.id.navigation_profile);
            }
        });

        MyViewHolder vh = new MyViewHolder(v);

        return vh;
    }

    @Override
    public void onBindViewHolder(final MyViewHolder holder, int position) {

        TripCardWrapper cur = feedPosts.get(position);

        holder.tripTitleView.setText(cur.getTripTitle());
        ViewPager viewPager = holder.viewPager;

        TripPostPagerAdapter pagerAdapter = new TripPostPagerAdapter(
                ctx, cur, fragment);

        viewPager.setAdapter(pagerAdapter);

    }

    @Override
    public int getItemCount() {
        return feedPosts.size();
    }
}