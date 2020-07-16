package com.foltran.mermaid.adapter.item_mover;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.cardview.widget.CardView;
import androidx.core.view.MotionEventCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.foltran.mermaid.R;
import com.foltran.mermaid.ui.new_trip.EditCardFragment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class RecyclerListAdapter extends RecyclerView.Adapter<RecyclerListAdapter.ItemViewHolder>
        implements ItemTouchHelperAdapter {

    private final List<String> itemLabels = new ArrayList<>();
    private final List<String> subItemLabels = new ArrayList<>();
    private final List<Drawable> itemIcons = new ArrayList<>();
    private List<Integer> gradientCodes;

    private final Context context;
    private final EditCardFragment editCardFragment;

    private final OnStartDragListener mDragStartListener;
    private Boolean showDelete;
    private Boolean enableGradient;

    public RecyclerListAdapter(Context context,
                               EditCardFragment editCardFragment,
                               OnStartDragListener dragStartListener,
                               List<String> items, List<String> subItems,
                               List<Drawable> icons,
                               Boolean showDelete) {

        this.context = context;
        this.showDelete = showDelete;
        this.enableGradient = !showDelete;
        mDragStartListener = dragStartListener;

        itemLabels.addAll(items);

        if (subItems != null) {
            subItemLabels.addAll(subItems);
        }

        itemIcons.addAll(icons);
        this.editCardFragment = editCardFragment;

        Resources res = context.getResources();
        this.gradientCodes = new ArrayList<>(Arrays.asList(
                res.getColor(R.color.colorGradient1),
                res.getColor(R.color.colorGradient2),
                res.getColor(R.color.colorGradient3),
                res.getColor(R.color.colorGradient4),
                res.getColor(R.color.colorGradient5),
                res.getColor(R.color.colorGradient6)
        ));
    }

    @Override
    public ItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_simple_recycler_change_order, parent, false);

        if (showDelete){
            ImageView delButton = view.findViewById(R.id.delete_icon);
            delButton.setVisibility(View.VISIBLE);
        }

        ItemViewHolder itemViewHolder = new ItemViewHolder(view);
        return itemViewHolder;
    }

    @Override
    public void onBindViewHolder(final ItemViewHolder holder, final int position) {

        holder.textView.setText(itemLabels.get(position));

        if (subItemLabels.size() > position){
            holder.subTextView.setText(subItemLabels.get(position));
            holder.subTextView.setVisibility(View.VISIBLE);
        }

        holder.iconView.setImageDrawable(itemIcons.get(position));

        if (enableGradient) {
            holder.handleView.setCardBackgroundColor(gradientCodes.get(position));
        }

        if (showDelete) {
            holder.btnDelete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    editCardFragment.deleteCard(position);

                    itemLabels.remove(position);
                    itemIcons.remove(position);

                    if (subItemLabels.size() > position){
                        subItemLabels.remove(position);
                    }
                    notifyDataSetChanged();
                }
            });
        }

        // Start a drag whenever the handle view it touched
        holder.handleView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (MotionEventCompat.getActionMasked(event) == MotionEvent.ACTION_DOWN) {
                    mDragStartListener.onStartDrag(holder);
                }
                return false;
            }
        });
    }

    @Override
    public void onItemDismiss(int position) {

        itemLabels.remove(position);
        itemIcons.remove(position);
        notifyItemRemoved(position);
    }

    @Override
    public boolean onItemMove(int fromPosition, int toPosition) {
        Collections.swap(itemLabels, fromPosition, toPosition);

        if (subItemLabels.size() > fromPosition && subItemLabels.size() > toPosition) {
            Collections.swap(subItemLabels, fromPosition, toPosition);
        }
        Collections.swap(itemIcons, fromPosition, toPosition);

        if (showDelete){
            editCardFragment.moveCards(fromPosition, toPosition);
        }
        notifyItemMoved(fromPosition, toPosition);

        return true;
    }

    @Override
    public int getItemCount() {
        return itemLabels.size();
    }

    public class ItemViewHolder extends RecyclerView.ViewHolder implements
            ItemTouchHelperViewHolder {

        private final TextView textView;
        private final TextView subTextView;
        private final ImageView iconView;
        private final ImageView btnDelete;
        private final CardView handleView;

        public ItemViewHolder(View itemView) {
            super(itemView);
            textView = (TextView) itemView.findViewById(R.id.text);
            subTextView = (TextView) itemView.findViewById(R.id.subItemText);
            iconView = (ImageView) itemView.findViewById(R.id.item_icon);
            handleView = (CardView) itemView.findViewById(R.id.handle);
            btnDelete = (ImageView) itemView.findViewById(R.id.delete_icon);
        }

        @Override
        public void onItemSelected() {
            itemView.setAlpha(0.5f);
        }

        @Override
        public void onItemClear() {
            itemView.setAlpha(1f);
            if (enableGradient) {
                notifyDataSetChanged();
            }
        }
    }

    public List<String> getItemsOrdered(){
        return itemLabels;
    }
}