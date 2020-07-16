package com.foltran.mermaid.adapter.trip_post;

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
import androidx.viewpager.widget.PagerAdapter;

import com.bumptech.glide.RequestManager;
import com.foltran.mermaid.R;
import com.foltran.mermaid.model.trip.TripCardWrapper;
import com.foltran.mermaid.storage.images.GlideApp;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.List;

public class TripPostPagerAdapter extends PagerAdapter {

    final long ONE_MEGABYTE = 1024*1024;

    Context context;
    Fragment fragment;
    TripCardWrapper trip;
    RequestManager glide;

    List<String> imagePaths = new ArrayList<>();
    List<String> shortTexts = new ArrayList<>();

    LayoutInflater layoutInflater;
    int itemLayout;


    public TripPostPagerAdapter(Context context, TripCardWrapper trip, Fragment fragment) {

        this.context = context;
        glide = GlideApp.with(context);

        List<String> feedCardImages = trip.getImagePaths();
        if (feedCardImages != null) this.imagePaths = new ArrayList<>(feedCardImages);

        List<String> feedCardShortTexts = trip.getShortTexts();
        if (feedCardShortTexts != null) this.shortTexts = new ArrayList<>(feedCardShortTexts);

        this.trip = trip;
        layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.itemLayout = R.layout.fragment_home_feed_item_card;
        this.fragment = fragment;
    }

    @Override
    public int getCount() {
        return imagePaths.size();
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == ((CardView) object);
    }

    @Override
    public Object instantiateItem(final ViewGroup container, final int position) {

        View itemView = layoutInflater.inflate(itemLayout, container, false);

        final TextView cardImageCaption= (TextView) itemView.findViewById(R.id.card_caption);
        cardImageCaption.setText(shortTexts.get(position));

        final ImageView cardImageView = (ImageView) itemView.findViewById(R.id.card_image);

        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageReference = storage.getReference();

        final StorageReference imgRef = storageReference.child("images/tripPosts/" + imagePaths.get(position));

        imgRef.getBytes(ONE_MEGABYTE).addOnSuccessListener(new OnSuccessListener<byte[]>() {
            @Override
            public void onSuccess(byte[] bytes) {
                glide.load(imgRef).into(cardImageView);

            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {

            }
        });

        itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Bundle bd = new Bundle();
                bd.putSerializable("curPost", trip);
                NavHostFragment.findNavController(fragment).navigate(R.id.navigation_trip, bd);
            }
        });

        container.addView(itemView);

        return itemView;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        container.removeView((LinearLayout) object);
    }
}
