package com.foltran.mermaid.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.viewpager.widget.PagerAdapter;

import com.bumptech.glide.RequestManager;
import com.foltran.mermaid.R;
import com.foltran.mermaid.storage.images.GlideApp;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.List;


/**
 * This class provides a pager structure that is used throughout the app
 */
public class SimpleImagePagerAdapter extends PagerAdapter {

    Context context;
    Fragment fragment;
    LayoutInflater layoutInflater;
    String imagePath;
    int itemLayout;
    int imageViewId;


    public SimpleImagePagerAdapter(Context context, String imagePath, int itemLayout,
                                   int imageViewId, Fragment fragment) {
        this.context = context;
        this.imagePath = imagePath;
        layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.itemLayout = itemLayout;
        this.imageViewId = imageViewId;
        this.fragment = fragment;
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

        final ImageView imageView = (ImageView) itemView.findViewById(imageViewId);

        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageReference = storage.getReference();

        final RequestManager glide = GlideApp.with(context);

        final StorageReference imgRef = storageReference.child(imagePath);
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
                //NavHostFragment.findNavController(fragment).navigate(R.id.navigation_trip);
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
