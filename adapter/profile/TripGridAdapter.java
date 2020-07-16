package com.foltran.mermaid.adapter.profile;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.RequestManager;
import com.foltran.mermaid.R;
import com.foltran.mermaid.model.profile.TripGridRow;
import com.foltran.mermaid.storage.images.GlideApp;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;


import java.util.List;

public class TripGridAdapter extends RecyclerView.Adapter<TripGridAdapter.TripGridHolder> {

    public List<TripGridRow> posts;
    FirebaseStorage storage = FirebaseStorage.getInstance();
    StorageReference storageReference = storage.getReference();

    RequestManager glide;
    final long ONE_MEGABYTE =  1024*1024;

    public Context context;
    private Fragment fragment;

    public static class TripGridHolder extends RecyclerView.ViewHolder {

        public TextView[] labels = new TextView[2];
        public ImageView[] imageViews = new ImageView[2];
        public CardView[] cardWrapper = new CardView[2];

        public TripGridHolder(View v) {
            super(v);

            cardWrapper[0] = v.findViewById(R.id.card_wrapper_1);
            cardWrapper[1] = v.findViewById(R.id.card_wrapper_2);
            labels[0] = v.findViewById(R.id.label1);
            labels[1] = v.findViewById(R.id.label2);

            imageViews[0] = v.findViewById(R.id.image1);
            imageViews[1] = v.findViewById(R.id.image2);
        }
    }

    public TripGridAdapter(List<TripGridRow> posts, Context context, Fragment fragment) {
        this.posts = posts;
        this.context = context;
        this.fragment = fragment;
        glide = GlideApp.with(context);
    }


    @Override
    public TripGridAdapter.TripGridHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        LinearLayout v = (LinearLayout) LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_profile_trip_grid, parent, false);

        TripGridHolder holder = new TripGridHolder(v);

        return holder;
    }

    @Override
    public void onBindViewHolder(final TripGridAdapter.TripGridHolder holder, int position) {

        final TripGridRow curRow = posts.get(position);

        addRowCol(holder, curRow, 0);
        if (curRow.getRowSize() > 1) addRowCol(holder, curRow, 1);
    }

    private void addRowCol(final TripGridAdapter.TripGridHolder holder, final TripGridRow curRow, final int col){

        holder.cardWrapper[col].setVisibility(View.VISIBLE);

        // when item of grid is clicked, navigate to trip post page
        holder.cardWrapper[col].setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Bundle bd = new Bundle();
                bd.putSerializable("curPost", curRow.getTripAt(col));
                NavHostFragment.findNavController(fragment).navigate(R.id.navigation_trip, bd);
            }
        });

        holder.labels[col].setText(curRow.getTitleAt(col));

        final StorageReference imgRef = storageReference.child("images/tripPosts/" + curRow.getImageAt(col, 0));

        imgRef.getBytes(ONE_MEGABYTE).addOnSuccessListener(new OnSuccessListener<byte[]>() {
            @Override
            public void onSuccess(byte[] bytes) {
                glide.load(imgRef).into(holder.imageViews[col]);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {

            }
        });

    }

    @Override
    public int getItemCount() {
        return posts.size();
    }
}
