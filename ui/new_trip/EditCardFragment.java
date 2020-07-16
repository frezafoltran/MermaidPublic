package com.foltran.mermaid.ui.new_trip;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;

import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.ViewPager;

import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.signature.ObjectKey;
import com.foltran.mermaid.MainActivity;
import com.foltran.mermaid.R;
import com.foltran.mermaid.adapter.item_mover.OnStartDragListener;
import com.foltran.mermaid.adapter.item_mover.RecyclerListAdapter;
import com.foltran.mermaid.adapter.item_mover.SimpleItemTouchHelperCallback;
import com.foltran.mermaid.adapter.trip.TripCardPagerAdapter;
import com.foltran.mermaid.model.location.Location;
import com.foltran.mermaid.model.trip.FeedCard;
import com.foltran.mermaid.model.trip.MapCard;
import com.foltran.mermaid.model.trip.ShortTextCard;
import com.foltran.mermaid.model.trip.TripCard;
import com.foltran.mermaid.model.trip.TripCardWrapper;
import com.foltran.mermaid.storage.images.ContrastTransform;
import com.foltran.mermaid.storage.images.LocalImageUtil;
import com.foltran.mermaid.ui.map_box.EditTripMapActivity;
import com.mapbox.mapboxsdk.geometry.LatLng;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import static android.app.Activity.RESULT_CANCELED;
import static android.app.Activity.RESULT_OK;

public class EditCardFragment extends Fragment implements OnStartDragListener {

    // constructor variables
    View root;
    LayoutInflater inflater;
    ViewGroup container;

    Context context;
    EditCardFragment thisFragment;
    PreviewTripFragment previewFragment;

    //main layout views
    LinearLayout backgroundWrapper;
    ViewPager cardViewPager;     // display cards that use pager
    CardView cardViewSimple;    //display cards that do not use pager

    LinearLayout cardViewPagerWrapper;
    RelativeLayout cardViewSimpleWrapper;

    LinearLayout btnReorderCards;
    LinearLayout allCardPreviewWrapper;

    View addCardBtn;

    // FeedCard options
    TripCardPagerAdapter cardViewPagerAdapter;
    ImageView addImageBtn;
    Boolean addImageFlag = true;   //set to true corresponds to adding image, false replaces cur image

    // manage cards popup
    ItemTouchHelper mItemTouchHelper;
    PopupWindow manageCardsPopup;

    //edit image popup
    ImageView curImageViewPopup;

    EditText tripTitleView;

    int curNumTripCards;

    // style specs
    int color;
    float popUpBGOpacity = 0.5f;
    GradientDrawable border = new GradientDrawable();
    int accentBorderWidth = 6;

    Display display;
    DisplayMetrics displayMetrics = new DisplayMetrics();
    float displayWidth;

    // object to handle local image requests
    final LocalImageUtil localImageUtil = new LocalImageUtil(this, null);

    NewTripViewModel model;

    public EditCardFragment(){}

    @SuppressLint("ValidFragment")
    public EditCardFragment(Context context, int color,
                            PreviewTripFragment previewFragment) {
        this.color = color;
        this.context = context;

        this.previewFragment = previewFragment;
    }

    @Override
    public void onCreate(Bundle bundle){
        super.onCreate(bundle);
        model = ViewModelProviders.of(requireActivity()).get(NewTripViewModel.class);
        model.init();

        model.getWorkingTrip().observe(this, new Observer<TripCardWrapper>() {
            @Override
            public void onChanged(TripCardWrapper tripCardWrapper) {
                handleTripChanges(tripCardWrapper);
            }
        });

        model.getFeedCard().observe(this, new Observer<FeedCard>() {
            @Override
            public void onChanged(FeedCard feedCard) {
                handleFeedCardChanges(feedCard);
            }
        });

        getActivity().getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        display = getActivity().getWindowManager().getDefaultDisplay();
        float screenDensity  = getResources().getDisplayMetrics().density;

        displayWidth = displayMetrics.widthPixels/screenDensity;

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        final View root = inflater.inflate(R.layout.new_trip_edit, container, false);

        this.root = root;
        this.inflater = inflater;
        this.container = container;
        this.backgroundWrapper = root.findViewById(R.id.new_trip_edit_wrapper);

        //init variables to manage display of current card
        this.cardViewPager = root.findViewById(R.id.edit_card_pager);
        this.cardViewSimple = root.findViewById(R.id.edit_card_cardview);

        this.cardViewPagerWrapper = root.findViewById(R.id.edit_card_pager_wrapper);
        this.cardViewSimpleWrapper = root.findViewById(R.id.edit_card_cardview_wrapper);

        localImageUtil.setContext(context);

        // variables for horizontal all card preview
        this.allCardPreviewWrapper = (LinearLayout)root.findViewById(R.id.scroll_preview);

        setupMainViewClicks();
        setupScrollPreview();

        cardViewPagerAdapter = new TripCardPagerAdapter(context,
                model.getFeedCard().getValue(), true, this);

        cardViewPagerAdapter.updateCard(model.getFeedCard().getValue());
        cardViewPager.setAdapter(cardViewPagerAdapter);

        if (curNumTripCards == model.numCards() || curNumTripCards == 0){
            updateCardsUi();
            curNumTripCards = model.numCards();
        }

        return root;
    }

