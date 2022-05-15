package com.porterlee.inventory;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;


public class ItemRecyclerAdapter extends RecyclerView.Adapter<ItemRecyclerAdapter.ViewHolder> {

    private final Context mContext;
    private ArrayList<ArrayList<String>> mData;
    private LayoutInflater mInflater;
    private ItemClickListener mClickListener;
    private MainActivity mInstance;

    private EditText quantityEditText;

    // data is passed into the constructor
    ItemRecyclerAdapter(Context context, ArrayList<ArrayList<String>> data, MainActivity instance) {
        this.mInflater = LayoutInflater.from(context);
        this.mData = data;
        this.mContext = context;
        this.mInstance = instance;
    }

    // inflates the row layout from xml when needed
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.inventory_quantity_item_layout, parent, false);
        return new ViewHolder(view);
    }

    // binds the data to the TextView in each row
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        ArrayList<String> item = mData.get(position);
        String animal = item.get(0);
        holder.myTextView.setText(animal);
        holder.quantityView.setText(item.get(1));
    }

    // total number of rows
    @Override
    public int getItemCount() {
        return mData.size();
    }


    // stores and recycles views as they are scrolled off screen
    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        TextView myTextView;
        TextView quantityView;

        private TextView barcodeTextView;
        private EditText quantityEditText;
        private ImageButton expandedMenuButton;
        private long id = -1;
        private long locationId = -1;
        private long quantity = 1;
        private String barcode = "";
        private String description = "";
        private String tags = "";
        private String dateTime = "";
        private boolean isSelected = false;

        ViewHolder(View itemView) {
            super(itemView);
            myTextView = itemView.findViewById(R.id.barcode_text_view);
            quantityView = itemView.findViewById(R.id.quantity);
            itemView.setOnClickListener(this);

            View expandedMenuButton = itemView.findViewById(R.id.menu_button);
            expandedMenuButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    PopupMenu popup = new PopupMenu(mContext, view);
                    MenuInflater inflater = popup.getMenuInflater();
                    inflater.inflate(R.menu.inventory_item_popup_menu, popup.getMenu());
                    popup.getMenu().findItem(R.id.remove_item).setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            mInstance.itemRemoveAction(getAdapterPosition());
                            return true;
                        }
                    });
                    popup.getMenu().findItem(R.id.edit_quantity).setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            mInstance.editQuantity(getAdapterPosition());
                            return true;
                        }
                    });
                    popup.show();
                }
            });
        }

        @Override
        public void onClick(View view) {
            if (mClickListener != null) mClickListener.onItemClick(view, getAdapterPosition());
        }


    }

    // convenience method for getting data at click position
    String getItem(int id) {
        ArrayList<String> item = mData.get(id);
        return item.get(0);
    }

    // allows clicks events to be caught
    void setClickListener(ItemClickListener itemClickListener) {
        this.mClickListener = itemClickListener;
    }

    // parent activity will implement this method to respond to click events
    public interface ItemClickListener {
        void onItemClick(View view, int position);
    }
}

