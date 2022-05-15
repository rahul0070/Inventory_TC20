package com.porterlee.inventory;

import android.content.Context;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;


public class LocationRecyclerAdapter extends RecyclerView.Adapter<LocationRecyclerAdapter.LocationViewHolder> {

    private final Context mContext;
    private List<String> mData;
    private LayoutInflater mInflater;
    private ItemClickListener mClickListener;
    private MainActivity instance;

    private static final String TAG = MainActivity.class.getSimpleName();
    private List<View> viewList = new ArrayList<View>();

    // data is passed into the constructor
    LocationRecyclerAdapter(Context context, List<String> data, MainActivity instance) {
        this.mInflater = LayoutInflater.from(context);
        this.mData = data;
        this.mContext = context;
        this.instance = instance;
    }

    // inflates the row layout from xml when needed
    @Override
    public LocationViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.location_recycler_view, parent, false);
        return new LocationViewHolder(view);
    }

    // binds the data to the TextView in each row
    @Override
    public void onBindViewHolder(LocationViewHolder holder, int position) {
        String animal = mData.get(position);
        holder.myTextView.setText(animal);

        //holder.myTextView.setBackgroundColor(Integer.parseInt("FF3480EA"));

//        if (selectedItem == position) {
//            holder.cardView.setCardBackgroundColor(context.getResources().getColor(R.color.colorPrimaryDark));
//        }

    }

    // total number of rows
    @Override
    public int getItemCount() {
        return mData.size();
    }


    // stores and recycles views as they are scrolled off screen
    public class LocationViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        TextView myTextView;
        Button button;
        private SparseBooleanArray selectedItems = new SparseBooleanArray();


        LocationViewHolder(View itemView) {
            super(itemView);
            myTextView = itemView.findViewById(R.id.barcode_text_view);
            itemView.setOnClickListener(this);
//            View expandedMenuButton = itemView.findViewById(R.id.menu_button);
//            expandedMenuButton.setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View view) {
//                    PopupMenu popup = new PopupMenu(mContext, view);
//                    MenuInflater inflater = popup.getMenuInflater();
//                    inflater.inflate(R.menu.inventory_item_popup_menu, popup.getMenu());
//                    popup.getMenu().findItem(R.id.remove_item).setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
//                        @Override
//                        public boolean onMenuItemClick(MenuItem item) {
//                            //return instance.removeAction(getAdapterPosition());
//                            return false;
//                        }
//                    });
//                    popup.show();
//                }
//            });

        }


        @Override
        public void onClick(View view) {
            if (mClickListener != null) mClickListener.onLocationClick(view, getAdapterPosition());
            instance.selectItem(view);



//            if (selectedItems.get(getAdapterPosition(), false)) {
//                selectedItems.delete(getAdapterPosition());
//                view.setSelected(false);
//            }
//            else {
//                selectedItems.put(getAdapterPosition(), true);
//                view.setSelected(true);
//            }

        }

//        public void selectItem(View view){
//            viewList.add(view);
//            for (View item : viewList){
//                Log.v(TAG, "View " + String.valueOf(viewList.size()));
//                item.setSelected(false);
//            }
//            view.setSelected(true);
//        }

    }

    // convenience method for getting data at click position
    String getItem(int id) {
        return mData.get(id);
    }

    // allows clicks events to be caught
    void setClickListener(ItemClickListener itemClickListener) {
        this.mClickListener = itemClickListener;
    }

    // parent activity will implement this method to respond to click events
    public interface ItemClickListener {
        void onLocationClick(View view, int position);
    }

}