    /**
     * Handles the manage cards popup movement of items
     * @param viewHolder The holder of the view to drag.
     */
    @Override
    public void onStartDrag(RecyclerView.ViewHolder viewHolder) {
        mItemTouchHelper.startDrag(viewHolder);
    }

    @Override
    public void onResume(){
        super.onResume();
    }

    /**
     * This method updates the UI based on the current trip when the fragment is destroyed and
     * onCreateView is called again but the user has not left NewTripFragment.
     */
    private void updateCardsUi(){

        List<TripCard> savedCards = model.getAllCards();
        TripCard feedCard = savedCards.get(0);

        View feedCardPreview = createPreviewCard(feedCard);
        cardViewPagerAdapter = new TripCardPagerAdapter(context, feedCard, true, this);
        cardViewPagerAdapter.updateCard(feedCard);
        cardViewPager.setAdapter(cardViewPagerAdapter);

        cardViewPagerWrapper.setVisibility(View.VISIBLE);
        cardViewSimpleWrapper.setVisibility(View.GONE);
        model.setDisplayedCard(0);

        feedCardPreview.performClick();

        for (TripCard card : savedCards.subList(1, savedCards.size())){
            createPreviewCard(card);
        }
    }

    /**
     * Sets up the edit options that are always present and those that
     * are related to the FeedCard.
     */
    private void setupMainViewClicks(){

        btnReorderCards = root.findViewById(R.id.reorder_cards_btn);
        btnReorderCards.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                backgroundWrapper.setAlpha(popUpBGOpacity);
                reorderCardsPopup();
            }
        });

        // FeedCard options are also added since FeedCard is always present once

        addImageBtn = root.findViewById(R.id.add_picture_btn);
        addImageBtn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                addImageFlag = true;
                localImageUtil.selectImage();
            }
        });

        tripTitleView = root.findViewById(R.id.trip_title);
        tripTitleView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                model.setTripTitle(s.toString());
            }

        });

    }

    public void setSelfFragment(EditCardFragment thisFragment){
        this.thisFragment = thisFragment;
    }

    private void setupScrollPreview(){

        addCardBtn = inflater.inflate(R.layout.new_trip_edit_scroll_preview, container, false);
        ImageView addCardImage = addCardBtn.findViewById(R.id.image_scroll_preview);
        addCardImage.setVisibility(View.VISIBLE);

        addCardImage.setImageResource(R.drawable.ic_add_black_48dp);

        allCardPreviewWrapper.addView(addCardBtn);

        addCardBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                backgroundWrapper.setAlpha(popUpBGOpacity);
                cardTypePopup();
            }
        });

    }

    private void handleTripChanges(TripCardWrapper updatedTrip){

        int numCards = updatedTrip.getNumCards();
        //need to add card to UI
        if (curNumTripCards < numCards){

            View previewCard = createPreviewCard(updatedTrip.getCardAt(numCards - 1));

            // if it's first card added, click on it
            if (numCards == 1) previewCard.performClick();
        }
        // check if any changes in preview
        else{
            for (int i = 0; i < allCardPreviewWrapper.getChildCount() - 1; i++){

                TripCard card = model.getCardAt(i);
                if (card.getType().equals("mapCard")){

                    View child = allCardPreviewWrapper.getChildAt(i);
                    String imageUrl = ((MapCard) card).getMapPreview().url().toString();
                    ImageView imagePreview = child.findViewById(R.id.image_scroll_preview);
                    Glide.with(context).load(imageUrl).into(imagePreview);
                }
            }

        }

        curNumTripCards = numCards;
    }

    private void handleFeedCardChanges(FeedCard feedCard){
        cardViewPagerAdapter.notifyDataSetChanged();
    }

    private View createPreviewCard(TripCard card){

        String cardType = card.getType();

        View child = inflater.inflate(R.layout.new_trip_edit_scroll_preview, container, false);
        ImageView imagePreview = child.findViewById(R.id.image_scroll_preview);
        ImageView imagePreviewFeedCard = child.findViewById(R.id.image_scroll_preview_feed_card);

        switch (cardType){

            case "feedCard":
                CardView wrapper = child.findViewById(R.id.feed_card_preview_wrapper);
                wrapper.setVisibility(View.VISIBLE);
                imagePreviewFeedCard.setImageResource(R.drawable.mermaid_tail_transparent);
                break;

            case "shortTextCard":
                imagePreview.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_text_fields_black_24dp));
                imagePreview.setVisibility(View.VISIBLE);
                break;

            case "mapCard":
                String imageUrl = ((MapCard) card).getMapPreview().url().toString();
                Glide.with(context).load(imageUrl).into(imagePreview);
                imagePreview.setVisibility(View.VISIBLE);
                break;

            default:

        }

        setClickPreview(child, allCardPreviewWrapper.getChildCount() - 1);
        allCardPreviewWrapper.addView(child, allCardPreviewWrapper.getChildCount() - 1);

        return child;
    }

    /**
     * Handles deletion of cards. Deletes card from the UI as well as the trip object
     * @param cardIndex
     */
    public void deleteCard(int cardIndex){

        // if index is out of bounds, not delete
        if (!model.removeCard(cardIndex + 1)) return;

        // remove from horizontal preview
        allCardPreviewWrapper.removeViewAt(cardIndex + 1);

        for (int i = cardIndex + 1; i < allCardPreviewWrapper.getChildCount() - 1; i++){
            setClickPreview(allCardPreviewWrapper.getChildAt(i), i);
        }

        allCardPreviewWrapper.getChildAt(model.curDisplayedCardIndex()).performClick();

    }

    public void moveCards(int fromPosition, int toPosition){

        // +1 accounts for the immutable feedCard
        View fromView = allCardPreviewWrapper.getChildAt(fromPosition + 1);
        View toView = allCardPreviewWrapper.getChildAt(toPosition + 1);

        allCardPreviewWrapper.removeView(fromView);
        allCardPreviewWrapper.addView(fromView, toPosition + 1);
        setClickPreview(fromView, toPosition + 1);

        allCardPreviewWrapper.removeView(toView);
        allCardPreviewWrapper.addView(toView, fromPosition + 1);
        setClickPreview(toView, fromPosition + 1);

        model.moveCards(fromPosition + 1, toPosition + 1);

        // uncomment if you wish to keep the card at the same position selected
        //allCardPreviewWrapper.getChildAt(trip.getCurIndexDisplayed()).performClick();
    }

    private void setClickPreview(View child, final int position){

        final View childToChange = child;

        child.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                View curCardPreview = allCardPreviewWrapper.getChildAt(model.curDisplayedCardIndex());
                model.setDisplayedCard(position);

                // highlight current choice
                if (curCardPreview != null) {
                    curCardPreview.setBackground(null);
                    curCardPreview.setBackgroundColor(getResources().getColor(R.color.colorWhite));
                    curCardPreview.setAlpha(1f);
                }

                border.setColor(getResources().getColor(R.color.colorWhite));
                border.setStroke(accentBorderWidth, getResources().getColor(R.color.colorYellowHighlight));

                childToChange.setBackground(border);
                childToChange.setAlpha(0.5f);
                updateDisplayCard();

            }
        });

    }

    private void updateDisplayCard(){

        String cardType = model.curCardType();

        // change edit options
        setupEditBox(cardType);

        togglePager(model.curCardNeedsPager());

        switch (cardType){
            case "feedCard":

                break;

            case "shortTextCard":
                inflater.inflate(R.layout.trip_card_short_text_editable, cardViewSimple, true);

                final EditText shortTextEditText = root.findViewById(R.id.shortText);

                final ShortTextCard curShortTextCard = ((ShortTextCard) model.curDisplayedCard());
                String curText = curShortTextCard.getText();

                if (!curText.equals("")) shortTextEditText.setText(curText);

                shortTextEditText.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {}

                    @Override
                    public void afterTextChanged(Editable s) {
                        curShortTextCard.setText(s.toString());
                    }
                });
                break;

            case "mapCard":
                inflater.inflate(R.layout.trip_card_map_editable, cardViewSimple, true);

                ImageView mapCardView = root.findViewById(R.id.map_card_img);

                final MapCard curCard = (MapCard) model.curDisplayedCard();
                //String imageUrl = buildMapPreview(curCard).url().toString();
                String imageUrl = curCard.getMapPreview().url().toString();

                Glide.with(context).load(imageUrl).into(mapCardView);

                mapCardView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        NavHostFragment.findNavController(EditCardFragment.this).navigate(R.id.navigation_edit_trip_map);
                    }
                });
                break;
        }
    }

    public void editImagePopup(){

        View editImagePopupView = inflater.inflate(R.layout.edit_image_feed_card_popup, null);

        TextView saveExitBtn = editImagePopupView.findViewById(R.id.save_changes_exit);
        curImageViewPopup = editImagePopupView.findViewById(R.id.cur_editable_image);

        TextView changeImageBtn = editImagePopupView.findViewById(R.id.add_picture_btn);
        ImageView rotateImage = editImagePopupView.findViewById(R.id.rotate_image);
        TextView sampleFilter = editImagePopupView.findViewById(R.id.filter_1);

        final int curIndex = cardViewPager.getCurrentItem();

        int width = LinearLayout.LayoutParams.MATCH_PARENT;
        int height = LinearLayout.LayoutParams.MATCH_PARENT;

        final PopupWindow editImagePopup = new PopupWindow(editImagePopupView, width, height, true);

        final FeedCard feedCard = (FeedCard) model.curDisplayedCard();
        final Integer[] curAttrs = feedCard.getSimpleAttrs(curIndex);

        Glide.with(context).load(feedCard.getUriAt(curIndex))
                .transform(new ContrastTransform(curAttrs[0], curAttrs[1]))
                .signature(new ObjectKey(System.currentTimeMillis()))
                .into(curImageViewPopup);

        saveExitBtn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                editImagePopup.dismiss();
            }
        });

        changeImageBtn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                addImageFlag = false;
                localImageUtil.selectImage();
            }
        });

        rotateImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //ensure that an image exists
                if (feedCard.getUris().size() > 0) {

                    int curItem = cardViewPager.getCurrentItem();
                    curImageViewPopup.setRotation(feedCard.getRotation(curItem) + 90);
                    feedCard.setRotation(curItem, feedCard.getRotation(curItem) + 90);
                    cardViewPagerAdapter.notifyDataSetChanged();
                }
            }
        });

        sampleFilter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // ensures an image exists
                if(feedCard.getUris().size() > 0) {

                    if (curAttrs[0] == 1 && curAttrs[1] == 0) {
                        feedCard.setSimpleAttrs(curIndex, new Integer[]{3, 0});
                    } else {
                        feedCard.setSimpleAttrs(curIndex, new Integer[]{1, 0});
                    }
                    cardViewPagerAdapter.notifyDataSetChanged();

                    Integer[] simpleAttrs = feedCard.getSimpleAttrs(curIndex);

                    Glide.with(context).load(feedCard.getUriAt(curIndex))
                            .transform(new ContrastTransform(simpleAttrs[0], simpleAttrs[1]))
                            .signature(new ObjectKey(System.currentTimeMillis()))
                            .into(curImageViewPopup);
                }
            }
        });


        editImagePopup.showAtLocation(container, Gravity.CENTER, 0, 0);

    }

    private void reorderCardsPopup(){

        View popupView = inflater.inflate(R.layout.new_trip_edit_reorder_cards_popup, null);

        List<String> labels = new ArrayList<>();
        List<Drawable> icons = new ArrayList<>();

        for (TripCard card : model.getAllCards().subList(1, model.numCards())){

            if (card.getType().equals("shortTextCard")) {
                icons.add(context.getResources().getDrawable(R.drawable.ic_text_fields_black_24dp));
            }
            else if (card.getType().equals("mapCard")) {
                icons.add(context.getResources().getDrawable(R.drawable.us_map));
            }
            else{
                icons.add(context.getResources().getDrawable(R.drawable.mermaid_tail_transparent));
            }
            labels.add(card.getType());
        }

        RecyclerListAdapter adapter = new RecyclerListAdapter(getActivity(), thisFragment,
                this, labels, null, icons, true);

        RecyclerView recyclerView = popupView.findViewById(R.id.reorder_cards_recycler);
        recyclerView.setHasFixedSize(true);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        ItemTouchHelper.Callback callback = new SimpleItemTouchHelperCallback(adapter);
        mItemTouchHelper = new ItemTouchHelper(callback);
        mItemTouchHelper.attachToRecyclerView(recyclerView);

        int width = LinearLayout.LayoutParams.WRAP_CONTENT;
        int height = LinearLayout.LayoutParams.WRAP_CONTENT;
        boolean focusable = true; // lets taps outside the popup also dismiss it
        manageCardsPopup = new PopupWindow(popupView, width, height, focusable);

        manageCardsPopup.showAtLocation(container, Gravity.CENTER, 0, 0);

        manageCardsPopup.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                backgroundWrapper.setAlpha(1f);
            }
        });

    }

    private void cardTypePopup() {

        View popupView = inflater.inflate(R.layout.new_trip_edit_card_type_popup, null);

        // create the popup window
        int width = LinearLayout.LayoutParams.WRAP_CONTENT;
        int height = LinearLayout.LayoutParams.WRAP_CONTENT;
        boolean focusable = true; // lets taps outside the popup also dismiss it
        final PopupWindow popupWindow = new PopupWindow(popupView, width, height, focusable);

        popupWindow.showAtLocation(container, Gravity.CENTER, 0, 0);

        popupWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                backgroundWrapper.setAlpha(1f);
            }
        });

        Button mapCardButton = popupView.findViewById(R.id.map_card_button);
        Button textCardButton = popupView.findViewById(R.id.text_card_button);

        mapCardButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                model.addCard(new MapCard());
                popupWindow.dismiss();
            }
        });

        textCardButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                model.addCard(new ShortTextCard(null));
                popupWindow.dismiss();
            }
        });

    }

    public void togglePager(Boolean showPager){
        if (showPager){
            cardViewSimpleWrapper.setVisibility(View.GONE);
            cardViewPagerWrapper.setVisibility(View.VISIBLE);
        }
        else{
            cardViewPagerWrapper.setVisibility(View.GONE);
            cardViewSimpleWrapper.setVisibility(View.VISIBLE);
            if(cardViewSimple.getChildCount() > 0) cardViewSimple.removeAllViews();
        }
    }

    public void setupEditBox(String cardType){

        LinearLayout topEdit = root.findViewById(R.id.edit_card_top);
        LinearLayout bottomEdit = root.findViewById(R.id.edit_card_bottom);

        switch (cardType){
            case "feedCard":
                editBoxFeedCard(topEdit);
                break;
            default:
                addImageBtn.setVisibility(View.GONE);
                //topEdit.setVisibility(View.GONE);
        }

    }

    private void editBoxFeedCard(LinearLayout topEdit){
        topEdit.setVisibility(View.VISIBLE);
        addImageBtn.setVisibility(View.VISIBLE);
    }


    public void getPictureActivity(Intent intent, int reqCode){
        startActivityForResult(intent, reqCode);
    }

    private void updatePagerAdapter(){

        Thread thread = new Thread() {
            @Override
            public void run() {

                cardViewPagerAdapter.updateCard((FeedCard) model.curDisplayedCard());

                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        cardViewPagerAdapter.notifyDataSetChanged();
                    }
                });


            }
        };
        thread.start();

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        if(resultCode != RESULT_CANCELED) {

            Uri curUri = null;
            switch (requestCode) {
                case 0:
                    if (resultCode == RESULT_OK && data != null) {
                        curUri = localImageUtil.getUri();
                    }

                    break;
                case 1:
                    if (resultCode == RESULT_OK && data != null) {
                        Uri selectedImage =  data.getData();
                        String[] filePathColumn = {MediaStore.Images.Media.DATA};
                        if (selectedImage != null) {
                            Cursor cursor = context.getContentResolver().query(selectedImage,
                                    filePathColumn, null, null, null);
                            if (cursor != null) {
                                cursor.moveToFirst();
                                curUri = data.getData();
                                cursor.close();
                            }
                        }

                    }
                    break;
            }

            FeedCard feedCard = ((FeedCard) model.curDisplayedCard());
            if (addImageFlag) {
                feedCard.addUri(curUri);
            }
            else{
                Glide.with(context).load(curUri).into(curImageViewPopup);
                feedCard.replaceUri(cardViewPager.getCurrentItem(), curUri);
            }

            updatePagerAdapter();

        }
    }

    public void updateFeedCardBottomText(String s){
        ((FeedCard) model.curDisplayedCard()).setBottomTextAt(cardViewPager.getCurrentItem(), s);
    }

}

