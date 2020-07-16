package com.foltran.mermaid.adapter.trip;

import android.content.Context;
import android.net.Uri;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.viewpager.widget.PagerAdapter;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestManager;
import com.bumptech.glide.signature.ObjectKey;
import com.foltran.mermaid.R;
import com.foltran.mermaid.model.trip.FeedCard;
import com.foltran.mermaid.model.trip.FeedCardSerializable;
import com.foltran.mermaid.model.trip.TripCard;
import com.foltran.mermaid.storage.images.ContrastTransform;
import com.foltran.mermaid.ui.new_trip.EditCardFragment;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;


/**
 * The TripCardPagerAdapter class is used in TripCardAdapter when the ListAdapter needs to contain
 * an instance of PagerAdapter, which allows the construction of cards that can be scrolled
 * horizontally.
 */
public class TripCardPagerAdapter extends PagerAdapter {

    Context context;
    LayoutInflater layoutInflater;
    TripCard card;
    RequestManager glide;
    Boolean isEditable;

    final long FIVE_MEGABYTE =  5 * 1024*1024;
    FirebaseStorage storage = FirebaseStorage.getInstance();
    StorageReference storageReference = storage.getReference();

    int numPages;

    EditCardFragment editCardFragment;

    FeedCard feedCard;

    FeedCardSerializable feedCardSerializable;

    public TripCardPagerAdapter(Context context, TripCard card, Boolean isEditable,
                                EditCardFragment editCardFragment) {

        this.numPages = 1;

        if (card.getSubType().equals("feedCardNoImages")){
            this.feedCardSerializable = (FeedCardSerializable) card;
            if (card != null && feedCardSerializable.getShortText().size() > 0) {
                this.numPages = feedCardSerializable.getShortText().size();
            }
        }
        else {
            this.feedCard = (FeedCard) card;
            if (card != null && feedCard.getUris().size() > 0) {
                this.numPages = feedCard.getUris().size();
            }
        }

        this.card = card;
        this.context = context;
        this.isEditable = isEditable;

        layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        this.editCardFragment = editCardFragment;

        glide = Glide.with(context);
    }

    public void updateCard(TripCard card){
        this.card = card;
        numPages = Math.max(feedCard.getUris().size(), 1);
    }

    @Override
    public int getItemPosition(Object object)
    {
        return POSITION_NONE;
    }


    @Override
    public int getCount() {
        return numPages;
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == ((CardView) object);
    }

    @Override
    public Object instantiateItem(final ViewGroup container, final int position) {

        View itemView;

        switch (card.getType()){

            case "feedCard":

                if (isEditable) {
                    itemView = layoutInflater.inflate(R.layout.trip_card_feed_editable, container, false);
                }
                else{
                    itemView = layoutInflater.inflate(R.layout.trip_card_feed, container, false);
                }

                if (card.getSubType().equals("feedCardNoImages")){
                    updateFeedCardSerializable(itemView, position);
                }
                else{
                    updateFeedCard(itemView, position);
                }
                break;
            default:
                itemView = layoutInflater.inflate(R.layout.trip_card_feed, container, false);

        }

        container.addView(itemView);

        return itemView;
    }

    private void updateFeedCardSerializable(View itemView, int position){

        final ImageView imageView = (ImageView) itemView.findViewById(R.id.cur_trip_pic);
        final TextView imageText = (TextView) itemView.findViewById(R.id.feed_card_bottom_text);

        if (position < feedCardSerializable.getNumCards()) {

            imageText.setText(feedCardSerializable.getShortTextAt(position));

            final StorageReference imgRef = storageReference.child("images/tripPosts/" +
                    feedCardSerializable.getImagePathAt(position));

            imgRef.getBytes(FIVE_MEGABYTE).addOnSuccessListener(new OnSuccessListener<byte[]>() {
                @Override
                public void onSuccess(byte[] bytes) {
                    glide.load(imgRef).into(imageView);
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {

                }
            });


        } else {
            imageView.setImageDrawable(context.getResources().getDrawable(R.drawable.mermaid_tail_transparent));
        }
    }

    private void updateFeedCard(View itemView, int position){

        final ImageView imageView = (ImageView) itemView.findViewById(R.id.cur_trip_pic);

        if (isEditable) {

            final LinearLayout contentWrapper = itemView.findViewById(R.id.content_wrapper);
            final LinearLayout emptyWrapper = itemView.findViewById(R.id.empty_card_wrapper);
            final ImageView editImageBtn = (ImageView) itemView.findViewById(R.id.edit_image);
            final EditText bottomTextEdit = itemView.findViewById(R.id.feed_card_bottom_text);

            if (position < feedCard.getUris().size()) {

                emptyWrapper.setVisibility(View.GONE);
                contentWrapper.setVisibility(View.VISIBLE);

                Uri curUri = feedCard.getUriAt(position);
                Integer[] simpleAttrs = feedCard.getSimpleAttrs(position);

                glide.load(curUri)
                        .transform(new ContrastTransform(simpleAttrs[0], simpleAttrs[1]))
                        .signature(new ObjectKey(System.currentTimeMillis()))
                        .into(imageView);

                imageView.setRotation(feedCard.getRotation(position));

                editImageBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        editCardFragment.editImagePopup();
                    }
                });


                bottomTextEdit.setText(feedCard.getBottomTextAt(position));
                // set up editText for bottom text
                bottomTextEdit.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {}

                    @Override
                    public void afterTextChanged(Editable s) {
                        editCardFragment.updateFeedCardBottomText(s.toString());
                    }
                });

            } else {

                contentWrapper.setVisibility(View.GONE);
                emptyWrapper.setVisibility(View.VISIBLE);
            }
        }
        else{

            final TextView imageText = (TextView) itemView.findViewById(R.id.feed_card_bottom_text);

            if (position < feedCard.getUris().size()) {


                Uri curUri = feedCard.getUriAt(position);
                Integer[] simpleAttrs = feedCard.getSimpleAttrs(position);

                glide.load(curUri)
                        .transform(new ContrastTransform(simpleAttrs[0], simpleAttrs[1]))
                        //.signature(new ObjectKey(System.currentTimeMillis()))
                        .into(imageView);

                imageView.setRotation(feedCard.getRotation(position));
                imageText.setText(feedCard.getBottomTextAt(position));


            } else {
                imageView.setImageDrawable(context.getResources().getDrawable(R.drawable.mermaid_tail_transparent));
            }

        }
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        container.removeView((CardView) object);
    }

}
