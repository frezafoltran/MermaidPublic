package com.foltran.mermaid.adapter.location;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.viewpager.widget.PagerAdapter;

import com.bumptech.glide.RequestManager;
import com.foltran.mermaid.R;
import com.foltran.mermaid.model.location.Location;
import com.foltran.mermaid.storage.images.GlideApp;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.List;

public class LocationPagerAdapter extends PagerAdapter {

    Location curLocation;
    Context context;
    Fragment fragment;
    String image;
    LayoutInflater layoutInflater;
    int itemLayout;


    public LocationPagerAdapter(Context context, Fragment fragment, Location cur) {

        this.context = context;
        this.fragment = fragment;
        this.curLocation = cur;
        this.image = cur.getStockImagePath();
        this.layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.itemLayout = R.layout.location_feed_item_card;

    }

    @Override
    public int getCount() {
        return 1;
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == ((CardView) object);
    }

    @Override
    public Object instantiateItem(final ViewGroup container, final int position) {

        View itemView = layoutInflater.inflate(itemLayout, container, false);

        final ImageView imageView = (ImageView) itemView.findViewById(R.id.location_image);

        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageReference = storage.getReference();

        final RequestManager glide = GlideApp.with(context);

        final StorageReference imgRef = storageReference.child(image);
        final long ONE_MEGABYTE = 1024*1024;

        imgRef.getBytes(ONE_MEGABYTE).addOnSuccessListener(new OnSuccessListener<byte[]>() {
            @Override
            public void onSuccess(byte[] bytes) {
                glide.load(imgRef).into(imageView);

            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {

            }
        });

        itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Bundle bundle = new Bundle();
                bundle.putSerializable("locationObj", curLocation);
                NavHostFragment.findNavController(fragment).navigate(R.id.navigation_location_result, bundle);
            }
        });


        container.addView(itemView);

        return itemView;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        container.removeView((CardView) object);
    }
}
