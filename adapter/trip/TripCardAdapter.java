package com.foltran.mermaid.adapter.trip;

import android.content.Context;
import android.database.DataSetObserver;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.TextView;

import androidx.viewpager.widget.ViewPager;

import com.bumptech.glide.Glide;
import com.foltran.mermaid.R;
import com.foltran.mermaid.model.trip.MapCard;
import com.foltran.mermaid.model.trip.ShortTextCard;
import com.foltran.mermaid.model.trip.TripCard;
import com.foltran.mermaid.ui.new_trip.EditCardFragment;

import java.util.List;

/**
 * The TripCardAdapter is an adapter used when displaying the full Trip. Since there are multiple
 * allowed card types within a Trip, TripCardAdapter handles the different layouts by inflating the
 * appropriate card
 *
 */
public class TripCardAdapter implements ListAdapter {

    List<TripCard> cards;
    Context context;
    Boolean isEditable;
    EditCardFragment editCardFragment;

    public TripCardAdapter(List<TripCard> cards, Context context, EditCardFragment editCardFragment) {
        this.cards = cards;
        this.context = context;
        this.isEditable = false;
        this.editCardFragment = editCardFragment;
    }
    @Override
    public boolean areAllItemsEnabled() {
        return false;
    }
    @Override
    public boolean isEnabled(int position) {
        return true;
    }
    @Override
    public void registerDataSetObserver(DataSetObserver observer) {
    }
    @Override
    public void unregisterDataSetObserver(DataSetObserver observer) {
    }
    @Override
    public int getCount() {
        return cards.size();
    }
    @Override
    public Object getItem(int position) {
        return position;
    }
    @Override
    public long getItemId(int position) {
        return position;
    }
    @Override
    public boolean hasStableIds() {
        return false;
    }
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        final TripCard curCard = cards.get(position);

        if(convertView == null) {
            LayoutInflater layoutInflater = LayoutInflater.from(context);

            switch (curCard.getType()){
                case "feedCard":
                    convertView = layoutInflater.inflate(R.layout.trip_card_pager, null);
                    setupPager(convertView, curCard);
                    break;

                case "shortTextCard":
                    convertView = layoutInflater.inflate(R.layout.trip_card_short_text, null);

                    // add content from card
                    final TextView shortText = convertView.findViewById(R.id.shortText);
                    ShortTextCard card = (ShortTextCard) curCard;
                    String curText = card.getText();
                    if (!curText.equals("")) shortText.setText(curText);

                    break;

                case "ratingCard":
                    convertView = layoutInflater.inflate(R.layout.trip_card_rating, null);
                    break;

                case "mapCard":
                    convertView = layoutInflater.inflate(R.layout.trip_card_map, null);
                    ImageView mapView = convertView.findViewById(R.id.map_image_preview);

                    String imageUrl = ((MapCard) curCard).getMapPreview().url().toString();
                    Glide.with(context).load(imageUrl).into(mapView);

                    break;

                default:
                    convertView = layoutInflater.inflate(R.layout.trip_card_pager, null);
            }
        }

        return convertView;
    }

    private void setupPager(View convertView, TripCard card){

        ViewPager viewPager = convertView.findViewById(R.id.trip_card_pager);

        TripCardPagerAdapter pagerAdapter = new TripCardPagerAdapter(context, card, false, editCardFragment);
        viewPager.setAdapter(pagerAdapter);
    }

    @Override
    public int getItemViewType(int position) {
        return position;
    }
    @Override
    public int getViewTypeCount() {
        return cards.size();
    }
    @Override
    public boolean isEmpty() {
        return false;
    }
}
